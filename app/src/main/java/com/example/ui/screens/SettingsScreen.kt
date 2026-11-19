package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.CategoryEntity
import com.example.domain.CategoryVisualMatcher
import com.example.domain.ThemeMode
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CurrencyDropdown
import com.example.ui.components.FintechCard
import com.example.ui.components.PinDialogMode
import com.example.ui.components.PinSetupOrVerifyDialog
import com.example.ui.components.StableFormDialog
import com.example.ui.components.budgetWiseSwitchColors
import com.example.ui.components.categoryIcon
import com.example.ui.components.categoryTint
import com.example.ui.components.isBiometricAuthAvailable
import com.example.ui.components.pickableCategoryIcons
import com.example.ui.viewmodel.BudgetWiseUiState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
  uiState: BudgetWiseUiState,
  onBack: () -> Unit,
  onUpdateDisplayName: (String) -> Unit,
  onUpdateCurrency: (String) -> Unit,
  onUpdateThemeMode: (ThemeMode) -> Unit,
  onUpdateBalanceHidden: (Boolean) -> Unit,
  onVerifyCurrentPin: (String) -> Boolean,
  onEnableAppLockWithPin: (pin: String, enableBiometric: Boolean) -> Unit,
  onChangeAppPin: (newPin: String) -> Unit,
  onToggleBiometricUnlock: (Boolean) -> Unit,
  onDisableAppLock: () -> Unit,
  onLockAppNow: () -> Unit,
  onAddCategory: (String, TransactionType, String) -> Unit,
  onDeleteCategory: (CategoryEntity) -> Unit,
  onResetDemoData: () -> Unit,
  onClearAllData: () -> Unit,
) {
  val context = LocalContext.current
  val biometricAvailable = remember(context) { isBiometricAuthAvailable(context) }

  var nameInput by remember(uiState.preferences.displayName) {
    mutableStateOf(uiState.preferences.displayName)
  }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var pendingDeleteCategory by remember { mutableStateOf<CategoryEntity?>(null) }
  var showResetConfirm by remember { mutableStateOf(false) }
  var showClearConfirm by remember { mutableStateOf(false) }
  var activePinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }

  if (showAddCategoryDialog) {
    AddCategoryDialog(
      existingCategories = uiState.categories,
      onAdd = { name, type, iconKey ->
        onAddCategory(name, type, iconKey)
        showAddCategoryDialog = false
      },
      onDismiss = { showAddCategoryDialog = false },
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

  if (showResetConfirm) {
    ConfirmationDialog(
      title = "Reset Demonstration Data?",
      message = "This replaces your current transactions, budgets, and savings goals with the BudgetWise demonstration dataset.",
      confirmLabel = "Reset Data",
      isDestructive = false,
      onConfirm = {
        onResetDemoData()
        showResetConfirm = false
      },
      onDismiss = { showResetConfirm = false },
    )
  }

  if (showClearConfirm) {
    ConfirmationDialog(
      title = "Delete All Financial Records?",
      message = "All transactions, budgets, savings goals, and custom categories will be permanently removed from this device.",
      confirmLabel = "Delete All",
      isDestructive = true,
      onConfirm = {
        onClearAllData()
        showClearConfirm = false
      },
      onDismiss = { showClearConfirm = false },
    )
  }

  activePinDialogMode?.let { mode ->
    PinSetupOrVerifyDialog(
      mode = mode,
      biometricAvailable = biometricAvailable,
      onVerifyCurrentPin = onVerifyCurrentPin,
      onCreatePin = { pin, enableBiometric ->
        onEnableAppLockWithPin(pin, enableBiometric)
        activePinDialogMode = null
      },
      onChangePin = { newPin ->
        onChangeAppPin(newPin)
        activePinDialogMode = null
      },
      onDisableAppLock = {
        onDisableAppLock()
        activePinDialogMode = null
      },
      onDismiss = { activePinDialogMode = null },
    )
  }

  Column(
    modifier = Modifier.fillMaxSize(),
  ) {
    // Standard 56dp mobile app bar (outer BudgetWiseApp Scaffold already handles safeDrawing insets)
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp)
        .padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("settings_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = stringResource(R.string.nav_settings),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 760.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        // Profile
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Person, title = "Profile")
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                label = { Text("Display Name") },
                singleLine = true,
                modifier = Modifier
                  .weight(1f)
                  .testTag("settings_name_input"),
              )
              Button(
                onClick = { onUpdateDisplayName(nameInput) },
                enabled = nameInput.isNotBlank() && nameInput.trim() != uiState.preferences.displayName,
              ) {
                Text("Save")
              }
            }
          }
        }

        // Preferred Currency
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Payments, title = "Preferred Currency")
            Spacer(modifier = Modifier.height(10.dp))
            CurrencyDropdown(
              selectedCurrencyCode = uiState.preferences.preferredCurrency,
              onSelectCurrency = onUpdateCurrency,
            )
          }
        }

        // Theme & Privacy Display
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Palette, title = "Appearance & Privacy")
            Spacer(modifier = Modifier.height(10.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = uiState.preferences.themeMode == mode,
                  onClick = { onUpdateThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                  modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                ) {
                  Text(mode.label.replace(" default", ""))
                }
              }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Hide Balances", style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "Mask monetary amounts across cards and charts",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.preferences.isBalanceHidden,
                onCheckedChange = onUpdateBalanceHidden,
                colors = budgetWiseSwitchColors(),
                modifier = Modifier.testTag("settings_privacy_switch"),
              )
            }
          }
        }

        // Security & App Lock
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Shield, title = "Security & App Lock")
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                Text("4-Digit PIN Lock", style = MaterialTheme.typography.titleSmall)
                Text(
                  text = "Require your PIN after launch or leaving the app",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
              Switch(
                checked = uiState.preferences.appLockEnabled,
                onCheckedChange = { wantEnabled ->
                  activePinDialogMode = if (wantEnabled) {
                    PinDialogMode.CREATE
                  } else {
                    PinDialogMode.DISABLE
                  }
                },
                colors = budgetWiseSwitchColors(),
                modifier = Modifier.testTag("settings_app_lock_switch"),
              )
            }

            if (uiState.preferences.appLockEnabled) {
              Spacer(modifier = Modifier.height(14.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Row(
                  modifier = Modifier.weight(1f).padding(end = 12.dp),
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Fingerprint,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Column {
                    Text("Biometric Unlock", style = MaterialTheme.typography.titleSmall)
                    Text(
                      text = if (biometricAvailable) {
                        "Use fingerprint or face recognition to unlock faster"
                      } else {
                        "Enroll fingerprint or face unlock in device settings"
                      },
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                  }
                }
                Switch(
                  checked = uiState.preferences.biometricUnlockEnabled && biometricAvailable,
                  enabled = biometricAvailable,
                  onCheckedChange = onToggleBiometricUnlock,
                  colors = budgetWiseSwitchColors(),
                  modifier = Modifier.testTag("settings_biometric_switch"),
                )
              }

              Spacer(modifier = Modifier.height(12.dp))
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                OutlinedButton(
                  onClick = { activePinDialogMode = PinDialogMode.CHANGE },
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("change_pin_button"),
                ) {
                  Icon(Icons.Rounded.Pin, contentDescription = null)
                  Text(
                    text = "Change PIN",
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 6.dp),
                  )
                }
                OutlinedButton(
                  onClick = onLockAppNow,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                  modifier = Modifier
                    .weight(1f)
                    .testTag("lock_app_now_button"),
                ) {
                  Icon(Icons.Rounded.LockReset, contentDescription = null)
                  Text(
                    text = "Lock Now",
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(start = 6.dp),
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))
              Text(
                text = stringResource(R.string.pin_recovery_setup_tip),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("settings_pin_recovery_note"),
              )
            }
          }
        }

        // Financial Categories
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(
              icon = Icons.Rounded.Category,
              title = "Financial Categories",
              subtitle = "Tap × on custom categories to remove them",
              trailing = {
                TextButton(
                  onClick = { showAddCategoryDialog = true },
                  modifier = Modifier.testTag("add_category_button"),
                ) {
                  Icon(Icons.Rounded.Add, contentDescription = null)
                  Text("Add")
                }
              },
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text("Income", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              uiState.categories.filter { it.type == TransactionType.INCOME }.forEach { cat ->
                CategoryChipItem(
                  category = cat,
                  onRequestConfirmDelete = { pendingDeleteCategory = cat },
                  onDirectDelete = { onDeleteCategory(cat) },
                )
              }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text("Expenses", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              uiState.categories.filter { it.type == TransactionType.EXPENSE }.forEach { cat ->
                CategoryChipItem(
                  category = cat,
                  onRequestConfirmDelete = { pendingDeleteCategory = cat },
                  onDirectDelete = { onDeleteCategory(cat) },
                )
              }
            }
          }
        }

        // Data Management
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Storage, title = "Data & Demonstration Records")
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
              onClick = { showResetConfirm = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("reset_demo_button"),
            ) {
              Icon(Icons.Rounded.RestartAlt, contentDescription = null)
              Text("Reset to Demonstration Data", modifier = Modifier.padding(start = 8.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
              onClick = { showClearConfirm = true },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("clear_all_data_button"),
            ) {
              Icon(Icons.Rounded.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error)
              Text(
                text = "Clear All Financial Records",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 8.dp),
              )
            }
          }
        }

        // App & Privacy Info
        item {
          FintechCard(modifier = Modifier.fillMaxWidth()) {
            SettingsCardHeader(icon = Icons.Rounded.Lock, title = "Privacy & Application Info")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.privacy_notice),
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = stringResource(R.string.app_info_footer),
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsCardHeader(
  icon: ImageVector,
  title: String,
  subtitle: String? = null,
  trailing: (@Composable () -> Unit)? = null,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      if (subtitle != null) {
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    trailing?.invoke()
  }
}

@Composable
private fun CategoryChipItem(
  category: CategoryEntity,
  onRequestConfirmDelete: () -> Unit,
  onDirectDelete: () -> Unit,
) {
  val categoryList = remember(category) { listOf(category) }
  val icon = categoryIcon(category.name, categoryList)
  val tint = categoryTint(category.name, categoryList)

  InputChip(
    selected = false,
    onClick = {
      if (!category.isDefault) {
        onRequestConfirmDelete()
      }
    },
    leadingIcon = {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = Modifier.size(16.dp),
      )
    },
    label = {
      Text(
        text = compactCategoryLabel(category.name),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    },
    colors = if (!category.isDefault) {
      InputChipDefaults.inputChipColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
      )
    } else {
      InputChipDefaults.inputChipColors()
    },
    trailingIcon = if (!category.isDefault) {
      {
        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .clickable(onClick = onDirectDelete),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Delete ${category.name}",
            modifier = Modifier.size(15.dp),
          )
        }
      }
    } else {
      null
    },
  )
}

@Composable
private fun AddCategoryDialog(
  existingCategories: List<CategoryEntity>,
  onAdd: (name: String, type: TransactionType, iconKey: String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var type by remember { mutableStateOf(TransactionType.EXPENSE) }
  var manualIconKey by remember { mutableStateOf<String?>(null) }
  var error by remember { mutableStateOf<String?>(null) }

  val iconOptions = remember(type) { pickableCategoryIcons(type) }
  val autoIconKey = remember(name, type) { CategoryVisualMatcher.inferIconKey(name, type) }
  val selectedIconKey = manualIconKey ?: autoIconKey
  val isAutoMatched = manualIconKey == null
  val iconRowState = rememberLazyListState()

  LaunchedEffect(selectedIconKey, iconOptions) {
    val idx = iconOptions.indexOfFirst { it.iconKey == selectedIconKey }
    if (idx >= 0) {
      iconRowState.animateScrollToItem((idx - 1).coerceAtLeast(0))
    }
  }

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = { Text("Add Financial Category", style = MaterialTheme.typography.titleLarge) },
    confirmButton = {
      Button(
        onClick = {
          val clean = name.trim()
          val existingMatch = existingCategories.firstOrNull {
            it.name.trim().equals(clean, ignoreCase = true)
          }
          when {
            clean.isEmpty() -> error = "Enter a category name."
            existingMatch != null -> {
              error = "\"${existingMatch.name}\" already exists under ${existingMatch.type.label}."
            }
            else -> onAdd(clean, type, selectedIconKey)
          }
        },
        modifier = Modifier.testTag("confirm_add_category_button"),
      ) { Text("Add") }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      TransactionType.entries.forEachIndexed { idx, option ->
        SegmentedButton(
          selected = type == option,
          onClick = {
            type = option
            manualIconKey = null
          },
          shape = SegmentedButtonDefaults.itemShape(idx, TransactionType.entries.size),
        ) { Text(option.label) }
      }
    }
    OutlinedTextField(
      value = name,
      onValueChange = {
        name = it
        error = null
      },
      label = { Text("Category Name") },
      placeholder = { Text(if (type == TransactionType.INCOME) "e.g., Bonus, Dividends" else "e.g., Coffee, Pets, Gym") },
      isError = error != null,
      supportingText = error?.let { msg -> { Text(msg) } },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("category_name_input"),
    )

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Category Icon",
          style = MaterialTheme.typography.labelLarge,
        )
        Text(
          text = if (isAutoMatched) "Auto-matched" else "Custom",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
        )
      }
      LazyRow(
        state = iconRowState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        items(iconOptions, key = { it.iconKey }) { option ->
          val isSelected = option.iconKey == selectedIconKey
          val accent = com.example.ui.components.accessibleCategoryColor(Color(option.colorHex))
          Surface(
            onClick = { manualIconKey = option.iconKey },
            shape = RoundedCornerShape(12.dp),
            color = if (isSelected) accent.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
              width = if (isSelected) 1.5.dp else 1.dp,
              color = if (isSelected) accent else MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
              .size(44.dp)
              .testTag("category_icon_option_${option.iconKey}"),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                tint = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }
      }
    }
  }
}
