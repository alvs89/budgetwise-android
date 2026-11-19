package com.example

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FinancialChatService
import com.example.ai.GeminiFinancialChatService
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.ChatRole
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class FinancialChatServiceTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `mapHistoryForGemini skips leading model turns and strips trailing user turn`() {
    val service = GeminiFinancialChatService()
    val history = listOf(
      ChatMessageEntity(id = 1, role = ChatRole.MODEL, content = "Orphan welcome"),
      ChatMessageEntity(id = 2, role = ChatRole.USER, content = "  How much did I spend?  "),
      ChatMessageEntity(id = 3, role = ChatRole.MODEL, content = "You spent 1,200."),
      ChatMessageEntity(id = 4, role = ChatRole.USER, content = "Unanswered retry prompt"),
    )

    val mapped = service.mapHistoryForGemini(history)

    assertEquals(2, mapped.size)
    assertEquals("user", mapped[0].role)
    assertEquals("model", mapped[1].role)
  }

  @Test
  fun `viewModel surfaces real service exception in chatUiState errorMessage`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val failingService = object : FinancialChatService {
      override fun streamReply(
        priorHistory: List<ChatMessageEntity>,
        userQuestion: String,
        financialContextDigest: String,
      ): Flow<String> = flow {
        throw IllegalStateException("Backend service unavailable (503)")
      }
    }
    val viewModel = BudgetWiseViewModel(app, failingService)
    viewModel.clearChatHistory()
    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.sessions.isEmpty() }
    }

    viewModel.sendChatMessage("How much did I spend on food?")

    val errorState = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.errorMessage != null }
    }
    assertFalse(errorState.isStreaming)
    assertEquals("Backend service unavailable (503)", errorState.errorMessage)
    assertEquals("How much did I spend on food?", errorState.failedPrompt)
  }

  @Test
  fun `viewModel cancelActiveChatRequest stops streaming cleanly`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val slowService = object : FinancialChatService {
      override fun streamReply(
        priorHistory: List<ChatMessageEntity>,
        userQuestion: String,
        financialContextDigest: String,
      ): Flow<String> = flow {
        delay(30_000L)
        emit("Late reply")
      }
    }
    val viewModel = BudgetWiseViewModel(app, slowService)
    viewModel.clearChatHistory()
    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.sessions.isEmpty() }
    }

    viewModel.sendChatMessage("Compare this week vs last week")

    val streamingState = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.isStreaming }
    }
    assertTrue(streamingState.isStreaming)

    viewModel.cancelActiveChatRequest()

    val idleState = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming }
    }
    assertFalse(idleState.isStreaming)
    assertNull(idleState.errorMessage)
    assertNotNull(idleState.messages)
  }
}
