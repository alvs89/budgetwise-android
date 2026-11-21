package com.example.ui.components

import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.example.data.local.entity.CategoryEntity

/**
 * A centered Material 3 form dialog with dynamic soft-keyboard avoidance.
 *
 * In its resting state, the dialog is centered both horizontally and vertically in the viewport.
 * When the soft keyboard appears, [WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE] paired
 * with [imePadding] smoothly centers the dialog within the visible area above the keyboard, while
 * inner vertical scrolling ensures input fields and Cancel/Save actions remain accessible.
 */
@Composable
fun StableFormDialog(
  onDismissRequest: () -> Unit,
  title: @Composable () -> Unit,
  confirmButton: @Composable () -> Unit,
  dismissButton: (@Composable () -> Unit)? = null,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  if (dismissIfAppLocked(onDismissRequest)) return

  Dialog(
    onDismissRequest = onDismissRequest,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      decorFitsSystemWindows = false,
    ),
  ) {
    val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
    SideEffect {
      @Suppress("DEPRECATION")
      dialogWindow?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = onDismissRequest,
        )
        .padding(horizontal = 20.dp, vertical = 24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Surface(
        shape = AlertDialogDefaults.shape,
        color = AlertDialogDefaults.containerColor,
        tonalElevation = AlertDialogDefaults.TonalElevation,
        shadowElevation = 8.dp,
        modifier = modifier
          .widthIn(min = 280.dp, max = 520.dp)
          .fillMaxWidth()
          .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = {},
          ),
      ) {
        Column(
          modifier = Modifier.padding(24.dp),
        ) {
          title()
          Spacer(modifier = Modifier.height(16.dp))
          Column(
            modifier = Modifier
              .weight(1f, fill = false)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
          )
          Spacer(modifier = Modifier.height(20.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            dismissButton?.invoke()
            confirmButton()
          }
        }
      }
    }
  }
}

/**
 * Compact Material 3 exposed dropdown selector for budget/transaction categories with
 * leading category icons and tinted badges, matching [CurrencyDropdown].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdown(
  selectedCategory: String,
  categoryNames: List<String>,
  allCategories: List<CategoryEntity>,
  onSelectCategory: (String) -> Unit,
  label: String = "Category",
  modifier: Modifier = Modifier,
  fieldTestTag: String = "budget_category_dropdown",
) {
  var expanded by remember { mutableStateOf(false) }
  val isAppLocked = LocalIsAppLocked.current
  LaunchedEffect(isAppLocked) {
    if (isAppLocked && expanded) {
      expanded = false
    }
  }
  val effectiveExpanded = expanded && !isAppLocked
  val focusManager = LocalFocusManager.current
  val selectedTint = categoryTint(selectedCategory, allCategories)

  ExposedDropdownMenuBox(
    expanded = effectiveExpanded,
    onExpandedChange = { next ->
      if (!isAppLocked) {
        if (next) {
          focusManager.clearFocus()
        }
        expanded = next
      }
    },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = selectedCategory,
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = { Text(label) },
      leadingIcon = {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = selectedTint.copy(alpha = 0.16f),
          modifier = Modifier.padding(start = 6.dp),
        ) {
          Box(
            modifier = Modifier.padding(6.dp),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = categoryIcon(selectedCategory, allCategories),
              contentDescription = null,
              tint = selectedTint,
              modifier = Modifier.size(18.dp),
            )
          }
        }
      },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = effectiveExpanded)
      },
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        .testTag(fieldTestTag),
    )

    ExposedDropdownMenu(
      expanded = effectiveExpanded,
      onDismissRequest = { expanded = false },
    ) {
      categoryNames.forEach { catName ->
        val isSelected = catName == selectedCategory
        val tint = categoryTint(catName, allCategories)
        DropdownMenuItem(
          text = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = tint.copy(alpha = if (isSelected) 0.22f else 0.12f),
              ) {
                Box(
                  modifier = Modifier.padding(6.dp),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = categoryIcon(catName, allCategories),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
              Text(
                text = catName,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
              )
            }
          },
          trailingIcon = if (isSelected) {
            {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
          } else {
            null
          },
          onClick = {
            onSelectCategory(catName)
            expanded = false
          },
          modifier = Modifier.testTag("category_dropdown_option_$catName"),
        )
      }
    }
  }
}
