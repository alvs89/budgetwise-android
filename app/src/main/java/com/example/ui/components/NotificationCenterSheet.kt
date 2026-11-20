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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.ErrorOutline
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
  onMarkAlertRead: (Long) -> Unit,
  onDismissAlert: (Long) -> Unit,
  onMarkAllRead: () -> Unit,
  onClearAll: () -> Unit,
  onOpenSettings: () -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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

        // Balanced, Layout-Stable Action Bar (never collapses into an empty left void)
        if (alerts.isNotEmpty()) {
          val statusLabel = if (unreadCount > 0) {
            "$unreadCount unread"
          } else {
            "${alerts.size} ${if (alerts.size == 1) "alert" else "alerts"} · All caught up"
          }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 2.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.labelMedium,
              color = if (unreadCount > 0) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .testTag("notification_status_summary"),
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
              TextButton(
                onClick = onMarkAllRead,
                enabled = unreadCount > 0,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier
                  .alpha(if (unreadCount > 0) 1f else 0.38f)
                  .testTag("notification_mark_all_read"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DoneAll,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = "Mark all read",
                  style = MaterialTheme.typography.labelMedium,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(start = 4.dp),
                )
              }
              TextButton(
                onClick = onClearAll,
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
            modifier = Modifier.padding(bottom = 10.dp),
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
                onMarkRead = { onMarkAlertRead(alert.id) },
                onDismiss = { onDismissAlert(alert.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun NotificationAlertCard(
  alert: FinancialAlertEntity,
  onClick: () -> Unit,
  onMarkRead: () -> Unit,
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

  Surface(
    shape = MaterialTheme.shapes.medium,
    color = if (alert.isRead) {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant
    },
    border = if (!alert.isRead) {
      BorderStroke(1.dp, accentColor.copy(alpha = 0.48f))
    } else {
      BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    },
    modifier = Modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("notification_alert_card_${alert.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      verticalAlignment = Alignment.Top,
    ) {
      // Unread left accent bar + severity icon
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        if (!alert.isRead) {
          Box(
            modifier = Modifier
              .width(3.dp)
              .height(34.dp)
              .clip(RoundedCornerShape(50))
              .background(accentColor),
          )
        }
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(accentColor.copy(alpha = if (alert.isRead) 0.09f else 0.16f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (alert.isRead) accentColor.copy(alpha = 0.72f) else accentColor,
            modifier = Modifier.size(20.dp),
          )
        }
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          if (!alert.isRead) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(accentColor),
            )
          }
          Text(
            text = alert.title,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = if (alert.isRead) FontWeight.Normal else FontWeight.SemiBold,
            ),
            color = if (alert.isRead) {
              MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
            } else {
              MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = alert.message,
          style = MaterialTheme.typography.bodySmall,
          color = if (alert.isRead) {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.76f)
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = timestampLabel,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        )
      }

      // Individual Mark-as-Read (when unread) + Dismiss actions
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
      ) {
        if (!alert.isRead) {
          IconButton(
            onClick = onMarkRead,
            modifier = Modifier
              .size(40.dp)
              .testTag("notification_mark_read_${alert.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Check,
              contentDescription = "Mark as read",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp),
            )
          }
        }
        IconButton(
          onClick = onDismiss,
          modifier = Modifier
            .size(40.dp)
            .testTag("notification_dismiss_${alert.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Dismiss notification",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }
  }
}
