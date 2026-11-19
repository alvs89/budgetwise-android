package com.example

import android.app.Application
import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.ai.FinancialChatService
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.domain.ChatRole
import com.example.domain.ChatTitleGenerator
import com.example.ui.viewmodel.BudgetWiseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatManagementTest {

  private val testDispatcher = UnconfinedTestDispatcher()

  private val stubChatService = object : FinancialChatService {
    override fun streamReply(
      priorHistory: List<ChatMessageEntity>,
      userQuestion: String,
      financialContextDigest: String,
    ): Flow<String> = flow {
      emit("BudgetWise reply to: $userQuestion")
    }
  }

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun `chatTitleGenerator generates concise relevant titles and detects generic greetings`() {
    assertTrue(ChatTitleGenerator.isGenericPrompt("Hi"))
    assertTrue(ChatTitleGenerator.isGenericPrompt("Hello!"))
    assertTrue(ChatTitleGenerator.isGenericPrompt("help me"))
    assertFalse(ChatTitleGenerator.isGenericPrompt("Am I overspending on food this month?"))

    assertEquals(
      "Overspending Check",
      ChatTitleGenerator.generateTitle(listOf("Am I overspending this month?")),
    )
    assertEquals(
      "Monthly Budget Plan",
      ChatTitleGenerator.generateTitle(listOf("Can you suggest a realistic monthly budget plan?")),
    )
    assertEquals(
      "Savings Goals Review",
      ChatTitleGenerator.generateTitle(listOf("How are my savings goals progressing?")),
    )
    assertEquals(
      "November Spending Summary",
      ChatTitleGenerator.generateTitle(listOf("How much did I spend in Nov?")),
    )
    assertEquals(
      "Transportation Budget Strategy",
      ChatTitleGenerator.generateTitle(listOf("How can I rein in my Transportation budget?")),
    )
    assertEquals(
      ChatTitleGenerator.DEFAULT_FALLBACK_TITLE,
      ChatTitleGenerator.generateTitle(listOf("Hi")),
    )

    // Never return generic labels or echo short prompts verbatim
    val shortPrompt = "coffee expenses"
    val shortTitle = ChatTitleGenerator.generateTitle(listOf(shortPrompt))
    assertFalse(shortTitle.equals(shortPrompt, ignoreCase = true))
    assertTrue(shortTitle.split(" ").size in 2..5)

    // Sanitizer rejects generic placeholders and verbatim copies, accepts clean 2-5 word titles
    assertEquals(null, ChatTitleGenerator.sanitizeCandidateTitle("New Chat", listOf("Help")))
    assertEquals(null, ChatTitleGenerator.sanitizeCandidateTitle("Untitled Chat", listOf("Help")))
    assertEquals(
      "Food Spending Analysis",
      ChatTitleGenerator.sanitizeCandidateTitle("\"Food Spending Analysis.\"", listOf("How much on food?")),
    )
  }

  @Test
  fun `welcome tagline and consistent BudgetWise branding strings match specification`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("BudgetWise", context.getString(R.string.app_name))
    assertEquals("Take control of your money", context.getString(R.string.welcome_tagline))
    assertTrue(context.getString(R.string.chat_title).contains("BudgetWise"))
    assertFalse(context.getString(R.string.chat_title).contains("Budget Wise"))
  }

  @Test
  fun `multiple chats persist independently, support auto-title refinement, manual rename preservation, and isolated delete`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)
    viewModel.clearChatHistory()

    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.sessions.isEmpty() }
    }

    // 1. Create Chat A starting with a generic greeting -> should auto-refine when topic arrives
    viewModel.sendChatMessage("Hi")
    val afterGreeting = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 2 && it.activeSession != null }
    }
    val sessionAId = afterGreeting.activeSession!!.id
    assertFalse(afterGreeting.activeSession!!.isTitleManuallySet)

    // Send a topic-rich second message -> auto-title should refine to Savings Goals Review
    viewModel.sendChatMessage("How are my savings goals doing?")
    val afterTopic = withTimeout(5_000L) {
      viewModel.chatUiState.first {
        !it.isStreaming &&
          it.messages.size == 4 &&
          it.activeSession?.id == sessionAId &&
          it.activeSession?.title == "Savings Goals Review"
      }
    }
    assertEquals("Savings Goals Review", afterTopic.activeSession?.title)
    assertFalse(afterTopic.activeSession!!.isTitleManuallySet)

    // 2. Manually rename Chat A -> subsequent messages must preserve the manual title
    viewModel.renameChatSession(sessionAId, "Q4 Emergency Fund Plan")
    val afterRename = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.activeSession?.title == "Q4 Emergency Fund Plan" }
    }
    assertTrue(afterRename.activeSession!!.isTitleManuallySet)

    viewModel.sendChatMessage("Am I overspending on food?")
    val afterManualFollowUp = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 6 }
    }
    assertEquals("Q4 Emergency Fund Plan", afterManualFollowUp.activeSession?.title)
    assertTrue(afterManualFollowUp.activeSession!!.isTitleManuallySet)

    // 3. Start New Chat B -> Chat A must remain saved in history
    viewModel.startNewChat()
    val draftState = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.activeSession == null && it.messages.isEmpty() }
    }
    assertEquals(1, draftState.sessions.size)

    viewModel.sendChatMessage("Am I overspending this month?")
    val sessionBState = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 2 && it.sessions.size == 2 }
    }
    val sessionBId = sessionBState.activeSession!!.id
    assertTrue(sessionBId != sessionAId)
    assertEquals("Overspending Check", sessionBState.activeSession?.title)

    // 4. Edit a message in Chat B -> Chat A must remain completely unaffected
    val sessionBFirstUserMsgId = sessionBState.messages.first { it.role == ChatRole.USER }.id
    viewModel.editChatMessageAndRegenerate(sessionBFirstUserMsgId, "Suggest a monthly budget plan")
    val afterEditB = withTimeout(5_000L) {
      viewModel.chatUiState.first {
        !it.isStreaming &&
          it.activeSession?.id == sessionBId &&
          it.messages.size == 2 &&
          it.messages.firstOrNull()?.content == "Suggest a monthly budget plan"
      }
    }
    assertEquals("Monthly Budget Plan", afterEditB.activeSession?.title)

    // Switch back to Chat A and verify its 6 messages & manual title are intact
    viewModel.selectChatSession(sessionAId)
    val backToA = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.activeSession?.id == sessionAId && it.messages.size == 6 }
    }
    assertEquals("Q4 Emergency Fund Plan", backToA.activeSession?.title)

    // 5. Simulate reopening the app with a fresh ViewModel instance -> verify persistence
    val reopenedViewModel = BudgetWiseViewModel(app, stubChatService)
    val reopenedState = withTimeout(5_000L) {
      reopenedViewModel.chatUiState.first { it.sessions.size == 2 }
    }
    assertEquals(2, reopenedState.sessions.size)
    assertNotNull(reopenedState.sessions.find { it.session.title == "Q4 Emergency Fund Plan" })
    assertNotNull(reopenedState.sessions.find { it.session.title == "Monthly Budget Plan" })

    // 6. Delete ONLY Chat B -> Chat A and its messages must remain intact
    reopenedViewModel.deleteChatSession(sessionBId)
    val afterDeleteB = withTimeout(5_000L) {
      reopenedViewModel.chatUiState.first { it.sessions.size == 1 && it.activeSession?.id == sessionAId }
    }
    assertEquals(1, afterDeleteB.sessions.size)
    assertEquals("Q4 Emergency Fund Plan", afterDeleteB.activeSession?.title)
    assertEquals(6, afterDeleteB.messages.size)
  }

  @Test
  fun `deleting a single message removes only that message and preserves the rest of the conversation`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = BudgetWiseViewModel(app, stubChatService)
    viewModel.clearChatHistory()

    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.sessions.isEmpty() }
    }

    viewModel.sendChatMessage("How can I rein in my Transportation budget?")
    val afterFirstTurn = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 2 }
    }
    viewModel.sendChatMessage("What about my Food budget?")
    val afterSecondTurn = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 4 }
    }

    val sessionId = afterSecondTurn.activeSession!!.id
    val firstUserMsgId = afterSecondTurn.messages[0].id
    val secondAssistantMsgId = afterSecondTurn.messages[3].id

    // Delete only the first user message
    viewModel.deleteChatMessage(firstUserMsgId)
    val afterDeleteOne = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.messages.size == 3 }
    }
    assertTrue(afterDeleteOne.messages.none { it.id == firstUserMsgId })
    assertEquals(sessionId, afterDeleteOne.activeSession?.id)

    // Delete only the second assistant reply
    viewModel.deleteChatMessage(secondAssistantMsgId)
    val afterDeleteTwo = withTimeout(5_000L) {
      viewModel.chatUiState.first { it.messages.size == 2 }
    }
    assertTrue(afterDeleteTwo.messages.none { it.id == secondAssistantMsgId })
    assertEquals(1, afterDeleteTwo.sessions.size)

    // Verify persistence after reopening the ViewModel
    val reopened = BudgetWiseViewModel(app, stubChatService)
    val reloadedState = withTimeout(5_000L) {
      reopened.chatUiState.first { it.activeSession?.id == sessionId && it.messages.size == 2 }
    }
    assertEquals(2, reloadedState.messages.size)
    assertEquals("What about my Food budget?", reloadedState.messages.last().content)
  }

  @Test
  fun `copying a chat response preserves full raw Markdown syntax on clipboard`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val markdownResponse = """
      Here is your **Transportation** summary in BudgetWise:
      * **Limit:** ₱2,000.00
      * **Spent:** ₱2,180.00
      * *Status:* Exceeded by **₱180.00**
    """.trimIndent()

    com.example.ui.components.copyMessageToClipboard(context, markdownResponse)

    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val copied = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
    assertEquals(markdownResponse, copied)
  }

  @Test
  fun `ai conversation title refinement updates auto-titled chats and preserves manual titles`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val aiTitledService = object : FinancialChatService {
      override fun streamReply(
        priorHistory: List<ChatMessageEntity>,
        userQuestion: String,
        financialContextDigest: String,
      ): Flow<String> = flow {
        emit("You spent ₱3,200 on rides and gas this month.")
      }

      override suspend fun generateConversationTitle(messages: List<ChatMessageEntity>): String =
        "Monthly Commute Cost Breakdown"
    }

    val viewModel = BudgetWiseViewModel(app, aiTitledService)
    viewModel.clearChatHistory()
    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.sessions.isEmpty() }
    }

    viewModel.sendChatMessage("Why are my daily rides costing so much lately?")
    val refinedState = withTimeout(5_000L) {
      viewModel.chatUiState.first {
        !it.isStreaming && it.activeSession?.title == "Monthly Commute Cost Breakdown"
      }
    }
    val sessionId = refinedState.activeSession!!.id
    assertFalse(refinedState.activeSession!!.isTitleManuallySet)

    // Manual rename must remain protected on subsequent turns
    viewModel.renameChatSession(sessionId, "Commute Optimization")
    withTimeout(5_000L) {
      viewModel.chatUiState.first { it.activeSession?.title == "Commute Optimization" }
    }

    viewModel.sendChatMessage("How do I cut transit costs by 20%?")
    val afterFollowUp = withTimeout(5_000L) {
      viewModel.chatUiState.first { !it.isStreaming && it.messages.size == 4 }
    }
    assertEquals("Commute Optimization", afterFollowUp.activeSession?.title)
    assertTrue(afterFollowUp.activeSession!!.isTitleManuallySet)
  }

  @Test
  fun `migration 2 to 3 preserves legacy chat messages into a default session`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val dbName = "migration_2_3_test.db"
    context.deleteDatabase(dbName)

    val config = SupportSQLiteOpenHelper.Configuration.builder(context)
      .name(dbName)
      .callback(object : SupportSQLiteOpenHelper.Callback(2) {
        override fun onCreate(db: SupportSQLiteDatabase) {
          db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `chat_messages` (
              `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
              `role` TEXT NOT NULL,
              `content` TEXT NOT NULL,
              `createdAtMillis` INTEGER NOT NULL
            )
            """.trimIndent(),
          )
          db.execSQL(
            "INSERT INTO `chat_messages` (`role`, `content`, `createdAtMillis`) VALUES ('USER', 'Am I overspending this month?', 1000)",
          )
          db.execSQL(
            "INSERT INTO `chat_messages` (`role`, `content`, `createdAtMillis`) VALUES ('MODEL', 'Here is your BudgetWise breakdown.', 2000)",
          )
        }

        override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
      })
      .build()

    val helper = FrameworkSQLiteOpenHelperFactory().create(config)
    try {
      val sqliteDb = helper.writableDatabase
      BudgetWiseDatabase.MIGRATION_2_3.migrate(sqliteDb)

      sqliteDb.query("SELECT id, title, isTitleManuallySet FROM chat_sessions").use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("Overspending Check", cursor.getString(1))
        assertEquals(0, cursor.getInt(2))
      }

      sqliteDb.query("SELECT sessionId, role, content FROM chat_messages ORDER BY createdAtMillis ASC").use { cursor ->
        assertEquals(2, cursor.count)
        assertTrue(cursor.moveToFirst())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("USER", cursor.getString(1))
        assertEquals("Am I overspending this month?", cursor.getString(2))
        assertTrue(cursor.moveToNext())
        assertEquals(1L, cursor.getLong(0))
        assertEquals("MODEL", cursor.getString(1))
      }
    } finally {
      helper.close()
      context.deleteDatabase(dbName)
    }
  }
}
