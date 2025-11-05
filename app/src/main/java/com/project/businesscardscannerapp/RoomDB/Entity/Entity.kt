// FileName: MultipleFiles/Entity.kt
package com.project.businesscardscannerapp.RoomDB.Entity

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
    val phones: List<String>,
    val email: String,
    val address: String,
    val website: String,
    val notes: String,
    val imagePath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val lastFollowUpDate: Long? = null, // Existing field, can be used for reminder date
    val lastViewedAt: Long = System.currentTimeMillis(),
    // New fields for reminder functionality
    val reminderTime: Long? = null, // Timestamp for the reminder
    val reminderMessage: String? = null, // Message for the reminder
    val tags: List<String> = emptyList(),
    // Add these new fields for lead scoring
    val leadScore: Int = 0,
    val leadCategory: String = "Unknown",
    val industry: String = "Unknown",
    val lastContactDate: Long? = null, // For lead scoring recency
    val countryCode: String? = null, // For timezone/country-specific reminders
    val firstContactMethod: String? = null, // e.g., "call", "email", "whatsapp"
    val lastInteractionAt: Long? = null, // Timestamp of last interaction
    val openedCount: Int = 0, // How many times the card was opened/viewed
    val pipelineStage: PipelineStage = PipelineStage.NEW
)

// In Entity.kt - MAKE SURE PipelineStage is just a regular enum, NOT an @Entity
enum class PipelineStage {
    NEW, CONTACTED, MEETING, NEGOTIATION, CLOSED_WON, CLOSED_LOST;

    fun getDisplayName(): String {
        return when (this) {
            NEW -> "New Lead"
            CONTACTED -> "Contacted"
            MEETING -> "Meeting Scheduled"
            NEGOTIATION -> "In Negotiation"
            CLOSED_WON -> "Closed Won"
            CLOSED_LOST -> "Closed Lost"
        }
    }

    fun getNextStage(): PipelineStage? {
        return when (this) {
            NEW -> CONTACTED
            CONTACTED -> MEETING
            MEETING -> NEGOTIATION
            NEGOTIATION -> CLOSED_WON
            CLOSED_WON -> null
            CLOSED_LOST -> null
        }
    }

    fun getPreviousStage(): PipelineStage? {
        return when (this) {
            NEW -> null
            CONTACTED -> NEW
            MEETING -> CONTACTED
            NEGOTIATION -> MEETING
            CLOSED_WON -> NEGOTIATION
            CLOSED_LOST -> NEGOTIATION
        }
    }
}

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

@Entity(
    tableName = "follow_ups",
    foreignKeys = [
        ForeignKey(
            entity = BusinessCard::class,
            parentColumns = ["id"],
            childColumns = ["cardId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId")]
)
data class FollowUp(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val scheduledAt: Long,       // millis
    val reason: String,          // "first-followup" | "no-reply" | "reopen"
    val suggestedByAi: Boolean,
    val status: String,          // "scheduled"|"done"|"snoozed"|"skipped"
    val completedAt: Long?       // millis
)

@Entity(tableName = "bandit_arms")
data class BanditArm(          // per-user bandit data
    @PrimaryKey val armId: String,   // e.g., "1d","3d","7d","14d"
    val tries: Int,
    val successes: Int              // “user contacted around reminder”
)

// Add to Entity.kt
@Entity(tableName = "goal_completions")
data class GoalCompletion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardId: Int,
    val completedAt: Long
)

// Add to Entity.kt
@Entity(tableName = "backup_metadata")
data class BackupMetadata(
    @PrimaryKey val id: Int = 1, // Singleton
    val lastBackupTimestamp: Long = 0,
    val backupId: String? = null,
    val isBackupEnabled: Boolean = false
)
