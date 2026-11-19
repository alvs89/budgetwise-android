package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.entity.ChatSessionEntity
import com.example.ui.viewmodel.ChatSessionSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ChatHistoryPanel(
  sessions: List<ChatSessionSummary>,
  activeSessionId: Long?,
  isStreaming: Boolean,
  onBack: () -> Unit,
  onNewChat: () -> Unit,
  onSelectSession: (Long) -> Unit,
  onRequestRename: (ChatSessionEntity) -> Unit,
  onRequestDelete: (ChatSessionEntity) -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(modifier = modifier) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("chat_history_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "Back to chat",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(1.dp),
      ) {
        Text(
          text = stringResource(R.string.chat_history_title),
          style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val countSubtitle = when (sessions.size) {
          0 -> "No saved BudgetWise chats"
          1 -> "1 saved BudgetWise chat"
          else -> "${sessions.size} saved BudgetWise chats"
        }
        Text(
          text = countSubtitle,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      IconButton(
        onClick = onNewChat,
        enabled = !isStreaming,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("chat_history_new_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Add,
          contentDescription = stringResource(R.string.chat_new_chat),
          tint = if (!isStreaming) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
          },
        )
      }

      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("chat_history_close_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Close,
          contentDescription = "Close assistant",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

    if (sessions.isEmpty()) {
      Column(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .padding(horizontal = 28.dp, vertical = 36.dp)
          .testTag("chat_history_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Box(
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(28.dp),
          )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "No conversations yet",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Start a chat with BudgetWise Assistant and your conversations will be named and saved here automatically.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(18.dp))
        FilledTonalButton(
          onClick = onNewChat,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("chat_history_empty_new_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(stringResource(R.string.chat_new_chat))
        }
      }
    } else {
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .testTag("chat_history_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        items(
          items = sessions,
          key = { it.session.id },
        ) { summary ->
          ChatHistoryItemCard(
            summary = summary,
            isActive = summary.session.id == activeSessionId,
            actionsEnabled = !isStreaming,
            onSelect = { onSelectSession(summary.session.id) },
            onRename = { onRequestRename(summary.session) },
            onDelete = { onRequestDelete(summary.session) },
          )
        }
      }
    }
  }
}

@Composable
private fun ChatHistoryItemCard(
  summary: ChatSessionSummary,
  isActive: Boolean,
  actionsEnabled: Boolean,
  onSelect: () -> Unit,
  onRename: () -> Unit,
  onDelete: () -> Unit,
) {
  val dateLabel = remember(summary.session.updatedAtMillis) {
    formatChatSessionTimestamp(summary.session.updatedAtMillis)
  }
  val countLabel = if (summary.messageCount == 1) {
    "1 message"
  } else {
    "${summary.messageCount} messages"
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = if (isActive) {
      MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    } else {
      MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    },
    border = BorderStroke(
      width = if (isActive) 1.5.dp else 1.dp,
      color = if (isActive) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
      } else {
        MaterialTheme.colorScheme.outlineVariant
      },
    ),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onSelect)
      .testTag("chat_history_item_${summary.session.id}"),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(
            if (isActive) {
              MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
            } else {
              MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            },
          ),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Rounded.ChatBubbleOutline,
          contentDescription = null,
          tint = if (isActive) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.size(18.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(3.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Text(
            text = summary.session.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
          )
          if (isActive) {
            Surface(
              shape = RoundedCornerShape(6.dp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            ) {
              Text(
                text = "Active",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
              )
            }
          }
        }

        if (summary.lastMessagePreview.isNotBlank()) {
          Text(
            text = summary.lastMessagePreview,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }

        Text(
          text = "$dateLabel · $countLabel",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onRename,
          enabled = actionsEnabled,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("chat_history_rename_${summary.session.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Edit,
            contentDescription = stringResource(R.string.chat_rename_action),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
          )
        }
        IconButton(
          onClick = onDelete,
          enabled = actionsEnabled,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("chat_history_delete_${summary.session.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete chat",
            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@Composable
fun RenameChatDialog(
  session: ChatSessionEntity,
  onConfirm: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  var titleInput by remember(session.id) { mutableStateOf(session.title) }
  val trimmed = titleInput.trim()

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = stringResource(R.string.chat_rename_dialog_title),
        style = MaterialTheme.typography.headlineSmall,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          if (trimmed.isNotEmpty()) {
            onConfirm(trimmed)
          }
        },
        enabled = trimmed.isNotEmpty(),
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("chat_rename_confirm_button"),
      ) {
        Text("Save")
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("chat_rename_cancel_button"),
      ) {
        Text("Cancel")
      }
    },
  ) {
    OutlinedTextField(
      value = titleInput,
      onValueChange = { next ->
        if (next.length <= 60) {
          titleInput = next
        }
      },
      label = { Text(stringResource(R.string.chat_rename_field_label)) },
      placeholder = { Text("e.g., November Food Budget") },
      singleLine = true,
      keyboardOptions = KeyboardOptions(
        capitalization = KeyboardCapitalization.Sentences,
        imeAction = ImeAction.Done,
      ),
      keyboardActions = KeyboardActions(
        onDone = {
          if (trimmed.isNotEmpty()) {
            onConfirm(trimmed)
          }
        },
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("chat_rename_input"),
    )
    Text(
      text = "Your custom title is preserved even as you continue the conversation.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

internal fun formatChatSessionTimestamp(millis: Long): String {
  val now = Calendar.getInstance()
  val target = Calendar.getInstance().apply { timeInMillis = millis }
  val sameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
  val sameDay = sameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
  val pattern = when {
    sameDay -> "h:mm a"
    sameYear -> "MMM d"
    else -> "MMM d, yyyy"
  }
  return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}
