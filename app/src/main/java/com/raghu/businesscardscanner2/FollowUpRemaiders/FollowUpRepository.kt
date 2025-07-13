package com.raghu.businesscardscanner2.FollowUpRemaiders

import kotlinx.coroutines.flow.Flow

// --- Repository: FollowUpRepository.kt ---
class FollowUpRepository(private val dao: FollowUpReminderDao) {
    fun getPendingReminders() = dao.getPendingReminders()

    suspend fun addReminder(reminder: FollowUpReminderEntity) = dao.insertReminder(reminder)

    suspend fun updateReminder(reminder: FollowUpReminderEntity) {
        dao.updateReminder(reminder)
    }

    suspend fun completeReminder(reminderId: Int) = dao.markReminderCompleted(reminderId)

//    suspend fun snoozeReminder(reminderId: Int, snoozeMillis: Long) {
//        val reminder = dao.getReminderById(reminderId)
//        val newDueDate = System.currentTimeMillis() + snoozeMillis
//        dao.updateReminder(reminder.copy(dueDate = newDueDate, snoozeCount = reminder.snoozeCount + 1))
//    }

    suspend fun snoozeReminder(reminderId: Int, snoozeMillis: Long) {
        val reminder = dao.getReminderById(reminderId)
        val newDueDate = System.currentTimeMillis() + snoozeMillis
        dao.updateReminder(reminder.copy(
            dueDate = newDueDate,
            snoozeCount = reminder.snoozeCount + 1
        ))
    }

    fun getReminderByIdFlow(reminderId: Int) = dao.getReminderByIdFlow(reminderId)
}
