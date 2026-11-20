package com.example.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.data.local.entity.FinancialAlertEntity
import com.example.domain.AlertDestination
import com.example.domain.AlertSeverity
import com.example.domain.FinancialAlertType
import com.example.domain.ThemeMode
import com.example.ui.theme.BudgetWiseTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationCenterSheetTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  private val sampleAlerts = listOf(
    FinancialAlertEntity(
      id = 1L,
      dedupeKey = "budget_exceeded_transport_2026_11",
      type = FinancialAlertType.BUDGET_EXCEEDED,
      severity = AlertSeverity.CRITICAL,
      title = "Transportation Budget Exceeded",
      message = "You've spent ₱2,180.00 of your ₱2,000.00 November limit.",
      targetDestination = AlertDestination.BUDGETS,
      isRead = false,
      createdAtMillis = 1_795_100_000_000L,
    ),
    FinancialAlertEntity(
      id = 2L,
      dedupeKey = "budget_warn_food_2026_11",
      type = FinancialAlertType.BUDGET_APPROACHING,
      severity = AlertSeverity.WARNING,
      title = "Food & Dining Approaching Limit",
      message = "You've used 81.8% of your November budget.",
      targetDestination = AlertDestination.BUDGETS,
      isRead = false,
      createdAtMillis = 1_795_099_000_000L,
    ),
  )

  @Test
  fun `clicking single notification X requires confirmation before deleting`() {
    val dismissedIds = mutableListOf<Long>()

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        NotificationCenterSheet(
          alerts = sampleAlerts,
          unreadCount = 2,
          onAlertClick = {},
          onToggleAlertRead = {},
          onDismissAlert = { dismissedIds.add(it) },
          onMarkAllRead = {},
          onMarkAllUnread = {},
          onClearAll = {},
          onOpenSettings = {},
          onDismiss = {},
        )
      }
    }

    // Tap X on first notification -> dialog opens, nothing deleted yet
    composeTestRule.onNodeWithTag("notification_dismiss_1").performClick()
    composeTestRule.onNodeWithText("Delete Notification?").assertIsDisplayed()
    assertEquals(emptyList<Long>(), dismissedIds)

    // Cancel keeps notification intact
    composeTestRule.onNodeWithTag("cancel_dialog_button").performClick()
    composeTestRule.onNodeWithText("Delete Notification?").assertDoesNotExist()
    assertEquals(emptyList<Long>(), dismissedIds)

    // Tap X again and confirm Delete -> invokes onDismissAlert(1L)
    composeTestRule.onNodeWithTag("notification_dismiss_1").performClick()
    composeTestRule.onNodeWithText("Delete Notification?").assertIsDisplayed()
    composeTestRule.onNodeWithTag("confirm_dialog_button").performClick()
    assertEquals(listOf(1L), dismissedIds)
  }

  @Test
  fun `clicking Clear all requires confirmation before clearing all notifications`() {
    var clearAllInvocations = 0

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        NotificationCenterSheet(
          alerts = sampleAlerts,
          unreadCount = 2,
          onAlertClick = {},
          onToggleAlertRead = {},
          onDismissAlert = {},
          onMarkAllRead = {},
          onMarkAllUnread = {},
          onClearAll = { clearAllInvocations++ },
          onOpenSettings = {},
          onDismiss = {},
        )
      }
    }

    // Tap Clear all -> dialog opens, nothing cleared yet
    composeTestRule.onNodeWithTag("notification_clear_all").performClick()
    composeTestRule.onNodeWithText("Clear All Notifications?").assertIsDisplayed()
    assertEquals(0, clearAllInvocations)

    // Cancel keeps inbox intact
    composeTestRule.onNodeWithTag("cancel_dialog_button").performClick()
    composeTestRule.onNodeWithText("Clear All Notifications?").assertDoesNotExist()
    assertEquals(0, clearAllInvocations)

    // Tap Clear all again and confirm -> invokes onClearAll once
    composeTestRule.onNodeWithTag("notification_clear_all").performClick()
    composeTestRule.onNodeWithTag("confirm_dialog_button").performClick()
    assertEquals(1, clearAllInvocations)
  }

  @Test
  fun `action bar toggles dynamically between Mark all read and Mark all unread`() {
    var markAllUnreadCount = 0
    val toggledIds = mutableListOf<Long>()
    val allReadAlerts = sampleAlerts.map { it.copy(isRead = true) }

    composeTestRule.setContent {
      BudgetWiseTheme(themeMode = ThemeMode.DARK) {
        NotificationCenterSheet(
          alerts = allReadAlerts,
          unreadCount = 0,
          onAlertClick = {},
          onToggleAlertRead = { toggledIds.add(it.id) },
          onDismissAlert = {},
          onMarkAllRead = {},
          onMarkAllUnread = { markAllUnreadCount++ },
          onClearAll = {},
          onOpenSettings = {},
          onDismiss = {},
        )
      }
    }

    composeTestRule.onNodeWithText("All read").assertIsDisplayed()
    composeTestRule.onNodeWithTag("notification_mark_all_unread").assertIsDisplayed().performClick()
    assertEquals(1, markAllUnreadCount)

    composeTestRule.onNodeWithTag("notification_toggle_read_2").performClick()
    assertEquals(listOf(2L), toggledIds)
  }
}
