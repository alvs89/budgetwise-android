package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.TransactionEntity
import com.example.domain.TransactionSortOrder
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.MonthSelectorBar
import com.example.ui.components.SwipeToRevealDeleteRow
import com.example.ui.components.TransactionRowItem
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryTint
import com.example.ui.viewmodel.BudgetWiseUiState

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
  modifier: Modifier = Modifier,
) {
  var showSortMenu by remember { mutableStateOf(false) }
  var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }

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
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
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
            AutoResizeSingleLineText(
              text = "Activity",
              style = MaterialTheme.typography.displaySmall,
              color = MaterialTheme.colorScheme.onBackground,
              minFontSize = 20.sp,
            )
            Text(
              text = "${uiState.filteredActivityTransactions.size} transactions",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Box {
            OutlinedButton(
              onClick = { showSortMenu = true },
              modifier = Modifier.testTag("sort_button"),
            ) {
              Icon(Icons.Rounded.SwapVert, contentDescription = null)
              Text(
                text = filter.sortOrder.label,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 6.dp),
              )
            }
            DropdownMenu(
              expanded = showSortMenu,
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

      if (filter.selectedMonthOnly) {
        item {
          MonthSelectorBar(
            selectedMonth = uiState.selectedMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier.fillMaxWidth(),
          )
        }
      }

      item {
        OutlinedTextField(
          value = filter.searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("Search title, category, or notes...") },
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
      }

      // Scope & Type filter chips
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilterChip(
            selected = filter.selectedMonthOnly,
            onClick = { onToggleMonthOnly(!filter.selectedMonthOnly) },
            label = { Text(if (filter.selectedMonthOnly) uiState.selectedMonth.shortLabel() else "All dates") },
          )
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
