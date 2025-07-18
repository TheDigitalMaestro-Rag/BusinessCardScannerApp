package com.raghu.businesscardscanner2.FollowUpRemaiders

import android.content.Context
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ReminderActionService {
    fun markReminderAsDone(context: Context, reminderId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            db.followUpReminderDao().markReminderCompleted(reminderId)

            // Ensure notification is cancelled
            NotificationHelper(context).cancelNotification(reminderId)
        }
    }

    fun snoozeReminder(context: Context, reminderId: Int, snoozeMillis: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.followUpReminderDao().getReminderById(reminderId)
            val newDueDate = System.currentTimeMillis() + snoozeMillis
            db.followUpReminderDao().updateReminder(
                reminder.copy(
                    dueDate = newDueDate,
                    snoozeCount = reminder.snoozeCount + 1
                )
            )

            // Cancel existing notification
            NotificationHelper(context).cancelNotification(reminderId)
        }
    }
}

//object ReminderActionService {
//
//    fun markReminderAsDone(context: Context, reminderId: Int) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val db = AppDatabase.getDatabase(context)
//            db.followUpReminderDao().markReminderCompleted(reminderId)
//        }
//    }
//
//    fun snoozeReminder(context: Context, reminderId: Int, snoozeMillis: Long) {
//        CoroutineScope(Dispatchers.IO).launch {
//            val db = AppDatabase.getDatabase(context)
//            val reminder = db.followUpReminderDao().getReminderById(reminderId)
//            val newDueDate = System.currentTimeMillis() + snoozeMillis
//            db.followUpReminderDao().updateReminder(
//                reminder.copy(
//                    dueDate = newDueDate,
//                    snoozeCount = reminder.snoozeCount + 1
//                )
//            )
//        }
//    }
//}