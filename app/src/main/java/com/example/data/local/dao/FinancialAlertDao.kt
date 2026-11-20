package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.FinancialAlertEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FinancialAlertDao {
  @Query("SELECT * FROM financial_alerts ORDER BY createdAtMillis DESC, id DESC")
  fun observeAllAlerts(): Flow<List<FinancialAlertEntity>>

  @Query("SELECT EXISTS(SELECT 1 FROM financial_alerts WHERE dedupeKey = :dedupeKey)")
  suspend fun existsByDedupeKey(dedupeKey: String): Boolean

  /** Returns rowId > 0 if newly inserted, or -1L if an alert with [dedupeKey] already exists. */
  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertIgnore(alert: FinancialAlertEntity): Long

  @Query("UPDATE financial_alerts SET isRead = 1 WHERE id = :id")
  suspend fun markAsRead(id: Long)

  @Query("UPDATE financial_alerts SET isRead = 1 WHERE isRead = 0")
  suspend fun markAllAsRead()

  @Query("DELETE FROM financial_alerts WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM financial_alerts WHERE dedupeKey = :dedupeKey")
  suspend fun deleteByDedupeKey(dedupeKey: String)

  @Query("DELETE FROM financial_alerts WHERE dedupeKey LIKE :prefix")
  suspend fun deleteByDedupeKeyPrefix(prefix: String)

  @Query("DELETE FROM financial_alerts")
  suspend fun deleteAll()
}
