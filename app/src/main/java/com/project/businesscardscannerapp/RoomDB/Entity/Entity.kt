package com.raghu.businesscardscanner2.RoomDB.Entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "business_cards")
data class BusinessCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val company: String,
    val position: String,
    val phone: String,
    val email: String,
    val address: String,
    val website: String,
    val notes: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    // Add this if you have it in your database
    @ColumnInfo(defaultValue = "0")
    val isFavorite: Boolean = false
)

// Folder entity
// Add to your existing entities
@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

// Junction table for many-to-many relationship
@Entity(
    tableName = "cardfoldercrossref",
    primaryKeys = ["cardId", "folderId"],
    foreignKeys = [
        ForeignKey(
            entity = BusinessCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Folder::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cardId"]),
        Index(value = ["folderId"])
    ]
)
data class CardFolderCrossRef(
    val cardId: Int,
    val folderId: Int
)