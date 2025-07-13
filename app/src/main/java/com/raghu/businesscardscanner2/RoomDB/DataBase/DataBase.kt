package com.raghu.businesscardscanner2.RoomDB.DataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.raghu.businesscardscanner2.Converters.Converters
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpReminderDao
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpReminderEntity
import com.raghu.businesscardscanner2.RoomDB.DAO.BusinessCardDao
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder

@Database(
    entities = [
        BusinessCard::class,
        Folder::class,
        CardFolderCrossRef::class,
        FollowUpReminderEntity::class
    ],
    version = 10,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao
    abstract fun followUpReminderDao(): FollowUpReminderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cardfoldercrossref (
                        cardId INTEGER NOT NULL,
                        folderId INTEGER NOT NULL,
                        PRIMARY KEY(cardId, folderId),
                        FOREIGN KEY(cardId) REFERENCES business_cards(id) ON DELETE CASCADE,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                database.execSQL("CREATE INDEX IF NOT EXISTS index_cardfoldercrossref_cardId ON cardfoldercrossref(cardId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cardfoldercrossref_folderId ON cardfoldercrossref(folderId)")
                database.execSQL("ALTER TABLE BusinessCard ADD COLUMN phones TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE business_cards 
                    ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0
                """.trimIndent())
            }
        }

        // In AppDatabase.kt
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
            CREATE TABLE IF NOT EXISTS follow_up_reminders (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                contactId INTEGER NOT NULL,
                contactName TEXT NOT NULL,
                message TEXT NOT NULL,
                dueDate INTEGER NOT NULL,
                isCompleted INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent())

                // Add lastFollowUpDate column if missing
                try {
                    database.execSQL("ALTER TABLE business_cards ADD COLUMN lastFollowUpDate INTEGER")
                } catch (e: Exception) {
                    // Column already exists
                }
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to follow_up_reminders
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN repeatType TEXT NOT NULL DEFAULT 'None'")
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN category TEXT NOT NULL DEFAULT 'General'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN phoneNumber TEXT")
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN email TEXT")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN companyName TEXT;")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN notes TEXT;")
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN tags TEXT;")
                database.execSQL("ALTER TABLE follow_up_reminders ADD COLUMN notificationSoundUri TEXT;")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "business_card_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
