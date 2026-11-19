package com.example.domain

import com.example.data.local.entity.TransactionEntity
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

/**
 * Builds structured ledger digests and contextual starter prompts for the financial assistant
 * directly from the current [BudgetWiseUiState], honoring Privacy Mode masking rules.
 */
object FinancialChatContextBuilder {

  /** Serializes monthly/all-time totals, weekly deltas, budgets, goals, and recent entries into a grounded digest. */
  fun buildContextDigest(state: BudgetWiseUiState): String {
    val currencyCode = state.preferences.preferredCurrency
    val currency = CurrencyOption.fromCode(currencyCode)
    val privacyMode = state.preferences.isBalanceHidden
    val displayName = state.preferences.displayName.trim().ifEmpty { "User" }

    fun fmt(amount: BigDecimal): String =
      FinanceCalculator.formatCurrency(amount, currencyCode)

    val weekly = WeeklySpendingAnalyzer.analyze(
      transactions = state.allTransactions,
      currencyCode = currencyCode,
    )

    val sortedTransactions = state.allTransactions.sortedWith(
      compareByDescending<TransactionEntity> { it.dateEpochDay }
        .thenByDescending { it.createdAtMillis }
    )

    return buildString {
      appendLine("USER PROFILE & PREFERENCES:")
      appendLine("- Name: $displayName")
      appendLine("- Preferred Currency: ${currency.displayName} (${currency.code}, symbol ${currency.symbol})")
      appendLine("- Selected Month in App: ${state.selectedMonth.displayLabel()}")
      appendLine("- Total Recorded Entries: ${state.allTransactions.size}")
      if (privacyMode) {
        appendLine("- PRIVACY MODE IS ENABLED: Avoid printing raw currency numbers in your reply unless the user explicitly insists; rely primarily on percentages, counts, proportions, and category insights.")
      } else {
        appendLine("- Format monetary values clearly using ${currency.symbol} (${currency.code}).")
      }
      appendLine()

      appendLine("SELECTED MONTH SUMMARY (${state.selectedMonth.displayLabel()}):")
      appendLine("- Income: ${fmt(state.totalIncome)}")
      appendLine("- Expenses: ${fmt(state.totalExpenses)}")
      appendLine("- Net Cash Flow: ${fmt(state.netCashFlow)}")
      appendLine("- Savings Rate: ${state.savingsRatePercent.toPlainString()}%")
      appendLine()

      appendLine("ALL-TIME TOTALS:")
      appendLine("- Lifetime Income: ${fmt(state.allTimeIncome)}")
      appendLine("- Lifetime Expenses: ${fmt(state.allTimeExpenses)}")
      appendLine("- Lifetime Net Cash Flow: ${fmt(state.allTimeNetCashFlow)}")
      appendLine("- Reserved in Savings Goals: ${fmt(state.totalReservedSavings)}")
      appendLine()

      appendLine("WEEKLY SPENDING COMPARISON (${weekly.windowLabel}):")
      if (!weekly.hasAnyExpenses) {
        appendLine("- No expense transactions recorded yet.")
      } else {
        val pctStr = weekly.totalPercentChange?.let {
          "${if (it > BigDecimal.ZERO) "+" else ""}${it.toPlainString()}%"
        } ?: "no prior week baseline"
        appendLine("- This Week (${weekly.thisWeekLabel}): ${fmt(weekly.thisWeekTotal)} (${weekly.thisWeekTxCount} expenses)")
        appendLine("- Last Week (${weekly.lastWeekLabel}): ${fmt(weekly.lastWeekTotal)} (${weekly.lastWeekTxCount} expenses)")
        appendLine("- Week-over-Week Delta: ${fmt(weekly.totalDelta)} ($pctStr)")
        if (weekly.categoryDeltas.isNotEmpty()) {
          appendLine("- Weekly Category Shifts:")
          weekly.categoryDeltas.take(6).forEach { cat ->
            val catPct = cat.percentChange?.let {
              "${if (it > BigDecimal.ZERO) "+" else ""}${it.toPlainString()}%"
            } ?: "new"
            appendLine("  * ${cat.category}: This week ${fmt(cat.thisWeekAmount)} vs Last week ${fmt(cat.lastWeekAmount)} ($catPct)")
          }
        }
      }
      appendLine()

      appendLine("EXPENSE BREAKDOWN (${state.selectedMonth.displayLabel()}):")
      if (state.expenseBreakdown.isEmpty()) {
        appendLine("- No expenses recorded for ${state.selectedMonth.displayLabel()}.")
      } else {
        state.expenseBreakdown.forEach { item ->
          appendLine("- ${item.category}: ${fmt(item.amount)} (${item.percentage.toPlainString()}%)")
        }
      }
      appendLine()

      appendLine("BUDGETS (${state.selectedMonth.displayLabel()}):")
      if (state.budgetSummaries.isEmpty()) {
        appendLine("- No budgets configured for ${state.selectedMonth.displayLabel()}.")
      } else {
        state.budgetSummaries.forEach { b ->
          appendLine(
            "- ${b.category}: Spent ${fmt(b.spentAmount)} of ${fmt(b.limitAmount)} limit " +
              "(${b.utilizationPercent.toPlainString()}%, remaining ${fmt(b.remainingAmount)}, status=${b.healthStatus.label})"
          )
        }
      }
      appendLine()

      appendLine("SAVINGS GOALS:")
      if (state.goalSummaries.isEmpty()) {
        appendLine("- No savings goals configured.")
      } else {
        state.goalSummaries.forEach { g ->
          appendLine(
            "- ${g.name}: Saved ${fmt(g.savedAmount)} / ${fmt(g.targetAmount)} " +
              "(${g.completionPercent.toPlainString()}%, target ${formatEpochDay(g.targetDateEpochDay)}, status=${g.status.label})"
          )
        }
      }
      appendLine()

      appendLine("RECENT TRANSACTIONS (most recent ${minOf(20, sortedTransactions.size)}):")
      if (sortedTransactions.isEmpty()) {
        appendLine("- None recorded.")
      } else {
        sortedTransactions.take(20).forEach { tx ->
          val dateStr = formatEpochDay(tx.dateEpochDay, "yyyy-MM-dd")
          val amtStr = fmt(FinanceCalculator.centavosToBigDecimal(tx.amountCentavos))
          val notePart = tx.notes.trim().takeIf { it.isNotEmpty() }?.let { " | Note: $it" } ?: ""
          val sampleTag = if (tx.isSampleData) " [Demo]" else ""
          appendLine("- $dateStr | ${tx.type.label} | ${tx.category} | ${tx.title} | $amtStr | ${tx.paymentMethod.label}$notePart$sampleTag")
        }
      }
    }
  }

  fun buildStarterPrompts(state: BudgetWiseUiState): List<String> {
    if (state.allTransactions.isEmpty()) {
      return listOf(
        "How should I organize my first monthly budget?",
        "What's a realistic savings rate to aim for?",
        "How do I set up categories for daily tracking?",
        "Give me a 50/30/20 starter budget plan",
      )
    }

    val topCat = state.expenseBreakdown.firstOrNull()?.category
    val overBudget = state.budgetSummaries.firstOrNull {
      it.healthStatus == BudgetHealthStatus.EXCEEDED || it.healthStatus == BudgetHealthStatus.NEAR_LIMIT
    }?.category

    return buildList {
      add("How much did I spend in ${state.selectedMonth.shortLabel()}?")
      if (topCat != null) {
        add("Break down my $topCat spending")
      } else {
        add("Compare this week vs last week")
      }
      if (overBudget != null) {
        add("How can I rein in my $overBudget budget?")
      } else if (state.budgetSummaries.isNotEmpty()) {
        add("Am I on track with all my budgets?")
      } else {
        add("Which categories should I budget first?")
      }
      add("Where can I save more each week?")
    }
  }
}
