package com.example.domain

/**
 * Synthesizes concise, topic-focused conversation titles (2–5 words) for BudgetWise Assistant.
 *
 * Designed to summarize the conversation's financial subject rather than copying the user's raw
 * message verbatim or falling back to generic labels like "New Chat" or "Untitled Chat".
 */
object ChatTitleGenerator {

  private const val MAX_TITLE_LENGTH = 42
  private const val MIN_WORDS = 2
  private const val MAX_WORDS = 5
  const val DEFAULT_FALLBACK_TITLE = "Finance Check-In"

  private val BANNED_GENERIC_TITLES = setOf(
    "new chat", "untitled", "untitled chat", "untitled conversation", "chat", "conversation",
    "assistant", "budgetwise", "budgetwise assistant", "help", "question", "hi", "hello", "hey",
  )

  private val GENERIC_GREETINGS = setOf(
    "hi", "hello", "hey", "yo", "sup", "good morning", "good afternoon", "good evening",
    "help", "help me", "start", "test", "testing", "question", "budgetwise", "assistant",
    "how are you", "thanks", "thank you",
  )

  private val STOP_WORDS = setOf(
    "a", "an", "and", "are", "about", "am", "at", "be", "can", "could", "did", "do", "does",
    "for", "from", "get", "give", "have", "help", "how", "i", "im", "in", "into", "is", "it",
    "just", "know", "last", "like", "make", "me", "much", "my", "need", "of", "on", "or", "our",
    "out", "please", "really", "rein", "see", "should", "show", "some", "tell", "that", "the",
    "their", "them", "there", "these", "they", "this", "to", "too", "understand", "vs", "want",
    "was", "we", "were", "what", "when", "where", "which", "who", "why", "will", "with", "would",
    "you", "your",
  )

  private val SMALL_WORDS = setOf("a", "an", "and", "at", "by", "for", "in", "of", "on", "or", "the", "to", "with")

  private val MONTH_MAP = listOf(
    Regex("""\bjan(?:uary)?\b""", RegexOption.IGNORE_CASE) to "January",
    Regex("""\bfeb(?:ruary)?\b""", RegexOption.IGNORE_CASE) to "February",
    Regex("""\bmar(?:ch)?\b""", RegexOption.IGNORE_CASE) to "March",
    Regex("""\bapr(?:il)?\b""", RegexOption.IGNORE_CASE) to "April",
    Regex("""\bmay\b""", RegexOption.IGNORE_CASE) to "May",
    Regex("""\bjun(?:e)?\b""", RegexOption.IGNORE_CASE) to "June",
    Regex("""\bjul(?:y)?\b""", RegexOption.IGNORE_CASE) to "July",
    Regex("""\baug(?:ust)?\b""", RegexOption.IGNORE_CASE) to "August",
    Regex("""\bsep(?:t|tember)?\b""", RegexOption.IGNORE_CASE) to "September",
    Regex("""\boct(?:ober)?\b""", RegexOption.IGNORE_CASE) to "October",
    Regex("""\bnov(?:ember)?\b""", RegexOption.IGNORE_CASE) to "November",
    Regex("""\bdec(?:ember)?\b""", RegexOption.IGNORE_CASE) to "December",
  )

  private val KNOWN_SUBJECTS = listOf(
    Regex("""\bfood\s*(?:and|&)\s*dining\b|\bdining\s*out\b""", RegexOption.IGNORE_CASE) to "Food & Dining",
    Regex("""\bfood\b|\bmeals?\b|\blunch\b|\bdinner\b|\brestaurants?\b""", RegexOption.IGNORE_CASE) to "Food",
    Regex("""\bgrocer(?:y|ies)\b""", RegexOption.IGNORE_CASE) to "Groceries",
    Regex("""\bcoffee\b|\bcafe\b""", RegexOption.IGNORE_CASE) to "Coffee",
    Regex("""\btransportation\b|\btransit\b|\bcommut(?:e|ing)\b|\bgrab\b|\bfuel\b|\bgas\b""", RegexOption.IGNORE_CASE) to "Transportation",
    Regex("""\bbills?\s*(?:and|&)\s*utilities\b""", RegexOption.IGNORE_CASE) to "Bills & Utilities",
    Regex("""\belectric(?:ity)?\b|\bwater\s+bill\b|\binternet\s+bill\b|\butilit(?:y|ies)\b""", RegexOption.IGNORE_CASE) to "Utility Bills",
    Regex("""\brent\b|\bhousing\b|\bmortgage\b""", RegexOption.IGNORE_CASE) to "Housing",
    Regex("""\bshopping\b|\bclothes\b|\bapparel\b""", RegexOption.IGNORE_CASE) to "Shopping",
    Regex("""\bentertainment\b|\bmovies?\b|\bgames?\b|\bsubscriptions?\b""", RegexOption.IGNORE_CASE) to "Entertainment",
    Regex("""\bhealthcare\b|\bmedical\b|\bhealth\b|\bpharmacy\b""", RegexOption.IGNORE_CASE) to "Healthcare",
    Regex("""\beducation\b|\btuition\b|\bcourses?\b|\bbooks?\b""", RegexOption.IGNORE_CASE) to "Education",
    Regex("""\ballowance\b""", RegexOption.IGNORE_CASE) to "Allowance",
    Regex("""\bsalary\b|\bpayroll\b|\bincome\b|\bearnings?\b""", RegexOption.IGNORE_CASE) to "Income",
    Regex("""\bemergency\s+fund\b""", RegexOption.IGNORE_CASE) to "Emergency Fund",
    Regex("""\bdebt\b|\bloans?\b|\bcredit\s+card\b""", RegexOption.IGNORE_CASE) to "Debt Management",
    Regex("""\btravel\b|\bvacation\b|\btrip\b""", RegexOption.IGNORE_CASE) to "Travel",
  )

  private val PUNCTUATION_STRIP_REGEX = Regex("""^[\s"'`*#\-–—:;,.!?()\[\]]+|[\s"'`*#\-–—:;,.!?()\[\]]+$""")
  private val NON_ALPHANUM_REGEX = Regex("""[^a-z0-9\s]""")
  private val WHITESPACE_REGEX = Regex("""\s+""")

  /**
   * Returns true if a prompt is too short or generic (e.g., "Hi", "Help") to determine a topic,
   * allowing a subsequent message to refine the auto-generated title.
   */
  fun isGenericPrompt(prompt: String): Boolean {
    val normalized = normalizeForComparison(prompt)
    if (normalized.isEmpty() || normalized.length < 4) return true
    if (normalized in GENERIC_GREETINGS) return true
    val meaningfulTokens = normalized.split(" ").filter { it.length >= 3 && it !in STOP_WORDS }
    return meaningfulTokens.isEmpty()
  }

  /**
   * Generates a concise 2–5 word topic title from the conversation messages without echoing the
   * user's raw prompt verbatim or returning generic placeholders like "New Chat".
   */
  fun generateTitle(
    userMessages: List<String>,
    assistantMessages: List<String> = emptyList(),
  ): String {
    val meaningfulUserPrompt = userMessages.firstOrNull { !isGenericPrompt(it) }
    val sourceText = meaningfulUserPrompt
      ?: assistantMessages.firstOrNull { it.isNotBlank() }
      ?: return DEFAULT_FALLBACK_TITLE

    val clean = sourceText.trim().replace(WHITESPACE_REGEX, " ")
    val lower = clean.lowercase()

    val inferred = inferSemanticTitle(lower, clean)
    if (inferred != null) {
      val validated = ensureNonVerbatimAndBounded(inferred, userMessages)
      if (validated != null) return validated
    }

    // Build a concise noun-focused topic phrase + intent suffix
    val keywords = extractKeywords(clean)
    val synthesized = if (keywords.isEmpty()) {
      DEFAULT_FALLBACK_TITLE
    } else {
      val intentSuffix = inferIntentSuffix(lower, keywords)
      val baseTokens = keywords.take(if (intentSuffix != null) 3 else 4)
      val combined = buildList {
        addAll(baseTokens.mapIndexed { idx, w -> formatTitleWord(w, idx == 0) })
        if (intentSuffix != null && baseTokens.none { it.equals(intentSuffix, ignoreCase = true) }) {
          add(intentSuffix)
        }
      }
      if (combined.size < MIN_WORDS) {
        (combined + "Overview").joinToString(" ")
      } else {
        combined.take(MAX_WORDS).joinToString(" ")
      }
    }

    return ensureNonVerbatimAndBounded(synthesized, userMessages) ?: DEFAULT_FALLBACK_TITLE
  }

  /**
   * Cleans and validates an AI-suggested conversation title, returning `null` if it is blank,
   * generic (e.g., "New Chat"), out of bounds, or a verbatim copy of a user message.
   */
  fun sanitizeCandidateTitle(
    rawCandidate: String?,
    userMessages: List<String>,
  ): String? {
    if (rawCandidate.isNullOrBlank()) return null
    val firstLine = rawCandidate
      .lineSequence()
      .map { it.trim() }
      .firstOrNull { it.isNotEmpty() }
      ?: return null

    val cleaned = firstLine
      .replace(Regex("""^(?:title|topic|subject)\s*:\s*""", RegexOption.IGNORE_CASE), "")
      .replace("**", "")
      .replace("*", "")
      .replace(PUNCTUATION_STRIP_REGEX, "")
      .replace(WHITESPACE_REGEX, " ")
      .trim()

    if (cleaned.isEmpty()) return null
    val normalized = normalizeForComparison(cleaned)
    if (normalized in BANNED_GENERIC_TITLES || normalized in GENERIC_GREETINGS) return null

    val words = cleaned.split(" ").filter { it.isNotBlank() }
    if (words.size < MIN_WORDS) return null
    val bounded = words.take(MAX_WORDS)
      .mapIndexed { idx, w -> formatTitleWord(w, idx == 0) }
      .joinToString(" ")

    return ensureNonVerbatimAndBounded(bounded, userMessages)
  }

  private fun inferSemanticTitle(lower: String, original: String): String? {
    val month = detectMonth(original)
    val subject = detectKnownSubject(original)

    return when {
      lower.contains("50/30/20") -> "50/30/20 Budget Plan"
      lower.contains("unsustainable") || lower.contains("overspend") -> {
        when {
          subject != null -> "$subject Overspending Review"
          month != null -> "$month Overspending Check"
          else -> "Overspending Check"
        }
      }
      lower.contains("savings rate") -> "Savings Rate Guidance"
      lower.contains("net cash flow") || lower.contains("cash flow") -> {
        if (month != null) "$month Cash Flow" else "Cash Flow Summary"
      }
      lower.contains("compare") && (lower.contains("week") || lower.contains("month")) -> {
        if (lower.contains("week")) "Weekly Spending Comparison" else "Monthly Spending Comparison"
      }
      lower.contains("save more") && lower.contains("week") -> "Weekly Savings Opportunities"
      lower.contains("where") && lower.contains("money") && lower.contains("go") -> {
        if (month != null) "$month Spending Breakdown" else "Top Spending Breakdown"
      }
      lower.contains("top") && (lower.contains("expense") || lower.contains("spending") || lower.contains("categor")) -> {
        if (month != null) "$month Top Expenses" else "Top Spending Categories"
      }
      lower.contains("emergency fund") -> "Emergency Fund Progress"
      lower.contains("savings goal") || lower.contains("my goals") -> "Savings Goals Review"
      lower.contains("budget plan") -> {
        if (month != null) "$month Budget Plan" else "Monthly Budget Plan"
      }
      lower.contains("first monthly budget") || lower.contains("which categories should i budget") ||
        (lower.contains("set up") && lower.contains("categor")) -> "Budget Category Setup"
      lower.contains("on track") && lower.contains("budget") -> "Monthly Budget Status"
      lower.contains("rein in") && lower.contains("budget") -> {
        val cat = subject ?: extractBudgetCategory(original)
        if (!cat.isNullOrBlank()) "$cat Budget Strategy" else "Budget Reduction Strategy"
      }
      subject != null && lower.contains("budget") -> "$subject Budget Review"
      subject != null && (lower.contains("break down") || lower.contains("breakdown")) -> "$subject Spending Breakdown"
      subject != null && (lower.contains("spend") || lower.contains("spent") || lower.contains("expense")) -> {
        if (month != null) "$month $subject Spending" else "$subject Spending Analysis"
      }
      month != null && (lower.contains("spend") || lower.contains("spent") || lower.contains("expense")) -> {
        "$month Spending Summary"
      }
      month != null && lower.contains("income") -> "$month Income Summary"
      month != null && lower.contains("budget") -> "$month Budget Overview"
      lower.contains("this week") && (lower.contains("spend") || lower.contains("spent")) -> "Weekly Spending Summary"
      lower.contains("this month") && (lower.contains("spend") || lower.contains("spent")) -> "Monthly Spending Summary"
      subject != null -> "$subject Overview"
      else -> null
    }
  }

  private fun detectMonth(text: String): String? =
    MONTH_MAP.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.second

  private fun detectKnownSubject(text: String): String? =
    KNOWN_SUBJECTS.firstOrNull { (regex, _) -> regex.containsMatchIn(text) }?.second

  private fun extractBudgetCategory(original: String): String? {
    val match = Regex("""my\s+([A-Za-z& ]{2,24}?)\s+budget""", RegexOption.IGNORE_CASE).find(original)
    val raw = match?.groupValues?.getOrNull(1)?.trim() ?: return null
    return raw.split(WHITESPACE_REGEX)
      .filter { it.isNotBlank() }
      .take(2)
      .mapIndexed { idx, w -> formatTitleWord(w, idx == 0) }
      .joinToString(" ")
  }

  private fun extractKeywords(text: String): List<String> {
    val cleaned = NON_ALPHANUM_REGEX.replace(text.lowercase(), " ")
    return cleaned.split(WHITESPACE_REGEX)
      .filter { token -> token.length >= 3 && token !in STOP_WORDS }
      .distinct()
  }

  private fun inferIntentSuffix(lower: String, keywords: List<String>): String? {
    val hasSummaryWord = keywords.any {
      it in setOf("summary", "review", "plan", "analysis", "breakdown", "overview", "strategy", "guide", "tips", "report")
    }
    if (hasSummaryWord) return null
    return when {
      lower.contains("budget") || lower.contains("limit") -> "Review"
      lower.contains("save") || lower.contains("saving") || lower.contains("goal") -> "Strategy"
      lower.contains("spend") || lower.contains("spent") || lower.contains("expense") || lower.contains("cost") -> "Summary"
      lower.contains("plan") || lower.contains("organiz") -> "Plan"
      else -> "Overview"
    }
  }

  private fun ensureNonVerbatimAndBounded(
    candidate: String,
    userMessages: List<String>,
  ): String? {
    val trimmed = truncateTitle(candidate)
    val normCandidate = normalizeForComparison(trimmed)
    if (normCandidate.isEmpty() || normCandidate in BANNED_GENERIC_TITLES) return null

    val isVerbatimCopy = userMessages.any { msg ->
      val normMsg = normalizeForComparison(msg)
      normMsg.isNotEmpty() && normCandidate == normMsg
    }
    val finalTitle = if (isVerbatimCopy) {
      val words = trimmed.split(" ").filter { it.isNotBlank() }.take(MAX_WORDS - 1)
      truncateTitle((words + "Summary").joinToString(" "))
    } else {
      trimmed
    }

    val finalWords = finalTitle.split(" ").filter { it.isNotBlank() }
    return if (finalWords.size >= MIN_WORDS) finalTitle else null
  }

  private fun normalizeForComparison(text: String): String =
    NON_ALPHANUM_REGEX.replace(text.lowercase(), " ")
      .replace(WHITESPACE_REGEX, " ")
      .trim()

  private fun formatTitleWord(word: String, isFirst: Boolean): String {
    if (word == "&") return "&"
    val lower = word.lowercase()
    if (!isFirst && lower in SMALL_WORDS) return lower
    if (word.any { it.isDigit() } || (word.length in 2..4 && word.all { it.isUpperCase() && it.isLetter() })) {
      return word
    }
    return lower.replaceFirstChar { it.uppercaseChar() }
  }

  private fun truncateTitle(title: String): String {
    val clean = title.trim().replace(WHITESPACE_REGEX, " ")
    val words = clean.split(" ").filter { it.isNotBlank() }.take(MAX_WORDS).joinToString(" ")
    if (words.length <= MAX_TITLE_LENGTH) return words
    val cut = words.take(MAX_TITLE_LENGTH - 1)
    val lastSpace = cut.lastIndexOf(' ')
    val trimmed = if (lastSpace > 12) cut.substring(0, lastSpace) else cut
    return "${trimmed.trimEnd()}…"
  }
}
