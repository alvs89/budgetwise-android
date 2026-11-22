package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.domain.FinanceCalculator
import com.example.domain.TransactionSortOrder
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.LocalIsAppLocked
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SpotlightTargetId
import com.example.ui.components.SwipeToRevealDeleteRow
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryTint
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
  uiState: BudgetWiseUiState,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  onSearchQueryChange: (String) -> Unit,
  onTypeFilterChange: (TransactionType?) -> Unit,
  onCategoryFilterChange: (String?) -> Unit,
  onToggleMonthOnly: (Boolean) -> Unit,
  onSortOrderChange: (TransactionSortOrder) -> Unit,
  onClearFilters: () -> Unit,
  onEditTransaction: (TransactionEntity) -> Unit,
  onDeleteTransaction: (TransactionEntity) -> Unit,
  onAddTransaction: () -> Unit,
  onExportData: () -> Unit = {},
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
  var showSortMenu by remember { mutableStateOf(false) }
  var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
  val isAppLocked = LocalIsAppLocked.current
  val isBalanceHidden = LocalIsBalanceHidden.current
  val currency = uiState.preferences.preferredCurrency

  LaunchedEffect(isAppLocked) {
    if (isAppLocked && showSortMenu) {
      showSortMenu = false
    }
  }

  pendingDeleteTx?.let { tx ->
    ConfirmationDialog(
      title = "Delete Transaction?",
      message = "Remove \"${tx.title}\" from your records? This will also update related budget totals.",
      confirmLabel = "Delete",
      onConfirm = {
        onDeleteTransaction(tx)
        pendingDeleteTx = null
      },
      onDismiss = { pendingDeleteTx = null },
    )
  }

  val filter = uiState.activityFilter
  val filteredList = uiState.filteredActivityTransactions
  val filteredNetCentavos = remember(filteredList) {
    filteredList.fold(0L) { acc, tx ->
      if (tx.type == TransactionType.INCOME) acc + tx.amountCentavos else acc - tx.amountCentavos
    }
  }
  val filteredSubtitle = remember(filteredList.size, filteredNetCentavos, currency, isBalanceHidden) {
    val countText = if (filteredList.size == 1) "1 transaction" else "${filteredList.size} transactions"
    if (filteredList.isEmpty()) {
      countText
    } else {
      val netAmount = FinanceCalculator.centavosToBigDecimal(kotlin.math.abs(filteredNetCentavos))
      val formatted = FinanceCalculator.formatCurrencyOrMasked(netAmount, currency, isBalanceHidden)
      val prefix = when {
        isBalanceHidden -> ""
        filteredNetCentavos > 0L -> "+"
        filteredNetCentavos < 0L -> "-"
        else -> ""
      }
      "$countText · Net $prefix$formatted"
    }
  }
  val availableCategories = remember(uiState.allTransactions, filter.typeFilter) {
    uiState.allTransactions
      .filter { filter.typeFilter == null || it.type == filter.typeFilter }
      .map { it.category }
      .distinct()
      .sorted()
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
      verticalArrangement = Arrangement.spacedBy(12.dp),
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
              text = "Activity",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
              minFontSize = 20.sp,
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            )
            Row(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                border = BorderStroke(0.75.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
              ) {
                IconButton(
                  onClick = onExportData,
                  modifier = Modifier
                    .size(40.dp)
                    .testTag("activity_export_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.FileDownload,
                    contentDescription = "Export financial report",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(19.dp),
                  )
                }
              }
              Box {
                OutlinedButton(
                  onClick = { showSortMenu = true },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                  modifier = Modifier.testTag("sort_button"),
                ) {
                  Icon(Icons.Rounded.SwapVert, contentDescription = null, modifier = Modifier.size(18.dp))
                  Text(
                    text = filter.sortOrder.label,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 6.dp),
                  )
                }
                DropdownMenu(
                  expanded = showSortMenu && !isAppLocked,
                  onDismissRequest = { showSortMenu = false },
                ) {
                  TransactionSortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                      text = { Text(order.label) },
                      onClick = {
                        onSortOrderChange(order)
                        showSortMenu = false
                      },
                    )
                  }
                }
              }
            }
          }
          AutoResizeSingleLineText(
            text = filteredSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minFontSize = 11.sp,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("activity_filtered_summary"),
          )
        }
      }

      // Clear Date Scope Toggle (This Month vs All Dates)
      item {
        val dateScopeOptions = listOf(true to "This Month", false to "All Dates")
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        ) {
          dateScopeOptions.forEachIndexed { index, (monthOnlyValue, label) ->
            SegmentedButton(
              selected = filter.selectedMonthOnly == monthOnlyValue,
              onClick = { onToggleMonthOnly(monthOnlyValue) },
              shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = dateScopeOptions.size,
              ),
              icon = {},
              modifier = Modifier.testTag(
                if (monthOnlyValue) "activity_scope_this_month" else "activity_scope_all_dates"
              ),
            ) {
              AutoResizeSingleLineText(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = LocalContentColor.current,
                minFontSize = 11.sp,
              )
            }
          }
        }
      }

      item {
        AnimatedVisibility(
          visible = filter.selectedMonthOnly,
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

      // Search bar + Type filter controls (spotlighted together for Tour Step 3)
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
              if (coords.isAttached) {
                runCatching { coords.boundsInRoot() }.getOrNull()?.let { rect ->
                  onRegisterSpotlightBounds(SpotlightTargetId.ACTIVITY_CONTROLS, rect)
                }
              }
            },
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedTextField(
            value = filter.searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = {
              Text(
                text = "Search transactions...",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
              if (filter.searchQuery.isNotEmpty()) {
                IconButton(onClick = { onSearchQueryChange("") }) {
                  Icon(Icons.Rounded.Clear, contentDescription = "Clear search")
                }
              }
            },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("activity_search_input"),
          )

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = filter.typeFilter == null,
              onClick = { onTypeFilterChange(null) },
              label = { Text("All types") },
            )
            FilterChip(
              selected = filter.typeFilter == TransactionType.INCOME,
              onClick = {
                onTypeFilterChange(
                  if (filter.typeFilter == TransactionType.INCOME) null else TransactionType.INCOME
                )
              },
              label = { Text("Income") },
            )
            FilterChip(
              selected = filter.typeFilter == TransactionType.EXPENSE,
              onClick = {
                onTypeFilterChange(
                  if (filter.typeFilter == TransactionType.EXPENSE) null else TransactionType.EXPENSE
                )
              },
              label = { Text("Expenses") },
            )
          }
        }
      }

      // Category filter chips
      if (availableCategories.isNotEmpty()) {
        item {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            FilterChip(
              selected = filter.categoryFilter == null,
              onClick = { onCategoryFilterChange(null) },
              label = { Text("All categories") },
            )
            availableCategories.forEach { category ->
              FilterChip(
                selected = filter.categoryFilter == category,
                onClick = {
                  onCategoryFilterChange(
                    if (filter.categoryFilter == category) null else category
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = categoryIcon(category, uiState.categories),
                    contentDescription = null,
                    tint = categoryTint(category, uiState.categories),
                    modifier = Modifier.size(16.dp),
                  )
                },
                label = { Text(compactCategoryLabel(category), maxLines = 1, softWrap = false) },
              )
            }
          }
        }
      }

      if (uiState.filteredActivityTransactions.isEmpty()) {
        item {
          val hasActiveFilters = filter.searchQuery.isNotBlank() ||
            filter.typeFilter != null ||
            filter.categoryFilter != null
          EmptyStatePanel(
            icon = Icons.Rounded.ReceiptLong,
            title = if (hasActiveFilters) "No matching transactions" else "No transactions yet",
            message = if (hasActiveFilters) {
              "Try clearing or adjusting your search query and filters."
            } else {
              "Log your first income or expense to start tracking your monthly cash flow."
            },
            actionLabel = if (hasActiveFilters) "Reset Filters" else "Add Transaction",
            actionIcon = if (hasActiveFilters) null else Icons.Rounded.Add,
            onAction = if (hasActiveFilters) onClearFilters else onAddTransaction,
          )
        }
      } else {
        items(uiState.filteredActivityTransactions, key = { it.id }) { tx ->
          SwipeToRevealDeleteRow(
            onDelete = { pendingDeleteTx = tx },
            deleteTestTag = "swipe_delete_transaction_${tx.id}",
          ) { isRevealed, closeReveal ->
            TransactionRowItem(
              transaction = tx,
              currencyCode = uiState.preferences.preferredCurrency,
              onClick = {
                if (isRevealed) closeReveal() else onEditTransaction(tx)
              },
              categories = uiState.categories,
            )
          }
        }
      }
    }
  }
}
