package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.AlertDestination
import com.example.domain.AlertSeverity
import com.example.domain.FinancialAlertType

/**
 * Local persistent record of a financial alert shown in the Notification Center.
 *
 * [dedupeKey] uniquely identifies an event (e.g., `budget_warn_Food and Dining_2026_11`,
 * `goal_milestone_3_75`, `daily_reminder_2026_11_19`) so identical alerts are never spammed.
 */
@Entity(
  tableName = "financial_alerts",
  indices = [
    Index(value = ["dedupeKey"], unique = true),
    Index(value = ["createdAtMillis"]),
  ],
)
data class FinancialAlertEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val dedupeKey: String,
  val type: FinancialAlertType,
  val severity: AlertSeverity,
  val title: String,
  val message: String,
  val targetDestination: AlertDestination,
  val isRead: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
