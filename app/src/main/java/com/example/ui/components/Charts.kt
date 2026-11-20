package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CategoryBreakdown
import com.example.domain.DailySpendingPoint
import com.example.domain.FinanceCalculator
import com.example.domain.MonthlyCashFlowPoint
import com.example.domain.MonthlySpendingPoint
import com.example.domain.YearMonthKey
import com.example.domain.compactCategoryLabel
import com.example.ui.theme.LocalFinanceColors
import java.math.BigDecimal
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TopExpenseCategoriesOverview(
  breakdown: List<CategoryBreakdown>,
  currencyCode: String,
  modifier: Modifier = Modifier,
  emptyMessage: String = "No expenses recorded for this month yet.",
) {
  if (breakdown.isEmpty()) {
    Text(
      text = emptyMessage,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }

  val topCategories = breakdown.take(4)
  val isBalanceHidden = LocalIsBalanceHidden.current
  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
    topCategories.forEach { item ->
      val fraction = (item.percentage.toFloat() / 100f).coerceIn(0f, 1f)
      val formattedAmt = FinanceCalculator.formatCompactCurrencyOrMasked(
        amount = item.amount,
        currencyCode = currencyCode,
        isHidden = isBalanceHidden,
        omitZeroDecimalsForStandard = true,
      )
      CategoryShareRow(
        category = compactCategoryLabel(item.category),
        amountAndShare = "$formattedAmt (${item.percentage.toPlainString()}%)",
        fraction = fraction,
        barColor = accessibleCategoryColor(Color(item.colorHex)),
      )
    }
  }
}

@Composable
private fun CategoryShareRow(
  category: String,
  amountAndShare: String,
  fraction: Float,
  barColor: Color,
) {
  val animatedFraction by animateFloatAsState(
    targetValue = fraction,
    animationSpec = tween(500),
    label = "category_share",
  )
  Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier.weight(1f),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(barColor),
        )
        Text(
          text = category,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Text(
        text = amountAndShare,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        softWrap = false,
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(7.dp)
        .clip(RoundedCornerShape(4.dp))
        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)),
    ) {
      if (fraction > 0f) {
        val barShape = RoundedCornerShape(4.dp)
        Box(
          modifier = Modifier
            .then(
              if (fraction < 0.01f) {
                Modifier.width(4.dp)
              } else {
                Modifier.fillMaxWidth(animatedFraction.coerceAtLeast(0.02f))
              }
            )
            .height(7.dp)
            .clip(barShape)
            .background(barColor),
        )
      }
    }
  }
}

@Composable
fun MonthlyIncomeExpenseChart(
  series: List<MonthlyCashFlowPoint>,
  currencyCode: String,
  modifier: Modifier = Modifier,
) {
  val financeColors = LocalFinanceColors.current
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  var selectedIndex by remember(series) { mutableIntStateOf(series.lastIndex.coerceAtLeast(0)) }

  if (series.isEmpty() || series.all { it.income <= BigDecimal.ZERO && it.expenses <= BigDecimal.ZERO }) {
    Text(
      text = "No multi-month cash flow records yet.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }

  val maxAmount = series.maxOf { maxOf(it.income.toFloat(), it.expenses.toFloat()) }.coerceAtLeast(1f)
  val selectedPoint = series.getOrNull(selectedIndex)

  Column(modifier = modifier.fillMaxWidth()) {
    if (selectedPoint != null) {
      val isHidden = LocalIsBalanceHidden.current
      val inText = FinanceCalculator.formatCurrencyOrMasked(
        amount = selectedPoint.income,
        currencyCode = currencyCode,
        isHidden = isHidden,
        omitZeroDecimals = true,
      )
      val outText = FinanceCalculator.formatCurrencyOrMasked(
        amount = selectedPoint.expenses,
        currencyCode = currencyCode,
        isHidden = isHidden,
        omitZeroDecimals = true,
      )
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = selectedPoint.yearMonth.displayLabel(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = financeColors.incomeContainer,
              modifier = Modifier.weight(1f),
            ) {
              AutoResizeSingleLineText(
                text = "In: $inText",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = financeColors.income,
                minFontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = financeColors.expenseContainer,
              modifier = Modifier.weight(1f),
            ) {
              AutoResizeSingleLineText(
                text = "Out: $outText",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = financeColors.expense,
                minFontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }
      }
      Spacer(modifier = Modifier.height(12.dp))
    }

    Canvas(
      modifier = Modifier
        .fillMaxWidth()
        .height(180.dp)
        .pointerInput(series) {
          detectTapGestures { offset ->
            val groupWidth = size.width.toFloat() / series.size.coerceAtLeast(1)
            val tapped = (offset.x / groupWidth).toInt().coerceIn(0, series.lastIndex)
            selectedIndex = tapped
          }
        },
    ) {
      val groupCount = series.size
      val groupWidth = size.width / groupCount
      val barWidth = (groupWidth * 0.28f).coerceAtMost(28.dp.toPx())
      val gap = 4.dp.toPx()
      val bottomPad = 8.dp.toPx()
      val chartHeight = size.height - bottomPad

      // 3 subtle horizontal gridlines
      for (i in 0..2) {
        val y = chartHeight * (i / 2f)
        drawLine(
          color = gridColor,
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1.dp.toPx(),
        )
      }

      series.forEachIndexed { index, point ->
        val centerX = groupWidth * index + groupWidth / 2f
        val incomeHeight = (point.income.toFloat() / maxAmount) * (chartHeight - 8.dp.toPx())
        val expenseHeight = (point.expenses.toFloat() / maxAmount) * (chartHeight - 8.dp.toPx())
        val isSelected = index == selectedIndex

        if (isSelected) {
          drawRoundRect(
            color = gridColor.copy(alpha = 0.45f),
            topLeft = Offset(groupWidth * index + 4.dp.toPx(), 0f),
            size = Size(groupWidth - 8.dp.toPx(), chartHeight),
            cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
          )
        }

        drawRoundRect(
          color = financeColors.income.copy(alpha = if (isSelected) 1f else 0.72f),
          topLeft = Offset(centerX - barWidth - gap / 2f, chartHeight - incomeHeight),
          size = Size(barWidth, incomeHeight.coerceAtLeast(2.dp.toPx())),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )

        drawRoundRect(
          color = financeColors.expense.copy(alpha = if (isSelected) 1f else 0.72f),
          topLeft = Offset(centerX + gap / 2f, chartHeight - expenseHeight),
          size = Size(barWidth, expenseHeight.coerceAtLeast(2.dp.toPx())),
          cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
      series.forEachIndexed { index, point ->
        Text(
          text = point.yearMonth.shortLabel(),
          style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Medium
          ),
          color = if (index == selectedIndex) {
            MaterialTheme.colorScheme.primary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { selectedIndex = index }
            .padding(horizontal = 6.dp, vertical = 4.dp),
        )
      }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
      LegendChip(color = financeColors.income, label = "Income")
      LegendChip(color = financeColors.expense, label = "Expenses")
    }
  }
}

@Composable
private fun LegendChip(color: Color, label: String) {
  Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color),
    )
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

private val DistinctDonutPaletteHex = listOf(
  0xFF10B981L, // Emerald
  0xFF06B6D4L, // Cyan
  0xFF8B5CF6L, // Violet
  0xFFF59E0BL, // Amber
  0xFF3B82F6L, // Sky Blue
  0xFFEC4899L, // Rose Pink
  0xFFF97316L, // Coral Orange
  0xFFF43F5EL, // Crimson Red
  0xFF84CC16L, // Lime
  0xFF6366F1L, // Indigo
)

private fun hexHueDegrees(colorHex: Long): Float {
  val r = ((colorHex shr 16) and 0xFFL).toFloat() / 255f
  val g = ((colorHex shr 8) and 0xFFL).toFloat() / 255f
  val b = (colorHex and 0xFFL).toFloat() / 255f
  val max = maxOf(r, g, b)
  val min = minOf(r, g, b)
  val delta = max - min
  if (delta < 0.0001f) return -1f // Achromatic / neutral gray
  val raw = when (max) {
    r -> ((g - b) / delta) % 6f
    g -> ((b - r) / delta) + 2f
    else -> ((r - g) / delta) + 4f
  } * 60f
  return if (raw < 0f) raw + 360f else raw
}

private fun circularHueDistance(h1: Float, h2: Float): Float {
  if (h1 < 0f && h2 < 0f) return 0f
  if (h1 < 0f || h2 < 0f) return 90f
  val diff = abs(h1 - h2)
  return minOf(diff, 360f - diff)
}

/**
 * Preserves each category's assigned/custom color unless two visible slices in the donut share
 * an identical or overly similar hue (including wrap-around adjacency between the last and first
 * slice at 12 o'clock), substituting the farthest available hue from [DistinctDonutPaletteHex].
 */
internal fun resolveDistinctDonutColorHexes(
  breakdown: List<CategoryBreakdown>,
): List<Long> {
  if (breakdown.isEmpty()) return emptyList()
  val minHueSeparation = 24f
  val assigned = ArrayList<Long>(breakdown.size)

  breakdown.forEachIndexed { idx, slice ->
    val preferred = slice.colorHex
    val preferredHue = hexHueDegrees(preferred)
    val isLastWrapping = idx == breakdown.lastIndex && breakdown.size > 1
    val wrapFirstHue = if (isLastWrapping) hexHueDegrees(assigned.first()) else null

    val clashesWithExisting = assigned.any { existing ->
      circularHueDistance(preferredHue, hexHueDegrees(existing)) < minHueSeparation
    }
    val clashesWithWrapNeighbor = wrapFirstHue != null &&
      circularHueDistance(preferredHue, wrapFirstHue) < minHueSeparation

    if (!clashesWithExisting && !clashesWithWrapNeighbor) {
      assigned.add(preferred)
    } else {
      val prevHue = assigned.lastOrNull()?.let { hexHueDegrees(it) }
      val bestCandidate = DistinctDonutPaletteHex.maxByOrNull { candidate ->
        val candHue = hexHueDegrees(candidate)
        val minAllDist = assigned.minOfOrNull { existing ->
          circularHueDistance(candHue, hexHueDegrees(existing))
        } ?: 180f
        val prevBonus = prevHue?.let { minOf(circularHueDistance(candHue, it), 90f) * 0.25f } ?: 0f
        val wrapBonus = wrapFirstHue?.let { minOf(circularHueDistance(candHue, it), 90f) * 0.25f } ?: 0f
        minAllDist + prevBonus + wrapBonus
      } ?: preferred
      assigned.add(bestCandidate)
    }
  }
  return assigned
}

@Composable
fun CategoryDonutChart(
  breakdown: List<CategoryBreakdown>,
  currencyCode: String,
  modifier: Modifier = Modifier,
) {
  if (breakdown.isEmpty()) {
    Text(
      text = "No expenses recorded for this month yet.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }

  var selectedIdx by remember(breakdown) { mutableIntStateOf(0) }
  val highlighted = breakdown.getOrNull(selectedIdx) ?: breakdown.first()
  val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
  val sliceColors = remember(breakdown, isDarkSurface) {
    resolveDistinctDonutColorHexes(breakdown).map { hex ->
      adaptColorForSurface(Color(hex), isDarkSurface)
    }
  }

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(190.dp),
      contentAlignment = Alignment.Center,
    ) {
      Canvas(modifier = Modifier.size(176.dp)) {
        val strokeWidth = 28.dp.toPx()
        var startAngle = -90f
        breakdown.forEachIndexed { idx, slice ->
          val sweep = (slice.percentage.toFloat() / 100f) * 360f
          val isSelected = idx == selectedIdx
          drawArc(
            color = sliceColors.getOrElse(idx) {
              adaptColorForSurface(Color(slice.colorHex), isDarkSurface)
            },
            startAngle = startAngle,
            sweepAngle = (sweep - 2f).coerceAtLeast(2f),
            useCenter = false,
            style = Stroke(
              width = if (isSelected) strokeWidth + 5.dp.toPx() else strokeWidth,
              cap = StrokeCap.Butt,
            ),
          )
          startAngle += sweep
        }
      }
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.widthIn(max = 104.dp),
      ) {
        Text(
          text = compactCategoryLabel(highlighted.category),
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          textAlign = TextAlign.Center,
        )
        AutoResizeSingleLineText(
          text = FinanceCalculator.formatCompactCurrencyOrMasked(
            amount = highlighted.amount,
            currencyCode = currencyCode,
            isHidden = LocalIsBalanceHidden.current,
            omitZeroDecimalsForStandard = true,
          ),
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface,
          minFontSize = 11.sp,
        )
        Text(
          text = "${highlighted.percentage.toPlainString()}%",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
        )
      }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      breakdown.forEachIndexed { idx, item ->
        val isSelected = idx == selectedIdx
        val resolvedColor = sliceColors.getOrElse(idx) {
          accessibleCategoryColor(Color(item.colorHex))
        }
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { selectedIdx = idx },
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              modifier = Modifier.weight(1f),
            ) {
              Box(
                modifier = Modifier
                  .size(10.dp)
                  .clip(CircleShape)
                  .background(resolvedColor),
              )
              Text(
                text = compactCategoryLabel(item.category),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
              Text(
                text = "${item.percentage.toPlainString()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = FinanceCalculator.formatCompactCurrencyOrMasked(
                  amount = item.amount,
                  currencyCode = currencyCode,
                  isHidden = LocalIsBalanceHidden.current,
                  omitZeroDecimalsForStandard = true,
                ),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
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

@Composable
fun SpendingTrendLineChart(
  points: List<DailySpendingPoint>,
  currencyCode: String,
  selectedMonth: YearMonthKey = YearMonthKey.current(),
  modifier: Modifier = Modifier,
) {
  if (points.isEmpty()) {
    Text(
      text = "No daily spending activity for this month yet.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }

  val totalDays = remember(selectedMonth) { selectedMonth.daysInMonth() }
  val monthShort = remember(selectedMonth) { selectedMonth.shortLabel() }
  val peakIdx = remember(points) {
    points.indices.maxByOrNull { points[it].amount } ?: 0
  }
  var selectedIdx by remember(points) { mutableIntStateOf(peakIdx) }
  val activePoint = points.getOrElse(selectedIdx) { points.first() }
  val isPeakSelected = selectedIdx == peakIdx
  val tickDays = remember(totalDays) {
    listOf(1, 8, 15, 22, totalDays).distinct()
  }

  val isHidden = LocalIsBalanceHidden.current
  val activeFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
    amount = activePoint.amount,
    currencyCode = currencyCode,
    isHidden = isHidden,
    omitZeroDecimalsForStandard = true,
  )

  Column(modifier = modifier.fillMaxWidth()) {
    Surface(
      shape = RoundedCornerShape(10.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("daily_spending_readout"),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (isPeakSelected) {
            "Peak · $monthShort ${activePoint.dayOfMonth}"
          } else {
            "$monthShort ${activePoint.dayOfMonth}"
          },
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
        )
        Text(
          text = activeFormatted,
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.primary,
          maxLines = 1,
          softWrap = false,
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    DailyCalendarCurveCanvas(
      points = points,
      totalDays = totalDays,
      tickDays = tickDays,
      selectedIndex = selectedIdx,
      onSelectIndex = { selectedIdx = it },
    )

    Spacer(modifier = Modifier.height(6.dp))

    // Proportionally aligned calendar X-axis labels directly matching canvas day coordinates
    CalendarAxisTickRow(
      tickDays = tickDays,
      totalDays = totalDays,
      monthShort = monthShort,
    )
  }
}

@Composable
private fun DailyCalendarCurveCanvas(
  points: List<DailySpendingPoint>,
  totalDays: Int,
  tickDays: List<Int>,
  selectedIndex: Int,
  onSelectIndex: (Int) -> Unit,
) {
  val lineColor = MaterialTheme.colorScheme.primary
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val surfaceColor = MaterialTheme.colorScheme.surface
  val maxVal = points.maxOfOrNull { it.amount.toFloat() }?.coerceAtLeast(1f) ?: 1f
  val spanDays = (totalDays - 1).coerceAtLeast(1).toFloat()

  Canvas(
    modifier = Modifier
      .fillMaxWidth()
      .height(150.dp)
      .pointerInput(points, totalDays) {
        detectTapGestures { tapOffset ->
          val padX = 12.dp.toPx()
          val usableW = (size.width - padX * 2).coerceAtLeast(1f)
          val nearestIdx = points.indices.minByOrNull { idx ->
            val fraction = ((points[idx].dayOfMonth - 1).coerceIn(0, totalDays - 1)) / spanDays
            val pointX = padX + fraction * usableW
            abs(tapOffset.x - pointX)
          }
          if (nearestIdx != null) {
            onSelectIndex(nearestIdx)
          }
        }
      },
  ) {
    val padX = 12.dp.toPx()
    val padY = 14.dp.toPx()
    val usableW = (size.width - padX * 2).coerceAtLeast(1f)
    val usableH = (size.height - padY * 2).coerceAtLeast(1f)
    val baselineY = padY + usableH

    // 3 horizontal reference gridlines
    for (i in 0..2) {
      val y = padY + usableH * (i / 2f)
      drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }

    // Subtle baseline tick marks aligned with the calendar labels below
    tickDays.forEach { day ->
      val frac = ((day - 1).coerceIn(0, totalDays - 1)) / spanDays
      val tickX = padX + frac * usableW
      drawLine(
        color = gridColor,
        start = Offset(tickX, baselineY),
        end = Offset(tickX, baselineY + 4.dp.toPx()),
        strokeWidth = 1.dp.toPx(),
      )
    }

    val coordinates = points.map { point ->
      val frac = ((point.dayOfMonth - 1).coerceIn(0, totalDays - 1)) / spanDays
      val x = padX + frac * usableW
      val normalized = (point.amount.toFloat() / maxVal).coerceIn(0f, 1f)
      val y = (padY + usableH * (1f - normalized)).coerceIn(padY, baselineY)
      Offset(x, y)
    }

    if (coordinates.size >= 2) {
      val linePath = Path().apply {
        moveTo(coordinates.first().x, coordinates.first().y)
        for (i in 1 until coordinates.size) {
          val prev = coordinates[i - 1]
          val curr = coordinates[i]
          val midX = (prev.x + curr.x) / 2f
          // Horizontal-tangent cubic Bezier stays strictly bounded within [min(prev.y, curr.y), max(prev.y, curr.y)]
          cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
        }
      }
      val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coordinates.last().x, baselineY)
        lineTo(coordinates.first().x, baselineY)
        close()
      }
      drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
          colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
          startY = padY,
          endY = baselineY,
        ),
      )
      drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    // Active vertical guide line for inspected day
    coordinates.getOrNull(selectedIndex)?.let { active ->
      drawLine(
        color = lineColor.copy(alpha = 0.35f),
        start = Offset(active.x, padY),
        end = Offset(active.x, baselineY),
        strokeWidth = 1.25.dp.toPx(),
      )
    }

    coordinates.forEachIndexed { idx, pt ->
      val isSelected = idx == selectedIndex
      if (isSelected) {
        drawCircle(color = lineColor.copy(alpha = 0.24f), radius = 10.dp.toPx(), center = pt)
        drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = pt)
        drawCircle(color = lineColor, radius = 4.5.dp.toPx(), center = pt)
      } else {
        drawCircle(color = Color.White, radius = 4.5.dp.toPx(), center = pt)
        drawCircle(color = lineColor, radius = 3.dp.toPx(), center = pt)
      }
    }
  }
}

@Composable
private fun CalendarAxisTickRow(
  tickDays: List<Int>,
  totalDays: Int,
  monthShort: String,
) {
  val spanDays = (totalDays - 1).coerceAtLeast(1).toFloat()
  Layout(
    content = {
      tickDays.forEach { day ->
        Text(
          text = "$monthShort $day",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
        )
      }
    },
    modifier = Modifier.fillMaxWidth(),
  ) { measurables, constraints ->
    val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0)) }
    val height = placeables.maxOfOrNull { it.height } ?: 0
    val width = constraints.maxWidth
    val padPx = 12.dp.roundToPx()
    val usableW = (width - padPx * 2).coerceAtLeast(1)

    layout(width, height) {
      placeables.forEachIndexed { idx, placeable ->
        val day = tickDays[idx]
        val frac = ((day - 1).coerceIn(0, totalDays - 1)) / spanDays
        val anchorX = padPx + (frac * usableW).roundToInt()
        val x = when (idx) {
          0 -> 0
          placeables.lastIndex -> (width - placeable.width).coerceAtLeast(0)
          else -> (anchorX - placeable.width / 2).coerceIn(0, (width - placeable.width).coerceAtLeast(0))
        }
        placeable.placeRelative(x = x, y = 0)
      }
    }
  }
}

@Composable
fun AllTimeMonthlySpendingChart(
  points: List<MonthlySpendingPoint>,
  currencyCode: String,
  modifier: Modifier = Modifier,
) {
  if (points.isEmpty()) {
    Text(
      text = "No expense history recorded yet.",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = modifier,
    )
    return
  }

  val peakPoint = points.maxByOrNull { it.amount }
  val axisLabels = remember(points) {
    if (points.size <= 6) {
      points.map { it.yearMonth.shortLabel() }
    } else {
      val step = (points.lastIndex.toFloat() / 5f)
      (0..5).map { idx ->
        val pointIdx = (idx * step).toInt().coerceIn(0, points.lastIndex)
        points[pointIdx].yearMonth.shortLabel()
      }
    }
  }

  Column(modifier = modifier.fillMaxWidth()) {
    if (peakPoint != null) {
      val peakFormatted = FinanceCalculator.formatCompactCurrencyOrMasked(
        amount = peakPoint.amount,
        currencyCode = currencyCode,
        isHidden = LocalIsBalanceHidden.current,
        omitZeroDecimalsForStandard = true,
      )
      Text(
        text = "Peak spending month: ${peakPoint.yearMonth.displayLabel()} ($peakFormatted)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))
    }

    SpendingCurveCanvas(values = points.map { it.amount.toFloat() })

    Spacer(modifier = Modifier.height(6.dp))
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = if (axisLabels.size == 1) Arrangement.Center else Arrangement.SpaceBetween,
    ) {
      axisLabels.forEach { label ->
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          softWrap = false,
        )
      }
    }
  }
}

@Composable
private fun SpendingCurveCanvas(values: List<Float>) {
  val lineColor = MaterialTheme.colorScheme.primary
  val gridColor = MaterialTheme.colorScheme.outlineVariant
  val maxVal = values.maxOrNull()?.coerceAtLeast(1f) ?: 1f

  Canvas(
    modifier = Modifier
      .fillMaxWidth()
      .height(150.dp),
  ) {
    val padX = 12.dp.toPx()
    val padY = 14.dp.toPx()
    val usableW = (size.width - padX * 2).coerceAtLeast(1f)
    val usableH = (size.height - padY * 2).coerceAtLeast(1f)

    for (i in 0..2) {
      val y = padY + usableH * (i / 2f)
      drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
    }

    val coordinates = if (values.size == 1) {
      val y = padY + usableH * (1f - (values.first() / maxVal))
      listOf(Offset(size.width / 2f, y))
    } else {
      values.mapIndexed { idx, value ->
        val x = padX + (idx.toFloat() / (values.size - 1)) * usableW
        val y = padY + usableH * (1f - (value / maxVal))
        Offset(x, y)
      }
    }

    if (coordinates.size >= 2) {
      val linePath = Path().apply {
        moveTo(coordinates.first().x, coordinates.first().y)
        for (i in 1 until coordinates.size) {
          val prev = coordinates[i - 1]
          val curr = coordinates[i]
          val midX = (prev.x + curr.x) / 2f
          cubicTo(midX, prev.y, midX, curr.y, curr.x, curr.y)
        }
      }
      val fillPath = Path().apply {
        addPath(linePath)
        lineTo(coordinates.last().x, padY + usableH)
        lineTo(coordinates.first().x, padY + usableH)
        close()
      }
      drawPath(
        path = fillPath,
        brush = Brush.verticalGradient(
          colors = listOf(lineColor.copy(alpha = 0.28f), lineColor.copy(alpha = 0.02f)),
          startY = padY,
          endY = padY + usableH,
        ),
      )
      drawPath(
        path = linePath,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
      )
    }

    coordinates.forEach { pt ->
      drawCircle(color = Color.White, radius = 5.dp.toPx(), center = pt)
      drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
    }
  }
}
