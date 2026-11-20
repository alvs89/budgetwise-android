package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Android's system sans-serif family provides genuine optical weights across
 * 400 (Normal), 500 (Medium), 600 (SemiBold), and 700 (Bold) without static-font
 * weight collapse or synthetic fake-bold distortion.
 */
val PlusJakartaSansFamily: FontFamily = FontFamily.SansSerif
val InterFontFamily: FontFamily = FontFamily.SansSerif

/**
 * 4-Tier Material 3 Fintech Typography Scale:
 * - Tier 1 (Bold / 700): Screen titles & hero balances/metrics with tight -0.5.sp tracking.
 * - Tier 2 (SemiBold / 600): Modal titles, card titles, and section headers.
 * - Tier 3 (Medium / 500): Form field labels, chip labels, and list item titles.
 * - Tier 4 (Normal / 400): Supporting copy, subtitles, descriptions, and timestamps.
 */
val BudgetWiseTypography = Typography(
  // Tier 1: Hero balances & primary metrics
  displayLarge = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 36.sp,
    lineHeight = 44.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
  ),
  displayMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 30.sp,
    lineHeight = 36.sp,
    letterSpacing = (-0.5).sp,
    fontFeatureSettings = "tnum",
  ),
  // Tier 1: Main screen titles (My Finances, Budgets, Savings Goals, Insights, Activity)
  displaySmall = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    lineHeight = 34.sp,
    letterSpacing = (-0.5).sp,
  ),
  // Tier 1: Hero secondary metrics (e.g., ₱12,140 / ₱15,000)
  headlineMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    lineHeight = 30.sp,
    letterSpacing = (-0.4).sp,
    fontFeatureSettings = "tnum",
  ),
  // Tier 2: Modal headers (Add Transaction, Edit Budget, Notifications)
  headlineSmall = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 20.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.2).sp,
  ),
  // Tier 2: Dialog titles & prominent card titles
  titleLarge = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 18.sp,
    lineHeight = 24.sp,
    letterSpacing = (-0.15).sp,
  ),
  // Tier 2: Card & section titles (Spending overview, Category limits, Key Observations)
  titleMedium = TextStyle(
    fontFamily = PlusJakartaSansFamily,
    fontWeight = FontWeight.SemiBold,
    fontSize = 16.sp,
    lineHeight = 22.sp,
  ),
  // Tier 3: List item titles & category names
  titleSmall = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  // Tier 4: Primary body & supporting copy
  bodyLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 15.sp,
    lineHeight = 22.sp,
  ),
  bodyMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    lineHeight = 20.sp,
  ),
  bodySmall = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    lineHeight = 17.sp,
  ),
  // Tier 3: Form field labels & button labels
  labelLarge = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.1.sp,
  ),
  labelMedium = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 0.25.sp,
  ),
  labelSmall = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.4.sp,
  ),
)
