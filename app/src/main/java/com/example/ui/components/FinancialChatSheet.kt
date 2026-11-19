package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.domain.ChatRole
import com.example.domain.FinancialChatContextBuilder
import com.example.ui.viewmodel.BudgetWiseUiState
import com.example.ui.viewmodel.ChatUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialChatSheet(
  uiState: BudgetWiseUiState,
  chatUiState: ChatUiState,
  onSendMessage: (String) -> Unit,
  onEditMessage: (messageId: Long, updatedPrompt: String) -> Unit,
  onDeleteMessage: (messageId: Long) -> Unit,
  onRegenerateMessage: (assistantMessageId: Long) -> Unit,
  onRetryLastMessage: () -> Unit,
  onCancelStreaming: () -> Unit,
  onNewChat: () -> Unit,
  onSelectSession: (Long) -> Unit,
  onRenameSession: (sessionId: Long, newTitle: String) -> Unit,
  onDeleteSession: (sessionId: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var draftMessage by rememberSaveable { mutableStateOf("") }
  var editingMessageId by rememberSaveable { mutableStateOf<Long?>(null) }
  var showHistoryView by rememberSaveable { mutableStateOf(false) }
  var sessionPendingRename by remember { mutableStateOf<ChatSessionEntity?>(null) }
  var sessionPendingDelete by remember { mutableStateOf<ChatSessionEntity?>(null) }
  var messagePendingDelete by remember { mutableStateOf<ChatMessageEntity?>(null) }
  val listState = rememberLazyListState()
  val inputFocusRequester = remember { FocusRequester() }

  val starterPrompts = remember(
    uiState.allTransactions.size,
    uiState.selectedMonth,
    uiState.expenseBreakdown,
    uiState.budgetSummaries,
  ) {
    FinancialChatContextBuilder.buildStarterPrompts(uiState)
  }

  val totalListCount = chatUiState.messages.size +
    (if (chatUiState.isStreaming) 1 else 0) +
    (if (chatUiState.errorMessage != null) 1 else 0)

  LaunchedEffect(chatUiState.activeSession?.id, totalListCount, chatUiState.streamingReply.length) {
    if (!showHistoryView && totalListCount > 0) {
      listState.animateScrollToItem(totalListCount - 1)
    }
  }

  LaunchedEffect(editingMessageId) {
    if (editingMessageId != null) {
      runCatching { inputFocusRequester.requestFocus() }
    }
  }

  BackHandler(enabled = showHistoryView) {
    showHistoryView = false
  }

  sessionPendingRename?.let { target ->
    RenameChatDialog(
      session = target,
      onConfirm = { newTitle ->
        onRenameSession(target.id, newTitle)
        sessionPendingRename = null
      },
      onDismiss = { sessionPendingRename = null },
    )
  }

  sessionPendingDelete?.let { target ->
    ConfirmationDialog(
      title = stringResource(R.string.chat_delete_single_title),
      message = stringResource(R.string.chat_delete_single_message, target.title),
      confirmLabel = "Delete",
      onConfirm = {
        val deletingActive = chatUiState.activeSession?.id == target.id
        if (deletingActive) {
          editingMessageId = null
          draftMessage = ""
        }
        onDeleteSession(target.id)
        sessionPendingDelete = null
      },
      onDismiss = { sessionPendingDelete = null },
    )
  }

  messagePendingDelete?.let { target ->
    ConfirmationDialog(
      title = stringResource(R.string.chat_delete_message_title),
      message = stringResource(R.string.chat_delete_message_body),
      confirmLabel = "Delete",
      onConfirm = {
        if (editingMessageId == target.id) {
          editingMessageId = null
          draftMessage = ""
        }
        onDeleteMessage(target.id)
        messagePendingDelete = null
      },
      onDismiss = { messagePendingDelete = null },
    )
  }

  fun submitDraft() {
    val trimmed = draftMessage.trim()
    if (trimmed.isNotEmpty() && !chatUiState.isStreaming) {
      val activeEditId = editingMessageId
      if (activeEditId != null) {
        onEditMessage(activeEditId, trimmed)
        editingMessageId = null
      } else {
        onSendMessage(trimmed)
      }
      draftMessage = ""
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    dragHandle = null,
    containerColor = MaterialTheme.colorScheme.surface,
    modifier = Modifier.testTag("financial_chat_sheet"),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.90f),
      contentAlignment = Alignment.TopCenter,
    ) {
      if (showHistoryView) {
        ChatHistoryPanel(
          sessions = chatUiState.sessions,
          activeSessionId = chatUiState.activeSession?.id,
          isStreaming = chatUiState.isStreaming,
          onBack = { showHistoryView = false },
          onNewChat = {
            editingMessageId = null
            draftMessage = ""
            onNewChat()
            showHistoryView = false
          },
          onSelectSession = { sessionId ->
            editingMessageId = null
            draftMessage = ""
            onSelectSession(sessionId)
            showHistoryView = false
          },
          onRequestRename = { session -> sessionPendingRename = session },
          onRequestDelete = { session -> sessionPendingDelete = session },
          onDismiss = onDismiss,
          modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .navigationBarsPadding(),
        )
      } else {
        Column(
          modifier = Modifier
            .widthIn(max = 640.dp)
            .fillMaxWidth()
            .fillMaxHeight()
            .navigationBarsPadding()
            .imePadding(),
        ) {
          // Active Chat Header Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = 4.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
          ) {
            IconButton(
              onClick = { showHistoryView = true },
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("chat_history_button"),
            ) {
              Box(contentAlignment = Alignment.TopEnd) {
                Icon(
                  imageVector = Icons.Rounded.History,
                  contentDescription = stringResource(R.string.chat_history_open),
                  tint = MaterialTheme.colorScheme.onSurface,
                )
                if (chatUiState.sessions.isNotEmpty()) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .clip(CircleShape)
                      .background(MaterialTheme.colorScheme.primary),
                  )
                }
              }
            }

            val activeSession = chatUiState.activeSession
            Column(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .then(
                  if (activeSession != null && !chatUiState.isStreaming) {
                    Modifier
                      .clickable { sessionPendingRename = activeSession }
                      .testTag("chat_active_title_rename")
                  } else {
                    Modifier
                  },
                )
                .padding(horizontal = 4.dp, vertical = 2.dp),
              verticalArrangement = Arrangement.spacedBy(1.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
              ) {
                Text(
                  text = activeSession?.title ?: stringResource(R.string.chat_title),
                  style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f, fill = false),
                )
                if (activeSession != null) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.chat_rename_action),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp),
                  )
                }
              }
              Text(
                text = "BudgetWise · ${uiState.allTransactions.size} entries · ${uiState.preferences.preferredCurrency}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }

            val hasConversation = activeSession != null ||
              chatUiState.messages.isNotEmpty() ||
              chatUiState.errorMessage != null

            IconButton(
              onClick = {
                editingMessageId = null
                draftMessage = ""
                onNewChat()
              },
              enabled = hasConversation && !chatUiState.isStreaming,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("chat_new_chat_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = stringResource(R.string.chat_new_chat),
                tint = if (hasConversation && !chatUiState.isStreaming) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
              )
            }

            if (activeSession != null) {
              IconButton(
                onClick = { sessionPendingDelete = activeSession },
                enabled = !chatUiState.isStreaming,
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("chat_delete_active_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.DeleteOutline,
                  contentDescription = "Delete current chat",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }

            IconButton(
              onClick = onDismiss,
              modifier = Modifier
                .minimumInteractiveComponentSize()
                .testTag("chat_close_button"),
            ) {
              Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Close assistant",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          if (uiState.preferences.isBalanceHidden) {
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.VisibilityOff,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = stringResource(R.string.chat_privacy_notice),
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          // Messages Area / Welcome Zero-State
          if (chatUiState.messages.isEmpty() && !chatUiState.isStreaming && chatUiState.errorMessage == null) {
            LazyColumn(
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_empty_state"),
              contentPadding = PaddingValues(18.dp),
              verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              item {
                Surface(
                  shape = MaterialTheme.shapes.large,
                  color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                  border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    Text(
                      text = "Ask me anything about your money",
                      style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                      color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                      text = "I can answer questions using your real BudgetWise transactions, compare spending weeks, check budget limits, and track savings goals.",
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurface,
                    )
                  }
                }
              }

              item {
                Text(
                  text = "Try asking",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(8.dp))
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.fillMaxWidth(),
                ) {
                  starterPrompts.forEachIndexed { index, prompt ->
                    AssistChip(
                      onClick = { onSendMessage(prompt) },
                      label = {
                        Text(
                          text = prompt,
                          style = MaterialTheme.typography.bodySmall,
                        )
                      },
                      colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                      ),
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_starter_chip_$index"),
                    )
                  }
                }
              }

              if (chatUiState.sessions.isNotEmpty()) {
                item {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Text(
                      text = "Recent Conversations",
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                      onClick = { showHistoryView = true },
                      modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("chat_zero_state_history_button"),
                    ) {
                      Text("View All (${chatUiState.sessions.size})")
                    }
                  }
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    chatUiState.sessions.take(3).forEach { summary ->
                      Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(12.dp))
                          .clickable {
                            editingMessageId = null
                            draftMessage = ""
                            onSelectSession(summary.session.id)
                          }
                          .testTag("chat_recent_item_${summary.session.id}"),
                      ) {
                        Row(
                          modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                          verticalAlignment = Alignment.CenterVertically,
                          horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                          Icon(
                            imageVector = Icons.Rounded.ChatBubbleOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                          )
                          Column(modifier = Modifier.weight(1f)) {
                            Text(
                              text = summary.session.title,
                              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                              color = MaterialTheme.colorScheme.onSurface,
                              maxLines = 1,
                              overflow = TextOverflow.Ellipsis,
                            )
                            if (summary.lastMessagePreview.isNotBlank()) {
                              Text(
                                text = summary.lastMessagePreview,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
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
          } else {
            LazyColumn(
              state = listState,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .testTag("chat_messages_list"),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              items(
                items = chatUiState.messages,
                key = { it.id },
              ) { msg ->
                ChatMessageBubble(
                  message = msg,
                  isEditingThis = editingMessageId == msg.id,
                  actionsEnabled = !chatUiState.isStreaming,
                  onEditClick = {
                    editingMessageId = msg.id
                    draftMessage = msg.content
                  },
                  onDeleteClick = {
                    messagePendingDelete = msg
                  },
                  onRegenerateClick = {
                    editingMessageId = null
                    onRegenerateMessage(msg.id)
                  },
                )
              }

              if (chatUiState.isStreaming) {
                item(key = "streaming_assistant_bubble") {
                  StreamingAssistantBubble(text = chatUiState.streamingReply)
                }
              }

              val errorMsg = chatUiState.errorMessage
              if (errorMsg != null) {
                item(key = "chat_error_card") {
                  ChatErrorCard(
                    errorMessage = errorMsg,
                    onRetry = onRetryLastMessage,
                  )
                }
              }
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

          if (editingMessageId != null) {
            Surface(
              color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_editing_banner"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(start = 16.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                  Text(
                    text = stringResource(R.string.chat_editing_banner),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
                IconButton(
                  onClick = {
                    editingMessageId = null
                    draftMessage = ""
                  },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("chat_cancel_edit_button"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.chat_cancel_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            }
          }

          // Input Composer Bar
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            OutlinedTextField(
              value = draftMessage,
              onValueChange = { draftMessage = it },
              placeholder = {
                Text(
                  text = stringResource(R.string.chat_input_placeholder),
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              },
              maxLines = 4,
              enabled = !chatUiState.isStreaming,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Send,
              ),
              keyboardActions = KeyboardActions(
                onSend = { submitDraft() },
              ),
              shape = RoundedCornerShape(24.dp),
              modifier = Modifier
                .weight(1f)
                .focusRequester(inputFocusRequester)
                .testTag("chat_input_field"),
            )

            if (chatUiState.isStreaming) {
              FilledIconButton(
                onClick = onCancelStreaming,
                colors = IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.errorContainer,
                  contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                  .size(50.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("chat_stop_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Stop,
                  contentDescription = stringResource(R.string.chat_stop),
                )
              }
            } else {
              FilledIconButton(
                onClick = { submitDraft() },
                enabled = draftMessage.isNotBlank(),
                colors = IconButtonDefaults.filledIconButtonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier
                  .size(50.dp)
                  .minimumInteractiveComponentSize()
                  .testTag("chat_send_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.Send,
                  contentDescription = stringResource(R.string.chat_send),
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ChatMessageBubble(
  message: ChatMessageEntity,
  isEditingThis: Boolean,
  actionsEnabled: Boolean,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
  onRegenerateClick: () -> Unit,
) {
  val context = LocalContext.current
  val isUser = message.role == ChatRole.USER
  var copiedRecently by remember(message.id) { mutableStateOf(false) }
  LaunchedEffect(copiedRecently) {
    if (copiedRecently) {
      delay(1_800L)
      copiedRecently = false
    }
  }
  val timeText = remember(message.createdAtMillis) {
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(message.createdAtMillis))
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = if (isUser) 18.dp else 4.dp,
        bottomEnd = if (isUser) 4.dp else 18.dp,
      ),
      color = if (isUser) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
      },
      border = when {
        isEditingThis -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f))
        isUser -> null
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
      },
      modifier = Modifier
        .widthIn(max = 520.dp)
        .animateContentSize(),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        if (isUser) {
          Text(
            text = message.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimary,
          )
        } else {
          MarkdownMessageText(
            markdown = message.content,
            baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
            boldColor = MaterialTheme.colorScheme.onSurface,
            accentColor = MaterialTheme.colorScheme.primary,
            baseStyle = MaterialTheme.typography.bodyMedium,
          )
        }
        Text(
          text = timeText,
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
          color = if (isUser) {
            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier.align(Alignment.End),
        )
      }
    }

    // Compact icon-only actions directly beneath each message bubble
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
      Row(
        modifier = Modifier.padding(top = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        IconButton(
          onClick = {
            copyMessageToClipboard(context, message.content)
            copiedRecently = true
          },
          modifier = Modifier
            .size(32.dp)
            .testTag("chat_copy_message_${message.id}"),
        ) {
          Icon(
            imageVector = if (copiedRecently) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
            contentDescription = stringResource(
              if (copiedRecently) R.string.chat_copied_message else R.string.chat_copy_message,
            ),
            tint = if (copiedRecently) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(16.dp),
          )
        }

        if (isUser) {
          IconButton(
            onClick = onEditClick,
            enabled = actionsEnabled,
            modifier = Modifier
              .size(32.dp)
              .testTag("chat_edit_message_${message.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Edit,
              contentDescription = stringResource(R.string.chat_edit_message),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp),
            )
          }
        } else {
          IconButton(
            onClick = onRegenerateClick,
            enabled = actionsEnabled,
            modifier = Modifier
              .size(32.dp)
              .testTag("chat_regenerate_message_${message.id}"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Refresh,
              contentDescription = stringResource(R.string.chat_regenerate),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(17.dp),
            )
          }
        }

        IconButton(
          onClick = onDeleteClick,
          enabled = actionsEnabled,
          modifier = Modifier
            .size(32.dp)
            .testTag("chat_delete_message_${message.id}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = stringResource(R.string.chat_delete_message),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(17.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StreamingAssistantBubble(text: String) {
  var waitingSeconds by remember { mutableStateOf(0) }
  LaunchedEffect(text.isBlank()) {
    if (text.isBlank()) {
      waitingSeconds = 0
      while (true) {
        delay(1_000L)
        waitingSeconds += 1
      }
    }
  }

  val statusLabel = when {
    waitingSeconds >= 16 -> "Still waiting — switching to backup model…"
    waitingSeconds >= 7 -> "Analyzing your transactions…"
    else -> "Checking your financial records…"
  }

  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.Start,
  ) {
    Surface(
      shape = RoundedCornerShape(
        topStart = 18.dp,
        topEnd = 18.dp,
        bottomStart = 4.dp,
        bottomEnd = 18.dp,
      ),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
      modifier = Modifier
        .widthIn(max = 520.dp)
        .animateContentSize()
        .testTag("chat_streaming_bubble"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (text.isBlank()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(
              text = statusLabel,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        } else {
          MarkdownMessageText(
            markdown = text,
            baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
            boldColor = MaterialTheme.colorScheme.onSurface,
            accentColor = MaterialTheme.colorScheme.primary,
            baseStyle = MaterialTheme.typography.bodyMedium,
          )
        }
      }
    }
  }
}

@Composable
private fun ChatErrorCard(
  errorMessage: String,
  onRetry: () -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.errorContainer,
    modifier = Modifier
      .fillMaxWidth()
      .testTag("chat_error_card"),
  ) {
    Column(
      modifier = Modifier.padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.ErrorOutline,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = errorMessage,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onErrorContainer,
          modifier = Modifier.weight(1f),
        )
      }
      FilledTonalButton(
        onClick = onRetry,
        modifier = Modifier
          .align(Alignment.End)
          .minimumInteractiveComponentSize()
          .testTag("chat_retry_button"),
      ) {
        Icon(
          imageVector = Icons.Rounded.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(stringResource(R.string.chat_retry))
      }
    }
  }
}

/** Copies the raw message text—including original Markdown syntax—to the system clipboard. */
internal fun copyMessageToClipboard(context: Context, rawContent: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
  clipboard.setPrimaryClip(ClipData.newPlainText("BudgetWise Chat Message", rawContent))
}

