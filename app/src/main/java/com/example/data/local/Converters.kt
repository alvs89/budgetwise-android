package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.ChatRole
import com.example.domain.GoalStatus
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType

class Converters {
  @TypeConverter
  fun fromTransactionType(value: TransactionType): String = value.name

  @TypeConverter
  fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

  @TypeConverter
  fun fromPaymentMethod(value: PaymentMethod): String = value.name

  @TypeConverter
  fun toPaymentMethod(value: String): PaymentMethod = PaymentMethod.valueOf(value)

  @TypeConverter
  fun fromGoalStatus(value: GoalStatus): String = value.name

  @TypeConverter
  fun toGoalStatus(value: String): GoalStatus = GoalStatus.valueOf(value)

  @TypeConverter
  fun fromChatRole(value: ChatRole): String = value.name

  @TypeConverter
  fun toChatRole(value: String): ChatRole = ChatRole.valueOf(value)
}
