package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.MILLIS_PER_DAY
import com.example.domain.PaymentMethod
import com.example.domain.TransactionType
import com.example.domain.compactCategoryLabel
import com.example.domain.currentEpochDay

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
    categories
      .filter { it.type == type }
      .map { it.name }
      .distinct()
      .sortedWith(compareBy<String>({ isOtherCategoryName(it) }, { it.lowercase() }))
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
      val maxSheetHeight = if (maxHeight != androidx.compose.ui.unit.Dp.Infinity) {
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
                  val firstCat = categories.firstOrNull { it.type == option }?.name
                    ?: if (option == TransactionType.INCOME) "Allowance" else "Food and Dining"
                  selectedCategory = firstCat
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

          // Compact Category Selector Row (opens 3-column visual icon grid picker)
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
              text = "Category",
              style = MaterialTheme.typography.labelLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val tint = categoryTint(selectedCategory, categories)
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
              modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { showCategoryPicker = true }
                .testTag("tx_category_selector"),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(tint.copy(alpha = 0.16f)),
                  contentAlignment = Alignment.Center,
                ) {
                  Icon(
                    imageVector = categoryIcon(selectedCategory, categories),
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                  )
                }
                Text(
                  text = compactCategoryLabel(selectedCategory),
                  style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                  modifier = Modifier.weight(1f),
                )
                Icon(
                  imageVector = Icons.Rounded.KeyboardArrowDown,
                  contentDescription = "Choose category",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(22.dp),
                )
              }
            }
          }

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

          // Single-row horizontal scrollable Payment Method chips with clear scroll affordance
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
    CategoryGridPickerDialog(
      type = type,
      availableCategories = availableCategories,
      allCategories = categories,
      selectedCategory = selectedCategory,
      onSelectCategory = { chosen ->
        selectedCategory = chosen
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

@Composable
private fun CategoryGridPickerDialog(
  type: TransactionType,
  availableCategories: List<String>,
  allCategories: List<CategoryEntity>,
  selectedCategory: String,
  onSelectCategory: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 6.dp,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .widthIn(max = 420.dp)
        .testTag("category_grid_picker_sheet"),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 18.dp),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = if (type == TransactionType.INCOME) "Select Income Category" else "Select Expense Category",
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
          )
          IconButton(
            onClick = onDismiss,
            modifier = Modifier
              .minimumInteractiveComponentSize()
              .testTag("category_picker_close"),
          ) {
            Icon(
              imageVector = Icons.Rounded.Close,
              contentDescription = "Close category picker",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        val rows = remember(availableCategories) { availableCategories.chunked(3) }
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 420.dp)
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          rows.forEach { rowCategories ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              rowCategories.forEach { cat ->
                val isSelected = cat == selectedCategory
                val tint = categoryTint(cat, allCategories)
                Surface(
                  shape = RoundedCornerShape(14.dp),
                  color = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
                  } else {
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                  },
                  border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                    },
                  ),
                  modifier = Modifier
                    .weight(1f)
                    .height(96.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onSelectCategory(cat) }
                    .testTag("tx_category_option_${cat.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"),
                ) {
                  Box(modifier = Modifier.fillMaxWidth()) {
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                          .align(Alignment.TopEnd)
                          .padding(6.dp)
                          .size(16.dp),
                      )
                    }
                    Column(
                      modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center)
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                      horizontalAlignment = Alignment.CenterHorizontally,
                      verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                      Box(
                        modifier = Modifier
                          .size(38.dp)
                          .clip(CircleShape)
                          .background(tint.copy(alpha = if (isSelected) 0.22f else 0.15f)),
                        contentAlignment = Alignment.Center,
                      ) {
                        Icon(
                          imageVector = categoryIcon(cat, allCategories),
                          contentDescription = null,
                          tint = tint,
                          modifier = Modifier.size(20.dp),
                        )
                      }
                      Text(
                        text = compactCategoryLabel(cat),
                        style = MaterialTheme.typography.labelMedium.copy(
                          fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        ),
                        color = if (isSelected) {
                          MaterialTheme.colorScheme.onSurface
                        } else {
                          MaterialTheme.colorScheme.onSurface.copy(alpha = 0.88f)
                        },
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                      )
                    }
                  }
                }
              }
              repeat(3 - rowCategories.size) {
                Spacer(modifier = Modifier.weight(1f))
              }
            }
          }
        }
      }
    }
  }
}

private fun isOtherCategoryName(name: String): Boolean {
  val normalized = name.trim().lowercase()
  return normalized == "other" || normalized == "others" || normalized.startsWith("other ")
}
