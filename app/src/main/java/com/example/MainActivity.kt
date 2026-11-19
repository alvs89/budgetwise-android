package com.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import com.example.ai.configureAppCheck
import com.example.ui.BudgetWiseApp
import com.example.ui.viewmodel.BudgetWiseViewModel

/**
 * Single-activity entry point for BudgetWise.
 *
 * Extends [FragmentActivity] so AndroidX `BiometricPrompt` can be hosted directly during
 * App Lock unlock and PIN recovery flows, and forwards foreground/background transitions to
 * [BudgetWiseViewModel] to enforce the background lock grace period.
 */
class MainActivity : FragmentActivity() {

  private val budgetWiseViewModel: BudgetWiseViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    configureAppCheck(this, intent)
    enableEdgeToEdge()

    setContent {
      BudgetWiseApp(viewModel = budgetWiseViewModel)
    }
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
}
