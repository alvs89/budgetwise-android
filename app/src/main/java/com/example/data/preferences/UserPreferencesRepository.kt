package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AlertThresholdDefaults
import com.example.domain.CurrencyOption
import com.example.domain.ThemeMode
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budgetwise_prefs")

data class UserPreferences(
  val displayName: String = "Alex",
  val preferredCurrency: String = CurrencyOption.PHP.code,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val onboardingCompleted: Boolean = false,
  val hasCompletedTour: Boolean = false,
  val hasSeededInitialData: Boolean = false,
  val hasAutoSeededDemoAccount: Boolean = false,
  val isOfflineGuestMode: Boolean = false,
  val hasDismissedLocalMigration: Boolean = false,
  val isBalanceHidden: Boolean = false,
  val appLockEnabled: Boolean = false,
  val biometricUnlockEnabled: Boolean = false,
  val pinHashRecord: String? = null,
  val failedPinAttempts: Int = 0,
  val pinLockedUntilMillis: Long = 0L,
  val budgetAlertsEnabled: Boolean = true,
  val goalAlertsEnabled: Boolean = true,
  val dailyReminderEnabled: Boolean = false,
  val budgetWarningThresholdPercent: Int = AlertThresholdDefaults.DEFAULT_WARNING_THRESHOLD_PERCENT,
  val dailyReminderHour: Int = AlertThresholdDefaults.DEFAULT_REMINDER_HOUR,
  val dailyReminderMinute: Int = AlertThresholdDefaults.DEFAULT_REMINDER_MINUTE,
)

/**
 * Device-local Jetpack DataStore preferences repository.
 *
 * Security-sensitive PIN lock keys and Spotlight Tour completion are scoped per authenticated
 * [userId] (or [LOCAL_GUEST_USER_ID] for offline guest mode) so each account maintains its own
 * lock state and onboarding tour state.
 */
class UserPreferencesRepository(
  private val context: Context,
  private val userId: String? = null,
) {
  private val isLocalGuest: Boolean = userId == LOCAL_GUEST_USER_ID
  private val uidSuffix: String = userId?.takeIf { it.isNotBlank() }?.let { "_$it" }.orEmpty()

  private val displayNameKey = if (isLocalGuest) {
    stringPreferencesKey("display_name_$LOCAL_GUEST_USER_ID")
  } else {
    stringPreferencesKey("display_name")
  }
  private val preferredCurrencyKey = stringPreferencesKey("preferred_currency")
  private val themeModeKey = stringPreferencesKey("theme_mode")
  private val onboardingCompletedKey = booleanPreferencesKey("onboarding_completed$uidSuffix")
  private val hasCompletedTourKey = booleanPreferencesKey("spotlight_tour_completed$uidSuffix")
  private val hasSeededInitialDataKey = booleanPreferencesKey("has_seeded_initial_data")
  private val hasAutoSeededDemoAccountKey = booleanPreferencesKey("demo_account_auto_seeded$uidSuffix")
  private val offlineGuestModeKey = booleanPreferencesKey("offline_guest_mode")
  private val hasDismissedLocalMigrationKey = booleanPreferencesKey("dismissed_local_migration$uidSuffix")
  private val isBalanceHiddenKey = booleanPreferencesKey("is_balance_hidden$uidSuffix")
  private val appLockEnabledKey = booleanPreferencesKey("app_lock_enabled$uidSuffix")
  private val biometricUnlockEnabledKey = booleanPreferencesKey("biometric_unlock_enabled$uidSuffix")
  private val pinHashRecordKey = stringPreferencesKey("pin_hash_record$uidSuffix")
  private val failedPinAttemptsKey = intPreferencesKey("failed_pin_attempts$uidSuffix")
  private val pinLockedUntilMillisKey = longPreferencesKey("pin_locked_until_millis$uidSuffix")
  private val lastActiveUserIdKey = stringPreferencesKey("last_active_user_id")
  private val budgetAlertsEnabledKey = booleanPreferencesKey("budget_alerts_enabled$uidSuffix")
  private val goalAlertsEnabledKey = booleanPreferencesKey("goal_alerts_enabled$uidSuffix")
  private val dailyReminderEnabledKey = booleanPreferencesKey("daily_reminder_enabled$uidSuffix")
  private val budgetWarningThresholdKey = intPreferencesKey("budget_warning_threshold_percent$uidSuffix")
  private val dailyReminderHourKey = intPreferencesKey("daily_reminder_hour$uidSuffix")
  private val dailyReminderMinuteKey = intPreferencesKey("daily_reminder_minute$uidSuffix")

  val userPreferences: Flow<UserPreferences> = context.dataStore.data
    .catch { err ->
      if (err is IOException) {
        emit(emptyPreferences())
      } else {
        throw err
      }
    }
    .map { prefs ->
    val rawTheme = prefs[themeModeKey] ?: ThemeMode.SYSTEM.name
    val themeMode = runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.SYSTEM)
    val rawThreshold = prefs[budgetWarningThresholdKey]
      ?: AlertThresholdDefaults.DEFAULT_WARNING_THRESHOLD_PERCENT
    val storedPinHash = prefs[pinHashRecordKey]?.takeIf { it.isNotBlank() }
    // Dual guardrail: PIN lock is strictly OFF unless explicitly enabled AND a valid PIN hash exists
    val effectiveAppLockEnabled = (prefs[appLockEnabledKey] ?: false) && storedPinHash != null
    val resolvedDisplayName = if (isLocalGuest) {
      prefs[displayNameKey]?.trim().orEmpty()
    } else {
      prefs[displayNameKey]?.takeIf { it.isNotBlank() } ?: "Alex"
    }
    UserPreferences(
      displayName = resolvedDisplayName,
      preferredCurrency = prefs[preferredCurrencyKey] ?: CurrencyOption.PHP.code,
      themeMode = themeMode,
      onboardingCompleted = prefs[onboardingCompletedKey] ?: false,
      hasCompletedTour = prefs[hasCompletedTourKey] ?: false,
      hasSeededInitialData = prefs[hasSeededInitialDataKey] ?: false,
      hasAutoSeededDemoAccount = prefs[hasAutoSeededDemoAccountKey] ?: false,
      isOfflineGuestMode = prefs[offlineGuestModeKey] ?: false,
      hasDismissedLocalMigration = prefs[hasDismissedLocalMigrationKey] ?: false,
      isBalanceHidden = prefs[isBalanceHiddenKey] ?: false,
      appLockEnabled = effectiveAppLockEnabled,
      biometricUnlockEnabled = effectiveAppLockEnabled && (prefs[biometricUnlockEnabledKey] ?: false),
      pinHashRecord = storedPinHash,
      failedPinAttempts = prefs[failedPinAttemptsKey] ?: 0,
      pinLockedUntilMillis = prefs[pinLockedUntilMillisKey] ?: 0L,
      budgetAlertsEnabled = prefs[budgetAlertsEnabledKey] ?: true,
      goalAlertsEnabled = prefs[goalAlertsEnabledKey] ?: true,
      dailyReminderEnabled = prefs[dailyReminderEnabledKey] ?: false,
      budgetWarningThresholdPercent = AlertThresholdDefaults.sanitizeThreshold(rawThreshold),
      dailyReminderHour = (prefs[dailyReminderHourKey] ?: AlertThresholdDefaults.DEFAULT_REMINDER_HOUR).coerceIn(0, 23),
      dailyReminderMinute = (prefs[dailyReminderMinuteKey] ?: AlertThresholdDefaults.DEFAULT_REMINDER_MINUTE).coerceIn(0, 59),
    )
  }

  suspend fun updateBalanceHidden(hidden: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[isBalanceHiddenKey] = hidden
    }
  }

  suspend fun updateDisplayName(name: String) {
    context.dataStore.edit { prefs ->
      val trimmed = name.trim()
      prefs[displayNameKey] = if (isLocalGuest) trimmed else trimmed.ifBlank { "Alex" }
    }
  }

  suspend fun updatePreferredCurrency(currencyCode: String) {
    context.dataStore.edit { prefs ->
      prefs[preferredCurrencyKey] = CurrencyOption.fromCode(currencyCode).code
    }
  }

  suspend fun updateThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs ->
      prefs[themeModeKey] = mode.name
    }
  }

  suspend fun markSpotlightTourCompleted() {
    context.dataStore.edit { prefs ->
      prefs[hasCompletedTourKey] = true
      prefs[onboardingCompletedKey] = true
    }
  }

  suspend fun completeOnboarding(displayName: String, currencyCode: String) {
    context.dataStore.edit { prefs ->
      prefs[displayNameKey] = displayName.trim().ifBlank { "Alex" }
      prefs[preferredCurrencyKey] = CurrencyOption.fromCode(currencyCode).code
      prefs[onboardingCompletedKey] = true
      prefs[hasCompletedTourKey] = true
    }
  }

  suspend fun markOnboardingSkipped() {
    context.dataStore.edit { prefs ->
      prefs[onboardingCompletedKey] = true
      prefs[hasCompletedTourKey] = true
    }
  }

  suspend fun markInitialDataSeeded() {
    context.dataStore.edit { prefs ->
      prefs[hasSeededInitialDataKey] = true
    }
  }

  suspend fun markDemoAccountAutoSeeded() {
    context.dataStore.edit { prefs ->
      prefs[hasAutoSeededDemoAccountKey] = true
    }
  }

  suspend fun setOfflineGuestMode(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[offlineGuestModeKey] = enabled
    }
  }

  suspend fun setDismissedLocalMigration(dismissed: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[hasDismissedLocalMigrationKey] = dismissed
    }
  }

  suspend fun enableAppLock(pinHashRecord: String, enableBiometric: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[appLockEnabledKey] = true
      prefs[biometricUnlockEnabledKey] = enableBiometric
      prefs[pinHashRecordKey] = pinHashRecord
      prefs[failedPinAttemptsKey] = 0
      prefs[pinLockedUntilMillisKey] = 0L
    }
  }

  suspend fun updatePin(pinHashRecord: String) {
    context.dataStore.edit { prefs ->
      prefs[pinHashRecordKey] = pinHashRecord
      prefs[failedPinAttemptsKey] = 0
      prefs[pinLockedUntilMillisKey] = 0L
    }
  }

  suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[biometricUnlockEnabledKey] = enabled
    }
  }

  suspend fun disableAppLock() {
    context.dataStore.edit { prefs ->
      prefs[appLockEnabledKey] = false
      prefs[biometricUnlockEnabledKey] = false
      prefs.remove(pinHashRecordKey)
      prefs[failedPinAttemptsKey] = 0
      prefs[pinLockedUntilMillisKey] = 0L
    }
  }

  suspend fun recordFailedPinAttempt(attempts: Int, lockedUntilMillis: Long) {
    context.dataStore.edit { prefs ->
      prefs[failedPinAttemptsKey] = attempts
      prefs[pinLockedUntilMillisKey] = lockedUntilMillis
    }
  }

  suspend fun resetPinAttempts() {
    context.dataStore.edit { prefs ->
      prefs[failedPinAttemptsKey] = 0
      prefs[pinLockedUntilMillisKey] = 0L
    }
  }

  suspend fun updateBudgetAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[budgetAlertsEnabledKey] = enabled
    }
  }

  suspend fun updateGoalAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[goalAlertsEnabledKey] = enabled
    }
  }

  suspend fun updateDailyReminderEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[dailyReminderEnabledKey] = enabled
    }
  }

  suspend fun updateBudgetWarningThreshold(percent: Int) {
    context.dataStore.edit { prefs ->
      prefs[budgetWarningThresholdKey] = AlertThresholdDefaults.sanitizeThreshold(percent)
    }
  }

  suspend fun updateDailyReminderTime(hour24: Int, minute: Int) {
    context.dataStore.edit { prefs ->
      prefs[dailyReminderHourKey] = hour24.coerceIn(0, 23)
      prefs[dailyReminderMinuteKey] = minute.coerceIn(0, 59)
    }
  }

  suspend fun getLastActiveUserId(): String? =
    context.dataStore.data
      .catch { err ->
        if (err is IOException) emit(emptyPreferences()) else throw err
      }
      .map { prefs -> prefs[lastActiveUserIdKey]?.takeIf { it.isNotBlank() } }
      .first()

  suspend fun setLastActiveUserId(uid: String?) {
    context.dataStore.edit { prefs ->
      if (uid.isNullOrBlank()) {
        prefs.remove(lastActiveUserIdKey)
      } else {
        prefs[lastActiveUserIdKey] = uid
      }
    }
  }

  companion object {
    const val LOCAL_GUEST_USER_ID = "local_guest"
  }
}
