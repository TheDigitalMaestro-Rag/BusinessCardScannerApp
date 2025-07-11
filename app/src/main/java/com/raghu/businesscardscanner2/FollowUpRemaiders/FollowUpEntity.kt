package com.raghu.businesscardscanner2.FollowUpRemaiders

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- Entity: FollowUpReminderEntity.kt ---
@Entity(tableName = "follow_up_reminders")
data class FollowUpReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Long,
    val contactName: String,
    val message: String,
    val dueDate: Long,
    val repeatType: String = "None", // "None", "Daily", "Weekly", "Monthly"
    val category: String = "General", // "General", "Call", "Email", "Meeting", "Task"
    val snoozeCount: Int = 0,
    val phoneNumber: String? = null,
    val email: String? = null,
    val companyName: String? = null,
    val isCompleted: Boolean = false,
    val notes: String? = null,
    val tags: String? = null, // comma-separated tags
    val notificationSoundUri: String? = null
)