package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.BudgetDao
import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ChatMessageDao
import com.example.data.local.dao.ChatSessionDao
import com.example.data.local.dao.SavingsGoalDao
import com.example.data.local.dao.TransactionDao
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ChatMessageEntity
import com.example.data.local.entity.ChatSessionEntity
import com.example.data.local.entity.SavingsContributionEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.TransactionEntity

/**
 * Local Room SQLite database storing transactions, monthly category budgets, savings goals,
 * contribution history, categories, and multi-session assistant conversations.
 */
@Database(
  entities = [
    TransactionEntity::class,
    BudgetEntity::class,
    SavingsGoalEntity::class,
    SavingsContributionEntity::class,
    CategoryEntity::class,
    ChatSessionEntity::class,
    ChatMessageEntity::class,
  ],
  version = 3,
  exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class BudgetWiseDatabase : RoomDatabase() {
  abstract fun transactionDao(): TransactionDao
  abstract fun budgetDao(): BudgetDao
  abstract fun savingsGoalDao(): SavingsGoalDao
  abstract fun categoryDao(): CategoryDao
  abstract fun chatSessionDao(): ChatSessionDao
  abstract fun chatMessageDao(): ChatMessageDao

  companion object {
    @Volatile
    private var INSTANCE: BudgetWiseDatabase? = null

    /** Adds persistent `chat_messages` storage while preserving all v1 financial records. */
    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `chat_messages` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `role` TEXT NOT NULL,
            `content` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS `index_chat_messages_createdAtMillis` ON `chat_messages` (`createdAtMillis`)"
        )
      }
    }

    /**
     * Introduces `chat_sessions` and migrates any existing v2 `chat_messages` into a default
     * auto-titled session (`id = 1`) backed by an `ON DELETE CASCADE` foreign key.
     */
    val MIGRATION_2_3 = object : Migration(2, 3) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `chat_sessions` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `title` TEXT NOT NULL,
            `isTitleManuallySet` INTEGER NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            `updatedAtMillis` INTEGER NOT NULL
          )
          """.trimIndent()
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS `index_chat_sessions_updatedAtMillis` ON `chat_sessions` (`updatedAtMillis`)"
        )

        val now = System.currentTimeMillis()
        var hasExistingMessages = false
        var minCreated = now
        var maxCreated = now
        var firstUserPrompt: String? = null

        db.query("SELECT COUNT(*), MIN(createdAtMillis), MAX(createdAtMillis) FROM `chat_messages`").use { cursor ->
          if (cursor.moveToFirst()) {
            val count = cursor.getInt(0)
            hasExistingMessages = count > 0
            if (hasExistingMessages) {
              minCreated = cursor.getLong(1)
              maxCreated = cursor.getLong(2)
            }
          }
        }

        if (hasExistingMessages) {
          db.query(
            "SELECT `content` FROM `chat_messages` WHERE `role` = 'USER' ORDER BY `createdAtMillis` ASC, `id` ASC LIMIT 1"
          ).use { cursor ->
            if (cursor.moveToFirst()) {
              firstUserPrompt = cursor.getString(0)
            }
          }
          val initialTitle = com.example.domain.ChatTitleGenerator.generateTitle(
            listOfNotNull(firstUserPrompt)
          )
          db.execSQL(
            "INSERT INTO `chat_sessions` (`id`, `title`, `isTitleManuallySet`, `createdAtMillis`, `updatedAtMillis`) VALUES (1, ?, 0, ?, ?)",
            arrayOf<Any>(initialTitle, minCreated, maxCreated),
          )
        }

        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `chat_messages_new` (
            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
            `sessionId` INTEGER NOT NULL,
            `role` TEXT NOT NULL,
            `content` TEXT NOT NULL,
            `createdAtMillis` INTEGER NOT NULL,
            FOREIGN KEY(`sessionId`) REFERENCES `chat_sessions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
          )
          """.trimIndent()
        )

        if (hasExistingMessages) {
          db.execSQL(
            """
            INSERT INTO `chat_messages_new` (`id`, `sessionId`, `role`, `content`, `createdAtMillis`)
            SELECT `id`, 1, `role`, `content`, `createdAtMillis` FROM `chat_messages`
            """.trimIndent()
          )
        }

        db.execSQL("DROP TABLE `chat_messages`")
        db.execSQL("ALTER TABLE `chat_messages_new` RENAME TO `chat_messages`")
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS `index_chat_messages_createdAtMillis` ON `chat_messages` (`createdAtMillis`)"
        )
        db.execSQL(
          "CREATE INDEX IF NOT EXISTS `index_chat_messages_sessionId` ON `chat_messages` (`sessionId`)"
        )
      }
    }

    fun getInstance(context: Context): BudgetWiseDatabase =
      INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          BudgetWiseDatabase::class.java,
          "budgetwise_db",
        )
          .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
          .build()
          .also { INSTANCE = it }
      }
  }
}
