package com.raghu.businesscardscanner2.RoomDB.DataBase

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.raghu.businesscardscanner2.RoomDB.DAO.BusinessCardDao
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.Entity.CardFolderCrossRef
import com.raghu.businesscardscanner2.RoomDB.Entity.Folder



@Database(
    entities = [
        BusinessCard::class,
        Folder::class,
        CardFolderCrossRef::class
    ],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun businessCardDao(): BusinessCardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Define your migrations
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create folders table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS folders (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())

                // Create junction table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS cardfoldercrossref (
                        cardId INTEGER NOT NULL,
                        folderId INTEGER NOT NULL,
                        PRIMARY KEY(cardId, folderId),
                        FOREIGN KEY(cardId) REFERENCES business_cards(id) ON DELETE CASCADE,
                        FOREIGN KEY(folderId) REFERENCES folders(id) ON DELETE CASCADE
                    )
                """.trimIndent())

                // Create indices for better performance
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cardfoldercrossref_cardId ON cardfoldercrossref(cardId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_cardfoldercrossref_folderId ON cardfoldercrossref(folderId)")
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


        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "business_card_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    // Add this only during development if needed
                    // .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
