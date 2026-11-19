package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.BudgetHealthStatus
import com.example.domain.BudgetSummary
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.TransactionType
import com.example.domain.YearMonthKey
import com.example.domain.compactCategoryLabel
import com.example.domain.formatEpochDay
import com.example.ui.theme.LocalFinanceColors
import java.math.BigDecimal

val LocalIsBalanceHidden = staticCompositionLocalOf { false }

@Composable
fun budgetWiseSwitchColors(): SwitchColors = SwitchDefaults.colors(
  checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
  checkedTrackColor = MaterialTheme.colorScheme.primary,
  checkedBorderColor = MaterialTheme.colorScheme.primary,
  uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
  uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
  uncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
  disabledUncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
  disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
  disabledUncheckedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
)

/**
 * Reusable currency text that respects the global privacy masking toggle (`isBalanceHidden`)
 * while preserving exact line-height and layout bounds so cards never shift when toggled.
 */
@Composable
fun MaskedCurrencyText(
  amount: BigDecimal,
  currencyCode: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  isHidden: Boolean = LocalIsBalanceHidden.current,
  showPlusSign: Boolean = false,
  forceMinusSign: Boolean = false,
  omitZeroDecimals: Boolean = false,
  useCompactForLargeAmounts: Boolean = false,
  autoResize: Boolean = false,
  minFontSize: TextUnit = 12.sp,
) {
  val displayText = if (useCompactForLargeAmounts) {
    FinanceCalculator.formatCompactCurrencyOrMasked(
      amount = amount,
      currencyCode = currencyCode,
      isHidden = isHidden,
      showPlusSign = showPlusSign,
      forceMinusSign = forceMinusSign,
      omitZeroDecimalsForStandard = omitZeroDecimals,
    )
  } else {
    FinanceCalculator.formatCurrencyOrMasked(
      amount = amount,
      currencyCode = currencyCode,
      isHidden = isHidden,
      showPlusSign = showPlusSign,
      forceMinusSign = forceMinusSign,
      omitZeroDecimals = omitZeroDecimals,
    )
  }
  if (autoResize) {
    AutoResizeSingleLineText(
      text = displayText,
      style = style,
      color = color,
      minFontSize = minFontSize,
      modifier = modifier,
    )
  } else {
    Text(
      text = displayText,
      style = style,
      color = color,
      maxLines = 1,
      softWrap = false,
      modifier = modifier,
    )
  }
}

/**
 * Single-line text that automatically scales down its font size if the rendered string
 * would overflow the available horizontal space, preventing mid-number line wrapping.
 */
@Composable
fun AutoResizeSingleLineText(
  text: String,
  style: TextStyle,
  color: Color,
  modifier: Modifier = Modifier,
  minFontSize: TextUnit = 12.sp,
) {
  val initialSize = style.fontSize.value
  var currentSizeSp by remember(text, initialSize) { mutableFloatStateOf(initialSize) }
  var readyToDraw by remember(text, initialSize) { mutableFloatStateOf(0f) }

  Text(
    text = text,
    style = style.copy(fontSize = currentSizeSp.sp),
    color = color,
    maxLines = 1,
    softWrap = false,
    overflow = TextOverflow.Clip,
    onTextLayout = { result ->
      if (result.didOverflowWidth && currentSizeSp > minFontSize.value) {
        currentSizeSp = (currentSizeSp * 0.9f).coerceAtLeast(minFontSize.value)
      } else {
        readyToDraw = 1f
      }
    },
    modifier = modifier.drawWithContent {
      if (readyToDraw > 0f || currentSizeSp <= minFontSize.value) {
        drawContent()
      }
    },
  )
}

/**
 * Clean progress bar without Material 3's default trailing stop-indicator dot artifact.
 */
@Composable
fun FintechProgressBar(
  progress: Float,
  color: Color,
  modifier: Modifier = Modifier,
  trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
  height: Dp = 8.dp,
) {
  val clamped = progress.coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = clamped,
    animationSpec = tween(durationMillis = 500),
    label = "fintech_progress",
  )

  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(4.dp))
      .background(trackColor),
  ) {
    if (animatedProgress > 0f) {
      Box(
        modifier = Modifier
          .then(
            if (animatedProgress < 0.015f) {
              Modifier.width(4.dp)
            } else {
              Modifier.fillMaxWidth(animatedProgress)
            }
          )
          .fillMaxHeight()
          .clip(RoundedCornerShape(4.dp))
          .background(color),
      )
    }
  }
}

@Composable
fun FintechCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  fillHeight: Boolean = false,
  verticalArrangement: Arrangement.Vertical = Arrangement.Top,
  content: @Composable ColumnScope.() -> Unit,
) {
  val cardModifier = if (onClick != null) {
    modifier.clip(MaterialTheme.shapes.medium).clickable(onClick = onClick)
  } else {
    modifier
  }
  Surface(
    modifier = cardModifier,
    shape = MaterialTheme.shapes.medium,
    color = containerColor,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier
        .then(if (fillHeight) Modifier.fillMaxHeight() else Modifier)
        .padding(16.dp),
      verticalArrangement = verticalArrangement,
      content = content,
    )
  }
}

@Composable
fun MonthSelectorBar(
  selectedMonth: YearMonthKey,
  onPreviousMonth: () -> Unit,
  onNextMonth: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(50),
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 2.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = onPreviousMonth,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("previous_month_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
          contentDescription = "Previous month",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.weight(1f, fill = false),
      ) {
        Icon(
          imageVector = Icons.Rounded.CalendarMonth,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.primary,
        )
        Text(
          text = selectedMonth.displayLabel(),
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier.testTag("selected_month_label"),
        )
      }
      IconButton(
        onClick = onNextMonth,
        modifier = Modifier
          .minimumInteractiveComponentSize()
          .testTag("next_month_button"),
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
          contentDescription = "Next month",
          tint = MaterialTheme.colorScheme.onSurface,
        )
      }
    }
  }
}

@Composable
fun SectionHeader(
  title: String,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f),
    )
    if (actionLabel != null && onActionClick != null) {
      TextButton(
        onClick = onActionClick,
        modifier = Modifier.minimumInteractiveComponentSize(),
      ) {
        Text(
          text = actionLabel,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

@Composable
fun SwipeToRevealDeleteRow(
  onDelete: () -> Unit,
  modifier: Modifier = Modifier,
  deleteTestTag: String? = null,
  content: @Composable (isRevealed: Boolean, closeReveal: () -> Unit) -> Unit,
) {
  val density = LocalDensity.current
  val revealWidthPx = with(density) { 88.dp.toPx() }
  val offsetX = remember { Animatable(0f) }
  val scope = rememberCoroutineScope()

  fun closeReveal() {
    scope.launch {
      offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
    }
  }

  val revealProgress = (-offsetX.value / revealWidthPx).coerceIn(0f, 1f)
  val isRevealed = offsetX.value <= -8f

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium),
  ) {
    // Revealed red delete button behind the right edge (transparent when closed to avoid corner bleed)
    Box(
      modifier = Modifier
        .matchParentSize()
        .graphicsLayer { alpha = (revealProgress * 4f).coerceIn(0f, 1f) }
        .background(MaterialTheme.colorScheme.error),
      contentAlignment = Alignment.CenterEnd,
    ) {
      Box(
        modifier = Modifier
          .width(88.dp)
          .fillMaxHeight()
          .clickable(enabled = isRevealed) {
            closeReveal()
            onDelete()
          }
          .then(if (deleteTestTag != null) Modifier.testTag(deleteTestTag) else Modifier),
        contentAlignment = Alignment.Center,
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.onError,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = "Delete",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onError,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .draggable(
          orientation = Orientation.Horizontal,
          state = rememberDraggableState { delta ->
            scope.launch {
              val next = (offsetX.value + delta).coerceIn(-revealWidthPx, 0f)
              offsetX.snapTo(next)
            }
          },
          onDragStopped = { velocity ->
            val target = if (offsetX.value < -revealWidthPx * 0.4f || velocity < -450f) {
              -revealWidthPx
            } else {
              0f
            }
            scope.launch {
              offsetX.animateTo(target, spring(stiffness = Spring.StiffnessMediumLow))
            }
          },
        ),
    ) {
      content(isRevealed, ::closeReveal)
    }
  }
}

@Composable
fun TransactionRowItem(
  transaction: TransactionEntity,
  currencyCode: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  categories: List<CategoryEntity> = emptyList(),
) {
  val financeColors = LocalFinanceColors.current
  val isBalanceHidden = LocalIsBalanceHidden.current
  val isIncome = transaction.type == TransactionType.INCOME
  val tint = categoryTint(transaction.category, categories)
  val amountText = if (isBalanceHidden) {
    FinanceCalculator.formatMaskedCurrency(currencyCode)
  } else {
    FinanceCalculator.formatCentavos(
      centavos = transaction.amountCentavos,
      currencyCode = currencyCode,
      showPlusSign = isIncome,
      forceMinusSign = !isIncome,
    )
  }
  val amountColor = if (isIncome) financeColors.income else financeColors.expense
  val compactCategory = compactCategoryLabel(transaction.category)
  val dateText = formatEpochDay(transaction.dateEpochDay, "MMM d")
  val metadataLine = "$compactCategory • $dateText • ${transaction.paymentMethod.label}"

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(MaterialTheme.shapes.medium)
      .clickable(onClick = onClick)
      .testTag("transaction_item_${transaction.id}"),
    shape = MaterialTheme.shapes.medium,
    color = MaterialTheme.colorScheme.surface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(tint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = categoryIcon(transaction.category, categories),
          contentDescription = transaction.category,
          tint = tint,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // Top Line: Title (left, single-line ellipsis) + Amount (right, nowrap)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = transaction.title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          Text(
            text = amountText,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = amountColor,
            maxLines = 1,
            softWrap = false,
          )
        }

        // Bottom Line: Single-line metadata spanning full row content width
        AutoResizeSingleLineText(
          text = metadataLine,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          minFontSize = 10.sp,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@Composable
fun DateFilterBar(
  selectedEpochDay: Long,
  todayEpochDay: Long,
  onSelectEpochDay: (Long) -> Unit,
  onOpenDatePicker: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val yesterdayEpochDay = todayEpochDay - 1L
  val isCustomDate = selectedEpochDay != todayEpochDay && selectedEpochDay != yesterdayEpochDay

  Row(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    FilterChip(
      selected = selectedEpochDay == todayEpochDay,
      onClick = { onSelectEpochDay(todayEpochDay) },
      label = { Text("Today", maxLines = 1, softWrap = false) },
      modifier = Modifier
        .height(40.dp)
        .testTag("date_chip_today"),
    )
    FilterChip(
      selected = selectedEpochDay == yesterdayEpochDay,
      onClick = { onSelectEpochDay(yesterdayEpochDay) },
      label = { Text("Yesterday", maxLines = 1, softWrap = false) },
      modifier = Modifier
        .height(40.dp)
        .testTag("date_chip_yesterday"),
    )
    FilterChip(
      selected = isCustomDate,
      onClick = onOpenDatePicker,
      leadingIcon = {
        Icon(
          imageVector = Icons.Rounded.CalendarMonth,
          contentDescription = null,
          modifier = Modifier.size(18.dp),
        )
      },
      label = {
        Text(
          text = formatEpochDay(selectedEpochDay),
          maxLines = 1,
          softWrap = false,
        )
      },
      modifier = Modifier
        .height(40.dp)
        .testTag("date_chip_custom"),
    )
  }
}

@Composable
fun BudgetProgressCard(
  summary: BudgetSummary,
  currencyCode: String,
  onClick: () -> Unit,
  onDeleteClick: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
  categories: List<CategoryEntity> = emptyList(),
) {
  val financeColors = LocalFinanceColors.current
  val statusColor = when (summary.healthStatus) {
    BudgetHealthStatus.NORMAL -> financeColors.income
    BudgetHealthStatus.APPROACHING -> financeColors.warning
    BudgetHealthStatus.NEAR_LIMIT -> financeColors.nearLimit
    BudgetHealthStatus.EXCEEDED -> financeColors.expense
  }
  val statusBg = statusColor.copy(alpha = 0.14f)
  val rawProgress = (summary.utilizationPercent.toFloat() / 100f).coerceIn(0f, 1f)

  FintechCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("budget_card_${summary.budgetId}"),
    onClick = onClick,
  ) {
    // Row 1: Icon + Category Name + Status Badge + Optional Delete Icon
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      val catTint = categoryTint(summary.category, categories)
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(RoundedCornerShape(10.dp))
          .background(catTint.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = categoryIcon(summary.category, categories),
          contentDescription = summary.category,
          tint = catTint,
          modifier = Modifier.size(18.dp),
        )
      }
      AutoResizeSingleLineText(
        text = compactCategoryLabel(summary.category),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        minFontSize = 11.5.sp,
        modifier = Modifier.weight(1f),
      )
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusBg,
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          if (summary.healthStatus == BudgetHealthStatus.EXCEEDED || summary.healthStatus == BudgetHealthStatus.NEAR_LIMIT) {
            Icon(
              imageVector = Icons.Rounded.WarningAmber,
              contentDescription = null,
              tint = statusColor,
              modifier = Modifier.size(14.dp),
            )
          }
          Text(
            text = "${summary.utilizationPercent.toPlainString()}%",
            style = MaterialTheme.typography.labelSmall,
            color = statusColor,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
      if (onDeleteClick != null) {
        IconButton(
          onClick = onDeleteClick,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("delete_budget_${summary.budgetId}"),
        ) {
          Icon(
            imageVector = Icons.Rounded.DeleteOutline,
            contentDescription = "Delete budget",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Row 2: Full-width decoupled Spent vs Limit metrics
    val isBalanceHidden = LocalIsBalanceHidden.current
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "Spent: ${
          FinanceCalculator.formatCompactCurrencyOrMasked(
            amount = summary.spentAmount,
            currencyCode = currencyCode,
            isHidden = isBalanceHidden,
            omitZeroDecimalsForStandard = true,
          )
        }",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .padding(end = 8.dp),
      )
      Text(
        text = "Limit: ${
          FinanceCalculator.formatCompactCurrencyOrMasked(
            amount = summary.limitAmount,
            currencyCode = currencyCode,
            isHidden = isBalanceHidden,
            omitZeroDecimalsForStandard = true,
          )
        }",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.align(Alignment.CenterVertically),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    FintechProgressBar(
      progress = rawProgress,
      color = statusColor,
      height = 8.dp,
    )
    Spacer(modifier = Modifier.height(8.dp))

    // Row 3: Status label & Remaining/Over amount
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = summary.healthStatus.label,
        style = MaterialTheme.typography.labelSmall,
        color = statusColor,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .padding(end = 8.dp),
      )
      val remainingFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
        amount = summary.remainingAmount.abs(),
        currencyCode = currencyCode,
        isHidden = isBalanceHidden,
        omitZeroDecimalsForStandard = true,
      )
      val remainingLabel = if (summary.remainingAmount >= BigDecimal.ZERO) {
        "$remainingFormatted remaining"
      } else {
        "Over by $remainingFormatted"
      }
      Text(
        text = remainingLabel,
        style = MaterialTheme.typography.labelSmall,
        color = if (summary.remainingAmount < BigDecimal.ZERO) financeColors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.align(Alignment.CenterVertically),
      )
    }
  }
}

@Composable
fun EmptyStatePanel(
  icon: ImageVector,
  title: String,
  message: String,
  actionLabel: String? = null,
  actionIcon: ImageVector? = null,
  onAction: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  FintechCard(modifier = modifier.fillMaxWidth()) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 20.dp, horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.size(28.dp),
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 280.dp),
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.widthIn(max = 280.dp),
      )
      if (actionLabel != null && onAction != null) {
        Spacer(modifier = Modifier.height(18.dp))
        Button(
          onClick = onAction,
          contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
        ) {
          if (actionIcon != null) {
            Icon(
              imageVector = actionIcon,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
          }
          Text(
            text = actionLabel,
            maxLines = 1,
            softWrap = false,
          )
        }
      }
    }
  }
}

@Composable
fun ConfirmationDialog(
  title: String,
  message: String,
  confirmLabel: String,
  isDestructive: Boolean = true,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(title, style = MaterialTheme.typography.titleLarge) },
    text = { Text(message, style = MaterialTheme.typography.bodyMedium) },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = if (isDestructive) {
          ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
          )
        } else {
          ButtonDefaults.buttonColors()
        },
        modifier = Modifier.testTag("confirm_dialog_button"),
      ) {
        Text(confirmLabel)
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        modifier = Modifier.testTag("cancel_dialog_button"),
      ) {
        Text("Cancel")
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyDropdown(
  selectedCurrencyCode: String,
  onSelectCurrency: (String) -> Unit,
  modifier: Modifier = Modifier,
  label: String? = null,
) {
  var expanded by remember { mutableStateOf(false) }
  val selectedOption = remember(selectedCurrencyCode) {
    CurrencyOption.fromCode(selectedCurrencyCode)
  }

  ExposedDropdownMenuBox(
    expanded = expanded,
    onExpandedChange = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
  ) {
    OutlinedTextField(
      value = "${selectedOption.displayName} · ${selectedOption.code}",
      onValueChange = {},
      readOnly = true,
      singleLine = true,
      label = label?.let { text -> { Text(text) } },
      leadingIcon = {
        Surface(
          shape = RoundedCornerShape(8.dp),
          color = MaterialTheme.colorScheme.primaryContainer,
          modifier = Modifier.padding(start = 6.dp),
        ) {
          Text(
            text = selectedOption.symbol,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      },
      trailingIcon = {
        ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
      },
      colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
      modifier = Modifier
        .fillMaxWidth()
        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        .testTag("currency_dropdown_field"),
    )

    ExposedDropdownMenu(
      expanded = expanded,
      onDismissRequest = { expanded = false },
    ) {
      CurrencyOption.entries.forEach { option ->
        val isSelected = option.code == selectedOption.code
        DropdownMenuItem(
          text = {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = if (isSelected) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceVariant
                },
              ) {
                Text(
                  text = option.symbol,
                  style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                )
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = option.displayName,
                  style = MaterialTheme.typography.bodyMedium,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
              Text(
                text = option.code,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          },
          trailingIcon = if (isSelected) {
            {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
              )
            }
          } else {
            null
          },
          onClick = {
            onSelectCurrency(option.code)
            expanded = false
          },
          modifier = Modifier.testTag("currency_option_${option.code}"),
        )
      }
    }
  }
}
