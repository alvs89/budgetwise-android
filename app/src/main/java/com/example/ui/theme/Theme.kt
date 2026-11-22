package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.domain.ThemeMode

data class FinanceSemanticColors(
  val income: Color,
  val incomeContainer: Color,
  val expense: Color,
  val expenseContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val nearLimit: Color,
  val heroStart: Color,
  val heroEnd: Color,
)

val LocalFinanceColors = staticCompositionLocalOf {
  FinanceSemanticColors(
    income = IncomeGreen,
    incomeContainer = IncomeContainerLight,
    expense = ExpenseRed,
    expenseContainer = ExpenseContainerLight,
    warning = WarningAmber,
    warningContainer = WarningContainerLight,
    nearLimit = NearLimitOrange,
    heroStart = ForestGreenDark,
    heroEnd = DarkNavy,
  )
}

private val LightColorScheme = lightColorScheme(
  primary = ForestGreen,
  onPrimary = Color.White,
  primaryContainer = MintContainer,
  onPrimaryContainer = ForestGreenDark,
  secondary = ForestGreenLight,
  onSecondary = Color.White,
  secondaryContainer = EmeraldSecondaryContainerLight,
  onSecondaryContainer = ForestGreenDark,
  tertiary = ForestGreenLight,
  onTertiary = Color.White,
  background = SoftWhiteBg,
  onBackground = Color(0xFF0F1D18),
  surface = CardSurfaceLight,
  onSurface = Color(0xFF0F1D18),
  surfaceVariant = CardSurfaceElevatedLight,
  onSurfaceVariant = Color(0xFF4B5E56),
  outline = OutlineLight,
  outlineVariant = Color(0xFFE9EEEC),
  error = ExpenseRed,
  onError = Color.White,
  errorContainer = ExpenseContainerLight,
  onErrorContainer = Color(0xFF7F1D1D),
)

private val DarkColorScheme = darkColorScheme(
  primary = MintAccent,
  onPrimary = ForestGreenDark,
  primaryContainer = Color(0xFF134E3A),
  onPrimaryContainer = MintContainer,
  secondary = Color(0xFF5EEAD4),
  onSecondary = ForestGreenDark,
  secondaryContainer = EmeraldSecondaryContainerDark,
  onSecondaryContainer = EmeraldOnSecondaryContainerDark,
  tertiary = Color(0xFF34D399),
  onTertiary = ForestGreenDark,
  background = DeepCharcoalBg,
  onBackground = Color(0xFFECF3F0),
  surface = CardSurfaceDark,
  onSurface = Color(0xFFECF3F0),
  surfaceVariant = CardSurfaceElevatedDark,
  onSurfaceVariant = Color(0xFF9DB3AA),
  outline = OutlineDark,
  outlineVariant = Color(0xFF1F2E28),
  error = ExpenseRedDark,
  onError = Color(0xFF450A0A),
  errorContainer = ExpenseContainerDark,
  onErrorContainer = Color(0xFFFECACA),
)

val BudgetWiseShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun BudgetWiseTheme(
  themeMode: ThemeMode = ThemeMode.SYSTEM,
  content: @Composable () -> Unit,
) {
  val useDark = when (themeMode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
  }

  val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
  val semanticColors = if (useDark) {
    FinanceSemanticColors(
      income = IncomeGreenDark,
      incomeContainer = IncomeContainerDark,
      expense = ExpenseRedDark,
      expenseContainer = ExpenseContainerDark,
      warning = WarningAmberDark,
      warningContainer = WarningContainerDark,
      nearLimit = NearLimitOrangeDark,
      heroStart = Color(0xFF0E4534),
      heroEnd = Color(0xFF0B2A22),
    )
  } else {
    FinanceSemanticColors(
      income = IncomeGreen,
      incomeContainer = IncomeContainerLight,
      expense = ExpenseRed,
      expenseContainer = ExpenseContainerLight,
      warning = WarningAmber,
      warningContainer = WarningContainerLight,
      nearLimit = NearLimitOrange,
      heroStart = ForestGreen,
      heroEnd = ForestGreenDark,
    )
  }

  CompositionLocalProvider(LocalFinanceColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = BudgetWiseTypography,
      shapes = BudgetWiseShapes,
      content = content,
    )
  }
}
