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
import com.example.domain.compactCategoryLabel
import com.example.ui.theme.LocalFinanceColors
import java.math.BigDecimal

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
  val total = breakdown.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.amount) }
  val highlighted = breakdown.getOrNull(selectedIdx) ?: breakdown.first()

  Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
    val isDarkSurface = MaterialTheme.colorScheme.surface.luminance() < 0.5f
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
            color = adaptColorForSurface(
              rawColor = Color(slice.colorHex),
              isDarkSurface = isDarkSurface,
            ),
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
                  .background(accessibleCategoryColor(Color(item.colorHex))),
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

  val peakPoint = points.maxByOrNull { it.amount }
  val axisLabels = remember(points) {
    val labels = points.take(7).map { "D${it.dayOfMonth}" }.toMutableList()
    if (points.size > 7) {
      labels.add("D${points.last().dayOfMonth}")
    }
    labels
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
        text = "Peak spending day: Day ${peakPoint.dayOfMonth} ($peakFormatted)",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Spacer(modifier = Modifier.height(10.dp))
    }

    SpendingCurveCanvas(values = points.map { it.amount.toFloat() })

    Spacer(modifier = Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
