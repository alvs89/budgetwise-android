package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlin.math.roundToInt

enum class SpotlightTab {
  HOME,
  ACTIVITY,
  BUDGETS,
  GOALS,
  INSIGHTS,
}

enum class SpotlightTargetId {
  HOME_OVERVIEW,
  FAB_ADD_TRANSACTION,
  ACTIVITY_CONTROLS,
  BUDGETS_OVERVIEW,
  GOALS_OVERVIEW,
  INSIGHTS_OVERVIEW,
  TOP_ACTION_BAR,
}

data class SpotlightTourStep(
  val tab: SpotlightTab,
  val targetId: SpotlightTargetId,
  val titleRes: Int,
  val bodyRes: Int,
  val icon: ImageVector,
  val highlightPaddingDp: Float = 8f,
  val cornerRadiusDp: Float = 20f,
)

val BudgetWiseSpotlightSteps: List<SpotlightTourStep> = listOf(
  SpotlightTourStep(
    tab = SpotlightTab.HOME,
    targetId = SpotlightTargetId.HOME_OVERVIEW,
    titleRes = R.string.spotlight_step1_title,
    bodyRes = R.string.spotlight_step1_body,
    icon = Icons.Rounded.AccountBalanceWallet,
    highlightPaddingDp = 6f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.HOME,
    targetId = SpotlightTargetId.FAB_ADD_TRANSACTION,
    titleRes = R.string.spotlight_step2_title,
    bodyRes = R.string.spotlight_step2_body,
    icon = Icons.Rounded.Add,
    highlightPaddingDp = 10f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.ACTIVITY,
    targetId = SpotlightTargetId.ACTIVITY_CONTROLS,
    titleRes = R.string.spotlight_step3_title,
    bodyRes = R.string.spotlight_step3_body,
    icon = Icons.Rounded.ReceiptLong,
    highlightPaddingDp = 8f,
    cornerRadiusDp = 18f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.BUDGETS,
    targetId = SpotlightTargetId.BUDGETS_OVERVIEW,
    titleRes = R.string.spotlight_step4_title,
    bodyRes = R.string.spotlight_step4_body,
    icon = Icons.Rounded.PieChart,
    highlightPaddingDp = 6f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.GOALS,
    targetId = SpotlightTargetId.GOALS_OVERVIEW,
    titleRes = R.string.spotlight_step5_title,
    bodyRes = R.string.spotlight_step5_body,
    icon = Icons.Rounded.Savings,
    highlightPaddingDp = 6f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.INSIGHTS,
    targetId = SpotlightTargetId.INSIGHTS_OVERVIEW,
    titleRes = R.string.spotlight_step6_title,
    bodyRes = R.string.spotlight_step6_body,
    icon = Icons.Rounded.AutoGraph,
    highlightPaddingDp = 6f,
    cornerRadiusDp = 20f,
  ),
  SpotlightTourStep(
    tab = SpotlightTab.HOME,
    targetId = SpotlightTargetId.TOP_ACTION_BAR,
    titleRes = R.string.spotlight_step7_title,
    bodyRes = R.string.spotlight_step7_body,
    icon = Icons.Rounded.Tune,
    highlightPaddingDp = 8f,
    cornerRadiusDp = 24f,
  ),
)

/**
 * Interactive 7-step tab-synchronized Spotlight Tour (Coach Marks) overlay rendered directly over
 * the live BudgetWise screens.
 *
 * Waits until the destination tab is active and its target bounds are attached and non-empty before
 * drawing the spotlight cutout, preventing stale-coordinate flashes during cross-tab transitions.
 * Clamps both cutouts and tooltips safely between system status and navigation bars, and anchors
 * the compact tooltip card collision-free relative to the highlighted target.
 */
@Composable
fun SpotlightTourOverlay(
  visible: Boolean,
  currentStepIndex: Int,
  isTargetTabActive: Boolean,
  targetBounds: Map<SpotlightTargetId, Rect>,
  onNext: () -> Unit,
  onBack: () -> Unit,
  onSkip: () -> Unit,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
) {
  if (visible) {
    BackHandler(enabled = true) {
      if (currentStepIndex > 0) {
        onBack()
      } else {
        onSkip()
      }
    }
  }

  AnimatedVisibility(
    visible = visible,
    enter = fadeIn(tween(220)),
    exit = fadeOut(tween(180)),
    modifier = modifier.fillMaxSize(),
  ) {
    val totalSteps = BudgetWiseSpotlightSteps.size
    val safeIndex = currentStepIndex.coerceIn(0, totalSteps - 1)
    val step = BudgetWiseSpotlightSteps[safeIndex]
    val isLastStep = safeIndex == totalSteps - 1
    val density = LocalDensity.current
    val highlightColor = MaterialTheme.colorScheme.primary
    val statusBarTopPx = WindowInsets.statusBars.getTop(density).toFloat()
    val navBarBottomPx = WindowInsets.navigationBars.getBottom(density).toFloat()

    var measuredTooltipHeightPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectTapGestures { /* Intercept background taps while tour is active */ }
        }
        .testTag("spotlight_tour_overlay"),
    ) {
      val containerWidthPx = with(density) { maxWidth.toPx() }.takeIf { it.isFinite() && it > 0f } ?: 0f
      val containerHeightPx = with(density) { maxHeight.toPx() }.takeIf { it.isFinite() && it > 0f } ?: 0f
      val hasContainerBounds = containerWidthPx > 0f && containerHeightPx > 0f
      val rawTargetRect = if (isTargetTabActive) targetBounds[step.targetId] else null
      val padPx = with(density) { step.highlightPaddingDp.dp.toPx() }
      val cornerPx = with(density) { step.cornerRadiusDp.dp.toPx() }

      val safeCutoutTopPx = statusBarTopPx.coerceIn(0f, containerHeightPx)
      val safeCutoutBottomPx = (containerHeightPx - navBarBottomPx).coerceIn(safeCutoutTopPx, containerHeightPx)

      val paddedRect = remember(
        rawTargetRect,
        padPx,
        containerWidthPx,
        containerHeightPx,
        safeCutoutTopPx,
        safeCutoutBottomPx,
      ) {
        if (!hasContainerBounds) {
          null
        } else {
          rawTargetRect
            ?.takeIf { r ->
              !r.isEmpty &&
                r.left.isFinite() &&
                r.top.isFinite() &&
                r.right.isFinite() &&
                r.bottom.isFinite()
            }
            ?.let { r ->
              val left = (r.left - padPx).coerceIn(0f, containerWidthPx)
              val top = (r.top - padPx).coerceIn(safeCutoutTopPx, safeCutoutBottomPx)
              val right = (r.right + padPx).coerceIn(left, containerWidthPx)
              val bottom = (r.bottom + padPx).coerceIn(top, safeCutoutBottomPx)
              Rect(left = left, top = top, right = right, bottom = bottom)
            }
        }
      }

      val hasValidTarget = paddedRect != null && paddedRect.width > 4f && paddedRect.height > 4f

      // Smoothly fade the cutout in once the destination tab's target rect is attached
      val cutoutAlpha by animateFloatAsState(
        targetValue = if (hasValidTarget) 1f else 0f,
        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
        label = "spotlight_cutout_alpha",
      )

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
      ) {
        drawRect(color = Color.Black.copy(alpha = 0.74f))

        val rect = paddedRect
        if (hasValidTarget && rect != null && cutoutAlpha > 0.01f) {
          val cutoutWidth = (rect.right - rect.left).coerceAtLeast(1f)
          val cutoutHeight = (rect.bottom - rect.top).coerceAtLeast(1f)
          val radius = CornerRadius(cornerPx, cornerPx)

          drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(rect.left, rect.top),
            size = Size(cutoutWidth, cutoutHeight),
            cornerRadius = radius,
            blendMode = BlendMode.Clear,
          )

          drawRoundRect(
            color = highlightColor.copy(alpha = 0.92f * cutoutAlpha),
            topLeft = Offset(rect.left, rect.top),
            size = Size(cutoutWidth, cutoutHeight),
            cornerRadius = radius,
            style = Stroke(width = 2.5.dp.toPx()),
          )
        }
      }

      // Smart max-space collision-free tooltip placement (never intersects paddedRect)
      val gapPx = with(density) { 14.dp.toPx() }
      val clearancePx = with(density) { 24.dp.toPx() }
      val safeMarginPx = with(density) { 12.dp.toPx() }
      val safeTopPx = (statusBarTopPx + safeMarginPx).coerceAtLeast(safeMarginPx)
      val safeBottomPx = (containerHeightPx - navBarBottomPx - safeMarginPx).coerceAtLeast(safeTopPx)
      val effectiveTooltipHeightPx = if (measuredTooltipHeightPx > 0) {
        measuredTooltipHeightPx.toFloat()
      } else {
        with(density) { 164.dp.toPx() }
      }
      val maxSafeTopPx = (safeBottomPx - effectiveTooltipHeightPx).coerceAtLeast(safeTopPx)

      val spaceAbove = if (paddedRect != null) (paddedRect.top - safeTopPx).coerceAtLeast(0f) else 0f
      val spaceBelow = if (paddedRect != null) (safeBottomPx - paddedRect.bottom).coerceAtLeast(0f) else 0f
      val placeCardBelow = hasValidTarget && (
        spaceBelow >= effectiveTooltipHeightPx + clearancePx ||
          spaceBelow >= spaceAbove
      )

      val rawDesiredTopPx = when {
        !hasContainerBounds -> safeTopPx
        !hasValidTarget -> ((safeTopPx + safeBottomPx - effectiveTooltipHeightPx) * 0.5f)
          .coerceIn(safeTopPx, maxSafeTopPx)
        placeCardBelow -> {
          // Anchor strictly below targetRect + 14.dp; never pull upward into paddedRect
          val strictlyBelowTop = paddedRect!!.bottom + gapPx
          strictlyBelowTop.coerceAtMost(maxOf(strictlyBelowTop, maxSafeTopPx))
        }
        else -> {
          // Anchor strictly above targetRect - 14.dp - tooltipHeight; never push downward into paddedRect
          val strictlyAboveTop = paddedRect!!.top - gapPx - effectiveTooltipHeightPx
          strictlyAboveTop.coerceAtLeast(minOf(strictlyAboveTop, safeTopPx))
        }
      }

      val animatedTopPx by animateFloatAsState(
        targetValue = rawDesiredTopPx.takeIf { it.isFinite() } ?: safeTopPx,
        animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
        label = "spotlight_tooltip_y",
      )
      val desiredTopIntPx = (if (measuredTooltipHeightPx > 0) animatedTopPx else rawDesiredTopPx)
        .takeIf { it.isFinite() }
        ?.roundToInt()
        ?: 0

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .offset { IntOffset(x = 0, y = desiredTopIntPx) }
          .padding(horizontal = 16.dp)
          .alpha(if (measuredTooltipHeightPx > 0 && hasValidTarget) 1f else 0f),
        contentAlignment = Alignment.TopCenter,
      ) {
        Surface(
          shape = RoundedCornerShape(18.dp),
          color = MaterialTheme.colorScheme.surface,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
          shadowElevation = 12.dp,
          modifier = Modifier
            .widthIn(max = 440.dp)
            .fillMaxWidth()
            .onSizeChanged { size ->
              if (size.height > 0 && size.height != measuredTooltipHeightPx) {
                measuredTooltipHeightPx = size.height
              }
            }
            .testTag("spotlight_tooltip_card"),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            // Top row: step badge ("1 of 7") on left, Skip on right (hidden on final step)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
              ) {
                Text(
                  text = stringResource(R.string.spotlight_step_counter, safeIndex + 1, totalSteps),
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.3.sp,
                  ),
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier
                    .padding(horizontal = 10.dp, vertical = 3.dp)
                    .testTag("spotlight_step_counter"),
                )
              }

              if (!isLastStep) {
                TextButton(
                  onClick = onSkip,
                  contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                  modifier = Modifier
                    .height(30.dp)
                    .testTag("spotlight_skip_button"),
                ) {
                  Text(
                    text = stringResource(R.string.welcome_skip),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            // Icon + Title + concise body copy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Box(
                modifier = Modifier
                  .size(34.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = step.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Text(
                  text = stringResource(step.titleRes),
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("spotlight_step_title"),
                )
                Text(
                  text = stringResource(step.bodyRes),
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("spotlight_step_body"),
                )
              }
            }

            // Bottom row: compact 7-dot progress indicator + Back / Next (or Done) controls
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                repeat(totalSteps) { index ->
                  val active = index == safeIndex
                  Box(
                    modifier = Modifier
                      .width(if (active) 16.dp else 6.dp)
                      .height(6.dp)
                      .clip(CircleShape)
                      .background(
                        if (active) {
                          MaterialTheme.colorScheme.primary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        }
                      ),
                  )
                }
              }

              Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                if (safeIndex > 0) {
                  OutlinedButton(
                    onClick = onBack,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier
                      .heightIn(min = 36.dp)
                      .testTag("spotlight_back_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = stringResource(R.string.welcome_back),
                      style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                      maxLines = 1,
                      softWrap = false,
                    )
                  }
                }

                Button(
                  onClick = { if (isLastStep) onDone() else onNext() },
                  shape = RoundedCornerShape(12.dp),
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                  ),
                  contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                  modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag(if (isLastStep) "spotlight_done_button" else "spotlight_next_button"),
                ) {
                  Text(
                    text = stringResource(
                      if (isLastStep) R.string.welcome_done else R.string.welcome_next
                    ),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    softWrap = false,
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = if (isLastStep) {
                      Icons.Rounded.Check
                    } else {
                      Icons.AutoMirrored.Rounded.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
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
