package com.example

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.BudgetHealthStatus
import com.example.domain.CategoryVisualMatcher
import com.example.domain.FinanceCalculator
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.compactCategoryLabel
import com.example.domain.epochDayFromDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class FinanceCalculatorTest {

  private val nov = YearMonthKey(2026, 11)
  private val oct = YearMonthKey(2026, 10)

  @Test
  fun `calculate totals and net cash flow accurately without floating point drift`() {
    val txs = listOf(
      TransactionEntity(
        id = 1,
        title = "Allowance",
        amountCentavos = 25_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = epochDayFromDate(2026, 11, 1),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      ),
      TransactionEntity(
        id = 2,
        title = "Coffee",
        amountCentavos = 199_99L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 11, 4),
        paymentMethod = PaymentMethod.CASH,
      ),
      TransactionEntity(
        id = 3,
        title = "Snack",
        amountCentavos = 1L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 11, 5),
        paymentMethod = PaymentMethod.CASH,
      ),
      TransactionEntity(
        id = 4,
        title = "Previous month",
        amountCentavos = 5_000_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 10, 12),
        paymentMethod = PaymentMethod.CASH,
      ),
    )

    val income = FinanceCalculator.calculateTotalIncome(txs, nov)
    val expenses = FinanceCalculator.calculateTotalExpenses(txs, nov)
    val net = FinanceCalculator.calculateNetCashFlow(income, expenses)

    assertEquals(BigDecimal("25000.00"), income)
    assertEquals(BigDecimal("200.00"), expenses)
    assertEquals(BigDecimal("24800.00"), net)
  }

  @Test
  fun `zero income and zero budget limit return zero safely`() {
    assertEquals(
      BigDecimal("0.0"),
      FinanceCalculator.calculateSavingsRatePercent(BigDecimal.ZERO, BigDecimal("500.00")),
    )
    assertEquals(
      BigDecimal("0.0"),
      FinanceCalculator.calculateBudgetUtilizationPercent(BigDecimal("100.00"), BigDecimal.ZERO),
    )
    assertNull(FinanceCalculator.parseAmountToCentavos("-50"))
    assertNull(FinanceCalculator.parseAmountToCentavos("0"))
    assertNull(FinanceCalculator.parseAmountToCentavos("abc"))
    assertEquals(12345L, FinanceCalculator.parseAmountToCentavos("123.45"))
  }

  @Test
  fun `budget utilization thresholds map accurately`() {
    val budgets = listOf(
      BudgetEntity(id = 1, category = "Food and Dining", limitCentavos = 1_000_00L, year = 2026, month = 11),
      BudgetEntity(id = 2, category = "Transportation", limitCentavos = 1_000_00L, year = 2026, month = 11),
      BudgetEntity(id = 3, category = "Education", limitCentavos = 1_000_00L, year = 2026, month = 11),
      BudgetEntity(id = 4, category = "Shopping", limitCentavos = 1_000_00L, year = 2026, month = 11),
    )
    val txs = listOf(
      TransactionEntity(title = "A", amountCentavos = 650_00L, type = TransactionType.EXPENSE, category = "Food and Dining", dateEpochDay = epochDayFromDate(2026, 11, 2), paymentMethod = PaymentMethod.CASH),
      TransactionEntity(title = "B", amountCentavos = 750_00L, type = TransactionType.EXPENSE, category = "Transportation", dateEpochDay = epochDayFromDate(2026, 11, 2), paymentMethod = PaymentMethod.CASH),
      TransactionEntity(title = "C", amountCentavos = 950_00L, type = TransactionType.EXPENSE, category = "Education", dateEpochDay = epochDayFromDate(2026, 11, 2), paymentMethod = PaymentMethod.CASH),
      TransactionEntity(title = "D", amountCentavos = 1_100_00L, type = TransactionType.EXPENSE, category = "Shopping", dateEpochDay = epochDayFromDate(2026, 11, 2), paymentMethod = PaymentMethod.CASH),
    )

    val summaries = FinanceCalculator.buildBudgetSummaries(budgets, txs, nov).associateBy { it.category }

    assertEquals(BudgetHealthStatus.NORMAL, summaries["Food and Dining"]?.healthStatus)
    assertEquals(BudgetHealthStatus.APPROACHING, summaries["Transportation"]?.healthStatus)
    assertEquals(BudgetHealthStatus.NEAR_LIMIT, summaries["Education"]?.healthStatus)
    assertEquals(BudgetHealthStatus.EXCEEDED, summaries["Shopping"]?.healthStatus)
    assertEquals(BigDecimal("-100.00"), summaries["Shopping"]?.remainingAmount)
  }

  @Test
  fun `insights handle empty month gracefully`() {
    val insights = FinanceCalculator.generateInsights(
      transactions = emptyList(),
      budgetSummaries = emptyList(),
      breakdown = emptyList(),
      selectedMonth = oct,
      currencyCode = "PHP",
    )
    assertEquals(1, insights.size)
    assertTrue(insights.first().title.contains("No records"))
  }

  @Test
  fun `compact currency omits trailing zeros for whole integers`() {
    assertEquals("₱2,180", FinanceCalculator.formatCurrency(BigDecimal("2180.00"), "PHP", omitZeroDecimals = true))
    assertEquals("₱2,180.50", FinanceCalculator.formatCurrency(BigDecimal("2180.50"), "PHP", omitZeroDecimals = true))
  }

  @Test
  fun `privacy mode masks currency values cleanly`() {
    assertEquals("₱ ••••••", FinanceCalculator.formatMaskedCurrency("PHP"))
    assertEquals("$ ••••••", FinanceCalculator.formatMaskedCurrency("USD"))
    assertEquals(
      "₱ ••••••",
      FinanceCalculator.formatCurrencyOrMasked(BigDecimal("13380.00"), "PHP", isHidden = true),
    )
    assertEquals(
      "₱13,380.00",
      FinanceCalculator.formatCurrencyOrMasked(BigDecimal("13380.00"), "PHP", isHidden = false),
    )
  }

  @Test
  fun `insights mask monetary amounts when privacy mode is enabled`() {
    val txs = listOf(
      TransactionEntity(
        id = 1,
        title = "Salary",
        amountCentavos = 25_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = epochDayFromDate(2026, 11, 1),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      ),
      TransactionEntity(
        id = 2,
        title = "Groceries",
        amountCentavos = 3_200_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 11, 3),
        paymentMethod = PaymentMethod.CASH,
      ),
    )
    val breakdown = FinanceCalculator.buildExpenseBreakdown(txs, emptyList(), nov)
    val insights = FinanceCalculator.generateInsights(
      transactions = txs,
      budgetSummaries = emptyList(),
      breakdown = breakdown,
      selectedMonth = nov,
      currencyCode = "PHP",
      maskAmounts = true,
    )
    assertTrue(insights.isNotEmpty())
    assertTrue(insights.any { it.message.contains("₱ ••••••") })
    assertTrue(insights.none { it.message.contains("25,000") || it.message.contains("3,200") })
  }

  @Test
  fun `yearMonthKey formats full month name without truncation`() {
    assertEquals("December", YearMonthKey(2026, 12).monthNameOnly())
    assertEquals("November", nov.monthNameOnly())
  }

  @Test
  fun `compactCategoryLabel shortens compound category names cleanly`() {
    assertEquals("Food & Dining", compactCategoryLabel("Food and Dining"))
    assertEquals("Bills & Utilities", compactCategoryLabel("Bills and Utilities"))
    assertEquals("Transportation", compactCategoryLabel("Transportation"))
  }

  @Test
  fun `calculateAllTime totals and reserved goal savings aggregate across months`() {
    val txs = listOf(
      TransactionEntity(
        id = 1,
        title = "October Allowance",
        amountCentavos = 15_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = epochDayFromDate(2026, 10, 15),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      ),
      TransactionEntity(
        id = 2,
        title = "October Transit",
        amountCentavos = 1_200_00L,
        type = TransactionType.EXPENSE,
        category = "Transportation",
        dateEpochDay = epochDayFromDate(2026, 10, 20),
        paymentMethod = PaymentMethod.GCASH,
      ),
      TransactionEntity(
        id = 3,
        title = "November Salary",
        amountCentavos = 25_000_00L,
        type = TransactionType.INCOME,
        category = "Salary",
        dateEpochDay = epochDayFromDate(2026, 11, 5),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      ),
      TransactionEntity(
        id = 4,
        title = "November Groceries",
        amountCentavos = 3_800_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 11, 10),
        paymentMethod = PaymentMethod.CASH,
      ),
    )
    val goals = listOf(
      SavingsGoalEntity(
        id = 1,
        name = "Emergency Fund",
        targetCentavos = 50_000_00L,
        currentSavedCentavos = 35_000_00L,
        targetDateEpochDay = epochDayFromDate(2027, 6, 1),
        createdDateEpochDay = epochDayFromDate(2026, 1, 1),
      ),
      SavingsGoalEntity(
        id = 2,
        name = "Travel Fund",
        targetCentavos = 15_000_00L,
        currentSavedCentavos = 6_250_00L,
        targetDateEpochDay = epochDayFromDate(2027, 3, 10),
        createdDateEpochDay = epochDayFromDate(2026, 5, 1),
      ),
    )

    val allTimeIncome = FinanceCalculator.calculateAllTimeTotalIncome(txs)
    val allTimeExpenses = FinanceCalculator.calculateAllTimeTotalExpenses(txs)
    val allTimeNet = FinanceCalculator.calculateNetCashFlow(allTimeIncome, allTimeExpenses)
    val reservedSavings = FinanceCalculator.calculateTotalReservedSavings(goals)
    val allTimeBreakdown = FinanceCalculator.buildAllTimeExpenseBreakdown(txs, emptyList())
    val allTimeMonthlySeries = FinanceCalculator.buildAllTimeMonthlySpendingSeries(txs)

    assertEquals(BigDecimal("40000.00"), allTimeIncome)
    assertEquals(BigDecimal("5000.00"), allTimeExpenses)
    assertEquals(BigDecimal("35000.00"), allTimeNet)
    assertEquals(BigDecimal("41250.00"), reservedSavings)
    assertEquals(2, allTimeBreakdown.size)
    assertEquals("Food and Dining", allTimeBreakdown.first().category)
    assertEquals(BigDecimal("3800.00"), allTimeBreakdown.first().amount)
    assertEquals(2, allTimeMonthlySeries.size)
    assertEquals(oct, allTimeMonthlySeries[0].yearMonth)
    assertEquals(BigDecimal("1200.00"), allTimeMonthlySeries[0].amount)
    assertEquals(nov, allTimeMonthlySeries[1].yearMonth)
    assertEquals(BigDecimal("3800.00"), allTimeMonthlySeries[1].amount)
  }

  @Test
  fun `extreme overspending clamps savings rate to zero and uses compact formatting`() {
    val income = BigDecimal("22500.00")
    val expenses = BigDecimal("100015120.00")
    val deficit = expenses.subtract(income)

    assertEquals(
      BigDecimal("0.0"),
      FinanceCalculator.calculateSavingsRatePercent(income, expenses),
    )
    assertEquals(
      "₱99.99M",
      FinanceCalculator.formatCompactCurrency(deficit, "PHP", omitZeroDecimalsForStandard = true),
    )
    assertEquals(
      "₱100.02M",
      FinanceCalculator.formatCompactCurrency(expenses, "PHP", omitZeroDecimalsForStandard = true),
    )
    assertEquals(
      "₱1.5B",
      FinanceCalculator.formatCompactCurrency(BigDecimal("1500000000.00"), "PHP", omitZeroDecimalsForStandard = true),
    )
    assertEquals(
      "₱28,500",
      FinanceCalculator.formatCompactCurrency(BigDecimal("28500.00"), "PHP", omitZeroDecimalsForStandard = true),
    )
  }

  @Test
  fun `categoryVisualMatcher matches custom categories, handles typos, and avoids false substrings`() {
    assertEquals("coffee", CategoryVisualMatcher.inferIconKey("Coffee & Snacks", TransactionType.EXPENSE))
    assertEquals("groceries", CategoryVisualMatcher.inferIconKey("Groceries", TransactionType.EXPENSE))
    assertEquals("pets", CategoryVisualMatcher.inferIconKey("Pet Care", TransactionType.EXPENSE))
    assertEquals("fitness", CategoryVisualMatcher.inferIconKey("Gym Membership", TransactionType.EXPENSE))
    assertEquals("flight", CategoryVisualMatcher.inferIconKey("Vacation Trip", TransactionType.EXPENSE))
    assertEquals("subscriptions", CategoryVisualMatcher.inferIconKey("Netflix & Spotify", TransactionType.EXPENSE))
    assertEquals("trending_up", CategoryVisualMatcher.inferIconKey("Stock Dividends", TransactionType.INCOME))
    assertEquals("gift", CategoryVisualMatcher.inferIconKey("Birthday Gift", TransactionType.EXPENSE))

    // Typo resilience (e.g. "Healthcre" -> Healthcare medical icon)
    assertEquals("medical", CategoryVisualMatcher.inferIconKey("Healthcre", TransactionType.EXPENSE))
    assertEquals("groceries", CategoryVisualMatcher.inferIconKey("Grocries", TransactionType.EXPENSE))

    // Word boundary safety ("Personal Care" must match spa, NOT "car")
    assertEquals("spa", CategoryVisualMatcher.inferIconKey("Personal Care", TransactionType.EXPENSE))

    // Unmatched generic names get non-slate deterministic color + type fallback icon
    assertEquals("label", CategoryVisualMatcher.inferIconKey("Sample", TransactionType.EXPENSE))
    assertEquals("payments", CategoryVisualMatcher.inferIconKey("Sample", TransactionType.INCOME))
    assertTrue(CategoryVisualMatcher.inferColorHex("Sample", TransactionType.EXPENSE) != 0xFF64748BL)
  }

  @Test
  fun `korean won and newly added currencies format properly in standard and compact mode`() {
    assertEquals(
      "₩50,000",
      FinanceCalculator.formatCurrency(BigDecimal("50000"), "KRW"),
    )
    assertEquals(
      "₩1.5M",
      FinanceCalculator.formatCompactCurrency(BigDecimal("1500000"), "KRW"),
    )
    assertEquals(
      "£1,250.50",
      FinanceCalculator.formatCurrency(BigDecimal("1250.50"), "GBP"),
    )
  }
}
