package com.example.domain

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.TransactionEntity
import java.io.OutputStream
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

enum class ExportFormat(
  val label: String,
  val extension: String,
  val mimeType: String,
) {
  CSV(
    label = "CSV",
    extension = "csv",
    mimeType = "text/csv",
  ),
  PDF(
    label = "PDF",
    extension = "pdf",
    mimeType = "application/pdf",
  ),
}

enum class ExportScope(val label: String) {
  SELECTED_MONTH("Selected Month"),
  ALL_TIME("All Time"),
}

data class ExportRequest(
  val format: ExportFormat = ExportFormat.CSV,
  val scope: ExportScope = ExportScope.SELECTED_MONTH,
  val includeTransactions: Boolean = true,
  val includeBudgets: Boolean = true,
)

/**
 * Immutable snapshot of transactions, budget summaries, and financial KPIs prepared for
 * offline CSV or PDF report generation.
 */
data class FinancialReportData(
  val ownerName: String,
  val currencyCode: String,
  val periodLabel: String,
  val periodSlug: String,
  val generatedDateLabel: String,
  val totalIncome: BigDecimal,
  val totalExpenses: BigDecimal,
  val netCashFlow: BigDecimal,
  val savingsRatePercent: BigDecimal,
  val transactions: List<TransactionEntity>,
  val budgets: List<BudgetSummary>,
  val includeTransactions: Boolean,
  val includeBudgets: Boolean,
)

/**
 * Generates spreadsheet-ready CSV files (UTF-8 with BOM, RFC 4180 escaping, formula-injection
 * neutralization) and multi-page A4 PDF financial reports from local BudgetWise records.
 */
object FinancialReportExporter {

  private const val UTF8_BOM = "\uFEFF"

  // A4 page geometry in PostScript points (72 dpi)
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN_X = 36f
  private const val CONTENT_WIDTH = PAGE_WIDTH - (MARGIN_X * 2f)
  private const val TOP_START_Y = 36f
  private const val FOOTER_RESERVED_Y = PAGE_HEIGHT - 44f

  /**
   * Builds a normalized [FinancialReportData] snapshot for the selected scope and content options.
   */
  fun buildReportData(
    ownerName: String,
    currencyCode: String,
    selectedMonth: YearMonthKey,
    allTransactions: List<TransactionEntity>,
    allBudgets: List<BudgetEntity>,
    scope: ExportScope,
    includeTransactions: Boolean,
    includeBudgets: Boolean,
    generatedEpochDay: Long = currentEpochDay(),
  ): FinancialReportData {
    val scopedTransactions = when (scope) {
      ExportScope.SELECTED_MONTH -> FinanceCalculator.filterTransactionsByMonth(allTransactions, selectedMonth)
      ExportScope.ALL_TIME -> allTransactions
    }.sortedWith(
      compareByDescending<TransactionEntity> { it.dateEpochDay }
        .thenByDescending { it.createdAtMillis },
    )

    val scopedBudgets = when (scope) {
      ExportScope.SELECTED_MONTH -> FinanceCalculator.buildBudgetSummaries(
        budgets = allBudgets,
        transactions = allTransactions,
        yearMonth = selectedMonth,
      )
      ExportScope.ALL_TIME -> {
        val distinctMonths = allBudgets
          .map { YearMonthKey(it.year, it.month) }
          .distinct()
          .sortedWith(compareByDescending<YearMonthKey> { it.year }.thenByDescending { it.month })
        distinctMonths.flatMap { ym ->
          FinanceCalculator.buildBudgetSummaries(
            budgets = allBudgets,
            transactions = allTransactions,
            yearMonth = ym,
          )
        }
      }
    }

    val totalIncome = when (scope) {
      ExportScope.SELECTED_MONTH -> FinanceCalculator.calculateTotalIncome(allTransactions, selectedMonth)
      ExportScope.ALL_TIME -> FinanceCalculator.calculateAllTimeTotalIncome(allTransactions)
    }
    val totalExpenses = when (scope) {
      ExportScope.SELECTED_MONTH -> FinanceCalculator.calculateTotalExpenses(allTransactions, selectedMonth)
      ExportScope.ALL_TIME -> FinanceCalculator.calculateAllTimeTotalExpenses(allTransactions)
    }
    val netCashFlow = FinanceCalculator.calculateNetCashFlow(totalIncome, totalExpenses)
    val savingsRate = FinanceCalculator.calculateSavingsRatePercent(totalIncome, totalExpenses)

    val periodLabel = when (scope) {
      ExportScope.SELECTED_MONTH -> selectedMonth.displayLabel()
      ExportScope.ALL_TIME -> "All Time"
    }
    val periodSlug = when (scope) {
      ExportScope.SELECTED_MONTH -> String.format(Locale.US, "%04d-%02d", selectedMonth.year, selectedMonth.month)
      ExportScope.ALL_TIME -> "AllTime"
    }

    return FinancialReportData(
      ownerName = ownerName.trim().ifBlank { "BudgetWise User" },
      currencyCode = CurrencyOption.fromCode(currencyCode).code,
      periodLabel = periodLabel,
      periodSlug = periodSlug,
      generatedDateLabel = formatEpochDay(generatedEpochDay, "MMM d, yyyy"),
      totalIncome = totalIncome,
      totalExpenses = totalExpenses,
      netCashFlow = netCashFlow,
      savingsRatePercent = savingsRate,
      transactions = if (includeTransactions) scopedTransactions else emptyList(),
      budgets = if (includeBudgets) scopedBudgets else emptyList(),
      includeTransactions = includeTransactions,
      includeBudgets = includeBudgets,
    )
  }

  /**
   * Produces a descriptive, filesystem-safe filename for the requested export.
   */
  fun suggestedFileName(data: FinancialReportData, format: ExportFormat): String {
    val contentTag = when {
      data.includeTransactions && data.includeBudgets -> "Report"
      data.includeTransactions -> "Transactions"
      data.includeBudgets -> "Budgets"
      else -> "Report"
    }
    return "BudgetWise_${contentTag}_${data.periodSlug}.${format.extension}"
  }

  /**
   * Formats [data] as a UTF-8 CSV document with BOM for seamless import into Excel, Google Sheets,
   * and offline BI tools.
   */
  fun buildCsvReport(data: FinancialReportData): String {
    val sb = StringBuilder(4096)
    sb.append(UTF8_BOM)

    // Metadata & Summary Header
    sb.appendLine(csvRow("BudgetWise Financial Report"))
    sb.appendLine(csvRow("Account Holder", sanitizeSpreadsheetText(data.ownerName)))
    sb.appendLine(csvRow("Period", data.periodLabel))
    sb.appendLine(csvRow("Currency", data.currencyCode))
    sb.appendLine(csvRow("Generated On", data.generatedDateLabel))
    sb.appendLine(csvRow("Total Income", formatPlainDecimal(data.totalIncome)))
    sb.appendLine(csvRow("Total Expenses", formatPlainDecimal(data.totalExpenses)))
    sb.appendLine(csvRow("Net Cash Flow", formatPlainDecimal(data.netCashFlow)))
    sb.appendLine(csvRow("Savings Rate (%)", data.savingsRatePercent.toPlainString()))
    sb.appendLine()

    if (data.includeBudgets) {
      sb.appendLine(csvRow("BUDGET SUMMARY"))
      sb.appendLine(
        csvRow(
          "Month",
          "Category",
          "Limit",
          "Spent",
          "Remaining",
          "Utilization (%)",
          "Status",
          "Currency",
        ),
      )
      if (data.budgets.isEmpty()) {
        sb.appendLine(csvRow("No budgets recorded for this period"))
      } else {
        data.budgets.forEach { budget ->
          sb.appendLine(
            csvRow(
              budget.yearMonth.displayLabel(),
              sanitizeSpreadsheetText(budget.category),
              formatPlainDecimal(budget.limitAmount),
              formatPlainDecimal(budget.spentAmount),
              formatPlainDecimal(budget.remainingAmount),
              budget.utilizationPercent.toPlainString(),
              budget.healthStatus.label,
              data.currencyCode,
            ),
          )
        }
        val totalLimit = data.budgets.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.limitAmount) }
        val totalSpent = data.budgets.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.spentAmount) }
        val totalRemaining = FinanceCalculator.calculateRemainingBudget(totalLimit, totalSpent)
        val totalUtilization = FinanceCalculator.calculateBudgetUtilizationPercent(totalSpent, totalLimit)
        sb.appendLine(
          csvRow(
            "TOTAL",
            "${data.budgets.size} categories",
            formatPlainDecimal(totalLimit),
            formatPlainDecimal(totalSpent),
            formatPlainDecimal(totalRemaining),
            totalUtilization.toPlainString(),
            FinanceCalculator.determineBudgetHealth(totalUtilization).label,
            data.currencyCode,
          ),
        )
      }
      sb.appendLine()
    }

    if (data.includeTransactions) {
      sb.appendLine(csvRow("TRANSACTIONS"))
      sb.appendLine(
        csvRow(
          "Date",
          "Title",
          "Type",
          "Category",
          "Amount",
          "Signed Amount",
          "Currency",
          "Payment Method",
          "Notes",
        ),
      )
      if (data.transactions.isEmpty()) {
        sb.appendLine(csvRow("No transactions recorded for this period"))
      } else {
        data.transactions.forEach { tx ->
          val amount = FinanceCalculator.centavosToBigDecimal(tx.amountCentavos)
          val signedAmount = if (tx.type == TransactionType.EXPENSE) amount.negate() else amount
          sb.appendLine(
            csvRow(
              formatEpochDay(tx.dateEpochDay, "yyyy-MM-dd"),
              sanitizeSpreadsheetText(tx.title),
              tx.type.label,
              sanitizeSpreadsheetText(tx.category),
              formatPlainDecimal(amount),
              formatPlainDecimal(signedAmount),
              data.currencyCode,
              tx.paymentMethod.label,
              sanitizeSpreadsheetText(tx.notes),
            ),
          )
        }
      }
    }

    return sb.toString()
  }

  /**
   * Writes [data] as a UTF-8 CSV stream to [outputStream].
   */
  fun writeCsvReport(data: FinancialReportData, outputStream: OutputStream) {
    outputStream.write(buildCsvReport(data).toByteArray(Charsets.UTF_8))
    outputStream.flush()
  }

  /**
   * Renders [data] as a styled, multi-page A4 PDF financial statement into [outputStream] using
   * Android's native [PdfDocument] API.
   */
  fun writePdfReport(data: FinancialReportData, outputStream: OutputStream) {
    val pdf = PdfDocument()
    try {
      renderPdfReport(data, AndroidPdfDocumentSink(pdf))
      pdf.writeTo(outputStream)
      outputStream.flush()
    } finally {
      pdf.close()
    }
  }

  internal interface PdfPageSink {
    fun startPage(width: Int, height: Int, pageNumber: Int): Canvas
    fun finishPage()
  }

  private class AndroidPdfDocumentSink(private val pdf: PdfDocument) : PdfPageSink {
    private var activePage: PdfDocument.Page? = null

    override fun startPage(width: Int, height: Int, pageNumber: Int): Canvas {
      val info = PdfDocument.PageInfo.Builder(width, height, pageNumber).create()
      val page = pdf.startPage(info)
      activePage = page
      return page.canvas
    }

    override fun finishPage() {
      activePage?.let { pdf.finishPage(it) }
      activePage = null
    }
  }

  internal fun renderPdfReport(data: FinancialReportData, sink: PdfPageSink): Int {
    val writer = PdfReportWriter(sink, data)
    writer.render()
    return writer.renderedPageCount
  }

  internal fun escapeCsvField(value: String): String {
    val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
    return if (needsQuotes) {
      "\"${value.replace("\"", "\"\"")}\""
    } else {
      value
    }
  }

  internal fun sanitizeSpreadsheetText(raw: String): String {
    val cleaned = raw.replace("\r\n", " ").replace('\n', ' ').replace('\r', ' ').trim()
    if (cleaned.isEmpty()) return ""
    val first = cleaned[0]
    return if (first == '=' || first == '+' || first == '-' || first == '@') {
      "'$cleaned"
    } else {
      cleaned
    }
  }

  private fun csvRow(vararg fields: String): String =
    fields.joinToString(",") { escapeCsvField(it) }

  private fun formatPlainDecimal(amount: BigDecimal): String =
    amount.setScale(2, RoundingMode.HALF_UP).toPlainString()

  /**
   * Stateful A4 pagination & drawing helper for [writePdfReport].
   */
  private class PdfReportWriter(
    private val sink: PdfPageSink,
    private val data: FinancialReportData,
  ) {
    var renderedPageCount: Int = 0
      private set
    private var canvas: Canvas? = null
    private var cursorY = TOP_START_Y

    // Brand & semantic colors for light-paper print readability
    private val emeraldDark = Color.parseColor("#0B5E42")
    private val emeraldSoftBg = Color.parseColor("#ECFDF5")
    private val slateText = Color.parseColor("#0F1D18")
    private val mutedText = Color.parseColor("#4B5E56")
    private val borderColor = Color.parseColor("#D5E0DB")
    private val tableHeaderBg = Color.parseColor("#F1F5F3")
    private val zebraRowBg = Color.parseColor("#F8FAF9")
    private val incomeColor = Color.parseColor("#0F7653")
    private val expenseColor = Color.parseColor("#C23535")
    private val warningColor = Color.parseColor("#B45309")
    private val nearLimitColor = Color.parseColor("#C2410C")

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.WHITE
      textSize = 18f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val subtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = Color.parseColor("#D1FAE5")
      textSize = 9.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val sectionHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = emeraldDark
      textSize = 12.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = mutedText
      textSize = 8.2f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val kpiValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slateText
      textSize = 12f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slateText
      textSize = 8.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slateText
      textSize = 8.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = slateText
      textSize = 8.5f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
    }

    private val mutedBodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      color = mutedText
      textSize = 8.2f
      typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.FILL
    }

    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
      style = Paint.Style.STROKE
      strokeWidth = 0.8f
      color = borderColor
    }

    fun render() {
      startNewPage()
      drawBrandHeader()
      drawKpiRow()

      if (data.includeBudgets) {
        drawBudgetsSection()
      }
      if (data.includeTransactions) {
        drawTransactionsSection()
      }

      finishCurrentPage()
    }

    private fun startNewPage() {
      finishCurrentPage()
      renderedPageCount += 1
      canvas = sink.startPage(PAGE_WIDTH, PAGE_HEIGHT, renderedPageCount)
      cursorY = TOP_START_Y
    }

    private fun finishCurrentPage() {
      val c = canvas ?: return
      // Subtle footer divider + text
      c.drawLine(MARGIN_X, FOOTER_RESERVED_Y + 6f, PAGE_WIDTH - MARGIN_X, FOOTER_RESERVED_Y + 6f, strokePaint)
      val footerLeft = "BudgetWise · Offline Financial Statement (${data.currencyCode})"
      c.drawText(footerLeft, MARGIN_X, FOOTER_RESERVED_Y + 20f, mutedBodyPaint)
      val pageLabel = "Page $renderedPageCount"
      val pageLabelWidth = mutedBodyPaint.measureText(pageLabel)
      c.drawText(pageLabel, PAGE_WIDTH - MARGIN_X - pageLabelWidth, FOOTER_RESERVED_Y + 20f, mutedBodyPaint)

      sink.finishPage()
      canvas = null
    }

    private fun ensureSpace(requiredHeight: Float, onNewPage: (() -> Unit)? = null) {
      if (cursorY + requiredHeight > FOOTER_RESERVED_Y) {
        startNewPage()
        onNewPage?.invoke()
      }
    }

    private fun drawBrandHeader() {
      val c = canvas ?: return
      val bannerHeight = 68f
      val rect = RectF(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + bannerHeight)
      fillPaint.color = emeraldDark
      c.drawRoundRect(rect, 10f, 10f, fillPaint)

      c.drawText("BudgetWise Financial Report", MARGIN_X + 16f, cursorY + 28f, titlePaint)
      val metaLine = "Period: ${data.periodLabel}   •   Account: ${data.ownerName}   •   Currency: ${data.currencyCode}"
      c.drawText(
        ellipsizeText(metaLine, subtitlePaint, CONTENT_WIDTH - 32f),
        MARGIN_X + 16f,
        cursorY + 46f,
        subtitlePaint,
      )
      val generatedLine = "Generated on ${data.generatedDateLabel}"
      c.drawText(generatedLine, MARGIN_X + 16f, cursorY + 59f, subtitlePaint)

      cursorY += bannerHeight + 16f
    }

    private fun drawKpiRow() {
      val c = canvas ?: return
      val cardGap = 10f
      val cardWidth = (CONTENT_WIDTH - cardGap * 3f) / 4f
      val cardHeight = 48f

      val items = listOf(
        Triple("TOTAL INCOME", formatPdfAmount(data.totalIncome), incomeColor),
        Triple("TOTAL EXPENSES", formatPdfAmount(data.totalExpenses), expenseColor),
        Triple(
          "NET CASH FLOW",
          formatPdfAmount(data.netCashFlow, showPlusSign = true),
          if (data.netCashFlow >= BigDecimal.ZERO) incomeColor else expenseColor,
        ),
        Triple("SAVINGS RATE", "${data.savingsRatePercent.toPlainString()}%", emeraldDark),
      )

      items.forEachIndexed { index, (label, value, accent) ->
        val left = MARGIN_X + index * (cardWidth + cardGap)
        val rect = RectF(left, cursorY, left + cardWidth, cursorY + cardHeight)
        fillPaint.color = emeraldSoftBg
        c.drawRoundRect(rect, 8f, 8f, fillPaint)
        c.drawRoundRect(rect, 8f, 8f, strokePaint)

        c.drawText(label, left + 10f, cursorY + 16f, labelPaint)
        kpiValuePaint.color = accent
        c.drawText(
          ellipsizeText(value, kpiValuePaint, cardWidth - 20f),
          left + 10f,
          cursorY + 35f,
          kpiValuePaint,
        )
      }
      kpiValuePaint.color = slateText
      cursorY += cardHeight + 20f
    }

    private fun drawBudgetsSection() {
      ensureSpace(64f)
      drawSectionHeading("Budget Performance (${data.budgets.size})")

      if (data.budgets.isEmpty()) {
        drawEmptySectionMessage("No budgets recorded for ${data.periodLabel}.")
        return
      }

      drawBudgetTableHeader()

      val rowHeight = 20f
      data.budgets.forEachIndexed { index, budget ->
        ensureSpace(rowHeight + 4f) {
          drawSectionHeading("Budget Performance (continued)")
          drawBudgetTableHeader()
        }
        val c = canvas ?: return
        if (index % 2 == 1) {
          fillPaint.color = zebraRowBg
          c.drawRect(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + rowHeight, fillPaint)
        }

        val textY = cursorY + 13.5f
        val xMonth = MARGIN_X + 6f
        val xCat = MARGIN_X + 78f
        val xSpent = MARGIN_X + 270f
        val xLimit = MARGIN_X + 350f
        val xPct = MARGIN_X + 412f
        val xStatus = MARGIN_X + 426f

        c.drawText(ellipsizeText(budget.yearMonth.shortLabel() + " " + budget.yearMonth.year, bodyPaint, 68f), xMonth, textY, bodyPaint)
        c.drawText(ellipsizeText(compactCategoryLabel(budget.category), bodyPaint, 128f), xCat, textY, bodyPaint)
        drawRightAligned(c, formatPdfAmount(budget.spentAmount), xSpent, textY, bodyPaint)
        drawRightAligned(c, formatPdfAmount(budget.limitAmount), xLimit, textY, bodyPaint)
        drawRightAligned(c, "${budget.utilizationPercent.toPlainString()}%", xPct, textY, bodyBoldPaint)

        val statusPaint = Paint(bodyBoldPaint).apply {
          color = when (budget.healthStatus) {
            BudgetHealthStatus.NORMAL -> incomeColor
            BudgetHealthStatus.APPROACHING -> warningColor
            BudgetHealthStatus.NEAR_LIMIT -> nearLimitColor
            BudgetHealthStatus.EXCEEDED -> expenseColor
          }
        }
        c.drawText(ellipsizeText(budget.healthStatus.label, statusPaint, PAGE_WIDTH - MARGIN_X - xStatus - 4f), xStatus, textY, statusPaint)

        cursorY += rowHeight
        c.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, strokePaint)
      }

      // Summary total row
      ensureSpace(24f)
      val c = canvas ?: return
      val totalLimit = data.budgets.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.limitAmount) }
      val totalSpent = data.budgets.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.spentAmount) }
      val totalUtil = FinanceCalculator.calculateBudgetUtilizationPercent(totalSpent, totalLimit)

      fillPaint.color = tableHeaderBg
      c.drawRect(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + 22f, fillPaint)
      val textY = cursorY + 14.5f
      c.drawText("TOTAL", MARGIN_X + 6f, textY, bodyBoldPaint)
      c.drawText("${data.budgets.size} categories", MARGIN_X + 78f, textY, mutedBodyPaint)
      drawRightAligned(c, formatPdfAmount(totalSpent), MARGIN_X + 270f, textY, bodyBoldPaint)
      drawRightAligned(c, formatPdfAmount(totalLimit), MARGIN_X + 350f, textY, bodyBoldPaint)
      drawRightAligned(c, "${totalUtil.toPlainString()}%", MARGIN_X + 412f, textY, bodyBoldPaint)
      c.drawText(FinanceCalculator.determineBudgetHealth(totalUtil).label, MARGIN_X + 426f, textY, bodyBoldPaint)
      cursorY += 22f
      c.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, strokePaint)
      cursorY += 18f
    }

    private fun drawBudgetTableHeader() {
      val c = canvas ?: return
      val h = 21f
      fillPaint.color = tableHeaderBg
      c.drawRect(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + h, fillPaint)
      val y = cursorY + 14f
      c.drawText("Month", MARGIN_X + 6f, y, tableHeaderPaint)
      c.drawText("Category", MARGIN_X + 78f, y, tableHeaderPaint)
      drawRightAligned(c, "Spent", MARGIN_X + 270f, y, tableHeaderPaint)
      drawRightAligned(c, "Limit", MARGIN_X + 350f, y, tableHeaderPaint)
      drawRightAligned(c, "Used", MARGIN_X + 412f, y, tableHeaderPaint)
      c.drawText("Status", MARGIN_X + 426f, y, tableHeaderPaint)
      cursorY += h
      c.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, strokePaint)
    }

    private fun drawTransactionsSection() {
      ensureSpace(64f)
      drawSectionHeading("Financial Transactions (${data.transactions.size})")

      if (data.transactions.isEmpty()) {
        drawEmptySectionMessage("No transactions recorded for ${data.periodLabel}.")
        return
      }

      drawTransactionTableHeader()

      val rowHeight = 20f
      data.transactions.forEachIndexed { index, tx ->
        ensureSpace(rowHeight + 4f) {
          drawSectionHeading("Financial Transactions (continued)")
          drawTransactionTableHeader()
        }
        val c = canvas ?: return
        if (index % 2 == 1) {
          fillPaint.color = zebraRowBg
          c.drawRect(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + rowHeight, fillPaint)
        }

        val textY = cursorY + 13.5f
        val xDate = MARGIN_X + 6f
        val xTitle = MARGIN_X + 68f
        val xCategory = MARGIN_X + 230f
        val xMethod = MARGIN_X + 345f
        val xAmountRight = PAGE_WIDTH - MARGIN_X - 6f

        val isIncome = tx.type == TransactionType.INCOME
        val amount = FinanceCalculator.centavosToBigDecimal(tx.amountCentavos)
        val formattedAmount = formatPdfAmount(
          amount = amount,
          showPlusSign = isIncome,
          forceMinusSign = !isIncome,
        )
        val amountPaint = Paint(bodyBoldPaint).apply {
          color = if (isIncome) incomeColor else expenseColor
        }

        c.drawText(formatEpochDay(tx.dateEpochDay, "yyyy-MM-dd"), xDate, textY, mutedBodyPaint)
        c.drawText(ellipsizeText(tx.title, bodyPaint, 154f), xTitle, textY, bodyPaint)
        c.drawText(ellipsizeText(compactCategoryLabel(tx.category), bodyPaint, 108f), xCategory, textY, bodyPaint)
        c.drawText(ellipsizeText(tx.paymentMethod.label, mutedBodyPaint, 78f), xMethod, textY, mutedBodyPaint)
        drawRightAligned(c, formattedAmount, xAmountRight, textY, amountPaint)

        cursorY += rowHeight
        c.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, strokePaint)
      }
      cursorY += 12f
    }

    private fun drawTransactionTableHeader() {
      val c = canvas ?: return
      val h = 21f
      fillPaint.color = tableHeaderBg
      c.drawRect(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY + h, fillPaint)
      val y = cursorY + 14f
      c.drawText("Date", MARGIN_X + 6f, y, tableHeaderPaint)
      c.drawText("Title", MARGIN_X + 68f, y, tableHeaderPaint)
      c.drawText("Category", MARGIN_X + 230f, y, tableHeaderPaint)
      c.drawText("Payment", MARGIN_X + 345f, y, tableHeaderPaint)
      drawRightAligned(c, "Amount", PAGE_WIDTH - MARGIN_X - 6f, y, tableHeaderPaint)
      cursorY += h
      c.drawLine(MARGIN_X, cursorY, PAGE_WIDTH - MARGIN_X, cursorY, strokePaint)
    }

    private fun drawSectionHeading(text: String) {
      val c = canvas ?: return
      c.drawText(text, MARGIN_X, cursorY + 12f, sectionHeaderPaint)
      cursorY += 20f
    }

    private fun drawEmptySectionMessage(message: String) {
      val c = canvas ?: return
      c.drawText(message, MARGIN_X + 6f, cursorY + 12f, mutedBodyPaint)
      cursorY += 26f
    }

    private fun drawRightAligned(c: Canvas, text: String, rightX: Float, baselineY: Float, paint: Paint) {
      val width = paint.measureText(text)
      c.drawText(text, rightX - width, baselineY, paint)
    }

    private fun formatPdfAmount(
      amount: BigDecimal,
      showPlusSign: Boolean = false,
      forceMinusSign: Boolean = false,
    ): String {
      return FinanceCalculator.formatCurrency(
        amount = amount,
        currencyCode = data.currencyCode,
        showPlusSign = showPlusSign,
        forceMinusSign = forceMinusSign,
      )
    }

    private fun ellipsizeText(text: String, paint: Paint, maxWidth: Float): String {
      if (text.isEmpty() || paint.measureText(text) <= maxWidth) return text
      val ellipsis = "…"
      val ellipsisWidth = paint.measureText(ellipsis)
      if (ellipsisWidth >= maxWidth) return ellipsis
      var low = 0
      var high = text.length
      while (low < high) {
        val mid = (low + high + 1) / 2
        if (paint.measureText(text, 0, mid) + ellipsisWidth <= maxWidth) {
          low = mid
        } else {
          high = mid - 1
        }
      }
      return text.substring(0, low).trimEnd() + ellipsis
    }
  }
}
