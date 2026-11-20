package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.ExportFormat
import com.example.domain.ExportScope
import com.example.domain.FinancialReportData
import com.example.domain.FinancialReportExporter
import com.example.domain.MILLIS_PER_DAY
import com.example.domain.currentEpochDay
import com.example.domain.epochDayFromDate
import com.example.domain.formatEpochDay
import com.example.ui.viewmodel.BudgetWiseUiState

private enum class ActiveDatePickerTarget {
  START,
  END,
}

/**
 * Centered Material 3 dialog for configuring and exporting BudgetWise transactions and budgets
 * to CSV or PDF.
 *
 * Supports exporting any selected month, a custom start/end date range, or all recorded history,
 * with responsive layout that prevents label clipping on compact Android viewports.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportReportDialog(
  uiState: BudgetWiseUiState,
  isExporting: Boolean,
  onRequestSave: (FinancialReportData, ExportFormat) -> Unit,
  onRequestShare: (FinancialReportData, ExportFormat) -> Unit,
  onDismiss: () -> Unit,
) {
  var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
  var selectedScope by remember { mutableStateOf(ExportScope.MONTH) }
  var targetMonth by remember(uiState.selectedMonth) { mutableStateOf(uiState.selectedMonth) }
  var customStartEpochDay by remember(uiState.selectedMonth) {
    mutableLongStateOf(epochDayFromDate(uiState.selectedMonth.year, uiState.selectedMonth.month, 1))
  }
  var customEndEpochDay by remember {
    mutableLongStateOf(currentEpochDay())
  }
  var activeDatePicker by remember { mutableStateOf<ActiveDatePickerTarget?>(null) }
  var includeTransactions by remember { mutableStateOf(true) }
  var includeBudgets by remember { mutableStateOf(true) }

  activeDatePicker?.let { target ->
    val initialDay = if (target == ActiveDatePickerTarget.START) customStartEpochDay else customEndEpochDay
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = initialDay * MILLIS_PER_DAY,
    )
    DatePickerDialog(
      onDismissRequest = { activeDatePicker = null },
      confirmButton = {
        TextButton(
          onClick = {
            val selectedMillis = pickerState.selectedDateMillis
            if (selectedMillis != null) {
              val chosenDay = Math.floorDiv(selectedMillis, MILLIS_PER_DAY)
              when (target) {
                ActiveDatePickerTarget.START -> {
                  customStartEpochDay = chosenDay
                  if (chosenDay > customEndEpochDay) {
                    customEndEpochDay = chosenDay
                  }
                }
                ActiveDatePickerTarget.END -> {
                  customEndEpochDay = chosenDay
                  if (chosenDay < customStartEpochDay) {
                    customStartEpochDay = chosenDay
                  }
                }
              }
            }
            activeDatePicker = null
          },
          modifier = Modifier.testTag("export_date_picker_confirm"),
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeDatePicker = null }) {
          Text("Cancel")
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }

  val reportData = remember(
    uiState.preferences.displayName,
    uiState.preferences.preferredCurrency,
    targetMonth,
    customStartEpochDay,
    customEndEpochDay,
    uiState.allTransactions,
    uiState.allBudgets,
    selectedScope,
    includeTransactions,
    includeBudgets,
  ) {
    FinancialReportExporter.buildReportData(
      ownerName = uiState.preferences.displayName,
      currencyCode = uiState.preferences.preferredCurrency,
      selectedMonth = targetMonth,
      allTransactions = uiState.allTransactions,
      allBudgets = uiState.allBudgets,
      scope = selectedScope,
      includeTransactions = includeTransactions,
      includeBudgets = includeBudgets,
      customStartEpochDay = customStartEpochDay,
      customEndEpochDay = customEndEpochDay,
    )
  }

  val totalRecords = reportData.transactions.size + reportData.budgets.size
  val hasSelection = includeTransactions || includeBudgets
  val canExport = hasSelection && totalRecords > 0 && !isExporting

  StableFormDialog(
    onDismissRequest = { if (!isExporting) onDismiss() },
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
          )
          Text(
            text = stringResource(R.string.export_dialog_title),
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        IconButton(
          onClick = onDismiss,
          enabled = !isExporting,
          modifier = Modifier
            .minimumInteractiveComponentSize()
            .testTag("export_cancel_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = "Close",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    },
    confirmButton = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = { onRequestShare(reportData, selectedFormat) },
          enabled = canExport,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("export_share_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share", maxLines = 1, softWrap = false)
        }
        Button(
          onClick = { onRequestSave(reportData, selectedFormat) },
          enabled = canExport,
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
          modifier = Modifier
            .weight(1f)
            .testTag("export_save_button"),
        ) {
          if (isExporting) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
              color = MaterialTheme.colorScheme.onPrimary,
            )
          } else {
            Icon(
              imageVector = Icons.Rounded.Download,
              contentDescription = null,
              modifier = Modifier.size(17.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text("Save ${selectedFormat.label}", maxLines = 1, softWrap = false)
        }
      }
    },
  ) {
    // 1. Format selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "File Format",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ExportFormat.entries.forEachIndexed { idx, format ->
          SegmentedButton(
            selected = selectedFormat == format,
            onClick = { selectedFormat = format },
            shape = SegmentedButtonDefaults.itemShape(idx, ExportFormat.entries.size),
            icon = {
              Icon(
                imageVector = if (format == ExportFormat.CSV) Icons.Rounded.TableChart else Icons.Rounded.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
            modifier = Modifier.testTag("export_format_${format.extension}"),
          ) {
            Text("${format.label} (.${format.extension})", maxLines = 1, softWrap = false)
          }
        }
      }
      Text(
        text = if (selectedFormat == ExportFormat.CSV) {
          stringResource(R.string.export_csv_hint)
        } else {
          stringResource(R.string.export_pdf_hint)
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }

    // 2. Flexible Time Period selector (Month stepper / Custom date range / All Time)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text(
        text = "Time Period",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        ExportScope.entries.forEachIndexed { idx, scopeOption ->
          SegmentedButton(
            selected = selectedScope == scopeOption,
            onClick = { selectedScope = scopeOption },
            shape = SegmentedButtonDefaults.itemShape(idx, ExportScope.entries.size),
            icon = {},
            modifier = Modifier.testTag("export_scope_${scopeOption.name.lowercase()}"),
          ) {
            Text(
              text = scopeOption.label,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }

      when (selectedScope) {
        ExportScope.MONTH -> {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
            ) {
              IconButton(
                onClick = { targetMonth = targetMonth.previous() },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("export_prev_month_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                  contentDescription = "Previous month",
                )
              }
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Icon(
                  imageVector = Icons.Rounded.CalendarMonth,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp),
                )
                Text(
                  text = targetMonth.displayLabel(),
                  style = MaterialTheme.typography.titleSmall,
                  color = MaterialTheme.colorScheme.onSurface,
                  modifier = Modifier.testTag("export_selected_month_label"),
                )
              }
              IconButton(
                onClick = { targetMonth = targetMonth.next() },
                modifier = Modifier
                  .minimumInteractiveComponentSize()
                  .testTag("export_next_month_button"),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                  contentDescription = "Next month",
                )
              }
            }
          }
        }
        ExportScope.CUSTOM_RANGE -> {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            DateRangeFieldButton(
              label = "From",
              dateText = formatEpochDay(customStartEpochDay, "MMM d, yyyy"),
              onClick = { activeDatePicker = ActiveDatePickerTarget.START },
              modifier = Modifier
                .weight(1f)
                .testTag("export_custom_start_button"),
            )
            DateRangeFieldButton(
              label = "To",
              dateText = formatEpochDay(customEndEpochDay, "MMM d, yyyy"),
              onClick = { activeDatePicker = ActiveDatePickerTarget.END },
              modifier = Modifier
                .weight(1f)
                .testTag("export_custom_end_button"),
            )
          }
        }
        ExportScope.ALL_TIME -> Unit
      }
    }

    // 3. Included datasets — FlowRow ensures natural chip widths so "Transactions" never clips
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "Include Data",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        FilterChip(
          selected = includeTransactions,
          onClick = { includeTransactions = !includeTransactions },
          label = { Text("Transactions", maxLines = 1, softWrap = false) },
          leadingIcon = if (includeTransactions) {
            {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          } else {
            null
          },
          modifier = Modifier.testTag("export_include_transactions"),
        )
        FilterChip(
          selected = includeBudgets,
          onClick = { includeBudgets = !includeBudgets },
          label = { Text("Budgets", maxLines = 1, softWrap = false) },
          leadingIcon = if (includeBudgets) {
            {
              Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
              )
            }
          } else {
            null
          },
          modifier = Modifier.testTag("export_include_budgets"),
        )
      }
    }

    // 4. Live export preview summary
    val previewSummary = remember(reportData, hasSelection, totalRecords, selectedFormat) {
      when {
        !hasSelection -> "Select at least one dataset (Transactions or Budgets) to export."
        totalRecords == 0 -> "No records found for ${reportData.periodLabel}. Try another month, range, or All Time."
        else -> {
          val parts = buildList {
            if (reportData.includeTransactions) {
              val n = reportData.transactions.size
              add("$n ${if (n == 1) "transaction" else "transactions"}")
            }
            if (reportData.includeBudgets) {
              val n = reportData.budgets.size
              add("$n ${if (n == 1) "budget" else "budgets"}")
            }
          }
          "${parts.joinToString(" · ")} · ${reportData.currencyCode}"
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(12.dp),
      color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("export_preview_card"),
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = FinancialReportExporter.suggestedFileName(reportData, selectedFormat),
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = previewSummary,
          style = MaterialTheme.typography.bodySmall,
          color = if (canExport) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
        )
      }
    }
  }
}

@Composable
private fun DateRangeFieldButton(
  label: String,
  dateText: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = modifier,
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Icon(
          imageVector = Icons.Rounded.CalendarMonth,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(14.dp),
        )
        Text(
          text = dateText,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
