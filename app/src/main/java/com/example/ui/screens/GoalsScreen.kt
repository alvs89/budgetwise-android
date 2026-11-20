package com.example.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import com.example.domain.GoalStatus
import com.example.domain.MILLIS_PER_DAY
import com.example.domain.SavingsGoalSummary
import com.example.domain.currentEpochDay
import com.example.domain.formatEpochDay
import com.example.ui.components.AutoResizeSingleLineText
import com.example.ui.components.ConfirmationDialog
import com.example.ui.components.EmptyStatePanel
import com.example.ui.components.FintechCard
import com.example.ui.components.FintechProgressBar
import com.example.ui.components.LocalIsBalanceHidden
import com.example.ui.components.MaskedCurrencyText
import com.example.ui.components.SectionHeader
import com.example.ui.components.StableFormDialog
import com.example.ui.theme.LocalFinanceColors
import com.example.ui.viewmodel.BudgetWiseUiState
import java.math.BigDecimal

@Composable
fun GoalsScreen(
  uiState: BudgetWiseUiState,
  onSaveGoal: (existing: SavingsGoalEntity?, name: String, targetCentavos: Long, targetDateEpochDay: Long, status: GoalStatus) -> Unit,
  onDeleteGoal: (SavingsGoalEntity) -> Unit,
  onRecordContribution: (goalId: Long, amountCentavos: Long, isWithdrawal: Boolean, note: String, dateEpochDay: Long) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showGoalEditor by remember { mutableStateOf(false) }
  var editingGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }
  var contributionTarget by remember { mutableStateOf<SavingsGoalSummary?>(null) }
  var historyGoal by remember { mutableStateOf<SavingsGoalSummary?>(null) }
  var pendingDeleteGoal by remember { mutableStateOf<SavingsGoalEntity?>(null) }

  val currency = uiState.preferences.preferredCurrency
  val financeColors = LocalFinanceColors.current

  if (showGoalEditor) {
    GoalEditorDialog(
      existing = editingGoal,
      currencyCode = currency,
      onSave = { name, targetCentavos, targetDateEpochDay, status ->
        onSaveGoal(editingGoal, name, targetCentavos, targetDateEpochDay, status)
        showGoalEditor = false
        editingGoal = null
      },
      onDismiss = {
        showGoalEditor = false
        editingGoal = null
      },
    )
  }

  contributionTarget?.let { goal ->
    GoalContributionDialog(
      goal = goal,
      currencyCode = currency,
      onSubmit = { amountCentavos, isWithdrawal, note, dateEpochDay ->
        onRecordContribution(goal.goalId, amountCentavos, isWithdrawal, note, dateEpochDay)
        contributionTarget = null
      },
      onDismiss = { contributionTarget = null },
    )
  }

  historyGoal?.let { goal ->
    val entries = uiState.allContributions.filter { it.goalId == goal.goalId }
    GoalHistorySheet(
      goal = goal,
      contributions = entries,
      currencyCode = currency,
      onDismiss = { historyGoal = null },
    )
  }

  pendingDeleteGoal?.let { goal ->
    ConfirmationDialog(
      title = "Delete Savings Goal?",
      message = "Delete \"${goal.name}\" and its contribution history? Your income and expense records will remain untouched.",
      confirmLabel = "Delete",
      onConfirm = {
        onDeleteGoal(goal)
        pendingDeleteGoal = null
      },
      onDismiss = { pendingDeleteGoal = null },
    )
  }

  val totalSaved = uiState.goalSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.savedAmount) }
  val totalTarget = uiState.goalSummaries.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.targetAmount) }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier
        .widthIn(max = 760.dp)
        .fillMaxSize(),
      contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Savings Goals",
              style = MaterialTheme.typography.displaySmall,
              maxLines = 1,
              modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            )
            FilledTonalButton(
              onClick = {
                editingGoal = null
                showGoalEditor = true
              },
              contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
              modifier = Modifier.testTag("add_goal_button"),
            ) {
              Icon(Icons.Rounded.Add, contentDescription = null)
              Text(
                text = "New Goal",
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(start = 4.dp),
              )
            }
          }
          Text(
            text = "Tracked independently from monthly cash flow",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Total Reserved Savings Hero Card
      item {
        val isHidden = LocalIsBalanceHidden.current
        Surface(
          shape = MaterialTheme.shapes.large,
          shadowElevation = 2.dp,
          modifier = Modifier.fillMaxWidth(),
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .background(
                brush = Brush.linearGradient(
                  colors = listOf(financeColors.heroStart, financeColors.heroEnd)
                )
              )
              .padding(20.dp),
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              @OptIn(ExperimentalLayoutApi::class)
              FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                Text(
                  text = "RESERVED SAVINGS",
                  style = MaterialTheme.typography.labelSmall.copy(
                    letterSpacing = 0.8.sp,
                    fontWeight = FontWeight.SemiBold,
                  ),
                  color = Color.White.copy(alpha = 0.85f),
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .padding(end = 8.dp),
                )
                Surface(
                  shape = RoundedCornerShape(50),
                  color = Color.White.copy(alpha = 0.16f),
                  modifier = Modifier.align(Alignment.CenterVertically),
                ) {
                  Text(
                    text = "${uiState.goalSummaries.count { it.status == GoalStatus.COMPLETED }}/${uiState.goalSummaries.size} completed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                  )
                }
              }

              MaskedCurrencyText(
                amount = totalSaved,
                currencyCode = currency,
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                autoResize = true,
                minFontSize = 22.sp,
                modifier = Modifier.fillMaxWidth(),
              )

              Text(
                text = "Combined goal targets: ${FinanceCalculator.formatCurrencyOrMasked(totalTarget, currency, isHidden)}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
              )
            }
          }
        }
      }

      if (uiState.goalSummaries.isEmpty()) {
        item {
          EmptyStatePanel(
            icon = Icons.Rounded.Savings,
            title = "No savings goals yet",
            message = "Create a savings target to track progress toward your financial milestones.",
            actionLabel = "Create Savings Goal",
            actionIcon = Icons.Rounded.Add,
            onAction = {
              editingGoal = null
              showGoalEditor = true
            },
          )
        }
      } else {
        item {
          SectionHeader(
            title = "Your goals (${uiState.goalSummaries.size})",
            icon = Icons.Rounded.Savings,
          )
        }
        items(uiState.goalSummaries, key = { it.goalId }) { summary ->
          val rawGoal = uiState.rawGoals.firstOrNull { it.id == summary.goalId }
          GoalDetailCard(
            goal = summary,
            currencyCode = currency,
            onContribute = { contributionTarget = summary },
            onHistory = { historyGoal = summary },
            onEdit = {
              if (rawGoal != null) {
                editingGoal = rawGoal
                showGoalEditor = true
              }
            },
            onDelete = {
              if (rawGoal != null) {
                pendingDeleteGoal = rawGoal
              }
            },
          )
        }
      }
    }
  }
}

@Composable
private fun GoalDetailCard(
  goal: SavingsGoalSummary,
  currencyCode: String,
  onContribute: () -> Unit,
  onHistory: () -> Unit,
  onEdit: () -> Unit,
  onDelete: () -> Unit,
) {
  val financeColors = LocalFinanceColors.current
  val rawProgress = (goal.completionPercent.toFloat() / 100f).coerceIn(0f, 1f)
  val statusColor = when (goal.status) {
    GoalStatus.COMPLETED -> financeColors.income
    GoalStatus.ACTIVE -> MaterialTheme.colorScheme.primary
    GoalStatus.PAUSED -> MaterialTheme.colorScheme.onSurfaceVariant
  }

  FintechCard(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("goal_card_${goal.goalId}"),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = goal.name,
          style = MaterialTheme.typography.titleMedium,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "Target date: ${formatEpochDay(goal.targetDateEpochDay)}",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = statusColor.copy(alpha = 0.14f),
      ) {
        Text(
          text = goal.status.label,
          style = MaterialTheme.typography.labelSmall,
          color = statusColor,
          maxLines = 1,
          softWrap = false,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))
    val isHidden = LocalIsBalanceHidden.current
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      MaskedCurrencyText(
        amount = goal.savedAmount,
        currencyCode = currencyCode,
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        color = statusColor,
        autoResize = true,
        minFontSize = 14.sp,
        modifier = Modifier
          .align(Alignment.Bottom)
          .padding(end = 8.dp),
      )
      Text(
        text = "${goal.completionPercent.toPlainString()}% of ${FinanceCalculator.formatCurrencyOrMasked(goal.targetAmount, currencyCode, isHidden)}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier.align(Alignment.Bottom),
      )
    }

    Spacer(modifier = Modifier.height(8.dp))
    FintechProgressBar(
      progress = rawProgress,
      color = statusColor,
      height = 8.dp,
    )

    Spacer(modifier = Modifier.height(6.dp))
    Text(
      text = if (goal.remainingAmount > BigDecimal.ZERO) {
        "${FinanceCalculator.formatCurrencyOrMasked(goal.remainingAmount, currencyCode, isHidden)} left to reach target"
      } else {
        "Goal target achieved!"
      },
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(12.dp))
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        FilledTonalButton(
          onClick = onContribute,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier.testTag("contribute_goal_${goal.goalId}"),
        ) {
          Text("Contribute", maxLines = 1, softWrap = false)
        }
        OutlinedButton(
          onClick = onHistory,
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
          modifier = Modifier.testTag("history_goal_${goal.goalId}"),
        ) {
          Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(16.dp))
          Text("History", maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 4.dp))
        }
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onEdit, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(Icons.Rounded.Edit, contentDescription = "Edit goal", modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onDelete, modifier = Modifier.minimumInteractiveComponentSize()) {
          Icon(
            Icons.Rounded.DeleteOutline,
            contentDescription = "Delete goal",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalEditorDialog(
  existing: SavingsGoalEntity?,
  currencyCode: String,
  onSave: (name: String, targetCentavos: Long, targetDateEpochDay: Long, status: GoalStatus) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by remember(existing) { mutableStateOf(existing?.name ?: "") }
  var targetText by remember(existing) {
    mutableStateOf(
      existing?.let { FinanceCalculator.centavosToBigDecimal(it.targetCentavos).toPlainString() } ?: ""
    )
  }
  var targetDateEpochDay by remember(existing) {
    mutableLongStateOf(existing?.targetDateEpochDay ?: (currentEpochDay() + 180L))
  }
  var status by remember(existing) { mutableStateOf(existing?.status ?: GoalStatus.ACTIVE) }
  var nameError by remember { mutableStateOf<String?>(null) }
  var amountError by remember { mutableStateOf<String?>(null) }
  var showDatePicker by remember { mutableStateOf(false) }

  val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol

  if (showDatePicker) {
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = targetDateEpochDay * MILLIS_PER_DAY)
    DatePickerDialog(
      onDismissRequest = { showDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          pickerState.selectedDateMillis?.let { targetDateEpochDay = it / MILLIS_PER_DAY }
          showDatePicker = false
        }) { Text("Select") }
      },
      dismissButton = {
        TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
      },
    ) { DatePicker(state = pickerState) }
  }

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(
        text = if (existing == null) "New Savings Goal" else "Edit Savings Goal",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    confirmButton = {
      Button(
        onClick = {
          val cleanName = name.trim()
          val centavos = FinanceCalculator.parseAmountToCentavos(targetText)
          var err = false
          if (cleanName.isEmpty()) {
            nameError = "Enter a goal name."
            err = true
          }
          if (centavos == null) {
            amountError = "Enter a positive target amount."
            err = true
          }
          if (!err && centavos != null) {
            onSave(cleanName, centavos, targetDateEpochDay, status)
          }
        },
        modifier = Modifier.testTag("save_goal_button"),
      ) {
        Text(if (existing == null) "Create" else "Save")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    OutlinedTextField(
      value = name,
      onValueChange = {
        name = it
        nameError = null
      },
      label = { Text("Goal Name") },
      placeholder = { Text("e.g., Emergency Fund") },
      isError = nameError != null,
      supportingText = nameError?.let { msg -> { Text(msg) } },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("goal_name_input"),
    )
    OutlinedTextField(
      value = targetText,
      onValueChange = {
        targetText = it
        amountError = null
      },
      label = { Text("Target Amount") },
      prefix = { Text("$currencySymbol ") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = amountError != null,
      supportingText = amountError?.let { msg -> { Text(msg) } },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("goal_target_input"),
    )
    OutlinedButton(
      onClick = { showDatePicker = true },
      modifier = Modifier.fillMaxWidth(),
    ) {
      Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
      Text("Target date: ${formatEpochDay(targetDateEpochDay)}", modifier = Modifier.padding(start = 6.dp))
    }
    if (existing != null) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
          text = "Status",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        SingleChoiceSegmentedButtonRow(
          modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        ) {
          GoalStatus.entries.forEachIndexed { index, option ->
            SegmentedButton(
              selected = status == option,
              onClick = { status = option },
              shape = SegmentedButtonDefaults.itemShape(
                index = index,
                count = GoalStatus.entries.size,
              ),
              icon = {},
              modifier = Modifier.testTag("goal_status_${option.name.lowercase()}"),
            ) {
              AutoResizeSingleLineText(
                text = option.label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = LocalContentColor.current,
                minFontSize = 10.sp,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun GoalContributionDialog(
  goal: SavingsGoalSummary,
  currencyCode: String,
  onSubmit: (amountCentavos: Long, isWithdrawal: Boolean, note: String, dateEpochDay: Long) -> Unit,
  onDismiss: () -> Unit,
) {
  var isWithdrawal by remember { mutableStateOf(false) }
  var amountText by remember { mutableStateOf("") }
  var note by remember { mutableStateOf("") }
  var amountError by remember { mutableStateOf<String?>(null) }
  val currencySymbol = CurrencyOption.fromCode(currencyCode).symbol

  StableFormDialog(
    onDismissRequest = onDismiss,
    title = { Text(goal.name, style = MaterialTheme.typography.titleLarge) },
    confirmButton = {
      Button(
        onClick = {
          val centavos = FinanceCalculator.parseAmountToCentavos(amountText)
          val maxAvailable = FinanceCalculator.bigDecimalToCentavos(goal.savedAmount)
          when {
            centavos == null -> amountError = "Enter a valid positive amount."
            isWithdrawal && centavos > maxAvailable ->
              amountError = "Cannot withdraw more than current saved amount."
            else -> onSubmit(centavos, isWithdrawal, note, currentEpochDay())
          }
        },
        modifier = Modifier.testTag("submit_contribution_button"),
      ) {
        Text(if (isWithdrawal) "Record Withdrawal" else "Add Contribution")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) { Text("Cancel") }
    },
  ) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      SegmentedButton(
        selected = !isWithdrawal,
        onClick = { isWithdrawal = false; amountError = null },
        shape = SegmentedButtonDefaults.itemShape(0, 2),
      ) { Text("Deposit") }
      SegmentedButton(
        selected = isWithdrawal,
        onClick = { isWithdrawal = true; amountError = null },
        shape = SegmentedButtonDefaults.itemShape(1, 2),
      ) { Text("Withdraw") }
    }
    val isHidden = LocalIsBalanceHidden.current
    Text(
      text = "Current saved: ${FinanceCalculator.formatCurrencyOrMasked(goal.savedAmount, currencyCode, isHidden)}",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedTextField(
      value = amountText,
      onValueChange = {
        amountText = it
        amountError = null
      },
      label = { Text("Amount") },
      prefix = { Text("$currencySymbol ") },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = amountError != null,
      supportingText = amountError?.let { msg -> { Text(msg) } },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("contribution_amount_input"),
    )
    OutlinedTextField(
      value = note,
      onValueChange = { note = it },
      label = { Text("Note (optional)") },
      placeholder = { Text(if (isWithdrawal) "e.g., Correction / emergency use" else "e.g., Monthly transfer") },
      singleLine = true,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalHistorySheet(
  goal: SavingsGoalSummary,
  contributions: List<SavingsContributionEntity>,
  currencyCode: String,
  onDismiss: () -> Unit,
) {
  val financeColors = LocalFinanceColors.current
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var expandedIds by remember(goal.goalId) { mutableStateOf(emptySet<Long>()) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    modifier = Modifier
      .statusBarsPadding()
      .testTag("goal_history_sheet"),
  ) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
      val maxSheetContentHeight = (maxHeight * 0.86f).coerceAtLeast(240.dp)
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = maxSheetContentHeight)
          .navigationBarsPadding()
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Rounded.History,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp),
          )
          Text(
            text = "${goal.name} · History",
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
          )
        }

        if (contributions.isEmpty()) {
          Text(
            text = "No contributions or adjustments recorded for this goal yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 32.dp),
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f, fill = false)
              .testTag("goal_history_list"),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            items(contributions, key = { it.id }) { entry ->
              val isExpanded = entry.id in expandedIds
              var isExpandable by remember(entry.id, entry.note) { mutableStateOf(false) }
              val noteText = entry.note.ifBlank {
                if (entry.isWithdrawal) "Withdrawal / correction" else "Contribution"
              }
              val isHidden = LocalIsBalanceHidden.current

              Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(MaterialTheme.shapes.small)
                  .clickable {
                    expandedIds = if (isExpanded) {
                      expandedIds - entry.id
                    } else {
                      expandedIds + entry.id
                    }
                  }
                  .animateContentSize()
                  .testTag("goal_history_item_${entry.id}"),
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.spacedBy(12.dp),
                  verticalAlignment = Alignment.Top,
                ) {
                  Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                  ) {
                    Text(
                      text = noteText,
                      style = MaterialTheme.typography.titleSmall,
                      maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                      overflow = TextOverflow.Ellipsis,
                      onTextLayout = { result ->
                        if (!isExpanded && result.hasVisualOverflow) {
                          isExpandable = true
                        }
                      },
                    )
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                      Text(
                        text = formatEpochDay(entry.dateEpochDay),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                      if (isExpandable || isExpanded) {
                        Text(
                          text = if (isExpanded) "Show less" else "Tap to expand",
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.primary,
                        )
                      }
                    }
                  }
                  Text(
                    text = if (isHidden) {
                      FinanceCalculator.formatMaskedCurrency(currencyCode)
                    } else {
                      FinanceCalculator.formatCentavos(
                        centavos = entry.amountCentavos,
                        currencyCode = currencyCode,
                        showPlusSign = !entry.isWithdrawal,
                        forceMinusSign = entry.isWithdrawal,
                      )
                    },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = if (entry.isWithdrawal) financeColors.expense else financeColors.income,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
