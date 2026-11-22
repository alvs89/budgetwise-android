package com.example.ui

import android.app.Activity
import android.app.Application
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
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import kotlin.math.abs
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.data.cloud.GoogleAuthHelper
import com.example.data.cloud.GoogleSignInResult
import com.example.data.cloud.authStateFlow
import com.example.data.cloud.getBudgetWiseFirestore
import com.example.data.local.entity.TransactionEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.domain.AlertDestination
import com.example.domain.ExportFormat
import com.example.domain.FinancialReportExporter
import com.example.ui.components.BudgetWiseSpotlightSteps
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.ExportReportDialog
import com.example.ui.components.FinancialChatSheet
import com.example.ui.components.LaunchSplashOverlay
import com.example.ui.components.LocalIsAppLocked
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.NotificationCenterSheet
import com.example.ui.components.PinLockOverlay
import com.example.ui.components.SpotlightTab
import com.example.ui.components.SpotlightTargetId
import com.example.ui.components.SpotlightTourOverlay
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
import com.example.ui.screens.SignInScreen
import com.example.ui.theme.BudgetWiseTheme
import com.example.ui.viewmodel.AppToastMessage
import com.example.ui.viewmodel.BudgetWiseViewModel
import com.example.ui.viewmodel.ToastType
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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

/**
 * Auth-gated root entry point for BudgetWise.
 *
 * All Firestore reads/writes and user-scoped ViewModels are strictly scoped inside `key(user.uid)`
 * behind Google Sign-In via Jetpack Credential Manager.
 */
@Composable
fun BudgetWiseRoot(
  foregroundEvents: Flow<Unit>,
  backgroundEvents: Flow<Unit>,
  pendingNotificationDeepLink: StateFlow<Pair<String, Long>?>,
  onConsumeNotificationDeepLink: () -> Unit,
) {
  val context = LocalContext.current
  val auth = remember { Firebase.auth }
  val currentUser by remember(auth) { auth.authStateFlow() }
    .collectAsStateWithLifecycle(initialValue = auth.currentUser)

  val localPrefsRepo = remember(context) { UserPreferencesRepository(context.applicationContext) }
  val localPrefs by localPrefsRepo.userPreferences.collectAsStateWithLifecycle(
    initialValue = UserPreferences()
  )

  var showLaunchSplash by rememberSaveable { mutableStateOf(true) }
  var dashboardRevealed by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    if (showLaunchSplash) {
      delay(2680L)
      dashboardRevealed = true
      showLaunchSplash = false
    } else {
      dashboardRevealed = true
    }
  }

  var isSigningIn by remember { mutableStateOf(false) }
  var signInError by remember { mutableStateOf<String?>(null) }
  var attemptedAutoSignIn by rememberSaveable { mutableStateOf(false) }
  var authSessionNonce by rememberSaveable { mutableIntStateOf(0) }
  val coroutineScope = rememberCoroutineScope()

  LaunchedEffect(currentUser, attemptedAutoSignIn) {
    if (currentUser == null && !attemptedAutoSignIn) {
      attemptedAutoSignIn = true
      val activityCtx = context.findActivity() ?: context
      GoogleAuthHelper.attemptAutoSignIn(activityCtx, auth)
    } else if (currentUser != null) {
      isSigningIn = false
      signInError = null
    }
  }

  Box(modifier = Modifier.fillMaxSize()) {
    val activeUser = currentUser
    if (activeUser == null) {
      BudgetWiseTheme(themeMode = localPrefs.themeMode) {
        SignInScreen(
          isSigningIn = isSigningIn,
          errorMessage = signInError,
          onSignInWithGoogle = {
            if (isSigningIn) return@SignInScreen
            isSigningIn = true
            signInError = null
            coroutineScope.launch {
              val activityCtx = context.findActivity() ?: context
              when (val result = GoogleAuthHelper.onGoogleSignInClicked(activityCtx, auth)) {
                is GoogleSignInResult.Success -> {
                  isSigningIn = false
                  signInError = null
                }
                GoogleSignInResult.Cancelled -> {
                  isSigningIn = false
                }
                is GoogleSignInResult.Failure -> {
                  isSigningIn = false
                  signInError = result.message
                }
              }
            }
          },
        )
      }
    } else {
      val sessionScopeKey = "${activeUser.uid}_$authSessionNonce"
      val application = remember(context) {
        (context.applicationContext as? Application) ?: context.findActivity()?.application
      }
      if (application != null) {
      key(sessionScopeKey) {
        val firestore = remember(context) { getBudgetWiseFirestore(context.applicationContext) }
        val scopedViewModel: BudgetWiseViewModel = viewModel(
          key = sessionScopeKey,
          factory = BudgetWiseViewModel.provideFactory(
            application = application,
            firestore = firestore,
            userId = activeUser.uid,
            accountDisplayName = activeUser.displayName,
            accountEmail = activeUser.email,
            accountPhotoUrl = activeUser.photoUrl?.toString(),
          ),
        )

        LaunchedEffect(scopedViewModel) {
          foregroundEvents.collect { scopedViewModel.onAppReturnedToForeground() }
        }
        LaunchedEffect(scopedViewModel) {
          backgroundEvents.collect { scopedViewModel.onAppMovedToBackground() }
        }
        val pendingDeepLink by pendingNotificationDeepLink.collectAsStateWithLifecycle()
        LaunchedEffect(scopedViewModel, pendingDeepLink) {
          val deepLink = pendingDeepLink ?: return@LaunchedEffect
          scopedViewModel.handleNotificationDeepLink(deepLink.first, deepLink.second)
          onConsumeNotificationDeepLink()
        }

        BudgetWiseApp(
          viewModel = scopedViewModel,
          externalSplashActive = showLaunchSplash,
          externalDashboardRevealed = dashboardRevealed,
          onSignOut = {
            coroutineScope.launch {
              GoogleAuthHelper.signOut(
                context = context,
                auth = auth,
                onBeforeSignOut = {
                  scopedViewModel.prepareForSignOut()
                  authSessionNonce += 1
                },
              )
            }
          },
        )
      }
      }
    }

    BudgetWiseTheme(themeMode = localPrefs.themeMode) {
      LaunchSplashOverlay(visible = showLaunchSplash)
    }
  }
}

@Composable
fun BudgetWiseApp(
  viewModel: BudgetWiseViewModel,
  externalSplashActive: Boolean? = null,
  externalDashboardRevealed: Boolean? = null,
  onSignOut: () -> Unit = {},
) {
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

  var activeToast by remember { mutableStateOf<AppToastMessage?>(null) }
  var isToastVisible by remember { mutableStateOf(false) }
  var toastSequence by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    viewModel.snackbarMessages.collect { toast ->
      activeToast = toast
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

  var internalShowLaunchSplash by rememberSaveable { mutableStateOf(externalSplashActive == null) }
  var internalDashboardRevealed by rememberSaveable { mutableStateOf(externalDashboardRevealed != null) }
  var hasPlayedDashboardEntrance by rememberSaveable { mutableStateOf(false) }

  if (externalSplashActive == null) {
    LaunchedEffect(Unit) {
      if (internalShowLaunchSplash) {
        delay(2680L)
        internalDashboardRevealed = true
        internalShowLaunchSplash = false
      } else {
        internalDashboardRevealed = true
      }
    }
  }

  val showLaunchSplash = externalSplashActive ?: internalShowLaunchSplash
  val dashboardRevealed = externalDashboardRevealed ?: internalDashboardRevealed
  val effectiveAppLock = isAppLocked &&
    uiState.preferences.appLockEnabled &&
    !uiState.preferences.pinHashRecord.isNullOrBlank()

  val spotlightBounds = remember { mutableStateMapOf<SpotlightTargetId, Rect>() }
  val updateSpotlightBounds: (SpotlightTargetId, Rect) -> Unit = remember {
    { targetId, rect ->
      if (
        rect.left.isFinite() &&
        rect.top.isFinite() &&
        rect.right.isFinite() &&
        rect.bottom.isFinite() &&
        rect.width > 1f &&
        rect.height > 1f
      ) {
        val existing = spotlightBounds[targetId]
        val changedSignificantly = existing == null ||
          abs(existing.left - rect.left) >= 1f ||
          abs(existing.top - rect.top) >= 1f ||
          abs(existing.right - rect.right) >= 1f ||
          abs(existing.bottom - rect.bottom) >= 1f
        if (changedSignificantly) {
          spotlightBounds[targetId] = rect
        }
      }
    }
  }
  val updateSpotlightBoundsFromCoords: (SpotlightTargetId, LayoutCoordinates) -> Unit = remember {
    { targetId, coords ->
      if (coords.isAttached) {
        runCatching { coords.boundsInRoot() }.getOrNull()?.let { rect ->
          updateSpotlightBounds(targetId, rect)
        }
      }
    }
  }
  var isReplayingTour by rememberSaveable { mutableStateOf(false) }
  var spotlightStepIndex by rememberSaveable { mutableIntStateOf(0) }
  var tourScrollToTopSignal by remember { mutableIntStateOf(0) }

  LaunchedEffect(uiState.shouldShowSpotlightTour) {
    if (uiState.shouldShowSpotlightTour) {
      spotlightStepIndex = 0
      tourScrollToTopSignal += 1
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
        if (uiState.isLoading) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .testTag("cloud_profile_loading"),
            contentAlignment = Alignment.Center,
          ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
          }
        } else {
          CompositionLocalProvider(LocalIsAppLocked provides effectiveAppLock) {
          val navController = rememberNavController()
          val backStackEntry by navController.currentBackStackEntryAsState()
          val currentDestination = backStackEntry?.destination
          val isHomeRoute = currentDestination?.hasRoute<HomeRoute>() == true
          val isSettingsRoute =
            currentDestination?.hasRoute<SettingsRoute>() == true ||
              currentDestination?.hasRoute<ManageCategoriesRoute>() == true ||
              currentDestination?.hasRoute<NotificationsSettingsRoute>() == true ||
              currentDestination?.hasRoute<HelpCenterRoute>() == true

    var showTransactionSheet by remember { mutableStateOf(false) }
    var editingTransaction by remember { mutableStateOf<TransactionEntity?>(null) }
    var pendingDeleteFromSheet by remember { mutableStateOf<TransactionEntity?>(null) }
    var showChatSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(effectiveAppLock) {
      if (effectiveAppLock) {
        showExportDialog = false
        showNotificationsSheet = false
        showTransactionSheet = false
        editingTransaction = null
        pendingDeleteFromSheet = null
        showChatSheet = false
      }
    }

    if (showExportDialog && !effectiveAppLock) {
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
              viewModel.notifyUser(error, ToastType.ERROR)
            }
          }
        },
        onDismiss = { showExportDialog = false },
      )
    }

    if (showChatSheet && !effectiveAppLock) {
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

    if (showTransactionSheet && !effectiveAppLock) {
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

    if (!effectiveAppLock) {
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

    LaunchedEffect(pendingAlertDestination, uiState.isLoading, effectiveAppLock) {
      val dest = pendingAlertDestination
      if (dest != null && !uiState.isLoading && !effectiveAppLock) {
        navigateToAlertDestination(dest)
        viewModel.consumePendingAlertDestination()
      }
    }

    if (showNotificationsSheet && !effectiveAppLock) {
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
          val showQuickAddFab = currentDestination?.hasRoute<HomeRoute>() == true ||
            currentDestination?.hasRoute<ActivityRoute>() == true
          if (showQuickAddFab) {
            FloatingActionButton(
              onClick = {
                editingTransaction = null
                showTransactionSheet = true
              },
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier
                .onGloballyPositioned { coords ->
                  updateSpotlightBoundsFromCoords(SpotlightTargetId.FAB_ADD_TRANSACTION, coords)
                }
                .testTag("fab_add_transaction"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = "Add transaction")
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
                onOpenAssistant = { showChatSheet = true },
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
                scrollToTopSignal = tourScrollToTopSignal,
                onRegisterSpotlightBounds = updateSpotlightBounds,
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
                scrollToTopSignal = tourScrollToTopSignal,
                onRegisterSpotlightBounds = updateSpotlightBounds,
              )
            }
            composable<BudgetsRoute> {
              BudgetsScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
                onSaveBudget = viewModel::saveBudget,
                onDeleteBudget = viewModel::deleteBudget,
                onCopyPreviousMonthBudgets = viewModel::copyPreviousMonthBudgets,
                scrollToTopSignal = tourScrollToTopSignal,
                onRegisterSpotlightBounds = updateSpotlightBounds,
              )
            }
            composable<GoalsRoute> {
              GoalsScreen(
                uiState = uiState,
                onSaveGoal = viewModel::saveGoal,
                onDeleteGoal = viewModel::deleteGoal,
                onRecordContribution = viewModel::recordGoalContribution,
                scrollToTopSignal = tourScrollToTopSignal,
                onRegisterSpotlightBounds = updateSpotlightBounds,
              )
            }
            composable<InsightsRoute> {
              InsightsScreen(
                uiState = uiState,
                onPreviousMonth = viewModel::selectPreviousMonth,
                onNextMonth = viewModel::selectNextMonth,
                onOpenAssistant = { showChatSheet = true },
                scrollToTopSignal = tourScrollToTopSignal,
                onRegisterSpotlightBounds = updateSpotlightBounds,
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
                onLoadDemoData = viewModel::loadDemonstrationData,
                onClearAllData = viewModel::clearAllRecords,
                onSignOut = onSignOut,
                onPickProfilePhotoUri = viewModel::updateProfilePhotoFromUri,
                onSaveCroppedProfilePhoto = viewModel::saveCroppedProfilePhoto,
                onShowErrorToast = { msg -> viewModel.notifyUser(msg, ToastType.ERROR) },
                onUseGooglePhoto = viewModel::useGoogleProfilePhoto,
                onRemoveProfilePhoto = viewModel::removeProfilePhoto,
                onVerifyCloudSync = viewModel::verifyCloudSync,
                onSuppressNextBackgroundLock = viewModel::suppressNextBackgroundLock,
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
                onReplayWelcomeGuide = {
                  val popped = navController.popBackStack(HomeRoute, inclusive = false)
                  if (!popped) {
                    navigateTopLevel(HomeRoute)
                  }
                  spotlightStepIndex = 0
                  tourScrollToTopSignal += 1
                  isReplayingTour = true
                },
              )
            }
          }
        }
        }

        val showSpotlightOverlay = !uiState.isLoading &&
          !effectiveAppLock &&
          !showLaunchSplash &&
          !isSettingsRoute &&
          (uiState.shouldShowSpotlightTour || isReplayingTour)

        val safeTourIndex = spotlightStepIndex.coerceIn(0, BudgetWiseSpotlightSteps.lastIndex)
        val activeTourStep = BudgetWiseSpotlightSteps[safeTourIndex]

        val isTargetTabActive = when (activeTourStep.tab) {
          SpotlightTab.HOME -> currentDestination?.hasRoute<HomeRoute>() == true
          SpotlightTab.ACTIVITY -> currentDestination?.hasRoute<ActivityRoute>() == true
          SpotlightTab.BUDGETS -> currentDestination?.hasRoute<BudgetsRoute>() == true
          SpotlightTab.GOALS -> currentDestination?.hasRoute<GoalsRoute>() == true
          SpotlightTab.INSIGHTS -> currentDestination?.hasRoute<InsightsRoute>() == true
        }

        LaunchedEffect(showSpotlightOverlay, safeTourIndex) {
          if (showSpotlightOverlay) {
            val targetRoute: Any = when (activeTourStep.tab) {
              SpotlightTab.HOME -> HomeRoute
              SpotlightTab.ACTIVITY -> ActivityRoute
              SpotlightTab.BUDGETS -> BudgetsRoute
              SpotlightTab.GOALS -> GoalsRoute
              SpotlightTab.INSIGHTS -> InsightsRoute
            }
            val alreadyOnTargetTab = when (activeTourStep.tab) {
              SpotlightTab.HOME -> currentDestination?.hasRoute<HomeRoute>() == true
              SpotlightTab.ACTIVITY -> currentDestination?.hasRoute<ActivityRoute>() == true
              SpotlightTab.BUDGETS -> currentDestination?.hasRoute<BudgetsRoute>() == true
              SpotlightTab.GOALS -> currentDestination?.hasRoute<GoalsRoute>() == true
              SpotlightTab.INSIGHTS -> currentDestination?.hasRoute<InsightsRoute>() == true
            }
            if (!alreadyOnTargetTab) {
              spotlightBounds.remove(activeTourStep.targetId)
              navigateTopLevel(targetRoute)
            }
            tourScrollToTopSignal += 1
          }
        }

        fun finishSpotlightTour() {
          isReplayingTour = false
          spotlightStepIndex = 0
          if (uiState.shouldShowSpotlightTour) {
            viewModel.completeSpotlightTour()
          }
          if (!isHomeRoute) {
            navigateTopLevel(HomeRoute)
          }
          tourScrollToTopSignal += 1
        }

        SpotlightTourHost(
          visible = showSpotlightOverlay,
          currentStepIndex = safeTourIndex,
          isTargetTabActive = isTargetTabActive,
          spotlightBounds = spotlightBounds,
          onNext = {
            if (safeTourIndex < BudgetWiseSpotlightSteps.lastIndex) {
              spotlightStepIndex = safeTourIndex + 1
            } else {
              finishSpotlightTour()
            }
          },
          onBack = {
            spotlightStepIndex = (safeTourIndex - 1).coerceAtLeast(0)
          },
          onSkip = { finishSpotlightTour() },
          onDone = { finishSpotlightTour() },
        )
        }
          }
        }

        TopFloatingToastBanner(
          visible = isToastVisible && !activeToast?.message.isNullOrBlank() && !effectiveAppLock && !showLaunchSplash,
          message = activeToast?.message.orEmpty(),
          type = activeToast?.type ?: ToastType.SUCCESS,
          onDismiss = { isToastVisible = false },
          modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 12.dp, start = 16.dp, end = 16.dp),
        )

        PinLockOverlay(
          visible = !uiState.isLoading && effectiveAppLock,
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

        if (externalSplashActive == null) {
          LaunchSplashOverlay(
            visible = showLaunchSplash,
          )
        }
      }
    }
  }
}

@Composable
private fun SpotlightTourHost(
  visible: Boolean,
  currentStepIndex: Int,
  isTargetTabActive: Boolean,
  spotlightBounds: Map<SpotlightTargetId, Rect>,
  onNext: () -> Unit,
  onBack: () -> Unit,
  onSkip: () -> Unit,
  onDone: () -> Unit,
) {
  // Only read the SnapshotStateMap when the tour overlay is actually visible so layout bounds
  // updates never invalidate the parent Scaffold or NavHost composition scope.
  val boundsSnapshot = if (visible) spotlightBounds.toMap() else emptyMap()
  SpotlightTourOverlay(
    visible = visible,
    currentStepIndex = currentStepIndex,
    isTargetTabActive = isTargetTabActive,
    targetBounds = boundsSnapshot,
    onNext = onNext,
    onBack = onBack,
    onSkip = onSkip,
    onDone = onDone,
  )
}

@Composable
private fun TopFloatingToastBanner(
  visible: Boolean,
  message: String,
  type: ToastType,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val (iconVector, accentColor, borderAlpha) = when (type) {
    ToastType.SUCCESS -> Triple(Icons.Rounded.CheckCircle, MaterialTheme.colorScheme.primary, 0.38f)
    ToastType.ERROR -> Triple(Icons.Rounded.ErrorOutline, MaterialTheme.colorScheme.error, 0.50f)
    ToastType.INFO -> Triple(Icons.Rounded.Info, MaterialTheme.colorScheme.tertiary, 0.42f)
  }

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
      border = BorderStroke(1.dp, accentColor.copy(alpha = borderAlpha)),
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
          imageVector = iconVector,
          contentDescription = null,
          tint = accentColor,
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
