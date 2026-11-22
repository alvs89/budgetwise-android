package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.data.local.entity.TransactionEntity
import com.example.domain.FinanceCalculator
import com.example.ui.components.AllTimeMonthlySpendingChart
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CompactEmptyBanner
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.FintechCard
import com.example.ui.components.FintechProgressBar
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MaskedCurrencyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.components.SpotlightTargetId
import com.example.ui.components.SwipeToRevealDeleteRow
import com.example.ui.components.TopExpenseCategoriesOverview
import com.example.ui.components.TransactionRowItem
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetWiseUiState
import com.example.ui.viewmodel.HomePeriodScope
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  uiState: BudgetWiseUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onHomePeriodScopeChange: (HomePeriodScope) -> Unit,
  onToggleBalanceHidden: () -> Unit,
  onOpenNotifications: () -> Unit,
  onOpenSettings: () -> Unit,
  onOpenAssistant: () -> Unit = {},
  onNavigateToActivity: () -> Unit,
  onNavigateToBudgets: () -> Unit,
  onNavigateToGoals: () -> Unit,
  onNavigateToInsights: () -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onDeleteTransaction: (TransactionEntity) -> Unit,
  onAddTransaction: () -> Unit,
  dashboardRevealed: Boolean = true,
  hasPlayedEntrance: Boolean = true,
  onEntranceAnimationPlayed: () -> Unit = {},
  scrollToTopSignal: Int = 0,
  onRegisterSpotlightBounds: (SpotlightTargetId, Rect) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier,
) {
  val listState = rememberLazyListState()
  LaunchedEffect(scrollToTopSignal) {
    if (scrollToTopSignal > 0) {
      listState.scrollToItem(0)
    }
  }
  val financeColors = LocalFinanceColors.current
  val currency = uiState.preferences.preferredCurrency
  val isAllTime = uiState.homePeriodScope == HomePeriodScope.ALL_TIME
  val displayedNetCashFlow = if (isAllTime) uiState.allTimeNetCashFlow else uiState.netCashFlow
  val displayedSavingsRate = if (isAllTime) uiState.allTimeSavingsRatePercent else uiState.savingsRatePercent
  val displayedIncome = if (isAllTime) uiState.allTimeIncome else uiState.totalIncome
  val displayedExpenses = if (isAllTime) uiState.allTimeExpenses else uiState.totalExpenses
  var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

  pendingDeleteTx?.let { tx ->
    ConfirmationDialog(
      title = "Delete Transaction?",
      message = "Remove \"${tx.title}\" from your records?",
      confirmLabel = "Delete",
      onConfirm = {
        onDeleteTransaction(tx)
        pendingDeleteTx = null
      },
      onDismiss = { pendingDeleteTx = null },
    )
  }

  val recentTransactionsSource = if (isAllTime) uiState.allTransactions else uiState.monthTransactions
  val recentTransactions = recentTransactionsSource
    .sortedWith(compareByDescending<TransactionEntity> { it.dateEpochDay }.thenByDescending { it.createdAtMillis })
    .take(5)

  LaunchedEffect(dashboardRevealed, hasPlayedEntrance) {
    if (dashboardRevealed && !hasPlayedEntrance) {
      delay(340L)
      onEntranceAnimationPlayed()
    }
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      // Top Greeting (full-width Row 1) + Title & Sleek Action Icons (Row 2)
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = "GOOD DAY, ${uiState.preferences.displayName.uppercase()}",
            style = MaterialTheme.typography.labelSmall.copy(
              letterSpacing = 0.6.sp,
              fontWeight = FontWeight.SemiBold,
            ),
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("home_greeting_text"),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AutoResizeSingleLineText(
              text = "My Finances",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
              minFontSize = 20.sp,
              modifier = Modifier.weight(1f),
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              ) {
                IconButton(
                  onClick = onOpenAssistant,
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("home_assistant_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = "Open financial assistant",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(19.dp),
                  )
                }
              }
              Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.onGloballyPositioned { coords ->
                  if (coords.isAttached) {
                    runCatching { coords.boundsInRoot() }.getOrNull()?.let { rect ->
                      onRegisterSpotlightBounds(SpotlightTargetId.TOP_ACTION_BAR, rect)
                    }
                  }
                },
              ) {
                val isHidden = LocalIsBalanceHidden.current
                IconButton(
                  onClick = onOpenNotifications,
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("notifications_button"),
                ) {
                  BadgedBox(
                    badge = {
                      if (uiState.unreadAlertCount > 0) {
                        Badge(
                          containerColor = MaterialTheme.colorScheme.error,
                          contentColor = MaterialTheme.colorScheme.onError,
                        ) {
                          Text(
                            text = if (uiState.unreadAlertCount > 9) "9+" else uiState.unreadAlertCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                          )
                        }
                      }
                    },
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Notifications,
                      contentDescription = if (uiState.unreadAlertCount > 0) {
                        "Notifications (${uiState.unreadAlertCount} unread)"
                      } else {
                        "Open notifications"
                      },
                      tint = if (uiState.unreadAlertCount > 0) {
                        MaterialTheme.colorScheme.primary
                      } else {
                        MaterialTheme.colorScheme.onSurface
                      },
                      modifier = Modifier.size(21.dp),
                    )
                  }
                }
                IconButton(
                  onClick = onToggleBalanceHidden,
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("privacy_toggle_button"),
                ) {
                  Icon(
                    imageVector = if (isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                    contentDescription = if (isHidden) "Show balances" else "Hide balances",
                    tint = if (isHidden) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                  )
                }
                IconButton(
                  onClick = onOpenSettings,
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("settings_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "Open settings",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(21.dp),
                  )
                }
              }
            }
          }
        }
      }

      // Period Scope Toggle (This Month vs All Time)
      item {
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        ) {
          HomePeriodScope.entries.forEachIndexed { index, scopeOption ->
            SegmentedButton(
              selected = uiState.homePeriodScope == scopeOption,
              onClick = { onHomePeriodScopeChange(scopeOption) },
              shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = HomePeriodScope.entries.size,
              ),
              icon = {},
              modifier = Modifier.testTag(
                if (scopeOption == HomePeriodScope.THIS_MONTH) {
                  "home_scope_this_month"
                } else {
                  "home_scope_all_time"
                }
              ),
            ) {
              AutoResizeSingleLineText(
                text = scopeOption.label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LocalContentColor.current,
                minFontSize = 11.sp,
              )
            }
          }
        }
      }

      // Month Selector (visible when viewing monthly scope)
      item {
        AnimatedVisibility(
          visible = !isAllTime,
          enter = fadeIn(tween(180)) + expandVertically(tween(200)),
          exit = fadeOut(tween(150)) + shrinkVertically(tween(180)),
        ) {
          MonthSelectorBar(
            selectedMonth = uiState.selectedMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      // Hero Net Cash Flow Card + Income & Expense Summary Cards (unified spotlight block for Step 1)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              if (coords.isAttached) {
                runCatching { coords.boundsInRoot() }.getOrNull()?.let { rect ->
                  onRegisterSpotlightBounds(SpotlightTargetId.HOME_OVERVIEW, rect)
                }
              }
            },
          verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          Surface(
            shape = MaterialTheme.shapes.large,
            modifier = Modifier
              .fillMaxWidth()
              .staggeredEntrance(
                revealed = dashboardRevealed,
                alreadyPlayed = hasPlayedEntrance,
                delayMillis = 0L,
              ),
            shadowElevation = 2.dp,
          ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(financeColors.heroStart, financeColors.heroEnd)
                )
              )
              .padding(20.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = if (isAllTime) "OVERALL NET CASH FLOW" else "NET CASH FLOW",
                  style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Color.White.copy(alpha = 0.85f),
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp),
                )
                val isHidden = LocalIsBalanceHidden.current
                val isDeficit = displayedExpenses > displayedIncome && displayedExpenses > BigDecimal.ZERO
                val badgeText = if (isDeficit) {
                  val deficitCompact = FinanceCalculator.formatCompactCurrencyOrMasked(
                    amount = displayedExpenses.subtract(displayedIncome),
                    currencyCode = currency,
                    isHidden = isHidden,
                    omitZeroDecimalsForStandard = true,
                  )
                  "Deficit • $deficitCompact"
                } else {
                  "Savings rate ${displayedSavingsRate.toPlainString()}%"
                }
                Surface(
                  shape = RoundedCornerShape(50),
                  color = if (isDeficit) Color(0xFFF87171).copy(alpha = 0.24f) else Color.White.copy(alpha = 0.16f),
                  modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                  Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (isDeficit) Color(0xFFFECACA) else Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              MaskedCurrencyText(
                amount = displayedNetCashFlow,
                currencyCode = currency,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                autoResize = true,
                minFontSize = 22.sp,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("hero_net_cash_flow"),
              )

              Text(
                text = if (isAllTime) {
                  "Cumulative across ${uiState.allTransactions.size} recorded transactions"
                } else {
                  "Based on recorded monthly transactions"
                },
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }

        // Side-by-side Equal-Height Income & Expense Summary Cards
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .staggeredEntrance(
              revealed = dashboardRevealed,
              alreadyPlayed = hasPlayedEntrance,
              delayMillis = 40L,
            ),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          FintechCard(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            fillHeight = true,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(financeColors.incomeContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowDownward,
                  contentDescription = null,
                  tint = financeColors.income,
                  modifier = Modifier.size(16.dp),
                )
              }
              AutoResizeSingleLineText(
                text = if (isAllTime) "Total Income" else "Income",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minFontSize = 11.sp,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MaskedCurrencyText(
              amount = displayedIncome,
              currencyCode = currency,
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = financeColors.income,
              useCompactForLargeAmounts = true,
              autoResize = true,
              minFontSize = 12.sp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("home_income_total"),
            )
          }

          FintechCard(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight(),
            fillHeight = true,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(30.dp)
                  .clip(CircleShape)
                  .background(financeColors.expenseContainer),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.ArrowUpward,
                  contentDescription = null,
                  tint = financeColors.expense,
                  modifier = Modifier.size(16.dp),
                )
              }
              AutoResizeSingleLineText(
                text = if (isAllTime) "Total Expenses" else "Expenses",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minFontSize = 11.sp,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            MaskedCurrencyText(
              amount = displayedExpenses,
              currencyCode = currency,
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = financeColors.expense,
              useCompactForLargeAmounts = true,
              autoResize = true,
              minFontSize = 12.sp,
              modifier = Modifier
                .fillMaxWidth()
                .testTag("home_expenses_total"),
            )
          }
        }
        }
      }

      // Top Expense Categories & Daily Spending Overview Card
      item {
        val activeBreakdown = if (isAllTime) uiState.allTimeExpenseBreakdown else uiState.expenseBreakdown
        val hasSpendingTrend = if (isAllTime) {
          uiState.allTimeMonthlySpendingSeries.isNotEmpty()
        } else {
          uiState.dailySpendingSeries.isNotEmpty()
        }
        FintechCard(
          modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(
              revealed = dashboardRevealed,
              alreadyPlayed = hasPlayedEntrance,
              delayMillis = 80L,
            ),
        ) {
          SectionHeader(
            title = "Spending overview",
            icon = Icons.Rounded.ReceiptLong,
            actionLabel = "Insights",
            onActionClick = onNavigateToInsights,
          )
          Spacer(modifier = Modifier.height(8.dp))
          if (activeBreakdown.isEmpty() && !hasSpendingTrend) {
            Text(
              text = if (isAllTime) {
                "No expenses recorded yet."
              } else {
                "No expenses recorded for this month yet."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          } else {
            TopExpenseCategoriesOverview(
              breakdown = activeBreakdown,
              currencyCode = currency,
              emptyMessage = if (isAllTime) {
                "No expenses recorded yet."
              } else {
                "No expenses recorded for this month yet."
              },
            )
            if (hasSpendingTrend) {
              Spacer(modifier = Modifier.height(16.dp))
              if (isAllTime) {
                AllTimeMonthlySpendingChart(
                  points = uiState.allTimeMonthlySpendingSeries,
                  currencyCode = currency,
                )
              } else {
                SpendingTrendLineChart(
                  points = uiState.dailySpendingSeries,
                  currencyCode = currency,
                  selectedMonth = uiState.selectedMonth,
                )
              }
            }
          }
        }
      }

      // Recent Transactions (prioritized right below Spending overview for daily checks)
      item {
        SectionHeader(
          title = "Recent transactions",
          icon = Icons.Rounded.ReceiptLong,
          actionLabel = "See all",
          onActionClick = onNavigateToActivity,
        )
      }

      if (recentTransactions.isEmpty()) {
        item {
          CompactEmptyBanner(
            icon = Icons.Rounded.ReceiptLong,
            title = "No transactions yet",
            subtitle = "Tap to log your first entry",
            onClick = onAddTransaction,
            modifier = Modifier.testTag("home_empty_transactions_banner"),
          )
        }
      } else {
        items(recentTransactions, key = { "home_tx_${it.id}" }) { tx ->
          SwipeToRevealDeleteRow(
            onDelete = { pendingDeleteTx = tx },
            deleteTestTag = "home_swipe_delete_transaction_${tx.id}",
          ) { isRevealed, closeReveal ->
            TransactionRowItem(
              transaction = tx,
              currencyCode = currency,
              onClick = {
                if (isRevealed) closeReveal() else onEditTransaction(tx)
              },
              categories = uiState.categories,
            )
          }
        }
      }

      // Budget Overview
      item {
        SectionHeader(
          title = if (isAllTime) "Budget overview · ${uiState.selectedMonth.monthNameOnly()}" else "Budget overview",
          icon = Icons.Rounded.AccountBalanceWallet,
          actionLabel = "All budgets",
          onActionClick = onNavigateToBudgets,
        )
      }

      if (uiState.budgetSummaries.isEmpty()) {
        item {
          CompactEmptyBanner(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "No budgets for ${uiState.selectedMonth.monthNameOnly()}",
            subtitle = "Set monthly spending limits",
            onClick = onNavigateToBudgets,
            modifier = Modifier.testTag("home_empty_budgets_banner"),
          )
        }
      } else {
        items(uiState.budgetSummaries.take(3), key = { "home_budget_${it.budgetId}" }) { summary ->
          BudgetProgressCard(
            summary = summary,
            currencyCode = currency,
            onClick = onNavigateToBudgets,
            categories = uiState.categories,
          )
        }
      }

      // Savings Goals Preview
      item {
        SectionHeader(
          title = "Savings goals",
          icon = Icons.Rounded.Savings,
          actionLabel = "All goals",
          onActionClick = onNavigateToGoals,
        )
      }

      if (uiState.goalSummaries.isEmpty()) {
        item {
          CompactEmptyBanner(
            icon = Icons.Rounded.Savings,
            title = "No savings goals yet",
            subtitle = "Create a savings target",
            onClick = onNavigateToGoals,
            modifier = Modifier.testTag("home_empty_goals_banner"),
          )
        }
      } else {
        item {
          FintechCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = onNavigateToGoals,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
              uiState.goalSummaries.take(3).forEach { goal ->
                val progress = (goal.completionPercent.toFloat() / 100f).coerceIn(0f, 1f)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = goal.name,
                      style = MaterialTheme.typography.titleSmall,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Text(
                      text = "${goal.completionPercent.toPlainString()}%",
                      style = MaterialTheme.typography.labelLarge,
                      color = MaterialTheme.colorScheme.primary,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                  FintechProgressBar(
                    progress = progress,
                    color = MaterialTheme.colorScheme.primary,
                    height = 7.dp,
                  )
                  val isHidden = LocalIsBalanceHidden.current
                  val savedStr = FinanceCalculator.formatCurrencyOrMasked(goal.savedAmount, currency, isHidden)
                  val targetStr = FinanceCalculator.formatCurrencyOrMasked(goal.targetAmount, currency, isHidden)
                  Text(
                    text = "$savedStr saved of $targetStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun Modifier.staggeredEntrance(
  revealed: Boolean,
  alreadyPlayed: Boolean,
  delayMillis: Long,
): Modifier = composed {
  if (alreadyPlayed) {
    return@composed this
  }
  val density = LocalDensity.current
  val travelPx = with(density) { 8.dp.toPx() }
  val alphaAnim = remember { Animatable(0f) }
  val offsetAnim = remember { Animatable(travelPx) }

  LaunchedEffect(revealed) {
    if (revealed) {
      if (delayMillis > 0L) delay(delayMillis)
      launch {
        alphaAnim.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        )
      }
      launch {
        offsetAnim.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
        )
      }
    }
  }

  graphicsLayer {
    alpha = alphaAnim.value
    translationY = offsetAnim.value
  }
}
