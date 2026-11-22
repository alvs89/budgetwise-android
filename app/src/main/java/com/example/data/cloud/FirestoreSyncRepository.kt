package com.example.data.cloud

import android.content.Context
import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.LocalGuestSnapshot
import com.example.domain.CurrencyOption
import com.example.domain.GoalStatus
import com.example.domain.YearMonthKey
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Cloud-backed Firestore repository scoped to a single authenticated [userId].
 *
 * Uses [MetadataChanges.INCLUDE] on snapshot listeners combined with Android's
 * [NetworkConnectivityObserver] and an active server handshake probe so the UI never gets stuck
 * reporting "Offline" after sign-out / re-sign-in while online. All active [ListenerRegistration]s
 * are tracked and detached cleanly on [detachAllListeners] during sign-out.
 */
class FirestoreSyncRepository(
  private val db: FirebaseFirestore,
  private val userId: String,
  appContext: Context? = null,
) {
  private val userDocRef: DocumentReference = db.collection("users").document(userId)
  private val transactionsCol = userDocRef.collection("transactions")
  private val budgetsCol = userDocRef.collection("budgets")
  private val goalsCol = userDocRef.collection("goals")
  private val contributionsCol = userDocRef.collection("contributions")
  private val categoriesCol = userDocRef.collection("categories")

  private val initMutex = Mutex()
  private val activeListeners = CopyOnWriteArrayList<ListenerRegistration>()

  private val txIdToDoc = ConcurrentHashMap<Long, String>()
  private val budgetIdToDoc = ConcurrentHashMap<Long, String>()
  private val goalIdToDoc = ConcurrentHashMap<Long, String>()
  private val categoryIdToDoc = ConcurrentHashMap<Long, String>()

  private val isOnlineFlow: Flow<Boolean> = if (appContext != null) {
    NetworkConnectivityObserver.observeIsOnline(appContext)
  } else {
    flowOf(true)
  }

  private val _isProbingServer = MutableStateFlow(false)
  private val _hasServerConfirmed = MutableStateFlow(false)
  private val _hasPendingWrites = MutableStateFlow(false)
  private val _hasSyncError = MutableStateFlow(false)

  private val _lastSyncError = MutableStateFlow<String?>(null)
  val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

  val syncStatus: Flow<CloudSyncStatus> = combine(
    isOnlineFlow,
    _isProbingServer,
    _hasServerConfirmed,
    _hasPendingWrites,
    _hasSyncError,
  ) { isOnline, isProbing, serverConfirmed, hasPendingWrites, hasSyncError ->
    when {
      !isOnline -> CloudSyncStatus.OFFLINE
      hasSyncError && !isProbing -> CloudSyncStatus.ERROR
      isProbing || hasPendingWrites || !serverConfirmed -> CloudSyncStatus.SYNCING
      else -> CloudSyncStatus.SYNCED
    }
  }

  val profileState: Flow<CloudProfileState> = callbackFlow {
    trySend(CloudProfileState.Loading)
    val registration = runCatching {
      userDocRef.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.GET, "users/$userId")
          _hasSyncError.value = true
          _lastSyncError.value = error.localizedMessage ?: "Unable to sync profile."
          trySend(CloudProfileState.Error(error.localizedMessage ?: "Unable to sync profile."))
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          if (!snapshot.exists()) {
            trySend(CloudProfileState.Missing)
          } else {
            val profile = parseUserProfileSafely(snapshot)
            if (profile != null) {
              trySend(CloudProfileState.Loaded(profile))
            } else {
              trySend(CloudProfileState.Missing)
            }
          }
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.GET, "users/$userId")
          trySend(CloudProfileState.Error("Unable to read synced profile."))
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.GET, "users/$userId")
      trySend(CloudProfileState.Error(err.localizedMessage ?: "Unable to connect to cloud profile."))
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  val transactions: Flow<List<TransactionEntity>> = callbackFlow {
    val registration = runCatching {
      transactionsCol.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/transactions")
          _hasSyncError.value = true
          _lastSyncError.value = "Could not load synced transactions."
          trySend(emptyList())
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          val mapped = snapshot.documents.mapNotNull { docSnap ->
            runCatching {
              val doc = docSnap.toObject(
                TransactionDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: return@runCatching null
              val stableId = stablePositiveLongId(docSnap.id)
              txIdToDoc[stableId] = docSnap.id
              doc.toEntity(stableId)
            }.onFailure { err ->
              FirestoreErrorHandler.logFirestoreError(
                err,
                OperationType.GET,
                "users/$userId/transactions/${docSnap.id}",
              )
            }.getOrNull()
          }.sortedWith(
            compareByDescending<TransactionEntity> { it.dateEpochDay }
              .thenByDescending { it.createdAtMillis }
          )
          trySend(mapped)
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/transactions")
          trySend(emptyList())
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/transactions")
      trySend(emptyList())
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  val budgets: Flow<List<BudgetEntity>> = callbackFlow {
    val registration = runCatching {
      budgetsCol.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/budgets")
          _hasSyncError.value = true
          trySend(emptyList())
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          val mapped = snapshot.documents.mapNotNull { docSnap ->
            runCatching {
              val doc = docSnap.toObject(
                BudgetDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: return@runCatching null
              val stableId = stablePositiveLongId(docSnap.id)
              budgetIdToDoc[stableId] = docSnap.id
              doc.toEntity(stableId)
            }.onFailure { err ->
              FirestoreErrorHandler.logFirestoreError(
                err,
                OperationType.GET,
                "users/$userId/budgets/${docSnap.id}",
              )
            }.getOrNull()
          }.sortedWith(
            compareByDescending<BudgetEntity> { it.year }
              .thenByDescending { it.month }
              .thenBy { it.category.lowercase(Locale.US) }
          )
          trySend(mapped)
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/budgets")
          trySend(emptyList())
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/budgets")
      trySend(emptyList())
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  val savingsGoals: Flow<List<SavingsGoalEntity>> = callbackFlow {
    val registration = runCatching {
      goalsCol.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/goals")
          _hasSyncError.value = true
          trySend(emptyList())
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          val mapped = snapshot.documents.mapNotNull { docSnap ->
            runCatching {
              val doc = docSnap.toObject(
                SavingsGoalDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: return@runCatching null
              val stableId = stablePositiveLongId(docSnap.id)
              goalIdToDoc[stableId] = docSnap.id
              doc.toEntity(stableId)
            }.onFailure { err ->
              FirestoreErrorHandler.logFirestoreError(
                err,
                OperationType.GET,
                "users/$userId/goals/${docSnap.id}",
              )
            }.getOrNull()
          }.sortedWith(
            compareByDescending<SavingsGoalEntity> { it.createdDateEpochDay }
              .thenByDescending { it.id }
          )
          trySend(mapped)
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/goals")
          trySend(emptyList())
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/goals")
      trySend(emptyList())
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  val savingsContributions: Flow<List<SavingsContributionEntity>> = callbackFlow {
    val registration = runCatching {
      contributionsCol.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/contributions")
          _hasSyncError.value = true
          trySend(emptyList())
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          val mapped = snapshot.documents.mapNotNull { docSnap ->
            runCatching {
              val doc = docSnap.toObject(
                SavingsContributionDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: return@runCatching null
              if (doc.goalDocId.isBlank()) return@runCatching null
              val stableId = stablePositiveLongId(docSnap.id)
              val stableGoalId = stablePositiveLongId(doc.goalDocId)
              doc.toEntity(stableId = stableId, stableGoalId = stableGoalId)
            }.onFailure { err ->
              FirestoreErrorHandler.logFirestoreError(
                err,
                OperationType.GET,
                "users/$userId/contributions/${docSnap.id}",
              )
            }.getOrNull()
          }.sortedWith(
            compareByDescending<SavingsContributionEntity> { it.dateEpochDay }
              .thenByDescending { it.createdAtMillis }
          )
          trySend(mapped)
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/contributions")
          trySend(emptyList())
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/contributions")
      trySend(emptyList())
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  val categories: Flow<List<CategoryEntity>> = callbackFlow {
    val registration = runCatching {
      categoriesCol.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
        if (error != null) {
          FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/categories")
          _hasSyncError.value = true
          trySend(emptyList())
          return@addSnapshotListener
        }
        if (snapshot == null) return@addSnapshotListener
        runCatching {
          recordSnapshotMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
          val mapped = snapshot.documents.mapNotNull { docSnap ->
            runCatching {
              val doc = docSnap.toObject(
                CategoryDoc::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              ) ?: return@runCatching null
              val stableId = stablePositiveLongId(docSnap.id)
              categoryIdToDoc[stableId] = docSnap.id
              doc.toEntity(stableId)
            }.onFailure { err ->
              FirestoreErrorHandler.logFirestoreError(
                err,
                OperationType.GET,
                "users/$userId/categories/${docSnap.id}",
              )
            }.getOrNull()
          }.sortedWith(categoryComparator())
          trySend(mapped)
        }.onFailure { err ->
          FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/categories")
          trySend(emptyList())
        }
      }
    }.onFailure { err ->
      FirestoreErrorHandler.logFirestoreError(err, OperationType.LIST, "users/$userId/categories")
      trySend(emptyList())
    }.getOrNull()

    if (registration != null) activeListeners.add(registration)
    awaitClose {
      if (registration != null) {
        activeListeners.remove(registration)
        runCatching { registration.remove() }
      }
    }
  }

  private fun parseUserProfileSafely(snapshot: DocumentSnapshot): CloudUserProfile? {
    val mapped = runCatching {
      snapshot.toObject(
        UserProfileDoc::class.java,
        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
      )?.toDomain()
    }.getOrNull()
    if (mapped != null) return mapped

    // Fallback field-by-field reader so null or legacy fields never crash deserialization
    return runCatching {
      val resolvedUid = snapshot.getString("userId")?.takeIf { it.isNotBlank() }
        ?: snapshot.id.ifBlank { userId }
      val displayName = snapshot.getString("displayName")?.trim()?.takeIf { it.isNotBlank() } ?: "Alex"
      val currency = CurrencyOption.fromCode(
        snapshot.getString("preferredCurrency") ?: CurrencyOption.PHP.code
      ).code
      val onboardingCompleted = snapshot.getBoolean("onboardingCompleted") ?: false
      val initializedData = snapshot.getBoolean("initializedData") ?: false
      val photoUrl = snapshot.getString("photoUrl")?.trim().orEmpty()
      val customPhotoBase64 = snapshot.getString("customPhotoBase64")?.trim().orEmpty()
      CloudUserProfile(
        userId = resolvedUid,
        displayName = displayName,
        preferredCurrency = currency,
        onboardingCompleted = onboardingCompleted,
        initializedData = initializedData,
        photoUrl = photoUrl,
        customPhotoBase64 = customPhotoBase64,
      )
    }.getOrNull()
  }

  private fun recordSnapshotMetadata(isFromCache: Boolean, hasPendingWrites: Boolean) {
    _hasPendingWrites.value = hasPendingWrites
    if (!isFromCache) {
      _hasServerConfirmed.value = true
      _hasSyncError.value = false
      _lastSyncError.value = null
    }
  }

  /**
   * Explicitly detaches all active Firestore snapshot listeners before sign-out so no stale
   * listener survives into a subsequent session.
   */
  fun detachAllListeners() {
    val snapshot = activeListeners.toList()
    activeListeners.clear()
    snapshot.forEach { reg -> runCatching { reg.remove() } }
    _hasServerConfirmed.value = false
    _hasPendingWrites.value = false
    _isProbingServer.value = false
    _hasSyncError.value = false
  }

  /**
   * Actively probes the Firestore server (`Source.SERVER` with graceful fallback to `Source.DEFAULT`)
   * and waits for pending writes to confirm live cloud sync status.
   */
  suspend fun verifyServerSync(): Result<Unit> {
    _isProbingServer.value = true
    _hasSyncError.value = false
    return try {
      runCatching { db.waitForPendingWrites().await() }
      val serverSnap = userDocRef.get(Source.SERVER).await()
      if (!serverSnap.metadata.isFromCache) {
        _hasServerConfirmed.value = true
        _hasPendingWrites.value = false
        _hasSyncError.value = false
        _lastSyncError.value = null
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (serverErr: Exception) {
      val fallbackSnap = runCatching { userDocRef.get(Source.DEFAULT).await() }.getOrNull()
      if (fallbackSnap != null && !fallbackSnap.metadata.isFromCache) {
        _hasServerConfirmed.value = true
        _hasSyncError.value = false
        Result.success(Unit)
      } else {
        FirestoreErrorHandler.logFirestoreError(serverErr, OperationType.GET, "users/$userId")
        Result.failure(serverErr)
      }
    } finally {
      _isProbingServer.value = false
    }
  }

  private fun categoryComparator(): Comparator<CategoryEntity> = compareBy(
    { entity ->
      val normalized = entity.name.trim().lowercase(Locale.US)
      if (normalized == "other" || normalized == "others" || normalized.startsWith("other ")) 1 else 0
    },
    { if (it.isDefault) 0 else 1 },
    { it.name.lowercase(Locale.US) },
  )

  /**
   * Initializes `/users/{userId}` and the 30 default categories on first sign-in with a clean
   * `₱0.00` ledger (zero demo data). Never overwrites a custom display name or avatar saved by
   * a returning user.
   */
  suspend fun ensureProfileInitialized(
    googleDisplayName: String?,
    googlePhotoUrl: String?,
    defaultCurrency: String = CurrencyOption.PHP.code,
  ): Result<CloudUserProfile> = runCloudCatching(OperationType.WRITE, "users/$userId") {
    initMutex.withLock {
      val snapshot = runCatching { userDocRef.get(Source.SERVER).await() }
        .getOrElse { userDocRef.get(Source.DEFAULT).await() }
      if (!snapshot.metadata.isFromCache) {
        _hasServerConfirmed.value = true
      }

      val cleanGoogleName = googleDisplayName?.trim()?.takeIf { it.isNotBlank() }?.take(60)
      val cleanGooglePhoto = googlePhotoUrl?.trim()?.takeIf { it.isNotBlank() }?.take(1024).orEmpty()

      if (snapshot.exists()) {
        val existingProfile = parseUserProfileSafely(snapshot)
        if (existingProfile != null) {
          val updates = mutableMapOf<String, Any>()
          // Only backfill Google photoUrl on legacy profile docs that never stored the field yet
          if (!snapshot.contains("photoUrl") && cleanGooglePhoto.isNotEmpty()) {
            updates["photoUrl"] = cleanGooglePhoto
          }
          if (!snapshot.contains("customPhotoBase64")) {
            updates["customPhotoBase64"] = ""
          }
          if (updates.isNotEmpty()) {
            updates["updatedAt"] = FieldValue.serverTimestamp()
            runCatching { userDocRef.update(updates).await() }
          }
          ensureDefaultCategoriesExistInternal()
          return@withLock if (!snapshot.contains("photoUrl") && cleanGooglePhoto.isNotEmpty()) {
            existingProfile.copy(photoUrl = cleanGooglePhoto)
          } else {
            existingProfile
          }
        }
      }

      val initialDisplayName = sanitizeDisplayName(cleanGoogleName ?: "Alex")
      val cleanCurrency = CurrencyOption.fromCode(defaultCurrency).code
      val profileMap = mapOf(
        "userId" to userId,
        "displayName" to initialDisplayName,
        "preferredCurrency" to cleanCurrency,
        "onboardingCompleted" to false,
        "initializedData" to true,
        "photoUrl" to cleanGooglePhoto,
        "customPhotoBase64" to "",
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      userDocRef.set(profileMap).await()
      ensureDefaultCategoriesExistInternal()
      _hasServerConfirmed.value = true
      CloudUserProfile(
        userId = userId,
        displayName = initialDisplayName,
        preferredCurrency = cleanCurrency,
        onboardingCompleted = false,
        initializedData = true,
        photoUrl = cleanGooglePhoto,
        customPhotoBase64 = "",
      )
    }
  }

  suspend fun markSpotlightTourCompleted(): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      userDocRef.update(
        mapOf(
          "onboardingCompleted" to true,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun updateDisplayName(displayName: String): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      val cleanName = sanitizeDisplayName(displayName)
      userDocRef.update(
        mapOf(
          "displayName" to cleanName,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun updatePreferredCurrency(currencyCode: String): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      val cleanCode = CurrencyOption.fromCode(currencyCode).code
      userDocRef.update(
        mapOf(
          "preferredCurrency" to cleanCode,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun updateCustomPhotoBase64(base64Jpeg: String): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      require(base64Jpeg.isNotBlank() && base64Jpeg.length <= 250_000) {
        "Profile photo is too large."
      }
      userDocRef.update(
        mapOf(
          "customPhotoBase64" to base64Jpeg,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun revertToGooglePhoto(googlePhotoUrl: String?): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      userDocRef.update(
        mapOf(
          "photoUrl" to googlePhotoUrl.orEmpty().trim().take(1024),
          "customPhotoBase64" to "",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun removeProfilePhoto(): Result<Unit> =
    runCloudCatching(OperationType.UPDATE, "users/$userId") {
      userDocRef.update(
        mapOf(
          "photoUrl" to "",
          "customPhotoBase64" to "",
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  // ---------------------------------------------------------------------------
  // Transactions
  // ---------------------------------------------------------------------------
  suspend fun saveTransaction(transaction: TransactionEntity): Result<Unit> =
    runCloudCatching(
      if (transaction.id == 0L) OperationType.CREATE else OperationType.UPDATE,
      "users/$userId/transactions",
    ) {
      val cleanTitle = transaction.title.trim().take(120)
      val cleanCategory = transaction.category.trim().take(80)
      val cleanNotes = transaction.notes.trim().take(500)
      require(cleanTitle.isNotEmpty()) { "Transaction title is required." }
      require(cleanCategory.isNotEmpty()) { "Transaction category is required." }
      require(transaction.amountCentavos > 0L) { "Transaction amount must be positive." }

      val existingDocId = if (transaction.id != 0L) txIdToDoc[transaction.id] else null
      if (existingDocId == null) {
        transactionsCol.document().set(
          mapOf(
            "userId" to userId,
            "title" to cleanTitle,
            "amountCentavos" to transaction.amountCentavos,
            "type" to transaction.type.name,
            "category" to cleanCategory,
            "dateEpochDay" to transaction.dateEpochDay,
            "paymentMethod" to transaction.paymentMethod.name,
            "notes" to cleanNotes,
            "sampleData" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        transactionsCol.document(existingDocId).update(
          mapOf(
            "title" to cleanTitle,
            "amountCentavos" to transaction.amountCentavos,
            "type" to transaction.type.name,
            "category" to cleanCategory,
            "dateEpochDay" to transaction.dateEpochDay,
            "paymentMethod" to transaction.paymentMethod.name,
            "notes" to cleanNotes,
            "sampleData" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    }

  suspend fun deleteTransaction(transaction: TransactionEntity): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId/transactions") {
      val docId = txIdToDoc[transaction.id] ?: return@runCloudCatching
      transactionsCol.document(docId).delete().await()
    }

  // ---------------------------------------------------------------------------
  // Monthly Budgets
  // ---------------------------------------------------------------------------
  suspend fun saveBudget(budget: BudgetEntity): Result<Boolean> =
    runCloudCatching(OperationType.WRITE, "users/$userId/budgets") {
      val cleanCategory = budget.category.trim().take(80)
      require(cleanCategory.isNotEmpty()) { "Budget category is required." }
      require(budget.limitCentavos > 0L) { "Budget limit must be positive." }
      require(budget.year in 2000..2100 && budget.month in 1..12) { "Invalid budget month." }

      val allBudgetSnaps = budgetsCol.get().await().documents
      val existingForSlot = allBudgetSnaps.firstOrNull { snap ->
        snap.getString("category") == cleanCategory &&
          snap.getLong("year")?.toInt() == budget.year &&
          snap.getLong("month")?.toInt() == budget.month
      }
      val existingDocId = if (budget.id != 0L) budgetIdToDoc[budget.id] else null

      if (existingDocId == null) {
        if (existingForSlot != null) {
          existingForSlot.reference.update(
            mapOf(
              "limitCentavos" to budget.limitCentavos,
              "sampleData" to false,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
          false
        } else {
          budgetsCol.document().set(
            mapOf(
              "userId" to userId,
              "category" to cleanCategory,
              "limitCentavos" to budget.limitCentavos,
              "year" to budget.year,
              "month" to budget.month,
              "sampleData" to false,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
          true
        }
      } else {
        if (existingForSlot != null && existingForSlot.id != existingDocId) {
          val batch = db.batch()
          batch.delete(budgetsCol.document(existingDocId))
          batch.update(
            existingForSlot.reference,
            mapOf(
              "limitCentavos" to budget.limitCentavos,
              "sampleData" to false,
              "updatedAt" to FieldValue.serverTimestamp(),
            ),
          )
          batch.commit().await()
        } else {
          budgetsCol.document(existingDocId).update(
            mapOf(
              "category" to cleanCategory,
              "limitCentavos" to budget.limitCentavos,
              "year" to budget.year,
              "month" to budget.month,
              "sampleData" to false,
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          ).await()
        }
        false
      }
    }

  suspend fun deleteBudget(budget: BudgetEntity): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId/budgets") {
      val docId = budgetIdToDoc[budget.id] ?: return@runCloudCatching
      budgetsCol.document(docId).delete().await()
    }

  // ---------------------------------------------------------------------------
  // Savings Goals & Contributions
  // ---------------------------------------------------------------------------
  suspend fun saveGoal(goal: SavingsGoalEntity): Result<Unit> =
    runCloudCatching(
      if (goal.id == 0L) OperationType.CREATE else OperationType.UPDATE,
      "users/$userId/goals",
    ) {
      val cleanName = goal.name.trim().take(100)
      require(cleanName.isNotEmpty()) { "Goal name is required." }
      require(goal.targetCentavos > 0L) { "Goal target must be positive." }
      val safeSaved = goal.currentSavedCentavos.coerceAtLeast(0L)

      val existingDocId = if (goal.id != 0L) goalIdToDoc[goal.id] else null
      if (existingDocId == null) {
        goalsCol.document().set(
          mapOf(
            "userId" to userId,
            "name" to cleanName,
            "targetCentavos" to goal.targetCentavos,
            "currentSavedCentavos" to safeSaved,
            "targetDateEpochDay" to goal.targetDateEpochDay,
            "createdDateEpochDay" to goal.createdDateEpochDay,
            "status" to goal.status.name,
            "sampleData" to false,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        goalsCol.document(existingDocId).update(
          mapOf(
            "name" to cleanName,
            "targetCentavos" to goal.targetCentavos,
            "currentSavedCentavos" to safeSaved,
            "targetDateEpochDay" to goal.targetDateEpochDay,
            "createdDateEpochDay" to goal.createdDateEpochDay,
            "status" to goal.status.name,
            "sampleData" to false,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    }

  suspend fun deleteGoal(goal: SavingsGoalEntity): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId/goals") {
      val goalDocId = goalIdToDoc[goal.id] ?: return@runCloudCatching
      val linkedContribs = contributionsCol.whereEqualTo("goalDocId", goalDocId).get().await()
      val refsToDelete = buildList {
        addAll(linkedContribs.documents.map { it.reference })
        add(goalsCol.document(goalDocId))
      }
      commitBatchDeletes(refsToDelete)
    }

  suspend fun recordGoalContribution(
    goalId: Long,
    amountCentavos: Long,
    isWithdrawal: Boolean,
    note: String,
    dateEpochDay: Long,
  ): Result<Unit> = runCloudCatching(OperationType.WRITE, "users/$userId/contributions") {
    require(amountCentavos > 0L) { "Contribution amount must be positive." }
    val goalDocId = goalIdToDoc[goalId] ?: error("Savings goal not found.")
    val goalRef = goalsCol.document(goalDocId)
    val goalSnap = goalRef.get().await()
    val goalDoc = goalSnap.toObject(
      SavingsGoalDoc::class.java,
      DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
    ) ?: error("Savings goal not found.")

    val delta = if (isWithdrawal) -amountCentavos else amountCentavos
    val newSaved = (goalDoc.currentSavedCentavos + delta).coerceAtLeast(0L)
    val currentStatus = safeGoalStatus(goalDoc.status)
    val updatedStatus = when {
      newSaved >= goalDoc.targetCentavos -> GoalStatus.COMPLETED
      currentStatus == GoalStatus.COMPLETED && newSaved < goalDoc.targetCentavos -> GoalStatus.ACTIVE
      else -> currentStatus
    }

    val batch = db.batch()
    val contribRef = contributionsCol.document()
    batch.set(
      contribRef,
      mapOf(
        "userId" to userId,
        "goalDocId" to goalDocId,
        "amountCentavos" to amountCentavos,
        "withdrawal" to isWithdrawal,
        "note" to note.trim().take(300),
        "dateEpochDay" to dateEpochDay,
        "createdAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.update(
      goalRef,
      mapOf(
        "currentSavedCentavos" to newSaved,
        "status" to updatedStatus.name,
        "updatedAt" to FieldValue.serverTimestamp(),
      ),
    )
    batch.commit().await()
  }

  // ---------------------------------------------------------------------------
  // Categories
  // ---------------------------------------------------------------------------
  suspend fun addCategory(category: CategoryEntity): Result<Unit> =
    runCloudCatching(OperationType.CREATE, "users/$userId/categories") {
      val cleanName = category.name.trim().take(80)
      require(cleanName.isNotEmpty()) { "Category name is required." }

      val existing = categoriesCol.get().await().documents.any { snap ->
        snap.getString("name").equals(cleanName, ignoreCase = true) &&
          snap.getString("type").equals(category.type.name, ignoreCase = true)
      }
      if (existing) return@runCloudCatching

      val docRef = if (category.isDefault) {
        categoriesCol.document(defaultCategoryDocId(category.type, cleanName))
      } else {
        categoriesCol.document()
      }
      docRef.set(
        mapOf(
          "userId" to userId,
          "name" to cleanName,
          "type" to category.type.name,
          "iconKey" to category.iconKey.trim().ifBlank { "category" }.take(60),
          "colorHex" to (category.colorHex and 0xFFFFFFFFL),
          "defaultCategory" to category.isDefault,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      ).await()
    }

  suspend fun updateCategory(
    existing: CategoryEntity,
    newName: String,
    newIconKey: String,
    newColorHex: Long,
  ): Result<Unit> = runCloudCatching(OperationType.UPDATE, "users/$userId/categories") {
    val docId = categoryIdToDoc[existing.id] ?: return@runCloudCatching
    val cleanName = if (existing.isDefault) {
      existing.name
    } else {
      newName.trim().ifEmpty { existing.name }.take(80)
    }

    val ops = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
    ops += categoriesCol.document(docId) to mapOf(
      "name" to cleanName,
      "iconKey" to newIconKey.trim().ifBlank { "category" }.take(60),
      "colorHex" to (newColorHex and 0xFFFFFFFFL),
      "updatedAt" to FieldValue.serverTimestamp(),
    )

    if (cleanName != existing.name) {
      val matchingTx = transactionsCol.whereEqualTo("category", existing.name).get().await()
      matchingTx.documents.forEach { snap ->
        ops += snap.reference to mapOf(
          "category" to cleanName,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      }
      val matchingBudgets = budgetsCol.whereEqualTo("category", existing.name).get().await()
      matchingBudgets.documents.forEach { snap ->
        ops += snap.reference to mapOf(
          "category" to cleanName,
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      }
    }

    commitBatchUpdates(ops)
  }

  suspend fun deleteCategory(category: CategoryEntity): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId/categories") {
      if (category.isDefault) return@runCloudCatching
      val docId = categoryIdToDoc[category.id] ?: return@runCloudCatching
      categoriesCol.document(docId).delete().await()
    }

  // ---------------------------------------------------------------------------
  // Clear All Financial Records (Production)
  // ---------------------------------------------------------------------------
  suspend fun clearAllFinancialRecords(): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId") {
      initMutex.withLock {
        val refs = mutableListOf<DocumentReference>()
        refs += transactionsCol.get().await().documents.map { it.reference }
        refs += budgetsCol.get().await().documents.map { it.reference }
        refs += contributionsCol.get().await().documents.map { it.reference }
        refs += goalsCol.get().await().documents.map { it.reference }
        refs += categoriesCol.get().await().documents
          .filter { it.getBoolean("defaultCategory") != true }
          .map { it.reference }
        commitBatchDeletes(refs)
        ensureDefaultCategoriesExistInternal()
      }
    }

  // ---------------------------------------------------------------------------
  // Demonstration Dataset Seeding (Idempotent Cloud Batch Sync)
  // ---------------------------------------------------------------------------
  /**
   * Populates the signed-in user's Firestore collections with a practical, realistic 6-month
   * demonstration dataset (transactions, multi-month budgets, savings goals, and contributions).
   *
   * Uses deterministic rules-compliant document IDs and skips any document ID or budget slot that
   * already exists so re-running never triggers disallowed updates or duplicates existing records.
   * Returns the number of newly written Firestore documents.
   */
  suspend fun seedDemonstrationData(
    referenceMonth: YearMonthKey = YearMonthKey.current(),
  ): Result<Int> = runCloudCatching(OperationType.WRITE, "users/$userId") {
    initMutex.withLock {
      ensureDefaultCategoriesExistInternal()

      val dataset = SampleDataSeeder.demonstrationDataset(referenceMonth)
      val existingTxIds = transactionsCol.get().await().documents.map { it.id }.toSet()
      val existingBudgetSnaps = budgetsCol.get().await().documents
      val existingBudgetIds = existingBudgetSnaps.map { it.id }.toSet()
      val existingBudgetSlots = existingBudgetSnaps.mapNotNull { snap ->
        val cat = snap.getString("category")?.trim()?.lowercase(Locale.US) ?: return@mapNotNull null
        val y = snap.getLong("year")?.toInt() ?: return@mapNotNull null
        val m = snap.getLong("month")?.toInt() ?: return@mapNotNull null
        Triple(cat, y, m)
      }.toSet()
      val existingGoalIds = goalsCol.get().await().documents.map { it.id }.toSet()
      val existingContribIds = contributionsCol.get().await().documents.map { it.id }.toSet()

      val entries = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

      dataset.transactions.forEach { record ->
        if (record.docId !in existingTxIds) {
          val tx = record.entity
          entries += transactionsCol.document(record.docId) to mapOf(
            "userId" to userId,
            "title" to tx.title.trim().take(120),
            "amountCentavos" to tx.amountCentavos,
            "type" to tx.type.name,
            "category" to tx.category.trim().take(80),
            "dateEpochDay" to tx.dateEpochDay,
            "paymentMethod" to tx.paymentMethod.name,
            "notes" to tx.notes.trim().take(500),
            "sampleData" to tx.isSampleData,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        }
      }

      dataset.budgets.forEach { record ->
        val budget = record.entity
        val slot = Triple(budget.category.trim().lowercase(Locale.US), budget.year, budget.month)
        if (record.docId !in existingBudgetIds && slot !in existingBudgetSlots) {
          entries += budgetsCol.document(record.docId) to mapOf(
            "userId" to userId,
            "category" to budget.category.trim().take(80),
            "limitCentavos" to budget.limitCentavos,
            "year" to budget.year,
            "month" to budget.month,
            "sampleData" to budget.isSampleData,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        }
      }

      dataset.goals.forEach { goalRecord ->
        val goal = goalRecord.entity
        val goalExistsOrWillExist = goalRecord.docId in existingGoalIds || true
        if (goalRecord.docId !in existingGoalIds) {
          entries += goalsCol.document(goalRecord.docId) to mapOf(
            "userId" to userId,
            "name" to goal.name.trim().take(100),
            "targetCentavos" to goal.targetCentavos,
            "currentSavedCentavos" to goal.currentSavedCentavos.coerceAtLeast(0L),
            "targetDateEpochDay" to goal.targetDateEpochDay,
            "createdDateEpochDay" to goal.createdDateEpochDay,
            "status" to goal.status.name,
            "sampleData" to goal.isSampleData,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        }
        if (goalExistsOrWillExist) {
          goalRecord.contributions.forEach { contrib ->
            if (contrib.docId !in existingContribIds) {
              entries += contributionsCol.document(contrib.docId) to mapOf(
                "userId" to userId,
                "goalDocId" to goalRecord.docId,
                "amountCentavos" to contrib.amountCentavos,
                "withdrawal" to contrib.isWithdrawal,
                "note" to contrib.note.trim().take(300),
                "dateEpochDay" to contrib.dateEpochDay,
                "createdAt" to FieldValue.serverTimestamp(),
              )
            }
          }
        }
      }

      if (entries.isNotEmpty()) {
        commitBatchSets(entries)
        _hasServerConfirmed.value = true
      }
      entries.size
    }
  }

  // ---------------------------------------------------------------------------
  // Local Guest -> Cloud Account Migration
  // ---------------------------------------------------------------------------
  /**
   * Returns true if the signed-in user's cloud account already contains at least one transaction,
   * budget, or savings goal so the app can present a clear non-destructive merge dialog.
   */
  suspend fun hasExistingFinancialRecords(): Boolean = try {
    val hasTx = transactionsCol.limit(1).get().await().documents.isNotEmpty()
    if (hasTx) {
      true
    } else {
      val hasBudget = budgetsCol.limit(1).get().await().documents.isNotEmpty()
      if (hasBudget) {
        true
      } else {
        goalsCol.limit(1).get().await().documents.isNotEmpty()
      }
    }
  } catch (e: CancellationException) {
    throw e
  } catch (_: Exception) {
    false
  }

  /**
   * Migrates locally stored offline guest records into the authenticated user's Firestore
   * subcollections without overwriting existing cloud budgets or duplicating custom categories.
   */
  suspend fun migrateLocalGuestSnapshot(snapshot: LocalGuestSnapshot): Result<Int> =
    runCloudCatching(OperationType.WRITE, "users/$userId") {
      initMutex.withLock {
        ensureDefaultCategoriesExistInternal()

        val existingCategoryKeys = categoriesCol.get().await().documents.mapNotNull { snap ->
          val name = snap.getString("name")?.trim()?.lowercase(Locale.US) ?: return@mapNotNull null
          val type = snap.getString("type")?.trim()?.uppercase(Locale.US) ?: return@mapNotNull null
          name to type
        }.toSet()

        val existingBudgetSlots = budgetsCol.get().await().documents.mapNotNull { snap ->
          val cat = snap.getString("category")?.trim()?.lowercase(Locale.US) ?: return@mapNotNull null
          val y = snap.getLong("year")?.toInt() ?: return@mapNotNull null
          val m = snap.getLong("month")?.toInt() ?: return@mapNotNull null
          Triple(cat, y, m)
        }.toSet()

        val entries = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

        snapshot.customCategories.forEach { category ->
          val cleanName = category.name.trim().take(80)
          if (cleanName.isNotEmpty()) {
            val key = cleanName.lowercase(Locale.US) to category.type.name
            if (key !in existingCategoryKeys) {
              entries += categoriesCol.document() to mapOf(
                "userId" to userId,
                "name" to cleanName,
                "type" to category.type.name,
                "iconKey" to category.iconKey.trim().ifBlank { "category" }.take(60),
                "colorHex" to (category.colorHex and 0xFFFFFFFFL),
                "defaultCategory" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp(),
              )
            }
          }
        }

        snapshot.transactions.forEach { tx ->
          val cleanTitle = tx.title.trim().take(120)
          val cleanCategory = tx.category.trim().take(80)
          if (cleanTitle.isNotEmpty() && cleanCategory.isNotEmpty() && tx.amountCentavos > 0L) {
            entries += transactionsCol.document() to mapOf(
              "userId" to userId,
              "title" to cleanTitle,
              "amountCentavos" to tx.amountCentavos,
              "type" to tx.type.name,
              "category" to cleanCategory,
              "dateEpochDay" to tx.dateEpochDay,
              "paymentMethod" to tx.paymentMethod.name,
              "notes" to tx.notes.trim().take(500),
              "sampleData" to tx.isSampleData,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          }
        }

        val addedBudgetSlots = mutableSetOf<Triple<String, Int, Int>>()
        snapshot.budgets.forEach { budget ->
          val cleanCategory = budget.category.trim().take(80)
          val slot = Triple(cleanCategory.lowercase(Locale.US), budget.year, budget.month)
          if (
            cleanCategory.isNotEmpty() &&
            budget.limitCentavos > 0L &&
            budget.year in 2000..2100 &&
            budget.month in 1..12 &&
            slot !in existingBudgetSlots &&
            addedBudgetSlots.add(slot)
          ) {
            entries += budgetsCol.document() to mapOf(
              "userId" to userId,
              "category" to cleanCategory,
              "limitCentavos" to budget.limitCentavos,
              "year" to budget.year,
              "month" to budget.month,
              "sampleData" to budget.isSampleData,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          }
        }

        val localGoalIdToCloudDocId = mutableMapOf<Long, String>()
        snapshot.goals.forEach { goal ->
          val cleanName = goal.name.trim().take(100)
          if (cleanName.isNotEmpty() && goal.targetCentavos > 0L) {
            val goalRef = goalsCol.document()
            localGoalIdToCloudDocId[goal.id] = goalRef.id
            entries += goalRef to mapOf(
              "userId" to userId,
              "name" to cleanName,
              "targetCentavos" to goal.targetCentavos,
              "currentSavedCentavos" to goal.currentSavedCentavos.coerceAtLeast(0L),
              "targetDateEpochDay" to goal.targetDateEpochDay,
              "createdDateEpochDay" to goal.createdDateEpochDay,
              "status" to goal.status.name,
              "sampleData" to goal.isSampleData,
              "createdAt" to FieldValue.serverTimestamp(),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          }
        }

        snapshot.contributions.forEach { contrib ->
          val cloudGoalDocId = localGoalIdToCloudDocId[contrib.goalId]
          if (cloudGoalDocId != null && contrib.amountCentavos > 0L) {
            entries += contributionsCol.document() to mapOf(
              "userId" to userId,
              "goalDocId" to cloudGoalDocId,
              "amountCentavos" to contrib.amountCentavos,
              "withdrawal" to contrib.isWithdrawal,
              "note" to contrib.note.trim().take(300),
              "dateEpochDay" to contrib.dateEpochDay,
              "createdAt" to FieldValue.serverTimestamp(),
            )
          }
        }

        if (entries.isNotEmpty()) {
          commitBatchSets(entries)
          _hasServerConfirmed.value = true
        }
        entries.size
      }
    }

  // ---------------------------------------------------------------------------
  // Internal Batch Helpers
  // ---------------------------------------------------------------------------
  private suspend fun ensureDefaultCategoriesExistInternal() {
    val existingIds = categoriesCol.get().await().documents.map { it.id }.toSet()
    val missingSets = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()
    SampleDataSeeder.defaultCategories().forEach { category ->
      val docId = defaultCategoryDocId(category.type, category.name)
      if (docId !in existingIds) {
        missingSets += categoriesCol.document(docId) to mapOf(
          "userId" to userId,
          "name" to category.name,
          "type" to category.type.name,
          "iconKey" to category.iconKey,
          "colorHex" to (category.colorHex and 0xFFFFFFFFL),
          "defaultCategory" to true,
          "createdAt" to FieldValue.serverTimestamp(),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      }
    }
    commitBatchSets(missingSets)
  }

  private suspend fun commitBatchSets(entries: List<Pair<DocumentReference, Map<String, Any>>>) {
    entries.chunked(MAX_BATCH_OPS).forEach { chunk ->
      val batch = db.batch()
      chunk.forEach { (ref, data) -> batch.set(ref, data) }
      batch.commit().await()
    }
  }

  private suspend fun commitBatchUpdates(entries: List<Pair<DocumentReference, Map<String, Any>>>) {
    entries.chunked(MAX_BATCH_OPS).forEach { chunk ->
      val batch = db.batch()
      chunk.forEach { (ref, data) -> batch.update(ref, data) }
      batch.commit().await()
    }
  }

  private suspend fun commitBatchDeletes(refs: List<DocumentReference>) {
    refs.chunked(MAX_BATCH_OPS).forEach { chunk ->
      val batch = db.batch()
      chunk.forEach { ref -> batch.delete(ref) }
      batch.commit().await()
    }
  }

  private suspend inline fun <T> runCloudCatching(
    operationType: OperationType,
    path: String,
    block: () -> T,
  ): Result<T> = try {
    Result.success(block())
  } catch (e: CancellationException) {
    throw e
  } catch (e: Exception) {
    FirestoreErrorHandler.logFirestoreError(e, operationType, path)
    _hasSyncError.value = true
    _lastSyncError.value = e.localizedMessage ?: "Cloud sync error."
    Result.failure(e)
  }

  private fun sanitizeDisplayName(name: String): String =
    name.trim().ifBlank { "Alex" }.take(60)

  companion object {
    private const val MAX_BATCH_OPS = 400
  }
}
