// FileName: MultipleFiles/DataBase.kt
package com.project.businesscardscannerapp.RoomDB.DataBase

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.project.businesscardscannerapp.Converters.Converters
import com.project.businesscardscannerapp.RoomDB.DAO.BusinessCardDao
import com.project.businesscardscannerapp.RoomDB.DAO.GoalDao
import com.project.businesscardscannerapp.RoomDB.DAO.InsightsDao
import com.project.businesscardscannerapp.RoomDB.Entity.BanditArm
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.Entity.CardFolderCrossRef
import com.project.businesscardscannerapp.RoomDB.Entity.Folder
import com.project.businesscardscannerapp.RoomDB.Entity.FollowUp
import com.project.businesscardscannerapp.RoomDB.Entity.GoalCompletion


@Database(
    entities = [
        BusinessCard::class,
        Folder::class,
        CardFolderCrossRef::class,
        FollowUp::class,
        BanditArm::class,
        GoalCompletion::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao
    abstract fun insightsDao(): InsightsDao
    abstract fun goalDao(): GoalDao

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
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    ALTER TABLE business_cards
                    ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0
                """.trimIndent())
            }
        }

        // FIXED: Correct version numbers (3→4)
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        // ADD MISSING MIGRATIONS for versions 4-8
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 5 here
                // If no changes, this can be empty but should exist
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 6 here
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 7 here
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 8 here
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 9 here
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 10 here
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add any schema changes for version 11 here
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN reminderTime INTEGER")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN reminderMessage TEXT")
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN leadScore INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN leadCategory TEXT NOT NULL DEFAULT 'Unknown'")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN industry TEXT NOT NULL DEFAULT 'Unknown'")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastContactDate INTEGER")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to business_cards table
                database.execSQL("ALTER TABLE business_cards ADD COLUMN countryCode TEXT")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN firstContactMethod TEXT")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastInteractionAt INTEGER")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN openedCount INTEGER NOT NULL DEFAULT 0")

                // Create follow_ups table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS follow_ups (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER NOT NULL,
                        scheduledAt INTEGER NOT NULL,
                        reason TEXT NOT NULL,
                        suggestedByAi INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        completedAt INTEGER,
                        FOREIGN KEY(cardId) REFERENCES business_cards(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create index
                database.execSQL("CREATE INDEX IF NOT EXISTS index_follow_ups_cardId ON follow_ups(cardId)")

                // Create bandit_arms table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS bandit_arms (
                        armId TEXT PRIMARY KEY NOT NULL,
                        tries INTEGER NOT NULL,
                        successes INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS goal_completions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        cardId INTEGER NOT NULL,
                        completedAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN tags TEXT NOT NULL DEFAULT '[]'")
            }
        }

        // NEW: Migration 16 to 17 - Add pipelineStage column
        private val MIGRATION_16_17 = object : Migration(16, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add pipelineStage column with default value 'NEW'
                database.execSQL("ALTER TABLE business_cards ADD COLUMN pipelineStage TEXT NOT NULL DEFAULT 'NEW'")

                // Create index for better performance on pipeline queries
                database.execSQL("CREATE INDEX IF NOT EXISTS index_business_cards_pipelineStage ON business_cards(pipelineStage)")
            }
        }

        // NEW: Migration 17 to 18 - Add any additional pipeline-related fields if needed
        private val MIGRATION_17_18 = object : Migration(17, 18) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // You can add more pipeline-related fields here if needed
                // For now, this migration can be empty but maintains version sequence
            }
        }

        // NEW: Migration 18 to 19 - Add stage transition timestamp tracking
        private val MIGRATION_18_19 = object : Migration(18, 19) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Optional: Add stage entry timestamp for analytics
                database.execSQL("ALTER TABLE business_cards ADD COLUMN stageEnteredAt INTEGER")

                // Initialize existing records with current timestamp
                database.execSQL("UPDATE business_cards SET stageEnteredAt = createdAt WHERE stageEnteredAt IS NULL")
            }
        }

        // In your Database.kt - TEMPORARY FIX
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "business_card_database_new"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15,
                        MIGRATION_15_16,
                        MIGRATION_16_17,
                        MIGRATION_17_18,
                        MIGRATION_18_19
                    )
//                    .fallbackToDestructiveMigration() // ADD THIS LINE TEMPORARILY
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
