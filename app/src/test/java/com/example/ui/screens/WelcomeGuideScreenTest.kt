package com.example.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.example.ui.theme.BudgetWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WelcomeGuideScreenTest {

  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun onboardingFlow_advancesThroughAll5StepsAndSubmitsDemoChoice() {
    var capturedName: String? = null
    var capturedCurrency: String? = null
    var capturedDemoChoice: Boolean? = null

    composeRule.setContent {
      BudgetWiseTheme {
        WelcomeGuideScreen(
          isReplayMode = false,
          initialDisplayName = "Alex",
          initialCurrencyCode = "PHP",
          onComplete = { name, currency, loadDemo ->
            capturedName = name
            capturedCurrency = currency
            capturedDemoChoice = loadDemo
          },
        )
      }
    }

    // Step 1 of 5
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 1 of 5")
    composeRule.onNodeWithTag("welcome_next_button").performClick()

    // Step 2 -> verify Back works too
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 2 of 5")
    composeRule.onNodeWithTag("welcome_back_button").assertIsDisplayed().performClick()
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 1 of 5")

    // Advance to Step 4 (Profile & Currency)
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 4 of 5")

    composeRule.onNodeWithTag("welcome_display_name_input").performTextClearance()
    composeRule.onNodeWithTag("welcome_display_name_input").performTextInput("Jordan")

    // Advance to Step 5 (Choose Starting Data)
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 5 of 5")

    // Pick Explore Demonstration Data and finish
    composeRule.onNodeWithTag("welcome_demo_data_option").performScrollTo().performClick()
    composeRule.onNodeWithTag("welcome_finish_button").performClick()

    assertEquals("Jordan", capturedName)
    assertEquals("PHP", capturedCurrency)
    assertEquals(true, capturedDemoChoice)
  }

  @Test
  fun onboardingFlow_skipInvokesOnSkipImmediately() {
    var skipped = false

    composeRule.setContent {
      BudgetWiseTheme {
        WelcomeGuideScreen(
          isReplayMode = false,
          onSkip = { skipped = true },
        )
      }
    }

    composeRule.onNodeWithTag("welcome_skip_button").performClick()
    assertTrue(skipped)
  }

  @Test
  fun replayMode_shows3TourStepsAndCallsOnFinishReplayWithoutMutatingData() {
    var replayFinished = false
    var completeCalled = false

    composeRule.setContent {
      BudgetWiseTheme {
        WelcomeGuideScreen(
          isReplayMode = true,
          onComplete = { _, _, _ -> completeCalled = true },
          onFinishReplay = { replayFinished = true },
        )
      }
    }

    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 1 of 3")
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 2 of 3")
    composeRule.onNodeWithTag("welcome_next_button").performClick()
    composeRule.onNodeWithTag("welcome_step_indicator").assertTextContains("Step 3 of 3")
    composeRule.onNodeWithTag("welcome_done_button").assertIsDisplayed().performClick()

    assertTrue(replayFinished)
    assertFalse(completeCalled)
  }
}
