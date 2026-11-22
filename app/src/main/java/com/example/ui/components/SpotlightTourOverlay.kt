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
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forum
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

enum class SpotlightTargetId {
  FAB_ADD_TRANSACTION,
  FAB_AI_ASSISTANT,
  NET_CASH_FLOW_CARD,
  TOP_ACTION_BAR,
  BOTTOM_NAV_BAR,
}

data class SpotlightTourStep(
  val targetId: SpotlightTargetId,
  val titleRes: Int,
  val bodyRes: Int,
  val icon: ImageVector,
  val highlightPaddingDp: Float = 8f,
  val cornerRadiusDp: Float = 18f,
)

val BudgetWiseSpotlightSteps: List<SpotlightTourStep> = listOf(
  SpotlightTourStep(
    targetId = SpotlightTargetId.FAB_ADD_TRANSACTION,
    titleRes = R.string.spotlight_step1_title,
    bodyRes = R.string.spotlight_step1_body,
    icon = Icons.Rounded.Add,
    highlightPaddingDp = 10f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    targetId = SpotlightTargetId.FAB_AI_ASSISTANT,
    titleRes = R.string.spotlight_step2_title,
    bodyRes = R.string.spotlight_step2_body,
    icon = Icons.Rounded.Forum,
    highlightPaddingDp = 10f,
    cornerRadiusDp = 18f,
  ),
  SpotlightTourStep(
    targetId = SpotlightTargetId.NET_CASH_FLOW_CARD,
    titleRes = R.string.spotlight_step3_title,
    bodyRes = R.string.spotlight_step3_body,
    icon = Icons.Rounded.AccountBalanceWallet,
    highlightPaddingDp = 6f,
    cornerRadiusDp = 22f,
  ),
  SpotlightTourStep(
    targetId = SpotlightTargetId.TOP_ACTION_BAR,
    titleRes = R.string.spotlight_step4_title,
    bodyRes = R.string.spotlight_step4_body,
    icon = Icons.Rounded.Tune,
    highlightPaddingDp = 8f,
    cornerRadiusDp = 24f,
  ),
  SpotlightTourStep(
    targetId = SpotlightTargetId.BOTTOM_NAV_BAR,
    titleRes = R.string.spotlight_step5_title,
    bodyRes = R.string.spotlight_step5_body,
    icon = Icons.Rounded.Explore,
    highlightPaddingDp = 4f,
    cornerRadiusDp = 16f,
  ),
)

/**
 * Interactive real-time Spotlight Tour (Coach Marks) overlay rendered directly over the live Home
 * dashboard.
 *
 * Uses measured root-space target bounds and measured tooltip height (`onSizeChanged`) to anchor
 * the compact tooltip card strictly ABOVE lower-half targets (`+` FAB, AI FAB, Bottom Nav Bar)
 * and strictly BELOW upper-half targets (Top Action Bar, Net Cash Flow Card) with zero overlap.
 */
@Composable
fun SpotlightTourOverlay(
  visible: Boolean,
  currentStepIndex: Int,
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
          detectTapGestures { /* Consume background taps while tour is active */ }
        }
        .testTag("spotlight_tour_overlay"),
    ) {
      val containerWidthPx = with(density) { maxWidth.toPx() }.takeIf { it.isFinite() && it > 0f } ?: 0f
      val containerHeightPx = with(density) { maxHeight.toPx() }.takeIf { it.isFinite() && it > 0f } ?: 0f
      val hasContainerBounds = containerWidthPx > 0f && containerHeightPx > 0f
      val rawTargetRect = targetBounds[step.targetId]
      val padPx = with(density) { step.highlightPaddingDp.dp.toPx() }
      val cornerPx = with(density) { step.cornerRadiusDp.dp.toPx() }

      val paddedRect = remember(rawTargetRect, padPx, containerWidthPx, containerHeightPx, navBarBottomPx, step.targetId) {
        if (!hasContainerBounds) {
          null
        } else {
          rawTargetRect
            ?.takeIf { r ->
              r.left.isFinite() && r.top.isFinite() && r.right.isFinite() && r.bottom.isFinite()
            }
            ?.let { r ->
              val isHorizontalBottomNav = step.targetId == SpotlightTargetId.BOTTOM_NAV_BAR &&
                r.width > r.height
              val navBarCutBottom = (containerHeightPx - navBarBottomPx).coerceIn(0f, containerHeightPx)
              val left = (r.left - padPx).coerceIn(0f, containerWidthPx)
              val top = (r.top - padPx).coerceIn(0f, containerHeightPx)
              val right = (r.right + padPx).coerceIn(left, containerWidthPx)
              val bottom = if (isHorizontalBottomNav && navBarBottomPx > 0f) {
                r.bottom.coerceAtMost(navBarCutBottom).coerceIn(top, navBarCutBottom)
              } else {
                (r.bottom + padPx).coerceIn(top, containerHeightPx)
              }
              Rect(left = left, top = top, right = right, bottom = bottom)
            }
        }
      }

      val hasValidTarget = paddedRect != null && paddedRect.width > 4f && paddedRect.height > 4f

      val animSpec = tween<Float>(durationMillis = 260, easing = FastOutSlowInEasing)
      val animLeft by animateFloatAsState(
        targetValue = paddedRect?.left ?: (containerWidthPx * 0.5f),
        animationSpec = animSpec,
        label = "spotlight_left",
      )
      val animTop by animateFloatAsState(
        targetValue = paddedRect?.top ?: (containerHeightPx * 0.5f),
        animationSpec = animSpec,
        label = "spotlight_top",
      )
      val animRight by animateFloatAsState(
        targetValue = paddedRect?.right ?: (containerWidthPx * 0.5f),
        animationSpec = animSpec,
        label = "spotlight_right",
      )
      val animBottom by animateFloatAsState(
        targetValue = paddedRect?.bottom ?: (containerHeightPx * 0.5f),
        animationSpec = animSpec,
        label = "spotlight_bottom",
      )

      Canvas(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
      ) {
        drawRect(color = Color.Black.copy(alpha = 0.74f))

        if (
          hasValidTarget &&
          animLeft.isFinite() &&
          animTop.isFinite() &&
          animRight.isFinite() &&
          animBottom.isFinite()
        ) {
          val cutoutWidth = (animRight - animLeft).coerceAtLeast(1f)
          val cutoutHeight = (animBottom - animTop).coerceAtLeast(1f)
          val radius = CornerRadius(cornerPx, cornerPx)

          drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(animLeft, animTop),
            size = Size(cutoutWidth, cutoutHeight),
            cornerRadius = radius,
            blendMode = BlendMode.Clear,
          )

          drawRoundRect(
            color = highlightColor.copy(alpha = 0.92f),
            topLeft = Offset(animLeft, animTop),
            size = Size(cutoutWidth, cutoutHeight),
            cornerRadius = radius,
            style = Stroke(width = 2.5.dp.toPx()),
          )
        }
      }

      // Collision-free root-coordinate placement using real measured tooltip height
      val screenMidY = containerHeightPx / 2f
      val gapPx = with(density) { 16.dp.toPx() }
      val safeMarginPx = with(density) { 16.dp.toPx() }
      val safeTopPx = (statusBarTopPx + safeMarginPx).coerceAtLeast(safeMarginPx)
      val safeBottomPx = (containerHeightPx - navBarBottomPx - safeMarginPx).coerceAtLeast(safeTopPx)
      val effectiveTooltipHeightPx = if (measuredTooltipHeightPx > 0) {
        measuredTooltipHeightPx.toFloat()
      } else {
        with(density) { 184.dp.toPx() }
      }
      val maxBottomAnchoredTopPx = (safeBottomPx - effectiveTooltipHeightPx).coerceAtLeast(safeTopPx)

      val targetCenterY = paddedRect?.center?.y ?: screenMidY
      val placeCardAbove = hasValidTarget && targetCenterY >= screenMidY

      val rawDesiredTopPx = when {
        !hasContainerBounds -> safeTopPx
        !hasValidTarget -> ((safeTopPx + safeBottomPx - effectiveTooltipHeightPx) * 0.5f)
          .coerceIn(safeTopPx, maxBottomAnchoredTopPx)
        placeCardAbove -> (paddedRect!!.top - gapPx - effectiveTooltipHeightPx)
          .coerceAtLeast(safeTopPx)
        else -> (paddedRect!!.bottom + gapPx)
          .coerceIn(safeTopPx, maxBottomAnchoredTopPx)
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
          .padding(horizontal = 20.dp)
          .alpha(if (measuredTooltipHeightPx > 0) 1f else 0f),
        contentAlignment = Alignment.TopCenter,
      ) {
        Surface(
          shape = RoundedCornerShape(20.dp),
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
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            // Header row: step counter pill + Skip button
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
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("spotlight_step_counter"),
                )
              }

              if (!isLastStep) {
                TextButton(
                  onClick = onSkip,
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                  modifier = Modifier
                    .heightIn(min = 36.dp)
                    .testTag("spotlight_skip_button"),
                ) {
                  Text(
                    text = stringResource(R.string.welcome_skip),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }

            // Compact Icon + Title + crisp 2-line copy
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
              verticalAlignment = Alignment.Top,
            ) {
              Box(
                modifier = Modifier
                  .size(38.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
              ) {
                Icon(
                  imageVector = step.icon,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(19.dp),
                )
              }
              Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
              ) {
                Text(
                  text = stringResource(step.titleRes),
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("spotlight_step_title"),
                )
                Text(
                  text = stringResource(step.bodyRes),
                  style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                  ),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.testTag("spotlight_step_body"),
                )
              }
            }

            // Progress dots + Back / Next (or Done) action buttons
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                repeat(totalSteps) { index ->
                  val active = index == safeIndex
                  Box(
                    modifier = Modifier
                      .width(if (active) 20.dp else 7.dp)
                      .height(7.dp)
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
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier
                      .heightIn(min = 42.dp)
                      .testTag("spotlight_back_button"),
                  ) {
                    Icon(
                      imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                      contentDescription = null,
                      modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = stringResource(R.string.welcome_back),
                      style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
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
                  contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                  modifier = Modifier
                    .heightIn(min = 42.dp)
                    .testTag(if (isLastStep) "spotlight_done_button" else "spotlight_next_button"),
                ) {
                  Text(
                    text = stringResource(
                      if (isLastStep) R.string.welcome_done else R.string.welcome_next
                    ),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Icon(
                    imageVector = if (isLastStep) {
                      Icons.Rounded.Check
                    } else {
                      Icons.AutoMirrored.Rounded.ArrowForward
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
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
