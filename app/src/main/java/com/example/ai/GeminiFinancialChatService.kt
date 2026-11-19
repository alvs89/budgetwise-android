package com.example.ai

import android.util.Log
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.ChatRole
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.content
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeout

private const val TAG = "GeminiChatService"
private const val PRIMARY_MODEL_NAME = "gemini-3.5-flash-lite"
private const val FALLBACK_MODEL_NAME = "gemini-3.8-flash"
private const val STREAM_ATTEMPT_TIMEOUT_MILLIS = 15_000L
private const val UNARY_PRIMARY_TIMEOUT_MILLIS = 20_000L
private const val TITLE_GENERATION_TIMEOUT_MILLIS = 10_000L
private const val MAX_HISTORY_MESSAGES = 12

/** Abstraction for streaming conversational financial assistant replies, enabling test doubles. */
interface FinancialChatService {
  fun streamReply(
    priorHistory: List<ChatMessageEntity>,
    userQuestion: String,
    financialContextDigest: String,
  ): Flow<String>

  /**
   * Generates a concise 2–5 word topic title summarizing the conversation, or returns `null`
   * if unavailable so the caller can fall back to local title synthesis.
   */
  suspend fun generateConversationTitle(messages: List<ChatMessageEntity>): String? = null
}

/**
 * Firebase AI Logic implementation of [FinancialChatService].
 *
 * Uses a three-stage resiliency strategy: primary streaming for fast time-to-first-token,
 * unary retry on the primary model if streaming times out before emitting text, and a fallback
 * model call when the primary model is unavailable.
 */
class GeminiFinancialChatService : FinancialChatService {

  private val systemPrompt = content {
    text(
      """
      You are BudgetWise Assistant, a helpful, practical personal finance chatbot inside an Android budgeting app.
      - Answer strictly using the user's [LIVE FINANCIAL LEDGER CONTEXT] provided with each question.
      - Never invent transactions, balances, categories, or budgets that are not present in the context.
      - If data is missing for a specific date range or category, explain clearly what is or isn't recorded.
      - Keep answers concise, scannable, and mobile-friendly (short paragraphs or compact bullet points).
      - Respect Privacy Mode instructions whenever Privacy Mode is enabled.
      - Offer concrete, actionable budgeting or saving tips grounded in the user's real numbers.
      - Always refer to the app as "BudgetWise" (one word, no space).
      """.trimIndent()
    )
  }

  private val aiBackend by lazy {
    Firebase.ai(backend = GenerativeBackend.googleAI())
  }

  private val primaryModel by lazy {
    aiBackend.generativeModel(
      modelName = PRIMARY_MODEL_NAME,
      systemInstruction = systemPrompt,
    )
  }

  private val fallbackModel by lazy {
    aiBackend.generativeModel(
      modelName = FALLBACK_MODEL_NAME,
      systemInstruction = systemPrompt,
    )
  }

  override fun streamReply(
    priorHistory: List<ChatMessageEntity>,
    userQuestion: String,
    financialContextDigest: String,
  ): Flow<String> = flow {
    val sanitizedHistory = mapHistoryForGemini(priorHistory)
    val groundedPrompt = buildString {
      appendLine("[LIVE FINANCIAL LEDGER CONTEXT]")
      appendLine(financialContextDigest.trim())
      appendLine()
      appendLine("[USER QUESTION]")
      append(userQuestion.trim())
    }
    val requestContents = sanitizedHistory + content("user") { text(groundedPrompt) }

    val accumulated = StringBuilder()
    var emittedVisibleText = false
    var lastFailure: Throwable? = null

    // Attempt 1: Fast streaming on gemini-3.5-flash-lite for low Time-To-First-Token.
    try {
      Log.d(TAG, "Attempt 1: streaming with $PRIMARY_MODEL_NAME (turns=${requestContents.size})")
      withTimeout(STREAM_ATTEMPT_TIMEOUT_MILLIS) {
        primaryModel.generateContentStream(requestContents).collect { chunk ->
          val delta = chunk.text.orEmpty()
          if (delta.isNotEmpty()) {
            accumulated.append(delta)
            if (accumulated.isNotBlank()) {
              if (!emittedVisibleText) {
                Log.d(TAG, "First visible token received from $PRIMARY_MODEL_NAME")
              }
              emittedVisibleText = true
              emit(accumulated.toString())
            }
          }
        }
      }
      if (emittedVisibleText) {
        return@flow
      }
      Log.w(TAG, "Attempt 1 ($PRIMARY_MODEL_NAME stream) completed with no visible text.")
    } catch (e: TimeoutCancellationException) {
      currentCoroutineContext().ensureActive()
      if (emittedVisibleText) {
        Log.w(TAG, "Stream timed out after emitting partial text; returning partial reply.", e)
        return@flow
      }
      Log.w(TAG, "Attempt 1 timed out after ${STREAM_ATTEMPT_TIMEOUT_MILLIS}ms; switching to unary request.", e)
      lastFailure = e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (emittedVisibleText) {
        Log.w(TAG, "Stream interrupted after partial text; keeping emitted reply.", e)
        return@flow
      }
      Log.e(TAG, "Attempt 1 ($PRIMARY_MODEL_NAME stream) failed; will retry unary.", e)
      lastFailure = e
    }

    // Attempt 2: Non-streaming unary request on gemini-3.5-flash-lite (avoids SSE proxy/buffering hangs).
    try {
      Log.d(TAG, "Attempt 2: unary request with $PRIMARY_MODEL_NAME")
      val response = withTimeout(UNARY_PRIMARY_TIMEOUT_MILLIS) {
        primaryModel.generateContent(requestContents)
      }
      val replyText = response.text?.trim().orEmpty()
      if (replyText.isNotEmpty()) {
        emit(replyText)
        return@flow
      }
      Log.w(TAG, "Attempt 2 ($PRIMARY_MODEL_NAME unary) returned empty text.")
    } catch (e: TimeoutCancellationException) {
      currentCoroutineContext().ensureActive()
      Log.w(TAG, "Attempt 2 timed out after ${UNARY_PRIMARY_TIMEOUT_MILLIS}ms; switching to $FALLBACK_MODEL_NAME.", e)
      lastFailure = e
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Attempt 2 ($PRIMARY_MODEL_NAME unary) failed; trying $FALLBACK_MODEL_NAME.", e)
      lastFailure = e
    }

    // Attempt 3: Unary fallback on gemini-3.8-flash using remaining outer timeout budget.
    try {
      Log.d(TAG, "Attempt 3: unary fallback with $FALLBACK_MODEL_NAME")
      val fallbackResponse = fallbackModel.generateContent(requestContents)
      val replyText = fallbackResponse.text?.trim().orEmpty()
      if (replyText.isNotEmpty()) {
        emit(replyText)
        return@flow
      }
      throw IllegalStateException("Assistant returned an empty response.")
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.e(TAG, "Attempt 3 ($FALLBACK_MODEL_NAME unary) failed.", e)
      throw (lastFailure?.takeIf { it !is TimeoutCancellationException } ?: e)
    }
  }.flowOn(Dispatchers.IO)

  override suspend fun generateConversationTitle(messages: List<ChatMessageEntity>): String? {
    if (messages.isEmpty()) return null
    val transcript = messages.take(4).joinToString("\n") { msg ->
      val speaker = if (msg.role == ChatRole.USER) "User" else "Assistant"
      "$speaker: ${msg.content.trim().take(280)}"
    }
    val titlePrompt = content("user") {
      text(
        """
        Generate a short, meaningful title for this personal finance conversation.
        Rules:
        - Output ONLY the title text (no quotes, no markdown, no prefix, no trailing punctuation).
        - Use 2 to 5 words in Title Case that capture the main financial topic or goal.
        - Do NOT copy the user's message verbatim.
        - Never output generic titles like "New Chat", "Untitled Chat", or "Finance Chat".

        Conversation:
        $transcript
        """.trimIndent()
      )
    }
    return try {
      kotlinx.coroutines.withContext(Dispatchers.IO) {
        withTimeout(TITLE_GENERATION_TIMEOUT_MILLIS) {
          primaryModel.generateContent(listOf(titlePrompt)).text?.trim()?.takeIf { it.isNotEmpty() }
        }
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      Log.w(TAG, "Title generation skipped; keeping local topic title.", e)
      null
    }
  }

  /**
   * Trims persisted session messages to the recent window and enforces strict `user` / `model`
   * turn alternation ending on a `model` turn so the upcoming user prompt appends cleanly.
   */
  internal fun mapHistoryForGemini(messages: List<ChatMessageEntity>): List<Content> {
    if (messages.isEmpty()) return emptyList()
    val recent = messages.takeLast(MAX_HISTORY_MESSAGES)
    val mapped = mutableListOf<Content>()
    var expectedRole = ChatRole.USER

    for (msg in recent) {
      val cleanText = msg.content.trim()
      if (cleanText.isEmpty()) continue
      if (mapped.isEmpty() && msg.role != ChatRole.USER) {
        continue
      }
      if (msg.role == expectedRole) {
        mapped += content(if (msg.role == ChatRole.USER) "user" else "model") {
          text(cleanText)
        }
        expectedRole = if (expectedRole == ChatRole.USER) ChatRole.MODEL else ChatRole.USER
      }
    }

    // Ensure history ends on a model turn so the new user message alternates cleanly.
    if (mapped.isNotEmpty() && expectedRole == ChatRole.MODEL) {
      mapped.removeAt(mapped.lastIndex)
    }
    return mapped
  }
}
