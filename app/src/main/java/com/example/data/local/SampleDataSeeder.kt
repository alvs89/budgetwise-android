package com.example.data.local

import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.epochDayFromDate
import java.util.Calendar

object SampleDataSeeder {

  fun defaultCategories(): List<CategoryEntity> = listOf(
    // Income
    CategoryEntity(name = "Allowance", type = TransactionType.INCOME, iconKey = "wallet", colorHex = 0xFF15803D, isDefault = true),
    CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconKey = "work", colorHex = 0xFF10B981, isDefault = true),
    CategoryEntity(name = "Freelance", type = TransactionType.INCOME, iconKey = "laptop", colorHex = 0xFF0D9488, isDefault = true),
    CategoryEntity(name = "Business", type = TransactionType.INCOME, iconKey = "store", colorHex = 0xFF2563EB, isDefault = true),
    CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconKey = "payments", colorHex = 0xFF64748B, isDefault = true),
    // Expense
    CategoryEntity(name = "Food and Dining", type = TransactionType.EXPENSE, iconKey = "restaurant", colorHex = 0xFF0F766E, isDefault = true),
    CategoryEntity(name = "Transportation", type = TransactionType.EXPENSE, iconKey = "directions_bus", colorHex = 0xFF1D4ED8, isDefault = true),
    CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconKey = "school", colorHex = 0xFF6D28D9, isDefault = true),
    CategoryEntity(name = "Bills and Utilities", type = TransactionType.EXPENSE, iconKey = "receipt", colorHex = 0xFFD97706, isDefault = true),
    CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorHex = 0xFFDB2777, isDefault = true),
    CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconKey = "movie", colorHex = 0xFF0284C7, isDefault = true),
    CategoryEntity(name = "Healthcare", type = TransactionType.EXPENSE, iconKey = "medical", colorHex = 0xFFDC2626, isDefault = true),
    CategoryEntity(name = "Housing", type = TransactionType.EXPENSE, iconKey = "home", colorHex = 0xFF3B82F6, isDefault = true),
    CategoryEntity(name = "Personal Care", type = TransactionType.EXPENSE, iconKey = "spa", colorHex = 0xFF059669, isDefault = true),
    CategoryEntity(name = "Other Expenses", type = TransactionType.EXPENSE, iconKey = "category", colorHex = 0xFF64748B, isDefault = true),
  )

  fun sampleTransactions(referenceMonth: YearMonthKey = YearMonthKey.current()): List<TransactionEntity> {
    val prev = referenceMonth.previous()
    val twoAgo = prev.previous()
    val maxCurrDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH).coerceAtLeast(15)
    fun currDay(day: Int): Long = epochDayFromDate(referenceMonth.year, referenceMonth.month, day.coerceAtMost(maxCurrDay))
    fun prevDay(day: Int): Long = epochDayFromDate(prev.year, prev.month, day.coerceAtMost(28))
    fun twoAgoDay(day: Int): Long = epochDayFromDate(twoAgo.year, twoAgo.month, day.coerceAtMost(28))

    return listOf(
      // --- Current Month Income (Total: ₱28,500.00) ---
      TransactionEntity(
        title = "Monthly Allowance",
        amountCentavos = 12_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = currDay(1),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Monthly family support",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Part-Time UI Project",
        amountCentavos = 10_500_00L,
        type = TransactionType.INCOME,
        category = "Freelance",
        dateEpochDay = currDay(6),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Landing page prototype milestone",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Campus Tutoring Stipend",
        amountCentavos = 6_000_00L,
        type = TransactionType.INCOME,
        category = "Salary",
        dateEpochDay = currDay(14),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Data structures lab assistant pay",
        isSampleData = true,
      ),

      // --- Current Month Expenses (Total: ₱15,120.00) ---
      // Food and Dining: ₱3,680.00 (Budget ₱4,500 -> 81.8% Approaching)
      TransactionEntity(
        title = "Weekly Groceries",
        amountCentavos = 2_150_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = currDay(2),
        paymentMethod = PaymentMethod.DEBIT_CARD,
        notes = "Fresh produce & pantry staples",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Campus Cafeteria & Coffee",
        amountCentavos = 680_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = currDay(8),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Study group lunches",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Dinner with Friends",
        amountCentavos = 850_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = currDay(15),
        paymentMethod = PaymentMethod.CASH,
        notes = "Friday meal out",
        isSampleData = true,
      ),
      // Transportation: ₱2,180.00 (Budget ₱2,000 -> 109.0% Exceeded)
      TransactionEntity(
        title = "Beep Card & MRT Load",
        amountCentavos = 980_00L,
        type = TransactionType.EXPENSE,
        category = "Transportation",
        dateEpochDay = currDay(3),
        paymentMethod = PaymentMethod.MAYA,
        notes = "Monthly commute pass",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Grab & Tricycle Rides",
        amountCentavos = 1_200_00L,
        type = TransactionType.EXPENSE,
        category = "Transportation",
        dateEpochDay = currDay(12),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Rainy evening commute",
        isSampleData = true,
      ),
      // Education: ₱2,800.00 (Budget ₱3,000 -> 93.3% Near Limit)
      TransactionEntity(
        title = "Algorithms Textbook & Printing",
        amountCentavos = 1_850_00L,
        type = TransactionType.EXPENSE,
        category = "Education",
        dateEpochDay = currDay(4),
        paymentMethod = PaymentMethod.DEBIT_CARD,
        notes = "Semester reference materials",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Cloud Lab Subscription",
        amountCentavos = 950_00L,
        type = TransactionType.EXPENSE,
        category = "Education",
        dateEpochDay = currDay(10),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        notes = "Portfolio hosting & dev tools",
        isSampleData = true,
      ),
      // Bills and Utilities: ₱2,650.00 (Budget ₱3,500 -> 75.7% Approaching)
      TransactionEntity(
        title = "Fiber Internet Share",
        amountCentavos = 1_650_00L,
        type = TransactionType.EXPENSE,
        category = "Bills and Utilities",
        dateEpochDay = currDay(5),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Monthly broadband split",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Mobile Data & Electricity",
        amountCentavos = 1_000_00L,
        type = TransactionType.EXPENSE,
        category = "Bills and Utilities",
        dateEpochDay = currDay(11),
        paymentMethod = PaymentMethod.MAYA,
        notes = "Prepaid promo + dorm utilities",
        isSampleData = true,
      ),
      // Shopping: ₱1,890.00 (Budget ₱3,500 -> 54.0% Normal)
      TransactionEntity(
        title = "Desk Lamp & Ergonomic Mouse",
        amountCentavos = 1_890_00L,
        type = TransactionType.EXPENSE,
        category = "Shopping",
        dateEpochDay = currDay(9),
        paymentMethod = PaymentMethod.DEBIT_CARD,
        notes = "Study desk upgrade",
        isSampleData = true,
      ),
      // Entertainment: ₱1,120.00 (Budget ₱2,000 -> 56.0% Normal)
      TransactionEntity(
        title = "Cinema & Streaming Bundle",
        amountCentavos = 1_120_00L,
        type = TransactionType.EXPENSE,
        category = "Entertainment",
        dateEpochDay = currDay(13),
        paymentMethod = PaymentMethod.CREDIT_CARD,
        notes = "Weekend movie + music plan",
        isSampleData = true,
      ),
      // Healthcare: ₱800.00
      TransactionEntity(
        title = "Vitamins & Pharmacy",
        amountCentavos = 800_00L,
        type = TransactionType.EXPENSE,
        category = "Healthcare",
        dateEpochDay = currDay(7),
        paymentMethod = PaymentMethod.CASH,
        notes = "Monthly multivitamins",
        isSampleData = true,
      ),

      // --- Previous Month Records (Income ₱24,000.00, Expenses ₱13,450.00) ---
      TransactionEntity(
        title = "Monthly Allowance",
        amountCentavos = 12_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = prevDay(1),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Previous month allowance",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Mobile App Bug Fix Gig",
        amountCentavos = 12_000_00L,
        type = TransactionType.INCOME,
        category = "Freelance",
        dateEpochDay = prevDay(15),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Client freelance sprint",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Groceries & Meal Prep",
        amountCentavos = 4_100_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = prevDay(5),
        paymentMethod = PaymentMethod.DEBIT_CARD,
        notes = "Previous month groceries",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Dormitory Rent Share",
        amountCentavos = 4_500_00L,
        type = TransactionType.EXPENSE,
        category = "Housing",
        dateEpochDay = prevDay(3),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Monthly housing share",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Commute & Rail Passes",
        amountCentavos = 1_750_00L,
        type = TransactionType.EXPENSE,
        category = "Transportation",
        dateEpochDay = prevDay(11),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Daily commute expenses",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Utilities & Internet",
        amountCentavos = 2_100_00L,
        type = TransactionType.EXPENSE,
        category = "Bills and Utilities",
        dateEpochDay = prevDay(18),
        paymentMethod = PaymentMethod.MAYA,
        notes = "Internet and power bill",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Haircut & Essentials",
        amountCentavos = 1_000_00L,
        type = TransactionType.EXPENSE,
        category = "Personal Care",
        dateEpochDay = prevDay(22),
        paymentMethod = PaymentMethod.CASH,
        notes = "Personal care essentials",
        isSampleData = true,
      ),

      // --- Two Months Ago Records for Multi-Month Trend Chart ---
      TransactionEntity(
        title = "Monthly Allowance",
        amountCentavos = 12_000_00L,
        type = TransactionType.INCOME,
        category = "Allowance",
        dateEpochDay = twoAgoDay(1),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Allowance",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Tutoring & Design Gig",
        amountCentavos = 9_500_00L,
        type = TransactionType.INCOME,
        category = "Freelance",
        dateEpochDay = twoAgoDay(12),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Design freelance",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Food & Campus Meals",
        amountCentavos = 4_400_00L,
        type = TransactionType.EXPENSE,
        category = "Food and Dining",
        dateEpochDay = twoAgoDay(8),
        paymentMethod = PaymentMethod.GCASH,
        notes = "Monthly meals",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Dormitory & Utilities",
        amountCentavos = 6_400_00L,
        type = TransactionType.EXPENSE,
        category = "Housing",
        dateEpochDay = twoAgoDay(4),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
        notes = "Rent & utilities",
        isSampleData = true,
      ),
      TransactionEntity(
        title = "Commute Expenses",
        amountCentavos = 1_650_00L,
        type = TransactionType.EXPENSE,
        category = "Transportation",
        dateEpochDay = twoAgoDay(16),
        paymentMethod = PaymentMethod.CASH,
        notes = "Transit pass",
        isSampleData = true,
      ),
    )
  }

  fun sampleBudgets(referenceMonth: YearMonthKey = YearMonthKey.current()): List<BudgetEntity> {
    val prev = referenceMonth.previous()
    return listOf(
      // Current month (6 budgets covering Normal, Approaching, Near Limit, Exceeded)
      BudgetEntity(category = "Food and Dining", limitCentavos = 4_500_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      BudgetEntity(category = "Transportation", limitCentavos = 2_000_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      BudgetEntity(category = "Education", limitCentavos = 3_000_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      BudgetEntity(category = "Bills and Utilities", limitCentavos = 3_500_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      BudgetEntity(category = "Shopping", limitCentavos = 3_500_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      BudgetEntity(category = "Entertainment", limitCentavos = 2_000_00L, year = referenceMonth.year, month = referenceMonth.month, isSampleData = true),
      // Previous month budgets
      BudgetEntity(category = "Food and Dining", limitCentavos = 4_500_00L, year = prev.year, month = prev.month, isSampleData = true),
      BudgetEntity(category = "Housing", limitCentavos = 5_000_00L, year = prev.year, month = prev.month, isSampleData = true),
      BudgetEntity(category = "Transportation", limitCentavos = 2_000_00L, year = prev.year, month = prev.month, isSampleData = true),
      BudgetEntity(category = "Bills and Utilities", limitCentavos = 2_500_00L, year = prev.year, month = prev.month, isSampleData = true),
    )
  }

  fun sampleGoals(referenceMonth: YearMonthKey = YearMonthKey.current()): List<SavingsGoalEntity> {
    val created = epochDayFromDate(referenceMonth.previous().year, referenceMonth.previous().month, 1)
    val nextYear = YearMonthKey(referenceMonth.year + 1, referenceMonth.month)
    return listOf(
      SavingsGoalEntity(
        name = "Emergency Fund",
        targetCentavos = 30_000_00L,
        currentSavedCentavos = 18_500_00L,
        targetDateEpochDay = epochDayFromDate(nextYear.year, nextYear.month, 15),
        createdDateEpochDay = created,
        status = GoalStatus.ACTIVE,
        isSampleData = true,
      ),
      SavingsGoalEntity(
        name = "New Laptop",
        targetCentavos = 45_000_00L,
        currentSavedCentavos = 27_000_00L,
        targetDateEpochDay = epochDayFromDate(nextYear.year, ((referenceMonth.month + 5) % 12) + 1, 20),
        createdDateEpochDay = created,
        status = GoalStatus.ACTIVE,
        isSampleData = true,
      ),
      SavingsGoalEntity(
        name = "Travel Fund",
        targetCentavos = 15_000_00L,
        currentSavedCentavos = 6_250_00L,
        targetDateEpochDay = epochDayFromDate(nextYear.year, ((referenceMonth.month + 3) % 12) + 1, 10),
        createdDateEpochDay = created,
        status = GoalStatus.ACTIVE,
        isSampleData = true,
      ),
      SavingsGoalEntity(
        name = "Educational Expenses",
        targetCentavos = 12_000_00L,
        currentSavedCentavos = 12_000_00L,
        targetDateEpochDay = epochDayFromDate(referenceMonth.year, referenceMonth.month, 25),
        createdDateEpochDay = created,
        status = GoalStatus.COMPLETED,
        isSampleData = true,
      ),
    )
  }

  fun sampleContributionsForGoals(
    goalIds: List<Long>,
    referenceMonth: YearMonthKey = YearMonthKey.current(),
  ): List<SavingsContributionEntity> {
    if (goalIds.size < 4) return emptyList()
    val prev = referenceMonth.previous()
    val d1 = epochDayFromDate(prev.year, prev.month, 5)
    val d2 = epochDayFromDate(prev.year, prev.month, 20)
    val d3 = epochDayFromDate(referenceMonth.year, referenceMonth.month, 6)
    val d4 = epochDayFromDate(referenceMonth.year, referenceMonth.month, 12)
    return listOf(
      SavingsContributionEntity(goalId = goalIds[0], amountCentavos = 10_000_00L, isWithdrawal = false, note = "Initial emergency reserve", dateEpochDay = d1),
      SavingsContributionEntity(goalId = goalIds[0], amountCentavos = 8_500_00L, isWithdrawal = false, note = "Monthly savings transfer", dateEpochDay = d3),
      SavingsContributionEntity(goalId = goalIds[1], amountCentavos = 15_000_00L, isWithdrawal = false, note = "Freelance savings allocation", dateEpochDay = d2),
      SavingsContributionEntity(goalId = goalIds[1], amountCentavos = 12_000_00L, isWithdrawal = false, note = "Top-up for dev laptop", dateEpochDay = d4),
      SavingsContributionEntity(goalId = goalIds[2], amountCentavos = 7_500_00L, isWithdrawal = false, note = "Trip fund deposit", dateEpochDay = d2),
      SavingsContributionEntity(goalId = goalIds[2], amountCentavos = 1_250_00L, isWithdrawal = true, note = "Adjustment for flight rebooking", dateEpochDay = d3),
      SavingsContributionEntity(goalId = goalIds[3], amountCentavos = 12_000_00L, isWithdrawal = false, note = "Tuition & certification reserve", dateEpochDay = d1),
    )
  }
}
