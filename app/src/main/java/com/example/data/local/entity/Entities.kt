package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.ChatRole
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType

@Entity(
  tableName = "transactions",
  indices = [Index(value = ["dateEpochDay"]), Index(value = ["category"]), Index(value = ["type"])],
)
data class TransactionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val amountCentavos: Long,
  val type: TransactionType,
  val category: String,
  val dateEpochDay: Long,
  val paymentMethod: PaymentMethod,
  val notes: String = "",
  val createdAtMillis: Long = System.currentTimeMillis(),
  val isSampleData: Boolean = false,
)

@Entity(
  tableName = "budgets",
  indices = [Index(value = ["category", "year", "month"], unique = true)],
)
data class BudgetEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val category: String,
  val limitCentavos: Long,
  val year: Int,
  val month: Int,
  val isSampleData: Boolean = false,
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val targetCentavos: Long,
  val currentSavedCentavos: Long,
  val targetDateEpochDay: Long,
  val createdDateEpochDay: Long,
  val status: GoalStatus = GoalStatus.ACTIVE,
  val isSampleData: Boolean = false,
)

@Entity(
  tableName = "savings_contributions",
  foreignKeys = [
    ForeignKey(
      entity = SavingsGoalEntity::class,
      parentColumns = ["id"],
      childColumns = ["goalId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["goalId"])],
)
data class SavingsContributionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val goalId: Long,
  val amountCentavos: Long,
  val isWithdrawal: Boolean = false,
  val note: String = "",
  val dateEpochDay: Long,
  val createdAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "categories",
  indices = [Index(value = ["name", "type"], unique = true)],
)
data class CategoryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val name: String,
  val type: TransactionType,
  val iconKey: String,
  val colorHex: Long,
  val isDefault: Boolean = true,
)

@Entity(
  tableName = "chat_sessions",
  indices = [Index(value = ["updatedAtMillis"])],
)
data class ChatSessionEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val title: String,
  val isTitleManuallySet: Boolean = false,
  val createdAtMillis: Long = System.currentTimeMillis(),
  val updatedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(
  tableName = "chat_messages",
  foreignKeys = [
    ForeignKey(
      entity = ChatSessionEntity::class,
      parentColumns = ["id"],
      childColumns = ["sessionId"],
      onDelete = ForeignKey.CASCADE,
    )
  ],
  indices = [Index(value = ["createdAtMillis"]), Index(value = ["sessionId"])],
)
data class ChatMessageEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0L,
  val sessionId: Long = 1L,
  val role: ChatRole,
  val content: String,
  val createdAtMillis: Long = System.currentTimeMillis(),
)
