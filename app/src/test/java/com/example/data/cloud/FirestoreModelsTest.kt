package com.example.data.cloud

import com.example.data.local.SampleDataSeeder
import com.example.domain.CurrencyOption
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FirestoreModelsTest {

  @Test
  fun stablePositiveLongId_isDeterministicPositiveAndDistinct() {
    val idA1 = stablePositiveLongId("tx_abc_123")
    val idA2 = stablePositiveLongId("tx_abc_123")
    val idB = stablePositiveLongId("tx_xyz_456")

    assertTrue(idA1 > 0L)
    assertTrue(idB > 0L)
    assertEquals(idA1, idA2)
    assertNotEquals(idA1, idB)
  }

  @Test
  fun defaultCategoryDocId_isRulesCompliantAndUniqueForAll30Defaults() {
    val rulesIdPattern = Regex("^[a-zA-Z0-9_-]{1,128}$")
    val defaults = SampleDataSeeder.defaultCategories()
    assertEquals(30, defaults.size)

    val docIds = defaults.map { defaultCategoryDocId(it.type, it.name) }
    assertEquals(defaults.size, docIds.toSet().size)
    docIds.forEach { id ->
      assertTrue("Doc ID '$id' must match Firestore rules isValidId", rulesIdPattern.matches(id))
    }
  }

  @Test
  fun safeEnumDeserializers_handleUnknownServerValuesWithoutThrowing() {
    assertEquals(TransactionType.INCOME, safeTransactionType("INCOME"))
    assertEquals(TransactionType.EXPENSE, safeTransactionType("UNKNOWN_FUTURE_TYPE"))
    assertEquals(PaymentMethod.GCASH, safePaymentMethod("GCASH"))
    assertEquals(PaymentMethod.CASH, safePaymentMethod("CRYPTO_WALLET"))
    assertEquals(GoalStatus.COMPLETED, safeGoalStatus("COMPLETED"))
    assertEquals(GoalStatus.ACTIVE, safeGoalStatus("INVALID_STATUS"))
  }

  @Test
  fun firestoreDocs_mapCleanlyToDomainAndLocalEntities() {
    val ts = Timestamp(1_700_000_000L, 0)
    val profile = UserProfileDoc(
      id = "uid_123",
      userId = "uid_123",
      displayName = "  Maria  ",
      preferredCurrency = "USD",
      onboardingCompleted = true,
      initializedData = true,
      createdAt = ts,
      updatedAt = ts,
    ).toDomain()

    assertEquals("uid_123", profile.userId)
    assertEquals("Maria", profile.displayName)
    assertEquals(CurrencyOption.USD.code, profile.preferredCurrency)
    assertTrue(profile.onboardingCompleted)

    val tx = TransactionDoc(
      id = "tx_1",
      userId = "uid_123",
      title = "Lunch",
      amountCentavos = 25000L,
      type = "EXPENSE",
      category = "Food and Dining",
      dateEpochDay = 20400L,
      paymentMethod = "MAYA",
      notes = "Rice bowl",
      sampleData = false,
      createdAt = ts,
      updatedAt = ts,
    ).toEntity(stableId = 42L)

    assertEquals(42L, tx.id)
    assertEquals(TransactionType.EXPENSE, tx.type)
    assertEquals(PaymentMethod.MAYA, tx.paymentMethod)
    assertFalse(tx.isSampleData)
    assertEquals(1_700_000_000_000L, tx.createdAtMillis)

    val contrib = SavingsContributionDoc(
      id = "c_1",
      userId = "uid_123",
      goalDocId = "g_1",
      amountCentavos = 50000L,
      withdrawal = true,
      note = "Adjustment",
      dateEpochDay = 20401L,
      createdAt = ts,
    ).toEntity(stableId = 7L, stableGoalId = 99L)

    assertEquals(7L, contrib.id)
    assertEquals(99L, contrib.goalId)
    assertTrue(contrib.isWithdrawal)
  }
}
