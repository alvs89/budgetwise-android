package com.example.data.repository

import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.ChatRole
import com.example.domain.YearMonthKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single point of coordination over Room DAOs and [UserPreferencesRepository].
 *
 * Exposes reactive [Flow] streams for financial and chat entities and serializes initial seed
 * and reset routines through [seedMutex] to prevent duplicate starter records.
 */
class BudgetWiseRepository(
  private val database: BudgetWiseDatabase,
  private val preferencesRepository: UserPreferencesRepository,
) {
  private val transactionDao = database.transactionDao()
  private val budgetDao = database.budgetDao()
  private val savingsGoalDao = database.savingsGoalDao()
  private val categoryDao = database.categoryDao()
  private val chatSessionDao = database.chatSessionDao()
  private val chatMessageDao = database.chatMessageDao()
  private val seedMutex = Mutex()

  val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
  val budgets: Flow<List<BudgetEntity>> = budgetDao.observeAllBudgets()
  val savingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAllGoals()
  val savingsContributions: Flow<List<SavingsContributionEntity>> =
    savingsGoalDao.observeAllContributions()
  val categories: Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()
  val chatSessions: Flow<List<ChatSessionEntity>> = chatSessionDao.observeAllSessions()
  val chatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.observeAllMessages()

  /**
   * Populates default categories and initial demonstration records once on first launch when
   * the database is empty.
   */
  suspend fun ensureInitialDataSeeded() {
    seedMutex.withLock {
      if (categoryDao.count() == 0) {
        categoryDao.insertAll(SampleDataSeeder.defaultCategories())
      }
      val prefs = preferencesRepository.userPreferences.first()
      if (!prefs.hasSeededInitialData &&
        transactionDao.count() == 0 &&
        budgetDao.count() == 0 &&
        savingsGoalDao.countGoals() == 0
      ) {
        seedDemoRecordsInternal()
        preferencesRepository.markInitialDataSeeded()
      }
    }
  }

  suspend fun resetToDemonstrationData() {
    seedMutex.withLock {
      transactionDao.deleteAll()
      budgetDao.deleteAll()
      savingsGoalDao.deleteAllContributions()
      savingsGoalDao.deleteAllGoals()
      if (categoryDao.count() == 0) {
        categoryDao.insertAll(SampleDataSeeder.defaultCategories())
      }
      seedDemoRecordsInternal()
      preferencesRepository.markInitialDataSeeded()
    }
  }

  suspend fun clearAllUserData() {
    seedMutex.withLock {
      transactionDao.deleteAll()
      budgetDao.deleteAll()
      savingsGoalDao.deleteAllContributions()
      savingsGoalDao.deleteAllGoals()
      categoryDao.deleteCustomCategories()
      chatMessageDao.deleteAll()
      chatSessionDao.deleteAll()
      preferencesRepository.markInitialDataSeeded()
    }
  }

  private suspend fun seedDemoRecordsInternal() {
    val refMonth = YearMonthKey.current()
    transactionDao.insertAll(SampleDataSeeder.sampleTransactions(refMonth))
    budgetDao.insertAll(SampleDataSeeder.sampleBudgets(refMonth))
    val goalIds = savingsGoalDao.insertAllGoals(SampleDataSeeder.sampleGoals(refMonth))
    savingsGoalDao.insertAllContributions(
      SampleDataSeeder.sampleContributionsForGoals(goalIds, refMonth)
    )
  }

  // Transactions
  suspend fun saveTransaction(transaction: TransactionEntity) {
    if (transaction.id == 0L) {
      transactionDao.insert(transaction)
    } else {
      transactionDao.update(transaction)
    }
  }

  suspend fun deleteTransaction(transaction: TransactionEntity) {
    transactionDao.delete(transaction)
  }

  // Budgets
  suspend fun saveBudget(budget: BudgetEntity): Boolean =
    budgetDao.upsertBudgetForMonth(budget)

  suspend fun deleteBudget(budget: BudgetEntity) {
    budgetDao.delete(budget)
  }

  // Savings Goals
  suspend fun saveGoal(goal: SavingsGoalEntity) {
    if (goal.id == 0L) {
      savingsGoalDao.insertGoal(goal)
    } else {
      savingsGoalDao.updateGoal(goal)
    }
  }

  suspend fun deleteGoal(goal: SavingsGoalEntity) {
    savingsGoalDao.deleteGoal(goal)
  }

  suspend fun recordGoalContribution(
    goalId: Long,
    amountCentavos: Long,
    isWithdrawal: Boolean,
    note: String,
    dateEpochDay: Long,
  ) {
    savingsGoalDao.recordContributionAndAdjustGoal(
      goalId = goalId,
      amountCentavos = amountCentavos,
      isWithdrawal = isWithdrawal,
      note = note,
      dateEpochDay = dateEpochDay,
    )
  }

  // Categories
  suspend fun addCategory(category: CategoryEntity) {
    categoryDao.insert(category)
  }

  suspend fun deleteCategory(category: CategoryEntity) {
    if (!category.isDefault) {
      categoryDao.delete(category)
    }
  }

  // Chat Sessions & Messages
  suspend fun createChatSession(title: String, isManualTitle: Boolean = false): Long {
    val now = System.currentTimeMillis()
    return chatSessionDao.insert(
      ChatSessionEntity(
        title = title,
        isTitleManuallySet = isManualTitle,
        createdAtMillis = now,
        updatedAtMillis = now,
      )
    )
  }

  suspend fun getChatSession(sessionId: Long): ChatSessionEntity? =
    chatSessionDao.getById(sessionId)

  suspend fun renameChatSession(sessionId: Long, newTitle: String, isManual: Boolean) {
    chatSessionDao.updateTitle(
      sessionId = sessionId,
      title = newTitle.trim(),
      isManual = isManual,
      updatedAtMillis = System.currentTimeMillis(),
    )
  }

  /** Deletes a single chat session; its messages are removed atomically via foreign-key cascade. */
  suspend fun deleteChatSession(sessionId: Long) {
    chatSessionDao.deleteById(sessionId)
  }

  suspend fun getMessagesForSession(sessionId: Long): List<ChatMessageEntity> =
    chatMessageDao.getMessagesForSession(sessionId)

  /** Inserts a chat turn and updates the parent session's `updatedAtMillis` for recency sorting. */
  suspend fun saveChatMessage(sessionId: Long, role: ChatRole, content: String): Long {
    val now = System.currentTimeMillis()
    val id = chatMessageDao.insert(
      ChatMessageEntity(
        sessionId = sessionId,
        role = role,
        content = content,
        createdAtMillis = now,
      )
    )
    chatSessionDao.touchUpdatedAt(sessionId, now)
    return id
  }

  suspend fun deleteChatMessagesByIds(ids: List<Long>) {
    if (ids.isNotEmpty()) {
      chatMessageDao.deleteByIds(ids)
    }
  }

  suspend fun clearChatHistory() {
    chatMessageDao.deleteAll()
    chatSessionDao.deleteAll()
  }
}
