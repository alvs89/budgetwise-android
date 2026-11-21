package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.FinancialChatService
import com.example.ai.GeminiFinancialChatService
import com.example.data.cloud.CloudProfileState
import com.example.data.cloud.CloudSyncStatus
import com.example.data.cloud.FirestoreSyncRepository
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.FinancialAlertEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.BudgetWiseRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.domain.AlertDestination
import com.example.domain.AlertThresholdDefaults
import com.example.domain.BudgetSummary
import com.example.domain.CategoryBreakdown
import com.example.domain.CategoryVisualMatcher
import com.example.domain.ChatRole
import com.example.domain.ChatTitleGenerator
import com.example.domain.DailySpendingPoint
import com.example.domain.ExportFormat
import com.example.domain.FinanceCalculator
import com.example.domain.FinancialAlertEvaluator
import com.example.domain.FinancialChatContextBuilder
import com.example.domain.FinancialInsight
import com.example.domain.FinancialReportData
import com.example.domain.FinancialReportExporter
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
import com.example.notifications.BudgetWiseNotificationManager
import com.example.notifications.DailyReminderScheduler
import java.io.File
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
import kotlinx.coroutines.flow.flowOf
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
  val allBudgets: List<BudgetEntity> = emptyList(),
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
  val financialAlerts: List<FinancialAlertEntity> = emptyList(),
  val unreadAlertCount: Int = 0,
  val syncStatus: CloudSyncStatus = CloudSyncStatus.SYNCED,
  val accountEmail: String? = null,
  val isCompletingOnboarding: Boolean = false,
  val cloudErrorMessage: String? = null,
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
  val alerts: List<FinancialAlertEntity>,
)

private data class CloudMetaInputs(
  val profileState: CloudProfileState,
  val syncStatus: CloudSyncStatus,
  val isCompletingOnboarding: Boolean,
  val cloudErrorMessage: String?,
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
  private val cloudRepository: FirestoreSyncRepository? = null,
  private val accountDisplayName: String? = null,
  private val accountEmail: String? = null,
) : AndroidViewModel(application) {

  private val preferencesRepository = UserPreferencesRepository(application)
  private val database = BudgetWiseDatabase.getInstance(application)
  private val repository = BudgetWiseRepository(database, preferencesRepository)

  private val transactionsSourceFlow = cloudRepository?.transactions ?: repository.transactions
  private val budgetsSourceFlow = cloudRepository?.budgets ?: repository.budgets
  private val goalsSourceFlow = cloudRepository?.savingsGoals ?: repository.savingsGoals
  private val contributionsSourceFlow = cloudRepository?.savingsContributions ?: repository.savingsContributions
  private val categoriesSourceFlow = cloudRepository?.categories ?: repository.categories
  private val profileSourceFlow = cloudRepository?.profileState ?: flowOf(CloudProfileState.Missing)
  private val syncStatusSourceFlow = cloudRepository?.syncStatus ?: flowOf(CloudSyncStatus.SYNCED)

  private val selectedMonthFlow = MutableStateFlow(YearMonthKey.current())
  private val activityFilterFlow = MutableStateFlow(ActivityFilterState())
  private val homePeriodScopeFlow = MutableStateFlow(HomePeriodScope.THIS_MONTH)
  private val isCompletingOnboardingFlow = MutableStateFlow(false)
  private val cloudErrorMessageFlow = MutableStateFlow<String?>(null)

  private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 8)
  val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

  private val _isAppLocked = MutableStateFlow(false)
  val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

  private val _isExportingReport = MutableStateFlow(false)
  val isExportingReport: StateFlow<Boolean> = _isExportingReport.asStateFlow()

  private val _pendingAlertDestination = MutableStateFlow<AlertDestination?>(null)
  val pendingAlertDestination: StateFlow<AlertDestination?> = _pendingAlertDestination.asStateFlow()

  private var pendingExportSnapshot: Pair<FinancialReportData, ExportFormat>? = null
  private var suppressNextBackgroundLockOnce: Boolean = false

  private val activeChatSelectionFlow = MutableStateFlow<ActiveChatSelection>(ActiveChatSelection.AutoLatest)
  private val chatStreamingStateFlow = MutableStateFlow<ChatStreamingState>(ChatStreamingState.Idle)
  private var activeChatJob: Job? = null
  private var clearChatJob: Job? = null

  private var backgroundedAtMillis: Long? = null

  init {
    BudgetWiseNotificationManager.ensureNotificationChannels(application)
    viewModelScope.launch {
      val initialPrefs = preferencesRepository.userPreferences.first()
      if (initialPrefs.appLockEnabled && !initialPrefs.pinHashRecord.isNullOrBlank()) {
        _isAppLocked.value = true
      }
      if (cloudRepository != null) {
        val seedName = accountDisplayName?.trim()?.takeIf { it.isNotBlank() }
          ?: initialPrefs.displayName
        cloudRepository.ensureProfileInitialized(
          defaultDisplayName = seedName,
          defaultCurrency = initialPrefs.preferredCurrency,
        ).onSuccess { profile ->
          preferencesRepository.updateDisplayName(profile.displayName)
          preferencesRepository.updatePreferredCurrency(profile.preferredCurrency)
        }.onFailure { err ->
          cloudErrorMessageFlow.value = err.localizedMessage ?: "Unable to initialize cloud account."
        }
      } else {
        repository.ensureInitialDataSeeded()
      }
      syncDailyReminderSchedule(initialPrefs)
      evaluateActiveAlertsOnStartup(initialPrefs)
    }
  }

  private val coreInputsFlow = combine(
    preferencesRepository.userPreferences,
    selectedMonthFlow,
    transactionsSourceFlow,
    budgetsSourceFlow,
    goalsSourceFlow,
  ) { prefs, month, transactions, budgets, goals ->
    CoreInputs(prefs, month, transactions, budgets, goals)
  }

  private val secondaryInputsFlow = combine(
    contributionsSourceFlow,
    categoriesSourceFlow,
    activityFilterFlow,
    homePeriodScopeFlow,
    repository.financialAlerts,
  ) { contributions, categories, filter, homeScope, alerts ->
    SecondaryInputs(contributions, categories, filter, homeScope, alerts)
  }

  private val cloudMetaInputsFlow = combine(
    profileSourceFlow,
    syncStatusSourceFlow,
    isCompletingOnboardingFlow,
    cloudErrorMessageFlow,
  ) { profileState, syncStatus, isCompleting, cloudError ->
    CloudMetaInputs(profileState, syncStatus, isCompleting, cloudError)
  }

  val uiState: StateFlow<BudgetWiseUiState> = combine(
    coreInputsFlow,
    secondaryInputsFlow,
    cloudMetaInputsFlow,
  ) { core, secondary, cloudMeta ->
    val isCloudProfileLoading = cloudRepository != null &&
      (cloudMeta.profileState is CloudProfileState.Loading || cloudMeta.profileState is CloudProfileState.Missing)
    val effectivePrefs = when (val ps = cloudMeta.profileState) {
      is CloudProfileState.Loaded -> core.prefs.copy(
        displayName = ps.profile.displayName,
        preferredCurrency = ps.profile.preferredCurrency,
        onboardingCompleted = ps.profile.onboardingCompleted,
      )
      else -> if (cloudRepository != null && !accountDisplayName.isNullOrBlank()) {
        core.prefs.copy(displayName = accountDisplayName.trim())
      } else {
        core.prefs
      }
    }
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
      currencyCode = effectivePrefs.preferredCurrency,
      maskAmounts = effectivePrefs.isBalanceHidden,
    )
    val filteredActivity = applyActivityFilter(
      transactions = transactions,
      selectedMonth = month,
      filter = secondary.activityFilter,
    )

    val resolvedCloudError = cloudMeta.cloudErrorMessage
      ?: (cloudMeta.profileState as? CloudProfileState.Error)?.message

    BudgetWiseUiState(
      isLoading = isCloudProfileLoading,
      preferences = effectivePrefs,
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
      allBudgets = core.budgets,
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
      financialAlerts = secondary.alerts,
      unreadAlertCount = secondary.alerts.count { !it.isRead },
      syncStatus = cloudMeta.syncStatus,
      accountEmail = accountEmail,
      isCompletingOnboarding = cloudMeta.isCompletingOnboarding,
      cloudErrorMessage = resolvedCloudError,
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
      if (cloudRepository != null) {
        val result = cloudRepository.saveTransaction(entity)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync transaction. Please try again.")
          return@launch
        }
      } else {
        repository.saveTransaction(entity)
      }
      val txMonth = YearMonthKey.fromEpochDay(dateEpochDay)
      selectedMonthFlow.value = txMonth
      _snackbarMessages.tryEmit(
        if (existingId == 0L) "Transaction added" else "Transaction updated"
      )
      if (type == TransactionType.EXPENSE) {
        evaluateBudgetThresholdForCategory(
          category = resolvedCategory,
          yearMonth = txMonth,
          dispatchSystemNotification = true,
        )
      }
    }
  }

  fun deleteTransaction(transaction: TransactionEntity) {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.deleteTransaction(transaction)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to delete transaction. Please try again.")
          return@launch
        }
      } else {
        repository.deleteTransaction(transaction)
      }
      _snackbarMessages.tryEmit("Transaction deleted")
      if (transaction.type == TransactionType.EXPENSE) {
        evaluateBudgetThresholdForCategory(
          category = transaction.category,
          yearMonth = YearMonthKey.fromEpochDay(transaction.dateEpochDay),
          dispatchSystemNotification = false,
        )
      }
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
      val budgetEntity = BudgetEntity(
        id = existingId,
        category = category,
        limitCentavos = limitCentavos,
        year = yearMonth.year,
        month = yearMonth.month,
        isSampleData = false,
      )
      val created = if (cloudRepository != null) {
        val result = cloudRepository.saveBudget(budgetEntity)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync budget. Please try again.")
          return@launch
        }
        result.getOrDefault(false)
      } else {
        repository.saveBudget(budgetEntity)
      }
      _snackbarMessages.tryEmit(
        if (created) "Budget created for $category" else "Budget updated for $category"
      )
      evaluateBudgetThresholdForCategory(
        category = category,
        yearMonth = yearMonth,
        dispatchSystemNotification = true,
      )
    }
  }

  fun deleteBudget(summary: BudgetSummary) {
    viewModelScope.launch {
      val budgetEntity = BudgetEntity(
        id = summary.budgetId,
        category = summary.category,
        limitCentavos = FinanceCalculator.bigDecimalToCentavos(summary.limitAmount),
        year = summary.yearMonth.year,
        month = summary.yearMonth.month,
        isSampleData = summary.isSampleData,
      )
      if (cloudRepository != null) {
        val result = cloudRepository.deleteBudget(budgetEntity)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to delete budget. Please try again.")
          return@launch
        }
      } else {
        repository.deleteBudget(budgetEntity)
      }
      repository.clearAlertDedupePrefix(
        FinancialAlertEvaluator.budgetDedupePrefix(summary.category, summary.yearMonth)
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
      if (cloudRepository != null) {
        val result = cloudRepository.saveGoal(entity)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync savings goal. Please try again.")
          return@launch
        }
      } else {
        repository.saveGoal(entity)
      }
      _snackbarMessages.tryEmit(if (existing == null) "Savings goal created" else "Savings goal updated")
      evaluateUpcomingGoalDeadlines(dispatchSystemNotification = true)
    }
  }

  fun deleteGoal(goal: SavingsGoalEntity) {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.deleteGoal(goal)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to delete savings goal. Please try again.")
          return@launch
        }
      } else {
        repository.deleteGoal(goal)
      }
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
      val previousGoal = goalsSourceFlow.first().firstOrNull { it.id == goalId }
      if (cloudRepository != null) {
        val result = cloudRepository.recordGoalContribution(
          goalId = goalId,
          amountCentavos = amountCentavos,
          isWithdrawal = isWithdrawal,
          note = note.trim(),
          dateEpochDay = dateEpochDay,
        )
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to record contribution. Please try again.")
          return@launch
        }
      } else {
        repository.recordGoalContribution(
          goalId = goalId,
          amountCentavos = amountCentavos,
          isWithdrawal = isWithdrawal,
          note = note.trim(),
          dateEpochDay = dateEpochDay,
        )
      }
      _snackbarMessages.tryEmit(
        if (isWithdrawal) "Savings adjustment recorded" else "Contribution added to goal"
      )
      if (!isWithdrawal && previousGoal != null) {
        val prefs = uiState.value.preferences
        if (prefs.goalAlertsEnabled) {
          val updatedSaved = (previousGoal.currentSavedCentavos + amountCentavos).coerceAtLeast(0L)
          val milestoneAlert = FinancialAlertEvaluator.evaluateGoalMilestoneAlert(
            goalId = previousGoal.id,
            goalName = previousGoal.name,
            previousSavedCentavos = previousGoal.currentSavedCentavos,
            newSavedCentavos = updatedSaved,
            targetCentavos = previousGoal.targetCentavos,
            currencyCode = prefs.preferredCurrency,
          )
          if (milestoneAlert != null) {
            val persisted = repository.insertAlertIfNew(milestoneAlert)
            if (persisted != null) {
              BudgetWiseNotificationManager.postSystemNotification(getApplication(), persisted)
            }
          }
        }
      }
    }
  }

  // Categories Management
  fun addCustomCategory(
    name: String,
    type: TransactionType,
    iconKey: String? = null,
    colorHex: Long? = null,
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
      val finalColorHex = colorHex ?: CategoryVisualMatcher.colorForIconOrName(
        iconKey = finalIconKey,
        name = cleanName,
        type = type,
        fallbackColor = inferredColorHex,
      )
      val entity = CategoryEntity(
        name = cleanName,
        type = type,
        iconKey = finalIconKey,
        colorHex = finalColorHex,
        isDefault = false,
      )
      if (cloudRepository != null) {
        val result = cloudRepository.addCategory(entity)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync new category. Please try again.")
          return@launch
        }
      } else {
        repository.addCategory(entity)
      }
      _snackbarMessages.tryEmit("Category \"$cleanName\" added")
    }
  }

  fun updateCategory(
    category: CategoryEntity,
    newName: String,
    newIconKey: String,
    newColorHex: Long,
  ) {
    viewModelScope.launch {
      val targetName = if (category.isDefault) category.name else newName.trim()
      if (targetName.isEmpty()) return@launch
      if (!category.isDefault && !targetName.equals(category.name, ignoreCase = true)) {
        val duplicate = uiState.value.categories.any {
          it.id != category.id && it.name.trim().equals(targetName, ignoreCase = true)
        }
        if (duplicate) {
          _snackbarMessages.tryEmit("Category \"$targetName\" already exists")
          return@launch
        }
      }
      if (cloudRepository != null) {
        val result = cloudRepository.updateCategory(
          existing = category,
          newName = targetName,
          newIconKey = newIconKey,
          newColorHex = newColorHex,
        )
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to update category. Please try again.")
          return@launch
        }
      } else {
        repository.updateCategory(
          existing = category,
          newName = targetName,
          newIconKey = newIconKey,
          newColorHex = newColorHex,
        )
      }
      _snackbarMessages.tryEmit("Category updated")
    }
  }

  fun deleteCustomCategory(category: CategoryEntity) {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.deleteCategory(category)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to remove category. Please try again.")
          return@launch
        }
      } else {
        repository.deleteCategory(category)
      }
      _snackbarMessages.tryEmit("Category removed")
    }
  }

  // Preferences, Onboarding & Data Management
  fun completeOnboarding(
    displayName: String,
    currencyCode: String,
    loadDemoData: Boolean = false,
  ) {
    viewModelScope.launch {
      isCompletingOnboardingFlow.value = true
      cloudErrorMessageFlow.value = null
      if (cloudRepository != null) {
        val result = cloudRepository.completeOnboarding(
          displayName = displayName,
          preferredCurrency = currencyCode,
          loadDemoData = loadDemoData,
        )
        if (result.isFailure) {
          cloudErrorMessageFlow.value =
            result.exceptionOrNull()?.localizedMessage ?: "Unable to finish setup. Please try again."
          isCompletingOnboardingFlow.value = false
          return@launch
        }
      } else if (loadDemoData) {
        repository.resetToDemonstrationData()
      }
      preferencesRepository.completeOnboarding(displayName, currencyCode)
      isCompletingOnboardingFlow.value = false
    }
  }

  fun skipOnboarding() {
    val currentPrefs = uiState.value.preferences
    completeOnboarding(
      displayName = currentPrefs.displayName,
      currencyCode = currentPrefs.preferredCurrency,
      loadDemoData = false,
    )
  }

  fun updateDisplayName(name: String) {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.updateDisplayName(name)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync display name. Please try again.")
          return@launch
        }
      }
      preferencesRepository.updateDisplayName(name)
      _snackbarMessages.tryEmit("Display name updated")
    }
  }

  fun updatePreferredCurrency(currencyCode: String) {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.updatePreferredCurrency(currencyCode)
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to sync currency. Please try again.")
          return@launch
        }
      }
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
      if (cloudRepository != null) {
        val result = cloudRepository.resetToDemonstrationData()
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to restore demonstration data. Please try again.")
          return@launch
        }
        repository.clearAllAlerts()
      } else {
        repository.resetToDemonstrationData()
      }
      selectedMonthFlow.value = YearMonthKey.current()
      _snackbarMessages.tryEmit("Demonstration dataset restored")
    }
  }

  fun clearAllRecords() {
    viewModelScope.launch {
      if (cloudRepository != null) {
        val result = cloudRepository.clearAllFinancialRecords()
        if (result.isFailure) {
          _snackbarMessages.tryEmit("Unable to clear financial records. Please try again.")
          return@launch
        }
        repository.clearAllAlerts()
        repository.clearChatHistory()
      } else {
        repository.clearAllUserData()
      }
      _snackbarMessages.tryEmit("All financial records cleared")
    }
  }

  fun prepareForSignOut() {
    activeChatJob?.cancel()
    activeChatJob = null
    chatStreamingStateFlow.value = ChatStreamingState.Idle
    _isAppLocked.value = false
  }

  fun notifyUser(message: String) {
    if (message.isNotBlank()) {
      _snackbarMessages.tryEmit(message)
    }
  }

  /**
   * Suppresses the next background-to-foreground App Lock check when intentionally handing off
   * control to Android's system file picker or share sheet.
   */
  fun suppressNextBackgroundLock() {
    suppressNextBackgroundLockOnce = true
  }

  fun preparePendingExport(data: FinancialReportData, format: ExportFormat) {
    pendingExportSnapshot = data to format
  }

  fun clearPendingExport() {
    pendingExportSnapshot = null
  }

  fun savePendingExportToUri(uri: Uri, onCompleted: () -> Unit = {}) {
    val snapshot = pendingExportSnapshot
    pendingExportSnapshot = null
    if (snapshot == null) {
      _snackbarMessages.tryEmit("Export expired. Please try again.")
      return
    }
    val (data, format) = snapshot
    val appContext = getApplication<Application>()
    val fileName = FinancialReportExporter.suggestedFileName(data, format)
    _isExportingReport.value = true

    viewModelScope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) {
          val bytes = FinancialReportExporter.buildReportBytes(data, format)
          FinancialReportExporter.writeBytesToDocumentUri(appContext, uri, bytes)
        }
      }
      _isExportingReport.value = false
      result.onSuccess {
        _snackbarMessages.tryEmit("Saved $fileName")
        onCompleted()
      }.onFailure { err ->
        Log.e("BudgetWiseExport", "Failed to save $fileName to SAF URI", err)
        _snackbarMessages.tryEmit("Export failed: ${err.localizedMessage ?: "Unable to write file"}")
      }
    }
  }

  fun saveExportToDownloadsFallback(
    data: FinancialReportData,
    format: ExportFormat,
    onCompleted: () -> Unit = {},
  ) {
    val appContext = getApplication<Application>()
    val fileName = FinancialReportExporter.suggestedFileName(data, format)
    _isExportingReport.value = true

    viewModelScope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) {
          val bytes = FinancialReportExporter.buildReportBytes(data, format)
          FinancialReportExporter.saveBytesToDownloadsFallback(
            context = appContext,
            fileName = fileName,
            mimeType = format.mimeType,
            bytes = bytes,
          )
        }
      }
      _isExportingReport.value = false
      result.onSuccess {
        _snackbarMessages.tryEmit("Saved to Downloads/BudgetWise/$fileName")
        onCompleted()
      }.onFailure { err ->
        Log.e("BudgetWiseExport", "Failed to save $fileName to Downloads", err)
        _snackbarMessages.tryEmit("Export failed: ${err.localizedMessage ?: "Unable to save file"}")
      }
    }
  }

  fun prepareShareExportFile(
    data: FinancialReportData,
    format: ExportFormat,
    onReady: (file: File, format: ExportFormat, periodLabel: String) -> Unit,
  ) {
    val appContext = getApplication<Application>()
    val fileName = FinancialReportExporter.suggestedFileName(data, format)
    _isExportingReport.value = true

    viewModelScope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) {
          val bytes = FinancialReportExporter.buildReportBytes(data, format)
          val exportsDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
          exportsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.name != fileName) {
              oldFile.delete()
            }
          }
          val outFile = File(exportsDir, fileName)
          outFile.writeBytes(bytes)
          outFile
        }
      }
      _isExportingReport.value = false
      result.onSuccess { outFile ->
        onReady(outFile, format, data.periodLabel)
      }.onFailure { err ->
        Log.e("BudgetWiseExport", "Failed to generate shareable report $fileName", err)
        _snackbarMessages.tryEmit("Share failed: ${err.localizedMessage ?: "Unable to prepare file"}")
      }
    }
  }

  // App Lock & PIN Security
  fun onAppMovedToBackground() {
    if (suppressNextBackgroundLockOnce) {
      backgroundedAtMillis = null
      return
    }
    val prefs = uiState.value.preferences
    if (prefs.appLockEnabled && !prefs.pinHashRecord.isNullOrBlank()) {
      backgroundedAtMillis = System.currentTimeMillis()
    }
  }

  fun onAppReturnedToForeground() {
    if (suppressNextBackgroundLockOnce) {
      suppressNextBackgroundLockOnce = false
      backgroundedAtMillis = null
      return
    }
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
      clearChatJob?.join()
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
    clearChatJob = viewModelScope.launch {
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

  // Financial Alerts & Notification Preferences
  fun setBudgetAlertsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.updateBudgetAlertsEnabled(enabled)
      if (enabled) {
        val prefs = preferencesRepository.userPreferences.first()
        evaluateAllCurrentMonthBudgets(prefs, dispatchSystemNotification = false)
      }
      _snackbarMessages.tryEmit(
        if (enabled) "Budget threshold alerts enabled" else "Budget threshold alerts paused"
      )
    }
  }

  fun setGoalAlertsEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.updateGoalAlertsEnabled(enabled)
      val prefs = preferencesRepository.userPreferences.first()
      syncDailyReminderSchedule(prefs)
      if (enabled) {
        evaluateUpcomingGoalDeadlines(dispatchSystemNotification = false)
      }
      _snackbarMessages.tryEmit(
        if (enabled) "Savings goal alerts enabled" else "Savings goal alerts paused"
      )
    }
  }

  fun setDailyReminderEnabled(enabled: Boolean) {
    viewModelScope.launch {
      preferencesRepository.updateDailyReminderEnabled(enabled)
      val prefs = preferencesRepository.userPreferences.first()
      syncDailyReminderSchedule(prefs)
      val timeLabel = AlertThresholdDefaults.formatReminderTime(
        prefs.dailyReminderHour,
        prefs.dailyReminderMinute,
      )
      _snackbarMessages.tryEmit(
        if (enabled) "Daily reminder scheduled for $timeLabel" else "Daily reminder turned off"
      )
    }
  }

  fun setBudgetWarningThreshold(percent: Int) {
    viewModelScope.launch {
      val sanitized = AlertThresholdDefaults.sanitizeThreshold(percent)
      preferencesRepository.updateBudgetWarningThreshold(sanitized)
      val prefs = preferencesRepository.userPreferences.first()
      if (prefs.budgetAlertsEnabled) {
        evaluateAllCurrentMonthBudgets(prefs, dispatchSystemNotification = false)
      }
      _snackbarMessages.tryEmit("Budget warning threshold set to $sanitized%")
    }
  }

  fun setDailyReminderTime(hour24: Int, minute: Int) {
    viewModelScope.launch {
      preferencesRepository.updateDailyReminderTime(hour24, minute)
      val prefs = preferencesRepository.userPreferences.first()
      syncDailyReminderSchedule(prefs)
      val formatted = AlertThresholdDefaults.formatReminderTime(hour24, minute)
      _snackbarMessages.tryEmit("Daily reminder time set to $formatted")
    }
  }

  fun markAlertAsRead(alertId: Long) {
    viewModelScope.launch {
      repository.markAlertAsRead(alertId)
    }
  }

  fun toggleAlertRead(alertId: Long, isCurrentlyRead: Boolean) {
    viewModelScope.launch {
      repository.setAlertRead(alertId, isRead = !isCurrentlyRead)
    }
  }

  fun markAllAlertsAsRead() {
    viewModelScope.launch {
      repository.markAllAlertsAsRead()
    }
  }

  fun markAllAlertsAsUnread() {
    viewModelScope.launch {
      repository.markAllAlertsAsUnread()
    }
  }

  fun deleteAlert(alertId: Long) {
    viewModelScope.launch {
      repository.deleteAlert(alertId)
    }
  }

  fun clearAllAlerts() {
    viewModelScope.launch {
      repository.clearAllAlerts()
      _snackbarMessages.tryEmit("Notifications cleared")
    }
  }

  fun handleNotificationDeepLink(destinationName: String?, alertId: Long = -1L) {
    if (alertId > 0L) {
      markAlertAsRead(alertId)
    }
    val destination = destinationName?.let { raw ->
      runCatching { AlertDestination.valueOf(raw) }.getOrNull()
    } ?: return
    _pendingAlertDestination.value = destination
  }

  fun consumePendingAlertDestination() {
    _pendingAlertDestination.value = null
  }

  private fun syncDailyReminderSchedule(prefs: UserPreferences) {
    DailyReminderScheduler.syncSchedule(
      context = getApplication(),
      dailyReminderEnabled = prefs.dailyReminderEnabled,
      goalAlertsEnabled = prefs.goalAlertsEnabled,
      hour24 = prefs.dailyReminderHour,
      minute = prefs.dailyReminderMinute,
    )
  }

  private suspend fun evaluateActiveAlertsOnStartup(prefs: UserPreferences) {
    if (prefs.budgetAlertsEnabled) {
      evaluateAllCurrentMonthBudgets(prefs, dispatchSystemNotification = false)
    }
    if (prefs.goalAlertsEnabled) {
      evaluateUpcomingGoalDeadlines(dispatchSystemNotification = false)
    }
  }

  private suspend fun evaluateAllCurrentMonthBudgets(
    prefs: UserPreferences,
    dispatchSystemNotification: Boolean,
  ) {
    val currentMonth = YearMonthKey.current()
    val budgets = budgetsSourceFlow.first()
      .filter { it.year == currentMonth.year && it.month == currentMonth.month }
    if (budgets.isEmpty()) return
    val transactions = transactionsSourceFlow.first()
    val monthExpenses = FinanceCalculator.filterTransactionsByMonth(transactions, currentMonth)
      .filter { it.type == TransactionType.EXPENSE }

    budgets.forEach { budget ->
      val spentCentavos = monthExpenses
        .filter { it.category.equals(budget.category, ignoreCase = true) }
        .sumOf { it.amountCentavos }
      val candidate = FinancialAlertEvaluator.evaluateBudgetAlert(
        category = budget.category,
        yearMonth = currentMonth,
        spentCentavos = spentCentavos,
        limitCentavos = budget.limitCentavos,
        warningThresholdPercent = prefs.budgetWarningThresholdPercent,
        currencyCode = prefs.preferredCurrency,
      )
      if (candidate != null) {
        val persisted = repository.insertAlertIfNew(candidate)
        if (persisted != null && dispatchSystemNotification) {
          BudgetWiseNotificationManager.postSystemNotification(getApplication(), persisted)
        }
      }
    }
  }

  private suspend fun evaluateBudgetThresholdForCategory(
    category: String,
    yearMonth: YearMonthKey,
    dispatchSystemNotification: Boolean,
  ) {
    val prefs = uiState.value.preferences
    if (!prefs.budgetAlertsEnabled) return

    val budget = budgetsSourceFlow.first().firstOrNull {
      it.year == yearMonth.year &&
        it.month == yearMonth.month &&
        it.category.equals(category, ignoreCase = true)
    } ?: return

    val monthExpenses = FinanceCalculator.filterTransactionsByMonth(
      transactionsSourceFlow.first(),
      yearMonth,
    ).filter {
      it.type == TransactionType.EXPENSE && it.category.equals(budget.category, ignoreCase = true)
    }
    val spentCentavos = monthExpenses.sumOf { it.amountCentavos }

    val candidate = FinancialAlertEvaluator.evaluateBudgetAlert(
      category = budget.category,
      yearMonth = yearMonth,
      spentCentavos = spentCentavos,
      limitCentavos = budget.limitCentavos,
      warningThresholdPercent = prefs.budgetWarningThresholdPercent,
      currencyCode = prefs.preferredCurrency,
    )

    if (candidate == null) {
      repository.clearAlertDedupePrefix(
        FinancialAlertEvaluator.budgetDedupePrefix(budget.category, yearMonth)
      )
      return
    }

    val persisted = repository.insertAlertIfNew(candidate)
    if (persisted != null && dispatchSystemNotification) {
      BudgetWiseNotificationManager.postSystemNotification(getApplication(), persisted)
    }
  }

  private suspend fun evaluateUpcomingGoalDeadlines(dispatchSystemNotification: Boolean) {
    val prefs = uiState.value.preferences
    if (!prefs.goalAlertsEnabled) return
    val goals = goalsSourceFlow.first()
    val alerts = FinancialAlertEvaluator.evaluateGoalDeadlineAlerts(
      goals = goals,
      todayEpochDay = currentEpochDay(),
      currencyCode = prefs.preferredCurrency,
    )
    alerts.forEach { candidate ->
      val persisted = repository.insertAlertIfNew(candidate)
      if (persisted != null && dispatchSystemNotification) {
        BudgetWiseNotificationManager.postSystemNotification(getApplication(), persisted)
      }
    }
  }

  companion object {
    private const val TAG = "BudgetWiseViewModel"
    private const val CHAT_RESPONSE_TIMEOUT_MILLIS = 60_000L

    fun provideFactory(
      application: Application,
      firestore: FirebaseFirestore,
      userId: String,
      accountDisplayName: String?,
      accountEmail: String?,
    ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val cloudRepo = FirestoreSyncRepository(db = firestore, userId = userId)
        return BudgetWiseViewModel(
          application = application,
          cloudRepository = cloudRepo,
          accountDisplayName = accountDisplayName,
          accountEmail = accountEmail,
        ) as T
      }
    }
  }
}
