package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
  @Query("SELECT * FROM transactions ORDER BY dateEpochDay DESC, createdAtMillis DESC")
  fun observeAllTransactions(): Flow<List<TransactionEntity>>

  @Query("SELECT COUNT(*) FROM transactions")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(transaction: TransactionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(transactions: List<TransactionEntity>)

  @Update
  suspend fun update(transaction: TransactionEntity)

  @Delete
  suspend fun delete(transaction: TransactionEntity)

  @Query("DELETE FROM transactions WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM transactions")
  suspend fun deleteAll()
}

@Dao
interface BudgetDao {
  @Query("SELECT * FROM budgets ORDER BY year DESC, month DESC, category ASC")
  fun observeAllBudgets(): Flow<List<BudgetEntity>>

  @Query("SELECT COUNT(*) FROM budgets")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(budget: BudgetEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(budgets: List<BudgetEntity>)

  @Update
  suspend fun update(budget: BudgetEntity)

  @Delete
  suspend fun delete(budget: BudgetEntity)

  @Query("SELECT * FROM budgets WHERE category = :category AND year = :year AND month = :month LIMIT 1")
  suspend fun findByCategoryAndMonth(category: String, year: Int, month: Int): BudgetEntity?

  @Query("DELETE FROM budgets WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM budgets")
  suspend fun deleteAll()

  /**
   * Atomically inserts or updates a monthly budget while enforcing one unique record per
   * `(category, year, month)`. Returns `true` if a new row was created, or `false` if updated.
   */
  @Transaction
  suspend fun upsertBudgetForMonth(budget: BudgetEntity): Boolean {
    val existingForSlot = findByCategoryAndMonth(budget.category, budget.year, budget.month)
    return if (budget.id == 0L) {
      if (existingForSlot != null) {
        update(existingForSlot.copy(limitCentavos = budget.limitCentavos, isSampleData = false))
        false
      } else {
        insert(budget)
        true
      }
    } else {
      if (existingForSlot != null && existingForSlot.id != budget.id) {
        deleteById(budget.id)
        update(existingForSlot.copy(limitCentavos = budget.limitCentavos, isSampleData = false))
      } else {
        update(budget)
      }
      false
    }
  }
}

@Dao
interface SavingsGoalDao {
  @Query("SELECT * FROM savings_goals ORDER BY createdDateEpochDay DESC, id DESC")
  fun observeAllGoals(): Flow<List<SavingsGoalEntity>>

  @Query("SELECT * FROM savings_contributions ORDER BY dateEpochDay DESC, createdAtMillis DESC")
  fun observeAllContributions(): Flow<List<SavingsContributionEntity>>

  @Query("SELECT * FROM savings_contributions WHERE goalId = :goalId ORDER BY dateEpochDay DESC, createdAtMillis DESC")
  fun observeContributionsForGoal(goalId: Long): Flow<List<SavingsContributionEntity>>

  @Query("SELECT * FROM savings_goals WHERE id = :goalId LIMIT 1")
  suspend fun getGoalById(goalId: Long): SavingsGoalEntity?

  @Query("SELECT COUNT(*) FROM savings_goals")
  suspend fun countGoals(): Int

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertGoal(goal: SavingsGoalEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllGoals(goals: List<SavingsGoalEntity>): List<Long>

  @Update
  suspend fun updateGoal(goal: SavingsGoalEntity)

  @Delete
  suspend fun deleteGoal(goal: SavingsGoalEntity)

  @Query("DELETE FROM savings_goals WHERE id = :id")
  suspend fun deleteGoalById(id: Long)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertContribution(contribution: SavingsContributionEntity): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAllContributions(contributions: List<SavingsContributionEntity>)

  @Query("DELETE FROM savings_goals")
  suspend fun deleteAllGoals()

  @Query("DELETE FROM savings_contributions")
  suspend fun deleteAllContributions()

  /**
   * Atomically logs a deposit or withdrawal entry, clamps the goal's saved balance at `>= 0`,
   * and transitions goal status between [com.example.domain.GoalStatus.ACTIVE] and
   * [com.example.domain.GoalStatus.COMPLETED] as thresholds are crossed.
   */
  @Transaction
  suspend fun recordContributionAndAdjustGoal(
    goalId: Long,
    amountCentavos: Long,
    isWithdrawal: Boolean,
    note: String,
    dateEpochDay: Long,
  ) {
    val goal = getGoalById(goalId) ?: return
    val delta = if (isWithdrawal) -amountCentavos else amountCentavos
    val newSaved = (goal.currentSavedCentavos + delta).coerceAtLeast(0L)
    val updatedStatus = when {
      newSaved >= goal.targetCentavos -> com.example.domain.GoalStatus.COMPLETED
      goal.status == com.example.domain.GoalStatus.COMPLETED && newSaved < goal.targetCentavos ->
        com.example.domain.GoalStatus.ACTIVE
      else -> goal.status
    }
    insertContribution(
      SavingsContributionEntity(
        goalId = goalId,
        amountCentavos = amountCentavos,
        isWithdrawal = isWithdrawal,
        note = note,
        dateEpochDay = dateEpochDay,
      )
    )
    updateGoal(
      goal.copy(
        currentSavedCentavos = newSaved,
        status = updatedStatus,
      )
    )
  }
}

@Dao
interface CategoryDao {
  @Query(
    "SELECT * FROM categories ORDER BY " +
      "CASE WHEN LOWER(TRIM(name)) = 'other' OR LOWER(TRIM(name)) = 'others' OR LOWER(TRIM(name)) LIKE 'other %' THEN 1 ELSE 0 END ASC, " +
      "isDefault DESC, name ASC"
  )
  fun observeAllCategories(): Flow<List<CategoryEntity>>

  @Query("SELECT COUNT(*) FROM categories")
  suspend fun count(): Int

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insert(category: CategoryEntity): Long

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertAll(categories: List<CategoryEntity>)

  @Delete
  suspend fun delete(category: CategoryEntity)

  @Query("DELETE FROM categories WHERE isDefault = 0")
  suspend fun deleteCustomCategories()
}

@Dao
interface ChatSessionDao {
  @Query("SELECT * FROM chat_sessions ORDER BY updatedAtMillis DESC, id DESC")
  fun observeAllSessions(): Flow<List<ChatSessionEntity>>

  @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
  suspend fun getById(sessionId: Long): ChatSessionEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: ChatSessionEntity): Long

  @Query("UPDATE chat_sessions SET title = :title, isTitleManuallySet = :isManual, updatedAtMillis = :updatedAtMillis WHERE id = :sessionId")
  suspend fun updateTitle(sessionId: Long, title: String, isManual: Boolean, updatedAtMillis: Long)

  @Query("UPDATE chat_sessions SET updatedAtMillis = :updatedAtMillis WHERE id = :sessionId")
  suspend fun touchUpdatedAt(sessionId: Long, updatedAtMillis: Long)

  @Query("DELETE FROM chat_sessions WHERE id = :sessionId")
  suspend fun deleteById(sessionId: Long)

  @Query("DELETE FROM chat_sessions")
  suspend fun deleteAll()
}

@Dao
interface ChatMessageDao {
  @Query("SELECT * FROM chat_messages ORDER BY createdAtMillis ASC, id ASC")
  fun observeAllMessages(): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtMillis ASC, id ASC")
  fun observeMessagesForSession(sessionId: Long): Flow<List<ChatMessageEntity>>

  @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAtMillis ASC, id ASC")
  suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(message: ChatMessageEntity): Long

  @Query("DELETE FROM chat_messages WHERE id IN (:ids)")
  suspend fun deleteByIds(ids: List<Long>)

  @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId")
  suspend fun deleteBySessionId(sessionId: Long)

  @Query("DELETE FROM chat_messages")
  suspend fun deleteAll()
}
