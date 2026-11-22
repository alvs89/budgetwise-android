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
    SpotlightTargetId.HOME_OVERVIEW to Rect(40f, 380f, 1040f, 920f),
    SpotlightTargetId.FAB_ADD_TRANSACTION to Rect(820f, 1780f, 980f, 1940f),
    SpotlightTargetId.ACTIVITY_CONTROLS to Rect(40f, 420f, 1040f, 680f),
    SpotlightTargetId.BUDGETS_OVERVIEW to Rect(40f, 420f, 1040f, 880f),
    SpotlightTargetId.GOALS_OVERVIEW to Rect(40f, 360f, 1040f, 820f),
    SpotlightTargetId.INSIGHTS_OVERVIEW to Rect(40f, 320f, 1040f, 760f),
    SpotlightTargetId.TOP_ACTION_BAR to Rect(660f, 140f, 1040f, 260f),
  )

  @Test
  fun `spotlight tour advances through all 7 steps, supports Back, hides Skip on final step, and completes on Done`() {
    var visible by mutableStateOf(true)
    var stepIndex by mutableIntStateOf(0)
    var completedCount = 0

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        SpotlightTourOverlay(
          visible = visible,
          currentStepIndex = stepIndex,
          isTargetTabActive = true,
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
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("1 of 7")
    composeTestRule.onNodeWithTag("spotlight_back_button").assertDoesNotExist()

    // Step 1 -> Step 2
    composeTestRule.onNodeWithTag("spotlight_next_button").performClick()
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("2 of 7")

    // Step 2 -> Back -> Step 1
    composeTestRule.onNodeWithTag("spotlight_back_button").performClick()
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("1 of 7")

    // Advance through steps 2, 3, 4, 5, 6, 7
    repeat(6) {
      composeTestRule.onNodeWithTag("spotlight_next_button").performClick()
    }
    composeTestRule.onNodeWithTag("spotlight_step_counter").assertTextEquals("7 of 7")
    composeTestRule.onNodeWithTag("spotlight_skip_button").assertDoesNotExist()
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
          isTargetTabActive = true,
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
