// FileName: MultipleFiles/DataBase.kt
package com.raghu.businesscardscanner2.RoomDB.DataBase

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.raghu.businesscardscanner2.Converters.Converters
import com.raghu.businesscardscanner2.RoomDB.DAO.BusinessCardDao
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder

@Database(
    entities = [
        BusinessCard::class,
        Folder::class,
        CardFolderCrossRef::class,
    ],
    version = 13, // Increment this version number
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao

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

        private val MIGRATION_3_4 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastViewedAt INTEGER NOT NULL DEFAULT ${System.currentTimeMillis()}")
            }
        }

        // New migration for reminderTime and reminderMessage
        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN reminderTime INTEGER")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN reminderMessage TEXT")
            }
        }

        // In DataBase.kt, add a new migration
        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE business_cards ADD COLUMN leadScore INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN leadCategory TEXT NOT NULL DEFAULT 'Unknown'")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN industry TEXT NOT NULL DEFAULT 'Unknown'")
                database.execSQL("ALTER TABLE business_cards ADD COLUMN lastContactDate INTEGER")
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
                        MIGRATION_11_12, // Add the new migration here
                        MIGRATION_12_13
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
