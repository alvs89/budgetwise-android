package com.example.domain

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

sealed interface CalcEvaluation {
  data object Empty : CalcEvaluation
  data class Valid(val value: BigDecimal) : CalcEvaluation
  data class Error(val message: String) : CalcEvaluation
}

/**
 * Pure, deterministic inline arithmetic calculator engine for monetary input fields.
 *
 * Evaluates standard `+`, `−`, `×`, `÷` expressions using [BigDecimal] with proper operator
 * precedence (`×` and `÷` before `+` and `−`), guarding against division by zero, negative
 * totals, duplicate decimals, and overflow beyond BudgetWise's safe monetary range.
 */
object AmountCalculatorEngine {

  private const val MAX_EXPRESSION_LENGTH = 48
  private const val MAX_OPERAND_DIGITS = 12
  private val MAX_AMOUNT = BigDecimal("999999999999.99")
  private val OPERATORS = setOf('+', '−', '×', '÷')

  fun normalizeSeedAmount(rawAmount: String): String {
    val cleaned = rawAmount.trim().replace(",", "")
    if (cleaned.isEmpty()) return ""
    val parsed = cleaned.toBigDecimalOrNull() ?: return ""
    if (parsed <= BigDecimal.ZERO || parsed > MAX_AMOUNT) return ""
    return formatForAmountField(parsed)
  }

  fun appendDigit(expression: String, digit: String): String {
    if (expression.length + digit.length > MAX_EXPRESSION_LENGTH) return expression
    val currentOperand = currentOperandOf(expression)
    val digitsOnlyCount = currentOperand.count { it.isDigit() }
    if (digitsOnlyCount + digit.length > MAX_OPERAND_DIGITS) return expression

    // Restrict fractional entry to 2 decimal places when typing digits directly
    val dotIndex = currentOperand.indexOf('.')
    if (dotIndex >= 0) {
      val decimalsSoFar = currentOperand.length - dotIndex - 1
      if (decimalsSoFar >= 2) return expression
      val allowed = digit.take((2 - decimalsSoFar).coerceAtLeast(0))
      return if (allowed.isEmpty()) expression else expression + allowed
    }

    if (digit == "00") {
      return when {
        currentOperand.isEmpty() -> expression + "0"
        currentOperand == "0" -> expression
        else -> expression + "00"
      }
    }

    if (currentOperand == "0") {
      return if (digit == "0") expression else expression.dropLast(1) + digit
    }

    return expression + digit
  }

  fun appendDecimal(expression: String): String {
    if (expression.length >= MAX_EXPRESSION_LENGTH) return expression
    val currentOperand = currentOperandOf(expression)
    if (currentOperand.contains('.')) return expression
    return if (currentOperand.isEmpty()) {
      expression + "0."
    } else {
      expression + "."
    }
  }

  fun appendOperator(expression: String, operator: Char): String {
    if (operator !in OPERATORS) return expression
    if (expression.isEmpty()) return expression
    val last = expression.last()
    return when {
      last in OPERATORS -> expression.dropLast(1) + operator
      last == '.' -> expression.dropLast(1) + operator
      expression.length >= MAX_EXPRESSION_LENGTH -> expression
      else -> expression + operator
    }
  }

  fun backspace(expression: String): String =
    if (expression.isEmpty()) "" else expression.dropLast(1)

  fun clear(): String = ""

  /**
   * Collapses a valid expression into its rounded numeric result when the user taps `=`.
   * Leaves the expression intact if evaluation produces an error or empty state.
   */
  fun commitEquals(expression: String): String {
    return when (val eval = evaluate(expression)) {
      is CalcEvaluation.Valid -> formatForAmountField(eval.value)
      else -> expression
    }
  }

  fun evaluate(rawExpression: String): CalcEvaluation {
    val sanitized = rawExpression
      .replace(" ", "")
      .replace(",", "")
      .replace('*', '×')
      .replace('/', '÷')
      .replace('-', '−')
      .trim()

    if (sanitized.isEmpty()) return CalcEvaluation.Empty

    // Allow live preview even while the user has just tapped a trailing operator or decimal
    val effective = sanitized.trimEnd { it in OPERATORS || it == '.' }
    if (effective.isEmpty()) return CalcEvaluation.Empty

    val numbers = mutableListOf<BigDecimal>()
    val ops = mutableListOf<Char>()
    val tokenBuilder = StringBuilder()

    for (ch in effective) {
      if (ch in OPERATORS) {
        val numberStr = tokenBuilder.toString()
        val num = numberStr.toBigDecimalOrNull() ?: return CalcEvaluation.Error("Invalid expression")
        numbers.add(num)
        ops.add(ch)
        tokenBuilder.clear()
      } else if (ch.isDigit() || ch == '.') {
        tokenBuilder.append(ch)
      } else {
        return CalcEvaluation.Error("Invalid character")
      }
    }

    val lastNum = tokenBuilder.toString().toBigDecimalOrNull()
      ?: return CalcEvaluation.Error("Invalid expression")
    numbers.add(lastNum)

    if (numbers.size != ops.size + 1) return CalcEvaluation.Error("Invalid expression")

    // Pass 1: Multiply & Divide (left to right)
    val collapsedNumbers = mutableListOf(numbers.first())
    val collapsedOps = mutableListOf<Char>()

    for (i in ops.indices) {
      val op = ops[i]
      val rhs = numbers[i + 1]
      when (op) {
        '×' -> {
          val lhs = collapsedNumbers.removeAt(collapsedNumbers.lastIndex)
          val product = try {
            lhs.multiply(rhs, MathContext.DECIMAL128)
          } catch (_: ArithmeticException) {
            return CalcEvaluation.Error("Amount is too large")
          }
          if (product.abs() > MAX_AMOUNT.multiply(BigDecimal("1000"))) {
            return CalcEvaluation.Error("Amount is too large")
          }
          collapsedNumbers.add(product)
        }
        '÷' -> {
          if (rhs.compareTo(BigDecimal.ZERO) == 0) {
            return CalcEvaluation.Error("Cannot divide by zero")
          }
          val lhs = collapsedNumbers.removeAt(collapsedNumbers.lastIndex)
          val quotient = try {
            lhs.divide(rhs, MathContext.DECIMAL128)
          } catch (_: ArithmeticException) {
            return CalcEvaluation.Error("Calculation error")
          }
          collapsedNumbers.add(quotient)
        }
        else -> {
          collapsedOps.add(op)
          collapsedNumbers.add(rhs)
        }
      }
    }

    // Pass 2: Add & Subtract (left to right)
    var result = collapsedNumbers.first()
    for (i in collapsedOps.indices) {
      val rhs = collapsedNumbers[i + 1]
      result = when (collapsedOps[i]) {
        '+' -> result.add(rhs, MathContext.DECIMAL128)
        '−' -> result.subtract(rhs, MathContext.DECIMAL128)
        else -> result
      }
    }

    val rounded = try {
      result.setScale(2, RoundingMode.HALF_UP)
    } catch (_: ArithmeticException) {
      return CalcEvaluation.Error("Amount is too large")
    }

    return when {
      rounded < BigDecimal.ZERO -> CalcEvaluation.Error("Amount cannot be negative")
      rounded > MAX_AMOUNT -> CalcEvaluation.Error("Amount is too large")
      else -> CalcEvaluation.Valid(rounded)
    }
  }

  /** Formats `1850÷4` cleanly into `1,850 ÷ 4` for display above the keypad. */
  fun formatExpressionForDisplay(expression: String): String {
    if (expression.isEmpty()) return "0"
    val out = StringBuilder()
    val operand = StringBuilder()
    val intFormatter = NumberFormat.getIntegerInstance(Locale.US)

    fun flushOperand() {
      if (operand.isEmpty()) return
      val raw = operand.toString()
      val dotIdx = raw.indexOf('.')
      val intPart = if (dotIdx >= 0) raw.substring(0, dotIdx) else raw
      val fracPart = if (dotIdx >= 0) raw.substring(dotIdx) else ""
      val formattedInt = intPart.toLongOrNull()?.let { intFormatter.format(it) } ?: intPart
      out.append(formattedInt).append(fracPart)
      operand.clear()
    }

    for (ch in expression) {
      if (ch in OPERATORS) {
        flushOperand()
        out.append(' ').append(ch).append(' ')
      } else {
        operand.append(ch)
      }
    }
    flushOperand()
    return out.toString().trimEnd()
  }

  /**
   * Formats a positive evaluated [BigDecimal] for insertion into the numeric Amount text field:
   * exact whole numbers omit `.00` (`215`), while fractional results keep 2 decimals (`462.50`).
   */
  fun formatForAmountField(value: BigDecimal): String {
    val scaled = value.setScale(2, RoundingMode.HALF_UP)
    return if (scaled.stripTrailingZeros().scale() <= 0) {
      scaled.setScale(0, RoundingMode.HALF_UP).toPlainString()
    } else {
      scaled.toPlainString()
    }
  }

  private fun currentOperandOf(expression: String): String {
    val lastOpIndex = expression.indexOfLast { it in OPERATORS }
    return if (lastOpIndex < 0) expression else expression.substring(lastOpIndex + 1)
  }
}
