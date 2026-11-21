package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.entity.CategoryEntity
import com.example.domain.BudgetHealthStatus
import com.example.domain.BudgetSummary
import com.example.domain.ThemeMode
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.ui.screens.BudgetsScreen
import com.example.ui.theme.BudgetWiseTheme
import com.example.ui.viewmodel.BudgetWiseUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.math.BigDecimal

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class AppLockDialogDismissalTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleCategories = listOf(
    CategoryEntity(
      name = "Bills and Utilities",
      type = TransactionType.EXPENSE,
      iconKey = "bolt",
      colorHex = 0xFFF59E0BL,
      isDefault = true,
    ),
    CategoryEntity(
      name = "Food and Dining",
      type = TransactionType.EXPENSE,
      iconKey = "restaurant",
      colorHex = 0xFF0EA5A4L,
      isDefault = true,
    ),
  )

  private val sampleBudget = BudgetSummary(
    budgetId = 1L,
    category = "Bills and Utilities",
    yearMonth = YearMonthKey(2026, 11),
    limitAmount = BigDecimal("5000.00"),
    spentAmount = BigDecimal("1200.00"),
    remainingAmount = BigDecimal("3800.00"),
    utilizationPercent = BigDecimal("24.0"),
    healthStatus = BudgetHealthStatus.NORMAL,
    isSampleData = false,
  )

  @Test
  fun `new and edit budget dialogs close immediately on app lock and stay closed after unlock`() {
    var isAppLocked by mutableStateOf(false)
    val uiState = BudgetWiseUiState(
      isLoading = false,
      selectedMonth = YearMonthKey(2026, 11),
      categories = sampleCategories,
      budgetSummaries = listOf(sampleBudget),
    )

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        CompositionLocalProvider(LocalIsAppLocked provides isAppLocked) {
          BudgetsScreen(
            uiState = uiState,
            onPreviousMonth = {},
            onNextMonth = {},
            onSaveBudget = { _, _, _, _ -> },
            onDeleteBudget = {},
          )
        }
      }
    }

    // 1) Open "New Monthly Budget" dialog
    composeTestRule.onNodeWithTag("add_budget_button").performClick()
    composeTestRule.onNodeWithText("New Monthly Budget").assertIsDisplayed()

    // Lock app -> dialog must immediately vanish (never obstructing PinLockOverlay)
    composeTestRule.runOnIdle { isAppLocked = true }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Monthly Budget").assertDoesNotExist()

    // Unlock app -> dialog must NOT remain left opened
    composeTestRule.runOnIdle { isAppLocked = false }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("New Monthly Budget").assertDoesNotExist()

    // 2) Open "Edit Budget" dialog by tapping existing budget card
    composeTestRule.onNodeWithTag("budget_card_1").performClick()
    composeTestRule.onNodeWithText("Edit Budget").assertIsDisplayed()

    // Lock -> vanishes immediately
    composeTestRule.runOnIdle { isAppLocked = true }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Edit Budget").assertDoesNotExist()

    // Unlock -> stays closed
    composeTestRule.runOnIdle { isAppLocked = false }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Edit Budget").assertDoesNotExist()
  }

  @Test
  fun `confirmation, category picker, and pin setup dialogs auto-dismiss on app lock while lock-screen dialogs remain visible`() {
    var isAppLocked by mutableStateOf(false)
    var showConfirm by mutableStateOf(true)
    var showCategoryPicker by mutableStateOf(true)
    var showPinSetup by mutableStateOf(true)

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        // Main app subtree sees LocalIsAppLocked = isAppLocked
        CompositionLocalProvider(LocalIsAppLocked provides isAppLocked) {
          if (showConfirm) {
            ConfirmationDialog(
              title = "Delete Budget?",
              message = "Remove this budget?",
              confirmLabel = "Delete",
              onConfirm = {},
              onDismiss = { showConfirm = false },
            )
          }
          if (showCategoryPicker) {
            CategoryPickerBottomSheet(
              activeType = TransactionType.EXPENSE,
              allCategories = sampleCategories,
              selectedCategory = "Bills and Utilities",
              allowTypeSwitch = false,
              onSelect = { _, _ -> },
              onDismiss = { showCategoryPicker = false },
            )
          }
          if (showPinSetup) {
            PinSetupOrVerifyDialog(
              mode = PinDialogMode.CREATE,
              biometricAvailable = false,
              onVerifyCurrentPin = { true },
              onCreatePin = { _, _ -> },
              onChangePin = {},
              onDisableAppLock = {},
              onDismiss = { showPinSetup = false },
            )
          }
        }

        // Lock-screen sibling (outside LocalIsAppLocked provider) still renders its own dialogs
        if (isAppLocked) {
          StableFormDialog(
            onDismissRequest = {},
            title = { Text("Forgot PIN Recovery") },
            confirmButton = { TextButton(onClick = {}) { Text("OK") } },
          ) {
            Text("Recovery dialog content")
          }
        }
      }
    }

    composeTestRule.onNodeWithText("Delete Budget?").assertIsDisplayed()
    composeTestRule.onNodeWithText("Select Expense Category").assertIsDisplayed()
    composeTestRule.onNodeWithText("Create 4-Digit PIN").assertIsDisplayed()

    // Activate App Lock
    composeTestRule.runOnIdle { isAppLocked = true }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Delete Budget?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Select Expense Category").assertDoesNotExist()
    composeTestRule.onNodeWithText("Create 4-Digit PIN").assertDoesNotExist()
    // Sibling dialog outside LocalIsAppLocked still works normally
    composeTestRule.onNodeWithText("Forgot PIN Recovery").assertIsDisplayed()

    // Unlock -> none of the dismissed main-app dialogs reappear
    composeTestRule.runOnIdle { isAppLocked = false }
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithText("Delete Budget?").assertDoesNotExist()
    composeTestRule.onNodeWithText("Select Expense Category").assertDoesNotExist()
    composeTestRule.onNodeWithText("Create 4-Digit PIN").assertDoesNotExist()
  }
}
