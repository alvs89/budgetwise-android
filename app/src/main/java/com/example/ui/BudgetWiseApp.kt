package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.FinancialChatSheet
import com.example.ui.components.LaunchSplashOverlay
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BudgetWiseTheme
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ActivityRoute
@Serializable object BudgetsRoute
@Serializable object GoalsRoute
@Serializable object InsightsRoute
@Serializable object SettingsRoute

private data class TopLevelDestination(
  val route: Any,
  val label: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val testTag: String,
)

private val TopLevelDestinations = listOf(
  TopLevelDestination(HomeRoute, "Home", Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
  TopLevelDestination(ActivityRoute, "Activity", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong, "nav_activity"),
  TopLevelDestination(BudgetsRoute, "Budgets", Icons.Rounded.PieChart, Icons.Outlined.PieChartOutline, "nav_budgets"),
  TopLevelDestination(GoalsRoute, "Goals", Icons.Rounded.Savings, Icons.Outlined.Savings, "nav_goals"),
  TopLevelDestination(InsightsRoute, "Insights", Icons.Rounded.Insights, Icons.Outlined.Insights, "nav_insights"),
)

@Composable
fun BudgetWiseApp(viewModel: BudgetWiseViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
  val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { message ->
      snackbarHostState.showSnackbar(message)
    }
  }

  var showLaunchSplash by rememberSaveable { mutableStateOf(true) }
  var dashboardRevealed by rememberSaveable { mutableStateOf(false) }
  var hasPlayedDashboardEntrance by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (showLaunchSplash) {
      delay(2680L)
      dashboardRevealed = true
      showLaunchSplash = false
    } else {
      dashboardRevealed = true
    }
  }

  BudgetWiseTheme(themeMode = uiState.preferences.themeMode) {
    CompositionLocalProvider(
      LocalIsBalanceHidden provides uiState.preferences.isBalanceHidden
    ) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MaterialTheme.colorScheme.background),
      ) {
        if (!uiState.isLoading) {
          val navController = rememberNavController()
          val backStackEntry by navController.currentBackStackEntryAsState()
          val currentDestination = backStackEntry?.destination
          val isSettingsRoute = currentDestination?.hasRoute<SettingsRoute>() == true

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteFromSheet by remember { mutableStateOf<TransactionEntity?>(null) }
    var showChatSheet by rememberSaveable { mutableStateOf(false) }

    if (showChatSheet && !isAppLocked) {
      FinancialChatSheet(
        uiState = uiState,
        chatUiState = chatUiState,
        onSendMessage = viewModel::sendChatMessage,
        onEditMessage = viewModel::editChatMessageAndRegenerate,
        onDeleteMessage = viewModel::deleteChatMessage,
        onRegenerateMessage = viewModel::regenerateAssistantMessage,
        onRetryLastMessage = viewModel::retryLastChatMessage,
        onCancelStreaming = viewModel::cancelActiveChatRequest,
        onNewChat = viewModel::startNewChat,
        onSelectSession = viewModel::selectChatSession,
        onRenameSession = viewModel::renameChatSession,
        onDeleteSession = viewModel::deleteChatSession,
        onDismiss = { showChatSheet = false },
      )
    }

    if (showTransactionSheet && !isAppLocked) {
      TransactionEditorSheet(
        existingTransaction = editingTransaction,
        categories = uiState.categories,
        currencyCode = uiState.preferences.preferredCurrency,
        onSave = { id, title, amountCentavos, type, category, dateEpochDay, paymentMethod, notes, existingCreatedAt ->
          viewModel.saveTransaction(
            existingId = id,
            title = title,
            amountCentavos = amountCentavos,
            type = type,
            category = category,
            dateEpochDay = dateEpochDay,
            paymentMethod = paymentMethod,
            notes = notes,
            existingCreatedAt = existingCreatedAt,
          )
          showTransactionSheet = false
          editingTransaction = null
        },
        onDelete = { tx -> pendingDeleteFromSheet = tx },
        onDismiss = {
          showTransactionSheet = false
          editingTransaction = null
        },
      )
    }

    if (!isAppLocked) {
      pendingDeleteFromSheet?.let { tx ->
        ConfirmationDialog(
          title = "Delete Transaction?",
          message = "Remove \"${tx.title}\" from your records?",
          confirmLabel = "Delete",
          onConfirm = {
            viewModel.deleteTransaction(tx)
            pendingDeleteFromSheet = null
            showTransactionSheet = false
            editingTransaction = null
          },
          onDismiss = { pendingDeleteFromSheet = null },
        )
      }
    }

    fun navigateTopLevel(route: Any) {
      navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
          saveState = true
        }
        launchSingleTop = true
        restoreState = true
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
          if (!useNavRail && !isSettingsRoute) {
            NavigationBar {
              TopLevelDestinations.forEach { dest ->
                val selected = when (dest.route) {
                  is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                  is ActivityRoute -> currentDestination?.hasRoute<ActivityRoute>() == true
                  is BudgetsRoute -> currentDestination?.hasRoute<BudgetsRoute>() == true
                  is GoalsRoute -> currentDestination?.hasRoute<GoalsRoute>() == true
                  is InsightsRoute -> currentDestination?.hasRoute<InsightsRoute>() == true
                  else -> false
                }
                NavigationBarItem(
                  selected = selected,
                  onClick = { navigateTopLevel(dest.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label) },
                  modifier = Modifier.testTag(dest.testTag),
                )
              }
            }
          }
        },
        floatingActionButton = {
          if (!isSettingsRoute) {
            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              SmallFloatingActionButton(
                onClick = { showChatSheet = true },
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("fab_open_chat"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Forum,
                  contentDescription = stringResource(R.string.chat_fab_label),
                )
              }

              FloatingActionButton(
                onClick = {
                  editingTransaction = null
                  showTransactionSheet = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_add_transaction"),
              ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
              }
            }
          }
        },
      ) { innerPadding ->
        Row(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        ) {
          if (useNavRail && !isSettingsRoute) {
            NavigationRail {
              TopLevelDestinations.forEach { dest ->
                val selected = when (dest.route) {
                  is HomeRoute -> currentDestination?.hasRoute<HomeRoute>() == true
                  is ActivityRoute -> currentDestination?.hasRoute<ActivityRoute>() == true
                  is BudgetsRoute -> currentDestination?.hasRoute<BudgetsRoute>() == true
                  is GoalsRoute -> currentDestination?.hasRoute<GoalsRoute>() == true
                  is InsightsRoute -> currentDestination?.hasRoute<InsightsRoute>() == true
                  else -> false
                }
                NavigationRailItem(
                  selected = selected,
                  onClick = { navigateTopLevel(dest.route) },
                  icon = {
                    Icon(
                      imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                      contentDescription = dest.label,
                    )
                  },
                  label = { Text(dest.label) },
                  modifier = Modifier.testTag("rail_${dest.testTag}"),
                )
              }
            }
          }

          NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.weight(1f),
          ) {
            composable<HomeRoute> {
              HomeScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
                onHomePeriodScopeChange = viewModel::setHomePeriodScope,
                onToggleBalanceHidden = viewModel::toggleBalanceHidden,
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onNavigateToActivity = { navigateTopLevel(ActivityRoute) },
                onNavigateToBudgets = { navigateTopLevel(BudgetsRoute) },
                onNavigateToGoals = { navigateTopLevel(GoalsRoute) },
                onNavigateToInsights = { navigateTopLevel(InsightsRoute) },
                onEditTransaction = { tx ->
                  editingTransaction = tx
                  showTransactionSheet = true
                },
                onDeleteTransaction = viewModel::deleteTransaction,
                onAddTransaction = {
                  editingTransaction = null
                  showTransactionSheet = true
                },
                dashboardRevealed = dashboardRevealed,
                hasPlayedEntrance = hasPlayedDashboardEntrance,
                onEntranceAnimationPlayed = { hasPlayedDashboardEntrance = true },
              )
            }
            composable<ActivityRoute> {
              ActivityScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onTypeFilterChange = viewModel::updateTypeFilter,
                onCategoryFilterChange = viewModel::updateCategoryFilter,
                onToggleMonthOnly = viewModel::toggleMonthOnlyFilter,
                onSortOrderChange = viewModel::updateSortOrder,
                onClearFilters = viewModel::clearActivityFilters,
                onEditTransaction = { tx ->
                  editingTransaction = tx
                  showTransactionSheet = true
                },
                onDeleteTransaction = viewModel::deleteTransaction,
                onAddTransaction = {
                  editingTransaction = null
                  showTransactionSheet = true
                },
              )
            }
            composable<BudgetsRoute> {
              BudgetsScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
                onSaveBudget = viewModel::saveBudget,
                onDeleteBudget = viewModel::deleteBudget,
              )
            }
            composable<GoalsRoute> {
              GoalsScreen(
                uiState = uiState,
                onSaveGoal = viewModel::saveGoal,
                onDeleteGoal = viewModel::deleteGoal,
                onRecordContribution = viewModel::recordGoalContribution,
              )
            }
            composable<InsightsRoute> {
              InsightsScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
              )
            }
            composable<SettingsRoute> {
              SettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onUpdateDisplayName = viewModel::updateDisplayName,
                onUpdateCurrency = viewModel::updatePreferredCurrency,
                onUpdateThemeMode = viewModel::updateThemeMode,
                onUpdateBalanceHidden = viewModel::setBalanceHidden,
                onVerifyCurrentPin = viewModel::verifyPin,
                onEnableAppLockWithPin = viewModel::enableAppLockWithPin,
                onChangeAppPin = viewModel::updateAppPin,
                onToggleBiometricUnlock = viewModel::setBiometricUnlockEnabled,
                onDisableAppLock = viewModel::disableAppLock,
                onLockAppNow = viewModel::lockAppNow,
                onAddCategory = viewModel::addCustomCategory,
                onDeleteCategory = viewModel::deleteCustomCategory,
                onResetDemoData = viewModel::resetDemonstrationData,
                onClearAllData = viewModel::clearAllRecords,
              )
            }
          }
        }
        }
        }
        }

        PinLockOverlay(
          visible = !uiState.isLoading && isAppLocked,
          splashActive = showLaunchSplash,
          biometricEnabled = uiState.preferences.biometricUnlockEnabled,
          failedAttempts = uiState.preferences.failedPinAttempts,
          lockedUntilMillis = uiState.preferences.pinLockedUntilMillis,
          onVerifyPin = viewModel::verifyPin,
          onBiometricSuccess = viewModel::unlockViaBiometric,
          onResetPinAfterDeviceAuth = viewModel::resetPinAfterDeviceAuth,
          onDisableAppLockAfterDeviceAuth = viewModel::disableAppLock,
          onFactoryResetForForgottenPin = viewModel::factoryResetForForgottenPin,
        )

        LaunchSplashOverlay(
          visible = showLaunchSplash,
        )
      }
    }
  }
}
