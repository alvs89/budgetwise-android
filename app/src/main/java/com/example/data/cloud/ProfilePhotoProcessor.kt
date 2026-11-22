package com.example.data.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.Base64
import android.util.Log
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Decodes picked gallery / Google Photos `content://` URIs cleanly (handling HEIC, WebP, EXIF
 * rotation, and cloud-backed Photo Picker streams), crops user-adjusted circular avatars to a
 * crisp 512x512 square, and encodes them to compact JPEG Base64 for cross-device Firestore sync.
 */
object ProfilePhotoProcessor {
  private const val TAG = "ProfilePhotoProcessor"
  private const val EDITABLE_MAX_DIMENSION_PX = 1440
  private const val OUTPUT_AVATAR_SIZE_PX = 512
  private const val INITIAL_JPEG_QUALITY = 85
  private const val MIN_JPEG_QUALITY = 45
  private const val MAX_BASE64_CHARS = 220_000

  /**
   * Safely decodes a `content://` image [uri] on a background thread into a software `ARGB_8888`
   * [Bitmap] suitable for interactive Compose preview & Canvas cropping.
   *
   * Uses [ImageDecoder] on API 28+ (automatic EXIF orientation + HEIC/WebP + cloud stream support)
   * with a single-pass `openInputStream` + `openFileDescriptor` fallback so cloud-backed Google
   * Photos URIs never fail due to double-opening streams or hardware bitmap restrictions.
   */
  fun decodeEditableBitmap(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int = EDITABLE_MAX_DIMENSION_PX,
  ): Bitmap? {
    val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      decodeViaImageDecoder(context, uri, maxDimensionPx)
        ?: decodeViaSinglePassBytes(context, uri, maxDimensionPx)
        ?: decodeViaFileDescriptor(context, uri, maxDimensionPx)
    } else {
      decodeViaSinglePassBytes(context, uri, maxDimensionPx)
        ?: decodeViaFileDescriptor(context, uri, maxDimensionPx)
    } ?: return null

    return ensureSoftwareArgb8888(decoded)
  }

  private fun decodeViaImageDecoder(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int,
  ): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    return runCatching {
      val source = ImageDecoder.createSource(context.contentResolver, uri)
      ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        decoder.isMutableRequired = false
        val srcW = info.size.width
        val srcH = info.size.height
        if (srcW > 0 && srcH > 0) {
          val longest = max(srcW, srcH)
          if (longest > maxDimensionPx) {
            val ratio = maxDimensionPx.toFloat() / longest.toFloat()
            val targetW = (srcW * ratio).roundToInt().coerceAtLeast(1)
            val targetH = (srcH * ratio).roundToInt().coerceAtLeast(1)
            decoder.setTargetSize(targetW, targetH)
          }
        }
      }
    }.onFailure { err ->
      Log.w(TAG, "ImageDecoder failed for uri=$uri, trying stream fallback", err)
    }.getOrNull()
  }

  private fun decodeViaSinglePassBytes(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int,
  ): Bitmap? = runCatching {
    val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
      stream.readBytes()
    } ?: return null
    if (bytes.isEmpty()) return null

    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
    if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

    val decodeOpts = BitmapFactory.Options().apply {
      inSampleSize = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimensionPx)
      inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
  }.onFailure { err ->
    Log.w(TAG, "Stream byte fallback failed for uri=$uri", err)
  }.getOrNull()

  private fun decodeViaFileDescriptor(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int,
  ): Bitmap? = runCatching {
    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
      val fd = pfd.fileDescriptor
      val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
      BitmapFactory.decodeFileDescriptor(fd, null, boundsOpts)
      if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

      val decodeOpts = BitmapFactory.Options().apply {
        inSampleSize = computeInSampleSize(boundsOpts.outWidth, boundsOpts.outHeight, maxDimensionPx)
        inPreferredConfig = Bitmap.Config.ARGB_8888
      }
      BitmapFactory.decodeFileDescriptor(fd, null, decodeOpts)
    }
  }.onFailure { err ->
    Log.w(TAG, "FileDescriptor fallback failed for uri=$uri", err)
  }.getOrNull()

  private fun computeInSampleSize(width: Int, height: Int, maxDimensionPx: Int): Int {
    var sampleSize = 1
    while (max(width / (sampleSize * 2), height / (sampleSize * 2)) >= maxDimensionPx) {
      sampleSize *= 2
    }
    return sampleSize
  }

  private fun ensureSoftwareArgb8888(bitmap: Bitmap): Bitmap {
    val isHardware = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
      bitmap.config == Bitmap.Config.HARDWARE
    if (!isHardware && bitmap.config == Bitmap.Config.ARGB_8888) {
      return bitmap
    }
    val copied = bitmap.copy(Bitmap.Config.ARGB_8888, false)
    if (copied != null && copied !== bitmap) {
      bitmap.recycle()
      return copied
    }
    return bitmap
  }

  /**
   * Crops the exact square region visible inside the circular cropper viewport and encodes it to
   * a `512x512` JPEG Base64 string (starting at quality 85%, stepping down only if needed to stay
   * comfortably within Firestore's document size limit).
   *
   * [offsetXNorm] and [offsetYNorm] are the user's pan translations divided by the square
   * viewport side length in pixels (`offsetPx / viewportSizePx`), and [scale] is the pinch zoom
   * factor (`>= 1f`) applied on top of `ContentScale.Crop`.
   */
  fun cropAndEncodeCircularAvatarToBase64(
    source: Bitmap,
    scale: Float,
    offsetXNorm: Float,
    offsetYNorm: Float,
    outputSizePx: Int = OUTPUT_AVATAR_SIZE_PX,
  ): String? {
    if (source.isRecycled || source.width <= 0 || source.height <= 0) return null
    val safeSource = ensureSoftwareArgb8888(source)
    val srcW = safeSource.width.toFloat()
    val srcH = safeSource.height.toFloat()
    val safeScale = scale.takeIf { it.isFinite() }?.coerceIn(1f, 5f) ?: 1f
    val safeOffsetX = offsetXNorm.takeIf { it.isFinite() } ?: 0f
    val safeOffsetY = offsetYNorm.takeIf { it.isFinite() } ?: 0f

    // At scale = 1f, ContentScale.Crop fills the unit square [0..1]x[0..1]
    val baseScale = max(1f / srcW, 1f / srcH)
    val effectiveScale = baseScale * safeScale
    val visibleSrcSide = (1f / effectiveScale).coerceAtMost(min(srcW, srcH))

    val halfVisible = visibleSrcSide / 2f
    val centerXSrc = (srcW / 2f - safeOffsetX / effectiveScale)
      .coerceIn(halfVisible, (srcW - halfVisible).coerceAtLeast(halfVisible))
    val centerYSrc = (srcH / 2f - safeOffsetY / effectiveScale)
      .coerceIn(halfVisible, (srcH - halfVisible).coerceAtLeast(halfVisible))

    val left = (centerXSrc - halfVisible).roundToInt().coerceIn(0, safeSource.width - 1)
    val top = (centerYSrc - halfVisible).roundToInt().coerceIn(0, safeSource.height - 1)
    val right = (centerXSrc + halfVisible).roundToInt().coerceIn(left + 1, safeSource.width)
    val bottom = (centerYSrc + halfVisible).roundToInt().coerceIn(top + 1, safeSource.height)

    val outputBitmap = Bitmap.createBitmap(outputSizePx, outputSizePx, Bitmap.Config.ARGB_8888)
    return try {
      val canvas = Canvas(outputBitmap)
      canvas.drawColor(Color.parseColor("#12221C"))
      val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
      canvas.drawBitmap(
        safeSource,
        Rect(left, top, right, bottom),
        RectF(0f, 0f, outputSizePx.toFloat(), outputSizePx.toFloat()),
        paint,
      )
      compressBitmapToBase64Jpeg(outputBitmap)
    } catch (err: Exception) {
      Log.e(TAG, "Failed to crop and encode avatar bitmap", err)
      null
    } finally {
      outputBitmap.recycle()
    }
  }

  /**
   * Convenience fallback that decodes [uri] and performs a centered 512x512 crop + JPEG encode.
   */
  fun encodePickedUriToBase64Jpeg(context: Context, uri: Uri): String? {
    val decoded = decodeEditableBitmap(context, uri) ?: return null
    return try {
      cropAndEncodeCircularAvatarToBase64(
        source = decoded,
        scale = 1f,
        offsetXNorm = 0f,
        offsetYNorm = 0f,
      )
    } finally {
      if (!decoded.isRecycled) decoded.recycle()
    }
  }

  private fun compressBitmapToBase64Jpeg(bitmap: Bitmap): String? {
    var quality = INITIAL_JPEG_QUALITY
    while (quality >= MIN_JPEG_QUALITY) {
      val baos = ByteArrayOutputStream()
      bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
      val candidate = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
      if (candidate.length <= MAX_BASE64_CHARS) {
        return candidate
      }
      quality -= 8
    }
    return null
  }

  fun decodeBase64ToBytes(base64: String?): ByteArray? {
    if (base64.isNullOrBlank()) return null
    return runCatching { Base64.decode(base64, Base64.DEFAULT) }.getOrNull()
  }
}
