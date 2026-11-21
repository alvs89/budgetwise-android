package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.MarkEmailUnread
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.FinancialAlertEntity
import com.example.domain.AlertSeverity
import com.example.domain.FinancialAlertType
import com.example.ui.theme.LocalFinanceColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationCenterSheet(
  alerts: List<FinancialAlertEntity>,
  unreadCount: Int,
  onAlertClick: (FinancialAlertEntity) -> Unit,
  onToggleAlertRead: (FinancialAlertEntity) -> Unit,
  onDismissAlert: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onMarkAllUnread: () -> Unit,
  onClearAll: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  if (dismissIfAppLocked(onDismiss)) return

  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var alertPendingDelete by remember { mutableStateOf<FinancialAlertEntity?>(null) }
  var showClearAllConfirm by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = Modifier
      .statusBarsPadding()
      .testTag("notification_center_sheet"),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxSheetHeight = (maxHeight * 0.86f).coerceAtLeast(260.dp)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = maxSheetHeight)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp),
      ) {
        // Header Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
          ) {
            Icon(
              imageVector = Icons.Rounded.Notifications,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp),
            )
            Text(
              text = "Notifications",
              style = MaterialTheme.typography.headlineSmall,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
          IconButton(
            onClick = {
              onDismiss()
              onOpenSettings()
            },
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("notification_center_settings_button"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Settings,
              contentDescription = "Notification settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        // Concise, Zero-Truncation Action Bar with Dynamic Bulk Read/Unread Toggle
        if (alerts.isNotEmpty()) {
          val hasUnread = unreadCount > 0
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = if (hasUnread) "$unreadCount unread" else "All read",
              style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Medium,
              ),
              color = if (hasUnread) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.testTag("notification_status_summary"),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              TextButton(
                onClick = if (hasUnread) onMarkAllRead else onMarkAllUnread,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag(
                  if (hasUnread) "notification_mark_all_read" else "notification_mark_all_unread"
                ),
              ) {
                Icon(
                  imageVector = if (hasUnread) Icons.Rounded.DoneAll else Icons.Rounded.MarkEmailUnread,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = if (hasUnread) "Mark all read" else "Mark all unread",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(start = 5.dp),
                )
              }
              TextButton(
                onClick = { showClearAllConfirm = true },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.testTag("notification_clear_all"),
              ) {
                Text(
                  text = "Clear all",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(bottom = 12.dp),
          )
        } else {
          Spacer(modifier = Modifier.height(10.dp))
        }

        if (alerts.isEmpty()) {
          Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 28.dp),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(48.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = Icons.Rounded.NotificationsNone,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(24.dp),
                )
              }
              Text(
                text = "You're all caught up",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Budget threshold warnings, savings goal milestones, and reminders will appear here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(alerts, key = { it.id }) { alert ->
              NotificationAlertCard(
                alert = alert,
                onClick = { onAlertClick(alert) },
                onToggleRead = { onToggleAlertRead(alert) },
                onDismiss = { alertPendingDelete = alert },
              )
            }
          }
        }
      }
    }
  }

  alertPendingDelete?.let { target ->
    ConfirmationDialog(
      title = stringResource(R.string.notification_delete_single_title),
      message = stringResource(R.string.notification_delete_single_message, target.title),
      confirmLabel = stringResource(R.string.notification_delete_confirm),
      isDestructive = true,
      onConfirm = {
        onDismissAlert(target.id)
        alertPendingDelete = null
      },
      onDismiss = { alertPendingDelete = null },
    )
  }

  if (showClearAllConfirm && alerts.isNotEmpty()) {
    val count = alerts.size
    ConfirmationDialog(
      title = stringResource(R.string.notification_clear_all_title),
      message = if (count == 1) {
        stringResource(R.string.notification_clear_all_message_one)
      } else {
        stringResource(R.string.notification_clear_all_message_many, count)
      },
      confirmLabel = stringResource(R.string.notification_clear_all_confirm),
      isDestructive = true,
      onConfirm = {
        onClearAll()
        showClearAllConfirm = false
      },
      onDismiss = { showClearAllConfirm = false },
    )
  }
}

@Composable
private fun NotificationAlertCard(
  alert: FinancialAlertEntity,
  onClick: () -> Unit,
  onToggleRead: () -> Unit,
  onDismiss: () -> Unit,
) {
  val financeColors = LocalFinanceColors.current
  val accentColor = when (alert.severity) {
    AlertSeverity.CRITICAL -> financeColors.expense
    AlertSeverity.WARNING -> financeColors.warning
    AlertSeverity.POSITIVE -> financeColors.income
    AlertSeverity.INFO -> MaterialTheme.colorScheme.primary
  }
  val icon = when (alert.type) {
    FinancialAlertType.BUDGET_EXCEEDED -> Icons.Rounded.ErrorOutline
    FinancialAlertType.BUDGET_APPROACHING -> Icons.Rounded.WarningAmber
    FinancialAlertType.GOAL_MILESTONE -> Icons.Rounded.EmojiEvents
    FinancialAlertType.GOAL_DEADLINE -> Icons.Rounded.Schedule
    FinancialAlertType.DAILY_REMINDER -> Icons.Rounded.Notifications
  }
  val timestampLabel = remember(alert.createdAtMillis) {
    SimpleDateFormat("MMM d · h:mm a", Locale.getDefault()).format(Date(alert.createdAtMillis))
  }

  // "One Quiet Unread Signal": surface contrast + typography weight only (no vertical stripes or dots)
  val containerColor = if (alert.isRead) {
    MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
  } else {
    MaterialTheme.colorScheme.surfaceVariant
  }
  val borderColor = if (alert.isRead) {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  } else {
    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
  }

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("notification_alert_card_${alert.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Top Row: Leading Icon + Title + Top-Right Persistent Read/Unread Toggle & Dismiss
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = if (alert.isRead) 0.09f else 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (alert.isRead) accentColor.copy(alpha = 0.65f) else accentColor,
            modifier = Modifier.size(18.dp),
          )
        }

        Text(
          text = alert.title,
          style = MaterialTheme.typography.titleSmall.copy(
            fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.SemiBold,
          ),
          color = if (alert.isRead) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
          } else {
            MaterialTheme.colorScheme.onSurface
          },
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.weight(1f),
        )

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
          IconButton(
            onClick = onToggleRead,
            modifier = Modifier
              .size(40.dp)
              .testTag("notification_toggle_read_${alert.id}"),
          ) {
            Icon(
              imageVector = if (alert.isRead) {
                Icons.Rounded.MarkEmailUnread
              } else {
                Icons.Rounded.MarkEmailRead
              },
              contentDescription = if (alert.isRead) "Mark as unread" else "Mark as read",
              tint = if (alert.isRead) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
              } else {
                MaterialTheme.colorScheme.primary
              },
              modifier = Modifier.size(18.dp),
            )
          }
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .size(40.dp)
              .testTag("notification_dismiss_${alert.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Delete notification",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.78f),
              modifier = Modifier.size(18.dp),
            )
          }
        }
      }

      // Full-Width Message Body & Timestamp Underneath (maximizes reading width)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(end = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
      ) {
        Text(
          text = alert.message,
          style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
          color = if (alert.isRead) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.58f)
          } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
          },
        )
        Text(
          text = timestampLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
            alpha = if (alert.isRead) 0.5f else 0.72f,
          ),
        )
      }
    }
  }
}
