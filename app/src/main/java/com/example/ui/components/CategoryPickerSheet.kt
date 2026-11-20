package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.CategoryEntity
import com.example.domain.CategoryVisualMatcher
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel

/**
 * Compact single-row Category selector shared across Add/Edit Transaction and New/Edit Budget.
 */
@Composable
fun CategorySelectorField(
  selectedCategory: String,
  allCategories: List<CategoryEntity>,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
  testTag: String = "tx_category_selector",
) {
  val tint = categoryTint(selectedCategory, allCategories)
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp),
  ) {
    if (label != null) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 52.dp)
        .clip(RoundedCornerShape(12.dp))
        .clickable(onClick = onClick)
        .testTag(testTag),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = categoryIcon(selectedCategory, allCategories),
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = compactCategoryLabel(selectedCategory),
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = "Choose category",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(22.dp),
        )
      }
    }
  }
}

/**
 * Clean Material 3 ModalBottomSheet displaying categories in a balanced 4-column circular
 * icon grid for fast visual scanning and 1-tap selection.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
  activeType: TransactionType,
  allCategories: List<CategoryEntity>,
  selectedCategory: String,
  allowTypeSwitch: Boolean,
  onSelect: (categoryName: String, type: TransactionType) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var currentType by remember(activeType) { mutableStateOf(activeType) }

  val filteredCategories = remember(allCategories, currentType) {
    val names = allCategories
      .filter { it.type == currentType }
      .map { it.name }
      .ifEmpty {
        if (currentType == TransactionType.INCOME) listOf("Salary", "Allowance", "Other Income")
        else listOf("Food and Dining", "Transportation", "Other Expenses")
      }
    sortCategoriesOtherLast(names)
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = Modifier
      .statusBarsPadding()
      .testTag("category_grid_picker_sheet"),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxGridHeight = if (maxHeight != Dp.Infinity) {
        (maxHeight * 0.56f).coerceIn(240.dp, 460.dp)
      } else {
        400.dp
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .navigationBarsPadding()
          .padding(horizontal = 20.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (currentType == TransactionType.INCOME) "Select Income Category" else "Select Expense Category",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("category_picker_close"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Close category picker",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        if (allowTypeSwitch) {
          val tabOrder = listOf(TransactionType.EXPENSE, TransactionType.INCOME)
          SingleChoiceSegmentedButtonRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 14.dp),
          ) {
            tabOrder.forEachIndexed { idx, option ->
              SegmentedButton(
                selected = currentType == option,
                onClick = { currentType = option },
                shape = SegmentedButtonDefaults.itemShape(idx, tabOrder.size),
                modifier = Modifier.testTag("category_picker_tab_${option.name.lowercase()}"),
              ) {
                Text(
                  text = if (option == TransactionType.EXPENSE) "Expenses" else "Income",
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (currentType == option) FontWeight.SemiBold else FontWeight.Medium,
                  ),
                )
              }
            }
          }
        }

        LazyVerticalGrid(
          columns = GridCells.Fixed(4),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
          contentPadding = PaddingValues(top = 4.dp, bottom = 28.dp),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp, max = maxGridHeight)
            .testTag("category_picker_grid"),
        ) {
          items(filteredCategories, key = { "${currentType.name}_$it" }) { cat ->
            val isSelected = cat == selectedCategory && currentType == activeType
            val tint = categoryTint(cat, allCategories)
            val optionTag = "tx_category_option_${cat.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"

            Column(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onSelect(cat, currentType) }
                .padding(vertical = 4.dp, horizontal = 2.dp)
                .testTag(optionTag),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Box(contentAlignment = Alignment.Center) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = if (isSelected) 0.24f else 0.14f))
                    .then(
                      if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                      } else {
                        Modifier.border(
                          1.dp,
                          MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                          CircleShape,
                        )
                      }
                    ),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = categoryIcon(cat, allCategories),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(24.dp),
                  )
                }
                if (isSelected) {
                  Box(
                    modifier = Modifier
                      .align(Alignment.TopEnd)
                      .size(16.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                  ) {
                    Icon(
                      imageVector = Icons.Rounded.Check,
                      contentDescription = "Selected",
                      tint = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.size(11.dp),
                    )
                  }
                }
              }

              Text(
                text = compactCategoryLabel(cat),
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                  letterSpacing = 0.sp,
                ),
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                },
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Curated Vector Icon & Accent Color Customizer Dialog for adding a custom category or
 * personalizing any existing (default or custom) category in Settings.
 */
@Composable
fun CategoryCustomizeDialog(
  editingCategory: CategoryEntity?,
  existingCategories: List<CategoryEntity>,
  initialType: TransactionType = TransactionType.EXPENSE,
  onSaveNew: (name: String, type: TransactionType, iconKey: String, colorHex: Long) -> Unit,
  onSaveEdit: (existing: CategoryEntity, newName: String, iconKey: String, colorHex: Long) -> Unit,
  onRequestDelete: ((CategoryEntity) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val isEditing = editingCategory != null
  val isDefaultCategory = editingCategory?.isDefault == true

  var name by remember(editingCategory) { mutableStateOf(editingCategory?.name ?: "") }
  var type by remember(editingCategory, initialType) {
    mutableStateOf(editingCategory?.type ?: initialType)
  }
  var manualIconKey by remember(editingCategory) { mutableStateOf(editingCategory?.iconKey) }
  var manualColorHex by remember(editingCategory) { mutableStateOf(editingCategory?.colorHex) }
  var error by remember { mutableStateOf<String?>(null) }

  val iconOptions = remember(type) { pickableCategoryIcons(type) }
  val autoIconKey = remember(name, type) { CategoryVisualMatcher.inferIconKey(name, type) }
  val autoColorHex = remember(name, type, manualIconKey) {
    val key = manualIconKey ?: autoIconKey
    CategoryVisualMatcher.colorForIconOrName(key, name, type)
  }

  val selectedIconKey = manualIconKey ?: autoIconKey
  val selectedColorHex = manualColorHex ?: autoColorHex
  val previewColor = accessibleCategoryColor(Color(selectedColorHex))

  val defaultTemplate = remember(editingCategory) {
    if (editingCategory != null && editingCategory.isDefault) {
      SampleDataSeeder.defaultCategories().firstOrNull {
        it.name.equals(editingCategory.name, ignoreCase = true) && it.type == editingCategory.type
      }
    } else null
  }

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = when {
            !isEditing -> "Add Financial Category"
            isDefaultCategory -> "Customize Category"
            else -> "Edit Category"
          },
          style = MaterialTheme.typography.titleLarge,
          modifier = Modifier.weight(1f),
        )
        if (editingCategory != null && !isDefaultCategory && onRequestDelete != null) {
          TextButton(
            onClick = { onRequestDelete(editingCategory) },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.testTag("dialog_delete_category_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", maxLines = 1, softWrap = false)
          }
        } else if (defaultTemplate != null &&
          (selectedIconKey != defaultTemplate.iconKey || selectedColorHex != defaultTemplate.colorHex)
        ) {
          TextButton(
            onClick = {
              manualIconKey = defaultTemplate.iconKey
              manualColorHex = defaultTemplate.colorHex
            },
            modifier = Modifier.testTag("reset_default_category_style"),
          ) {
            Icon(
              imageVector = Icons.Rounded.RestartAlt,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Reset", maxLines = 1, softWrap = false)
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (editingCategory != null) {
            val clean = if (isDefaultCategory) editingCategory.name else name.trim()
            val duplicate = if (!isDefaultCategory) {
              existingCategories.firstOrNull {
                it.id != editingCategory.id && it.name.trim().equals(clean, ignoreCase = true)
              }
            } else null
            when {
              clean.isEmpty() -> error = "Enter a category name."
              duplicate != null -> error = "\"${duplicate.name}\" already exists under ${duplicate.type.label}."
              else -> onSaveEdit(editingCategory, clean, selectedIconKey, selectedColorHex)
            }
          } else {
            val clean = name.trim()
            val duplicate = existingCategories.firstOrNull {
              it.name.trim().equals(clean, ignoreCase = true)
            }
            when {
              clean.isEmpty() -> error = "Enter a category name."
              duplicate != null -> error = "\"${duplicate.name}\" already exists under ${duplicate.type.label}."
              else -> onSaveNew(clean, type, selectedIconKey, selectedColorHex)
            }
          }
        },
        modifier = Modifier.testTag("confirm_add_category_button"),
      ) {
        Text(if (isEditing) "Save" else "Add")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    // Live Category Preview Pill
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(previewColor.copy(alpha = 0.18f))
            .border(1.5.dp, previewColor, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = iconForCategoryKey(selectedIconKey),
            contentDescription = null,
            tint = previewColor,
            modifier = Modifier.size(22.dp),
          )
        }
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = compactCategoryLabel(name.trim().ifEmpty { if (type == TransactionType.INCOME) "New Income" else "New Expense" }),
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = if (isDefaultCategory) {
              "Default ${type.label} · Icon & color customizable"
            } else {
              "${type.label} Category"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }

    if (!isEditing) {
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { idx, option ->
          SegmentedButton(
            selected = type == option,
            onClick = {
              type = option
              manualIconKey = null
              manualColorHex = null
            },
            shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size),
          ) { Text(option.label) }
        }
      }
    }

    if (!isDefaultCategory) {
      OutlinedTextField(
        value = name,
        onValueChange = {
          name = it
          error = null
        },
        label = { Text("Category Name") },
        placeholder = { Text(if (type == TransactionType.INCOME) "e.g., Dividends, Side Hustle" else "e.g., Milk Tea, Parking") },
        isError = error != null,
        supportingText = error?.let { msg -> { Text(msg) } },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("category_name_input"),
      )
    }

    // Accent Color Swatches (10 curated dark-mode-friendly colors)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Accent Color",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("category_color_swatch_row"),
      ) {
        items(CategoryVisualMatcher.accentSwatches, key = { it }) { swatchHex ->
          val isSelected = selectedColorHex == swatchHex
          val swatchColor = accessibleCategoryColor(Color(swatchHex))
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
              .background(swatchColor)
              .then(
                if (isSelected) {
                  Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                } else {
                  Modifier
                }
              )
              .clickable { manualColorHex = swatchHex }
              .testTag("category_color_swatch_${swatchHex.toString(16)}"),
            contentAlignment = Alignment.Center,
          ) {
            if (isSelected) {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected color",
                tint = Color.White,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }
      }
    }

    // Curated 6-Column Vector Icon Picker (~36 matching Material icons)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Icon Library",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = if (manualIconKey == null && !isEditing) "Auto-matched" else "${iconOptions.size} icons",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }

      val iconRows = remember(iconOptions) { iconOptions.chunked(6) }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 196.dp)
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        iconRows.forEach { rowIcons ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            rowIcons.forEach { option ->
              val isSelected = option.iconKey == selectedIconKey
              Surface(
                onClick = {
                  manualIconKey = option.iconKey
                  if (manualColorHex == null && !isEditing) {
                    manualColorHex = option.colorHex
                  }
                },
                shape = CircleShape,
                color = if (isSelected) {
                  previewColor.copy(alpha = 0.22f)
                } else {
                  MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
                border = BorderStroke(
                  width = if (isSelected) 1.5.dp else 1.dp,
                  color = if (isSelected) previewColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                ),
                modifier = Modifier
                  .weight(1f)
                  .height(42.dp)
                  .testTag("category_icon_option_${option.iconKey}"),
              ) {
                Box(contentAlignment = Alignment.Center) {
                  Icon(
                    imageVector = option.icon,
                    contentDescription = option.label,
                    tint = if (isSelected) previewColor else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                  )
                }
              }
            }
            repeat(6 - rowIcons.size) {
              Spacer(modifier = Modifier.weight(1f))
            }
          }
        }
      }
    }
  }
}
