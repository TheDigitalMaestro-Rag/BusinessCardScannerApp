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
        }
    }

    fun snoozeReminder(context: Context, reminderId: Int, snoozeMillis: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getDatabase(context)
            val reminder = db.followUpReminderDao().getReminderById(reminderId)
            db.followUpReminderDao().updateReminder(reminder.copy(dueDate = System.currentTimeMillis() + snoozeMillis))
        }
    }
}