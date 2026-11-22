package com.example.data.cloud

import androidx.annotation.Keep
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.CurrencyOption
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import java.util.Locale

enum class CloudSyncStatus {
  SYNCING,
  SYNCED,
  OFFLINE,
  ERROR,
}

sealed interface CloudProfileState {
  data object Loading : CloudProfileState
  data object Missing : CloudProfileState
  data class Loaded(val profile: CloudUserProfile) : CloudProfileState
  data class Error(val message: String) : CloudProfileState
}

data class CloudUserProfile(
  val userId: String,
  val displayName: String,
  val preferredCurrency: String,
  val onboardingCompleted: Boolean,
  val initializedData: Boolean,
  val photoUrl: String = "",
  val customPhotoBase64: String = "",
)

@Keep
@IgnoreExtraProperties
data class UserProfileDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "Alex",
  val preferredCurrency: String = CurrencyOption.PHP.code,
  val onboardingCompleted: Boolean = false,
  val initializedData: Boolean = false,
  val photoUrl: String = "",
  val customPhotoBase64: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toDomain(): CloudUserProfile = CloudUserProfile(
    userId = userId.ifBlank { id },
    displayName = displayName.trim().ifBlank { "Alex" },
    preferredCurrency = CurrencyOption.fromCode(preferredCurrency).code,
    onboardingCompleted = onboardingCompleted,
    initializedData = initializedData,
    photoUrl = photoUrl.trim(),
    customPhotoBase64 = customPhotoBase64.trim(),
  )
}

@Keep
@IgnoreExtraProperties
data class TransactionDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val title: String = "",
  val amountCentavos: Long = 0L,
  val type: String = TransactionType.EXPENSE.name,
  val category: String = "",
  val dateEpochDay: Long = 0L,
  val paymentMethod: String = PaymentMethod.CASH.name,
  val notes: String = "",
  val sampleData: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(stableId: Long): TransactionEntity = TransactionEntity(
    id = stableId,
    title = title,
    amountCentavos = amountCentavos.coerceAtLeast(0L),
    type = safeTransactionType(type),
    category = category,
    dateEpochDay = dateEpochDay,
    paymentMethod = safePaymentMethod(paymentMethod),
    notes = notes,
    createdAtMillis = createdAt?.toDate()?.time ?: 0L,
    isSampleData = sampleData,
  )
}

@Keep
@IgnoreExtraProperties
data class BudgetDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val category: String = "",
  val limitCentavos: Long = 0L,
  val year: Int = 2026,
  val month: Int = 1,
  val sampleData: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(stableId: Long): BudgetEntity = BudgetEntity(
    id = stableId,
    category = category,
    limitCentavos = limitCentavos.coerceAtLeast(0L),
    year = year.coerceIn(2000, 2100),
    month = month.coerceIn(1, 12),
    isSampleData = sampleData,
  )
}

@Keep
@IgnoreExtraProperties
data class SavingsGoalDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val targetCentavos: Long = 0L,
  val currentSavedCentavos: Long = 0L,
  val targetDateEpochDay: Long = 0L,
  val createdDateEpochDay: Long = 0L,
  val status: String = GoalStatus.ACTIVE.name,
  val sampleData: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(stableId: Long): SavingsGoalEntity = SavingsGoalEntity(
    id = stableId,
    name = name,
    targetCentavos = targetCentavos.coerceAtLeast(0L),
    currentSavedCentavos = currentSavedCentavos.coerceAtLeast(0L),
    targetDateEpochDay = targetDateEpochDay,
    createdDateEpochDay = createdDateEpochDay,
    status = safeGoalStatus(status),
    isSampleData = sampleData,
  )
}

@Keep
@IgnoreExtraProperties
data class SavingsContributionDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val goalDocId: String = "",
  val amountCentavos: Long = 0L,
  val withdrawal: Boolean = false,
  val note: String = "",
  val dateEpochDay: Long = 0L,
  @ServerTimestamp val createdAt: Timestamp? = null,
) {
  fun toEntity(stableId: Long, stableGoalId: Long): SavingsContributionEntity =
    SavingsContributionEntity(
      id = stableId,
      goalId = stableGoalId,
      amountCentavos = amountCentavos.coerceAtLeast(0L),
      isWithdrawal = withdrawal,
      note = note,
      dateEpochDay = dateEpochDay,
      createdAtMillis = createdAt?.toDate()?.time ?: 0L,
    )
}

@Keep
@IgnoreExtraProperties
data class CategoryDoc(
  @DocumentId val id: String = "",
  val userId: String = "",
  val name: String = "",
  val type: String = TransactionType.EXPENSE.name,
  val iconKey: String = "category",
  val colorHex: Long = 0xFF64748B,
  val defaultCategory: Boolean = false,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
) {
  fun toEntity(stableId: Long): CategoryEntity = CategoryEntity(
    id = stableId,
    name = name,
    type = safeTransactionType(type),
    iconKey = iconKey.ifBlank { "category" },
    colorHex = colorHex,
    isDefault = defaultCategory,
  )
}

fun safeTransactionType(raw: String?): TransactionType =
  TransactionType.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    ?: TransactionType.EXPENSE

fun safePaymentMethod(raw: String?): PaymentMethod =
  PaymentMethod.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    ?: PaymentMethod.CASH

fun safeGoalStatus(raw: String?): GoalStatus =
  GoalStatus.entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
    ?: GoalStatus.ACTIVE

/**
 * Deterministic positive 63-bit ID derived from a Firestore document ID (FNV-1a 64-bit) so
 * Compose list keys, domain summaries, and editors keep stable non-zero [Long] identifiers.
 */
fun stablePositiveLongId(docId: String): Long {
  var hash = -3750763034362895579L // 0xcbf29ce484222325
  for (i in docId.indices) {
    hash = hash xor docId[i].code.toLong()
    hash *= 1099511628211L // 0x100000001b3
  }
  val positive = hash and Long.MAX_VALUE
  return if (positive == 0L) 1L else positive
}

/**
 * Deterministic, rules-compliant document ID (`^[a-zA-Z0-9_-]+$`) for built-in default categories
 * so concurrent first-time initialization across devices never creates duplicate default entries.
 */
fun defaultCategoryDocId(type: TransactionType, name: String): String {
  val slug = name.lowercase(Locale.US)
    .replace(Regex("[^a-z0-9]+"), "_")
    .trim('_')
    .ifBlank { "other" }
  return "cat_${type.name.lowercase(Locale.US)}_$slug".take(100)
}
