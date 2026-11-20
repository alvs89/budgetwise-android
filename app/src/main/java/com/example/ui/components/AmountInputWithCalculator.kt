package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AmountCalculatorEngine
import com.example.domain.CalcEvaluation
import com.example.domain.CurrencyOption
import com.example.domain.FinanceCalculator
import java.math.BigDecimal

/**
 * Reusable monetary amount input field with a zero-clutter trailing calculator toggle.
 *
 * Users can type plain amounts directly with the system keyboard, or tap the trailing
 * calculator button to hide the soft keyboard and expand a compact 4×5 emerald arithmetic
 * keypad directly below the field.
 */
@Composable
fun AmountInputWithCalculator(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  currencyCode: String,
  fieldTestTag: String,
  modifier: Modifier = Modifier,
  errorText: String? = null,
) {
  val currencySymbol = remember(currencyCode) { CurrencyOption.fromCode(currencyCode).symbol }
  val keyboardController = LocalSoftwareKeyboardController.current
  val focusManager = LocalFocusManager.current

  var isCalculatorOpen by remember { mutableStateOf(false) }
  var expression by remember { mutableStateOf("") }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = { Text(label) },
      prefix = {
        Text(
          text = "$currencySymbol ",
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
        )
      },
      trailingIcon = {
        IconButton(
          onClick = {
            if (!isCalculatorOpen) {
              keyboardController?.hide()
              focusManager.clearFocus(force = true)
              expression = AmountCalculatorEngine.normalizeSeedAmount(value)
              isCalculatorOpen = true
            } else {
              isCalculatorOpen = false
            }
          },
          modifier = Modifier.testTag("${fieldTestTag}_calc_toggle"),
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(
                if (isCalculatorOpen) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  Color.Transparent
                },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Rounded.Calculate,
              contentDescription = if (isCalculatorOpen) "Close calculator" else "Open calculator",
              tint = if (isCalculatorOpen) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(21.dp),
            )
          }
        }
      },
      keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
      isError = errorText != null,
      supportingText = errorText?.let { msg -> { Text(msg) } },
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .onFocusChanged { focusState ->
          if (focusState.isFocused && isCalculatorOpen) {
            isCalculatorOpen = false
          }
        }
        .testTag(fieldTestTag),
    )

    AnimatedVisibility(
      visible = isCalculatorOpen,
      enter = expandVertically() + fadeIn(),
      exit = shrinkVertically() + fadeOut(),
    ) {
      InlineCalculatorKeypad(
        expression = expression,
        currencyCode = currencyCode,
        onExpressionChange = { expression = it },
        onApply = { evaluatedAmount ->
          onValueChange(AmountCalculatorEngine.formatForAmountField(evaluatedAmount))
          isCalculatorOpen = false
        },
        testTagPrefix = "${fieldTestTag}_calc",
      )
    }
  }
}

@Composable
private fun InlineCalculatorKeypad(
  expression: String,
  currencyCode: String,
  onExpressionChange: (String) -> Unit,
  onApply: (BigDecimal) -> Unit,
  testTagPrefix: String,
) {
  val evaluation = remember(expression) { AmountCalculatorEngine.evaluate(expression) }
  val formattedExpression = remember(expression) {
    AmountCalculatorEngine.formatExpressionForDisplay(expression)
  }
  val exprScrollState = rememberScrollState()
  LaunchedEffect(formattedExpression) {
    exprScrollState.scrollTo(exprScrollState.maxValue)
  }

  val canApply = evaluation is CalcEvaluation.Valid && evaluation.value > BigDecimal.ZERO
  val previewText = when (evaluation) {
    CalcEvaluation.Empty -> "= ${FinanceCalculator.formatCurrency(BigDecimal.ZERO, currencyCode)}"
    is CalcEvaluation.Valid -> "= ${FinanceCalculator.formatCurrency(evaluation.value, currencyCode)}"
    is CalcEvaluation.Error -> evaluation.message
  }
  val isErrorPreview = evaluation is CalcEvaluation.Error

  Surface(
    shape = RoundedCornerShape(16.dp),
    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier
      .fillMaxWidth()
      .testTag("${testTagPrefix}_panel"),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Live expression + real-time evaluated preview header
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
          horizontalAlignment = Alignment.End,
          verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
          Text(
            text = formattedExpression,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
              .horizontalScroll(exprScrollState)
              .testTag("${testTagPrefix}_expression"),
          )
          Text(
            text = previewText,
            style = if (isErrorPreview) {
              MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
            } else {
              MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            },
            color = if (isErrorPreview) {
              MaterialTheme.colorScheme.error
            } else {
              MaterialTheme.colorScheme.primary
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.testTag("${testTagPrefix}_preview"),
          )
        }
      }

      // 4×5 Balanced Tactile Keypad
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        // Row 1: C | ⌫ | ÷ | ×
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CalcKeyButton(
            label = "C",
            onClick = { onExpressionChange(AmountCalculatorEngine.clear()) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.error,
            testTag = "${testTagPrefix}_key_clear",
            modifier = Modifier.weight(1f),
          )
          CalcIconKeyButton(
            contentDescription = "Backspace",
            onClick = { onExpressionChange(AmountCalculatorEngine.backspace(expression)) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            testTag = "${testTagPrefix}_key_backspace",
            modifier = Modifier.weight(1f),
          )
          CalcKeyButton(
            label = "÷",
            onClick = { onExpressionChange(AmountCalculatorEngine.appendOperator(expression, '÷')) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            contentColor = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_div",
            modifier = Modifier.weight(1f),
          )
          CalcKeyButton(
            label = "×",
            onClick = { onExpressionChange(AmountCalculatorEngine.appendOperator(expression, '×')) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            contentColor = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_mul",
            modifier = Modifier.weight(1f),
          )
        }

        // Row 2: 7 | 8 | 9 | −
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CalcDigitKey("7", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("8", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("9", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcKeyButton(
            label = "−",
            onClick = { onExpressionChange(AmountCalculatorEngine.appendOperator(expression, '−')) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            contentColor = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_sub",
            modifier = Modifier.weight(1f),
          )
        }

        // Row 3: 4 | 5 | 6 | +
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CalcDigitKey("4", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("5", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("6", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcKeyButton(
            label = "+",
            onClick = { onExpressionChange(AmountCalculatorEngine.appendOperator(expression, '+')) },
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f),
            contentColor = MaterialTheme.colorScheme.primary,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_add",
            modifier = Modifier.weight(1f),
          )
        }

        // Row 4: 1 | 2 | 3 | =
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CalcDigitKey("1", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("2", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("3", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcKeyButton(
            label = "=",
            onClick = { onExpressionChange(AmountCalculatorEngine.commitEquals(expression)) },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_eq",
            modifier = Modifier.weight(1f),
          )
        }

        // Row 5: 00 | 0 | . | Apply
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          CalcDigitKey("00", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcDigitKey("0", expression, onExpressionChange, testTagPrefix, Modifier.weight(1f))
          CalcKeyButton(
            label = ".",
            onClick = { onExpressionChange(AmountCalculatorEngine.appendDecimal(expression)) },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            fontSize = 20.sp,
            testTag = "${testTagPrefix}_key_dot",
            modifier = Modifier.weight(1f),
          )
          CalcKeyButton(
            label = "Apply",
            onClick = {
              if (evaluation is CalcEvaluation.Valid && evaluation.value > BigDecimal.ZERO) {
                onApply(evaluation.value)
              }
            },
            enabled = canApply,
            containerColor = if (canApply) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
            },
            contentColor = if (canApply) {
              MaterialTheme.colorScheme.onPrimary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
            fontSize = 14.sp,
            testTag = "${testTagPrefix}_key_apply",
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun CalcDigitKey(
  digit: String,
  expression: String,
  onExpressionChange: (String) -> Unit,
  testTagPrefix: String,
  modifier: Modifier = Modifier,
) {
  CalcKeyButton(
    label = digit,
    onClick = { onExpressionChange(AmountCalculatorEngine.appendDigit(expression, digit)) },
    containerColor = MaterialTheme.colorScheme.surface,
    contentColor = MaterialTheme.colorScheme.onSurface,
    testTag = "${testTagPrefix}_key_$digit",
    modifier = modifier,
  )
}

@Composable
private fun CalcKeyButton(
  label: String,
  onClick: () -> Unit,
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(
      width = 0.75.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    ),
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(enabled = enabled, onClick = onClick)
      .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = fontSize,
          fontWeight = FontWeight.SemiBold,
        ),
        color = contentColor,
        maxLines = 1,
        softWrap = false,
      )
    }
  }
}

@Composable
private fun CalcIconKeyButton(
  contentDescription: String,
  onClick: () -> Unit,
  containerColor: Color,
  contentColor: Color,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = containerColor,
    border = BorderStroke(
      width = 0.75.dp,
      color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
    ),
    modifier = modifier
      .height(44.dp)
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
  ) {
    Box(contentAlignment = Alignment.Center) {
      Icon(
        imageVector = Icons.AutoMirrored.Rounded.Backspace,
        contentDescription = contentDescription,
        tint = contentColor,
        modifier = Modifier.size(19.dp),
      )
    }
  }
}
