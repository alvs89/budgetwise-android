package com.example.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.local.entity.TransactionEntity
import com.example.domain.AlertDestination
import com.example.domain.ExportFormat
import com.example.domain.FinancialReportExporter
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.FinancialChatSheet
import com.example.ui.components.LaunchSplashOverlay
import com.example.ui.components.LocalIsAppLocked
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.TransactionEditorSheet
import com.example.ui.screens.ActivityScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.GoalsScreen
import com.example.ui.screens.HelpCenterScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.InsightsScreen
import com.example.ui.screens.ManageCategoriesScreen
import com.example.ui.screens.NotificationsSettingsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.BudgetWiseTheme
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import java.io.File

@Serializable object HomeRoute
@Serializable object ActivityRoute
@Serializable object BudgetsRoute
@Serializable object GoalsRoute
@Serializable object InsightsRoute
@Serializable object SettingsRoute
@Serializable object ManageCategoriesRoute
@Serializable object NotificationsSettingsRoute
@Serializable object HelpCenterRoute

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
  val context = LocalContext.current
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val isAppLocked by viewModel.isAppLocked.collectAsStateWithLifecycle()
  val isExportingReport by viewModel.isExportingReport.collectAsStateWithLifecycle()
  val pendingAlertDestination by viewModel.pendingAlertDestination.collectAsStateWithLifecycle()
  val chatUiState by viewModel.chatUiState.collectAsStateWithLifecycle()
  var showExportDialog by rememberSaveable { mutableStateOf(false) }
  var showNotificationsSheet by rememberSaveable { mutableStateOf(false) }

  // Register SAF CreateDocument launchers unconditionally at the root composable so they
  // are never disposed while the system file picker is open.
  val csvSaveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType),
  ) { uri ->
    if (uri != null) {
      viewModel.savePendingExportToUri(uri) {
        showExportDialog = false
      }
    } else {
      viewModel.clearPendingExport()
    }
  }

  val pdfSaveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType),
  ) { uri ->
    if (uri != null) {
      viewModel.savePendingExportToUri(uri) {
        showExportDialog = false
      }
    } else {
      viewModel.clearPendingExport()
    }
  }

  var activeToastMessage by remember { mutableStateOf<String?>(null) }
  var isToastVisible by remember { mutableStateOf(false) }
  var toastSequence by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { message ->
      activeToastMessage = message
      isToastVisible = true
      toastSequence += 1
    }
  }

  LaunchedEffect(toastSequence) {
    if (toastSequence > 0) {
      delay(2800L)
      isToastVisible = false
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
          CompositionLocalProvider(LocalIsAppLocked provides isAppLocked) {
          val navController = rememberNavController()
          val backStackEntry by navController.currentBackStackEntryAsState()
          val currentDestination = backStackEntry?.destination
          val isSettingsRoute =
            currentDestination?.hasRoute<SettingsRoute>() == true ||
              currentDestination?.hasRoute<ManageCategoriesRoute>() == true ||
              currentDestination?.hasRoute<NotificationsSettingsRoute>() == true ||
              currentDestination?.hasRoute<HelpCenterRoute>() == true

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteFromSheet by remember { mutableStateOf<TransactionEntity?>(null) }
    var showChatSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isAppLocked) {
      if (isAppLocked) {
        showExportDialog = false
        showNotificationsSheet = false
        showTransactionSheet = false
        editingTransaction = null
        pendingDeleteFromSheet = null
        showChatSheet = false
      }
    }

    if (showExportDialog && !isAppLocked) {
      ExportReportDialog(
        uiState = uiState,
        isExporting = isExportingReport,
        onRequestSave = { reportData, format ->
          val fileName = FinancialReportExporter.suggestedFileName(reportData, format)
          viewModel.preparePendingExport(reportData, format)
          viewModel.suppressNextBackgroundLock()
          try {
            when (format) {
              ExportFormat.CSV -> csvSaveLauncher.launch(fileName)
              ExportFormat.PDF -> pdfSaveLauncher.launch(fileName)
            }
          } catch (_: ActivityNotFoundException) {
            viewModel.clearPendingExport()
            viewModel.saveExportToDownloadsFallback(reportData, format) {
              showExportDialog = false
            }
          } catch (_: Exception) {
            viewModel.clearPendingExport()
            viewModel.saveExportToDownloadsFallback(reportData, format) {
              showExportDialog = false
            }
          }
        },
        onRequestShare = { reportData, format ->
          viewModel.prepareShareExportFile(reportData, format) { outFile, fmt, periodLabel ->
            viewModel.suppressNextBackgroundLock()
            val error = launchShareSheet(
              context = context,
              file = outFile,
              format = fmt,
              periodLabel = periodLabel,
            )
            if (error == null) {
              showExportDialog = false
            } else {
              viewModel.notifyUser(error)
            }
          }
        },
        onDismiss = { showExportDialog = false },
      )
    }

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

    fun navigateToAlertDestination(destination: AlertDestination) {
      when (destination) {
        AlertDestination.BUDGETS -> navigateTopLevel(BudgetsRoute)
        AlertDestination.GOALS -> navigateTopLevel(GoalsRoute)
        AlertDestination.ACTIVITY -> navigateTopLevel(ActivityRoute)
        AlertDestination.ADD_TRANSACTION -> {
          editingTransaction = null
          showTransactionSheet = true
        }
      }
    }

    LaunchedEffect(pendingAlertDestination, uiState.isLoading, isAppLocked) {
      val dest = pendingAlertDestination
      if (dest != null && !uiState.isLoading && !isAppLocked && uiState.preferences.onboardingCompleted) {
        navigateToAlertDestination(dest)
        viewModel.consumePendingAlertDestination()
      }
    }

    if (showNotificationsSheet && !isAppLocked) {
      NotificationCenterSheet(
        alerts = uiState.financialAlerts,
        unreadCount = uiState.unreadAlertCount,
        onAlertClick = { alert ->
          viewModel.markAlertAsRead(alert.id)
          showNotificationsSheet = false
          navigateToAlertDestination(alert.targetDestination)
        },
        onToggleAlertRead = { alert ->
          viewModel.toggleAlertRead(alert.id, alert.isRead)
        },
        onDismissAlert = viewModel::deleteAlert,
        onMarkAllRead = viewModel::markAllAlertsAsRead,
        onMarkAllUnread = viewModel::markAllAlertsAsUnread,
        onClearAll = viewModel::clearAllAlerts,
        onOpenSettings = {
          showNotificationsSheet = false
          navController.navigate(NotificationsSettingsRoute)
        },
        onDismiss = { showNotificationsSheet = false },
      )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useNavRail = maxWidth >= 600.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
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
                onOpenNotifications = { showNotificationsSheet = true },
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
                onExportData = { showExportDialog = true },
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
                onOpenManageCategories = { navController.navigate(ManageCategoriesRoute) },
                onOpenNotificationsSettings = { navController.navigate(NotificationsSettingsRoute) },
                onOpenHelpCenter = { navController.navigate(HelpCenterRoute) },
                onExportData = { showExportDialog = true },
                onResetDemoData = viewModel::resetDemonstrationData,
                onClearAllData = viewModel::clearAllRecords,
              )
            }
            composable<ManageCategoriesRoute> {
              ManageCategoriesScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onAddCategory = viewModel::addCustomCategory,
                onUpdateCategory = viewModel::updateCategory,
                onDeleteCategory = viewModel::deleteCustomCategory,
              )
            }
            composable<NotificationsSettingsRoute> {
              NotificationsSettingsScreen(
                uiState = uiState,
                onBack = { navController.popBackStack() },
                onToggleBudgetAlerts = viewModel::setBudgetAlertsEnabled,
                onToggleGoalAlerts = viewModel::setGoalAlertsEnabled,
                onToggleDailyReminder = viewModel::setDailyReminderEnabled,
                onUpdateBudgetWarningThreshold = viewModel::setBudgetWarningThreshold,
                onUpdateDailyReminderTime = viewModel::setDailyReminderTime,
              )
            }
            composable<HelpCenterRoute> {
              HelpCenterScreen(
                onBack = { navController.popBackStack() },
                onOpenAssistant = { showChatSheet = true },
              )
            }
          }
        }
        }
        }
          }
        }

        TopFloatingToastBanner(
          visible = isToastVisible && !activeToastMessage.isNullOrBlank() && !isAppLocked && !showLaunchSplash,
          message = activeToastMessage.orEmpty(),
          onDismiss = { isToastVisible = false },
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )

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

@Composable
private fun TopFloatingToastBanner(
  visible: Boolean,
  message: String,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  AnimatedVisibility(
    visible = visible,
    enter = slideInVertically(
      animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      initialOffsetY = { fullHeight -> -fullHeight - 24 },
    ) + fadeIn(animationSpec = tween(durationMillis = 180)),
    exit = slideOutVertically(
      animationSpec = tween(durationMillis = 200),
      targetOffsetY = { fullHeight -> -fullHeight - 24 },
    ) + fadeOut(animationSpec = tween(durationMillis = 160)),
    modifier = modifier,
  ) {
    Surface(
      onClick = onDismiss,
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      contentColor = MaterialTheme.colorScheme.onSurface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
      tonalElevation = 4.dp,
      shadowElevation = 8.dp,
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxWidth()
        .testTag("app_top_toast"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.CheckCircle,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = message,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

internal fun buildShareExportChooserIntent(
  context: Context,
  file: File,
  format: ExportFormat,
  periodLabel: String,
): Intent {
  val authority = "${context.packageName}.fileprovider"
  val contentUri = FileProvider.getUriForFile(context, authority, file)
  val sendIntent = Intent(Intent.ACTION_SEND).apply {
    type = format.mimeType
    putExtra(Intent.EXTRA_STREAM, contentUri)
    putExtra(Intent.EXTRA_SUBJECT, "BudgetWise Financial Report ($periodLabel)")
    putExtra(Intent.EXTRA_TITLE, file.name)
    clipData = ClipData.newUri(context.contentResolver, file.name, contentUri)
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
  return Intent.createChooser(sendIntent, "Share ${format.label} Report").apply {
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
}

private fun launchShareSheet(
  context: Context,
  file: File,
  format: ExportFormat,
  periodLabel: String,
): String? {
  return try {
    val chooser = buildShareExportChooserIntent(context, file, format, periodLabel)
    val hostActivity = context.findActivity()
    if (hostActivity != null) {
      hostActivity.startActivity(chooser)
    } else {
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    }
    null
  } catch (_: ActivityNotFoundException) {
    "No app available to share ${format.label} files"
  } catch (err: Exception) {
    "Share failed: ${err.localizedMessage ?: "Unable to share file"}"
  }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}
