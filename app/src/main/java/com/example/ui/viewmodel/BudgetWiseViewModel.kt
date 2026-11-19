package com.example.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.FinancialChatService
import com.example.ai.GeminiFinancialChatService
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BudgetWiseRepository
import com.example.domain.BudgetSummary
import com.example.domain.CategoryBreakdown
import com.example.domain.CategoryVisualMatcher
import com.example.domain.ChatRole
import com.example.domain.ChatTitleGenerator
import com.example.domain.DailySpendingPoint
import com.example.domain.FinanceCalculator
import com.example.domain.FinancialChatContextBuilder
import com.example.domain.FinancialInsight
import com.example.domain.GoalStatus
import com.example.domain.MonthlyCashFlowPoint
import com.example.domain.MonthlySpendingPoint
import com.example.domain.PaymentMethod
import com.example.domain.PinSecurityManager
import com.example.domain.SavingsGoalSummary
import com.example.domain.ThemeMode
import com.example.domain.TransactionSortOrder
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.currentEpochDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.math.BigDecimal

enum class HomePeriodScope(val label: String) {
  THIS_MONTH("This Month"),
  ALL_TIME("All Time"),
}

data class ActivityFilterState(
  val searchQuery: String = "",
  val typeFilter: TransactionType? = null,
  val categoryFilter: String? = null,
  val selectedMonthOnly: Boolean = true,
  val sortOrder: TransactionSortOrder = TransactionSortOrder.DATE_DESC,
)

sealed interface ChatStreamingState {
  data object Idle : ChatStreamingState
  data class Streaming(val sessionId: Long, val partialReply: String) : ChatStreamingState
  data class Error(val sessionId: Long, val message: String, val failedPrompt: String) : ChatStreamingState
}

private sealed interface ActiveChatSelection {
  data object AutoLatest : ActiveChatSelection
  data class Session(val sessionId: Long) : ActiveChatSelection
  data object NewDraft : ActiveChatSelection
}

data class ChatSessionSummary(
  val session: ChatSessionEntity,
  val lastMessagePreview: String,
  val messageCount: Int,
)

data class ChatUiState(
  val activeSession: ChatSessionEntity? = null,
  val sessions: List<ChatSessionSummary> = emptyList(),
  val messages: List<ChatMessageEntity> = emptyList(),
  val isStreaming: Boolean = false,
  val streamingReply: String = "",
  val errorMessage: String? = null,
  val failedPrompt: String? = null,
)

data class BudgetWiseUiState(
  val isLoading: Boolean = true,
  val preferences: UserPreferences = UserPreferences(),
  val selectedMonth: YearMonthKey = YearMonthKey.current(),
  val homePeriodScope: HomePeriodScope = HomePeriodScope.THIS_MONTH,
  val allTransactions: List<TransactionEntity> = emptyList(),
  val monthTransactions: List<TransactionEntity> = emptyList(),
  val filteredActivityTransactions: List<TransactionEntity> = emptyList(),
  val activityFilter: ActivityFilterState = ActivityFilterState(),
  val totalIncome: BigDecimal = BigDecimal.ZERO,
  val totalExpenses: BigDecimal = BigDecimal.ZERO,
  val netCashFlow: BigDecimal = BigDecimal.ZERO,
  val savingsRatePercent: BigDecimal = BigDecimal.ZERO,
  val allTimeIncome: BigDecimal = BigDecimal.ZERO,
  val allTimeExpenses: BigDecimal = BigDecimal.ZERO,
  val allTimeNetCashFlow: BigDecimal = BigDecimal.ZERO,
  val allTimeSavingsRatePercent: BigDecimal = BigDecimal.ZERO,
  val totalReservedSavings: BigDecimal = BigDecimal.ZERO,
  val budgetSummaries: List<BudgetSummary> = emptyList(),
  val totalBudgetLimit: BigDecimal = BigDecimal.ZERO,
  val totalBudgetSpent: BigDecimal = BigDecimal.ZERO,
  val goalSummaries: List<SavingsGoalSummary> = emptyList(),
  val rawGoals: List<SavingsGoalEntity> = emptyList(),
  val allContributions: List<SavingsContributionEntity> = emptyList(),
  val expenseBreakdown: List<CategoryBreakdown> = emptyList(),
  val allTimeExpenseBreakdown: List<CategoryBreakdown> = emptyList(),
  val monthlySeries: List<MonthlyCashFlowPoint> = emptyList(),
  val dailySpendingSeries: List<DailySpendingPoint> = emptyList(),
  val allTimeMonthlySpendingSeries: List<MonthlySpendingPoint> = emptyList(),
  val insights: List<FinancialInsight> = emptyList(),
  val categories: List<CategoryEntity> = emptyList(),
)

private data class CoreInputs(
  val prefs: UserPreferences,
  val selectedMonth: YearMonthKey,
  val transactions: List<TransactionEntity>,
  val budgets: List<BudgetEntity>,
  val goals: List<SavingsGoalEntity>,
)

private data class SecondaryInputs(
  val contributions: List<SavingsContributionEntity>,
  val categories: List<CategoryEntity>,
  val activityFilter: ActivityFilterState,
  val homePeriodScope: HomePeriodScope,
)

/**
 * Primary MVVM state holder for BudgetWise.
 *
 * Combines reactive Room database streams, Jetpack DataStore preferences, active screen filters,
 * App Lock state, and multi-session assistant chat state into immutable [StateFlow] snapshots
 * consumed by Jetpack Compose screens.
 */
class BudgetWiseViewModel @JvmOverloads constructor(
  application: Application,
  private val geminiChatService: FinancialChatService = GeminiFinancialChatService(),
) : AndroidViewModel(application) {

  private val preferencesRepository = UserPreferencesRepository(application)
  private val database = BudgetWiseDatabase.getInstance(application)
  private val repository = BudgetWiseRepository(database, preferencesRepository)

  private val selectedMonthFlow = MutableStateFlow(YearMonthKey.current())
  private val activityFilterFlow = MutableStateFlow(ActivityFilterState())
  private val homePeriodScopeFlow = MutableStateFlow(HomePeriodScope.THIS_MONTH)

  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

  private val _isAppLocked = MutableStateFlow(false)
  val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

  private val activeChatSelectionFlow = MutableStateFlow<ActiveChatSelection>(ActiveChatSelection.AutoLatest)
  private val chatStreamingStateFlow = MutableStateFlow<ChatStreamingState>(ChatStreamingState.Idle)
  private var activeChatJob: Job? = null

  private var backgroundedAtMillis: Long? = null

  init {
    viewModelScope.launch {
      val initialPrefs = preferencesRepository.userPreferences.first()
      if (initialPrefs.appLockEnabled && !initialPrefs.pinHashRecord.isNullOrBlank()) {
        _isAppLocked.value = true
      }
      repository.ensureInitialDataSeeded()
    }
  }

  private val coreInputsFlow = combine(
    preferencesRepository.userPreferences,
    selectedMonthFlow,
    repository.transactions,
    repository.budgets,
    repository.savingsGoals,
  ) { prefs, month, transactions, budgets, goals ->
    CoreInputs(prefs, month, transactions, budgets, goals)
  }

  private val secondaryInputsFlow = combine(
    repository.savingsContributions,
    repository.categories,
    activityFilterFlow,
    homePeriodScopeFlow,
  ) { contributions, categories, filter, homeScope ->
    SecondaryInputs(contributions, categories, filter, homeScope)
  }

  val uiState: StateFlow<BudgetWiseUiState> = combine(
    coreInputsFlow,
    secondaryInputsFlow,
  ) { core, secondary ->
    val month = core.selectedMonth
    val transactions = core.transactions
    val monthTransactions = FinanceCalculator.filterTransactionsByMonth(transactions, month)
    val totalIncome = FinanceCalculator.calculateTotalIncome(transactions, month)
    val totalExpenses = FinanceCalculator.calculateTotalExpenses(transactions, month)
    val netCashFlow = FinanceCalculator.calculateNetCashFlow(totalIncome, totalExpenses)
    val savingsRate = FinanceCalculator.calculateSavingsRatePercent(totalIncome, totalExpenses)
    val allTimeIncome = FinanceCalculator.calculateAllTimeTotalIncome(transactions)
    val allTimeExpenses = FinanceCalculator.calculateAllTimeTotalExpenses(transactions)
    val allTimeNetCashFlow = FinanceCalculator.calculateNetCashFlow(allTimeIncome, allTimeExpenses)
    val allTimeSavingsRate = FinanceCalculator.calculateSavingsRatePercent(allTimeIncome, allTimeExpenses)
    val totalReservedSavings = FinanceCalculator.calculateTotalReservedSavings(core.goals)
    val budgetSummaries = FinanceCalculator.buildBudgetSummaries(core.budgets, transactions, month)
    val totalBudgetLimit = budgetSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.limitAmount) }
    val totalBudgetSpent = budgetSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.spentAmount) }
    val goalSummaries = FinanceCalculator.buildGoalSummaries(core.goals)
    val breakdown = FinanceCalculator.buildExpenseBreakdown(transactions, secondary.categories, month)
    val allTimeBreakdown = FinanceCalculator.buildAllTimeExpenseBreakdown(transactions, secondary.categories)
    val monthlySeries = FinanceCalculator.buildMonthlyCashFlowSeries(transactions, month, 6)
    val dailySeries = FinanceCalculator.buildDailySpendingSeries(transactions, month)
    val allTimeMonthlySpending = FinanceCalculator.buildAllTimeMonthlySpendingSeries(transactions)
    val insights = FinanceCalculator.generateInsights(
      transactions = transactions,
      budgetSummaries = budgetSummaries,
      breakdown = breakdown,
      selectedMonth = month,
      currencyCode = core.prefs.preferredCurrency,
      maskAmounts = core.prefs.isBalanceHidden,
    )
    val filteredActivity = applyActivityFilter(
      transactions = transactions,
      selectedMonth = month,
      filter = secondary.activityFilter,
    )

    BudgetWiseUiState(
      isLoading = false,
      preferences = core.prefs,
      selectedMonth = month,
      homePeriodScope = secondary.homePeriodScope,
      allTransactions = transactions,
      monthTransactions = monthTransactions,
      filteredActivityTransactions = filteredActivity,
      activityFilter = secondary.activityFilter,
      totalIncome = totalIncome,
      totalExpenses = totalExpenses,
      netCashFlow = netCashFlow,
      savingsRatePercent = savingsRate,
      allTimeIncome = allTimeIncome,
      allTimeExpenses = allTimeExpenses,
      allTimeNetCashFlow = allTimeNetCashFlow,
      allTimeSavingsRatePercent = allTimeSavingsRate,
      totalReservedSavings = totalReservedSavings,
      budgetSummaries = budgetSummaries,
      totalBudgetLimit = totalBudgetLimit,
      totalBudgetSpent = totalBudgetSpent,
      goalSummaries = goalSummaries,
      rawGoals = core.goals,
      allContributions = secondary.contributions,
      expenseBreakdown = breakdown,
      allTimeExpenseBreakdown = allTimeBreakdown,
      monthlySeries = monthlySeries,
      dailySpendingSeries = dailySeries,
      allTimeMonthlySpendingSeries = allTimeMonthlySpending,
      insights = insights,
      categories = secondary.categories,
    )
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = BudgetWiseUiState(),
  )

  val chatUiState: StateFlow<ChatUiState> = combine(
    repository.chatSessions,
    repository.chatMessages,
    activeChatSelectionFlow,
    chatStreamingStateFlow,
  ) { sessions, allMessages, selection, streaming ->
    val messagesBySession = allMessages.groupBy { it.sessionId }
    val sessionSummaries = sessions.map { session ->
      val sessionMsgs = messagesBySession[session.id].orEmpty()
      val preview = sessionMsgs.lastOrNull()?.content
        ?.replace(Regex("""\s+"""), " ")
        ?.trim()
        .orEmpty()
      ChatSessionSummary(
        session = session,
        lastMessagePreview = preview,
        messageCount = sessionMsgs.size,
      )
    }

    val activeSession = when (selection) {
      ActiveChatSelection.AutoLatest -> sessions.firstOrNull()
      ActiveChatSelection.NewDraft -> null
      is ActiveChatSelection.Session -> sessions.firstOrNull { it.id == selection.sessionId }
        ?: sessions.firstOrNull()
    }
    val activeMessages = if (activeSession != null) {
      messagesBySession[activeSession.id].orEmpty()
    } else {
      emptyList()
    }

    when (streaming) {
      ChatStreamingState.Idle -> ChatUiState(
        activeSession = activeSession,
        sessions = sessionSummaries,
        messages = activeMessages,
        isStreaming = false,
      )
      is ChatStreamingState.Streaming -> {
        val matchesActive = activeSession?.id == streaming.sessionId
        ChatUiState(
          activeSession = activeSession,
          sessions = sessionSummaries,
          messages = activeMessages,
          isStreaming = matchesActive,
          streamingReply = if (matchesActive) streaming.partialReply else "",
        )
      }
      is ChatStreamingState.Error -> {
        val matchesActive = activeSession?.id == streaming.sessionId
        ChatUiState(
          activeSession = activeSession,
          sessions = sessionSummaries,
          messages = activeMessages,
          isStreaming = false,
          errorMessage = if (matchesActive) streaming.message else null,
          failedPrompt = if (matchesActive) streaming.failedPrompt else null,
        )
      }
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = ChatUiState(),
  )

  private fun applyActivityFilter(
    transactions: List<TransactionEntity>,
    selectedMonth: YearMonthKey,
    filter: ActivityFilterState,
  ): List<TransactionEntity> {
    val base = if (filter.selectedMonthOnly) {
      FinanceCalculator.filterTransactionsByMonth(transactions, selectedMonth)
    } else {
      transactions
    }
    val query = filter.searchQuery.trim().lowercase()
    val filtered = base.filter { tx ->
      val matchesQuery = query.isEmpty() ||
        tx.title.lowercase().contains(query) ||
        tx.category.lowercase().contains(query) ||
        tx.notes.lowercase().contains(query)
      val matchesType = filter.typeFilter == null || tx.type == filter.typeFilter
      val matchesCategory = filter.categoryFilter == null || tx.category == filter.categoryFilter
      matchesQuery && matchesType && matchesCategory
    }
    return when (filter.sortOrder) {
      TransactionSortOrder.DATE_DESC -> filtered.sortedWith(
        compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.createdAtMillis }
      )
      TransactionSortOrder.DATE_ASC -> filtered.sortedWith(
        compareBy<TransactionEntity> { it.dateEpochDay }.thenBy { it.createdAtMillis }
      )
      TransactionSortOrder.AMOUNT_DESC -> filtered.sortedByDescending { it.amountCentavos }
      TransactionSortOrder.AMOUNT_ASC -> filtered.sortedBy { it.amountCentavos }
    }
  }

  // Month Navigation
  fun selectPreviousMonth() {
    selectedMonthFlow.update { it.previous() }
  }

  fun selectNextMonth() {
    selectedMonthFlow.update { it.next() }
  }

  fun selectMonth(yearMonth: YearMonthKey) {
    selectedMonthFlow.value = yearMonth
  }

  fun setHomePeriodScope(scope: HomePeriodScope) {
    homePeriodScopeFlow.value = scope
  }

  // Activity Filter Actions
  fun updateSearchQuery(query: String) {
    activityFilterFlow.update { it.copy(searchQuery = query) }
  }

  fun updateTypeFilter(type: TransactionType?) {
    activityFilterFlow.update { current ->
      val keepCategory = type == null || current.categoryFilter == null ||
        uiState.value.allTransactions.any { it.type == type && it.category == current.categoryFilter }
      current.copy(
        typeFilter = type,
        categoryFilter = if (keepCategory) current.categoryFilter else null,
      )
    }
  }

  fun updateCategoryFilter(category: String?) {
    activityFilterFlow.update { it.copy(categoryFilter = category) }
  }

  fun toggleMonthOnlyFilter(monthOnly: Boolean) {
    activityFilterFlow.update { it.copy(selectedMonthOnly = monthOnly) }
  }

  fun updateSortOrder(order: TransactionSortOrder) {
    activityFilterFlow.update { it.copy(sortOrder = order) }
  }

  fun clearActivityFilters() {
    activityFilterFlow.value = ActivityFilterState()
  }

  // Transaction CRUD
  fun saveTransaction(
    existingId: Long = 0L,
    title: String,
    amountCentavos: Long,
    type: TransactionType,
    category: String,
    dateEpochDay: Long,
    paymentMethod: PaymentMethod,
    notes: String,
    existingCreatedAt: Long? = null,
  ) {
    viewModelScope.launch {
      val currentCategories = uiState.value.categories
      val matchingForType = currentCategories.filter { it.type == type }.map { it.name }
      val resolvedCategory = if (matchingForType.isEmpty() || category in matchingForType) {
        category
      } else {
        matchingForType.first()
      }
      val entity = TransactionEntity(
        id = existingId,
        title = title.trim(),
        amountCentavos = amountCentavos,
        type = type,
        category = resolvedCategory,
        dateEpochDay = dateEpochDay,
        paymentMethod = paymentMethod,
        notes = notes.trim(),
        createdAtMillis = existingCreatedAt ?: System.currentTimeMillis(),
        isSampleData = false,
      )
      repository.saveTransaction(entity)
      selectedMonthFlow.value = YearMonthKey.fromEpochDay(dateEpochDay)
      _snackbarMessages.tryEmit(
        if (existingId == 0L) "Transaction added" else "Transaction updated"
      )
    }
  }

  fun deleteTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      repository.deleteTransaction(transaction)
      _snackbarMessages.tryEmit("Transaction deleted")
    }
  }

  // Budget CRUD
  fun saveBudget(
    existingId: Long = 0L,
    category: String,
    limitCentavos: Long,
    yearMonth: YearMonthKey,
  ) {
    viewModelScope.launch {
      val created = repository.saveBudget(
        BudgetEntity(
          id = existingId,
          category = category,
          limitCentavos = limitCentavos,
          year = yearMonth.year,
          month = yearMonth.month,
          isSampleData = false,
        )
      )
      _snackbarMessages.tryEmit(
        if (created) "Budget created for $category" else "Budget updated for $category"
      )
    }
  }

  fun deleteBudget(summary: BudgetSummary) {
    viewModelScope.launch {
      repository.deleteBudget(
        BudgetEntity(
          id = summary.budgetId,
          category = summary.category,
          limitCentavos = FinanceCalculator.bigDecimalToCentavos(summary.limitAmount),
          year = summary.yearMonth.year,
          month = summary.yearMonth.month,
          isSampleData = summary.isSampleData,
        )
      )
      _snackbarMessages.tryEmit("Budget deleted")
    }
  }

  // Savings Goals CRUD
  fun saveGoal(
    existing: SavingsGoalEntity? = null,
    name: String,
    targetCentavos: Long,
    targetDateEpochDay: Long,
    status: GoalStatus,
  ) {
    viewModelScope.launch {
      val currentSaved = existing?.currentSavedCentavos ?: 0L
      val computedStatus = when {
        targetCentavos > 0L && currentSaved >= targetCentavos -> GoalStatus.COMPLETED
        existing?.status == GoalStatus.COMPLETED && status == GoalStatus.COMPLETED && currentSaved < targetCentavos ->
          GoalStatus.ACTIVE
        else -> status
      }
      val entity = SavingsGoalEntity(
        id = existing?.id ?: 0L,
        name = name.trim(),
        targetCentavos = targetCentavos,
        currentSavedCentavos = currentSaved,
        targetDateEpochDay = targetDateEpochDay,
        createdDateEpochDay = existing?.createdDateEpochDay ?: currentEpochDay(),
        status = computedStatus,
        isSampleData = false,
      )
      repository.saveGoal(entity)
      _snackbarMessages.tryEmit(if (existing == null) "Savings goal created" else "Savings goal updated")
    }
  }

  fun deleteGoal(goal: SavingsGoalEntity) {
    viewModelScope.launch {
      repository.deleteGoal(goal)
      _snackbarMessages.tryEmit("Savings goal deleted")
    }
  }

  fun recordGoalContribution(
    goalId: Long,
    amountCentavos: Long,
    isWithdrawal: Boolean,
    note: String,
    dateEpochDay: Long,
  ) {
    viewModelScope.launch {
      repository.recordGoalContribution(
        goalId = goalId,
        amountCentavos = amountCentavos,
        isWithdrawal = isWithdrawal,
        note = note.trim(),
        dateEpochDay = dateEpochDay,
      )
      _snackbarMessages.tryEmit(
        if (isWithdrawal) "Savings adjustment recorded" else "Contribution added to goal"
      )
    }
  }

  // Categories Management
  fun addCustomCategory(
    name: String,
    type: TransactionType,
    iconKey: String? = null,
  ) {
    viewModelScope.launch {
      val cleanName = name.trim()
      if (cleanName.isEmpty()) return@launch
      val alreadyExists = uiState.value.categories.any {
        it.name.trim().equals(cleanName, ignoreCase = true)
      }
      if (alreadyExists) {
        _snackbarMessages.tryEmit("Category \"$cleanName\" already exists")
        return@launch
      }
      val (inferredIconKey, inferredColorHex) = CategoryVisualMatcher.inferIconAndColor(cleanName, type)
      val finalIconKey = iconKey?.takeIf { it.isNotBlank() } ?: inferredIconKey
      val finalColorHex = CategoryVisualMatcher.colorForIconOrName(
        iconKey = finalIconKey,
        name = cleanName,
        type = type,
        fallbackColor = inferredColorHex,
      )
      repository.addCategory(
        CategoryEntity(
          name = cleanName,
          type = type,
          iconKey = finalIconKey,
          colorHex = finalColorHex,
          isDefault = false,
        )
      )
      _snackbarMessages.tryEmit("Category \"$cleanName\" added")
    }
  }

  fun deleteCustomCategory(category: CategoryEntity) {
    viewModelScope.launch {
      repository.deleteCategory(category)
      _snackbarMessages.tryEmit("Category removed")
    }
  }

  // Preferences & Data Management
  fun completeOnboarding(displayName: String, currencyCode: String) {
    viewModelScope.launch {
      preferencesRepository.completeOnboarding(displayName, currencyCode)
    }
  }

  fun skipOnboarding() {
    viewModelScope.launch {
      preferencesRepository.markOnboardingSkipped()
    }
  }

  fun updateDisplayName(name: String) {
    viewModelScope.launch {
      preferencesRepository.updateDisplayName(name)
      _snackbarMessages.tryEmit("Display name updated")
    }
  }

  fun updatePreferredCurrency(currencyCode: String) {
    viewModelScope.launch {
      preferencesRepository.updatePreferredCurrency(currencyCode)
      _snackbarMessages.tryEmit("Currency updated to $currencyCode")
    }
  }

  fun updateThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      preferencesRepository.updateThemeMode(mode)
    }
  }

  fun toggleBalanceHidden() {
    viewModelScope.launch {
      val next = !uiState.value.preferences.isBalanceHidden
      preferencesRepository.updateBalanceHidden(next)
    }
  }

  fun setBalanceHidden(hidden: Boolean) {
    viewModelScope.launch {
      preferencesRepository.updateBalanceHidden(hidden)
    }
  }

  fun resetDemonstrationData() {
    viewModelScope.launch {
      repository.resetToDemonstrationData()
      selectedMonthFlow.value = YearMonthKey.current()
      _snackbarMessages.tryEmit("Demonstration dataset restored")
    }
  }

  fun clearAllRecords() {
    viewModelScope.launch {
      repository.clearAllUserData()
      _snackbarMessages.tryEmit("All financial records cleared")
    }
  }

  // App Lock & PIN Security
  fun onAppMovedToBackground() {
    val prefs = uiState.value.preferences
    if (prefs.appLockEnabled && !prefs.pinHashRecord.isNullOrBlank()) {
      backgroundedAtMillis = System.currentTimeMillis()
    }
  }

  fun onAppReturnedToForeground() {
    val pausedAt = backgroundedAtMillis ?: return
    backgroundedAtMillis = null
    val prefs = uiState.value.preferences
    if (prefs.appLockEnabled && !prefs.pinHashRecord.isNullOrBlank()) {
      val elapsed = System.currentTimeMillis() - pausedAt
      if (elapsed >= PinSecurityManager.BACKGROUND_LOCK_GRACE_MILLIS) {
        _isAppLocked.value = true
      }
    }
  }

  fun lockAppNow() {
    val prefs = uiState.value.preferences
    if (prefs.appLockEnabled && !prefs.pinHashRecord.isNullOrBlank()) {
      _isAppLocked.value = true
    }
  }

  fun enableAppLockWithPin(pin: String, enableBiometric: Boolean) {
    if (!PinSecurityManager.isValidPinFormat(pin)) return
    viewModelScope.launch {
      val record = withContext(Dispatchers.Default) { PinSecurityManager.createPinRecord(pin) }
      preferencesRepository.enableAppLock(record, enableBiometric)
      _isAppLocked.value = false
      _snackbarMessages.tryEmit("App Lock enabled")
    }
  }

  fun updateAppPin(pin: String) {
    if (!PinSecurityManager.isValidPinFormat(pin)) return
    viewModelScope.launch {
      val record = withContext(Dispatchers.Default) { PinSecurityManager.createPinRecord(pin) }
      preferencesRepository.updatePin(record)
      _snackbarMessages.tryEmit("PIN updated")
    }
  }

  fun setBiometricUnlockEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.setBiometricUnlockEnabled(enabled)
      _snackbarMessages.tryEmit(
        if (enabled) "Biometric unlock enabled" else "Biometric unlock disabled"
      )
    }
  }

  fun disableAppLock() {
    viewModelScope.launch {
      preferencesRepository.disableAppLock()
      _isAppLocked.value = false
      _snackbarMessages.tryEmit("App Lock disabled")
    }
  }

  fun verifyPin(pin: String): Boolean {
    val prefs = uiState.value.preferences
    val now = System.currentTimeMillis()
    if (PinSecurityManager.isLockedOut(prefs.pinLockedUntilMillis, now)) {
      return false
    }
    val matches = PinSecurityManager.verifyPin(pin, prefs.pinHashRecord)
    if (matches) {
      _isAppLocked.value = false
      viewModelScope.launch {
        if (prefs.failedPinAttempts > 0 || prefs.pinLockedUntilMillis > 0L) {
          preferencesRepository.resetPinAttempts()
        }
      }
      return true
    } else {
      val nextAttempts = prefs.failedPinAttempts + 1
      val lockoutDuration = PinSecurityManager.lockoutDurationForAttempts(nextAttempts)
      val lockedUntil = if (lockoutDuration > 0L) now + lockoutDuration else 0L
      viewModelScope.launch {
        preferencesRepository.recordFailedPinAttempt(nextAttempts, lockedUntil)
      }
      return false
    }
  }

  fun unlockViaBiometric() {
    _isAppLocked.value = false
    viewModelScope.launch {
      preferencesRepository.resetPinAttempts()
    }
  }

  fun resetPinAfterDeviceAuth(newPin: String) {
    if (!PinSecurityManager.isValidPinFormat(newPin)) return
    viewModelScope.launch {
      val record = withContext(Dispatchers.Default) { PinSecurityManager.createPinRecord(newPin) }
      preferencesRepository.updatePin(record)
      _isAppLocked.value = false
      _snackbarMessages.tryEmit("PIN reset successfully")
    }
  }

  fun factoryResetForForgottenPin() {
    activeChatJob?.cancel()
    activeChatJob = null
    chatStreamingStateFlow.value = ChatStreamingState.Idle
    viewModelScope.launch {
      repository.clearAllUserData()
      preferencesRepository.disableAppLock()
      _isAppLocked.value = false
      _snackbarMessages.tryEmit("App Lock removed and local records erased")
    }
  }

  // Gemini Financial Assistant Chatbot
  fun sendChatMessage(rawPrompt: String) {
    val cleanPrompt = rawPrompt.trim()
    if (cleanPrompt.isEmpty()) return
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) return

    activeChatJob?.cancel()
    activeChatJob = viewModelScope.launch {
      val currentActive = resolveActiveSession()
      val sessionId = if (currentActive == null) {
        val autoTitle = ChatTitleGenerator.generateTitle(listOf(cleanPrompt))
        val createdId = repository.createChatSession(title = autoTitle, isManualTitle = false)
        activeChatSelectionFlow.value = ActiveChatSelection.Session(createdId)
        createdId
      } else {
        activeChatSelectionFlow.value = ActiveChatSelection.Session(currentActive.id)
        currentActive.id
      }

      val priorHistory = repository.getMessagesForSession(sessionId)
      repository.saveChatMessage(sessionId, ChatRole.USER, cleanPrompt)

      // Refine auto-generated title if initial messages were generic and title was not manually set
      if (currentActive != null && !currentActive.isTitleManuallySet) {
        val priorUserMessages = priorHistory.filter { it.role == ChatRole.USER }.map { it.content }
        val allPriorWereGeneric = priorUserMessages.all { ChatTitleGenerator.isGenericPrompt(it) }
        if (allPriorWereGeneric && !ChatTitleGenerator.isGenericPrompt(cleanPrompt)) {
          val refinedTitle = ChatTitleGenerator.generateTitle(priorUserMessages + cleanPrompt)
          repository.renameChatSession(sessionId, refinedTitle, isManual = false)
        }
      }

      streamAssistantResponse(
        sessionId = sessionId,
        priorHistory = priorHistory,
        userQuestion = cleanPrompt,
      )
    }
  }

  fun retryLastChatMessage() {
    val currentError = chatStreamingStateFlow.value as? ChatStreamingState.Error ?: return
    val retryPrompt = currentError.failedPrompt
    val sessionId = currentError.sessionId
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) return

    activeChatJob?.cancel()
    activeChatJob = viewModelScope.launch {
      val sessionMessages = repository.getMessagesForSession(sessionId)
      val historyBeforeFailedTurn = if (
        sessionMessages.lastOrNull()?.role == ChatRole.USER &&
        sessionMessages.lastOrNull()?.content == retryPrompt
      ) {
        sessionMessages.dropLast(1)
      } else {
        sessionMessages
      }
      streamAssistantResponse(
        sessionId = sessionId,
        priorHistory = historyBeforeFailedTurn,
        userQuestion = retryPrompt,
      )
    }
  }

  fun cancelActiveChatRequest() {
    activeChatJob?.cancel()
    activeChatJob = null
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) {
      chatStreamingStateFlow.value = ChatStreamingState.Idle
    }
  }

  fun startNewChat() {
    activeChatJob?.cancel()
    activeChatJob = null
    chatStreamingStateFlow.value = ChatStreamingState.Idle
    activeChatSelectionFlow.value = ActiveChatSelection.NewDraft
  }

  fun selectChatSession(sessionId: Long) {
    activeChatJob?.cancel()
    activeChatJob = null
    chatStreamingStateFlow.value = ChatStreamingState.Idle
    activeChatSelectionFlow.value = ActiveChatSelection.Session(sessionId)
  }

  fun renameChatSession(sessionId: Long, newTitle: String) {
    val cleanTitle = newTitle.trim()
    if (cleanTitle.isEmpty()) return
    viewModelScope.launch {
      repository.renameChatSession(
        sessionId = sessionId,
        newTitle = cleanTitle,
        isManual = true,
      )
      _snackbarMessages.tryEmit("Chat renamed")
    }
  }

  fun deleteChatSession(sessionId: Long) {
    val currentActiveId = chatUiState.value.activeSession?.id
    if (currentActiveId == sessionId) {
      activeChatJob?.cancel()
      activeChatJob = null
      chatStreamingStateFlow.value = ChatStreamingState.Idle
      activeChatSelectionFlow.value = ActiveChatSelection.AutoLatest
    }
    viewModelScope.launch {
      repository.deleteChatSession(sessionId)
      _snackbarMessages.tryEmit("Chat deleted")
    }
  }

  /**
   * Deletes a single message from the active conversation without affecting other messages or
   * other saved chat sessions. Cleans up the parent session only if zero messages remain and its
   * title was not manually assigned.
   */
  fun deleteChatMessage(messageId: Long) {
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) return
    viewModelScope.launch {
      val activeSession = resolveActiveSession() ?: return@launch
      val sessionMessages = repository.getMessagesForSession(activeSession.id)
      if (sessionMessages.none { it.id == messageId }) return@launch

      repository.deleteChatMessagesByIds(listOf(messageId))

      val remaining = repository.getMessagesForSession(activeSession.id)
      if (remaining.isEmpty() && !activeSession.isTitleManuallySet) {
        activeChatSelectionFlow.value = ActiveChatSelection.NewDraft
        repository.deleteChatSession(activeSession.id)
      }
      _snackbarMessages.tryEmit("Message deleted")
    }
  }

  fun editChatMessageAndRegenerate(messageId: Long, updatedPrompt: String) {
    val cleanPrompt = updatedPrompt.trim()
    if (cleanPrompt.isEmpty()) return
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) return

    activeChatJob?.cancel()
    activeChatJob = viewModelScope.launch {
      val activeSession = resolveActiveSession() ?: return@launch
      val sessionMessages = repository.getMessagesForSession(activeSession.id)
      val targetIndex = sessionMessages.indexOfFirst { it.id == messageId && it.role == ChatRole.USER }
      if (targetIndex == -1) return@launch

      val priorHistory = sessionMessages.subList(0, targetIndex)
      val toDeleteIds = sessionMessages.subList(targetIndex, sessionMessages.size).map { it.id }
      chatStreamingStateFlow.value = ChatStreamingState.Streaming(activeSession.id, "")
      repository.deleteChatMessagesByIds(toDeleteIds)
      repository.saveChatMessage(activeSession.id, ChatRole.USER, cleanPrompt)

      // Update auto-title if the edited prompt shapes the session topic and title isn't manual
      val latestSession = repository.getChatSession(activeSession.id)
      if (latestSession != null && !latestSession.isTitleManuallySet) {
        val updatedUserMessages = priorHistory.filter { it.role == ChatRole.USER }.map { it.content } + cleanPrompt
        val refreshedTitle = ChatTitleGenerator.generateTitle(updatedUserMessages)
        repository.renameChatSession(activeSession.id, refreshedTitle, isManual = false)
      }

      streamAssistantResponse(
        sessionId = activeSession.id,
        priorHistory = priorHistory,
        userQuestion = cleanPrompt,
      )
    }
  }

  fun regenerateAssistantMessage(assistantMessageId: Long) {
    if (chatStreamingStateFlow.value is ChatStreamingState.Streaming) return

    activeChatJob?.cancel()
    activeChatJob = viewModelScope.launch {
      val activeSession = resolveActiveSession() ?: return@launch
      val sessionMessages = repository.getMessagesForSession(activeSession.id)
      val targetIndex = sessionMessages.indexOfFirst { it.id == assistantMessageId && it.role == ChatRole.MODEL }
      if (targetIndex == -1) return@launch

      val precedingUserIndex = (targetIndex - 1 downTo 0).firstOrNull { sessionMessages[it].role == ChatRole.USER }
        ?: return@launch
      val userPrompt = sessionMessages[precedingUserIndex].content
      val priorHistory = sessionMessages.subList(0, precedingUserIndex)
      val toDeleteIds = sessionMessages.subList(targetIndex, sessionMessages.size).map { it.id }

      repository.deleteChatMessagesByIds(toDeleteIds)
      streamAssistantResponse(
        sessionId = activeSession.id,
        priorHistory = priorHistory,
        userQuestion = userPrompt,
      )
    }
  }

  fun clearChatHistory() {
    activeChatJob?.cancel()
    activeChatJob = null
    chatStreamingStateFlow.value = ChatStreamingState.Idle
    activeChatSelectionFlow.value = ActiveChatSelection.NewDraft
    viewModelScope.launch {
      repository.clearChatHistory()
      _snackbarMessages.tryEmit("Chat history cleared")
    }
  }

  private suspend fun resolveActiveSession(): ChatSessionEntity? {
    val sessions = repository.chatSessions.first()
    return when (val selection = activeChatSelectionFlow.value) {
      ActiveChatSelection.NewDraft -> null
      ActiveChatSelection.AutoLatest -> sessions.firstOrNull()
      is ActiveChatSelection.Session -> sessions.firstOrNull { it.id == selection.sessionId }
    }
  }

  private suspend fun streamAssistantResponse(
    sessionId: Long,
    priorHistory: List<ChatMessageEntity>,
    userQuestion: String,
  ) {
    chatStreamingStateFlow.value = ChatStreamingState.Streaming(sessionId, "")
    var latestAccumulated = ""
    try {
      val contextDigest = FinancialChatContextBuilder.buildContextDigest(uiState.value)
      withTimeout(CHAT_RESPONSE_TIMEOUT_MILLIS) {
        geminiChatService.streamReply(
          priorHistory = priorHistory,
          userQuestion = userQuestion,
          financialContextDigest = contextDigest,
        ).collect { partial ->
          latestAccumulated = partial
          chatStreamingStateFlow.value = ChatStreamingState.Streaming(sessionId, partial)
        }
      }
      val finalReply = latestAccumulated.trim()
      if (finalReply.isEmpty()) {
        chatStreamingStateFlow.value = ChatStreamingState.Error(
          sessionId = sessionId,
          message = "Assistant returned an empty response.",
          failedPrompt = userQuestion,
        )
      } else {
        repository.saveChatMessage(sessionId, ChatRole.MODEL, finalReply)
        chatStreamingStateFlow.value = ChatStreamingState.Idle
        scheduleIntelligentTitleRefinement(sessionId)
      }
    } catch (e: TimeoutCancellationException) {
      Log.e(TAG, "Chat response timed out after ${CHAT_RESPONSE_TIMEOUT_MILLIS}ms", e)
      chatStreamingStateFlow.value = ChatStreamingState.Error(
        sessionId = sessionId,
        message = "Request timed out after 60 seconds. Please check your connection and try again.",
        failedPrompt = userQuestion,
      )
    } catch (e: CancellationException) {
      chatStreamingStateFlow.value = ChatStreamingState.Idle
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Failed to generate chatbot reply", e)
      val detailedMessage = e.localizedMessage?.takeIf { it.isNotBlank() }
        ?: e.cause?.localizedMessage?.takeIf { it.isNotBlank() }
        ?: "Unable to reach the assistant right now (${e.javaClass.simpleName}). Please try again."
      chatStreamingStateFlow.value = ChatStreamingState.Error(
        sessionId = sessionId,
        message = detailedMessage,
        failedPrompt = userQuestion,
      )
    }
  }

  /**
   * Refines an automatically named conversation using both user prompts and assistant replies
   * during early turns, while strictly preserving any manually assigned title.
   */
  private fun scheduleIntelligentTitleRefinement(sessionId: Long) {
    viewModelScope.launch {
      val initialSession = repository.getChatSession(sessionId) ?: return@launch
      if (initialSession.isTitleManuallySet) return@launch

      val sessionMessages = repository.getMessagesForSession(sessionId)
      val userMessages = sessionMessages.filter { it.role == ChatRole.USER }.map { it.content }
      if (userMessages.isEmpty() || userMessages.size > 2) return@launch

      val assistantMessages = sessionMessages.filter { it.role == ChatRole.MODEL }.map { it.content }
      val aiRawTitle = geminiChatService.generateConversationTitle(sessionMessages)
      val sanitizedAiTitle = ChatTitleGenerator.sanitizeCandidateTitle(aiRawTitle, userMessages)
      val candidateTitle = sanitizedAiTitle
        ?: ChatTitleGenerator.generateTitle(userMessages, assistantMessages)

      // Re-check before persisting in case the user manually renamed the chat mid-request
      val latestSession = repository.getChatSession(sessionId) ?: return@launch
      if (!latestSession.isTitleManuallySet && candidateTitle != latestSession.title) {
        repository.renameChatSession(sessionId, candidateTitle, isManual = false)
      }
    }
  }

  private companion object {
    const val TAG = "BudgetWiseViewModel"
    const val CHAT_RESPONSE_TIMEOUT_MILLIS = 60_000L
  }
}
