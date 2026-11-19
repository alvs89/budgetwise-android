package com.example.domain

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import kotlin.math.ceil

/**
 * Handles 4-digit App Lock PIN hashing, verification, and progressive brute-force lockout rules.
 *
 * PINs are never stored in plaintext; each PIN is derived with a unique 16-byte random salt
 * using PBKDF2 (`10,000` iterations) and verified using constant-time byte comparison.
 */
object PinSecurityManager {
  const val PIN_LENGTH = 4
  const val MAX_FAILED_ATTEMPTS = 5
  const val LOCKOUT_DURATION_MILLIS = 30_000L
  const val MAX_LOCKOUT_DURATION_MILLIS = 300_000L
  const val BACKGROUND_LOCK_GRACE_MILLIS = 5_000L
  const val RESET_CONFIRMATION_KEYWORD = "RESET"

  private const val SALT_BYTES = 16
  private const val HASH_BITS = 256
  private const val ITERATIONS = 10_000
  private const val ALG_SHA256 = "PBKDF2WithHmacSHA256"
  private const val ALG_SHA1 = "PBKDF2WithHmacSHA1"

  private val secureRandom = SecureRandom()

  fun isValidPinFormat(pin: String): Boolean =
    pin.length == PIN_LENGTH && pin.all { it.isDigit() }

  fun isValidResetConfirmation(input: String): Boolean =
    input.trim().equals(RESET_CONFIRMATION_KEYWORD, ignoreCase = false)

  /**
   * Generates a self-describing salted PBKDF2 hash record formatted as `algorithm:saltHex:hashHex`.
   */
  fun createPinRecord(pin: String): String {
    require(isValidPinFormat(pin)) { "PIN must be exactly $PIN_LENGTH digits." }
    val salt = ByteArray(SALT_BYTES).also { secureRandom.nextBytes(it) }
    val (algorithm, derivedKey) = deriveKey(pin.toCharArray(), salt, preferredAlgorithm())
    return "$algorithm:${salt.toHex()}:${derivedKey.toHex()}"
  }

  /**
   * Verifies [pin] against [storedRecord] using [MessageDigest.isEqual] to prevent timing leaks.
   */
  fun verifyPin(pin: String, storedRecord: String?): Boolean {
    if (!isValidPinFormat(pin) || storedRecord.isNullOrBlank()) return false
    val parts = storedRecord.split(":")
    if (parts.size != 3) return false
    val algorithm = parts[0]
    val salt = parts[1].hexToBytesOrNull() ?: return false
    val expectedHash = parts[2].hexToBytesOrNull() ?: return false

    val (_, actualHash) = deriveKey(pin.toCharArray(), salt, algorithm)
    return MessageDigest.isEqual(expectedHash, actualHash)
  }

  /**
   * Computes progressive brute-force cooldown once [MAX_FAILED_ATTEMPTS] is reached:
   * 5th failure = 30s, 6th = 60s, 7th = 120s, 8th+ = 300s (5 minutes cap).
   */
  fun lockoutDurationForAttempts(failedAttempts: Int): Long {
    if (failedAttempts < MAX_FAILED_ATTEMPTS) return 0L
    val escalationStep = (failedAttempts - MAX_FAILED_ATTEMPTS).coerceAtMost(4)
    val multiplier = 1L shl escalationStep
    return (LOCKOUT_DURATION_MILLIS * multiplier).coerceAtMost(MAX_LOCKOUT_DURATION_MILLIS)
  }

  fun isLockedOut(lockedUntilMillis: Long, nowMillis: Long = System.currentTimeMillis()): Boolean =
    lockedUntilMillis > nowMillis

  fun remainingLockoutSeconds(lockedUntilMillis: Long, nowMillis: Long = System.currentTimeMillis()): Int {
    val diff = lockedUntilMillis - nowMillis
    if (diff <= 0L) return 0
    return ceil(diff / 1000.0).toInt().coerceAtLeast(1)
  }

  /**
   * Formats remaining lockout time cleanly for live countdown display:
   * e.g., 29 -> "29s", 60 -> "1m 00s", 125 -> "2m 05s".
   */
  fun formatLockoutCountdown(remainingSeconds: Int): String {
    val safe = remainingSeconds.coerceAtLeast(0)
    if (safe < 60) return "${safe}s"
    val minutes = safe / 60
    val seconds = safe % 60
    return "${minutes}m ${"%02d".format(seconds)}s"
  }

  private fun preferredAlgorithm(): String = try {
    SecretKeyFactory.getInstance(ALG_SHA256)
    ALG_SHA256
  } catch (_: NoSuchAlgorithmException) {
    ALG_SHA1
  }

  private fun deriveKey(chars: CharArray, salt: ByteArray, algorithm: String): Pair<String, ByteArray> {
    val factory = try {
      SecretKeyFactory.getInstance(algorithm)
    } catch (_: NoSuchAlgorithmException) {
      SecretKeyFactory.getInstance(ALG_SHA1)
    }
    val spec = PBEKeySpec(chars, salt, ITERATIONS, HASH_BITS)
    return try {
      factory.algorithm to factory.generateSecret(spec).encoded
    } finally {
      spec.clearPassword()
    }
  }

  private fun ByteArray.toHex(): String =
    joinToString(separator = "") { byte -> "%02x".format(byte.toInt() and 0xFF) }

  private fun String.hexToBytesOrNull(): ByteArray? {
    if (length % 2 != 0 || isEmpty()) return null
    return try {
      ByteArray(length / 2) { index ->
        val hi = Character.digit(this[index * 2], 16)
        val lo = Character.digit(this[index * 2 + 1], 16)
        if (hi == -1 || lo == -1) return null
        ((hi shl 4) or lo).toByte()
      }
    } catch (_: Exception) {
      null
    }
  }
}
