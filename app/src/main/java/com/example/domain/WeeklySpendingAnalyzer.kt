package com.example.domain

import com.example.data.local.entity.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode

data class WeeklyCategoryDelta(
  val category: String,
  val thisWeekAmount: BigDecimal,
  val lastWeekAmount: BigDecimal,
  val deltaAmount: BigDecimal,
  val percentChange: BigDecimal?,
  val transactionCountThisWeek: Int,
)

data class WeeklySpendingComparison(
  val thisWeekStartEpochDay: Long,
  val thisWeekEndEpochDay: Long,
  val lastWeekStartEpochDay: Long,
  val lastWeekEndEpochDay: Long,
  val isLiveCurrentWeek: Boolean,
  val thisWeekLabel: String,
  val lastWeekLabel: String,
  val windowLabel: String,
  val thisWeekTotal: BigDecimal,
  val lastWeekTotal: BigDecimal,
  val totalDelta: BigDecimal,
  val totalPercentChange: BigDecimal?,
  val thisWeekTxCount: Int,
  val lastWeekTxCount: Int,
  val categoryDeltas: List<WeeklyCategoryDelta>,
  val topIncreaseCategory: WeeklyCategoryDelta?,
  val hasAnyExpenses: Boolean,
  val dataFingerprint: String,
)

/**
 * Computes rolling 7-day week-over-week expense comparisons and category deltas.
 *
 * When no expenses exist in the current 7-day window ending at [referenceEpochDay], the analyzer
 * anchors the comparison window to the most recent recorded expense day so historical ledgers
 * still produce meaningful week-over-week comparisons.
 */
object WeeklySpendingAnalyzer {

  private val HUNDRED = BigDecimal("100")

  /** Analyzes expense transactions across two adjacent 7-day windows and returns sorted category deltas. */
  fun analyze(
    transactions: List<TransactionEntity>,
    currencyCode: String,
    referenceEpochDay: Long = currentEpochDay(),
  ): WeeklySpendingComparison {
    val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (expenses.isEmpty()) {
      val thisStart = referenceEpochDay - 6L
      val lastEnd = thisStart - 1L
      val lastStart = lastEnd - 6L
      val thisLabel = formatRange(thisStart, referenceEpochDay)
      val lastLabel = formatRange(lastStart, lastEnd)
      return WeeklySpendingComparison(
        thisWeekStartEpochDay = thisStart,
        thisWeekEndEpochDay = referenceEpochDay,
        lastWeekStartEpochDay = lastStart,
        lastWeekEndEpochDay = lastEnd,
        isLiveCurrentWeek = true,
        thisWeekLabel = thisLabel,
        lastWeekLabel = lastLabel,
        windowLabel = "$thisLabel vs $lastLabel",
        thisWeekTotal = BigDecimal.ZERO.setScale(2),
        lastWeekTotal = BigDecimal.ZERO.setScale(2),
        totalDelta = BigDecimal.ZERO.setScale(2),
        totalPercentChange = null,
        thisWeekTxCount = 0,
        lastWeekTxCount = 0,
        categoryDeltas = emptyList(),
        topIncreaseCategory = null,
        hasAnyExpenses = false,
        dataFingerprint = "empty:$currencyCode",
      )
    }

    val recentStart = referenceEpochDay - 6L
    val hasRecentActivity = expenses.any { it.dateEpochDay in recentStart..referenceEpochDay }
    val latestExpenseDay = expenses.maxOf { it.dateEpochDay }
    val thisWeekEnd = if (hasRecentActivity) referenceEpochDay else latestExpenseDay
    val thisWeekStart = thisWeekEnd - 6L
    val lastWeekEnd = thisWeekStart - 1L
    val lastWeekStart = lastWeekEnd - 6L

    val thisWeekExpenses = expenses.filter { it.dateEpochDay in thisWeekStart..thisWeekEnd }
    val lastWeekExpenses = expenses.filter { it.dateEpochDay in lastWeekStart..lastWeekEnd }

    val thisWeekCentavos = thisWeekExpenses.sumOf { it.amountCentavos }
    val lastWeekCentavos = lastWeekExpenses.sumOf { it.amountCentavos }
    val thisWeekTotal = FinanceCalculator.centavosToBigDecimal(thisWeekCentavos)
    val lastWeekTotal = FinanceCalculator.centavosToBigDecimal(lastWeekCentavos)
    val totalDelta = thisWeekTotal.subtract(lastWeekTotal).setScale(2, RoundingMode.HALF_UP)
    val totalPercentChange = computePercentChange(thisWeekTotal, lastWeekTotal)

    val thisByCat = thisWeekExpenses.groupBy { it.category }
    val lastByCat = lastWeekExpenses.groupBy { it.category }
    val allCategories = (thisByCat.keys + lastByCat.keys).distinct()

    val categoryDeltas = allCategories.map { category ->
      val thisItems = thisByCat[category].orEmpty()
      val lastItems = lastByCat[category].orEmpty()
      val thisAmt = FinanceCalculator.centavosToBigDecimal(thisItems.sumOf { it.amountCentavos })
      val lastAmt = FinanceCalculator.centavosToBigDecimal(lastItems.sumOf { it.amountCentavos })
      val delta = thisAmt.subtract(lastAmt).setScale(2, RoundingMode.HALF_UP)
      WeeklyCategoryDelta(
        category = category,
        thisWeekAmount = thisAmt,
        lastWeekAmount = lastAmt,
        deltaAmount = delta,
        percentChange = computePercentChange(thisAmt, lastAmt),
        transactionCountThisWeek = thisItems.size,
      )
    }.sortedWith(
      compareByDescending<WeeklyCategoryDelta> { it.deltaAmount }
        .thenByDescending { it.thisWeekAmount }
    )

    val topIncrease = categoryDeltas.firstOrNull { it.deltaAmount > BigDecimal.ZERO }
      ?: categoryDeltas.firstOrNull()

    val thisLabel = formatRange(thisWeekStart, thisWeekEnd)
    val lastLabel = formatRange(lastWeekStart, lastWeekEnd)

    val fingerprint = buildString {
      append(currencyCode).append('|')
      append(thisWeekStart).append('-').append(thisWeekEnd).append('|')
      append(thisWeekCentavos).append('/').append(lastWeekCentavos).append('|')
      categoryDeltas.forEach { d ->
        append(d.category).append(':')
          .append(d.thisWeekAmount.toPlainString()).append(',')
          .append(d.lastWeekAmount.toPlainString()).append(';')
      }
    }

    return WeeklySpendingComparison(
      thisWeekStartEpochDay = thisWeekStart,
      thisWeekEndEpochDay = thisWeekEnd,
      lastWeekStartEpochDay = lastWeekStart,
      lastWeekEndEpochDay = lastWeekEnd,
      isLiveCurrentWeek = thisWeekEnd == referenceEpochDay,
      thisWeekLabel = thisLabel,
      lastWeekLabel = lastLabel,
      windowLabel = "$thisLabel vs $lastLabel",
      thisWeekTotal = thisWeekTotal,
      lastWeekTotal = lastWeekTotal,
      totalDelta = totalDelta,
      totalPercentChange = totalPercentChange,
      thisWeekTxCount = thisWeekExpenses.size,
      lastWeekTxCount = lastWeekExpenses.size,
      categoryDeltas = categoryDeltas,
      topIncreaseCategory = topIncrease,
      hasAnyExpenses = true,
      dataFingerprint = fingerprint,
    )
  }

  fun buildGeminiPrompt(
    comparison: WeeklySpendingComparison,
    budgetSummaries: List<BudgetSummary>,
    goalSummaries: List<SavingsGoalSummary>,
    currencyCode: String,
    privacyModeEnabled: Boolean,
  ): String {
    val currency = CurrencyOption.fromCode(currencyCode)
    fun fmt(amount: BigDecimal): String = FinanceCalculator.formatCurrency(amount, currencyCode)

    return buildString {
      appendLine("You are BudgetWise's personal financial insight analyst.")
      appendLine("Analyze the user's weekly spending comparison and return concise, encouraging, practical suggestions.")
      appendLine("Currency: ${currency.displayName} (${currency.code}, symbol ${currency.symbol}).")
      if (privacyModeEnabled) {
        appendLine("PRIVACY MODE IS ON: Do NOT include exact monetary numbers or currency amounts in your text output. Express differences strictly using percentages, counts, and category names.")
      } else {
        appendLine("Format any currency references using ${currency.symbol} (e.g., ${fmt(BigDecimal("1200"))}).")
      }
      appendLine()
      appendLine("WEEKLY WINDOW:")
      appendLine("- This Week (${comparison.thisWeekLabel}): ${fmt(comparison.thisWeekTotal)} across ${comparison.thisWeekTxCount} expense transactions")
      appendLine("- Last Week (${comparison.lastWeekLabel}): ${fmt(comparison.lastWeekTotal)} across ${comparison.lastWeekTxCount} expense transactions")
      val pctText = comparison.totalPercentChange?.let { "${it.toPlainString()}%" } ?: "N/A (no prior week baseline)"
      appendLine("- Week-over-Week Change: ${fmt(comparison.totalDelta)} ($pctText)")
      appendLine()
      appendLine("CATEGORY BREAKDOWN (This Week vs Last Week):")
      if (comparison.categoryDeltas.isEmpty()) {
        appendLine("- No categorized expenses recorded in these two weeks.")
      } else {
        comparison.categoryDeltas.take(8).forEach { cat ->
          val catPct = cat.percentChange?.let { "${if (it > BigDecimal.ZERO) "+" else ""}${it.toPlainString()}%" } ?: "new this week"
          appendLine("- ${cat.category}: This week ${fmt(cat.thisWeekAmount)} (${cat.transactionCountThisWeek} tx) vs Last week ${fmt(cat.lastWeekAmount)} | Change: $catPct")
        }
      }
      appendLine()
      if (budgetSummaries.isNotEmpty()) {
        appendLine("ACTIVE MONTHLY BUDGETS:")
        budgetSummaries.take(6).forEach { b ->
          appendLine("- ${b.category}: ${b.utilizationPercent.toPlainString()}% used (${fmt(b.spentAmount)} of ${fmt(b.limitAmount)}, status=${b.healthStatus.label})")
        }
        appendLine()
      }
      val activeGoals = goalSummaries.filter { it.status == GoalStatus.ACTIVE }
      if (activeGoals.isNotEmpty()) {
        appendLine("ACTIVE SAVINGS GOALS:")
        activeGoals.take(4).forEach { g ->
          appendLine("- ${g.name}: ${g.completionPercent.toPlainString()}% complete (${fmt(g.savedAmount)} / ${fmt(g.targetAmount)})")
        }
        appendLine()
      }
      appendLine("INSTRUCTIONS:")
      appendLine("1. Write a punchy 'headline' (3-6 words).")
      appendLine("2. Write 'weeklySummary' as 1-2 natural-language sentences highlighting the biggest week-over-week shift (for example: \"You've spent 20% more on dining out this week compared to last week—consider capping your lunch budget to save more.\") and keep it directly grounded in the numbers above.")
      appendLine("3. Provide 2-3 concrete, responsive 'recommendations' tailored to the user's top spending categories, tightest budgets, or active savings goals.")
      appendLine("4. Avoid generic fluff; keep each 'suggestedAction' short enough to fit on a mobile chip or action banner.")
    }
  }

  private fun computePercentChange(current: BigDecimal, previous: BigDecimal): BigDecimal? {
    if (previous <= BigDecimal.ZERO) return null
    return current.subtract(previous)
      .multiply(HUNDRED)
      .divide(previous, 1, RoundingMode.HALF_UP)
  }

  private fun formatRange(startEpochDay: Long, endEpochDay: Long): String {
    val startText = formatEpochDay(startEpochDay, "MMM d")
    val endText = formatEpochDay(endEpochDay, "MMM d")
    return if (startText == endText) startText else "$startText–$endText"
  }
}
