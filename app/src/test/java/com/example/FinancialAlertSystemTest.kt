package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.AlertDestination
import com.example.domain.AlertSeverity
import com.example.domain.AlertThresholdDefaults
import com.example.domain.FinancialAlertEvaluator
import com.example.domain.FinancialAlertType
import com.example.domain.GoalStatus
import com.example.domain.YearMonthKey
import com.example.domain.epochDayFromDate
import com.example.notifications.BudgetWiseNotificationManager
import com.example.notifications.DailyReminderScheduler
import java.util.Calendar
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialAlertSystemTest {

  private val nov2026 = YearMonthKey(2026, 11)

  @Test
  fun `budget alert evaluator respects custom warning threshold and 100 percent exceeded trigger`() {
    // 75% spent with 80% threshold -> no alert
    val belowDefault = FinancialAlertEvaluator.evaluateBudgetAlert(
      category = "Food and Dining",
      yearMonth = nov2026,
      spentCentavos = 375000L,
      limitCentavos = 500000L,
      warningThresholdPercent = 80,
      currencyCode = "PHP",
    )
    assertNull(belowDefault)

    // Same 75% spent with custom 75% threshold -> BUDGET_APPROACHING
    val approachingCustom = FinancialAlertEvaluator.evaluateBudgetAlert(
      category = "Food and Dining",
      yearMonth = nov2026,
      spentCentavos = 375000L,
      limitCentavos = 500000L,
      warningThresholdPercent = 75,
      currencyCode = "PHP",
    )
    assertNotNull(approachingCustom)
    assertEquals(FinancialAlertType.BUDGET_APPROACHING, approachingCustom!!.type)
    assertEquals(AlertSeverity.WARNING, approachingCustom.severity)
    assertEquals(AlertDestination.BUDGETS, approachingCustom.targetDestination)
    assertTrue(approachingCustom.message.contains("November"))

    // 105% spent -> BUDGET_EXCEEDED regardless of warning threshold
    val exceeded = FinancialAlertEvaluator.evaluateBudgetAlert(
      category = "Food and Dining",
      yearMonth = nov2026,
      spentCentavos = 525000L,
      limitCentavos = 500000L,
      warningThresholdPercent = 90,
      currencyCode = "PHP",
    )
    assertNotNull(exceeded)
    assertEquals(FinancialAlertType.BUDGET_EXCEEDED, exceeded!!.type)
    assertEquals(AlertSeverity.CRITICAL, exceeded.severity)
    assertTrue(exceeded.title.contains("Exceeded"))
  }

  @Test
  fun `goal milestone evaluator celebrates 50, 75, and 100 percent crossings without duplicate lower milestones`() {
    // Crossing 50%
    val halfAlert = FinancialAlertEvaluator.evaluateGoalMilestoneAlert(
      goalId = 7L,
      goalName = "Emergency Fund",
      previousSavedCentavos = 400000L,
      newSavedCentavos = 550000L,
      targetCentavos = 1000000L,
      currencyCode = "PHP",
    )
    assertNotNull(halfAlert)
    assertEquals("goal_milestone_7_50", halfAlert!!.dedupeKey)
    assertEquals(AlertSeverity.POSITIVE, halfAlert.severity)

    // Already above 50%, not yet 75% -> null
    val noNewMilestone = FinancialAlertEvaluator.evaluateGoalMilestoneAlert(
      goalId = 7L,
      goalName = "Emergency Fund",
      previousSavedCentavos = 550000L,
      newSavedCentavos = 700000L,
      targetCentavos = 1000000L,
      currencyCode = "PHP",
    )
    assertNull(noNewMilestone)

    // Jumping from 60% straight to 100% -> triggers 100% achieved
    val achievedAlert = FinancialAlertEvaluator.evaluateGoalMilestoneAlert(
      goalId = 7L,
      goalName = "Emergency Fund",
      previousSavedCentavos = 600000L,
      newSavedCentavos = 1000000L,
      targetCentavos = 1000000L,
      currencyCode = "PHP",
    )
    assertNotNull(achievedAlert)
    assertEquals("goal_milestone_7_100", achievedAlert!!.dedupeKey)
    assertTrue(achievedAlert.title.contains("Goal Achieved"))

    // Withdrawal -> null
    val withdrawal = FinancialAlertEvaluator.evaluateGoalMilestoneAlert(
      goalId = 7L,
      goalName = "Emergency Fund",
      previousSavedCentavos = 800000L,
      newSavedCentavos = 600000L,
      targetCentavos = 1000000L,
      currencyCode = "PHP",
    )
    assertNull(withdrawal)
  }

  @Test
  fun `goal deadline evaluator alerts only for active incomplete goals within 7 days`() {
    val today = epochDayFromDate(2026, 11, 19)
    val goals = listOf(
      SavingsGoalEntity(
        id = 1L,
        name = "Laptop Upgrade",
        targetCentavos = 5000000L,
        currentSavedCentavos = 3200000L,
        targetDateEpochDay = today + 5L,
        createdDateEpochDay = today - 30L,
        status = GoalStatus.ACTIVE,
      ),
      SavingsGoalEntity(
        id = 2L,
        name = "Far Future Trip",
        targetCentavos = 5000000L,
        currentSavedCentavos = 1000000L,
        targetDateEpochDay = today + 30L,
        createdDateEpochDay = today - 10L,
        status = GoalStatus.ACTIVE,
      ),
      SavingsGoalEntity(
        id = 3L,
        name = "Already Finished",
        targetCentavos = 2000000L,
        currentSavedCentavos = 2000000L,
        targetDateEpochDay = today + 2L,
        createdDateEpochDay = today - 20L,
        status = GoalStatus.COMPLETED,
      ),
      SavingsGoalEntity(
        id = 4L,
        name = "Paused Goal",
        targetCentavos = 2000000L,
        currentSavedCentavos = 500000L,
        targetDateEpochDay = today + 2L,
        createdDateEpochDay = today - 20L,
        status = GoalStatus.PAUSED,
      ),
    )

    val deadlineAlerts = FinancialAlertEvaluator.evaluateGoalDeadlineAlerts(
      goals = goals,
      todayEpochDay = today,
      currencyCode = "PHP",
    )

    assertEquals(1, deadlineAlerts.size)
    assertEquals(FinancialAlertType.GOAL_DEADLINE, deadlineAlerts[0].type)
    assertTrue(deadlineAlerts[0].title.contains("5 Days"))
    assertEquals(AlertDestination.GOALS, deadlineAlerts[0].targetDestination)
  }

  @Test
  fun `daily reminder scheduler computes future local delay for same day and next day`() {
    val morning = Calendar.getInstance().apply {
      set(2026, Calendar.NOVEMBER, 19, 10, 0, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    // 8:00 PM same day = 10 hours later
    val sameDayDelay = DailyReminderScheduler.computeDelayMillisUntilNextRun(
      hour24 = 20,
      minute = 0,
      nowMillis = morning,
    )
    assertEquals(10 * 60 * 60 * 1000L, sameDayDelay)

    // 8:00 AM already passed today -> 22 hours later (next morning)
    val nextDayDelay = DailyReminderScheduler.computeDelayMillisUntilNextRun(
      hour24 = 8,
      minute = 0,
      nowMillis = morning,
    )
    assertEquals(22 * 60 * 60 * 1000L, nextDayDelay)

    assertEquals("8:00 PM", AlertThresholdDefaults.formatReminderTime(20, 0))
    assertEquals("12:05 AM", AlertThresholdDefaults.formatReminderTime(0, 5))
  }

  @Test
  fun `notification manager routes alert channels and builds deep link content intent`() {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    assertEquals(
      BudgetWiseNotificationManager.CHANNEL_BUDGET_ALERTS,
      BudgetWiseNotificationManager.channelIdFor(FinancialAlertType.BUDGET_EXCEEDED),
    )
    assertEquals(
      BudgetWiseNotificationManager.CHANNEL_GOAL_ALERTS,
      BudgetWiseNotificationManager.channelIdFor(FinancialAlertType.GOAL_MILESTONE),
    )
    assertEquals(
      BudgetWiseNotificationManager.CHANNEL_DAILY_REMINDER,
      BudgetWiseNotificationManager.channelIdFor(FinancialAlertType.DAILY_REMINDER),
    )

    val intent = BudgetWiseNotificationManager.buildContentIntent(
      context = context,
      destination = AlertDestination.BUDGETS,
      alertId = 42L,
    )
    assertEquals(
      AlertDestination.BUDGETS.name,
      intent.getStringExtra(BudgetWiseNotificationManager.EXTRA_ALERT_DESTINATION),
    )
    assertEquals(42L, intent.getLongExtra(BudgetWiseNotificationManager.EXTRA_ALERT_ID, -1L))
  }

  @Test
  fun `financial alert dao deduplicates alerts, marks read, and clears prefix on budget raise`() = runTest {
    val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    val db = Room.inMemoryDatabaseBuilder(context, BudgetWiseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val dao = db.financialAlertDao()
      val alert = FinancialAlertEvaluator.evaluateBudgetAlert(
        category = "Transportation",
        yearMonth = nov2026,
        spentCentavos = 180000L,
        limitCentavos = 200000L,
        warningThresholdPercent = 80,
        currencyCode = "PHP",
      )!!

      val firstId = dao.insertIgnore(alert)
      val duplicateId = dao.insertIgnore(alert)
      assertTrue("First insert should succeed", firstId > 0L)
      assertEquals("Duplicate dedupeKey should be ignored", -1L, duplicateId)

      val stored = dao.observeAllAlerts().first()
      assertEquals(1, stored.size)
      assertEquals(false, stored[0].isRead)

      dao.markAsRead(firstId)
      assertEquals(true, dao.observeAllAlerts().first()[0].isRead)

      dao.deleteByDedupeKeyPrefix(
        FinancialAlertEvaluator.budgetDedupePrefix("Transportation", nov2026)
      )
      assertTrue(dao.observeAllAlerts().first().isEmpty())
    } finally {
      db.close()
    }
  }
}
