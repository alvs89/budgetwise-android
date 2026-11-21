package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.ThemeMode
import com.example.domain.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CurrencyDropdown
import com.example.ui.components.PinDialogMode
import com.example.ui.components.PinSetupOrVerifyDialog
import com.example.ui.components.budgetWiseSwitchColors
import com.example.ui.components.isBiometricAuthAvailable
import com.example.ui.viewmodel.BudgetWiseUiState

@OptIn(ExperimentalMaterial3Api::class)
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
  onOpenManageCategories: () -> Unit,
  onOpenNotificationsSettings: () -> Unit,
  onOpenHelpCenter: () -> Unit,
  onExportData: () -> Unit,
  onResetDemoData: () -> Unit,
  onClearAllData: () -> Unit,
) {
  val context = LocalContext.current
  val biometricAvailable = remember(context) { isBiometricAuthAvailable(context) }

  var nameInput by remember(uiState.preferences.displayName) {
    mutableStateOf(uiState.preferences.displayName)
  }
  var showResetConfirm by remember { mutableStateOf(false) }
  var showClearConfirm by remember { mutableStateOf(false) }
  var activePinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }

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
      title = "Clear All Financial Records?",
      message = "This permanently deletes all transactions, budgets, savings goals, and custom categories on this device. This cannot be undone.",
      confirmLabel = "Clear All",
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
      onCreatePin = { pin, enableBio ->
        onEnableAppLockWithPin(pin, enableBio)
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

  val subtleDividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
  val emeraldSegmentedColors = SegmentedButtonDefaults.colors(
    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
    activeBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
    inactiveContainerColor = Color.Transparent,
    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    inactiveBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f),
  )

  val incomeCount = remember(uiState.categories) {
    uiState.categories.count { it.type == TransactionType.INCOME }
  }
  val expenseCount = remember(uiState.categories) {
    uiState.categories.count { it.type == TransactionType.EXPENSE }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .navigationBarsPadding(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("settings_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back",
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = stringResource(R.string.nav_settings),
        style = MaterialTheme.typography.titleLarge.copy(
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
        ),
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        // Card 1: Account & Preferences (Display Name + Preferred Currency)
        item {
          SettingsGroupCard {
            SettingsCardHeader(
              icon = Icons.Rounded.Person,
              title = "Account & Preferences",
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Display Name with label above so the Save CTA aligns flush with the text box body
            Text(
              text = "Display Name",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              OutlinedTextField(
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = { Text("Your name") },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("settings_name_input"),
              )
              Button(
                onClick = { onUpdateDisplayName(nameInput) },
                enabled = nameInput.isNotBlank() && nameInput.trim() != uiState.preferences.displayName,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 18.dp),
                modifier = Modifier
                  .height(56.dp)
                  .testTag("settings_save_name_button"),
              ) {
                Text(
                  text = "Save",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                )
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 14.dp),
              color = subtleDividerColor,
            )

            Text(
              text = "Preferred Currency",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(6.dp))
            CurrencyDropdown(
              selectedCurrencyCode = uiState.preferences.preferredCurrency,
              onSelectCurrency = onUpdateCurrency,
            )
          }
        }

        // Card 2: Appearance & Security (Theme + Hide Balances + 4-Digit PIN Lock)
        item {
          SettingsGroupCard {
            SettingsCardHeader(
              icon = Icons.Rounded.Tune,
              title = "Appearance & Security",
            )
            Spacer(modifier = Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
              ThemeMode.entries.forEachIndexed { idx, mode ->
                SegmentedButton(
                  selected = uiState.preferences.themeMode == mode,
                  onClick = { onUpdateThemeMode(mode) },
                  shape = SegmentedButtonDefaults.itemShape(idx, ThemeMode.entries.size),
                  colors = emeraldSegmentedColors,
                  modifier = Modifier.testTag("theme_mode_${mode.name.lowercase()}"),
                ) {
                  Text(
                    text = mode.label.replace(" default", ""),
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 14.dp),
              color = subtleDividerColor,
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
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

            HorizontalDivider(
              modifier = Modifier.padding(vertical = 14.dp),
              color = subtleDividerColor,
            )

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
                  shape = RoundedCornerShape(12.dp),
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
                  shape = RoundedCornerShape(12.dp),
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

        // Card 3: Customization & Alerts (Two identical, aligned navigation rows)
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Column(modifier = Modifier.fillMaxWidth()) {
              SettingsNavigationRow(
                icon = Icons.Rounded.Category,
                title = "Financial Categories",
                subtitle = "$incomeCount income · $expenseCount expense categories",
                contentDescription = "Manage financial categories",
                onClick = onOpenManageCategories,
                testTag = "settings_financial_categories_row",
              )
              HorizontalDivider(
                color = subtleDividerColor,
                modifier = Modifier.padding(start = 66.dp, end = 16.dp),
              )
              SettingsNavigationRow(
                icon = Icons.Rounded.NotificationsActive,
                title = "Alerts & Notifications",
                subtitle = "Budget limits, milestones & reminders",
                contentDescription = "Manage alerts and notifications",
                onClick = onOpenNotificationsSettings,
                testTag = "settings_alerts_navigation_row",
              )
              HorizontalDivider(
                color = subtleDividerColor,
                modifier = Modifier.padding(start = 66.dp, end = 16.dp),
              )
              SettingsNavigationRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                title = stringResource(R.string.help_center_title),
                subtitle = stringResource(R.string.help_center_subtitle),
                contentDescription = "Open Help and FAQ",
                onClick = onOpenHelpCenter,
                testTag = "settings_help_center_row",
              )
            }
          }
        }

        // Card 4: Data Management
        item {
          SettingsGroupCard {
            SettingsCardHeader(
              icon = Icons.Rounded.Storage,
              title = "Data Management",
            )
            Spacer(modifier = Modifier.height(12.dp))
            Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              FilledTonalButton(
                onClick = onExportData,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                  containerColor = MaterialTheme.colorScheme.primaryContainer,
                  contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("settings_export_data_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.FileDownload,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = stringResource(R.string.export_action_label),
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
              OutlinedButton(
                onClick = { showResetConfirm = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.75f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("reset_demo_button"),
              ) {
                Icon(Icons.Rounded.RestartAlt, contentDescription = null)
                Text(
                  text = "Reset to Demonstration Data",
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
              OutlinedButton(
                onClick = { showClearConfirm = true },
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("clear_all_data_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteForever,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                )
                Text(
                  text = "Clear All Financial Records",
                  color = MaterialTheme.colorScheme.error,
                  modifier = Modifier.padding(start = 8.dp),
                )
              }
            }
          }
        }

        // Quiet Unboxed Footer (Privacy & Version Info)
        item {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.size(14.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "Offline-First Private Storage",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
              )
            }
            Text(
              text = stringResource(R.string.privacy_notice),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
              textAlign = TextAlign.Center,
            )
            Text(
              text = stringResource(R.string.app_info_footer),
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
              textAlign = TextAlign.Center,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsGroupCard(
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    modifier = modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      content = content,
    )
  }
}

@Composable
private fun SettingsNavigationRow(
  icon: ImageVector,
  title: String,
  subtitle: String,
  contentDescription: String,
  onClick: () -> Unit,
  testTag: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 64.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp)
      .testTag(testTag),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(20.dp),
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
    Icon(
      imageVector = Icons.Rounded.ChevronRight,
      contentDescription = contentDescription,
      tint = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.size(22.dp),
    )
  }
}

@Composable
private fun SettingsCardHeader(
  icon: ImageVector,
  title: String,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Icon(
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
      modifier = Modifier.size(20.dp),
    )
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
      color = MaterialTheme.colorScheme.onSurface,
    )
  }
}
