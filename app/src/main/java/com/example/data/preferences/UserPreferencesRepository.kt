package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.AlertThresholdDefaults
import com.example.domain.CurrencyOption
import com.example.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "budgetwise_prefs")

data class UserPreferences(
  val displayName: String = "Alex",
  val preferredCurrency: String = CurrencyOption.PHP.code,
  val themeMode: ThemeMode = ThemeMode.SYSTEM,
  val onboardingCompleted: Boolean = false,
  val hasSeededInitialData: Boolean = false,
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

class UserPreferencesRepository(private val context: Context) {

  private object Keys {
    val DISPLAY_NAME = stringPreferencesKey("display_name")
    val PREFERRED_CURRENCY = stringPreferencesKey("preferred_currency")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    val HAS_SEEDED_INITIAL_DATA = booleanPreferencesKey("has_seeded_initial_data")
    val IS_BALANCE_HIDDEN = booleanPreferencesKey("is_balance_hidden")
    val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
    val BIOMETRIC_UNLOCK_ENABLED = booleanPreferencesKey("biometric_unlock_enabled")
    val PIN_HASH_RECORD = stringPreferencesKey("pin_hash_record")
    val FAILED_PIN_ATTEMPTS = intPreferencesKey("failed_pin_attempts")
    val PIN_LOCKED_UNTIL_MILLIS = longPreferencesKey("pin_locked_until_millis")
    val BUDGET_ALERTS_ENABLED = booleanPreferencesKey("budget_alerts_enabled")
    val GOAL_ALERTS_ENABLED = booleanPreferencesKey("goal_alerts_enabled")
    val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
    val BUDGET_WARNING_THRESHOLD_PERCENT = intPreferencesKey("budget_warning_threshold_percent")
    val DAILY_REMINDER_HOUR = intPreferencesKey("daily_reminder_hour")
    val DAILY_REMINDER_MINUTE = intPreferencesKey("daily_reminder_minute")
  }

  val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
    val rawTheme = prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name
    val themeMode = runCatching { ThemeMode.valueOf(rawTheme) }.getOrDefault(ThemeMode.SYSTEM)
    val rawThreshold = prefs[Keys.BUDGET_WARNING_THRESHOLD_PERCENT]
      ?: AlertThresholdDefaults.DEFAULT_WARNING_THRESHOLD_PERCENT
    UserPreferences(
      displayName = prefs[Keys.DISPLAY_NAME]?.takeIf { it.isNotBlank() } ?: "Alex",
      preferredCurrency = prefs[Keys.PREFERRED_CURRENCY] ?: CurrencyOption.PHP.code,
      themeMode = themeMode,
      onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
      hasSeededInitialData = prefs[Keys.HAS_SEEDED_INITIAL_DATA] ?: false,
      isBalanceHidden = prefs[Keys.IS_BALANCE_HIDDEN] ?: false,
      appLockEnabled = prefs[Keys.APP_LOCK_ENABLED] ?: false,
      biometricUnlockEnabled = prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] ?: false,
      pinHashRecord = prefs[Keys.PIN_HASH_RECORD],
      failedPinAttempts = prefs[Keys.FAILED_PIN_ATTEMPTS] ?: 0,
      pinLockedUntilMillis = prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] ?: 0L,
      budgetAlertsEnabled = prefs[Keys.BUDGET_ALERTS_ENABLED] ?: true,
      goalAlertsEnabled = prefs[Keys.GOAL_ALERTS_ENABLED] ?: true,
      dailyReminderEnabled = prefs[Keys.DAILY_REMINDER_ENABLED] ?: false,
      budgetWarningThresholdPercent = AlertThresholdDefaults.sanitizeThreshold(rawThreshold),
      dailyReminderHour = (prefs[Keys.DAILY_REMINDER_HOUR] ?: AlertThresholdDefaults.DEFAULT_REMINDER_HOUR).coerceIn(0, 23),
      dailyReminderMinute = (prefs[Keys.DAILY_REMINDER_MINUTE] ?: AlertThresholdDefaults.DEFAULT_REMINDER_MINUTE).coerceIn(0, 59),
    )
  }

  suspend fun updateBalanceHidden(hidden: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.IS_BALANCE_HIDDEN] = hidden
    }
  }

  suspend fun updateDisplayName(name: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DISPLAY_NAME] = name.trim().ifBlank { "Alex" }
    }
  }

  suspend fun updatePreferredCurrency(currencyCode: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.PREFERRED_CURRENCY] = CurrencyOption.fromCode(currencyCode).code
    }
  }

  suspend fun updateThemeMode(mode: ThemeMode) {
    context.dataStore.edit { prefs ->
      prefs[Keys.THEME_MODE] = mode.name
    }
  }

  suspend fun completeOnboarding(displayName: String, currencyCode: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DISPLAY_NAME] = displayName.trim().ifBlank { "Alex" }
      prefs[Keys.PREFERRED_CURRENCY] = CurrencyOption.fromCode(currencyCode).code
      prefs[Keys.ONBOARDING_COMPLETED] = true
    }
  }

  suspend fun markOnboardingSkipped() {
    context.dataStore.edit { prefs ->
      prefs[Keys.ONBOARDING_COMPLETED] = true
    }
  }

  suspend fun markInitialDataSeeded() {
    context.dataStore.edit { prefs ->
      prefs[Keys.HAS_SEEDED_INITIAL_DATA] = true
    }
  }

  suspend fun enableAppLock(pinHashRecord: String, enableBiometric: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.APP_LOCK_ENABLED] = true
      prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = enableBiometric
      prefs[Keys.PIN_HASH_RECORD] = pinHashRecord
      prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
      prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] = 0L
    }
  }

  suspend fun updatePin(pinHashRecord: String) {
    context.dataStore.edit { prefs ->
      prefs[Keys.PIN_HASH_RECORD] = pinHashRecord
      prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
      prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] = 0L
    }
  }

  suspend fun setBiometricUnlockEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = enabled
    }
  }

  suspend fun disableAppLock() {
    context.dataStore.edit { prefs ->
      prefs[Keys.APP_LOCK_ENABLED] = false
      prefs[Keys.BIOMETRIC_UNLOCK_ENABLED] = false
      prefs.remove(Keys.PIN_HASH_RECORD)
      prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
      prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] = 0L
    }
  }

  suspend fun recordFailedPinAttempt(attempts: Int, lockedUntilMillis: Long) {
    context.dataStore.edit { prefs ->
      prefs[Keys.FAILED_PIN_ATTEMPTS] = attempts
      prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] = lockedUntilMillis
    }
  }

  suspend fun resetPinAttempts() {
    context.dataStore.edit { prefs ->
      prefs[Keys.FAILED_PIN_ATTEMPTS] = 0
      prefs[Keys.PIN_LOCKED_UNTIL_MILLIS] = 0L
    }
  }

  suspend fun updateBudgetAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.BUDGET_ALERTS_ENABLED] = enabled
    }
  }

  suspend fun updateGoalAlertsEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.GOAL_ALERTS_ENABLED] = enabled
    }
  }

  suspend fun updateDailyReminderEnabled(enabled: Boolean) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DAILY_REMINDER_ENABLED] = enabled
    }
  }

  suspend fun updateBudgetWarningThreshold(percent: Int) {
    context.dataStore.edit { prefs ->
      prefs[Keys.BUDGET_WARNING_THRESHOLD_PERCENT] = AlertThresholdDefaults.sanitizeThreshold(percent)
    }
  }

  suspend fun updateDailyReminderTime(hour24: Int, minute: Int) {
    context.dataStore.edit { prefs ->
      prefs[Keys.DAILY_REMINDER_HOUR] = hour24.coerceIn(0, 23)
      prefs[Keys.DAILY_REMINDER_MINUTE] = minute.coerceIn(0, 59)
    }
  }
}
