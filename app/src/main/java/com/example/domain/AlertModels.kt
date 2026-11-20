package com.example.domain

/** Classification of offline financial alerts surfaced in the Notification Center and Android tray. */
enum class FinancialAlertType {
  BUDGET_APPROACHING,
  BUDGET_EXCEEDED,
  GOAL_MILESTONE,
  GOAL_DEADLINE,
  DAILY_REMINDER,
}

/** Visual severity level for alert iconography and tinting. */
enum class AlertSeverity {
  INFO,
  POSITIVE,
  WARNING,
  CRITICAL,
}

/** Target destination navigated to when the user taps an alert in-app or from a system notification. */
enum class AlertDestination {
  BUDGETS,
  GOALS,
  ACTIVITY,
  ADD_TRANSACTION,
}

/** Allowed selectable budget warning thresholds before the 100% Exceeded alert fires. */
object AlertThresholdDefaults {
  const val DEFAULT_WARNING_THRESHOLD_PERCENT = 80
  val WARNING_THRESHOLD_OPTIONS: List<Int> = listOf(70, 75, 80, 85, 90)
  const val DEFAULT_REMINDER_HOUR = 20 // 8:00 PM
  const val DEFAULT_REMINDER_MINUTE = 0
  const val GOAL_DEADLINE_WINDOW_DAYS = 7L

  fun sanitizeThreshold(percent: Int): Int =
    if (percent in WARNING_THRESHOLD_OPTIONS) percent else DEFAULT_WARNING_THRESHOLD_PERCENT

  fun formatReminderTime(hour24: Int, minute: Int): String {
    val safeHour = hour24.coerceIn(0, 23)
    val safeMinute = minute.coerceIn(0, 59)
    val amPm = if (safeHour >= 12) "PM" else "AM"
    val hour12 = when {
      safeHour == 0 -> 12
      safeHour > 12 -> safeHour - 12
      else -> safeHour
    }
    return String.format("%d:%02d %s", hour12, safeMinute, amPm)
  }
}
