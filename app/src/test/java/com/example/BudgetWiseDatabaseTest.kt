package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.FinanceCalculator
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.epochDayFromDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BudgetWiseDatabaseTest {

  private lateinit var db: BudgetWiseDatabase

  @Before
  fun setUp() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    db = Room.inMemoryDatabaseBuilder(context, BudgetWiseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
  }

  @After
  fun tearDown() {
    db.close()
  }

  @Test
  fun savings_contributions_do_not_double_count_income_or_expenses() = runBlocking {
    val month = YearMonthKey(2026, 11)
    db.transactionDao().insert(
      TransactionEntity(
        title = "Salary",
        amountCentavos = 20_000_00L,
        type = TransactionType.INCOME,
        category = "Salary",
        dateEpochDay = epochDayFromDate(2026, 11, 5),
        paymentMethod = PaymentMethod.BANK_TRANSFER,
      )
    )
    val goalId = db.savingsGoalDao().insertGoal(
      SavingsGoalEntity(
        name = "Emergency Fund",
        targetCentavos = 10_000_00L,
        currentSavedCentavos = 0L,
        targetDateEpochDay = epochDayFromDate(2027, 6, 1),
        createdDateEpochDay = epochDayFromDate(2026, 11, 1),
        status = GoalStatus.ACTIVE,
      )
    )

    db.savingsGoalDao().recordContributionAndAdjustGoal(
      goalId = goalId,
      amountCentavos = 5_000_00L,
      isWithdrawal = false,
      note = "Initial deposit",
      dateEpochDay = epochDayFromDate(2026, 11, 6),
    )
    db.savingsGoalDao().recordContributionAndAdjustGoal(
      goalId = goalId,
      amountCentavos = 1_000_00L,
      isWithdrawal = true,
      note = "Correction",
      dateEpochDay = epochDayFromDate(2026, 11, 7),
    )

    val goal = db.savingsGoalDao().getGoalById(goalId)!!
    val txs = db.transactionDao().observeAllTransactions().first()

    assertEquals(4_000_00L, goal.currentSavedCentavos)
    assertEquals(BigDecimal("20000.00"), FinanceCalculator.calculateTotalIncome(txs, month))
    assertEquals(BigDecimal("0.00"), FinanceCalculator.calculateTotalExpenses(txs, month))
  }
}
