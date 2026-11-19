package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FinancialChatService
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.PaymentMethod
import com.example.domain.PinSecurityManager
import com.example.domain.TransactionType
import com.example.domain.currentEpochDay
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinRecoverySecurityTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val stubChatService = object : FinancialChatService {
    override fun streamReply(
      priorHistory: List<ChatMessageEntity>,
      userQuestion: String,
      financialContextDigest: String,
    ): Flow<String> = flowOf("Test reply")
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `progressive lockout escalates after 5 failed attempts and caps at 5 minutes`() {
    assertEquals(0L, PinSecurityManager.lockoutDurationForAttempts(0))
    assertEquals(0L, PinSecurityManager.lockoutDurationForAttempts(4))
    assertEquals(30_000L, PinSecurityManager.lockoutDurationForAttempts(5))
    assertEquals(60_000L, PinSecurityManager.lockoutDurationForAttempts(6))
    assertEquals(120_000L, PinSecurityManager.lockoutDurationForAttempts(7))
    assertEquals(240_000L, PinSecurityManager.lockoutDurationForAttempts(8))
    assertEquals(300_000L, PinSecurityManager.lockoutDurationForAttempts(9))
    assertEquals(300_000L, PinSecurityManager.lockoutDurationForAttempts(20))
  }

  @Test
  fun `destructive reset confirmation strictly requires exact RESET keyword`() {
    assertTrue(PinSecurityManager.isValidResetConfirmation("RESET"))
    assertTrue(PinSecurityManager.isValidResetConfirmation("  RESET  "))
    assertFalse(PinSecurityManager.isValidResetConfirmation("reset"))
    assertFalse(PinSecurityManager.isValidResetConfirmation("Reset"))
    assertFalse(PinSecurityManager.isValidResetConfirmation("RESE"))
    assertFalse(PinSecurityManager.isValidResetConfirmation(""))
  }

  @Test
  fun `resetPinAfterDeviceAuth replaces PIN, unlocks app, clears failures, and preserves records`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)

    viewModel.saveTransaction(
      existingId = 0L,
      title = "Cafe Test",
      amountCentavos = 45_000L,
      type = TransactionType.EXPENSE,
      category = "Food",
      dateEpochDay = currentEpochDay(),
      paymentMethod = PaymentMethod.CASH,
      notes = "Preserved across PIN reset",
    )
    withTimeout(5_000L) {
      viewModel.uiState.first { state ->
        state.allTransactions.any { it.title == "Cafe Test" }
      }
    }

    viewModel.enableAppLockWithPin("1234", enableBiometric = false)
    withTimeout(5_000L) {
      viewModel.uiState.first {
        it.preferences.appLockEnabled && PinSecurityManager.verifyPin("1234", it.preferences.pinHashRecord)
      }
    }

    viewModel.lockAppNow()
    assertTrue(viewModel.isAppLocked.value)

    // Record a failed attempt before recovery
    assertFalse(viewModel.verifyPin("9999"))
    withTimeout(5_000L) {
      viewModel.uiState.first { it.preferences.failedPinAttempts >= 1 }
    }

    // Perform Tier 1 recovery
    viewModel.resetPinAfterDeviceAuth("5678")

    val recoveredState = withTimeout(5_000L) {
      viewModel.uiState.first {
        PinSecurityManager.verifyPin("5678", it.preferences.pinHashRecord) &&
          it.preferences.failedPinAttempts == 0 &&
          it.preferences.pinLockedUntilMillis == 0L
      }
    }

    val unlockedAfterReset = withTimeout(5_000L) {
      viewModel.isAppLocked.first { !it }
    }

    assertFalse(unlockedAfterReset)
    assertTrue(recoveredState.preferences.appLockEnabled)
    assertFalse(PinSecurityManager.verifyPin("1234", recoveredState.preferences.pinHashRecord))
    assertTrue(recoveredState.allTransactions.any { it.title == "Cafe Test" })
  }

  @Test
  fun `factoryResetForForgottenPin wipes local data, removes PIN, and unlocks app`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)

    viewModel.saveTransaction(
      existingId = 0L,
      title = "Sensitive Payroll",
      amountCentavos = 250_000L,
      type = TransactionType.INCOME,
      category = "Salary",
      dateEpochDay = currentEpochDay(),
      paymentMethod = PaymentMethod.BANK_TRANSFER,
      notes = "Must be wiped on factory reset",
    )
    withTimeout(5_000L) {
      viewModel.uiState.first { state ->
        state.allTransactions.any { it.title == "Sensitive Payroll" }
      }
    }

    viewModel.enableAppLockWithPin("4321", enableBiometric = true)
    withTimeout(5_000L) {
      viewModel.uiState.first {
        it.preferences.appLockEnabled && PinSecurityManager.verifyPin("4321", it.preferences.pinHashRecord)
      }
    }

    viewModel.lockAppNow()
    assertTrue(viewModel.isAppLocked.value)

    // Perform Tier 2 destructive reset
    viewModel.factoryResetForForgottenPin()

    val wipedState = withTimeout(5_000L) {
      viewModel.uiState.first { state ->
        !state.preferences.appLockEnabled &&
          state.preferences.pinHashRecord == null &&
          state.allTransactions.isEmpty()
      }
    }
    val unlockedAfterWipe = withTimeout(5_000L) {
      viewModel.isAppLocked.first { !it }
    }

    assertFalse(unlockedAfterWipe)
    assertFalse(wipedState.preferences.appLockEnabled)
    assertFalse(wipedState.preferences.biometricUnlockEnabled)
    assertNull(wipedState.preferences.pinHashRecord)
    assertEquals(0, wipedState.preferences.failedPinAttempts)
    assertEquals(0L, wipedState.preferences.pinLockedUntilMillis)
    assertTrue(wipedState.allTransactions.isEmpty())
  }
}
