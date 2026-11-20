package com.example.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Checkroom
import androidx.compose.material.icons.rounded.ChildCare
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LaptopMac
import androidx.compose.material.icons.rounded.LocalBar
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.MedicalServices
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.graphics.ColorUtils
import com.example.data.local.entity.CategoryEntity
import com.example.domain.CategoryVisualMatcher
import com.example.domain.TransactionType

data class CategoryIconOption(
  val iconKey: String,
  val label: String,
  val icon: ImageVector,
  val colorHex: Long,
)

fun iconForCategoryKey(iconKey: String): ImageVector = when (iconKey) {
  "wallet" -> Icons.Rounded.AccountBalanceWallet
  "work" -> Icons.Rounded.Work
  "laptop" -> Icons.Rounded.LaptopMac
  "store" -> Icons.Rounded.Storefront
  "payments" -> Icons.Rounded.Payments
  "trending_up" -> Icons.AutoMirrored.Rounded.TrendingUp
  "bonus" -> Icons.Rounded.WorkspacePremium
  "savings" -> Icons.Rounded.Savings
  "gift" -> Icons.Rounded.CardGiftcard
  "charity" -> Icons.Rounded.VolunteerActivism
  "bank" -> Icons.Rounded.AccountBalance
  "restaurant" -> Icons.Rounded.Restaurant
  "coffee" -> Icons.Rounded.LocalCafe
  "groceries" -> Icons.Rounded.ShoppingCart
  "directions_bus" -> Icons.Rounded.DirectionsBus
  "car" -> Icons.Rounded.DirectionsCar
  "flight" -> Icons.Rounded.Flight
  "school" -> Icons.Rounded.School
  "receipt" -> Icons.AutoMirrored.Rounded.ReceiptLong
  "bolt" -> Icons.Rounded.Bolt
  "wifi" -> Icons.Rounded.Wifi
  "smartphone" -> Icons.Rounded.Smartphone
  "shopping_bag" -> Icons.Rounded.ShoppingBag
  "devices" -> Icons.Rounded.Devices
  "clothing" -> Icons.Rounded.Checkroom
  "movie" -> Icons.Rounded.Movie
  "subscriptions" -> Icons.Rounded.Autorenew
  "gaming" -> Icons.Rounded.SportsEsports
  "music" -> Icons.Rounded.MusicNote
  "book" -> Icons.AutoMirrored.Rounded.MenuBook
  "drinks" -> Icons.Rounded.LocalBar
  "medical" -> Icons.Rounded.MedicalServices
  "fitness" -> Icons.Rounded.FitnessCenter
  "home" -> Icons.Rounded.Home
  "repair" -> Icons.Rounded.Build
  "spa" -> Icons.Rounded.Spa
  "pets" -> Icons.Rounded.Pets
  "family" -> Icons.Rounded.ChildCare
  "label" -> Icons.AutoMirrored.Rounded.Label
  else -> Icons.Rounded.Category
}

/**
 * Curated library of 36 matching Material vector icons (ordered with the most relevant
 * icons first for the selected transaction type).
 */
fun pickableCategoryIcons(type: TransactionType): List<CategoryIconOption> {
  val incomeFirst = listOf(
    "work", "wallet", "laptop", "store", "trending_up", "bonus",
    "gift", "payments", "savings", "bank", "charity", "label",
  )
  val expenseCore = listOf(
    "restaurant", "groceries", "coffee", "directions_bus", "car", "home",
    "receipt", "bolt", "wifi", "smartphone", "school", "medical",
    "shopping_bag", "devices", "clothing", "movie", "subscriptions", "gaming",
    "spa", "fitness", "flight", "pets", "charity", "repair",
    "family", "music", "book", "drinks", "gift", "bank",
    "savings", "work", "wallet", "laptop", "label", "category",
  )
  val orderedKeys = if (type == TransactionType.INCOME) {
    (incomeFirst + expenseCore).distinct()
  } else {
    expenseCore.distinct()
  }
  return orderedKeys.mapNotNull { key ->
    CategoryVisualMatcher.ruleForIconKey(key)?.let { rule ->
      CategoryIconOption(
        iconKey = key,
        label = rule.label,
        icon = iconForCategoryKey(key),
        colorHex = rule.colorHex,
      )
    }
  }
}

fun categoryIcon(
  category: String,
  categories: List<CategoryEntity> = emptyList(),
): ImageVector {
  val matchedEntity = categories.firstOrNull { it.name.trim().equals(category.trim(), ignoreCase = true) }
  if (matchedEntity != null) {
    return iconForCategoryKey(matchedEntity.iconKey)
  }
  return iconForCategoryKey(CategoryVisualMatcher.inferIconKey(category))
}

@Composable
fun accessibleCategoryColor(
  rawColor: Color,
  surfaceColor: Color = MaterialTheme.colorScheme.surface,
): Color = adaptColorForSurface(rawColor, isDarkSurface = surfaceColor.luminance() < 0.5f)

fun adaptColorForSurface(
  rawColor: Color,
  isDarkSurface: Boolean,
): Color {
  val argb = rawColor.toArgb()
  val hsl = FloatArray(3)
  ColorUtils.colorToHSL(argb, hsl)
  val lum = ColorUtils.calculateLuminance(argb)
  if (isDarkSurface && lum < 0.24) {
    if (hsl[1] > 0.08f) {
      hsl[1] = hsl[1].coerceAtLeast(0.55f)
    }
    hsl[2] = hsl[2].coerceAtLeast(0.64f)
    return Color(ColorUtils.HSLToColor(hsl))
  }
  if (!isDarkSurface && lum > 0.52) {
    hsl[2] = hsl[2].coerceAtMost(0.42f)
    return Color(ColorUtils.HSLToColor(hsl))
  }
  return rawColor
}

@Composable
fun categoryTint(
  category: String,
  categories: List<CategoryEntity> = emptyList(),
): Color {
  val matchedEntity = categories.firstOrNull { it.name.trim().equals(category.trim(), ignoreCase = true) }
  val rawColor = if (matchedEntity != null) {
    Color(matchedEntity.colorHex)
  } else {
    Color(CategoryVisualMatcher.inferColorHex(category))
  }
  return accessibleCategoryColor(rawColor)
}

fun isOtherCategoryName(name: String): Boolean {
  val normalized = name.trim().lowercase()
  return normalized == "other" || normalized == "others" || normalized.startsWith("other ")
}

fun sortCategoriesOtherLast(categories: List<String>): List<String> =
  categories.distinct().sortedWith(compareBy<String>({ isOtherCategoryName(it) }, { it.lowercase() }))
