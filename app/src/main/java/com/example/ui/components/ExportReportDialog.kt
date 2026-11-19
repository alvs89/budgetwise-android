package com.example.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.R
import com.example.domain.ExportFormat
import com.example.domain.ExportScope
import com.example.domain.FinancialReportData
import com.example.domain.FinancialReportExporter
import com.example.ui.viewmodel.BudgetWiseUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Centered Material 3 dialog for exporting BudgetWise transactions and budgets to CSV or PDF.
 *
 * Uses Android Storage Access Framework ([ActivityResultContracts.CreateDocument]) for zero-permission
 * saving to device storage and [FileProvider] for instant sharing to spreadsheet or PDF apps.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportReportDialog(
  uiState: BudgetWiseUiState,
  onExportFeedback: (String) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()

  var selectedFormat by remember { mutableStateOf(ExportFormat.CSV) }
  var selectedScope by remember { mutableStateOf(ExportScope.SELECTED_MONTH) }
  var includeTransactions by remember { mutableStateOf(true) }
  var includeBudgets by remember { mutableStateOf(true) }
  var isExporting by remember { mutableStateOf(false) }
  var pendingSaveSnapshot by remember { mutableStateOf<Pair<FinancialReportData, ExportFormat>?>(null) }

  val reportData = remember(
    uiState.preferences.displayName,
    uiState.preferences.preferredCurrency,
    uiState.selectedMonth,
    uiState.allTransactions,
    uiState.allBudgets,
    selectedScope,
    includeTransactions,
    includeBudgets,
  ) {
    FinancialReportExporter.buildReportData(
      ownerName = uiState.preferences.displayName,
      currencyCode = uiState.preferences.preferredCurrency,
      selectedMonth = uiState.selectedMonth,
      allTransactions = uiState.allTransactions,
      allBudgets = uiState.allBudgets,
      scope = selectedScope,
      includeTransactions = includeTransactions,
      includeBudgets = includeBudgets,
    )
  }

  val totalRecords = reportData.transactions.size + reportData.budgets.size
  val hasSelection = includeTransactions || includeBudgets
  val canExport = hasSelection && totalRecords > 0 && !isExporting

  fun writeToUri(uri: android.net.Uri, data: FinancialReportData, format: ExportFormat) {
    isExporting = true
    scope.launch {
      val result = runCatching {
        withContext(Dispatchers.IO) {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            when (format) {
              ExportFormat.CSV -> FinancialReportExporter.writeCsvReport(data, out)
              ExportFormat.PDF -> FinancialReportExporter.writePdfReport(data, out)
            }
          } ?: error("Unable to open destination file.")
        }
      }
      isExporting = false
      result.onSuccess {
        val fileName = FinancialReportExporter.suggestedFileName(data, format)
        onExportFeedback("Saved $fileName")
        onDismiss()
      }.onFailure { err ->
        onExportFeedback("Export failed: ${err.localizedMessage ?: "Unable to write file"}")
      }
    }
  }

  val csvSaveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ExportFormat.CSV.mimeType),
  ) { uri ->
    val snapshot = pendingSaveSnapshot
    pendingSaveSnapshot = null
    if (uri != null && snapshot != null) {
      writeToUri(uri, snapshot.first, ExportFormat.CSV)
    }
  }

  val pdfSaveLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument(ExportFormat.PDF.mimeType),
  ) { uri ->
    val snapshot = pendingSaveSnapshot
    pendingSaveSnapshot = null
    if (uri != null && snapshot != null) {
      writeToUri(uri, snapshot.first, ExportFormat.PDF)
    }
  }

  fun shareReport(data: FinancialReportData, format: ExportFormat) {
    isExporting = true
    scope.launch {
      val fileName = FinancialReportExporter.suggestedFileName(data, format)
      val fileResult = runCatching {
        withContext(Dispatchers.IO) {
          val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
          // Clean up stale temporary exports to keep app cache lightweight
          exportsDir.listFiles()?.forEach { oldFile ->
            if (oldFile.name != fileName) {
              oldFile.delete()
            }
          }
          val outFile = File(exportsDir, fileName)
          outFile.outputStream().use { out ->
            when (format) {
              ExportFormat.CSV -> FinancialReportExporter.writeCsvReport(data, out)
              ExportFormat.PDF -> FinancialReportExporter.writePdfReport(data, out)
            }
          }
          outFile
        }
      }
      isExporting = false
      fileResult.onSuccess { outFile ->
        try {
          val authority = "${context.packageName}.fileprovider"
          val contentUri = FileProvider.getUriForFile(context, authority, outFile)
          val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = format.mimeType
            putExtra(Intent.EXTRA_STREAM, contentUri)
            putExtra(Intent.EXTRA_SUBJECT, "BudgetWise Financial Report (${data.periodLabel})")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
          }
          context.startActivity(
            Intent.createChooser(sendIntent, "Share ${format.label} Report"),
          )
          onExportFeedback("Prepared $fileName to share")
          onDismiss()
        } catch (_: ActivityNotFoundException) {
          onExportFeedback("No app available to share ${format.label} files")
        } catch (err: Exception) {
          onExportFeedback("Share failed: ${err.localizedMessage ?: "Unable to share file"}")
        }
      }.onFailure { err ->
        onExportFeedback("Export failed: ${err.localizedMessage ?: "Unable to create report"}")
      }
    }
  }

  StableFormDialog(
    onDismissRequest = { if (!isExporting) onDismiss() },
    title = {
      Row(
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
        )
      }
    },
    dismissButton = {
      TextButton(
        onClick = onDismiss,
        enabled = !isExporting,
        modifier = Modifier.testTag("export_cancel_button"),
      ) {
        Text("Cancel")
      }
    },
    confirmButton = {
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        OutlinedButton(
          onClick = { shareReport(reportData, selectedFormat) },
          enabled = canExport,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier.testTag("export_share_button"),
        ) {
          Icon(
            imageVector = Icons.Rounded.Share,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Share", maxLines = 1, softWrap = false)
        }
        Button(
          onClick = {
            val fileName = FinancialReportExporter.suggestedFileName(reportData, selectedFormat)
            pendingSaveSnapshot = reportData to selectedFormat
            when (selectedFormat) {
              ExportFormat.CSV -> csvSaveLauncher.launch(fileName)
              ExportFormat.PDF -> pdfSaveLauncher.launch(fileName)
            }
          },
          enabled = canExport,
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier.testTag("export_save_button"),
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
              modifier = Modifier.size(16.dp),
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

    // 2. Date range selector
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "Time Period",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        val monthShortLabel = "${uiState.selectedMonth.shortLabel()} ${uiState.selectedMonth.year}"
        ExportScope.entries.forEachIndexed { idx, scopeOption ->
          SegmentedButton(
            selected = selectedScope == scopeOption,
            onClick = { selectedScope = scopeOption },
            shape = SegmentedButtonDefaults.itemShape(idx, ExportScope.entries.size),
            modifier = Modifier.testTag("export_scope_${scopeOption.name.lowercase()}"),
          ) {
            Text(
              text = if (scopeOption == ExportScope.SELECTED_MONTH) monthShortLabel else scopeOption.label,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    // 3. Included datasets
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Text(
        text = "Include Data",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
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
          modifier = Modifier
            .weight(1f)
            .testTag("export_include_transactions"),
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
          modifier = Modifier
            .weight(1f)
            .testTag("export_include_budgets"),
        )
      }
    }

    // 4. Live export preview summary
    val previewSummary = remember(reportData, hasSelection, totalRecords, selectedFormat) {
      when {
        !hasSelection -> "Select at least one dataset (Transactions or Budgets) to export."
        totalRecords == 0 -> "No records found for ${reportData.periodLabel}. Switch to All Time or add entries first."
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
