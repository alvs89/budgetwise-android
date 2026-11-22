package com.example.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.example.domain.ThemeMode
import com.example.ui.theme.BudgetWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h891dp-xxhdpi")
class SpotlightTourOverlayTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleBounds = mapOf(
    SpotlightTargetId.FAB_ADD_TRANSACTION to Rect(820f, 1780f, 980f, 1940f),
    SpotlightTargetId.FAB_AI_ASSISTANT to Rect(840f, 1620f, 960f, 1740f),
    SpotlightTargetId.NET_CASH_FLOW_CARD to Rect(40f, 440f, 1040f, 880f),
    SpotlightTargetId.TOP_ACTION_BAR to Rect(660f, 140f, 1040f, 260f),
    SpotlightTargetId.BOTTOM_NAV_BAR to Rect(0f, 2040f, 1080f, 2240f),
  )

  @Test
  fun `spotlight tour advances through all 5 steps, supports Back, and completes on Done`() {
    var visible by mutableStateOf(true)
    var stepIndex by mutableIntStateOf(0)
    var completedCount = 0

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        SpotlightTourOverlay(
          visible = visible,
          currentStepIndex = stepIndex,
          targetBounds = sampleBounds,
          onNext = { stepIndex += 1 },
          onBack = { stepIndex = (stepIndex - 1).coerceAtLeast(0) },
          onSkip = { visible = false },
          onDone = {
            completedCount += 1
            visible = false
          },
        )
      }
    }

    composeTestRule.onNodeWithTag("spotlight_tour_overlay").assertIsDisplayed()
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("1 of 5")
    composeTestRule.onNodeWithTag("spotlight_back_button").assertDoesNotExist()

    // Step 1 -> Step 2
    composeTestRule.onNodeWithTag("spotlight_next_button").performClick()
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("2 of 5")

    // Step 2 -> Back -> Step 1
    composeTestRule.onNodeWithTag("spotlight_back_button").performClick()
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("1 of 5")

    // Advance through steps 2, 3, 4, 5
    repeat(4) {
      composeTestRule.onNodeWithTag("spotlight_next_button").performClick()
    }
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("5 of 5")
    composeTestRule.onNodeWithTag("spotlight_done_button").assertIsDisplayed().performClick()

    assertEquals(1, completedCount)
    assertFalse(visible)
  }

  @Test
  fun `skip button dismisses spotlight tour immediately`() {
    var skippedCount = 0
    var visible by mutableStateOf(true)

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        SpotlightTourOverlay(
          visible = visible,
          currentStepIndex = 0,
          targetBounds = sampleBounds,
          onNext = {},
          onBack = {},
          onSkip = {
            skippedCount += 1
            visible = false
          },
          onDone = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("spotlight_skip_button").assertIsDisplayed().performClick()
    assertEquals(1, skippedCount)
    assertFalse(visible)
  }
}
