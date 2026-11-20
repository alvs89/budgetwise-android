package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.ai.configureAppCheck
import com.example.notifications.BudgetWiseNotificationManager
import com.example.ui.BudgetWiseApp
import com.example.ui.viewmodel.BudgetWiseViewModel

/**
 * Single-activity entry point for BudgetWise.
 *
 * Extends [FragmentActivity] so AndroidX `BiometricPrompt` can be hosted directly during
 * App Lock unlock and PIN recovery flows, and forwards foreground/background transitions and
 * notification deep-link intents to [BudgetWiseViewModel].
 */
class MainActivity : FragmentActivity() {

  private val budgetWiseViewModel: BudgetWiseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    handleNotificationIntent(intent)
    enableEdgeToEdge()

    setContent {
      BudgetWiseApp(viewModel = budgetWiseViewModel)
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    handleNotificationIntent(intent)
  }

  override fun onStart() {
    super.onStart()
    budgetWiseViewModel.onAppReturnedToForeground()
  }

  override fun onStop() {
    super.onStop()
    if (!isChangingConfigurations) {
      budgetWiseViewModel.onAppMovedToBackground()
    }
  }

  private fun handleNotificationIntent(intent: Intent?) {
    val destination = intent?.getStringExtra(BudgetWiseNotificationManager.EXTRA_ALERT_DESTINATION)
      ?: return
    val alertId = intent.getLongExtra(BudgetWiseNotificationManager.EXTRA_ALERT_ID, -1L)
    budgetWiseViewModel.handleNotificationDeepLink(destination, alertId)
    intent.removeExtra(BudgetWiseNotificationManager.EXTRA_ALERT_DESTINATION)
    intent.removeExtra(BudgetWiseNotificationManager.EXTRA_ALERT_ID)
  }
}
