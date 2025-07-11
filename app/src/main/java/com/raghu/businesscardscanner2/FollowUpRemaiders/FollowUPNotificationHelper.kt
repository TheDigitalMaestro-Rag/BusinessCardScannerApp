package com.raghu.businesscardscanner2.FollowUpRemaiders

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.raghu.businesscardscanner.MainActivity
import com.raghu.businesscardscanner.R
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility.FollowUpRepeatUtils

// --- Notification Helper: NotificationHelper.kt ---
class NotificationHelper(private val context: Context) {
    init {
        createNotificationChannel()
    }

    fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "follow_up_channel",
                "Follow Up Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for follow-up reminders"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleFollowUpNotification(cardId: Int, message: String, triggerTime: Long, repeatType: String = "None",contactName: String = "", companyName: String = "",) {

        val snoozeIntent = Intent(context, FollowUpReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markDoneIntent = Intent(context, FollowUpReceiver::class.java).apply {
            action = "ACTION_MARK_DONE"
            putExtra("notification_id", cardId)
        }

        val markDonePendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + 2000,
            markDoneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Reminder for $contactName (${companyName.ifBlank { "Unknown Company" }})"
        val contentText = message


        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminderId", cardId)
            putExtra("destination", "reminders")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            cardId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}") // replace with your sound or use from reminder.notificationSoundUri

        val notification = NotificationCompat.Builder(context, "follow_up_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri) // ✅ Add this
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_snooze, "Snooze", snoozePendingIntent)
            .addAction(R.drawable.ic_done, "Mark Done", markDonePendingIntent)
            .build()


        val alarmIntent = Intent(context, FollowUpReceiver::class.java).apply {
            putExtra("notification", notification)
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
            putExtra("repeat_type", repeatType) // ✅ Pass repeatType
        }

        val pendingAlarmIntent = PendingIntent.getBroadcast(
            context,
            cardId,
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // ✅ Check permission before using setExact
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingAlarmIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingAlarmIntent
                )
            }
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingAlarmIntent
            )
        }
    }

    fun scheduleNotification(reminder: FollowUpReminderEntity) {
        // Build notification with custom sound
        val soundUri = reminder.notificationSoundUri?.let { Uri.parse(it) }
        val notification = NotificationCompat.Builder(context, "reminder_channel")
            .setContentTitle(reminder.contactName)
            .setContentText(reminder.message)
            .setSmallIcon(R.drawable.ic_notification)
            .setSound(soundUri)
            .build()
        // Schedule the notification (implementation depends on your AlarmManager/WorkManager logic)
    }

    fun cancelNotification(reminderId: Int) {
        // Cancel notification logic
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(reminderId)
    }
}

// --- Broadcast Receiver: FollowUpReceiver.kt ---
class FollowUpReceiver : BroadcastReceiver() {
    // FollowUpReceiver.kt
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val notificationId = intent.getIntExtra("notification_id", 0)
        val message = intent.getStringExtra("notification_message") ?: "Follow up reminder"

        when (action) {
            "ACTION_SNOOZE" -> {
                val snoozeMillis = System.currentTimeMillis() + 10 * 60 * 1000
                NotificationHelper(context).scheduleFollowUpNotification(notificationId, message, snoozeMillis)
            }

            "ACTION_MARK_DONE" -> {
                ReminderActionService.markReminderAsDone(context, notificationId)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId)
            }

            else -> {
                // Show notification
                val notification = intent.getParcelableExtra<Notification>("notification")
                notification?.let {
                    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.notify(notificationId, it)
                }

                // ✅ Auto-reschedule if repeat type is not "None"
                val repeatType = intent.getStringExtra("repeat_type") ?: "None"
                if (repeatType != "None") {
                    val nextTrigger = FollowUpRepeatUtils.calculateNextTriggerTime(System.currentTimeMillis(), repeatType)
                    if (nextTrigger > 0) {
                        NotificationHelper(context).scheduleFollowUpNotification(notificationId, message, nextTrigger,)
                    }
                }
            }
        }


    }

}