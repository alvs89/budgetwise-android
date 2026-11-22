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
    // Income (8)
    CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconKey = "work", colorHex = 0xFF10B981, isDefault = true),
    CategoryEntity(name = "Allowance", type = TransactionType.INCOME, iconKey = "wallet", colorHex = 0xFF0EA5A4, isDefault = true),
    CategoryEntity(name = "Freelance", type = TransactionType.INCOME, iconKey = "laptop", colorHex = 0xFF0284C7, isDefault = true),
    CategoryEntity(name = "Business", type = TransactionType.INCOME, iconKey = "store", colorHex = 0xFF3B82F6, isDefault = true),
    CategoryEntity(name = "Investments", type = TransactionType.INCOME, iconKey = "trending_up", colorHex = 0xFF10B981, isDefault = true),
    CategoryEntity(name = "Bonus", type = TransactionType.INCOME, iconKey = "bonus", colorHex = 0xFFEAB308, isDefault = true),
    CategoryEntity(name = "Gifts & Refunds", type = TransactionType.INCOME, iconKey = "gift", colorHex = 0xFFDB2777, isDefault = true),
    CategoryEntity(name = "Other Income", type = TransactionType.INCOME, iconKey = "payments", colorHex = 0xFF64748B, isDefault = true),
    // Expense (22)
    CategoryEntity(name = "Food and Dining", type = TransactionType.EXPENSE, iconKey = "restaurant", colorHex = 0xFF0EA5A4, isDefault = true),
    CategoryEntity(name = "Groceries", type = TransactionType.EXPENSE, iconKey = "groceries", colorHex = 0xFF10B981, isDefault = true),
    CategoryEntity(name = "Coffee & Snacks", type = TransactionType.EXPENSE, iconKey = "coffee", colorHex = 0xFFEA580C, isDefault = true),
    CategoryEntity(name = "Transportation", type = TransactionType.EXPENSE, iconKey = "directions_bus", colorHex = 0xFF3B82F6, isDefault = true),
    CategoryEntity(name = "Car & Fuel", type = TransactionType.EXPENSE, iconKey = "car", colorHex = 0xFF0284C7, isDefault = true),
    CategoryEntity(name = "Housing", type = TransactionType.EXPENSE, iconKey = "home", colorHex = 0xFF3B82F6, isDefault = true),
    CategoryEntity(name = "Bills and Utilities", type = TransactionType.EXPENSE, iconKey = "bolt", colorHex = 0xFFEAB308, isDefault = true),
    CategoryEntity(name = "Internet & Load", type = TransactionType.EXPENSE, iconKey = "wifi", colorHex = 0xFF0284C7, isDefault = true),
    CategoryEntity(name = "Education", type = TransactionType.EXPENSE, iconKey = "school", colorHex = 0xFF7C3AED, isDefault = true),
    CategoryEntity(name = "Healthcare", type = TransactionType.EXPENSE, iconKey = "medical", colorHex = 0xFFF43F5E, isDefault = true),
    CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorHex = 0xFFDB2777, isDefault = true),
    CategoryEntity(name = "Electronics", type = TransactionType.EXPENSE, iconKey = "devices", colorHex = 0xFF6366F1, isDefault = true),
    CategoryEntity(name = "Entertainment", type = TransactionType.EXPENSE, iconKey = "movie", colorHex = 0xFF6366F1, isDefault = true),
    CategoryEntity(name = "Subscriptions", type = TransactionType.EXPENSE, iconKey = "subscriptions", colorHex = 0xFF7C3AED, isDefault = true),
    CategoryEntity(name = "Personal Care", type = TransactionType.EXPENSE, iconKey = "spa", colorHex = 0xFF10B981, isDefault = true),
    CategoryEntity(name = "Fitness & Sports", type = TransactionType.EXPENSE, iconKey = "fitness", colorHex = 0xFFEA580C, isDefault = true),
    CategoryEntity(name = "Travel", type = TransactionType.EXPENSE, iconKey = "flight", colorHex = 0xFF0284C7, isDefault = true),
    CategoryEntity(name = "Pets", type = TransactionType.EXPENSE, iconKey = "pets", colorHex = 0xFFEAB308, isDefault = true),
    CategoryEntity(name = "Gifts & Charity", type = TransactionType.EXPENSE, iconKey = "charity", colorHex = 0xFFF43F5E, isDefault = true),
    CategoryEntity(name = "Repairs", type = TransactionType.EXPENSE, iconKey = "repair", colorHex = 0xFF64748B, isDefault = true),
    CategoryEntity(name = "Family & Kids", type = TransactionType.EXPENSE, iconKey = "family", colorHex = 0xFFDB2777, isDefault = true),
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

  /**
   * Practical, multi-month realistic personal finance dataset for live account demonstrations.
   * Covers 6 consecutive months of income & expenses across varied categories and payment methods,
   * current + previous month budgets spanning every health tier (Normal, Approaching, Near Limit,
   * Exceeded), and 4 savings goals (3 active, 1 completed) with full contribution histories.
   */
  fun demonstrationDataset(
    referenceMonth: YearMonthKey = YearMonthKey.current(),
  ): DemonstrationDataset {
    val m0 = referenceMonth
    val m1 = m0.previous()
    val m2 = m1.previous()
    val m3 = m2.previous()
    val m4 = m3.previous()
    val m5 = m4.previous()

    val today = Calendar.getInstance()
    val isCurrentCalendarMonth =
      today.get(Calendar.YEAR) == m0.year && (today.get(Calendar.MONTH) + 1) == m0.month
    val maxCurrDay = if (isCurrentCalendarMonth) {
      today.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    } else {
      28
    }

    fun dayIn(ym: YearMonthKey, day: Int, cap: Int = 28): Long =
      epochDayFromDate(ym.year, ym.month, day.coerceIn(1, cap))

    fun currDay(day: Int): Long = dayIn(m0, day, maxCurrDay)

    val transactions = listOf(
      // =========================================================================
      // Current Month (m0): Income ₱64,500.00 | Expenses ₱42,480.00 | Net +₱22,020.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_01",
        entity = TransactionEntity(
          title = "1st Cutoff Net Salary",
          amountCentavos = 26_500_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = currDay(1),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Bi-monthly payroll direct deposit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_02",
        entity = TransactionEntity(
          title = "Condo Rent Share",
          amountCentavos = 12_000_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = currDay(1),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly residential lease payment",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_03",
        entity = TransactionEntity(
          title = "SM Supermarket Weekly Haul",
          amountCentavos = 2_840_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = currDay(2),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Rice, chicken breast, vegetables & pantry staples",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_04",
        entity = TransactionEntity(
          title = "Anytime Fitness Monthly Dues",
          amountCentavos = 1_500_00L,
          type = TransactionType.EXPENSE,
          category = "Fitness & Sports",
          dateEpochDay = currDay(2),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Monthly gym membership auto-debit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_05",
        entity = TransactionEntity(
          title = "Meralco Electric Bill",
          amountCentavos = 3_920_00L,
          type = TransactionType.EXPENSE,
          category = "Bills and Utilities",
          dateEpochDay = currDay(3),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Monthly residential electricity billing",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_06",
        entity = TransactionEntity(
          title = "Beep Card & MRT-3 Load",
          amountCentavos = 940_00L,
          type = TransactionType.EXPENSE,
          category = "Transportation",
          dateEpochDay = currDay(3),
          paymentMethod = PaymentMethod.MAYA,
          notes = "Weekday rail & bus commute fare load",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_07",
        entity = TransactionEntity(
          title = "Team Lunch at Manam",
          amountCentavos = 1_250_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = currDay(4),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Sprint release lunch with workmates",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_08",
        entity = TransactionEntity(
          title = "PLDT Home Fiber Plan",
          amountCentavos = 1_699_00L,
          type = TransactionType.EXPENSE,
          category = "Internet & Load",
          dateEpochDay = currDay(5),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly 200 Mbps home broadband",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_09",
        entity = TransactionEntity(
          title = "Iced Spanish Latte & Croissant",
          amountCentavos = 420_00L,
          type = TransactionType.EXPENSE,
          category = "Coffee & Snacks",
          dateEpochDay = currDay(5),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Remote work coffee shop session",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_10",
        entity = TransactionEntity(
          title = "Maynilad Water & Condo Dues",
          amountCentavos = 930_00L,
          type = TransactionType.EXPENSE,
          category = "Bills and Utilities",
          dateEpochDay = currDay(6),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly water bill and building maintenance",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_11",
        entity = TransactionEntity(
          title = "Spotify, YouTube & Cloud Storage",
          amountCentavos = 649_00L,
          type = TransactionType.EXPENSE,
          category = "Subscriptions",
          dateEpochDay = currDay(7),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Music, ad-free video, and 200GB cloud backup",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_12",
        entity = TransactionEntity(
          title = "UI/UX Design Consultation",
          amountCentavos = 9_500_00L,
          type = TransactionType.INCOME,
          category = "Freelance",
          dateEpochDay = currDay(8),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Mobile wireframe review and design system handoff",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_13",
        entity = TransactionEntity(
          title = "GrabFood Weekday Dinners",
          amountCentavos = 1_120_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = currDay(9),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Late-night meal deliveries during deadline week",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_14",
        entity = TransactionEntity(
          title = "Puregold Household Restock",
          amountCentavos = 1_680_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = currDay(10),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Laundry detergent, toiletries & breakfast oats",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_15",
        entity = TransactionEntity(
          title = "Uniqlo Work Shirts & Basics",
          amountCentavos = 2_190_00L,
          type = TransactionType.EXPENSE,
          category = "Shopping",
          dateEpochDay = currDay(11),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Sale wardrobe refresh for office days",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_16",
        entity = TransactionEntity(
          title = "Smart Postpaid & Mobile Data",
          amountCentavos = 500_00L,
          type = TransactionType.EXPENSE,
          category = "Internet & Load",
          dateEpochDay = currDay(11),
          paymentMethod = PaymentMethod.MAYA,
          notes = "Monthly mobile voice and 5G data plan",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_17",
        entity = TransactionEntity(
          title = "GrabCar Rainy Commutes",
          amountCentavos = 1_450_00L,
          type = TransactionType.EXPENSE,
          category = "Transportation",
          dateEpochDay = currDay(12),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Late evening rides home during heavy rain",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_18",
        entity = TransactionEntity(
          title = "Afternoon Matcha & Pastries",
          amountCentavos = 490_00L,
          type = TransactionType.EXPENSE,
          category = "Coffee & Snacks",
          dateEpochDay = currDay(13),
          paymentMethod = PaymentMethod.MAYA,
          notes = "Coffee catch-up with college friends",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_19",
        entity = TransactionEntity(
          title = "Sunday Family Samgyupsal",
          amountCentavos = 1_580_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = currDay(14),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Weekend dinner treat with family",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_20",
        entity = TransactionEntity(
          title = "Mercury Drug Vitamins & Checkup",
          amountCentavos = 1_450_00L,
          type = TransactionType.EXPENSE,
          category = "Healthcare",
          dateEpochDay = currDay(14),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Vitamin C, D3, and routine dental co-pay",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_21",
        entity = TransactionEntity(
          title = "2nd Cutoff Net Salary",
          amountCentavos = 26_500_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = currDay(15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Bi-monthly payroll direct deposit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_22",
        entity = TransactionEntity(
          title = "Netflix Standard Plan",
          amountCentavos = 499_00L,
          type = TransactionType.EXPENSE,
          category = "Subscriptions",
          dateEpochDay = currDay(15),
          paymentMethod = PaymentMethod.MAYA,
          notes = "Monthly streaming subscription",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_23",
        entity = TransactionEntity(
          title = "Shopee Desk Organizer & USB-C Hub",
          amountCentavos = 1_300_00L,
          type = TransactionType.EXPENSE,
          category = "Shopping",
          dateEpochDay = currDay(16),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Work-from-home cable tray and adapter",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_24",
        entity = TransactionEntity(
          title = "Barbershop Haircut & Skincare",
          amountCentavos = 1_193_00L,
          type = TransactionType.EXPENSE,
          category = "Personal Care",
          dateEpochDay = currDay(16),
          paymentMethod = PaymentMethod.CASH,
          notes = "Monthly haircut and sunscreen refill",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_25",
        entity = TransactionEntity(
          title = "Weekend Market Fresh Produce",
          amountCentavos = 900_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = currDay(17),
          paymentMethod = PaymentMethod.CASH,
          notes = "Fresh fruits, farm eggs, and cooking herbs",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_26",
        entity = TransactionEntity(
          title = "Digital Bank Interest & Cashback",
          amountCentavos = 2_000_00L,
          type = TransactionType.INCOME,
          category = "Investments",
          dateEpochDay = currDay(18),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "High-yield savings interest and card rebates",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_27",
        entity = TransactionEntity(
          title = "Angkas & Weekend Rides",
          amountCentavos = 850_00L,
          type = TransactionType.EXPENSE,
          category = "Transportation",
          dateEpochDay = currDay(18),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Quick errands across Makati & BGC",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_28",
        entity = TransactionEntity(
          title = "Cold Brew & Afternoon Merienda",
          amountCentavos = 430_00L,
          type = TransactionType.EXPENSE,
          category = "Coffee & Snacks",
          dateEpochDay = currDay(19),
          paymentMethod = PaymentMethod.CASH,
          notes = "Office coffee run and snacks",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_29",
        entity = TransactionEntity(
          title = "Weekday Office Lunches",
          amountCentavos = 700_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = currDay(20),
          paymentMethod = PaymentMethod.CASH,
          notes = "Cafeteria meals during on-site workdays",
          isSampleData = false,
        ),
      ),

      // =========================================================================
      // 1 Month Ago (m1): Income ₱60,000.00 | Expenses ₱38,950.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_30",
        entity = TransactionEntity(
          title = "Monthly Net Salary",
          amountCentavos = 53_000_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = dayIn(m1, 15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Combined bi-monthly payroll",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_31",
        entity = TransactionEntity(
          title = "Landing Page Redesign Project",
          amountCentavos = 7_000_00L,
          type = TransactionType.INCOME,
          category = "Freelance",
          dateEpochDay = dayIn(m1, 22),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Client website polish milestone",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_32",
        entity = TransactionEntity(
          title = "Condo Rent Share",
          amountCentavos = 12_000_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = dayIn(m1, 1),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly apartment lease",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_33",
        entity = TransactionEntity(
          title = "Supermarket Groceries",
          amountCentavos = 5_650_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = dayIn(m1, 7),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Monthly groceries and household supplies",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_34",
        entity = TransactionEntity(
          title = "Restaurants & Food Delivery",
          amountCentavos = 4_100_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = dayIn(m1, 13),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Weekday lunches and weekend dining",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_35",
        entity = TransactionEntity(
          title = "Electric & Water Utilities",
          amountCentavos = 4_720_00L,
          type = TransactionType.EXPENSE,
          category = "Bills and Utilities",
          dateEpochDay = dayIn(m1, 5),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Meralco + Maynilad bills",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_36",
        entity = TransactionEntity(
          title = "Fiber Internet & Mobile Plan",
          amountCentavos = 2_199_00L,
          type = TransactionType.EXPENSE,
          category = "Internet & Load",
          dateEpochDay = dayIn(m1, 6),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Broadband and mobile postpaid",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_37",
        entity = TransactionEntity(
          title = "Commute & Ride-Hailing",
          amountCentavos = 3_480_00L,
          type = TransactionType.EXPENSE,
          category = "Transportation",
          dateEpochDay = dayIn(m1, 18),
          paymentMethod = PaymentMethod.MAYA,
          notes = "MRT load, Grab, and Angkas trips",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_38",
        entity = TransactionEntity(
          title = "Online Course & Tech Books",
          amountCentavos = 2_850_00L,
          type = TransactionType.EXPENSE,
          category = "Education",
          dateEpochDay = dayIn(m1, 19),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Kotlin & system design reference books",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_39",
        entity = TransactionEntity(
          title = "Movies, Subscriptions & Gym",
          amountCentavos = 3_951_00L,
          type = TransactionType.EXPENSE,
          category = "Entertainment",
          dateEpochDay = dayIn(m1, 24),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "IMAX tickets, streaming, and fitness dues",
          isSampleData = false,
        ),
      ),

      // =========================================================================
      // 2 Months Ago (m2): Income ₱71,000.00 (with Quarterly Bonus) | Expenses ₱45,200.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_40",
        entity = TransactionEntity(
          title = "Monthly Net Salary",
          amountCentavos = 53_000_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = dayIn(m2, 15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Combined bi-monthly payroll",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_41",
        entity = TransactionEntity(
          title = "Quarterly Performance Bonus",
          amountCentavos = 18_000_00L,
          type = TransactionType.INCOME,
          category = "Bonus",
          dateEpochDay = dayIn(m2, 25),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Q3 team delivery performance incentive",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_42",
        entity = TransactionEntity(
          title = "Condo Rent Share",
          amountCentavos = 12_000_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = dayIn(m2, 1),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly lease",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_43",
        entity = TransactionEntity(
          title = "Groceries & Home Supplies",
          amountCentavos = 6_100_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = dayIn(m2, 8),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Bulk pantry restock",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_44",
        entity = TransactionEntity(
          title = "Dining & Coffee Outings",
          amountCentavos = 5_300_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = dayIn(m2, 14),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Birthday dinner and team lunches",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_45",
        entity = TransactionEntity(
          title = "Utilities & Broadband",
          amountCentavos = 6_900_00L,
          type = TransactionType.EXPENSE,
          category = "Bills and Utilities",
          dateEpochDay = dayIn(m2, 6),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Electric, water, and home fiber",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_46",
        entity = TransactionEntity(
          title = "Weekend Tagaytay & Beach Trip",
          amountCentavos = 8_400_00L,
          type = TransactionType.EXPENSE,
          category = "Travel",
          dateEpochDay = dayIn(m2, 20),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Resort stay, fuel split, and dining",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_47",
        entity = TransactionEntity(
          title = "Noise-Canceling Earbuds & Transit",
          amountCentavos = 6_500_00L,
          type = TransactionType.EXPENSE,
          category = "Electronics",
          dateEpochDay = dayIn(m2, 22),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Commute earbuds replacement + fare load",
          isSampleData = false,
        ),
      ),

      // =========================================================================
      // 3 Months Ago (m3): Income ₱58,500.00 | Expenses ₱37,100.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_48",
        entity = TransactionEntity(
          title = "Monthly Net Salary",
          amountCentavos = 53_000_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = dayIn(m3, 15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Bi-monthly payroll credit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_49",
        entity = TransactionEntity(
          title = "Weekend Technical Mentoring",
          amountCentavos = 5_500_00L,
          type = TransactionType.INCOME,
          category = "Freelance",
          dateEpochDay = dayIn(m3, 21),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Bootcamp code review sessions",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_50",
        entity = TransactionEntity(
          title = "Condo Rent Share",
          amountCentavos = 12_000_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = dayIn(m3, 1),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly apartment rent",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_51",
        entity = TransactionEntity(
          title = "Groceries & Meal Prep",
          amountCentavos = 5_800_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = dayIn(m3, 9),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Weekly supermarket runs",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_52",
        entity = TransactionEntity(
          title = "Utilities, Internet & Mobile",
          amountCentavos = 6_850_00L,
          type = TransactionType.EXPENSE,
          category = "Bills and Utilities",
          dateEpochDay = dayIn(m3, 6),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Power, water, fiber, and mobile plan",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_53",
        entity = TransactionEntity(
          title = "Dining, Commute & Healthcare",
          amountCentavos = 8_250_00L,
          type = TransactionType.EXPENSE,
          category = "Food and Dining",
          dateEpochDay = dayIn(m3, 16),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Monthly dining out, transit, and pharmacy",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_54",
        entity = TransactionEntity(
          title = "Aircon Cleaning & Home Repairs",
          amountCentavos = 4_200_00L,
          type = TransactionType.EXPENSE,
          category = "Repairs",
          dateEpochDay = dayIn(m3, 23),
          paymentMethod = PaymentMethod.CASH,
          notes = "Preventive AC maintenance and faucet fix",
          isSampleData = false,
        ),
      ),

      // =========================================================================
      // 4 Months Ago (m4): Income ₱56,000.00 | Expenses ₱36,400.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_55",
        entity = TransactionEntity(
          title = "Monthly Net Salary",
          amountCentavos = 53_000_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = dayIn(m4, 15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Payroll deposit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_56",
        entity = TransactionEntity(
          title = "Dividend & Bank Interest",
          amountCentavos = 3_000_00L,
          type = TransactionType.INCOME,
          category = "Investments",
          dateEpochDay = dayIn(m4, 24),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Quarterly REIT dividend & digital bank interest",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_57",
        entity = TransactionEntity(
          title = "Condo Rent & Utilities",
          amountCentavos = 18_400_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = dayIn(m4, 2),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Rent, electricity, water & fiber bundle",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_58",
        entity = TransactionEntity(
          title = "Groceries & Dining",
          amountCentavos = 9_600_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = dayIn(m4, 12),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Monthly food and groceries",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_59",
        entity = TransactionEntity(
          title = "Commute, Shopping & Gifts",
          amountCentavos = 8_400_00L,
          type = TransactionType.EXPENSE,
          category = "Shopping",
          dateEpochDay = dayIn(m4, 19),
          paymentMethod = PaymentMethod.CREDIT_CARD,
          notes = "Running shoes, family gift, and commute load",
          isSampleData = false,
        ),
      ),

      // =========================================================================
      // 5 Months Ago (m5): Income ₱54,500.00 | Expenses ₱35,200.00
      // =========================================================================
      DemoTransactionRecord(
        docId = "demo_tx_60",
        entity = TransactionEntity(
          title = "Monthly Net Salary",
          amountCentavos = 53_000_00L,
          type = TransactionType.INCOME,
          category = "Salary",
          dateEpochDay = dayIn(m5, 15),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Payroll deposit",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_61",
        entity = TransactionEntity(
          title = "Expense Reimbursement & Rebates",
          amountCentavos = 1_500_00L,
          type = TransactionType.INCOME,
          category = "Gifts & Refunds",
          dateEpochDay = dayIn(m5, 22),
          paymentMethod = PaymentMethod.GCASH,
          notes = "Work travel reimbursement",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_62",
        entity = TransactionEntity(
          title = "Condo Rent & Utilities",
          amountCentavos = 18_100_00L,
          type = TransactionType.EXPENSE,
          category = "Housing",
          dateEpochDay = dayIn(m5, 2),
          paymentMethod = PaymentMethod.BANK_TRANSFER,
          notes = "Monthly housing and utility bills",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_63",
        entity = TransactionEntity(
          title = "Groceries & Daily Meals",
          amountCentavos = 9_400_00L,
          type = TransactionType.EXPENSE,
          category = "Groceries",
          dateEpochDay = dayIn(m5, 11),
          paymentMethod = PaymentMethod.DEBIT_CARD,
          notes = "Monthly groceries and office meals",
          isSampleData = false,
        ),
      ),
      DemoTransactionRecord(
        docId = "demo_tx_64",
        entity = TransactionEntity(
          title = "Commute, Health & Subscriptions",
          amountCentavos = 7_700_00L,
          type = TransactionType.EXPENSE,
          category = "Transportation",
          dateEpochDay = dayIn(m5, 18),
          paymentMethod = PaymentMethod.MAYA,
          notes = "Transit load, annual clinic lab, and plans",
          isSampleData = false,
        ),
      ),
    )

    // Current Month (8 budgets across all 4 health tiers) + Previous Month (7 budgets)
    val budgets = listOf(
      // Current Month (m0)
      DemoBudgetRecord(
        docId = "demo_budget_01",
        entity = BudgetEntity(category = "Food and Dining", limitCentavos = 4_200_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱4,650 / ₱4,200 -> 110.7% EXCEEDED
      DemoBudgetRecord(
        docId = "demo_budget_02",
        entity = BudgetEntity(category = "Housing", limitCentavos = 12_500_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱12,000 / ₱12,500 -> 96.0% NEAR LIMIT
      DemoBudgetRecord(
        docId = "demo_budget_03",
        entity = BudgetEntity(category = "Groceries", limitCentavos = 6_000_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱5,420 / ₱6,000 -> 90.3% NEAR LIMIT
      DemoBudgetRecord(
        docId = "demo_budget_04",
        entity = BudgetEntity(category = "Coffee & Snacks", limitCentavos = 1_500_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱1,340 / ₱1,500 -> 89.3% APPROACHING
      DemoBudgetRecord(
        docId = "demo_budget_05",
        entity = BudgetEntity(category = "Bills and Utilities", limitCentavos = 5_500_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱4,850 / ₱5,500 -> 88.2% APPROACHING
      DemoBudgetRecord(
        docId = "demo_budget_06",
        entity = BudgetEntity(category = "Transportation", limitCentavos = 3_800_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱3,240 / ₱3,800 -> 85.3% APPROACHING
      DemoBudgetRecord(
        docId = "demo_budget_07",
        entity = BudgetEntity(category = "Shopping", limitCentavos = 5_000_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱3,490 / ₱5,000 -> 69.8% NORMAL
      DemoBudgetRecord(
        docId = "demo_budget_08",
        entity = BudgetEntity(category = "Subscriptions", limitCentavos = 1_800_00L, year = m0.year, month = m0.month, isSampleData = false),
      ), // ₱1,148 / ₱1,800 -> 63.8% NORMAL

      // Previous Month (m1)
      DemoBudgetRecord(
        docId = "demo_budget_09",
        entity = BudgetEntity(category = "Housing", limitCentavos = 12_500_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_10",
        entity = BudgetEntity(category = "Groceries", limitCentavos = 6_000_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_11",
        entity = BudgetEntity(category = "Food and Dining", limitCentavos = 4_200_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_12",
        entity = BudgetEntity(category = "Bills and Utilities", limitCentavos = 5_200_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_13",
        entity = BudgetEntity(category = "Internet & Load", limitCentavos = 2_500_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_14",
        entity = BudgetEntity(category = "Transportation", limitCentavos = 3_800_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
      DemoBudgetRecord(
        docId = "demo_budget_15",
        entity = BudgetEntity(category = "Education", limitCentavos = 3_000_00L, year = m1.year, month = m1.month, isSampleData = false),
      ),
    )

    fun futureMonth(offsetMonths: Int): YearMonthKey {
      var cursor = m0
      repeat(offsetMonths) { cursor = cursor.next() }
      return cursor
    }

    val goals = listOf(
      DemoGoalRecord(
        docId = "demo_goal_01",
        entity = SavingsGoalEntity(
          name = "Emergency Fund (6 Months)",
          targetCentavos = 150_000_00L,
          currentSavedCentavos = 112_500_00L,
          targetDateEpochDay = dayIn(futureMonth(8), 15),
          createdDateEpochDay = dayIn(m5, 2),
          status = GoalStatus.ACTIVE,
          isSampleData = false,
        ),
        contributions = listOf(
          DemoContributionRecord(
            docId = "demo_contrib_01",
            amountCentavos = 45_000_00L,
            isWithdrawal = false,
            note = "Initial emergency safety reserve",
            dateEpochDay = dayIn(m4, 16),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_02",
            amountCentavos = 25_000_00L,
            isWithdrawal = false,
            note = "Quarterly bonus transfer",
            dateEpochDay = dayIn(m2, 26),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_03",
            amountCentavos = 22_500_00L,
            isWithdrawal = false,
            note = "Monthly high-yield reserve deposit",
            dateEpochDay = dayIn(m1, 16),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_04",
            amountCentavos = 20_000_00L,
            isWithdrawal = false,
            note = "Current month payroll auto-transfer",
            dateEpochDay = currDay(16),
          ),
        ),
      ),
      DemoGoalRecord(
        docId = "demo_goal_02",
        entity = SavingsGoalEntity(
          name = "Japan Autumn Trip",
          targetCentavos = 80_000_00L,
          currentSavedCentavos = 42_000_00L,
          targetDateEpochDay = dayIn(futureMonth(10), 20),
          createdDateEpochDay = dayIn(m4, 5),
          status = GoalStatus.ACTIVE,
          isSampleData = false,
        ),
        contributions = listOf(
          DemoContributionRecord(
            docId = "demo_contrib_05",
            amountCentavos = 18_000_00L,
            isWithdrawal = false,
            note = "Initial Kyoto & Tokyo flight fund",
            dateEpochDay = dayIn(m3, 18),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_06",
            amountCentavos = 16_000_00L,
            isWithdrawal = false,
            note = "Freelance project allocation",
            dateEpochDay = dayIn(m1, 23),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_07",
            amountCentavos = 12_000_00L,
            isWithdrawal = false,
            note = "Hotel & JR Pass savings deposit",
            dateEpochDay = currDay(9),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_08",
            amountCentavos = 4_000_00L,
            isWithdrawal = true,
            note = "Seat sale baggage & travel insurance booking",
            dateEpochDay = currDay(14),
          ),
        ),
      ),
      DemoGoalRecord(
        docId = "demo_goal_03",
        entity = SavingsGoalEntity(
          name = "MacBook Pro Upgrade",
          targetCentavos = 95_000_00L,
          currentSavedCentavos = 95_000_00L,
          targetDateEpochDay = dayIn(m0, 25),
          createdDateEpochDay = dayIn(m5, 10),
          status = GoalStatus.COMPLETED,
          isSampleData = false,
        ),
        contributions = listOf(
          DemoContributionRecord(
            docId = "demo_contrib_09",
            amountCentavos = 38_000_00L,
            isWithdrawal = false,
            note = "Previous laptop resale allocation",
            dateEpochDay = dayIn(m4, 20),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_10",
            amountCentavos = 32_000_00L,
            isWithdrawal = false,
            note = "Mid-year bonus & consulting savings",
            dateEpochDay = dayIn(m2, 26),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_11",
            amountCentavos = 25_000_00L,
            isWithdrawal = false,
            note = "Final milestone contribution — goal reached!",
            dateEpochDay = currDay(8),
          ),
        ),
      ),
      DemoGoalRecord(
        docId = "demo_goal_04",
        entity = SavingsGoalEntity(
          name = "Home Office Ergonomics",
          targetCentavos = 25_000_00L,
          currentSavedCentavos = 9_500_00L,
          targetDateEpochDay = dayIn(futureMonth(4), 10),
          createdDateEpochDay = dayIn(m2, 12),
          status = GoalStatus.ACTIVE,
          isSampleData = false,
        ),
        contributions = listOf(
          DemoContributionRecord(
            docId = "demo_contrib_12",
            amountCentavos = 3_500_00L,
            isWithdrawal = false,
            note = "Standing desk starter savings",
            dateEpochDay = dayIn(m2, 16),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_13",
            amountCentavos = 3_000_00L,
            isWithdrawal = false,
            note = "Ergonomic chair allocation",
            dateEpochDay = dayIn(m1, 17),
          ),
          DemoContributionRecord(
            docId = "demo_contrib_14",
            amountCentavos = 3_000_00L,
            isWithdrawal = false,
            note = "Monthly workspace contribution",
            dateEpochDay = currDay(16),
          ),
        ),
      ),
    )

    return DemonstrationDataset(
      transactions = transactions,
      budgets = budgets,
      goals = goals,
    )
  }
}

data class DemoTransactionRecord(
  val docId: String,
  val entity: TransactionEntity,
)

data class DemoBudgetRecord(
  val docId: String,
  val entity: BudgetEntity,
)

data class DemoContributionRecord(
  val docId: String,
  val amountCentavos: Long,
  val isWithdrawal: Boolean,
  val note: String,
  val dateEpochDay: Long,
)

data class DemoGoalRecord(
  val docId: String,
  val entity: SavingsGoalEntity,
  val contributions: List<DemoContributionRecord>,
)

data class DemonstrationDataset(
  val transactions: List<DemoTransactionRecord>,
  val budgets: List<DemoBudgetRecord>,
  val goals: List<DemoGoalRecord>,
)

