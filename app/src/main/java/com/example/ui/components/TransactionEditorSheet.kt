package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.MILLIS_PER_DAY
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.currentEpochDay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val raw = categories
      .filter { it.type == type }
      .map { it.name }
      .ifEmpty {
        if (type == TransactionType.INCOME) listOf("Salary", "Allowance", "Other Income")
        else listOf("Food and Dining", "Transportation", "Other Expenses")
      }
    sortCategoriesOtherLast(raw)
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
  var showCategoryPicker by remember { mutableStateOf(false) }

  val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol
  val primaryCtaLabel = when {
    existingTransaction != null -> if (type == TransactionType.INCOME) "Update Income" else "Update Expense"
    type == TransactionType.INCOME -> "Save Income"
    else -> "Save Expense"
  }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = Modifier
      .statusBarsPadding()
      .testTag("transaction_editor_sheet"),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxSheetHeight = if (maxHeight != Dp.Infinity) {
        (maxHeight * 0.90f).coerceAtLeast(360.dp)
      } else {
        680.dp
      }
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = maxSheetHeight)
          .navigationBarsPadding()
          .imePadding(),
      ) {
        // Scrollable compact form body
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 14.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
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
                  val ordered = sortCategoriesOtherLast(
                    categories.filter { it.type == option }.map { it.name }
                  )
                  selectedCategory = ordered.firstOrNull()
                    ?: if (option == TransactionType.INCOME) "Salary" else "Food and Dining"
                },
                shape = SegmentedButtonDefaults.itemShape(
                  index = index,
                  count = TransactionType.entries.size,
                ),
                modifier = Modifier.testTag("tx_type_${option.name.lowercase()}"),
              ) {
                Text(
                  text = option.label,
                  style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = if (type == option) FontWeight.SemiBold else FontWeight.Medium,
                  ),
                )
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
            placeholder = {
              Text(if (type == TransactionType.INCOME) "e.g., Monthly Allowance" else "e.g., Lunch")
            },
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
            prefix = {
              Text(
                text = "$currencySymbol ",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
              )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            isError = amountError != null,
            supportingText = amountError?.let { msg -> { Text(msg) } },
            singleLine = true,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("tx_amount_input"),
          )

          // Compact Category Selector Row -> opens 4-column circular icon grid sheet
          CategorySelectorField(
            selectedCategory = selectedCategory,
            allCategories = categories,
            label = "Category",
            onClick = { showCategoryPicker = true },
            testTag = "tx_category_selector",
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Date",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DateFilterBar(
              selectedEpochDay = dateEpochDay,
              todayEpochDay = currentEpochDay(),
              onSelectEpochDay = { dateEpochDay = it },
              onOpenDatePicker = { showDatePicker = true },
            )
          }

          // Single-row horizontal scrollable Payment Method chips with unmistakable scroll cue
          val paymentRowState = rememberLazyListState()
          val paymentCoroutineScope = rememberCoroutineScope()
          val canScrollMorePayments by remember {
            derivedStateOf { paymentRowState.canScrollForward }
          }
          val canScrollBackPayments by remember {
            derivedStateOf { paymentRowState.canScrollBackward }
          }
          val sheetBgColor = BottomSheetDefaults.ContainerColor

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = "Payment Method",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              AnimatedVisibility(
                visible = canScrollMorePayments,
                enter = fadeIn(),
                exit = fadeOut(),
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                      paymentCoroutineScope.launch {
                        paymentRowState.animateScrollToItem(PaymentMethod.entries.lastIndex)
                      }
                    }
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .testTag("tx_payment_swipe_hint"),
                ) {
                  Text(
                    text = "Swipe for more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                  )
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = "Scroll payment methods",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp),
                  )
                }
              }
            }
            Box(modifier = Modifier.fillMaxWidth()) {
              LazyRow(
                state = paymentRowState,
                modifier = Modifier
                  .fillMaxWidth()
                  .testTag("tx_payment_method_row"),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(end = 16.dp),
              ) {
                items(PaymentMethod.entries, key = { it.name }) { method ->
                  val isSelected = paymentMethod == method
                  FilterChip(
                    selected = isSelected,
                    onClick = { paymentMethod = method },
                    label = {
                      Text(
                        text = method.label,
                        style = MaterialTheme.typography.labelLarge.copy(
                          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        maxLines = 1,
                        softWrap = false,
                      )
                    },
                    modifier = Modifier
                      .height(38.dp)
                      .testTag("tx_payment_${method.name.lowercase()}"),
                  )
                }
              }
              if (canScrollBackPayments) {
                Box(
                  modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(20.dp)
                    .height(38.dp)
                    .background(
                      Brush.horizontalGradient(
                        colors = listOf(sheetBgColor, Color.Transparent),
                      )
                    ),
                )
              }
              if (canScrollMorePayments) {
                Box(
                  modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .height(38.dp)
                    .background(
                      Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, sheetBgColor),
                      )
                    ),
                )
              }
            }
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes (optional)") },
            minLines = 1,
            maxLines = 2,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("tx_notes_input"),
          )
        }

        // Sticky Footer Action Bar — always pinned and reachable with one thumb
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
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
              .height(50.dp)
              .testTag("save_transaction_button"),
          ) {
            Text(
              text = primaryCtaLabel,
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
          }
        }
      }
    }
  }

  if (showCategoryPicker) {
    CategoryPickerBottomSheet(
      activeType = type,
      allCategories = categories,
      selectedCategory = selectedCategory,
      allowTypeSwitch = true,
      onSelect = { chosenCategory, chosenType ->
        type = chosenType
        selectedCategory = chosenCategory
        showCategoryPicker = false
      },
      onDismiss = { showCategoryPicker = false },
    )
  }

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
}
