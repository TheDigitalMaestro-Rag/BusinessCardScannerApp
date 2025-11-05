package com.project.businesscardscannerapp.Workers

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.project.businesscardscannerapp.ReminderBroadcastReceiver
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.ViewModel.NotificationConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class FollowUpActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SNOOZE = "ACTION_SNOOZE"
        const val ACTION_DONE = "ACTION_DONE"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getIntExtra(NotificationConstants.REMINDER_CARD_ID, -1)
        if (cardId == -1) return

        // Dismiss the notification
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        notificationManager?.cancel(cardId)

        when (intent.action) {
            ACTION_SNOOZE -> handleSnooze(context, intent, cardId)
            ACTION_DONE -> handleDone(context, cardId)
        }

        // REMOVED: Direct activity start - this causes the trampoline restriction
        // Instead, we'll update the notification to show the navigation intent
        // or use a different approach
    }

    private fun handleSnooze(context: Context, intent: Intent, cardId: Int) {
        val snoozeDays = intent.getIntExtra("snooze_days", 1)
        val cardName = intent.getStringExtra("cardName") ?: "Contact"
        val message = intent.getStringExtra(NotificationConstants.REMINDER_MESSAGE) ?: "Time to follow up!"

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

                val card = repository.getCardById(cardId)
                card?.let {
                    val newReminderTime = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(snoozeDays.toLong())

                    // Update card in database
                    repository.update(card.copy(
                        reminderTime = newReminderTime,
                        reminderMessage = message
                    ))

                    // Reschedule the alarm
                    scheduleReminder(context, cardId, newReminderTime, message, cardName)

                    // Show success message
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Snoozed for $snoozeDays day(s)", Toast.LENGTH_SHORT).show()

                        // Update the notification to show the new time
                        updateNotificationWithNavigation(context, cardId, cardName, "Snoozed until ${java.text.SimpleDateFormat("MMM dd, HH:mm").format(java.util.Date(newReminderTime))}")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to snooze reminder", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleDone(context: Context, cardId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)
                val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

                val card = repository.getCardById(cardId)
                card?.let {
                    // Clear reminder from database
                    repository.update(it.copy(
                        reminderTime = null,
                        reminderMessage = null
                    ))

                    // Cancel the alarm
                    cancelReminder(context, cardId)

                    // Show success message
                    CoroutineScope(Dispatchers.Main).launch {
                        Toast.makeText(context, "Reminder marked as done", Toast.LENGTH_SHORT).show()

                        // Update notification to show completion
                        updateNotificationWithNavigation(context, cardId, card.name, "Reminder completed")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    Toast.makeText(context, "Failed to mark as done", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateNotificationWithNavigation(context: Context, cardId: Int, cardName: String, message: String) {
        // Create a new notification that allows navigation
        val mainIntent = Intent(context, com.project.businesscardscannerapp.MainActivity::class.java).apply {
            putExtra(NotificationConstants.REMINDER_CARD_ID, cardId)
            putExtra("navigateToDetails", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            cardId,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show a new notification that user can click to navigate
        com.project.businesscardscannerapp.Utils.NotificationUtils.showSmartNotification(
            context = context,
            title = "Follow-up: $cardName",
            message = message,
            notificationId = com.project.businesscardscannerapp.Utils.NotificationIdGenerator.getUniqueId(),
            priority = androidx.core.app.NotificationCompat.PRIORITY_DEFAULT
        )
    }

    private fun scheduleReminder(context: Context, cardId: Int, time: Long, message: String, cardName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java).apply {
            putExtra(NotificationConstants.REMINDER_CARD_ID, cardId)
            putExtra(NotificationConstants.REMINDER_MESSAGE, message)
            putExtra(NotificationConstants.REMINDER_NOTIFICATION_ID, cardId + NotificationConstants.REMINDER_REQUEST_CODE_BASE)
            putExtra("cardName", cardName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + NotificationConstants.REMINDER_REQUEST_CODE_BASE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    private fun cancelReminder(context: Context, cardId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderBroadcastReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + NotificationConstants.REMINDER_REQUEST_CODE_BASE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }
}