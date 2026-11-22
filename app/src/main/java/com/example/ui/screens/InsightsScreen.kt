package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.DonutLarge
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.FinanceCalculator
import com.example.domain.FinancialInsight
import com.example.domain.InsightSeverity
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FintechCard
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.MonthlyIncomeExpenseChart
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.components.SpotlightTargetId
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

@Composable
fun InsightsScreen(
  uiState: BudgetWiseUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onOpenAssistant: () -> Unit = {},
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
  val currency = uiState.preferences.preferredCurrency
  val isBalanceHidden = LocalIsBalanceHidden.current
  val financeColors = LocalFinanceColors.current
  val overallBudgetUtil = FinanceCalculator.calculateBudgetUtilizationPercent(
    uiState.totalBudgetSpent,
    uiState.totalBudgetLimit,
  )
  val remainingBudget = FinanceCalculator.calculateRemainingBudget(
    limit = uiState.totalBudgetLimit,
    actualSpent = uiState.totalBudgetSpent,
  )

  val hasMultiMonthData = uiState.monthlySeries.any {
    it.income > BigDecimal.ZERO || it.expenses > BigDecimal.ZERO
  }
  val hasExpenseData = uiState.expenseBreakdown.isNotEmpty()
  val hasDailyData = uiState.dailySpendingSeries.isNotEmpty()
  val hasAnyChartData = hasMultiMonthData || hasExpenseData || hasDailyData

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      state = listState,
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(end = 8.dp),
          ) {
            Text("Insights", style = MaterialTheme.typography.displaySmall)
            Text(
              text = "Spending patterns and financial trends",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
          ) {
            IconButton(
              onClick = onOpenAssistant,
              modifier = Modifier
                .size(40.dp)
                .testTag("insights_assistant_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = "Ask financial assistant",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp),
              )
            }
          }
        }
      }

      // Monthly Breakdown + Symmetrical KPI Grid (spotlighted together for Tour Step 6)
      item {
        val isMonthlyDeficit = uiState.netCashFlow < BigDecimal.ZERO
        val savingsSubtext = when {
          uiState.totalIncome <= BigDecimal.ZERO && uiState.totalExpenses <= BigDecimal.ZERO -> "No activity logged"
          uiState.netCashFlow >= BigDecimal.ZERO -> {
            val savedFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
              amount = uiState.netCashFlow,
              currencyCode = currency,
              isHidden = isBalanceHidden,
              omitZeroDecimalsForStandard = true,
            )
            "$savedFormatted net saved"
          }
          else -> {
            val deficitFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
              amount = uiState.netCashFlow.abs(),
              currencyCode = currency,
              isHidden = isBalanceHidden,
              omitZeroDecimalsForStandard = true,
            )
            "$deficitFormatted deficit"
          }
        }
        val budgetSubtext = when {
          uiState.totalBudgetLimit <= BigDecimal.ZERO -> "No active limits"
          remainingBudget >= BigDecimal.ZERO -> {
            val remainingFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
              amount = remainingBudget,
              currencyCode = currency,
              isHidden = isBalanceHidden,
              omitZeroDecimalsForStandard = true,
            )
            "$remainingFormatted remaining"
          }
          else -> {
            val overFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
              amount = remainingBudget.abs(),
              currencyCode = currency,
              isHidden = isBalanceHidden,
              omitZeroDecimalsForStandard = true,
            )
            "$overFormatted over limit"
          }
        }

        Column(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              if (coords.isAttached) {
                runCatching { coords.boundsInRoot() }.getOrNull()?.let { rect ->
                  onRegisterSpotlightBounds(SpotlightTargetId.INSIGHTS_OVERVIEW, rect)
                }
              }
            },
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          MonthSelectorBar(
            selectedMonth = uiState.selectedMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier.fillMaxWidth(),
          )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          FintechCard(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("insights_savings_rate_card"),
            fillHeight = true,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            AutoResizeSingleLineText(
              text = "Savings Rate",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              minFontSize = 10.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoResizeSingleLineText(
              text = if (uiState.totalIncome > BigDecimal.ZERO || isMonthlyDeficit) {
                "${uiState.savingsRatePercent.toPlainString()}%"
              } else {
                "—"
              },
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = when {
                isMonthlyDeficit -> financeColors.expense
                uiState.totalIncome <= BigDecimal.ZERO -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> financeColors.income
              },
              minFontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            AutoResizeSingleLineText(
              text = savingsSubtext,
              style = MaterialTheme.typography.bodySmall,
              color = if (isMonthlyDeficit) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
              minFontSize = 10.sp,
            )
          }

          FintechCard(
            modifier = Modifier
              .weight(1f)
              .fillMaxHeight()
              .testTag("insights_budget_utilization_card"),
            fillHeight = true,
            verticalArrangement = Arrangement.SpaceBetween,
          ) {
            AutoResizeSingleLineText(
              text = "Budget Utilization",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              minFontSize = 10.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AutoResizeSingleLineText(
              text = if (uiState.totalBudgetLimit > BigDecimal.ZERO) "${overallBudgetUtil.toPlainString()}%" else "—",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = if (uiState.totalBudgetLimit > BigDecimal.ZERO) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              minFontSize = 15.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            AutoResizeSingleLineText(
              text = budgetSubtext,
              style = MaterialTheme.typography.bodySmall,
              color = if (uiState.totalBudgetLimit > BigDecimal.ZERO && remainingBudget < BigDecimal.ZERO) {
                financeColors.expense
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              minFontSize = 10.sp,
            )
          }
        }
        }
      }

      // Charts surfaced high above the fold; consolidated into one clean empty state when no data exists
      if (!hasAnyChartData) {
        item {
          EmptyStatePanel(
            icon = Icons.Rounded.AutoGraph,
            title = "No analytics for ${uiState.selectedMonth.monthNameOnly()} yet",
            message = "Log income or expense transactions to unlock 6-month cash flow trends, category breakdowns, and daily spending charts.",
            modifier = Modifier.testTag("insights_empty_charts_panel"),
          )
        }
      } else {
        // 6-Month Income vs Expenses Chart
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.AutoGraph,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text("6-Month Income vs Expenses", style = MaterialTheme.typography.titleMedium)
            }
            if (hasMultiMonthData) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap any month bar to inspect figures",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            MonthlyIncomeExpenseChart(
              series = uiState.monthlySeries,
              currencyCode = currency,
            )
          }
        }

        // Expense Distribution Donut Chart
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.DonutLarge,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Expense Distribution · ${uiState.selectedMonth.monthNameOnly()}",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            if (hasExpenseData) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap any category row to highlight its share",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(12.dp))
            CategoryDonutChart(
              breakdown = uiState.expenseBreakdown,
              currencyCode = currency,
            )
          }
        }

        // Daily Spending Trend Line Chart
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Rounded.ShowChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = "Daily Spending Trend · ${uiState.selectedMonth.monthNameOnly()}",
                style = MaterialTheme.typography.titleMedium,
              )
            }
            if (hasDailyData) {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Tap any point to inspect daily spending",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            Spacer(modifier = Modifier.height(10.dp))
            SpendingTrendLineChart(
              points = uiState.dailySpendingSeries,
              currencyCode = currency,
              selectedMonth = uiState.selectedMonth,
            )
          }
        }
      }

      // Key Observations (actionable deterministic observations including budget warnings)
      if (uiState.insights.isNotEmpty() && hasAnyChartData) {
        item {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Rounded.Lightbulb,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text("Key Observations", style = MaterialTheme.typography.titleMedium)
          }
        }

        items(uiState.insights) { insight ->
          InsightCard(insight = insight)
        }
      }
    }
  }
}

@Composable
private fun InsightCard(insight: FinancialInsight) {
  val financeColors = LocalFinanceColors.current
  val (icon, tint) = when (insight.severity) {
    InsightSeverity.POSITIVE -> Icons.Rounded.CheckCircleOutline to financeColors.income
    InsightSeverity.INFO -> Icons.Rounded.Info to MaterialTheme.colorScheme.primary
    InsightSeverity.WARNING -> Icons.Rounded.WarningAmber to financeColors.warning
    InsightSeverity.ALERT -> Icons.Rounded.ErrorOutline to financeColors.expense
  }

  FintechCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.Top,
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = tint,
          modifier = Modifier.size(20.dp),
        )
      }
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(insight.title, style = MaterialTheme.typography.titleSmall, color = tint)
        Text(
          text = insight.message,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}
