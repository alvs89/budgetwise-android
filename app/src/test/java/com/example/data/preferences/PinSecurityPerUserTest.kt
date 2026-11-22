package com.example.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.PinSecurityManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PinSecurityPerUserTest {

  @Test
  fun `new user account starts with PIN lock OFF even when another account enabled PIN`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val existingUserRepo = UserPreferencesRepository(context, userId = "uid_existing_alpha")
    val brandNewUserRepo = UserPreferencesRepository(context, userId = "uid_brand_new_beta")

    val hashRecord = PinSecurityManager.createPinRecord("2580")
    existingUserRepo.enableAppLock(pinHashRecord = hashRecord, enableBiometric = true)

    val existingPrefs = existingUserRepo.userPreferences.first()
    assertTrue(existingPrefs.appLockEnabled)
    assertTrue(existingPrefs.biometricUnlockEnabled)
    assertTrue(!existingPrefs.pinHashRecord.isNullOrBlank())

    // Brand-new account must strictly default to PIN lock OFF and no PIN hash
    val newUserPrefs = brandNewUserRepo.userPreferences.first()
    assertFalse(newUserPrefs.appLockEnabled)
    assertFalse(newUserPrefs.biometricUnlockEnabled)
    assertNull(newUserPrefs.pinHashRecord)
    assertFalse(newUserPrefs.hasCompletedTour)
  }

  @Test
  fun `spotlight tour completion is isolated per authenticated user`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val userA = UserPreferencesRepository(context, userId = "uid_tour_a")
    val userB = UserPreferencesRepository(context, userId = "uid_tour_b")

    userA.markSpotlightTourCompleted()

    assertTrue(userA.userPreferences.first().hasCompletedTour)
    assertFalse(userB.userPreferences.first().hasCompletedTour)
  }
}
