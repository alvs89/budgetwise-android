package com.example

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.ExportFormat
import com.example.domain.ExportScope
import com.example.domain.FinancialReportExporter
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.epochDayFromDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialReportExporterTest {

  private val nov2026 = YearMonthKey(2026, 11)
  private val oct2026 = YearMonthKey(2026, 10)

  private fun sampleTransactions(): List<TransactionEntity> = listOf(
    TransactionEntity(
      id = 1L,
      title = "Monthly Salary",
      amountCentavos = 4500000L,
      type = TransactionType.INCOME,
      category = "Salary",
      dateEpochDay = epochDayFromDate(2026, 11, 1),
      paymentMethod = PaymentMethod.BANK_TRANSFER,
      notes = "Main payroll",
      createdAtMillis = 1000L,
    ),
    TransactionEntity(
      id = 2L,
      title = "Groceries, \"Fresh\" Market",
      amountCentavos = 235050L,
      type = TransactionType.EXPENSE,
      category = "Food and Dining",
      dateEpochDay = epochDayFromDate(2026, 11, 5),
      paymentMethod = PaymentMethod.GCASH,
      notes = "=SUM(A1:A5)\nWeekly stock-up",
      createdAtMillis = 2000L,
    ),
    TransactionEntity(
      id = 3L,
      title = "October Internet Bill",
      amountCentavos = 180000L,
      type = TransactionType.EXPENSE,
      category = "Bills and Utilities",
      dateEpochDay = epochDayFromDate(2026, 10, 15),
      paymentMethod = PaymentMethod.MAYA,
      notes = "Fiber plan",
      createdAtMillis = 500L,
    ),
  )

  private fun sampleBudgets(): List<BudgetEntity> = listOf(
    BudgetEntity(
      id = 10L,
      category = "Food and Dining",
      limitCentavos = 500000L,
      year = 2026,
      month = 11,
    ),
    BudgetEntity(
      id = 11L,
      category = "Bills and Utilities",
      limitCentavos = 200000L,
      year = 2026,
      month = 10,
    ),
  )

  @Test
  fun `csv export includes BOM, summary metrics, budgets, transactions, and safe escaping`() {
    val report = FinancialReportExporter.buildReportData(
      ownerName = "Alvin",
      currencyCode = "PHP",
      selectedMonth = nov2026,
      allTransactions = sampleTransactions(),
      allBudgets = sampleBudgets(),
      scope = ExportScope.SELECTED_MONTH,
      includeTransactions = true,
      includeBudgets = true,
      generatedEpochDay = epochDayFromDate(2026, 11, 19),
    )

    val csv = FinancialReportExporter.buildCsvReport(report)

    assertTrue("CSV should start with UTF-8 BOM for Excel compatibility", csv.startsWith("\uFEFF"))
    assertTrue(csv.contains("BudgetWise Financial Report"))
    assertTrue(csv.contains("Period,November 2026"))
    assertTrue(csv.contains("Currency,PHP"))
    assertTrue(csv.contains("Total Income,45000.00"))
    assertTrue(csv.contains("Total Expenses,2350.50"))
    assertTrue(csv.contains("Net Cash Flow,42649.50"))

    // Budget section should only contain Nov 2026 budget
    assertTrue(csv.contains("BUDGET SUMMARY"))
    assertTrue(csv.contains("November 2026,Food and Dining,5000.00,2350.50,2649.50,47.0,On track,PHP"))
    assertFalse(csv.contains("October 2026,Bills and Utilities"))

    // Transactions section should properly quote commas/quotes and neutralize formula injection
    assertTrue(csv.contains("TRANSACTIONS"))
    assertTrue(csv.contains("\"Groceries, \"\"Fresh\"\" Market\""))
    assertTrue(csv.contains("'=SUM(A1:A5) Weekly stock-up"))
    assertTrue(csv.contains("2350.50,-2350.50,PHP"))
    assertTrue(csv.contains("45000.00,45000.00,PHP"))
    assertFalse(csv.contains("October Internet Bill"))
  }

  @Test
  fun `all-time scope exports budgets and transactions across multiple months`() {
    val report = FinancialReportExporter.buildReportData(
      ownerName = "Alvin",
      currencyCode = "USD",
      selectedMonth = nov2026,
      allTransactions = sampleTransactions(),
      allBudgets = sampleBudgets(),
      scope = ExportScope.ALL_TIME,
      includeTransactions = true,
      includeBudgets = true,
    )

    assertEquals(3, report.transactions.size)
    assertEquals(2, report.budgets.size)
    assertEquals("BudgetWise_Report_AllTime.csv", FinancialReportExporter.suggestedFileName(report, ExportFormat.CSV))
    assertEquals("BudgetWise_Report_AllTime.pdf", FinancialReportExporter.suggestedFileName(report, ExportFormat.PDF))

    val csv = FinancialReportExporter.buildCsvReport(report)
    assertTrue(csv.contains("November 2026,Food and Dining"))
    assertTrue(csv.contains("October 2026,Bills and Utilities"))
    assertTrue(csv.contains("October Internet Bill"))
  }

  @Test
  fun `selective dataset flags control sections and suggested filenames`() {
    val budgetsOnly = FinancialReportExporter.buildReportData(
      ownerName = "Alvin",
      currencyCode = "PHP",
      selectedMonth = nov2026,
      allTransactions = sampleTransactions(),
      allBudgets = sampleBudgets(),
      scope = ExportScope.SELECTED_MONTH,
      includeTransactions = false,
      includeBudgets = true,
    )

    assertEquals("BudgetWise_Budgets_2026-11.csv", FinancialReportExporter.suggestedFileName(budgetsOnly, ExportFormat.CSV))
    val csv = FinancialReportExporter.buildCsvReport(budgetsOnly)
    assertTrue(csv.contains("BUDGET SUMMARY"))
    assertFalse(csv.contains("TRANSACTIONS"))
  }

  @Test
  fun `pdf renderer paginates cleanly across multiple A4 pages`() {
    val manyTransactions = (1..55).map { idx ->
      TransactionEntity(
        id = idx.toLong(),
        title = "Expense Entry #$idx - Coffee & Commute Long Description",
        amountCentavos = 15000L + idx * 100L,
        type = if (idx % 10 == 0) TransactionType.INCOME else TransactionType.EXPENSE,
        category = if (idx % 10 == 0) "Freelance" else "Food and Dining",
        dateEpochDay = epochDayFromDate(2026, 11, (idx % 28) + 1),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Note $idx",
        createdAtMillis = idx.toLong(),
      )
    }

    val report = FinancialReportExporter.buildReportData(
      ownerName = "Alvin",
      currencyCode = "PHP",
      selectedMonth = nov2026,
      allTransactions = manyTransactions,
      allBudgets = sampleBudgets(),
      scope = ExportScope.SELECTED_MONTH,
      includeTransactions = true,
      includeBudgets = true,
      generatedEpochDay = epochDayFromDate(2026, 11, 19),
    )

    val startedPages = mutableListOf<Triple<Int, Int, Int>>()
    var finishedPages = 0
    val testSink = object : FinancialReportExporter.PdfPageSink {
      override fun startPage(width: Int, height: Int, pageNumber: Int): android.graphics.Canvas {
        startedPages += Triple(width, height, pageNumber)
        return android.graphics.Canvas(
          android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888),
        )
      }

      override fun finishPage() {
        finishedPages += 1
      }
    }

    val pageCount = FinancialReportExporter.renderPdfReport(report, testSink)

    assertTrue("55 transactions + budgets should paginate across at least 2 A4 pages", pageCount >= 2)
    assertEquals(pageCount, startedPages.size)
    assertEquals(pageCount, finishedPages)
    startedPages.forEachIndexed { idx, (w, h, num) ->
      assertEquals(595, w)
      assertEquals(842, h)
      assertEquals(idx + 1, num)
    }
  }
}
