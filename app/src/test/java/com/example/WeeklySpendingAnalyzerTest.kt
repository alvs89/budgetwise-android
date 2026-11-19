package com.example

import com.example.data.local.entity.TransactionEntity
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.WeeklySpendingAnalyzer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class WeeklySpendingAnalyzerTest {

  private fun expense(
    title: String,
    category: String,
    amountCentavos: Long,
    epochDay: Long,
  ) = TransactionEntity(
    title = title,
    amountCentavos = amountCentavos,
    type = TransactionType.EXPENSE,
    category = category,
    dateEpochDay = epochDay,
    paymentMethod = PaymentMethod.CASH,
    notes = "",
  )

  @Test
  fun `calculates 20 percent weekly increase for dining out accurately`() {
    val today = 20_000L
    // Last week: [19987..19993] -> 1000.00
    // This week: [19994..20000] -> 1200.00 (+20.0%)
    val txs = listOf(
      expense("Lunch", "Food and Dining", 1000_00L, today - 10),
      expense("Dinner", "Food and Dining", 1200_00L, today - 1),
    )

    val result = WeeklySpendingAnalyzer.analyze(
      transactions = txs,
      currencyCode = "PHP",
      referenceEpochDay = today,
    )

    assertTrue(result.hasAnyExpenses)
    assertTrue(result.isLiveCurrentWeek)
    assertEquals(BigDecimal("1200.00"), result.thisWeekTotal)
    assertEquals(BigDecimal("1000.00"), result.lastWeekTotal)
    assertEquals(BigDecimal("200.00"), result.totalDelta)
    assertEquals(BigDecimal("20.0"), result.totalPercentChange)
    assertNotNull(result.topIncreaseCategory)
    assertEquals("Food and Dining", result.topIncreaseCategory?.category)
    assertEquals(BigDecimal("20.0"), result.topIncreaseCategory?.percentChange)
  }

  @Test
  fun `falls back to latest active expense window when current week has no records`() {
    val today = 20_000L
    val latestTxDay = today - 20L
    val txs = listOf(
      expense("Bus", "Transportation", 500_00L, latestTxDay - 8L),
      expense("Taxi", "Transportation", 750_00L, latestTxDay),
    )

    val result = WeeklySpendingAnalyzer.analyze(
      transactions = txs,
      currencyCode = "KRW",
      referenceEpochDay = today,
    )

    assertTrue(result.hasAnyExpenses)
    assertFalse(result.isLiveCurrentWeek)
    assertEquals(latestTxDay, result.thisWeekEndEpochDay)
    assertEquals(BigDecimal("750.00"), result.thisWeekTotal)
    assertEquals(BigDecimal("500.00"), result.lastWeekTotal)
    assertEquals(BigDecimal("50.0"), result.totalPercentChange)
  }

  @Test
  fun `handles empty expense list cleanly`() {
    val result = WeeklySpendingAnalyzer.analyze(
      transactions = emptyList(),
      currencyCode = "USD",
      referenceEpochDay = 20_000L,
    )

    assertFalse(result.hasAnyExpenses)
    assertNull(result.totalPercentChange)
    assertTrue(result.categoryDeltas.isEmpty())
  }

  @Test
  fun `prompt respects privacy mode and preferred currency`() {
    val today = 20_000L
    val txs = listOf(
      expense("Coffee", "Food and Dining", 500_00L, today - 9),
      expense("Cafe", "Food and Dining", 700_00L, today),
    )
    val comparison = WeeklySpendingAnalyzer.analyze(txs, "KRW", today)

    val prompt = WeeklySpendingAnalyzer.buildGeminiPrompt(
      comparison = comparison,
      budgetSummaries = emptyList(),
      goalSummaries = emptyList(),
      currencyCode = "KRW",
      privacyModeEnabled = true,
    )

    assertTrue(prompt.contains("KRW"))
    assertTrue(prompt.contains("PRIVACY MODE IS ON"))
    assertTrue(prompt.contains("Food and Dining"))
  }
}
