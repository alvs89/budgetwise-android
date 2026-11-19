package com.example.ui.components

import android.app.KeyguardManager
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.R
import com.example.domain.PinSecurityManager

private tailrec fun Context.findRecoveryFragmentActivity(): FragmentActivity? = when (this) {
  is FragmentActivity -> this
  is ContextWrapper -> baseContext.findRecoveryFragmentActivity()
  else -> null
}

/**
 * Returns true if the device supports strong/weak biometrics or a secure system lock screen
 * (PIN, pattern, or password) that can prove device ownership during PIN recovery.
 */
fun isDeviceCredentialRecoveryAvailable(context: Context): Boolean {
  return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
    val manager = BiometricManager.from(context)
    val result = manager.canAuthenticate(
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    )
    result == BiometricManager.BIOMETRIC_SUCCESS
  } else {
    val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    keyguard?.isDeviceSecure == true || isBiometricAuthAvailable(context)
  }
}

/**
 * Triggers Android's system Biometric / Device Credential prompt to verify device ownership
 * before allowing a PIN reset without data loss.
 */
fun showDeviceCredentialRecoveryPrompt(
  context: Context,
  title: String = "Verify Device Ownership",
  subtitle: String = "Authenticate with your phone lock screen or biometrics to reset your BudgetWise PIN",
  onSuccess: () -> Unit,
  onError: (String) -> Unit = {},
) {
  val activity = context.findRecoveryFragmentActivity()
  if (activity == null) {
    onError("Unable to open device authentication on this screen.")
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
        val cancelled = errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
          errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
          errorCode == BiometricPrompt.ERROR_CANCELED
        if (!cancelled) {
          onError(errString.toString().ifBlank { "Device verification could not be completed." })
        }
      }
    },
  )

  try {
    val allowedAuthenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      BiometricManager.Authenticators.BIOMETRIC_STRONG or
        BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    } else {
      BiometricManager.Authenticators.BIOMETRIC_WEAK or
        BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
      .setTitle(title)
      .setSubtitle(subtitle)
      .setAllowedAuthenticators(allowedAuthenticators)
      .build()
    prompt.authenticate(promptInfo)
  } catch (_: Exception) {
    onError("Device credential verification is unavailable on this device.")
  }
}

private enum class RecoveryStep {
  OPTIONS,
  SET_NEW_PIN,
  CONFIRM_WIPE,
}

/**
 * Full-screen in-place PIN recovery view rendered directly inside [PinLockOverlay]'s opaque
 * surface so step transitions happen immediately with zero dialog-window lag and no 4-digit
 * lock keypad bleeding through behind the recovery UI.
 */
@Composable
fun ForgotPinRecoveryContent(
  onResetPinAfterDeviceAuth: (newPin: String) -> Unit,
  onDisableAppLockAfterDeviceAuth: () -> Unit,
  onFactoryResetAndWipeData: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val deviceRecoveryAvailable = remember(context) { isDeviceCredentialRecoveryAvailable(context) }

  var step by remember { mutableStateOf(RecoveryStep.OPTIONS) }
  var authError by remember { mutableStateOf<String?>(null) }
  var isCompleting by remember { mutableStateOf(false) }

  // State for SET_NEW_PIN step after device ownership is verified
  var pinEntryStage by remember { mutableIntStateOf(0) } // 0 = enter new PIN, 1 = confirm new PIN
  var firstNewPin by remember { mutableStateOf("") }
  var currentPinInput by remember { mutableStateOf("") }
  var pinMismatchError by remember { mutableStateOf<String?>(null) }

  // State for CONFIRM_WIPE step
  var resetConfirmationInput by remember { mutableStateOf("") }
  var showDisableLockConfirm by remember { mutableStateOf(false) }

  if (showDisableLockConfirm) {
    ConfirmationDialog(
      title = stringResource(R.string.pin_recovery_disable_confirm_title),
      message = stringResource(R.string.pin_recovery_disable_confirm_message),
      confirmLabel = stringResource(R.string.pin_recovery_disable_confirm_button),
      onConfirm = {
        showDisableLockConfirm = false
        isCompleting = true
        onDisableAppLockAfterDeviceAuth()
      },
      onDismiss = { showDisableLockConfirm = false },
    )
  }

  BackHandler(enabled = !isCompleting) {
    when (step) {
      RecoveryStep.CONFIRM_WIPE -> step = RecoveryStep.OPTIONS
      RecoveryStep.SET_NEW_PIN, RecoveryStep.OPTIONS -> onDismiss()
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .testTag("pin_recovery_screen"),
    contentAlignment = Alignment.Center,
  ) {
    AnimatedContent(
      targetState = step,
      transitionSpec = {
        fadeIn(tween(durationMillis = 160)) togetherWith fadeOut(tween(durationMillis = 120))
      },
      label = "pin_recovery_step",
    ) { activeStep ->
      when (activeStep) {
        RecoveryStep.OPTIONS -> {
          Column(
            modifier = Modifier
              .widthIn(max = 400.dp)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.LockReset,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp),
              )
            }

            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = stringResource(R.string.pin_recovery_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center,
              )
              Text(
                text = stringResource(R.string.pin_recovery_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
              )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Tier 1: Device Authentication Recovery (Zero Data Loss)
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = stringResource(R.string.pin_recovery_device_auth_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                }
                Text(
                  text = if (deviceRecoveryAvailable) {
                    stringResource(R.string.pin_recovery_device_auth_desc)
                  } else {
                    stringResource(R.string.pin_recovery_device_auth_unavailable)
                  },
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (authError != null) {
                  Text(
                    text = authError!!,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                  )
                }
                Button(
                  onClick = {
                    authError = null
                    showDeviceCredentialRecoveryPrompt(
                      context = context,
                      onSuccess = {
                        pinEntryStage = 0
                        firstNewPin = ""
                        currentPinInput = ""
                        pinMismatchError = null
                        step = RecoveryStep.SET_NEW_PIN
                      },
                      onError = { msg -> authError = msg },
                    )
                  },
                  enabled = deviceRecoveryAvailable && !isCompleting,
                  modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pin_recovery_device_auth_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Key,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Verify & Reset PIN",
                    modifier = Modifier.padding(start = 8.dp),
                  )
                }
              }
            }

            // Tier 2: Destructive Reset Fallback (Privacy Guarantee)
            Surface(
              shape = MaterialTheme.shapes.medium,
              color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                  )
                  Text(
                    text = stringResource(R.string.pin_recovery_wipe_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.error,
                  )
                }
                Text(
                  text = stringResource(R.string.pin_recovery_wipe_desc),
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                  onClick = {
                    resetConfirmationInput = ""
                    step = RecoveryStep.CONFIRM_WIPE
                  },
                  enabled = !isCompleting,
                  colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                  ),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("pin_recovery_wipe_option_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Text(
                    text = "Reset App & Clear Data…",
                    modifier = Modifier.padding(start = 8.dp),
                  )
                }
              }
            }

            TextButton(
              onClick = onDismiss,
              enabled = !isCompleting,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("pin_recovery_cancel_button"),
            ) {
              Text("Back to PIN Login")
            }
          }
        }

        RecoveryStep.SET_NEW_PIN -> {
          fun onRecoveryDigit(digit: Int) {
            if (isCompleting || currentPinInput.length >= PinSecurityManager.PIN_LENGTH) return
            pinMismatchError = null
            val next = currentPinInput + digit.toString()
            currentPinInput = next
            if (next.length == PinSecurityManager.PIN_LENGTH) {
              if (pinEntryStage == 0) {
                firstNewPin = next
                currentPinInput = ""
                pinEntryStage = 1
              } else {
                if (next == firstNewPin) {
                  isCompleting = true
                  onResetPinAfterDeviceAuth(next)
                } else {
                  currentPinInput = ""
                  pinMismatchError = "PINs didn't match. Try again."
                }
              }
            }
          }

          Column(
            modifier = Modifier
              .widthIn(max = 360.dp)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
          ) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.VerifiedUser,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(30.dp),
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = if (pinEntryStage == 0) "Set New 4-Digit PIN" else "Confirm New PIN",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = when {
                isCompleting -> "Saving your new PIN…"
                pinMismatchError != null -> pinMismatchError!!
                pinEntryStage == 0 -> "Device verified. Choose a new 4-digit PIN to protect your records."
                else -> "Re-enter your new 4-digit PIN to confirm."
              },
              style = MaterialTheme.typography.bodyMedium,
              color = if (pinMismatchError != null) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            PinDotsIndicator(
              filledCount = currentPinInput.length,
              isError = pinMismatchError != null,
            )

            Spacer(modifier = Modifier.height(32.dp))

            NumericPinKeypad(
              enabled = !isCompleting,
              onDigitClick = ::onRecoveryDigit,
              onBackspaceClick = {
                if (currentPinInput.isNotEmpty()) {
                  currentPinInput = currentPinInput.dropLast(1)
                }
              },
              leftActionText = stringResource(R.string.pin_keypad_cancel),
              onLeftActionClick = onDismiss,
              leftActionTestTag = "pin_recovery_reset_cancel_button",
            )

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
              onClick = { showDisableLockConfirm = true },
              enabled = !isCompleting,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("pin_recovery_turn_off_lock_button"),
            ) {
              Text(
                text = stringResource(R.string.pin_recovery_turn_off_action),
                style = MaterialTheme.typography.labelMedium.copy(
                  textDecoration = TextDecoration.Underline,
                ),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.80f),
              )
            }
          }
        }

        RecoveryStep.CONFIRM_WIPE -> {
          val confirmed = PinSecurityManager.isValidResetConfirmation(resetConfirmationInput)
          Column(
            modifier = Modifier
              .widthIn(max = 400.dp)
              .fillMaxWidth()
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.errorContainer),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = Icons.Rounded.DeleteForever,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(28.dp),
              )
            }

            Text(
              text = "Erase All Data & Remove PIN?",
              style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground,
              textAlign = TextAlign.Center,
            )

            Text(
              text = "This action cannot be undone. Every transaction, budget, savings goal, custom category, and saved chat message on this device will be permanently deleted before App Lock is removed.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
            )

            OutlinedTextField(
              value = resetConfirmationInput,
              onValueChange = { resetConfirmationInput = it },
              label = { Text(stringResource(R.string.pin_recovery_confirm_hint)) },
              placeholder = { Text(PinSecurityManager.RESET_CONFIRMATION_KEYWORD) },
              singleLine = true,
              enabled = !isCompleting,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pin_recovery_reset_keyword_input"),
            )

            Button(
              onClick = {
                isCompleting = true
                onFactoryResetAndWipeData()
              },
              enabled = confirmed && !isCompleting,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
              ),
              modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .testTag("pin_recovery_confirm_wipe_button"),
            ) {
              Text(stringResource(R.string.pin_recovery_confirm_action))
            }

            TextButton(
              onClick = { step = RecoveryStep.OPTIONS },
              enabled = !isCompleting,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("pin_recovery_back_button"),
            ) {
              Text("Back")
            }
          }
        }
      }
    }
  }
}
