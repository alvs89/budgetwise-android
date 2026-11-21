package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.ai.configureAppCheck
import com.example.notifications.BudgetWiseNotificationManager
import com.example.ui.BudgetWiseRoot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Single-activity entry point for BudgetWise.
 *
 * Initializes Firebase App Check before composing the auth-gated [BudgetWiseRoot], extends
 * [FragmentActivity] so AndroidX `BiometricPrompt` can host App Lock recovery flows, and forwards
 * lifecycle transitions and notification deep-links into the currently signed-in user's ViewModel.
 */
class MainActivity : FragmentActivity() {

  private val _foregroundEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
  private val _backgroundEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
  private val _pendingNotificationDeepLink = MutableStateFlow<Pair<String, Long>?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    handleNotificationIntent(intent)
    enableEdgeToEdge()

    setContent {
      BudgetWiseRoot(
        foregroundEvents = _foregroundEvents.asSharedFlow(),
        backgroundEvents = _backgroundEvents.asSharedFlow(),
        pendingNotificationDeepLink = _pendingNotificationDeepLink.asStateFlow(),
        onConsumeNotificationDeepLink = { _pendingNotificationDeepLink.value = null },
      )
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  override fun onStart() {
    super.onStart()
    _foregroundEvents.tryEmit(Unit)
  }

  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      _backgroundEvents.tryEmit(Unit)
    }
  }

  private fun handleNotificationIntent(intent: Intent?) {
    val destination = intent?.getStringExtra(BudgetWiseNotificationManager.EXTRA_ALERT_DESTINATION)
      ?: return
    val alertId = intent.getLongExtra(BudgetWiseNotificationManager.EXTRA_ALERT_ID, -1L)
    _pendingNotificationDeepLink.value = destination to alertId
    intent.removeExtra(BudgetWiseNotificationManager.EXTRA_ALERT_DESTINATION)
    intent.removeExtra(BudgetWiseNotificationManager.EXTRA_ALERT_ID)
  }
}
