package com.example.domain

import com.example.data.local.entity.FinancialAlertEntity
import com.example.data.local.entity.SavingsGoalEntity
import java.math.BigDecimal

/**
 * Pure, deterministic evaluator for BudgetWise financial alerts.
 *
 * Generates deduplicated [FinancialAlertEntity] records for:
 * 1. Monthly category budget warnings (custom threshold %) and exceeded (> 100%) alerts.
 * 2. Savings goal milestone celebrations (50%, 75%, 100%).
 * 3. Upcoming savings goal deadline reminders (within 7 days of target date).
 * 4. Opt-in daily expense logging reminders.
 */
object FinancialAlertEvaluator {

  private val GOAL_MILESTONES = listOf(100, 75, 50)

  fun budgetDedupePrefix(category: String, yearMonth: YearMonthKey): String =
    "budget_%_${category}_${yearMonth.year}_${yearMonth.month}%"

  fun evaluateBudgetAlert(
    category: String,
    yearMonth: YearMonthKey,
    spentCentavos: Long,
    limitCentavos: Long,
    warningThresholdPercent: Int,
    currencyCode: String,
    nowMillis: Long = System.currentTimeMillis(),
  ): FinancialAlertEntity? {
    if (limitCentavos <= 0L || spentCentavos <= 0L) return null

    val spent = FinanceCalculator.centavosToBigDecimal(spentCentavos)
    val limit = FinanceCalculator.centavosToBigDecimal(limitCentavos)
    val utilization = FinanceCalculator.calculateBudgetUtilizationPercent(spent, limit)
    val shortCategory = compactCategoryLabel(category)
    val spentFormatted = FinanceCalculator.formatCurrency(spent, currencyCode)
    val limitFormatted = FinanceCalculator.formatCurrency(limit, currencyCode)
    val safeThreshold = AlertThresholdDefaults.sanitizeThreshold(warningThresholdPercent)

    if (spentCentavos > limitCentavos) {
      val overAmount = FinanceCalculator.centavosToBigDecimal(spentCentavos - limitCentavos)
      val overFormatted = FinanceCalculator.formatCurrency(overAmount, currencyCode)
      return FinancialAlertEntity(
        dedupeKey = "budget_exceeded_${category}_${yearMonth.year}_${yearMonth.month}",
        type = FinancialAlertType.BUDGET_EXCEEDED,
        severity = AlertSeverity.CRITICAL,
        title = "$shortCategory Budget Exceeded",
        message = "You've spent $spentFormatted of your $limitFormatted ${yearMonth.monthNameOnly()} limit ($overFormatted over · ${utilization.toPlainString()}%).",
        targetDestination = AlertDestination.BUDGETS,
        isRead = false,
        createdAtMillis = nowMillis,
      )
    }

    if (utilization >= BigDecimal(safeThreshold)) {
      val remaining = FinanceCalculator.centavosToBigDecimal((limitCentavos - spentCentavos).coerceAtLeast(0L))
      val remainingFormatted = FinanceCalculator.formatCurrency(remaining, currencyCode)
      return FinancialAlertEntity(
        dedupeKey = "budget_warn_${category}_${yearMonth.year}_${yearMonth.month}_$safeThreshold",
        type = FinancialAlertType.BUDGET_APPROACHING,
        severity = AlertSeverity.WARNING,
        title = "$shortCategory Approaching Limit",
        message = "You've used ${utilization.toPlainString()}% ($spentFormatted of $limitFormatted) of your ${yearMonth.monthNameOnly()} budget — $remainingFormatted left.",
        targetDestination = AlertDestination.BUDGETS,
        isRead = false,
        createdAtMillis = nowMillis,
      )
    }

    return null
  }

  fun evaluateGoalMilestoneAlert(
    goalId: Long,
    goalName: String,
    previousSavedCentavos: Long,
    newSavedCentavos: Long,
    targetCentavos: Long,
    currencyCode: String,
    nowMillis: Long = System.currentTimeMillis(),
  ): FinancialAlertEntity? {
    if (targetCentavos <= 0L || newSavedCentavos <= previousSavedCentavos) return null

    val prevPercent = FinanceCalculator.calculateBudgetUtilizationPercent(
      actualSpent = FinanceCalculator.centavosToBigDecimal(previousSavedCentavos),
      limit = FinanceCalculator.centavosToBigDecimal(targetCentavos),
    )
    val newPercent = FinanceCalculator.calculateBudgetUtilizationPercent(
      actualSpent = FinanceCalculator.centavosToBigDecimal(newSavedCentavos),
      limit = FinanceCalculator.centavosToBigDecimal(targetCentavos),
    )

    val crossedMilestone = GOAL_MILESTONES.firstOrNull { milestone ->
      val threshold = BigDecimal(milestone)
      newPercent >= threshold && prevPercent < threshold
    } ?: return null

    val savedFormatted = FinanceCalculator.formatCentavos(newSavedCentavos, currencyCode)
    val targetFormatted = FinanceCalculator.formatCentavos(targetCentavos, currencyCode)

    val (title, message) = when (crossedMilestone) {
      100 -> "Goal Achieved: $goalName" to
        "Congratulations! You reached your $targetFormatted target for \"$goalName\"."
      75 -> "75% Milestone: $goalName" to
        "Almost there! You've saved $savedFormatted of $targetFormatted toward \"$goalName\"."
      else -> "Halfway There: $goalName" to
        "Great momentum! You've saved $savedFormatted of $targetFormatted (${newPercent.toPlainString()}%) on \"$goalName\"."
    }

    return FinancialAlertEntity(
      dedupeKey = "goal_milestone_${goalId}_$crossedMilestone",
      type = FinancialAlertType.GOAL_MILESTONE,
      severity = AlertSeverity.POSITIVE,
      title = title,
      message = message,
      targetDestination = AlertDestination.GOALS,
      isRead = false,
      createdAtMillis = nowMillis,
    )
  }

  fun evaluateGoalDeadlineAlerts(
    goals: List<SavingsGoalEntity>,
    todayEpochDay: Long = currentEpochDay(),
    currencyCode: String,
    nowMillis: Long = System.currentTimeMillis(),
  ): List<FinancialAlertEntity> {
    return goals.mapNotNull { goal ->
      if (goal.status != GoalStatus.ACTIVE) return@mapNotNull null
      if (goal.currentSavedCentavos >= goal.targetCentavos) return@mapNotNull null

      val daysLeft = goal.targetDateEpochDay - todayEpochDay
      if (daysLeft !in 0L..AlertThresholdDefaults.GOAL_DEADLINE_WINDOW_DAYS) return@mapNotNull null

      val remainingCentavos = (goal.targetCentavos - goal.currentSavedCentavos).coerceAtLeast(0L)
      val remainingFormatted = FinanceCalculator.formatCentavos(remainingCentavos, currencyCode)
      val targetFormatted = FinanceCalculator.formatCentavos(goal.targetCentavos, currencyCode)
      val targetDateLabel = formatEpochDay(goal.targetDateEpochDay)

      val title = when (daysLeft) {
        0L -> "Goal Due Today: ${goal.name}"
        1L -> "Goal Deadline Tomorrow: ${goal.name}"
        else -> "Goal Deadline in $daysLeft Days"
      }

      FinancialAlertEntity(
        dedupeKey = "goal_deadline_${goal.id}_${goal.targetDateEpochDay}",
        type = FinancialAlertType.GOAL_DEADLINE,
        severity = AlertSeverity.WARNING,
        title = title,
        message = "\"${goal.name}\" still needs $remainingFormatted to reach $targetFormatted by $targetDateLabel.",
        targetDestination = AlertDestination.GOALS,
        isRead = false,
        createdAtMillis = nowMillis,
      )
    }
  }

  fun buildDailyLoggingReminderAlert(
    todayEpochDay: Long = currentEpochDay(),
    nowMillis: Long = System.currentTimeMillis(),
  ): FinancialAlertEntity =
    FinancialAlertEntity(
      dedupeKey = "daily_reminder_${formatEpochDay(todayEpochDay, "yyyy-MM-dd")}",
      type = FinancialAlertType.DAILY_REMINDER,
      severity = AlertSeverity.INFO,
      title = "Daily BudgetWise Check-In",
      message = "Take 10 seconds to log today's expenses to keep your BudgetWise cash flow accurate.",
      targetDestination = AlertDestination.ADD_TRANSACTION,
      isRead = false,
      createdAtMillis = nowMillis,
    )
}
