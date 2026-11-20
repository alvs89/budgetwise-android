package com.example.domain

import kotlin.math.abs

data class CategoryVisualRule(
  val iconKey: String,
  val label: String,
  val colorHex: Long,
  val exactNames: List<String> = emptyList(),
  val keywords: List<String> = emptyList(),
)

object CategoryVisualMatcher {

  val accentSwatches: List<Long> = listOf(
    0xFF10B981, // Emerald
    0xFF0EA5A4, // Teal
    0xFF0284C7, // Sky Blue
    0xFF3B82F6, // Royal Blue
    0xFF6366F1, // Indigo
    0xFF7C3AED, // Violet
    0xFFDB2777, // Pink
    0xFFF43F5E, // Coral Red
    0xFFEA580C, // Tangerine
    0xFFEAB308, // Amber Gold
  )

  private val customPalette = accentSwatches

  val rules: List<CategoryVisualRule> = listOf(
    // Specific multi-domain / everyday categories checked with high specificity
    CategoryVisualRule(
      iconKey = "coffee",
      label = "Coffee & Snacks",
      colorHex = 0xFFEA580C,
      exactNames = listOf("coffee and snacks", "coffee & snacks"),
      keywords = listOf("coffee", "cafe", "tea", "boba", "milk tea", "bakery", "drinks", "beverage", "starbucks", "snack", "snacks"),
    ),
    CategoryVisualRule(
      iconKey = "groceries",
      label = "Groceries",
      colorHex = 0xFF10B981,
      exactNames = listOf("groceries"),
      keywords = listOf("grocery", "groceries", "supermarket", "palengke", "market", "produce", "pantry"),
    ),
    CategoryVisualRule(
      iconKey = "pets",
      label = "Pets",
      colorHex = 0xFFD97706,
      exactNames = listOf("pets"),
      keywords = listOf("pet", "pets", "dog", "dogs", "cat", "cats", "vet", "veterinary", "pet food", "kibble"),
    ),
    CategoryVisualRule(
      iconKey = "fitness",
      label = "Fitness & Sports",
      colorHex = 0xFFEA580C,
      exactNames = listOf("fitness and sports", "fitness & sports"),
      keywords = listOf("gym", "fitness", "workout", "sport", "sports", "yoga", "pilates", "running", "cycling"),
    ),
    CategoryVisualRule(
      iconKey = "flight",
      label = "Travel",
      colorHex = 0xFF0284C7,
      exactNames = listOf("travel"),
      keywords = listOf("travel", "flight", "flights", "trip", "vacation", "hotel", "airline", "resort", "staycation", "tour"),
    ),
    CategoryVisualRule(
      iconKey = "car",
      label = "Car & Fuel",
      colorHex = 0xFF0369A1,
      exactNames = listOf("car and fuel", "car & fuel"),
      keywords = listOf("car", "auto", "vehicle", "gas", "gasoline", "fuel", "diesel", "parking", "toll", "rfid", "motorcycle", "moto"),
    ),
    CategoryVisualRule(
      iconKey = "subscriptions",
      label = "Subscriptions",
      colorHex = 0xFF7C3AED,
      exactNames = listOf("subscriptions"),
      keywords = listOf("subscription", "subscriptions", "streaming", "netflix", "spotify", "disney", "youtube", "membership", "icloud"),
    ),
    CategoryVisualRule(
      iconKey = "devices",
      label = "Electronics",
      colorHex = 0xFF0284C7,
      exactNames = listOf("electronics"),
      keywords = listOf("electronics", "electronic", "gadget", "gadgets", "computer", "tablet", "pc", "hardware", "charger", "headphones"),
    ),
    CategoryVisualRule(
      iconKey = "gaming",
      label = "Gaming",
      colorHex = 0xFF6366F1,
      keywords = listOf("game", "games", "gaming", "esports", "steam", "playstation", "nintendo", "xbox", "arcade"),
    ),
    CategoryVisualRule(
      iconKey = "wifi",
      label = "Internet & Load",
      colorHex = 0xFF3B82F6,
      exactNames = listOf("internet and load", "internet & load"),
      keywords = listOf("internet", "wifi", "broadband", "fiber", "mobile", "telecom", "load", "data", "phone", "prepaid"),
    ),
    CategoryVisualRule(
      iconKey = "smartphone",
      label = "Mobile & Phone",
      colorHex = 0xFF0EA5A4,
      keywords = listOf("smartphone", "cellphone", "sim", "airtime"),
    ),
    CategoryVisualRule(
      iconKey = "bolt",
      label = "Power & Water",
      colorHex = 0xFFEAB308,
      keywords = listOf("electric", "electricity", "meralco", "power", "solar", "water", "maynilad"),
    ),
    CategoryVisualRule(
      iconKey = "clothing",
      label = "Clothing",
      colorHex = 0xFFC026D3,
      keywords = listOf("clothes", "clothing", "apparel", "fashion", "shoes", "laundry", "outfit", "wardrobe"),
    ),
    CategoryVisualRule(
      iconKey = "family",
      label = "Family & Kids",
      colorHex = 0xFFE11D48,
      exactNames = listOf("family and kids", "family & kids"),
      keywords = listOf("baby", "child", "children", "kids", "kid", "family", "daycare", "diaper", "parenting"),
    ),
    CategoryVisualRule(
      iconKey = "charity",
      label = "Gifts & Charity",
      colorHex = 0xFFF43F5E,
      exactNames = listOf("gifts and charity", "gifts & charity"),
      keywords = listOf("charity", "donation", "donations", "tithe", "volunteer", "offering", "help"),
    ),
    CategoryVisualRule(
      iconKey = "gift",
      label = "Gifts & Refunds",
      colorHex = 0xFFDB2777,
      exactNames = listOf("gifts and refunds", "gifts & refunds"),
      keywords = listOf("gift", "gifts", "present", "birthday", "wedding", "holiday", "christmas", "refund", "refunds", "reimbursement"),
    ),
    CategoryVisualRule(
      iconKey = "bonus",
      label = "Bonus & Rewards",
      colorHex = 0xFFEAB308,
      exactNames = listOf("bonus"),
      keywords = listOf("bonus", "bonuses", "prize", "reward", "rewards", "incentive", "thirteenth", "commission"),
    ),
    CategoryVisualRule(
      iconKey = "trending_up",
      label = "Investments",
      colorHex = 0xFF10B981,
      exactNames = listOf("investments"),
      keywords = listOf("invest", "investment", "investments", "stock", "stocks", "dividend", "dividends", "crypto", "trading", "interest", "profit"),
    ),
    CategoryVisualRule(
      iconKey = "savings",
      label = "Savings",
      colorHex = 0xFF059669,
      keywords = listOf("savings", "saving", "emergency", "fund", "deposit", "pension", "retirement"),
    ),
    CategoryVisualRule(
      iconKey = "repair",
      label = "Repairs",
      colorHex = 0xFF64748B,
      exactNames = listOf("repairs"),
      keywords = listOf("repair", "repairs", "maintenance", "tools", "mechanic", "plumber", "fix"),
    ),
    CategoryVisualRule(
      iconKey = "bank",
      label = "Banking & Loans",
      colorHex = 0xFF6366F1,
      keywords = listOf("bank", "loan", "loans", "debt", "credit", "tax", "taxes", "government", "sss", "pagibig", "philhealth"),
    ),
    // Built-in Income & Expense categories
    CategoryVisualRule(
      iconKey = "wallet",
      label = "Allowance",
      colorHex = 0xFF0EA5A4,
      exactNames = listOf("allowance"),
      keywords = listOf("allowance", "baon", "stipend", "pocket money", "cashback", "rebate"),
    ),
    CategoryVisualRule(
      iconKey = "work",
      label = "Salary",
      colorHex = 0xFF10B981,
      exactNames = listOf("salary"),
      keywords = listOf("salary", "wage", "wages", "payroll", "paycheck", "job", "overtime"),
    ),
    CategoryVisualRule(
      iconKey = "laptop",
      label = "Freelance",
      colorHex = 0xFF0EA5A4,
      exactNames = listOf("freelance"),
      keywords = listOf("freelance", "consulting", "gig", "side hustle", "remote", "software", "tech"),
    ),
    CategoryVisualRule(
      iconKey = "store",
      label = "Business",
      colorHex = 0xFF3B82F6,
      exactNames = listOf("business"),
      keywords = listOf("business", "sales", "shop", "store", "ecommerce", "retail", "merch"),
    ),
    CategoryVisualRule(
      iconKey = "restaurant",
      label = "Food & Dining",
      colorHex = 0xFF0EA5A4,
      exactNames = listOf("food and dining", "food & dining"),
      keywords = listOf("food", "dining", "lunch", "dinner", "breakfast", "meal", "meals", "restaurant", "takeout", "delivery", "fast food"),
    ),
    CategoryVisualRule(
      iconKey = "directions_bus",
      label = "Transportation",
      colorHex = 0xFF3B82F6,
      exactNames = listOf("transportation"),
      keywords = listOf("transport", "transportation", "commute", "bus", "train", "jeep", "jeepney", "mrt", "lrt", "fare", "transit", "taxi", "grab", "uber"),
    ),
    CategoryVisualRule(
      iconKey = "school",
      label = "Education",
      colorHex = 0xFF7C3AED,
      exactNames = listOf("education"),
      keywords = listOf("education", "school", "tuition", "college", "university", "study", "class", "course", "training", "supplies"),
    ),
    CategoryVisualRule(
      iconKey = "receipt",
      label = "Bills & Utilities",
      colorHex = 0xFFEAB308,
      exactNames = listOf("bills and utilities", "bills & utilities"),
      keywords = listOf("bill", "bills", "utility", "utilities", "invoice", "fee", "fees", "dues", "installment", "postpaid"),
    ),
    CategoryVisualRule(
      iconKey = "shopping_bag",
      label = "Shopping",
      colorHex = 0xFFDB2777,
      exactNames = listOf("shopping"),
      keywords = listOf("shopping", "mall", "lazada", "shopee", "amazon", "haul", "accessories"),
    ),
    CategoryVisualRule(
      iconKey = "movie",
      label = "Entertainment",
      colorHex = 0xFF6366F1,
      exactNames = listOf("entertainment"),
      keywords = listOf("entertainment", "movie", "movies", "cinema", "concert", "theater", "hobby", "hobbies", "leisure", "party"),
    ),
    CategoryVisualRule(
      iconKey = "medical",
      label = "Healthcare",
      colorHex = 0xFFF43F5E,
      exactNames = listOf("healthcare"),
      keywords = listOf("health", "healthcare", "medical", "medicine", "pharmacy", "doctor", "clinic", "hospital", "dental", "dentist", "therapy", "vitamins", "checkup", "insurance"),
    ),
    CategoryVisualRule(
      iconKey = "home",
      label = "Housing & Rent",
      colorHex = 0xFF3B82F6,
      exactNames = listOf("housing", "housing and rent", "housing & rent"),
      keywords = listOf("home", "house", "housing", "rent", "mortgage", "condo", "apartment", "furniture", "decor", "cleaning"),
    ),
    CategoryVisualRule(
      iconKey = "spa",
      label = "Personal Care",
      colorHex = 0xFF10B981,
      exactNames = listOf("personal care"),
      keywords = listOf("personal care", "personal", "care", "salon", "hair", "haircut", "barber", "skincare", "makeup", "cosmetics", "massage", "wellness", "grooming"),
    ),
    CategoryVisualRule(
      iconKey = "music",
      label = "Music & Audio",
      colorHex = 0xFF7C3AED,
      keywords = listOf("music", "audio", "podcast", "instrument", "karaoke"),
    ),
    CategoryVisualRule(
      iconKey = "book",
      label = "Books & Reading",
      colorHex = 0xFF0284C7,
      keywords = listOf("book", "books", "reading", "kindle", "magazine", "stationery"),
    ),
    CategoryVisualRule(
      iconKey = "drinks",
      label = "Social & Drinks",
      colorHex = 0xFFDB2777,
      keywords = listOf("bar", "wine", "nightlife", "hangout", "social"),
    ),
    CategoryVisualRule(
      iconKey = "payments",
      label = "Income / Cash",
      colorHex = 0xFF64748B,
      exactNames = listOf("other income"),
      keywords = listOf("other income", "income", "cash", "earning", "earnings"),
    ),
    CategoryVisualRule(
      iconKey = "label",
      label = "Custom Label",
      colorHex = 0xFF0EA5A4,
    ),
    CategoryVisualRule(
      iconKey = "category",
      label = "Other",
      colorHex = 0xFF64748B,
      exactNames = listOf("other expenses"),
      keywords = listOf("other expenses", "other", "misc", "miscellaneous"),
    ),
  )

  private val ruleByKey: Map<String, CategoryVisualRule> = rules.associateBy { it.iconKey }

  fun ruleForIconKey(iconKey: String): CategoryVisualRule? = ruleByKey[iconKey]

  fun inferSpecificMatch(name: String): CategoryVisualRule? {
    val normalized = normalize(name)
    if (normalized.isEmpty()) return null

    // 1. Exact built-in name match
    rules.firstOrNull { rule -> rule.exactNames.any { it == normalized } }?.let { return it }

    val tokens = normalized.split(' ').filter { it.isNotEmpty() }

    // 2. Multi-word phrase match (e.g. "personal care", "pet food", "milk tea")
    rules.firstOrNull { rule ->
      rule.keywords.any { kw -> ' ' in kw && (normalized == kw || " $normalized ".contains(" $kw ")) }
    }?.let { return it }

    // 3. Exact single-word token match (safe from substring traps like "care" matching "car")
    for (token in tokens) {
      rules.firstOrNull { rule -> rule.keywords.any { it == token } }?.let { return it }
    }

    // 4. Compound/prefix token match for longer stems (>= 4 chars)
    for (token in tokens) {
      if (token.length >= 4) {
        rules.firstOrNull { rule ->
          rule.keywords.any { kw ->
            ' ' !in kw && kw.length >= 4 && (token.startsWith(kw) || kw.startsWith(token))
          }
        }?.let { return it }
      }
    }

    // 5. Typo-resilient Damerau-Levenshtein match (e.g. "Healthcre" -> "healthcare")
    var bestRule: CategoryVisualRule? = null
    var bestDistance = Int.MAX_VALUE
    for (token in tokens) {
      if (token.length < 4) continue
      val maxAllowed = if (token.length >= 7) 2 else 1
      for (rule in rules) {
        for (kw in rule.keywords) {
          if (' ' in kw || kw.length < 4) continue
          if (abs(token.length - kw.length) > maxAllowed) continue
          val dist = damerauLevenshtein(token, kw)
          if (dist <= maxAllowed && dist < bestDistance) {
            bestDistance = dist
            bestRule = rule
          }
        }
      }
    }
    return bestRule
  }

  fun inferIconKey(name: String, type: TransactionType? = null): String {
    inferSpecificMatch(name)?.let { return it.iconKey }
    return when (type) {
      TransactionType.INCOME -> "payments"
      TransactionType.EXPENSE -> "label"
      null -> "category"
    }
  }

  fun inferColorHex(name: String, type: TransactionType? = null): Long {
    inferSpecificMatch(name)?.let { return it.colorHex }
    val normalized = normalize(name)
    if (normalized.isEmpty()) {
      return when (type) {
        TransactionType.INCOME -> 0xFF0F766E
        else -> 0xFF64748B
      }
    }
    val idx = (normalized.hashCode() and Int.MAX_VALUE) % customPalette.size
    return customPalette[idx]
  }

  fun inferIconAndColor(name: String, type: TransactionType? = null): Pair<String, Long> =
    inferIconKey(name, type) to inferColorHex(name, type)

  fun colorForIconOrName(
    iconKey: String,
    name: String,
    type: TransactionType? = null,
    fallbackColor: Long = inferColorHex(name, type),
  ): Long {
    val specific = inferSpecificMatch(name)
    if (specific != null && specific.iconKey == iconKey) {
      return specific.colorHex
    }
    val rule = ruleByKey[iconKey]
    return if (rule != null && iconKey != "category" && iconKey != "payments" && iconKey != "label") {
      rule.colorHex
    } else {
      fallbackColor
    }
  }

  private fun normalize(raw: String): String =
    raw.lowercase()
      .replace("&", " and ")
      .replace(Regex("[^a-z0-9]+"), " ")
      .trim()

  private fun damerauLevenshtein(a: String, b: String): Int {
    val m = a.length
    val n = b.length
    val dp = Array(m + 1) { IntArray(n + 1) }
    for (i in 0..m) dp[i][0] = i
    for (j in 0..n) dp[0][j] = j
    for (i in 1..m) {
      for (j in 1..n) {
        val cost = if (a[i - 1] == b[j - 1]) 0 else 1
        var value = minOf(
          dp[i - 1][j] + 1,
          dp[i][j - 1] + 1,
          dp[i - 1][j - 1] + cost,
        )
        if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
          value = minOf(value, dp[i - 2][j - 2] + 1)
        }
        dp[i][j] = value
      }
    }
    return dp[m][n]
  }
}
