package com.example.data.cloud

import com.example.data.local.SampleDataSeeder
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.CurrencyOption
import com.example.domain.GoalStatus
import com.example.domain.YearMonthKey
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

/**
 * Cloud-backed Firestore repository scoped to a single authenticated [userId].
 *
 * All synced financial records live under `/users/{userId}/...` in BudgetWise's named Firestore
 * database. Offline persistence is handled natively by the Firestore SDK, and every snapshot read
 * uses [DocumentSnapshot.ServerTimestampBehavior.ESTIMATE] for smooth optimistic UI updates.
 */
class FirestoreSyncRepository(
  private val db: FirebaseFirestore,
  private val userId: String,
) {
  private val userDocRef: DocumentReference = db.collection("users").document(userId)
  private val transactionsCol = userDocRef.collection("transactions")
  private val budgetsCol = userDocRef.collection("budgets")
  private val goalsCol = userDocRef.collection("goals")
  private val contributionsCol = userDocRef.collection("contributions")
  private val categoriesCol = userDocRef.collection("categories")

  private val initMutex = Mutex()

  private val txIdToDoc = ConcurrentHashMap<Long, String>()
  private val budgetIdToDoc = ConcurrentHashMap<Long, String>()
  private val goalIdToDoc = ConcurrentHashMap<Long, String>()
  private val categoryIdToDoc = ConcurrentHashMap<Long, String>()

  private val _syncStatus = MutableStateFlow(CloudSyncStatus.SYNCING)
  val syncStatus: StateFlow<CloudSyncStatus> = _syncStatus.asStateFlow()

  private val _lastSyncError = MutableStateFlow<String?>(null)
  val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

  val profileState: Flow<CloudProfileState> = userDocRef.snapshots()
    .map<DocumentSnapshot, CloudProfileState> { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      if (!snapshot.exists()) {
        CloudProfileState.Missing
      } else {
        val doc = snapshot.toObject(
          UserProfileDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )
        if (doc != null) {
          CloudProfileState.Loaded(doc.toDomain())
        } else {
          CloudProfileState.Missing
        }
      }
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.GET, "users/$userId")
      _syncStatus.value = CloudSyncStatus.ERROR
      _lastSyncError.value = error.localizedMessage ?: "Unable to sync profile."
      emit(CloudProfileState.Error(error.localizedMessage ?: "Unable to sync profile."))
    }

  val transactions: Flow<List<TransactionEntity>> = transactionsCol.snapshots()
    .map { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      val mapped = snapshot.documents.mapNotNull { docSnap ->
        val doc = docSnap.toObject(
          TransactionDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@mapNotNull null
        val stableId = stablePositiveLongId(docSnap.id)
        txIdToDoc[stableId] = docSnap.id
        doc.toEntity(stableId)
      }
      mapped.sortedWith(
        compareByDescending<TransactionEntity> { it.dateEpochDay }
          .thenByDescending { it.createdAtMillis }
      )
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/transactions")
      _syncStatus.value = CloudSyncStatus.ERROR
      _lastSyncError.value = "Could not load synced transactions."
      emit(emptyList())
    }

  val budgets: Flow<List<BudgetEntity>> = budgetsCol.snapshots()
    .map { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      val mapped = snapshot.documents.mapNotNull { docSnap ->
        val doc = docSnap.toObject(
          BudgetDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@mapNotNull null
        val stableId = stablePositiveLongId(docSnap.id)
        budgetIdToDoc[stableId] = docSnap.id
        doc.toEntity(stableId)
      }
      mapped.sortedWith(
        compareByDescending<BudgetEntity> { it.year }
          .thenByDescending { it.month }
          .thenBy { it.category.lowercase(Locale.US) }
      )
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/budgets")
      _syncStatus.value = CloudSyncStatus.ERROR
      emit(emptyList())
    }

  val savingsGoals: Flow<List<SavingsGoalEntity>> = goalsCol.snapshots()
    .map { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      val mapped = snapshot.documents.mapNotNull { docSnap ->
        val doc = docSnap.toObject(
          SavingsGoalDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@mapNotNull null
        val stableId = stablePositiveLongId(docSnap.id)
        goalIdToDoc[stableId] = docSnap.id
        doc.toEntity(stableId)
      }
      mapped.sortedWith(
        compareByDescending<SavingsGoalEntity> { it.createdDateEpochDay }
          .thenByDescending { it.id }
      )
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/goals")
      _syncStatus.value = CloudSyncStatus.ERROR
      emit(emptyList())
    }

  val savingsContributions: Flow<List<SavingsContributionEntity>> = contributionsCol.snapshots()
    .map { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      val mapped = snapshot.documents.mapNotNull { docSnap ->
        val doc = docSnap.toObject(
          SavingsContributionDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@mapNotNull null
        if (doc.goalDocId.isBlank()) return@mapNotNull null
        val stableId = stablePositiveLongId(docSnap.id)
        val stableGoalId = stablePositiveLongId(doc.goalDocId)
        doc.toEntity(stableId = stableId, stableGoalId = stableGoalId)
      }
      mapped.sortedWith(
        compareByDescending<SavingsContributionEntity> { it.dateEpochDay }
          .thenByDescending { it.createdAtMillis }
      )
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/contributions")
      _syncStatus.value = CloudSyncStatus.ERROR
      emit(emptyList())
    }

  val categories: Flow<List<CategoryEntity>> = categoriesCol.snapshots()
    .map { snapshot ->
      updateSyncMetadata(snapshot.metadata.isFromCache, snapshot.metadata.hasPendingWrites())
      val mapped = snapshot.documents.mapNotNull { docSnap ->
        val doc = docSnap.toObject(
          CategoryDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        ) ?: return@mapNotNull null
        val stableId = stablePositiveLongId(docSnap.id)
        categoryIdToDoc[stableId] = docSnap.id
        doc.toEntity(stableId)
      }
      mapped.sortedWith(categoryComparator())
    }
    .catch { error ->
      if (error is CancellationException) throw error
      FirestoreErrorHandler.logFirestoreError(error, OperationType.LIST, "users/$userId/categories")
      _syncStatus.value = CloudSyncStatus.ERROR
      emit(emptyList())
    }

  private fun updateSyncMetadata(isFromCache: Boolean, hasPendingWrites: Boolean) {
    _syncStatus.value = when {
      hasPendingWrites -> CloudSyncStatus.SYNCING
      isFromCache -> CloudSyncStatus.OFFLINE
      else -> CloudSyncStatus.SYNCED
    }
    if (!isFromCache && !hasPendingWrites) {
      _lastSyncError.value = null
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
   * Ensures `/users/{userId}` and default categories exist on first sign-in without overwriting
   * an existing returning user's cloud profile or data.
   */
  suspend fun ensureProfileInitialized(
    defaultDisplayName: String,
    defaultCurrency: String = CurrencyOption.PHP.code,
  ): Result<CloudUserProfile> = runCloudCatching(OperationType.WRITE, "users/$userId") {
    initMutex.withLock {
      val snapshot = userDocRef.get().await()
      if (snapshot.exists()) {
        val existing = snapshot.toObject(
          UserProfileDoc::class.java,
          DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
        )?.toDomain()
        if (existing != null) {
          ensureDefaultCategoriesExistInternal()
          return@withLock existing
        }
      }

      val cleanName = sanitizeDisplayName(defaultDisplayName)
      val cleanCurrency = CurrencyOption.fromCode(defaultCurrency).code
      val profileMap = mapOf(
        "userId" to userId,
        "displayName" to cleanName,
        "preferredCurrency" to cleanCurrency,
        "onboardingCompleted" to false,
        "initializedData" to false,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
      userDocRef.set(profileMap).await()
      ensureDefaultCategoriesExistInternal()
      CloudUserProfile(
        userId = userId,
        displayName = cleanName,
        preferredCurrency = cleanCurrency,
        onboardingCompleted = false,
        initializedData = false,
      )
    }
  }

  /**
   * Completes the new-user Welcome Guide, saving display name & preferred currency to the cloud
   * and optionally populating starter demonstration records.
   */
  suspend fun completeOnboarding(
    displayName: String,
    preferredCurrency: String,
    loadDemoData: Boolean,
  ): Result<Unit> = runCloudCatching(OperationType.UPDATE, "users/$userId") {
    initMutex.withLock {
      val cleanName = sanitizeDisplayName(displayName)
      val cleanCurrency = CurrencyOption.fromCode(preferredCurrency).code
      val snapshot = userDocRef.get().await()
      if (!snapshot.exists()) {
        userDocRef.set(
          mapOf(
            "userId" to userId,
            "displayName" to cleanName,
            "preferredCurrency" to cleanCurrency,
            "onboardingCompleted" to true,
            "initializedData" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      } else {
        userDocRef.update(
          mapOf(
            "displayName" to cleanName,
            "preferredCurrency" to cleanCurrency,
            "onboardingCompleted" to true,
            "initializedData" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }

      ensureDefaultCategoriesExistInternal()

      if (loadDemoData) {
        val existingTx = transactionsCol.limit(1).get().await()
        val existingBudgets = budgetsCol.limit(1).get().await()
        val existingGoals = goalsCol.limit(1).get().await()
        if (existingTx.isEmpty && existingBudgets.isEmpty && existingGoals.isEmpty) {
          seedDemoRecordsInternal()
        }
      }
    }
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
        val docRef = transactionsCol.document()
        docRef.set(
          mapOf(
            "userId" to userId,
            "title" to cleanTitle,
            "amountCentavos" to transaction.amountCentavos,
            "type" to transaction.type.name,
            "category" to cleanCategory,
            "dateEpochDay" to transaction.dateEpochDay,
            "paymentMethod" to transaction.paymentMethod.name,
            "notes" to cleanNotes,
            "sampleData" to transaction.isSampleData,
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
            "sampleData" to transaction.isSampleData,
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
              "sampleData" to budget.isSampleData,
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
              "sampleData" to budget.isSampleData,
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
            "sampleData" to goal.isSampleData,
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
            "sampleData" to goal.isSampleData,
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
  // Demo Data Reset & Clear Financial Records
  // ---------------------------------------------------------------------------
  suspend fun resetToDemonstrationData(): Result<Unit> =
    runCloudCatching(OperationType.WRITE, "users/$userId") {
      initMutex.withLock {
        deleteAllRecordsInSubcollections(deleteCustomCategories = false)
        ensureDefaultCategoriesExistInternal()
        seedDemoRecordsInternal()
        userDocRef.update(
          mapOf(
            "initializedData" to true,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        ).await()
      }
    }

  suspend fun clearAllFinancialRecords(): Result<Unit> =
    runCloudCatching(OperationType.DELETE, "users/$userId") {
      initMutex.withLock {
        deleteAllRecordsInSubcollections(deleteCustomCategories = true)
        ensureDefaultCategoriesExistInternal()
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

  private suspend fun seedDemoRecordsInternal() {
    val refMonth = YearMonthKey.current()
    val sets = mutableListOf<Pair<DocumentReference, Map<String, Any>>>()

    SampleDataSeeder.sampleTransactions(refMonth).forEach { tx ->
      sets += transactionsCol.document() to mapOf(
        "userId" to userId,
        "title" to tx.title,
        "amountCentavos" to tx.amountCentavos,
        "type" to tx.type.name,
        "category" to tx.category,
        "dateEpochDay" to tx.dateEpochDay,
        "paymentMethod" to tx.paymentMethod.name,
        "notes" to tx.notes,
        "sampleData" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }

    SampleDataSeeder.sampleBudgets(refMonth).forEach { budget ->
      sets += budgetsCol.document() to mapOf(
        "userId" to userId,
        "category" to budget.category,
        "limitCentavos" to budget.limitCentavos,
        "year" to budget.year,
        "month" to budget.month,
        "sampleData" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }

    val sampleGoals = SampleDataSeeder.sampleGoals(refMonth)
    val goalRefs = sampleGoals.map { goalsCol.document() }
    val syntheticGoalIds = goalRefs.map { stablePositiveLongId(it.id) }
    val goalIdToDocId = syntheticGoalIds.zip(goalRefs.map { it.id }).toMap()

    sampleGoals.zip(goalRefs).forEach { (goal, goalRef) ->
      sets += goalRef to mapOf(
        "userId" to userId,
        "name" to goal.name,
        "targetCentavos" to goal.targetCentavos,
        "currentSavedCentavos" to goal.currentSavedCentavos,
        "targetDateEpochDay" to goal.targetDateEpochDay,
        "createdDateEpochDay" to goal.createdDateEpochDay,
        "status" to goal.status.name,
        "sampleData" to true,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    }

    SampleDataSeeder.sampleContributionsForGoals(syntheticGoalIds, refMonth).forEach { contrib ->
      val goalDocId = goalIdToDocId[contrib.goalId] ?: return@forEach
      sets += contributionsCol.document() to mapOf(
        "userId" to userId,
        "goalDocId" to goalDocId,
        "amountCentavos" to contrib.amountCentavos,
        "withdrawal" to contrib.isWithdrawal,
        "note" to contrib.note,
        "dateEpochDay" to contrib.dateEpochDay,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    }

    commitBatchSets(sets)
  }

  private suspend fun deleteAllRecordsInSubcollections(deleteCustomCategories: Boolean) {
    val refs = mutableListOf<DocumentReference>()
    refs += transactionsCol.get().await().documents.map { it.reference }
    refs += budgetsCol.get().await().documents.map { it.reference }
    refs += contributionsCol.get().await().documents.map { it.reference }
    refs += goalsCol.get().await().documents.map { it.reference }
    if (deleteCustomCategories) {
      refs += categoriesCol.get().await().documents
        .filter { it.getBoolean("defaultCategory") != true }
        .map { it.reference }
    }
    commitBatchDeletes(refs)
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
    _syncStatus.value = CloudSyncStatus.ERROR
    _lastSyncError.value = e.localizedMessage ?: "Cloud sync error."
    Result.failure(e)
  }

  private fun sanitizeDisplayName(name: String): String =
    name.trim().ifBlank { "Alex" }.take(60)

  companion object {
    private const val MAX_BATCH_OPS = 400
  }
}
