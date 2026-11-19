package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.MILLIS_PER_DAY
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.domain.currentEpochDay

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditorSheet(
  existingTransaction: TransactionEntity?,
  categories: List<CategoryEntity>,
  currencyCode: String,
  onSave: (
    existingId: Long,
    title: String,
    amountCentavos: Long,
    type: TransactionType,
    category: String,
    dateEpochDay: Long,
    paymentMethod: PaymentMethod,
    notes: String,
    existingCreatedAt: Long?,
  ) -> Unit,
  onDelete: ((TransactionEntity) -> Unit)?,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var type by remember(existingTransaction) {
    mutableStateOf(existingTransaction?.type ?: TransactionType.EXPENSE)
  }
  val availableCategories = remember(categories, type) {
    categories
      .filter { it.type == type }
      .map { it.name }
      .distinct()
      .ifEmpty {
        if (type == TransactionType.INCOME) listOf("Allowance", "Salary", "Other Income")
        else listOf("Food and Dining", "Transportation", "Other Expenses")
      }
  }
  var title by remember(existingTransaction) { mutableStateOf(existingTransaction?.title ?: "") }
  var amountText by remember(existingTransaction) {
    mutableStateOf(
      existingTransaction?.let {
        FinanceCalculator.centavosToBigDecimal(it.amountCentavos).toPlainString()
      } ?: ""
    )
  }
  var selectedCategory by remember(existingTransaction) {
    val existingCat = existingTransaction?.category
    mutableStateOf(
      if (existingCat != null && existingCat in availableCategories) existingCat
      else availableCategories.first()
    )
  }
  LaunchedEffect(type, availableCategories) {
    if (selectedCategory !in availableCategories) {
      selectedCategory = availableCategories.first()
    }
  }
  var dateEpochDay by remember(existingTransaction) {
    mutableLongStateOf(existingTransaction?.dateEpochDay ?: currentEpochDay())
  }
  var paymentMethod by remember(existingTransaction) {
    mutableStateOf(existingTransaction?.paymentMethod ?: PaymentMethod.CASH)
  }
  var notes by remember(existingTransaction) { mutableStateOf(existingTransaction?.notes ?: "") }
  var titleError by remember { mutableStateOf<String?>(null) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol

  if (showDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = dateEpochDay * MILLIS_PER_DAY
    )
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              dateEpochDay = millis / MILLIS_PER_DAY
            }
            showDatePicker = false
          }
        ) {
          Text("Select")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) {
      DatePicker(state = datePickerState)
    }
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .imePadding()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp)
        .padding(bottom = 28.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (existingTransaction == null) "Add Transaction" else "Edit Transaction",
          style = MaterialTheme.typography.headlineSmall,
        )
        if (existingTransaction != null && onDelete != null) {
          TextButton(
            onClick = { onDelete(existingTransaction) },
            colors = ButtonDefaults.textButtonColors(
              contentColor = MaterialTheme.colorScheme.error,
            ),
            modifier = Modifier.testTag("sheet_delete_transaction"),
          ) {
            Icon(
              imageVector = Icons.Rounded.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Delete", maxLines = 1, softWrap = false)
          }
        }
      }

      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        TransactionType.entries.forEachIndexed { index, option ->
          SegmentedButton(
            selected = type == option,
            onClick = {
              type = option
              val firstCat = categories.firstOrNull { it.type == option }?.name
                ?: if (option == TransactionType.INCOME) "Allowance" else "Food and Dining"
              selectedCategory = firstCat
            },
            shape = SegmentedButtonDefaults.itemShape(index = index, count = TransactionType.entries.size),
            modifier = Modifier.testTag("tx_type_${option.name.lowercase()}"),
          ) {
            Text(option.label)
          }
        }
      }

      OutlinedTextField(
        value = title,
        onValueChange = {
          title = it
          titleError = null
        },
        label = { Text("Title") },
        placeholder = { Text(if (type == TransactionType.INCOME) "e.g., Monthly Allowance" else "e.g., Lunch") },
        isError = titleError != null,
        supportingText = titleError?.let { msg -> { Text(msg) } },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tx_title_input"),
      )

      OutlinedTextField(
        value = amountText,
        onValueChange = {
          amountText = it
          amountError = null
        },
        label = { Text("Amount ($currencyCode)") },
        prefix = { Text("$currencySymbol ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = amountError != null,
        supportingText = amountError?.let { msg -> { Text(msg) } },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth()
          .testTag("tx_amount_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Category", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          availableCategories.forEach { cat ->
            FilterChip(
              selected = selectedCategory == cat,
              onClick = { selectedCategory = cat },
              leadingIcon = {
                Icon(
                  imageVector = categoryIcon(cat, categories),
                  contentDescription = null,
                  tint = categoryTint(cat, categories),
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(compactCategoryLabel(cat), maxLines = 1, softWrap = false) },
            )
          }
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Date", style = MaterialTheme.typography.labelLarge)
        DateFilterBar(
          selectedEpochDay = dateEpochDay,
          todayEpochDay = currentEpochDay(),
          onSelectEpochDay = { dateEpochDay = it },
          onOpenDatePicker = { showDatePicker = true },
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Payment Method", style = MaterialTheme.typography.labelLarge)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          PaymentMethod.entries.forEach { method ->
            FilterChip(
              selected = paymentMethod == method,
              onClick = { paymentMethod = method },
              label = { Text(method.label) },
            )
          }
        }
      }

      OutlinedTextField(
        value = notes,
        onValueChange = { notes = it },
        label = { Text("Notes (optional)") },
        maxLines = 3,
        modifier = Modifier.fillMaxWidth(),
      )

      Spacer(modifier = Modifier.height(4.dp))
      Button(
        onClick = {
          val cleanTitle = title.trim()
          val parsedCentavos = FinanceCalculator.parseAmountToCentavos(amountText)
          var hasError = false
          if (cleanTitle.isEmpty()) {
            titleError = "Please enter a transaction title."
            hasError = true
          }
          if (parsedCentavos == null) {
            amountError = "Enter a valid amount greater than zero."
            hasError = true
          }
          if (!hasError && parsedCentavos != null) {
            val safeCategory = if (selectedCategory in availableCategories) {
              selectedCategory
            } else {
              availableCategories.first()
            }
            onSave(
              existingTransaction?.id ?: 0L,
              cleanTitle,
              parsedCentavos,
              type,
              safeCategory,
              dateEpochDay,
              paymentMethod,
              notes,
              existingTransaction?.createdAtMillis,
            )
          }
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("save_transaction_button"),
      ) {
        Text(if (existingTransaction == null) "Save Transaction" else "Update Transaction")
      }
    }
  }
}
