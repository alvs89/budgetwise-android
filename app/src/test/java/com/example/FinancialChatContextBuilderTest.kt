package com.example

import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferences
import com.example.domain.BudgetHealthStatus
import com.example.domain.BudgetSummary
import com.example.domain.CategoryBreakdown
import com.example.domain.FinancialChatContextBuilder
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.SavingsGoalSummary
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.epochDayFromDate
import com.example.ui.viewmodel.BudgetWiseUiState
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinancialChatContextBuilderTest {

  @Test
  fun `context digest grounds transactions, budgets, goals, currency, and privacy mode`() {
    val nov = YearMonthKey(2026, 11)
    val tx = TransactionEntity(
      id = 1L,
      title = "Kimchi Stew Lunch",
      amountCentavos = 12_000_00L,
      type = TransactionType.EXPENSE,
      category = "Food and Dining",
      dateEpochDay = epochDayFromDate(2026, 11, 15),
      paymentMethod = PaymentMethod.DEBIT_CARD,
      notes = "Gangnam spot",
    )
    val uiState = BudgetWiseUiState(
      isLoading = false,
      preferences = UserPreferences(
        displayName = "Alex",
        preferredCurrency = "KRW",
        isBalanceHidden = true,
      ),
      selectedMonth = nov,
      allTransactions = listOf(tx),
      monthTransactions = listOf(tx),
      totalIncome = BigDecimal("100000.00"),
      totalExpenses = BigDecimal("12000.00"),
      netCashFlow = BigDecimal("88000.00"),
      savingsRatePercent = BigDecimal("88.0"),
      budgetSummaries = listOf(
        BudgetSummary(
          budgetId = 1L,
          category = "Food and Dining",
          yearMonth = nov,
          limitAmount = BigDecimal("20000.00"),
          spentAmount = BigDecimal("12000.00"),
          remainingAmount = BigDecimal("8000.00"),
          utilizationPercent = BigDecimal("60.0"),
          healthStatus = BudgetHealthStatus.NORMAL,
          isSampleData = false,
        )
      ),
      goalSummaries = listOf(
        SavingsGoalSummary(
          goalId = 1L,
          name = "Emergency Reserve",
          targetAmount = BigDecimal("500000.00"),
          savedAmount = BigDecimal("250000.00"),
          remainingAmount = BigDecimal("250000.00"),
          completionPercent = BigDecimal("50.0"),
          targetDateEpochDay = epochDayFromDate(2027, 6, 1),
          createdDateEpochDay = epochDayFromDate(2026, 1, 1),
          status = GoalStatus.ACTIVE,
          isSampleData = false,
        )
      ),
      expenseBreakdown = listOf(
        CategoryBreakdown(
          category = "Food and Dining",
          amount = BigDecimal("12000.00"),
          percentage = BigDecimal("100.0"),
          colorHex = 0xFF0F766EL,
        )
      ),
    )

    val digest = FinancialChatContextBuilder.buildContextDigest(uiState)

    assertTrue(digest.contains("Alex"))
    assertTrue(digest.contains("KRW"))
    assertTrue(digest.contains("PRIVACY MODE IS ENABLED"))
    assertTrue(digest.contains("Kimchi Stew Lunch"))
    assertTrue(digest.contains("Emergency Reserve"))
    assertTrue(digest.contains("Food and Dining"))
  }

  @Test
  fun `starter prompts adapt to exceeded budget and top spending category`() {
    val nov = YearMonthKey(2026, 11)
    val tx = TransactionEntity(
      id = 1L,
      title = "Cafe",
      amountCentavos = 500_00L,
      type = TransactionType.EXPENSE,
      category = "Food and Dining",
      dateEpochDay = epochDayFromDate(2026, 11, 12),
      paymentMethod = PaymentMethod.CASH,
    )
    val state = BudgetWiseUiState(
      isLoading = false,
      selectedMonth = nov,
      allTransactions = listOf(tx),
      expenseBreakdown = listOf(
        CategoryBreakdown("Food and Dining", BigDecimal("500.00"), BigDecimal("100.0"), 0xFF0F766EL)
      ),
      budgetSummaries = listOf(
        BudgetSummary(
          budgetId = 1L,
          category = "Transportation",
          yearMonth = nov,
          limitAmount = BigDecimal("100.00"),
          spentAmount = BigDecimal("150.00"),
          remainingAmount = BigDecimal("-50.00"),
          utilizationPercent = BigDecimal("150.0"),
          healthStatus = BudgetHealthStatus.EXCEEDED,
          isSampleData = false,
        )
      ),
    )

    val prompts = FinancialChatContextBuilder.buildStarterPrompts(state)

    assertTrue(prompts.any { it.contains("Food and Dining") })
    assertTrue(prompts.any { it.contains("Transportation") })
  }
}
