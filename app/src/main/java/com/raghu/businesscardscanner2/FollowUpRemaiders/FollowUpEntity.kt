package com.raghu.businesscardscanner2.FollowUpRemaiders

import androidx.room.Entity
import androidx.room.PrimaryKey

// --- Entity: FollowUpReminderEntity.kt ---
@Entity(tableName = "follow_up_reminders")
data class FollowUpReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Long,
    var contactName: String,
    var message: String,
    var dueDate: Long,
    var repeatType: String = "None", // "None", "Daily", "Weekly", "Monthly"
    var category: String = "General", // "General", "Call", "Email", "Meeting", "Task"
    var snoozeCount: Int = 0,
    var phoneNumber: String? = null,
    var email: String? = null,
    var companyName: String? = null,
    var isCompleted: Boolean = false,
    var notes: String? = null,
    var tags: String? = null, // comma-separated tags
    var notificationSoundUri: String? = null
)