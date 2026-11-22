package com.example.domain

enum class HelpTopic(val label: String) {
  ALL("All"),
  BASICS("Basics"),
  BUDGETS_GOALS("Budgets & Goals"),
  SECURITY("Security"),
  DATA_EXPORT("Data & Export"),
}

data class HelpFaqItem(
  val id: String,
  val topic: HelpTopic,
  val question: String,
  val answer: String,
  val keywords: List<String> = emptyList(),
)

/**
 * Curated, jargon-free self-service Help & FAQ catalog tailored to BudgetWise's real features.
 */
object HelpCenterCatalog {

  val items: List<HelpFaqItem> = listOf(
    HelpFaqItem(
      id = "net_cash_flow",
      topic = HelpTopic.BASICS,
      question = "How is Net Cash Flow calculated?",
      answer = "Net Cash Flow is your recorded Income minus your recorded Expenses for the selected period. On Home, you can switch between a single Month view and an All-Time view. Because BudgetWise never links to your bank accounts, this figure reflects the transactions you log—not a live bank balance.",
      keywords = listOf("balance", "bank", "income", "expense", "monthly", "all time", "formula"),
    ),
    HelpFaqItem(
      id = "savings_rate_deficit",
      topic = HelpTopic.BASICS,
      question = "What do 'Savings Rate', 'Savings Benchmark', and 'Deficit' mean?",
      answer = "Savings Rate shows the percentage of your recorded income left over after expenses. In Insights, BudgetWise compares this against a 20% Savings Benchmark to help gauge your cushion. If your expenses exceed your recorded income for a period, BudgetWise shows a Deficit instead of a surplus.",
      keywords = listOf("surplus", "negative", "red", "percent", "20%", "insights", "benchmark"),
    ),
    HelpFaqItem(
      id = "inline_calculator",
      topic = HelpTopic.BASICS,
      question = "How do I use the built-in calculator for amounts?",
      answer = "Tap the calculator icon inside any Amount field—when adding or editing a transaction, setting a monthly budget, or contributing to a savings goal. You can split a bill (1850 ÷ 4), total daily items (15 + 65 + 120), or apply a discount (1500 × 0.8), then tap Apply to fill the result without leaving BudgetWise.",
      keywords = listOf("math", "split", "bill", "discount", "keypad", "divide", "multiply", "add"),
    ),
    HelpFaqItem(
      id = "payment_methods_activity",
      topic = HelpTopic.BASICS,
      question = "How do I track Cash, GCash, Maya, and card spending?",
      answer = "When adding or editing a transaction, pick the payment method you used—Cash, GCash, Maya, Debit Card, Credit Card, or Bank Transfer. In the Activity tab, you can search by title or note, filter by type, category, or month, and swipe left on any entry to delete it.",
      keywords = listOf("ewallet", "e-wallet", "gcash", "maya", "debit", "credit", "bank transfer", "filter", "swipe"),
    ),
    HelpFaqItem(
      id = "goals_vs_cash_flow",
      topic = HelpTopic.BUDGETS_GOALS,
      question = "Do Savings Goals deduct from my monthly cash flow?",
      answer = "No. Savings Goals are tracked independently as Reserved in Goals so your monthly income, expense totals, and category budgets stay accurate without double-counting. You can record both deposits and withdrawals toward any goal and review its full history anytime.",
      keywords = listOf("reserved", "contribution", "deposit", "withdraw", "target", "double count"),
    ),
    HelpFaqItem(
      id = "budget_alerts_reminders",
      topic = HelpTopic.BUDGETS_GOALS,
      question = "How do Budget Threshold Alerts and Daily Reminders work?",
      answer = "BudgetWise watches your active monthly budgets and notifies you when spending reaches your chosen warning threshold (70% to 90%) or exceeds 100%, as well as when you hit savings milestones. You can customize alert toggles, warning thresholds, and evening Daily Logging Reminders under Settings → Alerts & Notifications, and view all alerts from the bell icon on Home.",
      keywords = listOf("notification", "threshold", "exceeded", "bell", "inbox", "milestone", "evening"),
    ),
    HelpFaqItem(
      id = "customize_categories",
      topic = HelpTopic.BUDGETS_GOALS,
      question = "How can I customize categories, icons, or colors?",
      answer = "Go to Settings → Financial Categories. Tap any of the built-in Income or Expense categories to pick a new icon or accent color (or reset it to its default look). Tap + Add in the top-right corner to create your own custom category, which immediately appears in transactions and budgets.",
      keywords = listOf("icon", "color", "custom", "add category", "edit category", "delete category"),
    ),
    HelpFaqItem(
      id = "forgot_pin",
      topic = HelpTopic.SECURITY,
      question = "What happens if I forget my 4-Digit PIN?",
      answer = "Tap Forgot PIN? on the lock screen to verify your identity with your phone's screen lock or biometrics (fingerprint or face unlock). Once verified, you can create a new 4-digit PIN or turn off App Lock without losing any financial records. If your device has no screen lock set up, a confirmed local reset option is available as a last resort.",
      keywords = listOf("lock", "biometric", "fingerprint", "face", "unlock", "recovery", "reset pin"),
    ),
    HelpFaqItem(
      id = "hide_balances_eye",
      topic = HelpTopic.SECURITY,
      question = "What does the Eye icon (Hide Balances) do?",
      answer = "Tapping the Eye icon on Home—or turning on Hide Balances in Settings—masks monetary amounts across cards, budgets, goals, and charts. It is designed for quick privacy whenever you check your finances in public.",
      keywords = listOf("privacy", "mask", "blur", "public", "stealth", "hidden", "asterisk"),
    ),
    HelpFaqItem(
      id = "data_storage_privacy",
      topic = HelpTopic.DATA_EXPORT,
      question = "Where is my financial data stored? Is it private?",
      answer = "Your transactions, budgets, savings goals, categories, display name, and currency are synced privately to your Google account so you can access BudgetWise across multiple phones, with automatic offline caching when you have no signal. Security rules restrict access strictly to your signed-in account. BudgetWise never requests bank credentials, and your 4-Digit PIN lock, theme, and notification schedules remain strictly local to each device.",
      keywords = listOf("offline", "local", "cloud", "sync", "upload", "server", "bank credentials", "assistant", "ai", "devices"),
    ),
    HelpFaqItem(
      id = "cloud_sync_devices",
      topic = HelpTopic.DATA_EXPORT,
      question = "How do I access BudgetWise on another phone?",
      answer = "Simply install BudgetWise on your second device and sign in with the same Google account. Your transactions, monthly budgets, savings goals, custom categories, preferred currency, and display name sync automatically—skipping the initial setup guide. For security, you can set a separate 4-Digit PIN lock on each phone.",
      keywords = listOf("multi-device", "second phone", "google", "account", "sign in", "backup", "restore", "sync"),
    ),
    HelpFaqItem(
      id = "export_backup",
      topic = HelpTopic.DATA_EXPORT,
      question = "How do I back up or export my financial reports?",
      answer = "Open Settings → Data Management and tap Export Report (CSV / PDF)—or tap the download icon in the Activity tab. Choose CSV for spreadsheets (Excel or Google Sheets) or PDF for a clean printable statement, select a Monthly, Custom Date Range, or All-Time period, then save to your device or share directly.",
      keywords = listOf("csv", "pdf", "excel", "sheets", "statement", "share", "download", "backup"),
    ),
    HelpFaqItem(
      id = "clear_all_data",
      topic = HelpTopic.DATA_EXPORT,
      question = "How do I start fresh or clear all financial records?",
      answer = "Open Settings → Data Management and tap Clear All Financial Records. This permanently erases your transactions, monthly budgets, savings goals, and custom categories while keeping your 30 default categories ready for a clean ₱0.00 start.",
      keywords = listOf("reset", "erase", "delete all", "fresh start", "wipe", "clear"),
    ),
  )

  /**
   * Filters [items] by [topic] and a space-separated multi-term [query] matched against
   * each item's question, answer, topic label, and search keywords.
   */
  fun filter(
    topic: HelpTopic,
    query: String,
    source: List<HelpFaqItem> = items,
  ): List<HelpFaqItem> {
    val tokens = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return source.filter { item ->
      val topicMatches = topic == HelpTopic.ALL || item.topic == topic
      if (!topicMatches) return@filter false
      if (tokens.isEmpty()) return@filter true

      val searchable = buildString {
        append(item.question.lowercase())
        append(' ')
        append(item.answer.lowercase())
        append(' ')
        append(item.topic.label.lowercase())
        if (item.keywords.isNotEmpty()) {
          append(' ')
          append(item.keywords.joinToString(" ").lowercase())
        }
      }
      tokens.all { token -> searchable.contains(token) }
    }
  }
}
