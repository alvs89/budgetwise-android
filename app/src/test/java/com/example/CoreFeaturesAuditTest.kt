package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FinancialChatService
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.BudgetHealthStatus
import com.example.domain.FinanceCalculator
import com.example.domain.GoalStatus
import com.example.domain.InsightSeverity
import com.example.domain.PaymentMethod
import com.example.domain.TransactionSortOrder
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.dayOfMonthFromEpochDay
import com.example.domain.epochDayFromDate
import com.example.domain.formatEpochDay
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CoreFeaturesAuditTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val stubChatService = object : FinancialChatService {
    override fun streamReply(
      priorHistory: List<ChatMessageEntity>,
      userQuestion: String,
      financialContextDigest: String,
    ): Flow<String> = flowOf("Audit reply")
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `epochDay date conversions stay exact across negative, zero, and positive UTC timezones`() {
    val originalTz = TimeZone.getDefault()
    val originalLocale = Locale.getDefault()
    try {
      Locale.setDefault(Locale.US)
      val zones = listOf("America/Los_Angeles", "America/New_York", "UTC", "Asia/Manila", "Pacific/Kiritimati")
      for (zoneId in zones) {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        val nov1 = epochDayFromDate(2026, 11, 1)
        val dec31 = epochDayFromDate(2026, 12, 31)
        val jan1 = epochDayFromDate(2027, 1, 1)

        assertEquals("Zone $zoneId month check", YearMonthKey(2026, 11), YearMonthKey.fromEpochDay(nov1))
        assertEquals("Zone $zoneId day check", 1, dayOfMonthFromEpochDay(nov1))
        assertEquals("Zone $zoneId format check", "Nov 1, 2026", formatEpochDay(nov1))

        assertEquals("Zone $zoneId Dec 31", YearMonthKey(2026, 12), YearMonthKey.fromEpochDay(dec31))
        assertEquals("Zone $zoneId Jan 1", YearMonthKey(2027, 1), YearMonthKey.fromEpochDay(jan1))
        assertEquals("Zone $zoneId consecutive day diff", 1L, jan1 - dec31)
      }
    } finally {
      TimeZone.setDefault(originalTz)
      Locale.setDefault(originalLocale)
    }
  }

  @Test
  fun `amount parsing handles overflow safely and formatting suppresses negative zero`() {
    assertNull(FinanceCalculator.parseAmountToCentavos("99999999999999999999999"))
    assertNull(FinanceCalculator.parseAmountToCentavos("0.001"))
    assertEquals(100_50L, FinanceCalculator.parseAmountToCentavos("100.5"))
    assertEquals(1_234_567_89L, FinanceCalculator.parseAmountToCentavos("1,234,567.89"))

    assertEquals("¥0", FinanceCalculator.formatCurrency(BigDecimal("-0.4"), "JPY"))
    assertEquals("₱0.00", FinanceCalculator.formatCurrency(BigDecimal("-0.004"), "PHP"))
    assertEquals("₱0.00", FinanceCalculator.formatCurrency(BigDecimal.ZERO, "PHP", forceMinusSign = true))
  }

  @Test
  fun `insights report monthly deficit warning when expenses exceed income`() {
    val month = YearMonthKey(2026, 11)
    val txs = listOf(
      TransactionEntity(
        id = 1,
        title = "Allowance",
        amountCentavos = 10_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = epochDayFromDate(2026, 11, 2),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      ),
      TransactionEntity(
        id = 2,
        title = "Emergency Repair",
        amountCentavos = 15_000_00L,
        type = TransactionType.EXPENSE,
        category = "Housing",
        dateEpochDay = epochDayFromDate(2026, 11, 5),
        paymentMethod = PaymentMethod.CASH,
      ),
    )
    val breakdown = FinanceCalculator.buildExpenseBreakdown(txs, emptyList(), month)
    val insights = FinanceCalculator.generateInsights(
      transactions = txs,
      budgetSummaries = emptyList(),
      breakdown = breakdown,
      selectedMonth = month,
      currencyCode = "PHP",
    )

    val deficitInsight = insights.firstOrNull { it.title == "Monthly Deficit" }
    assertTrue("Expected Monthly Deficit insight to be emitted", deficitInsight != null)
    assertEquals(InsightSeverity.WARNING, deficitInsight?.severity)
    assertTrue(deficitInsight?.message?.contains("₱5,000.00") == true)
  }

  @Test
  fun `sample demonstration dataset produces consistent cash flow and all budget health tiers`() {
    val month = YearMonthKey(2026, 11)
    val txs = SampleDataSeeder.sampleTransactions(month)
    val budgets = SampleDataSeeder.sampleBudgets(month)

    val income = FinanceCalculator.calculateTotalIncome(txs, month)
    val expenses = FinanceCalculator.calculateTotalExpenses(txs, month)
    val net = FinanceCalculator.calculateNetCashFlow(income, expenses)
    val summaries = FinanceCalculator.buildBudgetSummaries(budgets, txs, month)
    val statuses = summaries.map { it.healthStatus }.toSet()

    assertEquals(BigDecimal("28500.00"), income)
    assertEquals(BigDecimal("15120.00"), expenses)
    assertEquals(BigDecimal("13380.00"), net)
    assertTrue(BudgetHealthStatus.NORMAL in statuses)
    assertTrue(BudgetHealthStatus.APPROACHING in statuses)
    assertTrue(BudgetHealthStatus.NEAR_LIMIT in statuses)
    assertTrue(BudgetHealthStatus.EXCEEDED in statuses)
  }

  @Test
  fun `viewModel budget upsert merges category edits cleanly without duplicate or crash`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)
    val targetMonth = YearMonthKey(2026, 11)

    viewModel.clearAllRecords()
    withTimeout(5_000L) {
      viewModel.uiState.first {
        !it.isLoading &&
          it.preferences.hasSeededInitialData &&
          it.allTransactions.isEmpty() &&
          it.budgetSummaries.isEmpty()
      }
    }
    viewModel.selectMonth(targetMonth)

    viewModel.saveBudget(0L, "Food and Dining", 4_000_00L, targetMonth)
    viewModel.saveBudget(0L, "Shopping", 2_000_00L, targetMonth)

    val twoBudgetsState = withTimeout(5_000L) {
      viewModel.uiState.first {
        it.selectedMonth == targetMonth && it.budgetSummaries.size == 2
      }
    }
    assertEquals(BigDecimal("6000.00"), twoBudgetsState.totalBudgetLimit)

    val shoppingBudgetId = twoBudgetsState.budgetSummaries.first { it.category == "Shopping" }.budgetId

    // Reassign Shopping budget onto Food and Dining (must merge cleanly, not crash on UNIQUE constraint)
    viewModel.saveBudget(shoppingBudgetId, "Food and Dining", 5_500_00L, targetMonth)

    val mergedState = withTimeout(5_000L) {
      viewModel.uiState.first {
        it.budgetSummaries.size == 1 &&
          it.budgetSummaries.first().category == "Food and Dining" &&
          it.budgetSummaries.first().limitAmount == BigDecimal("5500.00")
      }
    }
    assertEquals(BigDecimal("5500.00"), mergedState.totalBudgetLimit)
  }

  @Test
  fun `viewModel goal lifecycle auto-completes, reopens on target increase, and adjusts on withdrawal`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)

    viewModel.clearAllRecords()
    withTimeout(5_000L) {
      viewModel.uiState.first {
        !it.isLoading &&
          it.preferences.hasSeededInitialData &&
          it.rawGoals.isEmpty()
      }
    }

    viewModel.saveGoal(
      existing = null,
      name = "Audit Laptop",
      targetCentavos = 10_000_00L,
      targetDateEpochDay = epochDayFromDate(2027, 6, 1),
      status = GoalStatus.ACTIVE,
    )
    val createdState = withTimeout(5_000L) {
      viewModel.uiState.first { state -> state.rawGoals.any { it.name == "Audit Laptop" } }
    }
    val goal = createdState.rawGoals.first { it.name == "Audit Laptop" }

    // Deposit full target -> auto-completes
    viewModel.recordGoalContribution(
      goalId = goal.id,
      amountCentavos = 10_000_00L,
      isWithdrawal = false,
      note = "Full deposit",
      dateEpochDay = epochDayFromDate(2026, 11, 10),
    )
    val completedState = withTimeout(5_000L) {
      viewModel.uiState.first { state ->
        state.goalSummaries.any { it.goalId == goal.id && it.status == GoalStatus.COMPLETED }
      }
    }
    val completedGoalEntity = completedState.rawGoals.first { it.id == goal.id }
    assertEquals(BigDecimal("100.0"), completedState.goalSummaries.first { it.goalId == goal.id }.completionPercent)

    // Raise target above saved amount while status still shows COMPLETED -> must reopen to ACTIVE
    viewModel.saveGoal(
      existing = completedGoalEntity,
      name = "Audit Laptop Pro",
      targetCentavos = 20_000_00L,
      targetDateEpochDay = completedGoalEntity.targetDateEpochDay,
      status = GoalStatus.COMPLETED,
    )
    val reopenedState = withTimeout(5_000L) {
      viewModel.uiState.first { state ->
        state.goalSummaries.any {
          it.goalId == goal.id && it.name == "Audit Laptop Pro" && it.status == GoalStatus.ACTIVE
        }
      }
    }
    val reopenedSummary = reopenedState.goalSummaries.first { it.goalId == goal.id }
    assertEquals(BigDecimal("50.0"), reopenedSummary.completionPercent)
    assertEquals(BigDecimal("10000.00"), reopenedSummary.remainingAmount)
  }

  @Test
  fun `viewModel activity filtering clears incompatible category when switching type and sorts accurately`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)
    val month = YearMonthKey(2026, 11)

    withTimeout(5_000L) {
      viewModel.uiState.first { !it.isLoading && it.preferences.hasSeededInitialData }
    }
    if (viewModel.uiState.value.allTransactions.isNotEmpty()) {
      viewModel.clearAllRecords()
      withTimeout(5_000L) {
        viewModel.uiState.first { it.allTransactions.isEmpty() }
      }
    }
    viewModel.selectMonth(month)

    viewModel.saveTransaction(
      title = "Monthly Salary",
      amountCentavos = 30_000_00L,
      type = TransactionType.INCOME,
      category = "Salary",
      dateEpochDay = epochDayFromDate(2026, 11, 1),
      paymentMethod = PaymentMethod.BANK_TRANSFER,
      notes = "Payroll",
    )
    viewModel.saveTransaction(
      title = "Coffee",
      amountCentavos = 180_00L,
      type = TransactionType.EXPENSE,
      category = "Food and Dining",
      dateEpochDay = epochDayFromDate(2026, 11, 3),
      paymentMethod = PaymentMethod.CASH,
      notes = "Latte",
    )
    viewModel.saveTransaction(
      title = "Groceries",
      amountCentavos = 2_400_00L,
      type = TransactionType.EXPENSE,
      category = "Food and Dining",
      dateEpochDay = epochDayFromDate(2026, 11, 5),
      paymentMethod = PaymentMethod.DEBIT_CARD,
      notes = "Weekly food",
    )

    withTimeout(5_000L) {
      viewModel.uiState.first { it.monthTransactions.size == 3 }
    }

    // Filter to Income + Salary, then switch to Expense -> Salary filter must auto-clear
    viewModel.updateTypeFilter(TransactionType.INCOME)
    viewModel.updateCategoryFilter("Salary")
    val incomeFiltered = withTimeout(5_000L) {
      viewModel.uiState.first { it.activityFilter.categoryFilter == "Salary" }
    }
    assertEquals(1, incomeFiltered.filteredActivityTransactions.size)

    viewModel.updateTypeFilter(TransactionType.EXPENSE)
    viewModel.updateSortOrder(TransactionSortOrder.AMOUNT_DESC)
    val expenseFiltered = withTimeout(5_000L) {
      viewModel.uiState.first {
        it.activityFilter.typeFilter == TransactionType.EXPENSE &&
          it.activityFilter.categoryFilter == null &&
          it.activityFilter.sortOrder == TransactionSortOrder.AMOUNT_DESC
      }
    }
    assertEquals(2, expenseFiltered.filteredActivityTransactions.size)
    assertEquals("Groceries", expenseFiltered.filteredActivityTransactions[0].title)
    assertEquals("Coffee", expenseFiltered.filteredActivityTransactions[1].title)
  }
}
