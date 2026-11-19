package com.example.domain

import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

private val UTC_TIME_ZONE: TimeZone = TimeZone.getTimeZone("UTC")

enum class TransactionType(val label: String) {
  INCOME("Income"),
  EXPENSE("Expense"),
}

enum class PaymentMethod(val label: String) {
  CASH("Cash"),
  GCASH("GCash"),
  MAYA("Maya"),
  DEBIT_CARD("Debit Card"),
  CREDIT_CARD("Credit Card"),
  BANK_TRANSFER("Bank Transfer"),
}

enum class GoalStatus(val label: String) {
  ACTIVE("Active"),
  COMPLETED("Completed"),
  PAUSED("Paused"),
}

enum class CurrencyOption(
  val code: String,
  val symbol: String,
  val displayName: String,
  val fractionDigits: Int,
) {
  PHP("PHP", "₱", "Philippine Peso (₱)", 2),
  USD("USD", "$", "US Dollar ($)", 2),
  KRW("KRW", "₩", "South Korean Won (₩)", 0),
  JPY("JPY", "¥", "Japanese Yen (¥)", 0),
  EUR("EUR", "€", "Euro (€)", 2),
  GBP("GBP", "£", "British Pound (£)", 2),
  SGD("SGD", "S$", "Singapore Dollar (S$)", 2),
  AUD("AUD", "A$", "Australian Dollar (A$)", 2),
  CAD("CAD", "C$", "Canadian Dollar (C$)", 2),
  CNY("CNY", "CN¥", "Chinese Yuan (CN¥)", 2),
  HKD("HKD", "HK$", "Hong Kong Dollar (HK$)", 2),
  TWD("TWD", "NT$", "New Taiwan Dollar (NT$)", 0),
  THB("THB", "฿", "Thai Baht (฿)", 2),
  MYR("MYR", "RM", "Malaysian Ringgit (RM)", 2),
  IDR("IDR", "Rp", "Indonesian Rupiah (Rp)", 0),
  VND("VND", "₫", "Vietnamese Dong (₫)", 0),
  INR("INR", "₹", "Indian Rupee (₹)", 2),
  CHF("CHF", "CHF", "Swiss Franc (CHF)", 2);

  companion object {
    fun fromCode(code: String): CurrencyOption =
      entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: PHP
  }
}

enum class ThemeMode(val label: String) {
  SYSTEM("System default"),
  LIGHT("Light"),
  DARK("Dark"),
}

enum class BudgetHealthStatus(val label: String) {
  NORMAL("On track"),
  APPROACHING("Approaching limit"),
  NEAR_LIMIT("Near limit"),
  EXCEEDED("Exceeded"),
}

data class YearMonthKey(val year: Int, val month: Int) {
  init {
    require(month in 1..12) { "Month must be 1..12" }
  }

  fun previous(): YearMonthKey =
    if (month == 1) YearMonthKey(year - 1, 12) else YearMonthKey(year, month - 1)

  fun next(): YearMonthKey =
    if (month == 12) YearMonthKey(year + 1, 1) else YearMonthKey(year, month + 1)

  fun displayLabel(): String {
    val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM yyyy", Locale.getDefault()).apply {
      timeZone = UTC_TIME_ZONE
    }.format(cal.time)
  }

  fun shortLabel(): String {
    val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMM", Locale.getDefault()).apply {
      timeZone = UTC_TIME_ZONE
    }.format(cal.time)
  }

  fun monthNameOnly(): String {
    val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
      clear()
      set(Calendar.YEAR, year)
      set(Calendar.MONTH, month - 1)
      set(Calendar.DAY_OF_MONTH, 1)
    }
    return SimpleDateFormat("MMMM", Locale.getDefault()).apply {
      timeZone = UTC_TIME_ZONE
    }.format(cal.time)
  }

  fun monthNameUpper(): String = monthNameOnly().uppercase(Locale.getDefault())

  companion object {
    fun current(): YearMonthKey {
      val cal = Calendar.getInstance()
      return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }

    fun fromEpochDay(epochDay: Long): YearMonthKey {
      val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
        timeInMillis = epochDay * MILLIS_PER_DAY
      }
      return YearMonthKey(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
    }
  }
}

const val MILLIS_PER_DAY = 86_400_000L

fun currentEpochDay(): Long {
  val localCal = Calendar.getInstance()
  return epochDayFromDate(
    year = localCal.get(Calendar.YEAR),
    monthOneBased = localCal.get(Calendar.MONTH) + 1,
    dayOfMonth = localCal.get(Calendar.DAY_OF_MONTH),
  )
}

fun epochDayFromDate(year: Int, monthOneBased: Int, dayOfMonth: Int): Long {
  val utcCal = Calendar.getInstance(UTC_TIME_ZONE).apply {
    clear()
    set(Calendar.YEAR, year)
    set(Calendar.MONTH, monthOneBased - 1)
    set(Calendar.DAY_OF_MONTH, dayOfMonth)
  }
  return Math.floorDiv(utcCal.timeInMillis, MILLIS_PER_DAY)
}

fun formatEpochDay(epochDay: Long, pattern: String = "MMM d, yyyy"): String {
  val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
    timeInMillis = epochDay * MILLIS_PER_DAY
  }
  return SimpleDateFormat(pattern, Locale.getDefault()).apply {
    timeZone = UTC_TIME_ZONE
  }.format(cal.time)
}

fun dayOfMonthFromEpochDay(epochDay: Long): Int {
  val cal = Calendar.getInstance(UTC_TIME_ZONE).apply {
    timeInMillis = epochDay * MILLIS_PER_DAY
  }
  return cal.get(Calendar.DAY_OF_MONTH)
}

data class BudgetSummary(
  val budgetId: Long,
  val category: String,
  val yearMonth: YearMonthKey,
  val limitAmount: BigDecimal,
  val spentAmount: BigDecimal,
  val remainingAmount: BigDecimal,
  val utilizationPercent: BigDecimal,
  val healthStatus: BudgetHealthStatus,
  val isSampleData: Boolean,
)

data class SavingsGoalSummary(
  val goalId: Long,
  val name: String,
  val targetAmount: BigDecimal,
  val savedAmount: BigDecimal,
  val remainingAmount: BigDecimal,
  val completionPercent: BigDecimal,
  val targetDateEpochDay: Long,
  val createdDateEpochDay: Long,
  val status: GoalStatus,
  val isSampleData: Boolean,
)

data class CategoryBreakdown(
  val category: String,
  val amount: BigDecimal,
  val percentage: BigDecimal,
  val colorHex: Long,
)

data class MonthlyCashFlowPoint(
  val yearMonth: YearMonthKey,
  val income: BigDecimal,
  val expenses: BigDecimal,
)

data class DailySpendingPoint(
  val dayOfMonth: Int,
  val amount: BigDecimal,
)

data class MonthlySpendingPoint(
  val yearMonth: YearMonthKey,
  val amount: BigDecimal,
)

enum class InsightSeverity {
  POSITIVE,
  INFO,
  WARNING,
  ALERT,
}

data class FinancialInsight(
  val title: String,
  val message: String,
  val severity: InsightSeverity,
)

enum class TransactionSortOrder(val label: String) {
  DATE_DESC("Newest first"),
  DATE_ASC("Oldest first"),
  AMOUNT_DESC("Highest amount"),
  AMOUNT_ASC("Lowest amount"),
}

fun compactCategoryLabel(category: String): String = when (category) {
  "Food and Dining" -> "Food & Dining"
  "Bills and Utilities" -> "Bills & Utilities"
  else -> category.replace(" and ", " & ")
}

enum class ChatRole {
  USER,
  MODEL,
}
