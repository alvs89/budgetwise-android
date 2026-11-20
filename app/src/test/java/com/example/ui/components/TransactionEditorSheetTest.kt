package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.font.FontWeight
import com.example.data.local.entity.CategoryEntity
import com.example.domain.PaymentMethod
import com.example.domain.ThemeMode
import com.example.domain.TransactionType
import com.example.ui.theme.BudgetWiseTheme
import com.example.ui.theme.BudgetWiseTypography
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class TransactionEditorSheetTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleCategories = listOf(
    CategoryEntity(name = "Food and Dining", type = TransactionType.EXPENSE, iconKey = "restaurant", colorHex = 0xFF0EA5A4L, isDefault = true),
    CategoryEntity(name = "Transportation", type = TransactionType.EXPENSE, iconKey = "directions_bus", colorHex = 0xFF3B82F6L, isDefault = true),
    CategoryEntity(name = "Shopping", type = TransactionType.EXPENSE, iconKey = "shopping_bag", colorHex = 0xFFEC4899L, isDefault = true),
    CategoryEntity(name = "Bills and Utilities", type = TransactionType.EXPENSE, iconKey = "receipt", colorHex = 0xFFF59E0BL, isDefault = true),
    CategoryEntity(name = "Salary", type = TransactionType.INCOME, iconKey = "work", colorHex = 0xFF10B981L, isDefault = true),
    CategoryEntity(name = "Allowance", type = TransactionType.INCOME, iconKey = "savings", colorHex = 0xFF22C55EL, isDefault = true),
  )

  @Test
  fun `category selector opens filtered 3-column grid picker and sticky CTA submits transaction`() {
    var savedCategory: String? = null
    var savedMethod: PaymentMethod? = null
    var savedType: TransactionType? = null

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        TransactionEditorSheet(
          existingTransaction = null,
          categories = sampleCategories,
          currencyCode = "PHP",
          onSave = { _, _, _, type, category, _, method, _, _ ->
            savedType = type
            savedCategory = category
            savedMethod = method
          },
          onDelete = null,
          onDismiss = {},
        )
      }
    }

    // Default CTA is "Save Expense" and Category selector is visible
    composeTestRule.onNodeWithText("Save Expense").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tx_category_selector").assertIsDisplayed().performClick()

    // Grid picker shows expense categories, NOT income categories
    composeTestRule.onNodeWithText("Select Expense Category").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tx_category_option_transportation").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tx_category_option_salary").assertDoesNotExist()

    // 1-tap selection picks Transportation and closes picker
    composeTestRule.onNodeWithTag("tx_category_option_transportation").performClick()
    composeTestRule.onNodeWithText("Select Expense Category").assertDoesNotExist()

    // Select GCash from single-row horizontal payment method chips
    composeTestRule.onNodeWithTag("tx_payment_gcash").assertIsDisplayed().performClick()

    // Fill title + amount and tap sticky Save Expense button
    composeTestRule.onNodeWithTag("tx_title_input").performTextInput("Grab Ride")
    composeTestRule.onNodeWithTag("tx_amount_input").performTextInput("240")
    composeTestRule.onNodeWithTag("save_transaction_button").performClick()

    assertEquals(TransactionType.EXPENSE, savedType)
    assertEquals("Transportation", savedCategory)
    assertEquals(PaymentMethod.GCASH, savedMethod)
  }

  @Test
  fun `switching to Income updates sticky CTA and filters category grid to Income only`() {
    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        TransactionEditorSheet(
          existingTransaction = null,
          categories = sampleCategories,
          currencyCode = "PHP",
          onSave = { _, _, _, _, _, _, _, _, _ -> },
          onDelete = null,
          onDismiss = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("tx_type_income").performClick()
    composeTestRule.onNodeWithText("Save Income").assertIsDisplayed()

    composeTestRule.onNodeWithTag("tx_category_selector").performClick()
    composeTestRule.onNodeWithText("Select Income Category").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tx_category_option_salary").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tx_category_option_shopping").assertDoesNotExist()
  }

  @Test
  fun `typography enforces 4-tier fintech weight hierarchy`() {
    // Tier 1: Bold (700)
    assertEquals(FontWeight.Bold, BudgetWiseTypography.displayLarge.fontWeight)
    assertEquals(FontWeight.Bold, BudgetWiseTypography.displaySmall.fontWeight)
    assertEquals(FontWeight.Bold, BudgetWiseTypography.headlineMedium.fontWeight)

    // Tier 2: SemiBold (600)
    assertEquals(FontWeight.SemiBold, BudgetWiseTypography.headlineSmall.fontWeight)
    assertEquals(FontWeight.SemiBold, BudgetWiseTypography.titleLarge.fontWeight)
    assertEquals(FontWeight.SemiBold, BudgetWiseTypography.titleMedium.fontWeight)

    // Tier 3: Medium (500)
    assertEquals(FontWeight.Medium, BudgetWiseTypography.titleSmall.fontWeight)
    assertEquals(FontWeight.Medium, BudgetWiseTypography.labelLarge.fontWeight)

    // Tier 4: Normal (400)
    assertEquals(FontWeight.Normal, BudgetWiseTypography.bodyLarge.fontWeight)
    assertEquals(FontWeight.Normal, BudgetWiseTypography.bodyMedium.fontWeight)
    assertEquals(FontWeight.Normal, BudgetWiseTypography.bodySmall.fontWeight)
  }
}
