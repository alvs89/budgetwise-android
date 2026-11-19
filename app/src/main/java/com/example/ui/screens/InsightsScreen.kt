package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.BudgetHealthStatus
import com.example.domain.FinanceCalculator
import com.example.domain.FinancialInsight
import com.example.domain.InsightSeverity
import com.example.domain.compactCategoryLabel
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.FintechCard
import com.example.ui.components.FintechProgressBar
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MaskedCurrencyText
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.MonthlyIncomeExpenseChart
import com.example.ui.components.SpendingTrendLineChart
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

@Composable
fun InsightsScreen(
  uiState: BudgetWiseUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  modifier: Modifier = Modifier,
) {
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

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column {
          Text("Insights", style = MaterialTheme.typography.displaySmall)
          Text(
            text = "Financial analytics from your local records",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // All-Time Lifetime Summary Card (independent of selected month)
      item {
        Surface(
          shape = MaterialTheme.shapes.large,
          shadowElevation = 2.dp,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("insights_all_time_overview_card"),
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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "ALL-TIME OVERVIEW",
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
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color.White.copy(alpha = 0.16f),
                  modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                  Text(
                    text = "${uiState.allTransactions.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  text = "Overall Net Cash Flow",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color.White.copy(alpha = 0.78f),
                )
                MaskedCurrencyText(
                  amount = uiState.allTimeNetCashFlow,
                  currencyCode = currency,
                  style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                  ),
                  color = Color.White,
                  autoResize = true,
                  minFontSize = 18.sp,
                  modifier = Modifier
                    .fillMaxWidth()
                    .testTag("insights_all_time_net_cash_flow"),
                )
              }

              HorizontalDivider(color = Color.White.copy(alpha = 0.16f))

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  AutoResizeSingleLineText(
                    text = "Total Income",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.74f),
                    minFontSize = 10.sp,
                  )
                  MaskedCurrencyText(
                    amount = uiState.allTimeIncome,
                    currencyCode = currency,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF86EFAC),
                    useCompactForLargeAmounts = true,
                    autoResize = true,
                    minFontSize = 12.sp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("insights_all_time_income"),
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  AutoResizeSingleLineText(
                    text = "Total Expenses",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.74f),
                    minFontSize = 10.sp,
                  )
                  MaskedCurrencyText(
                    amount = uiState.allTimeExpenses,
                    currencyCode = currency,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFFFCA5A5),
                    useCompactForLargeAmounts = true,
                    autoResize = true,
                    minFontSize = 12.sp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("insights_all_time_expenses"),
                  )
                }
              }

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  AutoResizeSingleLineText(
                    text = "Reserved in Goals",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.74f),
                    minFontSize = 10.sp,
                  )
                  MaskedCurrencyText(
                    amount = uiState.totalReservedSavings,
                    currencyCode = currency,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF6EE7B7),
                    useCompactForLargeAmounts = true,
                    autoResize = true,
                    minFontSize = 12.sp,
                    modifier = Modifier
                      .fillMaxWidth()
                      .testTag("insights_all_time_goal_savings"),
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  AutoResizeSingleLineText(
                    text = "Overall Savings Rate",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.74f),
                    minFontSize = 10.sp,
                  )
                  AutoResizeSingleLineText(
                    text = if (uiState.allTimeIncome > BigDecimal.ZERO) {
                      "${uiState.allTimeSavingsRatePercent.toPlainString()}%"
                    } else {
                      "—"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    minFontSize = 12.sp,
                    modifier = Modifier.fillMaxWidth(),
                  )
                }
              }
            }
          }
        }
      }

      item {
        Text(
          text = "Monthly Breakdown",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      item {
        MonthSelectorBar(
          selectedMonth = uiState.selectedMonth,
          onPreviousMonth = onPreviousMonth,
          onNextMonth = onNextMonth,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Equal-height symmetrical KPI Grid
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

      // Deterministic Observations
      item {
        Text("Key Observations", style = MaterialTheme.typography.titleMedium)
      }

      items(uiState.insights) { insight ->
        InsightCard(insight = insight)
      }

      // 6-Month Income vs Expenses Chart
      item {
        FintechCard(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("6-Month Income vs Expenses", style = MaterialTheme.typography.titleMedium)
          }
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap any month bar to inspect figures",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
          Text("Expense Distribution · ${uiState.selectedMonth.shortLabel()}", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "Tap any category row to highlight its share",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
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
          Text("Daily Spending Trend · ${uiState.selectedMonth.shortLabel()}", style = MaterialTheme.typography.titleMedium)
          Spacer(modifier = Modifier.height(10.dp))
          SpendingTrendLineChart(
            points = uiState.dailySpendingSeries,
            currencyCode = currency,
          )
        }
      }

      // Budget Performance Summary
      if (uiState.budgetSummaries.isNotEmpty()) {
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            Text("Budget Performance", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
              uiState.budgetSummaries.forEach { summary ->
                val color = when (summary.healthStatus) {
                  BudgetHealthStatus.NORMAL -> financeColors.income
                  BudgetHealthStatus.APPROACHING -> financeColors.warning
                  BudgetHealthStatus.NEAR_LIMIT -> financeColors.nearLimit
                  BudgetHealthStatus.EXCEEDED -> financeColors.expense
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = compactCategoryLabel(summary.category),
                      style = MaterialTheme.typography.bodyMedium,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.weight(1f),
                    )
                    Text(
                      text = "${summary.utilizationPercent.toPlainString()}%",
                      style = MaterialTheme.typography.labelLarge,
                      color = color,
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                  FintechProgressBar(
                    progress = (summary.utilizationPercent.toFloat() / 100f).coerceIn(0f, 1f),
                    color = color,
                    height = 7.dp,
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
