package com.example

import com.example.domain.PinSecurityManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinSecurityManagerTest {

  @Test
  fun `validates strictly 4-digit numeric PINs`() {
    assertTrue(PinSecurityManager.isValidPinFormat("0429"))
    assertTrue(PinSecurityManager.isValidPinFormat("9999"))
    assertFalse(PinSecurityManager.isValidPinFormat("123"))
    assertFalse(PinSecurityManager.isValidPinFormat("12345"))
    assertFalse(PinSecurityManager.isValidPinFormat("12a4"))
    assertFalse(PinSecurityManager.isValidPinFormat(""))
  }

  @Test
  fun `salted PBKDF2 hashing produces unique records and verifies accurately`() {
    val firstRecord = PinSecurityManager.createPinRecord("2580")
    val secondRecord = PinSecurityManager.createPinRecord("2580")

    assertNotEquals(firstRecord, secondRecord)
    assertTrue(PinSecurityManager.verifyPin("2580", firstRecord))
    assertTrue(PinSecurityManager.verifyPin("2580", secondRecord))
    assertFalse(PinSecurityManager.verifyPin("2581", firstRecord))
    assertFalse(PinSecurityManager.verifyPin("0000", firstRecord))
    assertFalse(PinSecurityManager.verifyPin("2580", null))
    assertFalse(PinSecurityManager.verifyPin("2580", "malformed:hash"))
  }

  @Test
  fun `lockout timing and remaining countdown seconds compute safely`() {
    val now = 1_000_000L
    val until = now + 29_200L

    assertTrue(PinSecurityManager.isLockedOut(until, now))
    assertEquals(30, PinSecurityManager.remainingLockoutSeconds(until, now))

    assertFalse(PinSecurityManager.isLockedOut(now, now))
    assertEquals(0, PinSecurityManager.remainingLockoutSeconds(now - 500L, now))
  }

  @Test
  fun `lockout countdown formats seconds and minutes cleanly`() {
    assertEquals("0s", PinSecurityManager.formatLockoutCountdown(-5))
    assertEquals("1s", PinSecurityManager.formatLockoutCountdown(1))
    assertEquals("30s", PinSecurityManager.formatLockoutCountdown(30))
    assertEquals("1m 00s", PinSecurityManager.formatLockoutCountdown(60))
    assertEquals("2m 05s", PinSecurityManager.formatLockoutCountdown(125))
    assertEquals("5m 00s", PinSecurityManager.formatLockoutCountdown(300))
  }
}
