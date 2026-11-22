package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import kotlin.math.max

/**
 * Interactive dark-emerald Circular Avatar Cropper modal with pinch-to-zoom and drag-to-pan.
 *
 * Clamps pan offsets strictly to the rendered image bounds so the circular cutout never shows
 * empty margins, and double-tapping resets the transform cleanly.
 */
@Composable
fun ProfilePhotoCropDialog(
  bitmap: Bitmap,
  isSaving: Boolean,
  onCancel: () -> Unit,
  onSaveCrop: (scale: Float, offsetXNorm: Float, offsetYNorm: Float) -> Unit,
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
  val srcW = bitmap.width.toFloat().coerceAtLeast(1f)
  val srcH = bitmap.height.toFloat().coerceAtLeast(1f)

  var scale by remember(bitmap) { mutableFloatStateOf(1f) }
  var offset by remember(bitmap) { mutableStateOf(Offset.Zero) }
  var lastViewportPx by remember(bitmap) { mutableFloatStateOf(1f) }

  Dialog(
    onDismissRequest = { if (!isSaving) onCancel() },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnBackPress = !isSaving,
      dismissOnClickOutside = !isSaving,
    ),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
      tonalElevation = 6.dp,
      shadowElevation = 12.dp,
      modifier = Modifier
        .padding(horizontal = 20.dp)
        .widthIn(max = 420.dp)
        .fillMaxWidth()
        .testTag("profile_photo_crop_dialog"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = stringResource(R.string.crop_photo_title),
            style = MaterialTheme.typography.titleLarge.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = (-0.3).sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
          )
          Text(
            text = stringResource(R.string.crop_photo_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        BoxWithConstraints(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0A1411))
            .clipToBounds()
            .testTag("crop_photo_viewport"),
          contentAlignment = Alignment.Center,
        ) {
          val density = LocalDensity.current
          val viewportPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
          lastViewportPx = viewportPx

          // Compute exact max pan so the zoomed image always covers the circular cutout cleanly
          fun clampOffset(candidate: Offset, currentScale: Float): Offset {
            val baseScale = max(viewportPx / srcW, viewportPx / srcH)
            val renderedW = srcW * baseScale * currentScale
            val renderedH = srcH * baseScale * currentScale
            val maxPanX = ((renderedW - viewportPx) / 2f).coerceAtLeast(0f)
            val maxPanY = ((renderedH - viewportPx) / 2f).coerceAtLeast(0f)
            return Offset(
              x = candidate.x.coerceIn(-maxPanX, maxPanX),
              y = candidate.y.coerceIn(-maxPanY, maxPanY),
            )
          }

          val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
            if (!isSaving) {
              val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
              val nextOffset = clampOffset(offset + panChange, nextScale)
              scale = nextScale
              offset = nextOffset
            }
          }

          val ringColor = MaterialTheme.colorScheme.primary

          Box(
            modifier = Modifier
              .fillMaxSize()
              .pointerInput(bitmap, isSaving) {
                detectTapGestures(
                  onDoubleTap = {
                    if (!isSaving) {
                      scale = 1f
                      offset = Offset.Zero
                    }
                  },
                )
              }
              .transformable(state = transformableState),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              bitmap = imageBitmap,
              contentDescription = stringResource(R.string.crop_photo_title),
              contentScale = ContentScale.Crop,
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = scale
                  scaleY = scale
                  translationX = offset.x
                  translationY = offset.y
                },
            )

            // Scrim with centered transparent circular cutout and crisp mint guide ring
            Canvas(
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
            ) {
              drawRect(color = Color.Black.copy(alpha = 0.68f))
              val radius = (size.minDimension / 2f) - 2.dp.toPx()
              drawCircle(
                color = Color.Transparent,
                radius = radius,
                center = center,
                blendMode = BlendMode.Clear,
              )
              drawCircle(
                color = ringColor.copy(alpha = 0.92f),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx()),
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          OutlinedButton(
            onClick = onCancel,
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("crop_photo_cancel_button"),
          ) {
            Text(
              text = stringResource(R.string.pin_keypad_cancel),
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
          }

          Button(
            onClick = {
              val denom = lastViewportPx.coerceAtLeast(1f)
              onSaveCrop(scale, offset.x / denom, offset.y / denom)
            },
            enabled = !isSaving,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("crop_photo_save_button"),
          ) {
            if (isSaving) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = stringResource(R.string.crop_photo_saving),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
              )
            } else {
              Text(
                text = stringResource(R.string.crop_photo_save),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
      }
    }
  }
}
