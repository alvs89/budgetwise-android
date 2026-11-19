package com.example.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.domain.PinSecurityManager
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

fun isBiometricAuthAvailable(context: Context): Boolean {
  val manager = BiometricManager.from(context)
  val result = manager.canAuthenticate(
    BiometricManager.Authenticators.BIOMETRIC_STRONG or
      BiometricManager.Authenticators.BIOMETRIC_WEAK
  )
  return result == BiometricManager.BIOMETRIC_SUCCESS
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
  is FragmentActivity -> this
  is ContextWrapper -> baseContext.findFragmentActivity()
  else -> null
}

fun showBiometricUnlockPrompt(
  context: Context,
  title: String = "Unlock BudgetWise",
  subtitle: String = "Use your fingerprint or face to access your finances",
  onSuccess: () -> Unit,
  onFallbackToPin: () -> Unit = {},
) {
  val activity = context.findFragmentActivity()
  if (activity == null) {
    onFallbackToPin()
    return
  }
  val executor = ContextCompat.getMainExecutor(activity)
  val prompt = BiometricPrompt(
    activity,
    executor,
    object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        onFallbackToPin()
      }
    },
  )
  val promptInfo = BiometricPrompt.PromptInfo.Builder()
    .setTitle(title)
    .setSubtitle(subtitle)
    .setAllowedAuthenticators(
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    .setNegativeButtonText("Use PIN")
    .setConfirmationRequired(false)
    .build()
  prompt.authenticate(promptInfo)
}

@Composable
fun PinLockOverlay(
  visible: Boolean,
  splashActive: Boolean,
  biometricEnabled: Boolean,
  failedAttempts: Int,
  lockedUntilMillis: Long,
  onVerifyPin: (String) -> Boolean,
  onBiometricSuccess: () -> Unit,
  onResetPinAfterDeviceAuth: (String) -> Unit,
  onDisableAppLockAfterDeviceAuth: () -> Unit,
  onFactoryResetForForgottenPin: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val financeColors = LocalFinanceColors.current
  val biometricAvailable = remember(context) { isBiometricAuthAvailable(context) }
  val canUseBiometric = biometricEnabled && biometricAvailable

  var enteredPin by remember { mutableStateOf("") }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  var biometricTriggeredThisLock by remember { mutableStateOf(false) }
  var showForgotPinDialog by remember { mutableStateOf(false) }

  val shakeOffset = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  LaunchedEffect(visible) {
    if (!visible) {
      biometricTriggeredThisLock = false
    } else {
      enteredPin = ""
      errorMessage = null
      showForgotPinDialog = false
    }
  }

  LaunchedEffect(visible, splashActive, canUseBiometric) {
    if (visible && !splashActive && canUseBiometric && !biometricTriggeredThisLock) {
      biometricTriggeredThisLock = true
      delay(120L)
      showBiometricUnlockPrompt(
        context = context,
        onSuccess = onBiometricSuccess,
      )
    }
  }

  LaunchedEffect(visible, lockedUntilMillis) {
    nowMillis = System.currentTimeMillis()
    if (visible && lockedUntilMillis > nowMillis) {
      while (lockedUntilMillis > System.currentTimeMillis()) {
        nowMillis = System.currentTimeMillis()
        delay(250L)
      }
      nowMillis = System.currentTimeMillis()
      errorMessage = null
    }
  }

  val isLockedOut = PinSecurityManager.isLockedOut(lockedUntilMillis, nowMillis)
  val remainingLockoutSec = PinSecurityManager.remainingLockoutSeconds(lockedUntilMillis, nowMillis)
  val lockoutMessage = stringResource(R.string.pin_lockout_message)

  fun triggerShake() {
    scope.launch {
      val offsets = listOf(-18f, 18f, -12f, 12f, -6f, 6f, 0f)
      offsets.forEach { target ->
        shakeOffset.animateTo(target, tween(durationMillis = 36))
      }
    }
  }

  fun onDigitTap(digit: Int) {
    if (isLockedOut || enteredPin.length >= PinSecurityManager.PIN_LENGTH) return
    errorMessage = null
    val next = enteredPin + digit.toString()
    enteredPin = next
    if (next.length == PinSecurityManager.PIN_LENGTH) {
      val ok = onVerifyPin(next)
      if (!ok) {
        enteredPin = ""
        val nextAttempts = failedAttempts + 1
        val lockoutMs = PinSecurityManager.lockoutDurationForAttempts(nextAttempts)
        val remaining = (PinSecurityManager.MAX_FAILED_ATTEMPTS - nextAttempts).coerceAtLeast(0)
        errorMessage = if (lockoutMs > 0L) {
          lockoutMessage
        } else {
          "Incorrect PIN · $remaining attempt${if (remaining == 1) "" else "s"} left"
        }
        triggerShake()
      }
    }
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
    exit = fadeOut(tween(durationMillis = 220, easing = FastOutSlowInEasing)),
    modifier = modifier,
  ) {
    DisposableEffect(Unit) {
      onDispose {
        enteredPin = ""
        errorMessage = null
        showForgotPinDialog = false
      }
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .safeDrawingPadding()
        .testTag("pin_lock_overlay"),
      contentAlignment = Alignment.Center,
    ) {
      if (showForgotPinDialog) {
        ForgotPinRecoveryContent(
          onResetPinAfterDeviceAuth = onResetPinAfterDeviceAuth,
          onDisableAppLockAfterDeviceAuth = onDisableAppLockAfterDeviceAuth,
          onFactoryResetAndWipeData = onFactoryResetForForgottenPin,
          onDismiss = { showForgotPinDialog = false },
        )
      } else {
        Column(
          modifier = Modifier
            .widthIn(max = 360.dp)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.Center,
        ) {
          Surface(
            modifier = Modifier.size(64.dp),
            shape = RoundedCornerShape(18.dp),
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
          ) {
            Box(
              modifier = Modifier
                .fillMaxSize()
                .background(
                  brush = Brush.linearGradient(
                    colors = listOf(financeColors.heroStart, financeColors.heroEnd),
                  ),
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(30.dp),
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "BudgetWise is Locked",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
          )

          Spacer(modifier = Modifier.height(6.dp))

          val statusText = when {
            isLockedOut -> lockoutMessage
            errorMessage != null -> errorMessage!!
            else -> "Enter your 4-digit PIN to continue"
          }
          val statusColor = if (isLockedOut || errorMessage != null) {
            MaterialTheme.colorScheme.error
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          }

          Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = statusColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .heightIn(min = 24.dp)
              .testTag("pin_lock_status_text"),
          )

          if (isLockedOut) {
            Spacer(modifier = Modifier.height(10.dp))
            val formattedCountdown = PinSecurityManager.formatLockoutCountdown(remainingLockoutSec)
            Surface(
              shape = RoundedCornerShape(999.dp),
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f)),
              modifier = Modifier.testTag("pin_lockout_countdown_badge"),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Schedule,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = stringResource(R.string.pin_lockout_countdown_format, formattedCountdown),
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontFeatureSettings = "tnum",
                  ),
                  color = MaterialTheme.colorScheme.error,
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(22.dp))

          PinDotsIndicator(
            filledCount = enteredPin.length,
            totalCount = PinSecurityManager.PIN_LENGTH,
            isError = errorMessage != null || isLockedOut,
            modifier = Modifier.graphicsLayer { translationX = shakeOffset.value },
          )

          Spacer(modifier = Modifier.height(32.dp))

          NumericPinKeypad(
            enabled = !isLockedOut,
            showBiometricButton = canUseBiometric,
            onDigitClick = ::onDigitTap,
            onBackspaceClick = {
              if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
              }
            },
            onBiometricClick = {
              showBiometricUnlockPrompt(
                context = context,
                onSuccess = onBiometricSuccess,
              )
            },
          )

          Spacer(modifier = Modifier.height(16.dp))

          TextButton(
            onClick = { showForgotPinDialog = true },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("pin_forgot_button"),
          ) {
            Text(
              text = stringResource(R.string.pin_forgot_action),
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }
      }
    }
  }
}

@Composable
fun PinDotsIndicator(
  filledCount: Int,
  totalCount: Int = PinSecurityManager.PIN_LENGTH,
  isError: Boolean = false,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(18.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(totalCount) { index ->
      val filled = index < filledCount
      val fillColor = when {
        isError -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> Color.Transparent
      }
      val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        filled -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
      }
      Box(
        modifier = Modifier
          .size(16.dp)
          .clip(CircleShape)
          .background(fillColor)
          .border(width = 1.5.dp, color = borderColor, shape = CircleShape),
      )
    }
  }
}

@Composable
fun NumericPinKeypad(
  enabled: Boolean = true,
  showBiometricButton: Boolean = false,
  buttonSize: Dp = 66.dp,
  onDigitClick: (Int) -> Unit,
  onBackspaceClick: () -> Unit,
  onBiometricClick: (() -> Unit)? = null,
  leftActionText: String? = null,
  onLeftActionClick: (() -> Unit)? = null,
  leftActionTestTag: String = "pin_left_action_button",
  modifier: Modifier = Modifier,
) {
  val haptic = LocalHapticFeedback.current
  val rows = listOf(
    listOf(1, 2, 3),
    listOf(4, 5, 6),
    listOf(7, 8, 9),
  )

  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(14.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    rows.forEach { row ->
      Row(
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        row.forEach { digit ->
          KeypadDigitButton(
            digit = digit,
            size = buttonSize,
            enabled = enabled,
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
              onDigitClick(digit)
            },
          )
        }
      }
    }

    Row(
      horizontalArrangement = Arrangement.spacedBy(22.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (showBiometricButton && onBiometricClick != null) {
        Surface(
          modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onBiometricClick)
            .testTag("pin_biometric_button"),
          shape = CircleShape,
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Rounded.Fingerprint,
              contentDescription = "Unlock with biometrics",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(28.dp),
            )
          }
        }
      } else if (leftActionText != null && onLeftActionClick != null) {
        Box(
          modifier = Modifier
            .size(buttonSize)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onLeftActionClick)
            .testTag(leftActionTestTag),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = leftActionText,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = if (enabled) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            },
            maxLines = 1,
            softWrap = false,
            textAlign = TextAlign.Center,
          )
        }
      } else {
        Spacer(modifier = Modifier.size(buttonSize))
      }

      KeypadDigitButton(
        digit = 0,
        size = buttonSize,
        enabled = enabled,
        onClick = {
          haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          onDigitClick(0)
        },
      )

      Surface(
        modifier = Modifier
          .size(buttonSize)
          .clip(CircleShape)
          .clickable(enabled = enabled, onClick = onBackspaceClick)
          .testTag("pin_backspace_button"),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.Backspace,
            contentDescription = "Backspace",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun KeypadDigitButton(
  digit: Int,
  size: Dp,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .size(size)
      .clip(CircleShape)
      .clickable(enabled = enabled, onClick = onClick)
      .testTag("pin_digit_$digit"),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = digit.toString(),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        color = if (enabled) {
          MaterialTheme.colorScheme.onSurface
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        },
      )
    }
  }
}

enum class PinDialogMode {
  CREATE,
  CHANGE,
  DISABLE,
}

@Composable
fun PinSetupOrVerifyDialog(
  mode: PinDialogMode,
  biometricAvailable: Boolean,
  onVerifyCurrentPin: (String) -> Boolean,
  onCreatePin: (pin: String, enableBiometric: Boolean) -> Unit,
  onChangePin: (newPin: String) -> Unit,
  onDisableAppLock: () -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val deviceRecoveryAvailable = remember(context) { isDeviceCredentialRecoveryAvailable(context) }

  // Stage meanings:
  // 0 = verify current PIN (for CHANGE / DISABLE)
  // 1 = enter new PIN (for CREATE / CHANGE)
  // 2 = confirm new PIN (for CREATE / CHANGE)
  var stage by remember(mode) {
    mutableIntStateOf(if (mode == PinDialogMode.CREATE) 1 else 0)
  }
  var firstPin by remember { mutableStateOf("") }
  var currentInput by remember { mutableStateOf("") }
  var errorText by remember { mutableStateOf<String?>(null) }
  var enableBiometric by remember { mutableStateOf(biometricAvailable) }

  val title = when (stage) {
    0 -> if (mode == PinDialogMode.DISABLE) "Disable App Lock" else "Verify Current PIN"
    1 -> if (mode == PinDialogMode.CHANGE) "Create New PIN" else "Create 4-Digit PIN"
    else -> "Confirm 4-Digit PIN"
  }
  val subtitle = when (stage) {
    0 -> "Enter your current 4-digit PIN to continue"
    1 -> "Choose a 4-digit PIN to protect BudgetWise"
    else -> "Re-enter your 4-digit PIN to confirm"
  }

  fun handleDigit(digit: Int) {
    if (currentInput.length >= PinSecurityManager.PIN_LENGTH) return
    errorText = null
    val next = currentInput + digit.toString()
    currentInput = next

    if (next.length == PinSecurityManager.PIN_LENGTH) {
      when (stage) {
        0 -> {
          if (onVerifyCurrentPin(next)) {
            currentInput = ""
            if (mode == PinDialogMode.DISABLE) {
              onDisableAppLock()
            } else {
              stage = 1
            }
          } else {
            currentInput = ""
            errorText = "Incorrect current PIN"
          }
        }
        1 -> {
          firstPin = next
          currentInput = ""
          stage = 2
        }
        2 -> {
          if (next == firstPin) {
            if (mode == PinDialogMode.CREATE) {
              onCreatePin(next, enableBiometric)
            } else {
              onChangePin(next)
            }
          } else {
            currentInput = ""
            errorText = "PINs didn't match. Try again."
          }
        }
      }
    }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Rounded.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
      )
    },
    text = {
      Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text(
          text = errorText ?: subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = if (errorText != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )

        PinDotsIndicator(
          filledCount = currentInput.length,
          isError = errorText != null,
        )

        if (mode == PinDialogMode.CREATE && stage == 2 && biometricAvailable) {
          Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Fingerprint,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "Unlock with biometrics",
                  style = MaterialTheme.typography.bodySmall,
                )
              }
              Switch(
                checked = enableBiometric,
                onCheckedChange = { enableBiometric = it },
                colors = budgetWiseSwitchColors(),
              )
            }
          }
        }

        NumericPinKeypad(
          buttonSize = 54.dp,
          onDigitClick = ::handleDigit,
          onBackspaceClick = {
            if (currentInput.isNotEmpty()) {
              currentInput = currentInput.dropLast(1)
            }
          },
        )

        if (stage == 0 && deviceRecoveryAvailable) {
          TextButton(
            onClick = {
              errorText = null
              showDeviceCredentialRecoveryPrompt(
                context = context,
                title = "Verify Device Ownership",
                subtitle = if (mode == PinDialogMode.DISABLE) {
                  "Authenticate with your device lock screen or biometrics to disable App Lock"
                } else {
                  "Authenticate with your device lock screen or biometrics to set a new PIN"
                },
                onSuccess = {
                  currentInput = ""
                  errorText = null
                  if (mode == PinDialogMode.DISABLE) {
                    onDisableAppLock()
                  } else {
                    stage = 1
                  }
                },
                onError = { msg -> errorText = msg },
              )
            },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("pin_dialog_forgot_button"),
          ) {
            Text(
              text = stringResource(R.string.pin_forgot_action),
              style = MaterialTheme.typography.labelMedium,
            )
          }
        } else if (mode == PinDialogMode.CREATE && stage == 1) {
          Text(
            text = stringResource(R.string.pin_recovery_setup_tip),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
          )
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("pin_dialog_cancel_button"),
      ) {
        Text("Cancel")
      }
    },
  )
}
