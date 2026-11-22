package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.FinancialAlertEntity
import com.example.domain.AlertDestination
import com.example.domain.FinancialAlertType

/**
 * Manages Android notification channels, permission checks, and dispatching local system
 * notifications with deep-link intents back into [MainActivity].
 */
object BudgetWiseNotificationManager {

  const val CHANNEL_BUDGET_ALERTS = "budgetwise_budget_alerts"
  const val CHANNEL_GOAL_ALERTS = "budgetwise_goal_alerts"
  const val CHANNEL_DAILY_REMINDER = "budgetwise_daily_reminder"

  const val EXTRA_ALERT_DESTINATION = "com.example.budgetwise.EXTRA_ALERT_DESTINATION"
  const val EXTRA_ALERT_ID = "com.example.budgetwise.EXTRA_ALERT_ID"

  fun ensureNotificationChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return

    val budgetChannel = NotificationChannel(
      CHANNEL_BUDGET_ALERTS,
      "Budget Threshold Alerts",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "Instant warnings when spending approaches or exceeds a monthly category budget."
    }

    val goalChannel = NotificationChannel(
      CHANNEL_GOAL_ALERTS,
      "Savings Goal Milestones & Deadlines",
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = "Milestone celebrations (50%, 75%, 100%) and 7-day target date reminders."
    }

    val reminderChannel = NotificationChannel(
      CHANNEL_DAILY_REMINDER,
      "Daily Logging Reminder",
      NotificationManager.IMPORTANCE_DEFAULT,
    ).apply {
      description = "Gentle opt-in daily reminder to log today's expenses."
    }

    manager.createNotificationChannels(listOf(budgetChannel, goalChannel, reminderChannel))
  }

  fun hasNotificationPermission(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
      if (!granted) return false
    }
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
  }

  fun channelIdFor(type: FinancialAlertType): String = when (type) {
    FinancialAlertType.BUDGET_APPROACHING,
    FinancialAlertType.BUDGET_EXCEEDED,
    -> CHANNEL_BUDGET_ALERTS
    FinancialAlertType.GOAL_MILESTONE,
    FinancialAlertType.GOAL_DEADLINE,
    -> CHANNEL_GOAL_ALERTS
    FinancialAlertType.DAILY_REMINDER -> CHANNEL_DAILY_REMINDER
  }

  fun buildContentIntent(
    context: Context,
    destination: AlertDestination,
    alertId: Long,
  ): Intent = Intent(context, MainActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
    putExtra(EXTRA_ALERT_DESTINATION, destination.name)
    putExtra(EXTRA_ALERT_ID, alertId)
  }

  /**
   * Dispatches an Android system notification for [alert] if OS permission is granted.
   * Returns `true` when posted, or `false` if skipped due to missing permission.
   */
  fun postSystemNotification(context: Context, alert: FinancialAlertEntity): Boolean {
    ensureNotificationChannels(context)
    if (!hasNotificationPermission(context)) return false

    val channelId = channelIdFor(alert.type)
    val notificationId = if (alert.id > 0L) {
      (alert.id % Int.MAX_VALUE).toInt()
    } else {
      alert.dedupeKey.hashCode()
    }

    val tapIntent = buildContentIntent(context, alert.targetDestination, alert.id)
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      tapIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    val priority = when (alert.type) {
      FinancialAlertType.BUDGET_EXCEEDED,
      FinancialAlertType.BUDGET_APPROACHING,
      -> NotificationCompat.PRIORITY_HIGH
      else -> NotificationCompat.PRIORITY_DEFAULT
    }

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification_budgetwise)
      .setContentTitle(alert.title)
      .setContentText(alert.message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(alert.message))
      .setPriority(priority)
      .setCategory(NotificationCompat.CATEGORY_REMINDER)
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .build()

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    return try {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
      true
    } catch (_: SecurityException) {
      false
    } catch (_: Exception) {
      false
    }
  }
}
