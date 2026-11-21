package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.hasTestTag
import com.example.domain.HelpCenterCatalog
import com.example.domain.HelpTopic
import com.example.domain.ThemeMode
import com.example.ui.theme.BudgetWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class HelpCenterScreenTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `catalog filters accurately by topic and multi-word keywords`() {
    val allItems = HelpCenterCatalog.filter(HelpTopic.ALL, "")
    assertEquals(12, allItems.size)

    val pinMatches = HelpCenterCatalog.filter(HelpTopic.ALL, "forgot pin")
    assertTrue(pinMatches.any { it.id == "forgot_pin" })

    val calcMatches = HelpCenterCatalog.filter(HelpTopic.BASICS, "split bill")
    assertEquals(listOf("inline_calculator"), calcMatches.map { it.id })

    val exportMatches = HelpCenterCatalog.filter(HelpTopic.DATA_EXPORT, "pdf")
    assertEquals(listOf("export_backup"), exportMatches.map { it.id })

    val noneMatches = HelpCenterCatalog.filter(HelpTopic.SECURITY, "csv spreadsheet")
    assertTrue(noneMatches.isEmpty())
  }

  @Test
  fun `live search, topic chips, single-open accordion, empty state, and assistant CTA work cleanly`() {
    var assistantOpenedCount = 0
    var backClickedCount = 0

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        HelpCenterScreen(
          onBack = { backClickedCount += 1 },
          onOpenAssistant = { assistantOpenedCount += 1 },
        )
      }
    }

    // 1. Expand first FAQ card -> answer appears
    composeTestRule.onNodeWithTag("help_faq_card_net_cash_flow").performClick()
    composeTestRule.onNodeWithTag("help_faq_answer_net_cash_flow", useUnmergedTree = true).assertIsDisplayed()

    // Expanding second card smoothly collapses the first (single-open accordion)
    composeTestRule.onNodeWithTag("help_faq_card_savings_rate_deficit").performClick()
    composeTestRule.onNodeWithTag("help_faq_answer_savings_rate_deficit", useUnmergedTree = true).assertIsDisplayed()
    composeTestRule.onNodeWithTag("help_faq_answer_net_cash_flow", useUnmergedTree = true).assertDoesNotExist()

    // 2. Topic chip filters list to Security items
    composeTestRule.onNodeWithTag("help_topic_chips_row")
      .performScrollToNode(hasTestTag("help_topic_chip_security"))
    composeTestRule.onNodeWithTag("help_topic_chip_security").performClick()
    composeTestRule.onNodeWithTag("help_faq_card_forgot_pin").assertIsDisplayed()
    composeTestRule.onNodeWithTag("help_faq_card_net_cash_flow").assertDoesNotExist()

    // 3. Live search with no match shows empty state and Reset Search restores items
    composeTestRule.onNodeWithTag("help_search_input").performTextInput("zzzz_unknown_term")
    composeTestRule.onNodeWithTag("help_empty_state").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reset Search").performClick()
    composeTestRule.onNodeWithTag("help_faq_card_net_cash_flow").assertIsDisplayed()

    // 4. Filter by keyword "export" and tap Ask BudgetWise Assistant CTA
    composeTestRule.onNodeWithTag("help_search_input").performTextInput("export")
    composeTestRule.onNodeWithTag("help_faq_card_export_backup").assertIsDisplayed()
    composeTestRule.onNodeWithTag("help_ask_assistant_button").performClick()
    assertEquals(1, assistantOpenedCount)

    // 5. Back button invokes onBack
    composeTestRule.onNodeWithTag("help_center_back_button").performClick()
    assertEquals(1, backClickedCount)
  }
}
