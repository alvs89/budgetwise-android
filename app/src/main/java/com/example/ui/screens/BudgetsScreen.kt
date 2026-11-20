package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.BudgetSummary
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.ui.components.AmountInputWithCalculator
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.BudgetProgressCard
import com.example.ui.components.CategoryPickerBottomSheet
import com.example.ui.components.CategorySelectorField
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FintechProgressBar
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SectionHeader
import com.example.ui.components.StableFormDialog
import com.example.ui.components.SwipeToRevealDeleteRow
import com.example.ui.components.sortCategoriesOtherLast
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

@Composable
fun BudgetsScreen(
  uiState: BudgetWiseUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSaveBudget: (existingId: Long, category: String, limitCentavos: Long, yearMonth: YearMonthKey) -> Unit,
  onDeleteBudget: (BudgetSummary) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showEditor by remember { mutableStateOf(false) }
  var editingBudget by remember { mutableStateOf<BudgetSummary?>(null) }
  var pendingDeleteBudget by remember { mutableStateOf<BudgetSummary?>(null) }

  val currency = uiState.preferences.preferredCurrency
  val financeColors = LocalFinanceColors.current

  if (showEditor) {
    BudgetEditorDialog(
      existing = editingBudget,
      categories = uiState.categories,
      selectedMonth = uiState.selectedMonth,
      currencyCode = currency,
      onSave = { id, category, limitCentavos ->
        onSaveBudget(id, category, limitCentavos, uiState.selectedMonth)
        showEditor = false
        editingBudget = null
      },
      onDelete = editingBudget?.let { summary ->
        {
          showEditor = false
          editingBudget = null
          pendingDeleteBudget = summary
        }
      },
      onDismiss = {
        showEditor = false
        editingBudget = null
      },
    )
  }

  pendingDeleteBudget?.let { budget ->
    ConfirmationDialog(
      title = "Delete Budget?",
      message = "Remove the ${budget.category} budget for ${budget.yearMonth.displayLabel()}?",
      confirmLabel = "Delete",
      onConfirm = {
        onDeleteBudget(budget)
        pendingDeleteBudget = null
      },
      onDismiss = { pendingDeleteBudget = null },
    )
  }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            AutoResizeSingleLineText(
              text = "Budgets",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
              minFontSize = 20.sp,
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            )
            FilledTonalButton(
              onClick = {
                editingBudget = null
                showEditor = true
              },
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier.testTag("add_budget_button"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
              Text("New Budget", maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
            }
          }
          Text(
            text = "Monthly category spending limits",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      item {
        MonthSelectorBar(
          selectedMonth = uiState.selectedMonth,
          onPreviousMonth = onPreviousMonth,
          onNextMonth = onNextMonth,
          modifier = Modifier.fillMaxWidth(),
        )
      }

      // Overall Monthly Budget Summary Hero Card
      item {
        val overallUtil = FinanceCalculator.calculateBudgetUtilizationPercent(
          uiState.totalBudgetSpent,
          uiState.totalBudgetLimit,
        )
        val overallRemaining = FinanceCalculator.calculateRemainingBudget(
          uiState.totalBudgetLimit,
          uiState.totalBudgetSpent,
        )
        val progress = (overallUtil.toFloat() / 100f).coerceIn(0f, 1f)
        val heroBarColor = when {
          overallUtil >= BigDecimal("100") -> Color(0xFFF87171)
          overallUtil >= BigDecimal("90") -> Color(0xFFFB923C)
          overallUtil >= BigDecimal("70") -> Color(0xFFFBBF24)
          else -> Color(0xFF4ADE80)
        }

        Surface(
          shape = MaterialTheme.shapes.large,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
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
                  text = "TOTAL BUDGETED",
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
                    text = "${overallUtil.toPlainString()}% used",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              val isHidden = LocalIsBalanceHidden.current
              val spentText = FinanceCalculator.formatCurrencyOrMasked(
                amount = uiState.totalBudgetSpent,
                currencyCode = currency,
                isHidden = isHidden,
                omitZeroDecimals = true,
              )
              val limitText = FinanceCalculator.formatCurrencyOrMasked(
                amount = uiState.totalBudgetLimit,
                currencyCode = currency,
                isHidden = isHidden,
                omitZeroDecimals = true,
              )
              AutoResizeSingleLineText(
                text = "$spentText / $limitText",
                style = MaterialTheme.typography.headlineMedium.copy(
                  fontSize = 26.sp,
                  fontWeight = FontWeight.Bold,
                ),
                color = Color.White,
                minFontSize = 16.sp,
                modifier = Modifier.fillMaxWidth(),
              )

              FintechProgressBar(
                progress = progress,
                color = heroBarColor,
                trackColor = Color.White.copy(alpha = 0.2f),
                height = 8.dp,
              )

              val remFormatted = FinanceCalculator.formatCurrencyOrMasked(
                amount = overallRemaining.abs(),
                currencyCode = currency,
                isHidden = isHidden,
                omitZeroDecimals = true,
              )
              Text(
                text = if (overallRemaining >= BigDecimal.ZERO) {
                  "$remFormatted remaining across ${uiState.budgetSummaries.size} categories"
                } else {
                  "Over total budget by $remFormatted"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (overallRemaining < BigDecimal.ZERO) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.78f),
              )
            }
          }
        }
      }

      if (uiState.budgetSummaries.isEmpty()) {
        item {
          EmptyStatePanel(
            icon = Icons.Rounded.AccountBalanceWallet,
            title = "No budgets for ${uiState.selectedMonth.monthNameOnly()}",
            message = "Set spending limits for your categories to keep your monthly expenses on track.",
            actionLabel = "Set Monthly Budget",
            onAction = {
              editingBudget = null
              showEditor = true
            },
          )
        }
      } else {
        item {
          SectionHeader(
            title = "Category limits (${uiState.budgetSummaries.size})",
            icon = Icons.Rounded.AccountBalanceWallet,
          )
        }
        items(uiState.budgetSummaries, key = { it.budgetId }) { summary ->
          SwipeToRevealDeleteRow(
            onDelete = { pendingDeleteBudget = summary },
            deleteTestTag = "swipe_delete_budget_${summary.budgetId}",
          ) { isRevealed, closeReveal ->
            BudgetProgressCard(
              summary = summary,
              currencyCode = currency,
              onClick = {
                if (isRevealed) {
                  closeReveal()
                } else {
                  editingBudget = summary
                  showEditor = true
                }
              },
              categories = uiState.categories,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun BudgetEditorDialog(
  existing: BudgetSummary?,
  categories: List<CategoryEntity>,
  selectedMonth: YearMonthKey,
  currencyCode: String,
  onSave: (existingId: Long, category: String, limitCentavos: Long) -> Unit,
  onDelete: (() -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val expenseCategories = remember(categories, existing) {
    val base = categories
      .filter { it.type == TransactionType.EXPENSE }
      .map { it.name }
      .ifEmpty { listOf("Food and Dining", "Transportation", "Education", "Bills and Utilities", "Shopping") }
    val withExisting = if (existing != null && existing.category !in base) listOf(existing.category) + base else base
    sortCategoriesOtherLast(withExisting)
  }
  var selectedCategory by remember(existing) {
    mutableStateOf(existing?.category ?: expenseCategories.first())
  }
  var showCategoryPicker by remember { mutableStateOf(false) }
  var limitText by remember(existing) {
    mutableStateOf(existing?.limitAmount?.toPlainString() ?: "")
  }
  var limitError by remember { mutableStateOf<String?>(null) }
  val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existing == null) "New Monthly Budget" else "Edit Budget",
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.weight(1f),
        )
        if (existing != null && onDelete != null) {
          TextButton(
            onClick = onDelete,
            colors = ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.testTag("dialog_delete_budget_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", maxLines = 1, softWrap = false)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val centavos = FinanceCalculator.parseAmountToCentavos(limitText)
          if (centavos == null) {
            limitError = "Enter a valid positive limit."
          } else {
            onSave(existing?.budgetId ?: 0L, selectedCategory, centavos)
          }
        },
        modifier = Modifier.testTag("save_budget_button"),
      ) {
        Text(if (existing == null) "Create" else "Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    Text(
      text = "Period: ${selectedMonth.displayLabel()}",
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.primary,
    )
    CategorySelectorField(
      selectedCategory = selectedCategory,
      allCategories = categories,
      onClick = { showCategoryPicker = true },
      label = "Budget Category",
      testTag = "budget_category_dropdown",
    )
    AmountInputWithCalculator(
      value = limitText,
      onValueChange = {
        limitText = it
        limitError = null
      },
      label = "Monthly Spending Limit",
      currencyCode = currencyCode,
      fieldTestTag = "budget_limit_input",
      errorText = limitError,
    )
  }

  if (showCategoryPicker) {
    CategoryPickerBottomSheet(
      activeType = TransactionType.EXPENSE,
      allCategories = categories,
      selectedCategory = selectedCategory,
      allowTypeSwitch = false,
      onSelect = { chosenCategory, _ ->
        selectedCategory = chosenCategory
        showCategoryPicker = false
      },
      onDismiss = { showCategoryPicker = false },
    )
  }
}
