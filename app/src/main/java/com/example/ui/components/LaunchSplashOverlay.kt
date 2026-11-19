package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalFinanceColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Brand-only 3.0-second animated launch splash ("BudgetWise"):
 * - 0..950ms: Emblem scales into view (0.85 -> 1.03) with ambient emerald radial swell.
 * - 650..1850ms: "BudgetWise" fades & rises gently while letter-tracking opens cleanly.
 * - 1200..2680ms: Emerald glow breathes smoothly, subtle sheen glides across the badge, and an accent bar extends.
 * - 2680..3000ms: Cross-fades smoothly into the Home Dashboard (or App Lock if enabled).
 */
@Composable
fun LaunchSplashOverlay(
  visible: Boolean,
  modifier: Modifier = Modifier,
) {
  val financeColors = LocalFinanceColors.current
  val density = LocalDensity.current
  val titleTravelPx = with(density) { 10.dp.toPx() }
  val sheenTravelPx = with(density) { 120.dp.toPx() }

  val logoScale = remember { Animatable(0.85f) }
  val logoAlpha = remember { Animatable(0f) }
  val glowScale = remember { Animatable(0.72f) }
  val glowAlpha = remember { Animatable(0f) }
  val sheenOffsetX = remember { Animatable(-sheenTravelPx) }
  val titleAlpha = remember { Animatable(0f) }
  val titleTranslationY = remember { Animatable(titleTravelPx) }
  val titleTrackingSp = remember { Animatable(0.6f) }
  val taglineAlpha = remember { Animatable(0f) }
  val accentProgress = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    // Logo fade-in & subtle breathing scale across 0..2650ms
    launch {
      logoAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
      )
    }
    launch {
      logoScale.animateTo(
        targetValue = 1.03f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
      )
      logoScale.animateTo(
        targetValue = 0.99f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      )
      logoScale.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
      )
    }

    // Ambient emerald radial glow swell & breath across 0..2650ms
    launch {
      glowAlpha.animateTo(
        targetValue = 0.36f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
      )
      glowAlpha.animateTo(
        targetValue = 0.18f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      )
      glowAlpha.animateTo(
        targetValue = 0.30f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      )
    }
    launch {
      glowScale.animateTo(
        targetValue = 1.12f,
        animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing),
      )
      glowScale.animateTo(
        targetValue = 0.94f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      )
      glowScale.animateTo(
        targetValue = 1.06f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      )
    }

    // Soft specular sheen across badge (1150..2250ms)
    launch {
      delay(1150L)
      sheenOffsetX.animateTo(
        targetValue = sheenTravelPx,
        animationSpec = tween(durationMillis = 1050, easing = LinearOutSlowInEasing),
      )
    }

    // Brand title reveal & tracking settle (650..2000ms)
    launch {
      delay(650L)
      launch {
        titleAlpha.animateTo(
          targetValue = 1f,
          animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        )
      }
      launch {
        titleTranslationY.animateTo(
          targetValue = 0f,
          animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        )
      }
      launch {
        titleTrackingSp.animateTo(
          targetValue = 2.0f,
          animationSpec = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
        )
      }
    }

    // Tagline reveal right after brand title (1050..1850ms)
    launch {
      delay(1050L)
      taglineAlpha.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 750, easing = FastOutSlowInEasing),
      )
    }

    // Minimalist emerald accent bar progression (1400..2500ms)
    launch {
      delay(1400L)
      accentProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
      )
    }
  }

  AnimatedVisibility(
    visible = visible,
    exit = fadeOut(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)),
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .testTag("launch_splash_overlay"),
      contentAlignment = Alignment.Center,
    ) {
      // Ambient emerald radial glow breathing behind the emblem
      Box(
        modifier = Modifier
          .size(300.dp)
          .graphicsLayer {
            alpha = glowAlpha.value
            scaleX = glowScale.value
            scaleY = glowScale.value
          }
          .background(
            brush = Brush.radialGradient(
              colors = listOf(
                financeColors.income.copy(alpha = 0.55f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                Color.Transparent,
              ),
            ),
          ),
      )

      Column(
        modifier = Modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
      ) {
        Surface(
          modifier = Modifier
            .size(86.dp)
            .graphicsLayer {
              scaleX = logoScale.value
              scaleY = logoScale.value
              alpha = logoAlpha.value
            },
          shape = RoundedCornerShape(24.dp),
          shadowElevation = 6.dp,
          border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(financeColors.heroStart, financeColors.heroEnd),
                ),
              ),
            contentAlignment = Alignment.Center,
          ) {
            // Subtle specular sheen pass
            Box(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { translationX = sheenOffsetX.value }
                .background(
                  brush = Brush.horizontalGradient(
                    colors = listOf(
                      Color.Transparent,
                      Color.White.copy(alpha = 0.14f),
                      Color.Transparent,
                    ),
                  ),
                ),
            )
            Icon(
              imageVector = Icons.Rounded.AccountBalanceWallet,
              contentDescription = "BudgetWise",
              tint = Color.White,
              modifier = Modifier.size(40.dp),
            )
          }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = titleTrackingSp.value.sp,
          ),
          color = MaterialTheme.colorScheme.onBackground,
          maxLines = 1,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .graphicsLayer {
              alpha = titleAlpha.value
              translationY = titleTranslationY.value
            }
            .testTag("launch_splash_title"),
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = stringResource(R.string.welcome_tagline),
          style = MaterialTheme.typography.bodyMedium.copy(
            letterSpacing = 0.3.sp,
          ),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .graphicsLayer {
              alpha = taglineAlpha.value
            }
            .testTag("launch_splash_tagline"),
        )

        Spacer(modifier = Modifier.height(14.dp))

        Box(
          modifier = Modifier
            .width(44.dp)
            .height(3.dp)
            .graphicsLayer {
              scaleX = accentProgress.value
              alpha = accentProgress.value * 0.85f
            }
            .clip(CircleShape)
            .background(
              brush = Brush.horizontalGradient(
                colors = listOf(
                  MaterialTheme.colorScheme.primary,
                  financeColors.income,
                ),
              ),
            ),
        )
      }
    }
  }
}
