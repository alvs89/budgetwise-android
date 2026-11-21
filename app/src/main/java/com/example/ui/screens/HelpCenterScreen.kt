package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.HelpCenterCatalog
import com.example.domain.HelpFaqItem
import com.example.domain.HelpTopic
import com.example.ui.components.EmptyStatePanel

/**
 * Dedicated self-service Help & FAQ screen accessed from Settings.
 *
 * Provides real-time keyword search, single-row topic filter chips, collapsible Material 3
 * accordion cards, and a direct callout to the BudgetWise AI Assistant.
 */
@Composable
fun HelpCenterScreen(
  onBack: () -> Unit,
  onOpenAssistant: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val keyboardController = LocalSoftwareKeyboardController.current

  var searchQuery by rememberSaveable { mutableStateOf("") }
  var selectedTopicName by rememberSaveable { mutableStateOf(HelpTopic.ALL.name) }
  var expandedFaqId by rememberSaveable { mutableStateOf<String?>(null) }

  val selectedTopic = remember(selectedTopicName) {
    HelpTopic.entries.firstOrNull { it.name == selectedTopicName } ?: HelpTopic.ALL
  }

  val filteredItems = remember(selectedTopic, searchQuery) {
    HelpCenterCatalog.filter(topic = selectedTopic, query = searchQuery)
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .navigationBarsPadding(),
  ) {
    // Top Header matching SettingsScreen / ManageCategoriesScreen visual rhythm
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 56.dp)
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      IconButton(
        onClick = onBack,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("help_center_back_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = stringResource(R.string.help_center_back_cd),
          tint = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = stringResource(R.string.help_center_title),
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
          .fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        // 1. Live Search Bar
        item(key = "help_search_bar") {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
              Text(
                text = stringResource(R.string.help_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
            leadingIcon = {
              Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
              {
                IconButton(
                  onClick = { searchQuery = "" },
                  modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("help_search_clear"),
                ) {
                  Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.help_search_clear_cd),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                  )
                }
              }
            } else {
              null
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surface,
              unfocusedContainerColor = MaterialTheme.colorScheme.surface,
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
              onSearch = {
                focusManager.clearFocus()
                keyboardController?.hide()
              },
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("help_search_input"),
          )
        }

        // 2. Topic Filter Chips (Single Horizontal LazyRow, Unified Emerald Active Styling)
        item(key = "help_topic_chips") {
          LazyRow(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("help_topic_chips_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            items(HelpTopic.entries, key = { it.name }) { topic ->
              val isSelected = topic == selectedTopic
              FilterChip(
                selected = isSelected,
                onClick = {
                  selectedTopicName = topic.name
                  if (expandedFaqId != null &&
                    HelpCenterCatalog.filter(topic = topic, query = searchQuery)
                      .none { it.id == expandedFaqId }
                  ) {
                    expandedFaqId = null
                  }
                },
                label = {
                  Text(
                    text = topic.label,
                    style = MaterialTheme.typography.labelMedium.copy(
                      fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    ),
                    maxLines = 1,
                    softWrap = false,
                  )
                },
                shape = RoundedCornerShape(12.dp),
                colors = FilterChipDefaults.filterChipColors(
                  containerColor = MaterialTheme.colorScheme.surface,
                  labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                  enabled = true,
                  selected = isSelected,
                  borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f),
                  selectedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                  borderWidth = 1.dp,
                  selectedBorderWidth = 1.dp,
                ),
                modifier = Modifier
                  .height(36.dp)
                  .testTag("help_topic_chip_${topic.name.lowercase()}"),
              )
            }
          }
        }

        // 3. Collapsible Accordion FAQ Cards (or Empty Search State)
        if (filteredItems.isEmpty()) {
          item(key = "help_empty_state") {
            EmptyStatePanel(
              icon = Icons.Rounded.SearchOff,
              title = stringResource(R.string.help_empty_title),
              message = stringResource(R.string.help_empty_message),
              actionLabel = stringResource(R.string.help_empty_reset_action),
              onAction = {
                searchQuery = ""
                selectedTopicName = HelpTopic.ALL.name
                expandedFaqId = null
              },
              modifier = Modifier
                .padding(top = 4.dp)
                .testTag("help_empty_state"),
            )
          }
        } else {
          items(filteredItems, key = { it.id }) { faq ->
            val isExpanded = expandedFaqId == faq.id
            HelpAccordionCard(
              item = faq,
              showTopicBadge = selectedTopic == HelpTopic.ALL,
              isExpanded = isExpanded,
              onToggle = {
                expandedFaqId = if (isExpanded) null else faq.id
              },
            )
          }
        }

        // 4. "Still have a question?" Assistant Footer Callout
        item(key = "help_assistant_callout") {
          Spacer(modifier = Modifier.height(6.dp))
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("help_assistant_callout_card"),
          ) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
              verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                  )
                }
                Column(
                  modifier = Modifier.weight(1f),
                  verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                  Text(
                    text = stringResource(R.string.help_assistant_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                  )
                  Text(
                    text = stringResource(R.string.help_assistant_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }

              Button(
                onClick = onOpenAssistant,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 48.dp)
                  .testTag("help_ask_assistant_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.AutoAwesome,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = stringResource(R.string.help_assistant_cta),
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  maxLines = 1,
                  softWrap = false,
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
private fun HelpAccordionCard(
  item: HelpFaqItem,
  showTopicBadge: Boolean,
  isExpanded: Boolean,
  onToggle: () -> Unit,
) {
  val chevronRotation by animateFloatAsState(
    targetValue = if (isExpanded) 180f else 0f,
    animationSpec = tween(durationMillis = 200),
    label = "faq_chevron_rotation",
  )

  val borderColor = if (isExpanded) {
    MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onToggle)
      .animateContentSize(
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
      )
      .testTag("help_faq_card_${item.id}"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Column(
          modifier = Modifier.weight(1f),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (showTopicBadge) {
            Text(
              text = item.topic.label.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
              ),
              color = MaterialTheme.colorScheme.primary,
            )
          }
          Text(
            text = item.question,
            style = MaterialTheme.typography.titleSmall.copy(
              fontWeight = FontWeight.SemiBold,
              lineHeight = 21.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
        }
        Icon(
          imageVector = Icons.Rounded.KeyboardArrowDown,
          contentDescription = if (isExpanded) "Collapse answer" else "Expand answer",
          tint = if (isExpanded) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier
            .size(22.dp)
            .rotate(chevronRotation),
        )
      }

      if (isExpanded) {
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(
          color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = item.answer,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontWeight = FontWeight.Normal,
            lineHeight = 22.sp,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.testTag("help_faq_answer_${item.id}"),
        )
      }
    }
  }
}
