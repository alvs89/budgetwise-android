package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LayersClear
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.CurrencyOption
import com.example.ui.components.CurrencyDropdown
import kotlinx.coroutines.launch

/**
 * Multi-step interactive Welcome Guide for first-time BudgetWise accounts and Help & FAQ replays.
 *
 * - When [isReplayMode] is `false`, renders all 5 onboarding steps (Feature Tour + Profile &
 *   Currency setup + Start Fresh vs. Explore Demonstration Data choice).
 * - When [isReplayMode] is `true`, renders the 3-step feature tour read-only with a `Done` CTA
 *   so returning users can revisit the guide without altering their profile or ledger.
 */
@Composable
fun WelcomeGuideScreen(
  isReplayMode: Boolean = false,
  initialDisplayName: String = "Alex",
  initialCurrencyCode: String = CurrencyOption.PHP.code,
  isSubmitting: Boolean = false,
  errorMessage: String? = null,
  onComplete: (displayName: String, currencyCode: String, loadDemoData: Boolean) -> Unit = { _, _, _ -> },
  onSkip: () -> Unit = {},
  onFinishReplay: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val totalSteps = if (isReplayMode) 3 else 5
  val pagerState = rememberPagerState(initialPage = 0, pageCount = { totalSteps })
  val coroutineScope = rememberCoroutineScope()

  var displayNameInput by rememberSaveable(initialDisplayName) {
    mutableStateOf(initialDisplayName.trim().ifBlank { "Alex" })
  }
  var selectedCurrency by rememberSaveable(initialCurrencyCode) {
    mutableStateOf(CurrencyOption.fromCode(initialCurrencyCode).code)
  }
  var loadDemoData by rememberSaveable { mutableStateOf(false) }

  BackHandler(enabled = true) {
    when {
      isSubmitting -> Unit
      pagerState.currentPage > 0 -> {
        coroutineScope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
      }
      isReplayMode -> onFinishReplay()
    }
  }

  val isLastPage = pagerState.currentPage == totalSteps - 1

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.TopCenter,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 560.dp)
        .fillMaxSize(),
    ) {
      // Top bar: Step pill + Skip action
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(999.dp),
          color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        ) {
          Text(
            text = stringResource(
              R.string.welcome_step_counter,
              pagerState.currentPage + 1,
              totalSteps,
            ),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
              .padding(horizontal = 12.dp, vertical = 6.dp)
              .testTag("welcome_step_indicator"),
          )
        }

        TextButton(
          onClick = {
            if (isReplayMode) onFinishReplay() else onSkip()
          },
          enabled = !isSubmitting,
          modifier = Modifier
            .heightIn(min = 48.dp)
            .testTag("welcome_skip_button"),
        ) {
          Text(
            text = stringResource(R.string.welcome_skip),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Main swipeable pager
      HorizontalPager(
        state = pagerState,
        userScrollEnabled = !isSubmitting,
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
      ) { page ->
        when (page) {
          0 -> TourStepPage(
            heroIcon = Icons.Rounded.CloudSync,
            title = stringResource(R.string.welcome_step1_title),
            body = stringResource(R.string.welcome_step1_body),
            highlights = listOf(
              TourHighlight(
                icon = Icons.Rounded.CloudSync,
                title = "Synced Across Devices",
                subtitle = "Sign in on any phone to access your latest ledger instantly.",
              ),
              TourHighlight(
                icon = Icons.Rounded.Tune,
                title = "Offline-Ready & Private",
                subtitle = "Log expenses without signal; your PIN lock stays on your phone.",
              ),
            ),
            testTag = "welcome_page_0",
          )

          1 -> TourStepPage(
            heroIcon = Icons.Rounded.ReceiptLong,
            title = stringResource(R.string.welcome_step2_title),
            body = stringResource(R.string.welcome_step2_body),
            highlights = listOf(
              TourHighlight(
                icon = Icons.Rounded.Calculate,
                title = "Built-In Amount Calculator",
                subtitle = "Split bills (1850 ÷ 4) or total receipts right inside the Amount field.",
              ),
              TourHighlight(
                icon = Icons.Rounded.ReceiptLong,
                title = "Cash, E-Wallets & Cards",
                subtitle = "Track Cash, GCash, Maya, debit/credit cards, and 30+ categories.",
              ),
            ),
            testTag = "welcome_page_1",
          )

          2 -> TourStepPage(
            heroIcon = Icons.Rounded.Insights,
            title = stringResource(R.string.welcome_step3_title),
            body = stringResource(R.string.welcome_step3_body),
            highlights = listOf(
              TourHighlight(
                icon = Icons.Rounded.PieChart,
                title = "Monthly Budgets & Alerts",
                subtitle = "Get early heads-ups when category spending approaches your limit.",
              ),
              TourHighlight(
                icon = Icons.Rounded.Savings,
                title = "Independent Savings Goals",
                subtitle = "Track milestones without double-counting monthly expenses.",
              ),
              TourHighlight(
                icon = Icons.Rounded.AutoAwesome,
                title = "Visual Charts & AI Assistant",
                subtitle = "Review spending trends or ask questions about your budget anytime.",
              ),
            ),
            testTag = "welcome_page_2",
          )

          3 -> ProfileSetupStepPage(
            displayName = displayNameInput,
            onDisplayNameChange = { displayNameInput = it },
            selectedCurrencyCode = selectedCurrency,
            onSelectCurrency = { selectedCurrency = it },
          )

          else -> StartingDataStepPage(
            loadDemoData = loadDemoData,
            onSelectDemoData = { loadDemoData = it },
            errorMessage = errorMessage,
          )
        }
      }

      // Bottom navigation controls & animated emerald page dots
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          repeat(totalSteps) { index ->
            val isSelected = pagerState.currentPage == index
            val dotWidth by animateDpAsState(
              targetValue = if (isSelected) 24.dp else 8.dp,
              animationSpec = tween(durationMillis = 220),
              label = "welcome_dot_width",
            )
            val dotColor by animateColorAsState(
              targetValue = if (isSelected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.outlineVariant
              },
              animationSpec = tween(durationMillis = 220),
              label = "welcome_dot_color",
            )
            Box(
              modifier = Modifier
                .height(8.dp)
                .width(dotWidth)
                .clip(CircleShape)
                .background(dotColor),
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          if (pagerState.currentPage > 0) {
            OutlinedButton(
              onClick = {
                coroutineScope.launch {
                  pagerState.animateScrollToPage(pagerState.currentPage - 1)
                }
              },
              enabled = !isSubmitting,
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
              modifier = Modifier
                .weight(0.42f)
                .heightIn(min = 52.dp)
                .testTag("welcome_back_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
            }
          }

          val primaryTag = when {
            !isLastPage -> "welcome_next_button"
            isReplayMode -> "welcome_done_button"
            else -> "welcome_finish_button"
          }

          Button(
            onClick = {
              when {
                !isLastPage -> {
                  coroutineScope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                  }
                }
                isReplayMode -> onFinishReplay()
                else -> onComplete(
                  displayNameInput.trim().ifBlank { "Alex" },
                  selectedCurrency,
                  loadDemoData,
                )
              }
            },
            enabled = !isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            modifier = Modifier
              .weight(if (pagerState.currentPage > 0) 0.58f else 1f)
              .heightIn(min = 52.dp)
              .testTag(primaryTag),
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(
                strokeWidth = 2.5.dp,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp),
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = stringResource(R.string.welcome_saving),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
            } else {
              val ctaText = when {
                !isLastPage -> stringResource(R.string.welcome_next)
                isReplayMode -> stringResource(R.string.welcome_done)
                else -> stringResource(R.string.welcome_get_started)
              }
              Text(
                text = ctaText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              )
              if (!isLastPage) {
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
            }
          }
        }
      }
    }
  }
}

private data class TourHighlight(
  val icon: ImageVector,
  val title: String,
  val subtitle: String,
)

@Composable
private fun TourStepPage(
  heroIcon: ImageVector,
  title: String,
  body: String,
  highlights: List<TourHighlight>,
  testTag: String,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 12.dp)
      .testTag(testTag),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .size(84.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(
          Brush.linearGradient(
            colors = listOf(
              MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
              MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
            )
          )
        ),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = heroIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(42.dp),
      )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
      ),
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = body,
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      highlights.forEach { item ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = item.icon,
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
                text = item.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
              )
              Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ProfileSetupStepPage(
  displayName: String,
  onDisplayNameChange: (String) -> Unit,
  selectedCurrencyCode: String,
  onSelectCurrency: (String) -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 12.dp)
      .testTag("welcome_page_3"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp),
      )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
      text = stringResource(R.string.welcome_step4_title),
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
      ),
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = stringResource(R.string.welcome_step4_body),
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Surface(
      shape = RoundedCornerShape(18.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = stringResource(R.string.welcome_step4_name_label),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          OutlinedTextField(
            value = displayName,
            onValueChange = { if (it.length <= 60) onDisplayNameChange(it) },
            placeholder = { Text(stringResource(R.string.welcome_step4_name_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("welcome_display_name_input"),
          )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = stringResource(R.string.welcome_step4_currency_label),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Box(modifier = Modifier.testTag("welcome_currency_picker")) {
            CurrencyDropdown(
              selectedCurrencyCode = selectedCurrencyCode,
              onSelectCurrency = onSelectCurrency,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun StartingDataStepPage(
  loadDemoData: Boolean,
  onSelectDemoData: (Boolean) -> Unit,
  errorMessage: String?,
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp, vertical = 12.dp)
      .testTag("welcome_page_4"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Spacer(modifier = Modifier.height(12.dp))

    Box(
      modifier = Modifier
        .size(80.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Explore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(40.dp),
      )
    }

    Spacer(modifier = Modifier.height(22.dp))

    Text(
      text = stringResource(R.string.welcome_step5_title),
      style = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
      ),
      color = MaterialTheme.colorScheme.onBackground,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
      text = stringResource(R.string.welcome_step5_body),
      style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    Column(
      modifier = Modifier.fillMaxWidth(),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      StartingOptionCard(
        icon = Icons.Rounded.LayersClear,
        title = stringResource(R.string.welcome_option_fresh_title),
        description = stringResource(R.string.welcome_option_fresh_desc),
        selected = !loadDemoData,
        onClick = { onSelectDemoData(false) },
        testTag = "welcome_start_fresh_option",
      )

      StartingOptionCard(
        icon = Icons.Rounded.AutoAwesome,
        title = stringResource(R.string.welcome_option_demo_title),
        description = stringResource(R.string.welcome_option_demo_desc),
        selected = loadDemoData,
        onClick = { onSelectDemoData(true) },
        testTag = "welcome_demo_data_option",
      )

      if (!errorMessage.isNullOrBlank()) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("welcome_error_banner"),
        ) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(14.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun StartingOptionCard(
  icon: ImageVector,
  title: String,
  description: String,
  selected: Boolean,
  onClick: () -> Unit,
  testTag: String,
) {
  val borderColor = if (selected) {
    MaterialTheme.colorScheme.primary
  } else {
    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.75f)
  }
  val containerColor = if (selected) {
    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
  } else {
    MaterialTheme.colorScheme.surface
  }

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = containerColor,
    border = BorderStroke(if (selected) 1.5.dp else 1.dp, borderColor),
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
      }
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
          text = description,
          style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Icon(
        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
        contentDescription = if (selected) "Selected" else "Not selected",
        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp),
      )
    }
  }
}
