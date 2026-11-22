package com.example.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Pin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.data.cloud.CloudSyncStatus
import com.example.data.cloud.ProfilePhotoProcessor
import com.example.domain.ThemeMode
import com.example.domain.TransactionType
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.CurrencyDropdown
import com.example.ui.components.LocalIsAppLocked
import com.example.ui.components.PinDialogMode
import com.example.ui.components.PinSetupOrVerifyDialog
import com.example.ui.components.ProfilePhotoCropDialog
import com.example.ui.components.budgetWiseSwitchColors
import com.example.ui.components.isBiometricAuthAvailable
import com.example.ui.viewmodel.BudgetWiseUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
  onClearAllData: () -> Unit,
  onSignOut: () -> Unit = {},
  onPickProfilePhotoUri: (Uri) -> Unit = {},
  onSaveCroppedProfilePhoto: (
    bitmap: Bitmap,
    scale: Float,
    offsetXNorm: Float,
    offsetYNorm: Float,
    onFinished: (Boolean) -> Unit,
  ) -> Unit = { _, _, _, _, onFinished -> onFinished(false) },
  onShowErrorToast: (String) -> Unit = {},
  onUseGooglePhoto: () -> Unit = {},
  onRemoveProfilePhoto: () -> Unit = {},
  onVerifyCloudSync: () -> Unit = {},
  onSuppressNextBackgroundLock: () -> Unit = {},
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val isAppLocked = LocalIsAppLocked.current
  val biometricAvailable = remember(context) { isBiometricAuthAvailable(context) }

  var nameInput by remember(uiState.preferences.displayName) {
    mutableStateOf(uiState.preferences.displayName)
  }
  var showClearConfirm by remember { mutableStateOf(false) }
  var showSignOutConfirm by remember { mutableStateOf(false) }
  var showPhotoActionsDialog by remember { mutableStateOf(false) }
  var pendingCropBitmap by remember { mutableStateOf<Bitmap?>(null) }
  var isDecodingPickedPhoto by remember { mutableStateOf(false) }
  var activePinDialogMode by remember { mutableStateOf<PinDialogMode?>(null) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia(),
  ) { pickedUri ->
    if (pickedUri != null) {
      isDecodingPickedPhoto = true
      coroutineScope.launch {
        val decoded = withContext(Dispatchers.IO) {
          ProfilePhotoProcessor.decodeEditableBitmap(context.applicationContext, pickedUri)
        }
        isDecodingPickedPhoto = false
        if (decoded != null) {
          pendingCropBitmap = decoded
        } else {
          onShowErrorToast(context.getString(R.string.crop_photo_read_error))
        }
      }
    }
  }

  LaunchedEffect(isAppLocked) {
    if (isAppLocked) {
      showClearConfirm = false
      showSignOutConfirm = false
      showPhotoActionsDialog = false
      pendingCropBitmap = null
      activePinDialogMode = null
    }
  }

  val cropBitmap = pendingCropBitmap
  if (cropBitmap != null && !cropBitmap.isRecycled && !isAppLocked) {
    ProfilePhotoCropDialog(
      bitmap = cropBitmap,
      isSaving = uiState.isUpdatingPhoto,
      onCancel = { pendingCropBitmap = null },
      onSaveCrop = { scale, offsetXNorm, offsetYNorm ->
        onSaveCroppedProfilePhoto(cropBitmap, scale, offsetXNorm, offsetYNorm) { succeeded ->
          if (succeeded) {
            pendingCropBitmap = null
          }
        }
      },
    )
  }

  if (showPhotoActionsDialog && !isAppLocked) {
    ProfilePhotoActionDialog(
      hasCustomPhoto = uiState.customPhotoBase64.isNotBlank(),
      hasAnyPhoto = uiState.customPhotoBase64.isNotBlank() || uiState.profilePhotoUrl.isNotBlank(),
      hasGooglePhoto = !uiState.googlePhotoUrl.isNullOrBlank(),
      onChooseFromGallery = {
        showPhotoActionsDialog = false
        onSuppressNextBackgroundLock()
        photoPickerLauncher.launch(
          PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
      },
      onUseGooglePhoto = {
        showPhotoActionsDialog = false
        onUseGooglePhoto()
      },
      onRemovePhoto = {
        showPhotoActionsDialog = false
        onRemoveProfilePhoto()
      },
      onDismiss = { showPhotoActionsDialog = false },
    )
  }

  if (showClearConfirm && !isAppLocked) {
    ConfirmationDialog(
      title = "Clear All Financial Records?",
      message = "This permanently deletes all transactions, budgets, savings goals, and custom categories in your BudgetWise account. This cannot be undone.",
      confirmLabel = "Clear All",
      onConfirm = {
        onClearAllData()
        showClearConfirm = false
      },
      onDismiss = { showClearConfirm = false },
    )
  }

  if (showSignOutConfirm && !isAppLocked) {
    ConfirmationDialog(
      title = stringResource(R.string.settings_sign_out_confirm_title),
      message = stringResource(R.string.settings_sign_out_confirm_message),
      confirmLabel = stringResource(R.string.settings_sign_out_action),
      isDestructive = false,
      onConfirm = {
        showSignOutConfirm = false
        onSignOut()
      },
      onDismiss = { showSignOutConfirm = false },
    )
  }

  if (!isAppLocked) {
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
        // Card 1: Account & Preferences (Google Cloud Account + Display Name + Preferred Currency)
        item {
          SettingsGroupCard {
            SettingsCardHeader(
              icon = Icons.Rounded.Person,
              title = "Account & Preferences",
            )
            Spacer(modifier = Modifier.height(14.dp))

            if (!uiState.accountEmail.isNullOrBlank()) {
              CloudAccountBanner(
                displayName = uiState.preferences.displayName,
                email = uiState.accountEmail,
                profilePhotoUrl = uiState.profilePhotoUrl,
                customPhotoBase64 = uiState.customPhotoBase64,
                isUpdatingPhoto = uiState.isUpdatingPhoto || isDecodingPickedPhoto,
                syncStatus = uiState.syncStatus,
                onAvatarClick = { showPhotoActionsDialog = true },
                onVerifySyncClick = onVerifyCloudSync,
              )
              HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = subtleDividerColor,
              )
            }

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

        // Bottom Sign Out Action (industry-standard placement above privacy/version footer)
        if (!uiState.accountEmail.isNullOrBlank()) {
          item {
            OutlinedButton(
              onClick = { showSignOutConfirm = true },
              shape = RoundedCornerShape(14.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.40f)),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("settings_sign_out_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.Logout,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.settings_sign_out_action),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                softWrap = false,
              )
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
                text = stringResource(R.string.settings_footer_cloud_badge),
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

@Composable
private fun CloudAccountBanner(
  displayName: String,
  email: String,
  profilePhotoUrl: String,
  customPhotoBase64: String,
  isUpdatingPhoto: Boolean,
  syncStatus: CloudSyncStatus,
  onAvatarClick: () -> Unit,
  onVerifySyncClick: () -> Unit,
) {
  val context = LocalContext.current
  val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "B"
  val customPhotoBytes = remember(customPhotoBase64) {
    ProfilePhotoProcessor.decodeBase64ToBytes(customPhotoBase64)
  }
  val avatarModel: Any? = customPhotoBytes ?: profilePhotoUrl.takeIf { it.isNotBlank() }

  val (syncIcon, syncLabel, syncTint) = when (syncStatus) {
    CloudSyncStatus.SYNCED -> Triple(
      Icons.Rounded.CloudDone,
      stringResource(R.string.settings_sync_status_synced),
      MaterialTheme.colorScheme.primary,
    )
    CloudSyncStatus.SYNCING -> Triple(
      Icons.Rounded.CloudSync,
      stringResource(R.string.settings_sync_status_syncing),
      MaterialTheme.colorScheme.primary,
    )
    CloudSyncStatus.OFFLINE -> Triple(
      Icons.Rounded.CloudOff,
      stringResource(R.string.settings_sync_status_offline),
      MaterialTheme.colorScheme.tertiary,
    )
    CloudSyncStatus.ERROR -> Triple(
      Icons.Rounded.ErrorOutline,
      stringResource(R.string.settings_sync_status_error),
      MaterialTheme.colorScheme.error,
    )
  }

  val spinTransition = rememberInfiniteTransition(label = "sync_refresh_spin")
  val refreshRotation by spinTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 900, easing = LinearEasing),
      repeatMode = RepeatMode.Restart,
    ),
    label = "sync_refresh_angle",
  )

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("settings_cloud_account_section"),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Tight Identity Row: 60.dp avatar + tightly grouped email & 28.dp interactive sync pill
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(60.dp)
          .clickable(enabled = !isUpdatingPhoto, onClick = onAvatarClick)
          .testTag("settings_profile_avatar"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier
            .size(58.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = initial,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
          )
          if (avatarModel != null) {
            AsyncImage(
              model = ImageRequest.Builder(context)
                .data(avatarModel)
                .crossfade(true)
                .build(),
              contentDescription = stringResource(R.string.settings_avatar_edit_cd),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
            )
          }

          if (isUpdatingPhoto) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.45f)),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp),
              )
            }
          }
        }

        // Crisp 22.dp mint camera edit badge with 2.dp card-surface cutout border ring
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .size(22.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.CameraAlt,
            contentDescription = stringResource(R.string.settings_avatar_edit_cd),
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(12.dp),
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = email,
          style = MaterialTheme.typography.titleSmall.copy(
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
          ),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("settings_account_email"),
        )
        Row(
          modifier = Modifier
            .height(28.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(syncTint.copy(alpha = 0.14f))
            .border(0.75.dp, syncTint.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .clickable(onClick = onVerifySyncClick)
            .padding(horizontal = 10.dp)
            .testTag("settings_sync_status_badge"),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = syncIcon,
            contentDescription = null,
            tint = syncTint,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = if (isUpdatingPhoto) {
              stringResource(R.string.settings_photo_updating)
            } else {
              syncLabel
            },
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = syncTint,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
          Icon(
            imageVector = Icons.Rounded.Refresh,
            contentDescription = stringResource(R.string.settings_sync_tap_hint),
            tint = syncTint.copy(alpha = 0.85f),
            modifier = Modifier
              .size(13.dp)
              .rotate(if (syncStatus == CloudSyncStatus.SYNCING) refreshRotation else 0f),
          )
        }
      }
    }
  }
}

@Composable
private fun ProfilePhotoActionDialog(
  hasCustomPhoto: Boolean,
  hasAnyPhoto: Boolean,
  hasGooglePhoto: Boolean,
  onChooseFromGallery: () -> Unit,
  onUseGooglePhoto: () -> Unit,
  onRemovePhoto: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    titleContentColor = MaterialTheme.colorScheme.onSurface,
    textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    title = {
      Text(
        text = stringResource(R.string.settings_photo_dialog_title),
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_photo_action_dialog"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = stringResource(R.string.settings_photo_dialog_subtitle),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(2.dp))
        FilledTonalButton(
          onClick = onChooseFromGallery,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
          ),
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .testTag("photo_action_choose_gallery"),
        ) {
          Icon(
            imageVector = Icons.Rounded.PhotoLibrary,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = stringResource(R.string.settings_photo_choose_gallery),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (hasGooglePhoto && (hasCustomPhoto || !hasAnyPhoto)) {
          OutlinedButton(
            onClick = onUseGooglePhoto,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("photo_action_use_google"),
          ) {
            Icon(
              imageVector = Icons.Rounded.AccountCircle,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.settings_photo_use_google),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        if (hasAnyPhoto) {
          OutlinedButton(
            onClick = onRemovePhoto,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(min = 48.dp)
              .testTag("photo_action_remove"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.error,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = stringResource(R.string.settings_photo_remove),
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text(stringResource(R.string.pin_keypad_cancel))
      }
    },
  )
}
