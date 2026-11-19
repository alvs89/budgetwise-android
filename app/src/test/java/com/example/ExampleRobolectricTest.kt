package com.example

import android.content.Context
import android.view.WindowManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.BudgetWiseDatabase
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.domain.ChatRole
import com.example.ui.components.adaptColorForSurface
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("BudgetWise", appName)
  }

  @Test
  fun `dark category tint adapts to dark surface for clear visibility`() {
    val legacyHousingNavy = Color(0xFF102A43)
    val adapted = adaptColorForSurface(legacyHousingNavy, isDarkSurface = true)
    assertTrue("Expected adapted luminance > 0.25, was ${adapted.luminance()}", adapted.luminance() > 0.25f)
  }

  @Test
  fun `mainActivity does not set FLAG_SECURE so screenshots are allowed`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).create()
    val flags = controller.get().window.attributes.flags
    assertTrue(
      "Expected FLAG_SECURE to be disabled on MainActivity window",
      (flags and WindowManager.LayoutParams.FLAG_SECURE) == 0,
    )
  }

  @Test
  fun `chat messages persist locally in Room database`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, BudgetWiseDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      val sessionId = db.chatSessionDao().insert(
        ChatSessionEntity(title = "Weekly Spending", createdAtMillis = 1000L, updatedAtMillis = 1000L),
      )
      val dao = db.chatMessageDao()
      dao.insert(ChatMessageEntity(sessionId = sessionId, role = ChatRole.USER, content = "How much did I spend this week?", createdAtMillis = 1000L))
      dao.insert(ChatMessageEntity(sessionId = sessionId, role = ChatRole.MODEL, content = "You spent ₱1,530.00 this week.", createdAtMillis = 2000L))

      val saved = dao.observeMessagesForSession(sessionId).first()
      assertEquals(2, saved.size)
      assertEquals(ChatRole.USER, saved[0].role)
      assertEquals(ChatRole.MODEL, saved[1].role)

      dao.deleteAll()
      assertTrue(dao.observeAllMessages().first().isEmpty())
    } finally {
      db.close()
    }
  }
}
