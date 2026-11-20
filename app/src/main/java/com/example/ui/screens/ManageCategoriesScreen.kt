package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.ui.components.CategoryCustomizeDialog
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryTint
import com.example.ui.components.isOtherCategoryName
import com.example.ui.viewmodel.BudgetWiseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
  uiState: BudgetWiseUiState,
  onBack: () -> Unit,
  onAddCategory: (String, TransactionType, String, Long) -> Unit,
  onUpdateCategory: (CategoryEntity, String, String, Long) -> Unit,
  onDeleteCategory: (CategoryEntity) -> Unit,
) {
  BackHandler(onBack = onBack)

  var selectedTab by rememberSaveable { mutableStateOf(TransactionType.EXPENSE) }
  var showAddDialog by remember { mutableStateOf(false) }
  var editingCategory by remember { mutableStateOf<CategoryEntity?>(null) }
  var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }

  val expenseCategories = remember(uiState.categories) {
    uiState.categories
      .filter { it.type == TransactionType.EXPENSE }
      .sortedWith(
        compareBy<CategoryEntity> { isOtherCategoryName(it.name) }
          .thenBy { it.name.lowercase() },
      )
  }
  val incomeCategories = remember(uiState.categories) {
    uiState.categories
      .filter { it.type == TransactionType.INCOME }
      .sortedWith(
        compareBy<CategoryEntity> { isOtherCategoryName(it.name) }
          .thenBy { it.name.lowercase() },
      )
  }
  val displayedCategories = if (selectedTab == TransactionType.EXPENSE) {
    expenseCategories
  } else {
    incomeCategories
  }

  if (showAddDialog) {
    CategoryCustomizeDialog(
      editingCategory = null,
      existingCategories = uiState.categories,
      initialType = selectedTab,
      onSaveNew = { name, type, iconKey, colorHex ->
        onAddCategory(name, type, iconKey, colorHex)
        selectedTab = type
        showAddDialog = false
      },
      onSaveEdit = { _, _, _, _ -> },
      onDismiss = { showAddDialog = false },
    )
  }

  editingCategory?.let { target ->
    CategoryCustomizeDialog(
      editingCategory = target,
      existingCategories = uiState.categories,
      onSaveNew = { _, _, _, _ -> },
      onSaveEdit = { existing, newName, iconKey, colorHex ->
        onUpdateCategory(existing, newName, iconKey, colorHex)
        editingCategory = null
      },
      onRequestDelete = { cat ->
        editingCategory = null
        pendingDeleteCategory = cat
      },
      onDismiss = { editingCategory = null },
    )
  }

  pendingDeleteCategory?.let { cat ->
    ConfirmationDialog(
      title = "Remove Category?",
      message = "Remove custom category \"${cat.name}\"? Existing transactions using it will remain in your history.",
      confirmLabel = "Remove",
      onConfirm = {
        onDeleteCategory(cat)
        pendingDeleteCategory = null
      },
      onDismiss = { pendingDeleteCategory = null },
    )
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .navigationBarsPadding(),
  ) {
    // Top Header matching SettingsScreen visual rhythm (zero double status-bar insets)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("manage_categories_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back to Settings",
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = "Financial Categories",
        style = MaterialTheme.typography.titleLarge.copy(
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
      TextButton(
        onClick = { showAddDialog = true },
        modifier = Modifier.testTag("add_category_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "Add",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
          maxLines = 1,
          softWrap = false,
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .widthIn(max = 760.dp),
      ) {
        // Segmented Expenses | Income Tab Switcher
        val emeraldSegmentedColors = SegmentedButtonDefaults.colors(
          activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
          activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
          inactiveContainerColor = Color.Transparent,
          inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
          inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
        )
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp),
        ) {
          SegmentedButton(
            selected = selectedTab == TransactionType.EXPENSE,
            onClick = { selectedTab = TransactionType.EXPENSE },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            colors = emeraldSegmentedColors,
            modifier = Modifier.testTag("manage_categories_tab_expense"),
          ) {
            Text(
              text = "Expenses (${expenseCategories.size})",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
          SegmentedButton(
            selected = selectedTab == TransactionType.INCOME,
            onClick = { selectedTab = TransactionType.INCOME },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
            colors = emeraldSegmentedColors,
            modifier = Modifier.testTag("manage_categories_tab_income"),
          ) {
            Text(
              text = "Income (${incomeCategories.size})",
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Text(
          text = "Tap any category to customize its icon, color, or name",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )

        Spacer(modifier = Modifier.height(6.dp))

        LazyColumn(
          modifier = Modifier
            .fillMaxSize()
            .testTag("manage_categories_list"),
          contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 32.dp),
        ) {
          itemsIndexed(
            items = displayedCategories,
            key = { _, item -> item.id },
          ) { index, category ->
            val isFirst = index == 0
            val isLast = index == displayedCategories.lastIndex
            val rowShape = when {
              isFirst && isLast -> RoundedCornerShape(18.dp)
              isFirst -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
              isLast -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
              else -> RoundedCornerShape(0.dp)
            }

            Surface(
              shape = rowShape,
              color = MaterialTheme.colorScheme.surface,
              border = if (isFirst && isLast) {
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
              } else {
                null
              },
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(modifier = Modifier.fillMaxWidth()) {
                CategoryListRow(
                  category = category,
                  onClick = { editingCategory = category },
                )
                if (!isLast) {
                  HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                    modifier = Modifier.padding(start = 68.dp, end = 16.dp),
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
private fun CategoryListRow(
  category: CategoryEntity,
  onClick: () -> Unit,
) {
  val singleList = remember(category) { listOf(category) }
  val icon = categoryIcon(category.name, singleList)
  val tint = categoryTint(category.name, singleList)

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 56.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 10.dp)
      .testTag(
        "manage_category_row_${category.name.lowercase().replace(Regex("[^a-z0-9]+"), "_")}",
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(tint.copy(alpha = 0.16f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(20.dp),
      )
    }

    Text(
      text = compactCategoryLabel(category.name),
      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )

    if (!category.isDefault) {
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
      ) {
        Text(
          text = "Custom",
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
      }
    }

    Icon(
      imageVector = Icons.Rounded.Edit,
      contentDescription = "Customize ${compactCategoryLabel(category.name)}",
      tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
      modifier = Modifier.size(18.dp),
    )
  }
}
