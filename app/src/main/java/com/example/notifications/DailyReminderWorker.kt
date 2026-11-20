package com.example.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.BudgetWiseDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BudgetWiseRepository
import com.example.domain.FinancialAlertEvaluator
import com.example.domain.currentEpochDay
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.first

/**
 * Play-policy-compliant WorkManager scheduler for BudgetWise's daily logging reminder and
 * 7-day savings goal deadline sweep. Uses [WorkManager] instead of restricted exact alarms.
 */
object DailyReminderScheduler {

  const val UNIQUE_WORK_NAME = "budgetwise_daily_financial_check"

  /**
   * Computes milliseconds until the next occurrence of [hour24]:[minute] in local device time.
   * If the time has already passed today (within 1 minute margin), schedules for tomorrow.
   */
  fun computeDelayMillisUntilNextRun(
    hour24: Int,
    minute: Int,
    nowMillis: Long = System.currentTimeMillis(),
  ): Long {
    val safeHour = hour24.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)

    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val target = Calendar.getInstance().apply {
      timeInMillis = nowMillis
      set(Calendar.HOUR_OF_DAY, safeHour)
      set(Calendar.MINUTE, safeMinute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }

    if (target.timeInMillis <= now.timeInMillis + 60_000L) {
      target.add(Calendar.DAY_OF_YEAR, 1)
    }
    return (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
  }

  /**
   * Schedules or cancels the daily background worker according to [dailyReminderEnabled] and
   * [goalAlertsEnabled]. When enabled, targets the user's chosen local time.
   */
  fun syncSchedule(
    context: Context,
    dailyReminderEnabled: Boolean,
    goalAlertsEnabled: Boolean,
    hour24: Int,
    minute: Int,
  ) {
    runCatching {
      val workManager = WorkManager.getInstance(context.applicationContext)
      if (!dailyReminderEnabled && !goalAlertsEnabled) {
        workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
        return
      }
      val delayMillis = computeDelayMillisUntilNextRun(hour24, minute)
      val request = OneTimeWorkRequestBuilder<DailyReminderWorker>()
        .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
        .addTag(UNIQUE_WORK_NAME)
        .build()

      workManager.enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        request,
      )
    }
  }
}

/**
 * Background [CoroutineWorker] that runs at the user's scheduled local time:
 * 1. If Daily Logging Reminder is enabled and no transaction has been recorded today, inserts
 *    a deduplicated reminder alert and posts a gentle system notification.
 * 2. If Goal Alerts are enabled, evaluates upcoming 7-day goal deadlines and notifies once per goal.
 * 3. Re-enqueues itself for tomorrow at the user's preferred time.
 */
class DailyReminderWorker(
  appContext: Context,
  workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val prefsRepo = UserPreferencesRepository(applicationContext)
    val prefs = prefsRepo.userPreferences.first()
    val db = BudgetWiseDatabase.getInstance(applicationContext)
    val repository = BudgetWiseRepository(db, prefsRepo)
    val today = currentEpochDay()

    if (prefs.dailyReminderEnabled) {
      val transactions = repository.transactions.first()
      val hasLoggedToday = transactions.any { it.dateEpochDay == today }
      if (!hasLoggedToday) {
        val candidate = FinancialAlertEvaluator.buildDailyLoggingReminderAlert(todayEpochDay = today)
        val persisted = repository.insertAlertIfNew(candidate)
        if (persisted != null) {
          BudgetWiseNotificationManager.postSystemNotification(applicationContext, persisted)
        }
      }
    }

    if (prefs.goalAlertsEnabled) {
      val goals = repository.savingsGoals.first()
      val deadlineAlerts = FinancialAlertEvaluator.evaluateGoalDeadlineAlerts(
        goals = goals,
        todayEpochDay = today,
        currencyCode = prefs.preferredCurrency,
      )
      deadlineAlerts.forEach { candidate ->
        val persisted = repository.insertAlertIfNew(candidate)
        if (persisted != null) {
          BudgetWiseNotificationManager.postSystemNotification(applicationContext, persisted)
        }
      }
    }

    DailyReminderScheduler.syncSchedule(
      context = applicationContext,
      dailyReminderEnabled = prefs.dailyReminderEnabled,
      goalAlertsEnabled = prefs.goalAlertsEnabled,
      hour24 = prefs.dailyReminderHour,
      minute = prefs.dailyReminderMinute,
    )

    return Result.success()
  }
}
