package com.example.domain

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Pure domain calculation and formatting engine for BudgetWise.
 *
 * All monetary amounts are persisted as integer minor units (centavos) and converted to
 * [BigDecimal] with [RoundingMode.HALF_UP] for deterministic financial arithmetic.
 */
object FinanceCalculator {

  private val HUNDRED = BigDecimal("100")
  private val MAX_SAFE_CENTAVOS = BigDecimal("99999999999999") // Caps parsed inputs below 1 trillion currency units

  /** Converts stored integer centavos into a scale-2 [BigDecimal] currency amount. */
  fun centavosToBigDecimal(centavos: Long): BigDecimal =
    BigDecimal.valueOf(centavos, 2)

  /**
   * Parses a user-entered monetary string into strictly positive integer centavos,
   * returning `null` for blank, non-numeric, non-positive, or out-of-range inputs.
   */
  fun parseAmountToCentavos(rawInput: String): Long? {
    val sanitized = rawInput.trim().replace(",", "")
    if (sanitized.isEmpty()) return null
    val bd = sanitized.toBigDecimalOrNull() ?: return null
    if (bd <= BigDecimal.ZERO) return null
    val scaledCentavos = bd.setScale(2, RoundingMode.HALF_UP).multiply(HUNDRED)
    if (scaledCentavos <= BigDecimal.ZERO || scaledCentavos > MAX_SAFE_CENTAVOS) return null
    return try {
      scaledCentavos.longValueExact()
    } catch (_: ArithmeticException) {
      null
    }
  }

  fun bigDecimalToCentavos(amount: BigDecimal): Long =
    amount.setScale(2, RoundingMode.HALF_UP).multiply(HUNDRED).longValueExact()

  fun filterTransactionsByMonth(
    transactions: List<TransactionEntity>,
    yearMonth: YearMonthKey,
  ): List<TransactionEntity> =
    transactions.filter { YearMonthKey.fromEpochDay(it.dateEpochDay) == yearMonth }

  fun calculateTotalIncome(
    transactions: List<TransactionEntity>,
    yearMonth: YearMonthKey,
  ): BigDecimal {
    val totalCentavos = filterTransactionsByMonth(transactions, yearMonth)
      .asSequence()
      .filter { it.type == TransactionType.INCOME }
      .sumOf { it.amountCentavos }
    return centavosToBigDecimal(totalCentavos)
  }

  fun calculateTotalExpenses(
    transactions: List<TransactionEntity>,
    yearMonth: YearMonthKey,
  ): BigDecimal {
    val totalCentavos = filterTransactionsByMonth(transactions, yearMonth)
      .asSequence()
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { it.amountCentavos }
    return centavosToBigDecimal(totalCentavos)
  }

  fun calculateAllTimeTotalIncome(transactions: List<TransactionEntity>): BigDecimal {
    val totalCentavos = transactions
      .asSequence()
      .filter { it.type == TransactionType.INCOME }
      .sumOf { it.amountCentavos }
    return centavosToBigDecimal(totalCentavos)
  }

  fun calculateAllTimeTotalExpenses(transactions: List<TransactionEntity>): BigDecimal {
    val totalCentavos = transactions
      .asSequence()
      .filter { it.type == TransactionType.EXPENSE }
      .sumOf { it.amountCentavos }
    return centavosToBigDecimal(totalCentavos)
  }

  fun calculateTotalReservedSavings(goals: List<SavingsGoalEntity>): BigDecimal {
    val totalCentavos = goals.sumOf { it.currentSavedCentavos }
    return centavosToBigDecimal(totalCentavos)
  }

  fun calculateNetCashFlow(income: BigDecimal, expenses: BigDecimal): BigDecimal =
    income.subtract(expenses).setScale(2, RoundingMode.HALF_UP)

  fun calculateRemainingBudget(limit: BigDecimal, actualSpent: BigDecimal): BigDecimal =
    limit.subtract(actualSpent).setScale(2, RoundingMode.HALF_UP)

  /**
   * Calculates the percentage of [limit] consumed by [actualSpent] rounded to 1 decimal place.
   * Returns `0.0` when [limit] is zero or negative.
   */
  fun calculateBudgetUtilizationPercent(actualSpent: BigDecimal, limit: BigDecimal): BigDecimal {
    if (limit <= BigDecimal.ZERO) return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
    return actualSpent
      .multiply(HUNDRED)
      .divide(limit, 1, RoundingMode.HALF_UP)
  }

  /**
   * Computes monthly or all-time savings rate as `(income - expenses) / income * 100`,
   * clamped to `[0.0, 100.0]` so deficit periods report a `0.0%` savings rate.
   */
  fun calculateSavingsRatePercent(income: BigDecimal, expenses: BigDecimal): BigDecimal {
    if (income <= BigDecimal.ZERO || expenses >= income) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
    }
    val net = income.subtract(expenses)
    return net
      .multiply(HUNDRED)
      .divide(income, 1, RoundingMode.HALF_UP)
      .coerceIn(BigDecimal.ZERO.setScale(1), BigDecimal("100.0"))
  }

  /**
   * Maps a budget utilization percentage to a four-tier health status:
   * `< 70%` Normal, `70%–89.9%` Approaching, `90%–99.9%` Near Limit, `>= 100%` Exceeded.
   */
  fun determineBudgetHealth(utilizationPercent: BigDecimal): BudgetHealthStatus {
    val pct = utilizationPercent.toDouble()
    return when {
      pct >= 100.0 -> BudgetHealthStatus.EXCEEDED
      pct >= 90.0 -> BudgetHealthStatus.NEAR_LIMIT
      pct >= 70.0 -> BudgetHealthStatus.APPROACHING
      else -> BudgetHealthStatus.NORMAL
    }
  }

  fun buildBudgetSummaries(
    budgets: List<BudgetEntity>,
    transactions: List<TransactionEntity>,
    yearMonth: YearMonthKey,
  ): List<BudgetSummary> {
    val monthExpenses = filterTransactionsByMonth(transactions, yearMonth)
      .filter { it.type == TransactionType.EXPENSE }
      .groupBy { it.category }

    return budgets
      .filter { it.year == yearMonth.year && it.month == yearMonth.month }
      .map { budget ->
        val limit = centavosToBigDecimal(budget.limitCentavos)
        val spentCentavos = monthExpenses[budget.category]?.sumOf { it.amountCentavos } ?: 0L
        val spent = centavosToBigDecimal(spentCentavos)
        val remaining = calculateRemainingBudget(limit, spent)
        val utilization = calculateBudgetUtilizationPercent(spent, limit)
        BudgetSummary(
          budgetId = budget.id,
          category = budget.category,
          yearMonth = yearMonth,
          limitAmount = limit,
          spentAmount = spent,
          remainingAmount = remaining,
          utilizationPercent = utilization,
          healthStatus = determineBudgetHealth(utilization),
          isSampleData = budget.isSampleData,
        )
      }
      .sortedByDescending { it.utilizationPercent }
  }

  fun buildGoalSummaries(goals: List<SavingsGoalEntity>): List<SavingsGoalSummary> =
    goals.map { goal ->
      val target = centavosToBigDecimal(goal.targetCentavos)
      val saved = centavosToBigDecimal(goal.currentSavedCentavos)
      val remaining = target.subtract(saved).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP)
      val pct = if (target <= BigDecimal.ZERO) {
        BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP)
      } else {
        saved.multiply(HUNDRED).divide(target, 1, RoundingMode.HALF_UP)
          .min(BigDecimal("100.0"))
      }
      SavingsGoalSummary(
        goalId = goal.id,
        name = goal.name,
        targetAmount = target,
        savedAmount = saved,
        remainingAmount = remaining,
        completionPercent = pct,
        targetDateEpochDay = goal.targetDateEpochDay,
        createdDateEpochDay = goal.createdDateEpochDay,
        status = goal.status,
        isSampleData = goal.isSampleData,
      )
    }

  fun buildExpenseBreakdown(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    yearMonth: YearMonthKey,
  ): List<CategoryBreakdown> =
    calculateCategoryBreakdown(
      expenses = filterTransactionsByMonth(transactions, yearMonth)
        .filter { it.type == TransactionType.EXPENSE },
      categories = categories,
    )

  fun buildAllTimeExpenseBreakdown(
    transactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
  ): List<CategoryBreakdown> =
    calculateCategoryBreakdown(
      expenses = transactions.filter { it.type == TransactionType.EXPENSE },
      categories = categories,
    )

  private fun calculateCategoryBreakdown(
    expenses: List<TransactionEntity>,
    categories: List<CategoryEntity>,
  ): List<CategoryBreakdown> {
    val totalCentavos = expenses.sumOf { it.amountCentavos }
    if (totalCentavos <= 0L) return emptyList()
    val totalBd = centavosToBigDecimal(totalCentavos)
    val colorLookup = categories.associate { it.name to it.colorHex }

    return expenses
      .groupBy { it.category }
      .map { (category, list) ->
        val amount = centavosToBigDecimal(list.sumOf { it.amountCentavos })
        val pct = amount.multiply(HUNDRED).divide(totalBd, 1, RoundingMode.HALF_UP)
        val storedColor = colorLookup[category]
        val resolvedColor = if (storedColor != null && storedColor != 0xFF64748BL) {
          storedColor
        } else {
          defaultCategoryColor(category)
        }
        CategoryBreakdown(
          category = category,
          amount = amount,
          percentage = pct,
          colorHex = resolvedColor,
        )
      }
      .sortedByDescending { it.amount }
  }

  fun buildMonthlyCashFlowSeries(
    transactions: List<TransactionEntity>,
    endMonth: YearMonthKey,
    monthsCount: Int = 6,
  ): List<MonthlyCashFlowPoint> {
    val months = mutableListOf<YearMonthKey>()
    var cursor = endMonth
    repeat(monthsCount) {
      months.add(0, cursor)
      cursor = cursor.previous()
    }
    return months.map { ym ->
      MonthlyCashFlowPoint(
        yearMonth = ym,
        income = calculateTotalIncome(transactions, ym),
        expenses = calculateTotalExpenses(transactions, ym),
      )
    }
  }

  fun buildDailySpendingSeries(
    transactions: List<TransactionEntity>,
    yearMonth: YearMonthKey,
  ): List<DailySpendingPoint> {
    val monthExpenses = filterTransactionsByMonth(transactions, yearMonth)
      .filter { it.type == TransactionType.EXPENSE }
    if (monthExpenses.isEmpty()) return emptyList()

    val byDay = monthExpenses
      .groupBy { dayOfMonthFromEpochDay(it.dateEpochDay) }
      .mapValues { (_, items) -> centavosToBigDecimal(items.sumOf { it.amountCentavos }) }

    return byDay.entries
      .sortedBy { it.key }
      .map { (day, amount) -> DailySpendingPoint(dayOfMonth = day, amount = amount) }
  }

  fun buildAllTimeMonthlySpendingSeries(
    transactions: List<TransactionEntity>,
  ): List<MonthlySpendingPoint> {
    val allExpenses = transactions.filter { it.type == TransactionType.EXPENSE }
    if (allExpenses.isEmpty()) return emptyList()

    return allExpenses
      .groupBy { YearMonthKey.fromEpochDay(it.dateEpochDay) }
      .entries
      .sortedWith(compareBy<Map.Entry<YearMonthKey, List<TransactionEntity>>> { it.key.year }.thenBy { it.key.month })
      .map { (yearMonth, items) ->
        MonthlySpendingPoint(
          yearMonth = yearMonth,
          amount = centavosToBigDecimal(items.sumOf { it.amountCentavos }),
        )
      }
  }

  /**
   * Produces deterministic, rule-based financial insights for [selectedMonth] covering top
   * spending category, budget threshold alerts, savings rate, and month-over-month spending delta.
   * Monetary values are masked when [maskAmounts] is enabled.
   */
  fun generateInsights(
    transactions: List<TransactionEntity>,
    budgetSummaries: List<BudgetSummary>,
    breakdown: List<CategoryBreakdown>,
    selectedMonth: YearMonthKey,
    currencyCode: String,
    maskAmounts: Boolean = false,
  ): List<FinancialInsight> {
    fun displayAmt(amount: BigDecimal): String = if (maskAmounts) {
      formatMaskedCurrency(currencyCode)
    } else if (amount.abs() >= COMPACT_THRESHOLD) {
      formatCompactCurrency(amount, currencyCode)
    } else {
      formatCurrency(amount, currencyCode)
    }

    val currentIncome = calculateTotalIncome(transactions, selectedMonth)
    val currentExpenses = calculateTotalExpenses(transactions, selectedMonth)
    val monthTransactions = filterTransactionsByMonth(transactions, selectedMonth)

    if (monthTransactions.isEmpty()) {
      return listOf(
        FinancialInsight(
          title = "No records for ${selectedMonth.displayLabel()}",
          message = "Add income or expense transactions for this month to unlock personalized spending observations and comparisons.",
          severity = InsightSeverity.INFO,
        )
      )
    }

    val insights = mutableListOf<FinancialInsight>()

    // 1. Highest spending category
    val topCategory = breakdown.firstOrNull()
    if (topCategory != null) {
      insights += FinancialInsight(
        title = "Top Expense Category",
        message = "${topCategory.category} is your highest expense category at ${displayAmt(topCategory.amount)} (${topCategory.percentage.toPlainString()}% of monthly spending).",
        severity = InsightSeverity.INFO,
      )
    }

    // 2. Budget warnings / performance
    val exceeded = budgetSummaries.filter { it.healthStatus == BudgetHealthStatus.EXCEEDED }
    val nearLimit = budgetSummaries.filter {
      it.healthStatus == BudgetHealthStatus.NEAR_LIMIT || it.healthStatus == BudgetHealthStatus.APPROACHING
    }
    when {
      exceeded.isNotEmpty() -> {
        val first = exceeded.first()
        val over = first.spentAmount.subtract(first.limitAmount)
        insights += FinancialInsight(
          title = "Budget Exceeded",
          message = "You have exceeded your ${first.category} budget by ${displayAmt(over)} (${first.utilizationPercent.toPlainString()}% used).",
          severity = InsightSeverity.ALERT,
        )
      }
      nearLimit.isNotEmpty() -> {
        val first = nearLimit.first()
        insights += FinancialInsight(
          title = "Approaching Budget Limit",
          message = "Your ${first.category} budget is at ${first.utilizationPercent.toPlainString()}% with ${displayAmt(first.remainingAmount)} remaining.",
          severity = InsightSeverity.WARNING,
        )
      }
      budgetSummaries.isNotEmpty() -> {
        insights += FinancialInsight(
          title = "Budgets On Track",
          message = "All ${budgetSummaries.size} category budgets for ${selectedMonth.shortLabel()} are within healthy limits below 70%.",
          severity = InsightSeverity.POSITIVE,
        )
      }
    }

    // 3. Savings rate benchmark insight
    if (currentIncome > BigDecimal.ZERO) {
      val isDeficit = currentExpenses > currentIncome
      val savingsRate = calculateSavingsRatePercent(currentIncome, currentExpenses)
      val benchmark = BigDecimal("20.0")
      val severity = when {
        isDeficit -> InsightSeverity.WARNING
        savingsRate >= benchmark -> InsightSeverity.POSITIVE
        else -> InsightSeverity.INFO
      }
      val msg = when {
        isDeficit ->
          "Expenses exceeded income by ${displayAmt(currentExpenses.subtract(currentIncome))} this month."
        savingsRate > benchmark -> {
          val aheadBy = savingsRate.subtract(benchmark).setScale(1, RoundingMode.HALF_UP)
          "Great job — your ${savingsRate.toPlainString()}% savings rate exceeds the recommended 20% benchmark by ${aheadBy.toPlainString()} percentage points."
        }
        savingsRate.compareTo(benchmark) == 0 ->
          "Right on target — your ${savingsRate.toPlainString()}% savings rate meets the recommended 20% benchmark."
        else -> {
          val gap = benchmark.subtract(savingsRate).setScale(1, RoundingMode.HALF_UP)
          "Your ${savingsRate.toPlainString()}% savings rate is ${gap.toPlainString()} percentage points below the recommended 20% benchmark."
        }
      }
      insights += FinancialInsight(
        title = if (isDeficit) "Monthly Deficit" else "Savings Benchmark",
        message = msg,
        severity = severity,
      )
    } else if (currentExpenses > BigDecimal.ZERO) {
      insights += FinancialInsight(
        title = "No Income Recorded",
        message = "No income entries are logged for ${selectedMonth.shortLabel()}, so a savings rate cannot be calculated yet.",
        severity = InsightSeverity.INFO,
      )
    }

    // 4. Month-over-month comparison
    val prevMonth = selectedMonth.previous()
    val prevExpenses = calculateTotalExpenses(transactions, prevMonth)
    if (prevExpenses > BigDecimal.ZERO && currentExpenses > BigDecimal.ZERO) {
      val diff = currentExpenses.subtract(prevExpenses)
      val pctChange = diff.abs()
        .multiply(HUNDRED)
        .divide(prevExpenses, 1, RoundingMode.HALF_UP)
      when {
        diff > BigDecimal.ZERO -> {
          insights += FinancialInsight(
            title = "Month-over-Month Spending",
            message = "Your spending increased by ${pctChange.toPlainString()}% (${displayAmt(diff)}) compared with ${prevMonth.displayLabel()}.",
            severity = InsightSeverity.WARNING,
          )
        }
        diff < BigDecimal.ZERO -> {
          insights += FinancialInsight(
            title = "Month-over-Month Spending",
            message = "Great job — your spending decreased by ${pctChange.toPlainString()}% (${displayAmt(diff.abs())}) compared with ${prevMonth.displayLabel()}.",
            severity = InsightSeverity.POSITIVE,
          )
        }
        else -> {
          insights += FinancialInsight(
            title = "Month-over-Month Spending",
            message = "Your total spending matches ${prevMonth.displayLabel()} at ${displayAmt(currentExpenses)}.",
            severity = InsightSeverity.INFO,
          )
        }
      }
    } else {
      insights += FinancialInsight(
        title = "Comparison Baseline Pending",
        message = "Log expenses across two consecutive months to compare spending changes against ${prevMonth.shortLabel()}.",
        severity = InsightSeverity.INFO,
      )
    }

    return insights
  }

  fun formatCurrency(
    amount: BigDecimal,
    currencyCode: String,
    showPlusSign: Boolean = false,
    forceMinusSign: Boolean = false,
    omitZeroDecimals: Boolean = false,
  ): String {
    val currency = CurrencyOption.fromCode(currencyCode)
    val absValue = amount.abs().setScale(currency.fractionDigits, RoundingMode.HALF_UP)
    val isWholeNumber = absValue.stripTrailingZeros().scale() <= 0
    val effectiveDigits = if (omitZeroDecimals && isWholeNumber) 0 else currency.fractionDigits
    val nf = NumberFormat.getNumberInstance(Locale.US).apply {
      minimumFractionDigits = effectiveDigits
      maximumFractionDigits = effectiveDigits
    }
    val formattedNumber = nf.format(absValue)
    val isNonZero = absValue.compareTo(BigDecimal.ZERO) > 0
    val signPrefix = when {
      isNonZero && (forceMinusSign || amount < BigDecimal.ZERO) -> "−"
      isNonZero && showPlusSign && amount > BigDecimal.ZERO -> "+"
      else -> ""
    }
    return "$signPrefix${currency.symbol}$formattedNumber"
  }

  fun formatCentavos(
    centavos: Long,
    currencyCode: String,
    showPlusSign: Boolean = false,
    forceMinusSign: Boolean = false,
    omitZeroDecimals: Boolean = false,
  ): String = formatCurrency(
    amount = centavosToBigDecimal(centavos),
    currencyCode = currencyCode,
    showPlusSign = showPlusSign,
    forceMinusSign = forceMinusSign,
    omitZeroDecimals = omitZeroDecimals,
  )

  fun formatMaskedCurrency(currencyCode: String): String {
    val currency = CurrencyOption.fromCode(currencyCode)
    return "${currency.symbol} ••••••"
  }

  private val COMPACT_THRESHOLD = BigDecimal("1000000")
  private val THOUSAND_MILLIONS = BigDecimal("1000000000")
  private val TRILLION = BigDecimal("1000000000000")

  /**
   * Formats large monetary values (`>= 1,000,000`) using compact `M`, `B`, or `T` suffixes
   * to prevent overflow in dashboard summary cards while preserving full precision below `1M`.
   */
  fun formatCompactCurrency(
    amount: BigDecimal,
    currencyCode: String,
    showPlusSign: Boolean = false,
    forceMinusSign: Boolean = false,
    omitZeroDecimalsForStandard: Boolean = false,
  ): String {
    val absValue = amount.abs()
    if (absValue < COMPACT_THRESHOLD) {
      return formatCurrency(
        amount = amount,
        currencyCode = currencyCode,
        showPlusSign = showPlusSign,
        forceMinusSign = forceMinusSign,
        omitZeroDecimals = omitZeroDecimalsForStandard,
      )
    }
    val currency = CurrencyOption.fromCode(currencyCode)
    val (divisor, suffix) = when {
      absValue >= TRILLION -> TRILLION to "T"
      absValue >= THOUSAND_MILLIONS -> THOUSAND_MILLIONS to "B"
      else -> COMPACT_THRESHOLD to "M"
    }
    val scaled = absValue
      .divide(divisor, 2, RoundingMode.HALF_UP)
      .stripTrailingZeros()
      .toPlainString()
    val signPrefix = when {
      forceMinusSign || amount < BigDecimal.ZERO -> "−"
      showPlusSign && amount > BigDecimal.ZERO -> "+"
      else -> ""
    }
    return "$signPrefix${currency.symbol}$scaled$suffix"
  }

  fun formatCompactCurrencyOrMasked(
    amount: BigDecimal,
    currencyCode: String,
    isHidden: Boolean,
    showPlusSign: Boolean = false,
    forceMinusSign: Boolean = false,
    omitZeroDecimalsForStandard: Boolean = false,
  ): String = if (isHidden) {
    formatMaskedCurrency(currencyCode)
  } else {
    formatCompactCurrency(
      amount = amount,
      currencyCode = currencyCode,
      showPlusSign = showPlusSign,
      forceMinusSign = forceMinusSign,
      omitZeroDecimalsForStandard = omitZeroDecimalsForStandard,
    )
  }

  fun formatCurrencyOrMasked(
    amount: BigDecimal,
    currencyCode: String,
    isHidden: Boolean,
    showPlusSign: Boolean = false,
    forceMinusSign: Boolean = false,
    omitZeroDecimals: Boolean = false,
  ): String = if (isHidden) {
    formatMaskedCurrency(currencyCode)
  } else {
    formatCurrency(
      amount = amount,
      currencyCode = currencyCode,
      showPlusSign = showPlusSign,
      forceMinusSign = forceMinusSign,
      omitZeroDecimals = omitZeroDecimals,
    )
  }

  fun defaultCategoryColor(category: String): Long =
    CategoryVisualMatcher.inferColorHex(category)
}
