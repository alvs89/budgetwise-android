package com.example.data.local

import androidx.room.TypeConverter
import com.example.domain.AlertDestination
import com.example.domain.AlertSeverity
import com.example.domain.ChatRole
import com.example.domain.FinancialAlertType
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

  @TypeConverter
  fun fromFinancialAlertType(value: FinancialAlertType): String = value.name

  @TypeConverter
  fun toFinancialAlertType(value: String): FinancialAlertType = FinancialAlertType.valueOf(value)

  @TypeConverter
  fun fromAlertSeverity(value: AlertSeverity): String = value.name

  @TypeConverter
  fun toAlertSeverity(value: String): AlertSeverity = AlertSeverity.valueOf(value)

  @TypeConverter
  fun fromAlertDestination(value: AlertDestination): String = value.name

  @TypeConverter
  fun toAlertDestination(value: String): AlertDestination = AlertDestination.valueOf(value)
}
