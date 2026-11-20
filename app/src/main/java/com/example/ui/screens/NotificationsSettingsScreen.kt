package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AlertThresholdDefaults
import com.example.notifications.BudgetWiseNotificationManager
import com.example.ui.components.StableFormDialog
import com.example.ui.components.budgetWiseSwitchColors
import com.example.ui.viewmodel.BudgetWiseUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSettingsScreen(
  uiState: BudgetWiseUiState,
  onBack: () -> Unit,
  onToggleBudgetAlerts: (Boolean) -> Unit,
  onToggleGoalAlerts: (Boolean) -> Unit,
  onToggleDailyReminder: (Boolean) -> Unit,
  onUpdateBudgetWarningThreshold: (Int) -> Unit,
  onUpdateDailyReminderTime: (hour24: Int, minute: Int) -> Unit,
) {
  BackHandler(onBack = onBack)

  val context = LocalContext.current
  var hasNotificationPermission by remember {
    mutableStateOf(BudgetWiseNotificationManager.hasNotificationPermission(context))
  }
  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { granted ->
    hasNotificationPermission = granted || BudgetWiseNotificationManager.hasNotificationPermission(context)
  }

  fun requestSystemNotificationPermissionIfNeeded() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
      notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
  }

  var showReminderTimePicker by remember { mutableStateOf(false) }

  if (showReminderTimePicker) {
    val timePickerState = rememberTimePickerState(
      initialHour = uiState.preferences.dailyReminderHour,
      initialMinute = uiState.preferences.dailyReminderMinute,
      is24Hour = false,
    )
    StableFormDialog(
      onDismissRequest = { showReminderTimePicker = false },
      title = { Text("Daily Reminder Time") },
      confirmButton = {
        Button(
          onClick = {
            onUpdateDailyReminderTime(timePickerState.hour, timePickerState.minute)
            showReminderTimePicker = false
          },
          modifier = Modifier.testTag("confirm_reminder_time_button"),
        ) {
          Text("Save Time")
        }
      },
      dismissButton = {
        TextButton(onClick = { showReminderTimePicker = false }) {
          Text("Cancel")
        }
      },
    ) {
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
      ) {
        TimePicker(state = timePickerState)
      }
    }
  }

  val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .navigationBarsPadding(),
  ) {
    // Header matching SettingsScreen and ManageCategoriesScreen rhythm
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier.testTag("notifications_settings_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back to Settings",
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = "Alerts & Notifications",
        style = MaterialTheme.typography.titleLarge.copy(
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = (-0.5).sp,
        ),
        color = MaterialTheme.colorScheme.onBackground,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
      )
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

    Box(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth(),
      contentAlignment = Alignment.TopCenter,
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 760.dp)
          .fillMaxSize()
          .testTag("notifications_settings_list"),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
          item {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = Icons.Rounded.NotificationsActive,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp),
                )
                Text(
                  text = "Allow system notifications to see alerts outside the app. In-app Notification Center stays active either way.",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.weight(1f),
                )
                TextButton(
                  onClick = { requestSystemNotificationPermissionIfNeeded() },
                  modifier = Modifier.testTag("settings_allow_notifications_button"),
                ) {
                  Text(
                    text = "Allow",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        // Alert Preferences Card
        item {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("settings_alerts_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            ) {
              // 1. Budget Threshold Alerts
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text("Budget Threshold Alerts", style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = "Alert when spending nears your warning threshold or exceeds 100%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = uiState.preferences.budgetAlertsEnabled,
                  onCheckedChange = { enabled ->
                    if (enabled) requestSystemNotificationPermissionIfNeeded()
                    onToggleBudgetAlerts(enabled)
                  },
                  colors = budgetWiseSwitchColors(),
                  modifier = Modifier.testTag("settings_budget_alerts_switch"),
                )
              }

              if (uiState.preferences.budgetAlertsEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                  text = "Approaching Limit Warning Threshold",
                  style = MaterialTheme.typography.labelMedium,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  AlertThresholdDefaults.WARNING_THRESHOLD_OPTIONS.forEach { pct ->
                    val isSelected = uiState.preferences.budgetWarningThresholdPercent == pct
                    Surface(
                      shape = RoundedCornerShape(10.dp),
                      color = if (isSelected) {
                        MaterialTheme.colorScheme.primaryContainer
                      } else {
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                      },
                      border = BorderStroke(
                        width = if (isSelected) 1.dp else 0.75.dp,
                        color = if (isSelected) {
                          MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                        } else {
                          MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        },
                      ),
                      modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(role = Role.RadioButton) { onUpdateBudgetWarningThreshold(pct) }
                        .semantics { selected = isSelected }
                        .testTag("settings_threshold_chip_$pct"),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Text(
                          text = "$pct%",
                          style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                          ),
                          color = if (isSelected) {
                            MaterialTheme.colorScheme.onPrimaryContainer
                          } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                          },
                          maxLines = 1,
                          softWrap = false,
                        )
                      }
                    }
                  }
                }
                Text(
                  text = "Exceeding 100% of any monthly budget always sends an urgent alert.",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(top = 8.dp),
                )
              }

              HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = dividerColor,
              )

              // 2. Savings Goal Milestones & Deadlines
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text("Goal Milestones & Deadlines", style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = "Celebrate 50%, 75%, and 100% progress and remind 7 days before target date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = uiState.preferences.goalAlertsEnabled,
                  onCheckedChange = { enabled ->
                    if (enabled) requestSystemNotificationPermissionIfNeeded()
                    onToggleGoalAlerts(enabled)
                  },
                  colors = budgetWiseSwitchColors(),
                  modifier = Modifier.testTag("settings_goal_alerts_switch"),
                )
              }

              HorizontalDivider(
                modifier = Modifier.padding(vertical = 14.dp),
                color = dividerColor,
              )

              // 3. Daily Logging Reminder
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                  Text("Daily Logging Reminder", style = MaterialTheme.typography.titleSmall)
                  Text(
                    text = "Gentle evening nudge if no transactions were logged that day",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
                Switch(
                  checked = uiState.preferences.dailyReminderEnabled,
                  onCheckedChange = { enabled ->
                    if (enabled) requestSystemNotificationPermissionIfNeeded()
                    onToggleDailyReminder(enabled)
                  },
                  colors = budgetWiseSwitchColors(),
                  modifier = Modifier.testTag("settings_daily_reminder_switch"),
                )
              }

              if (uiState.preferences.dailyReminderEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                val formattedReminderTime = AlertThresholdDefaults.formatReminderTime(
                  uiState.preferences.dailyReminderHour,
                  uiState.preferences.dailyReminderMinute,
                )
                Surface(
                  shape = RoundedCornerShape(12.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showReminderTimePicker = true }
                    .testTag("settings_reminder_time_button"),
                ) {
                  Row(
                    modifier = Modifier
                      .fillMaxWidth()
                      .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Icon(
                        imageVector = Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                      )
                      Text(
                        text = "Reminder Time",
                        style = MaterialTheme.typography.titleSmall,
                      )
                    }
                    Text(
                      text = formattedReminderTime,
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                    )
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
