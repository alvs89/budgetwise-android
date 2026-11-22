package com.example.data.repository

import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.FinancialAlertEntity
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

data class LocalGuestSnapshot(
  val transactions: List<TransactionEntity>,
  val budgets: List<BudgetEntity>,
  val goals: List<SavingsGoalEntity>,
  val contributions: List<SavingsContributionEntity>,
  val customCategories: List<CategoryEntity>,
) {
  val totalRecordCount: Int
    get() = transactions.size + budgets.size + goals.size + customCategories.size
}

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
  private val financialAlertDao = database.financialAlertDao()
  private val seedMutex = Mutex()

  val transactions: Flow<List<TransactionEntity>> = transactionDao.observeAllTransactions()
  val budgets: Flow<List<BudgetEntity>> = budgetDao.observeAllBudgets()
  val savingsGoals: Flow<List<SavingsGoalEntity>> = savingsGoalDao.observeAllGoals()
  val savingsContributions: Flow<List<SavingsContributionEntity>> =
    savingsGoalDao.observeAllContributions()
  val categories: Flow<List<CategoryEntity>> = categoryDao.observeAllCategories()
  val chatSessions: Flow<List<ChatSessionEntity>> = chatSessionDao.observeAllSessions()
  val chatMessages: Flow<List<ChatMessageEntity>> = chatMessageDao.observeAllMessages()
  val financialAlerts: Flow<List<FinancialAlertEntity>> = financialAlertDao.observeAllAlerts()

  /**
   * Captures a consistent snapshot of all financial records stored locally on this device so
   * offline guest records can be safely migrated into a signed-in account with zero data loss.
   */
  suspend fun getLocalGuestSnapshot(): LocalGuestSnapshot = seedMutex.withLock {
    val tx = transactionDao.observeAllTransactions().first()
    val budgetsList = budgetDao.observeAllBudgets().first()
    val goalsList = savingsGoalDao.observeAllGoals().first()
    val contribsList = savingsGoalDao.observeAllContributions().first()
    val customCats = categoryDao.observeAllCategories().first().filter { !it.isDefault }
    LocalGuestSnapshot(
      transactions = tx,
      budgets = budgetsList,
      goals = goalsList,
      contributions = contribsList,
      customCategories = customCats,
    )
  }

  /**
   * Clears financial tables stored locally in Room after a successful guest-to-account migration.
   */
  suspend fun clearLocalGuestFinancialRecords() {
    seedMutex.withLock {
      transactionDao.deleteAll()
      budgetDao.deleteAll()
      savingsGoalDao.deleteAllContributions()
      savingsGoalDao.deleteAllGoals()
      categoryDao.deleteCustomCategories()
    }
  }

  /**
   * Populates the 30 built-in income & expense categories once on first launch so accounts start
   * with a clean ₱0.00 ledger ready for real transactions.
   */
  suspend fun ensureInitialDataSeeded() {
    seedMutex.withLock {
      categoryDao.insertAll(SampleDataSeeder.defaultCategories())
      val prefs = preferencesRepository.userPreferences.first()
      if (!prefs.hasSeededInitialData) {
        preferencesRepository.markInitialDataSeeded()
      }
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
      financialAlertDao.deleteAll()
      preferencesRepository.markInitialDataSeeded()
    }
  }

  suspend fun seedDemonstrationData(
    referenceMonth: YearMonthKey = YearMonthKey.current(),
  ): Int = seedMutex.withLock {
    categoryDao.insertAll(SampleDataSeeder.defaultCategories())
    val dataset = SampleDataSeeder.demonstrationDataset(referenceMonth)
    var inserted = 0
    dataset.transactions.forEach { record ->
      transactionDao.insert(record.entity)
      inserted += 1
    }
    dataset.budgets.forEach { record ->
      budgetDao.upsertBudgetForMonth(record.entity)
      inserted += 1
    }
    dataset.goals.forEach { goalRecord ->
      val goalId = savingsGoalDao.insertGoal(goalRecord.entity)
      inserted += 1
      val contributions = goalRecord.contributions.map { c ->
        SavingsContributionEntity(
          goalId = goalId,
          amountCentavos = c.amountCentavos,
          isWithdrawal = c.isWithdrawal,
          note = c.note,
          dateEpochDay = c.dateEpochDay,
        )
      }
      if (contributions.isNotEmpty()) {
        savingsGoalDao.insertAllContributions(contributions)
        inserted += contributions.size
      }
    }
    preferencesRepository.markInitialDataSeeded()
    inserted
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

  suspend fun updateCategory(
    existing: CategoryEntity,
    newName: String,
    newIconKey: String,
    newColorHex: Long,
  ) {
    val cleanName = if (existing.isDefault) existing.name else newName.trim().ifEmpty { existing.name }
    categoryDao.update(
      existing.copy(
        name = cleanName,
        iconKey = newIconKey,
        colorHex = newColorHex,
      )
    )
    if (cleanName != existing.name) {
      transactionDao.renameCategory(oldName = existing.name, newName = cleanName)
      budgetDao.renameCategory(oldName = existing.name, newName = cleanName)
    }
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

  // Financial Alerts
  /**
   * Persists [alert] if its [FinancialAlertEntity.dedupeKey] has not been recorded yet.
   * Returns the persisted [FinancialAlertEntity] (with generated id) when newly inserted, or null
   * if it was a duplicate.
   */
  suspend fun insertAlertIfNew(alert: FinancialAlertEntity): FinancialAlertEntity? {
    val insertedId = financialAlertDao.insertIgnore(alert)
    return if (insertedId > 0L) alert.copy(id = insertedId) else null
  }

  suspend fun markAlertAsRead(id: Long) {
    financialAlertDao.markAsRead(id)
  }

  suspend fun setAlertRead(id: Long, isRead: Boolean) {
    financialAlertDao.setRead(id, isRead)
  }

  suspend fun markAllAlertsAsRead() {
    financialAlertDao.markAllAsRead()
  }

  suspend fun markAllAlertsAsUnread() {
    financialAlertDao.markAllAsUnread()
  }

  suspend fun deleteAlert(id: Long) {
    financialAlertDao.deleteById(id)
  }

  suspend fun clearAlertDedupePrefix(prefix: String) {
    financialAlertDao.deleteByDedupeKeyPrefix(prefix)
  }

  suspend fun clearAllAlerts() {
    financialAlertDao.deleteAll()
  }
}
