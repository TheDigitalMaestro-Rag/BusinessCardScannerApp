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
import android.util.Log
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
    fun scheduleFollowUpNotification(cardId: Int, message: String, triggerTime: Long, repeatType: String = "None",contactName: String? = null, companyName: String? = null, snoozeMinutes: Long = 10) {
        // Create a proper title handling empty cases
        // Create a proper title handling all cases
        val title = buildString {
            append("Reminder")

            val nameToShow = contactName?.takeIf { it.isNotBlank() }
            val companyToShow = companyName?.takeIf { it.isNotBlank() }

            when {
                nameToShow != null && companyToShow != null ->
                    append(" for $nameToShow ($companyToShow)")
                nameToShow != null ->
                    append(" for $nameToShow")
                companyToShow != null ->
                    append(" for $companyToShow")
                else ->
                    append(": Follow-up")
            }
        }


        val contentText = message

        // Rest of your existing code...
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


        // Validate snooze minutes
        val validSnoozeMinutes = if (snoozeMinutes < 1) 10 else snoozeMinutes

        val snoozeIntent = Intent(context, FollowUpReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
            putExtra("snooze_minutes", validSnoozeMinutes)
        }

        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + 1000,
            snoozeIntent,
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
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_snooze,
                    "Snooze (${validSnoozeMinutes} min)",
                    snoozePendingIntent
                )
            )
            .addAction(R.drawable.ic_done, "Mark Done", markDonePendingIntent)
            .build()


        val alarmIntent = Intent(context, FollowUpReceiver::class.java).apply {
            putExtra("notification", notification)
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
            putExtra("repeat_type", repeatType)
            contactName?.let { putExtra("contact_name", it) }
            companyName?.let { putExtra("company_name", it) }
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

//    fun scheduleNotification(reminder: FollowUpReminderEntity) {
//        // Build notification with custom sound
//        val soundUri = reminder.notificationSoundUri?.let { Uri.parse(it) }
//        val notification = NotificationCompat.Builder(context, "reminder_channel")
//            .setContentTitle(reminder.contactName)
//            .setContentText(reminder.message)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setSound(soundUri)
//            .build()
//        // Schedule the notification (implementation depends on your AlarmManager/WorkManager logic)
//    }

    fun scheduleNotification(reminder: FollowUpReminderEntity) {
        scheduleFollowUpNotification(
            cardId = reminder.id,
            message = reminder.message,
            triggerTime = reminder.dueDate,
            repeatType = reminder.repeatType,
            contactName = reminder.contactName,
            companyName = reminder.companyName
        )
    }

    fun cancelNotification(reminderId: Int) {
        // Cancel notification
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(reminderId)

        // Cancel pending alarm
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, FollowUpReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
            it.cancel()
        }
    }
}

// --- Broadcast Receiver: FollowUpReceiver.kt ---
class FollowUpReceiver : BroadcastReceiver() {
    // FollowUpReceiver.kt
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            val notificationId = intent.getIntExtra("notification_id", 0)
            val message = intent.getStringExtra("notification_message") ?: "Follow up reminder"

            when (action) {
                "ACTION_SNOOZE" -> {
                    val notificationId = intent.getIntExtra("notification_id", 0)
                    val message = intent.getStringExtra("notification_message") ?: "Follow up reminder"
                    val snoozeMinutes = intent.getLongExtra("snooze_minutes", 10)

                    // Update in database first
                    ReminderActionService.snoozeReminder(
                        context,
                        notificationId,
                        snoozeMinutes * 60 * 1000
                    )

                    // Then schedule new notification
                    val snoozeMillis = System.currentTimeMillis() + snoozeMinutes * 60 * 1000
                    NotificationHelper(context).scheduleFollowUpNotification(
                        notificationId,
                        message,
                        snoozeMillis,
                        snoozeMinutes = snoozeMinutes,
                        contactName = intent.getStringExtra("contact_name"),
                        companyName = intent.getStringExtra("company_name")
                    )
                }


                "ACTION_MARK_DONE" -> {
                    ReminderActionService.markReminderAsDone(context, notificationId)
                    val notificationManager =
                        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(notificationId)
                }

                else -> {
                    // Show notification
                    val notification = intent.getParcelableExtra<Notification>("notification")
                    notification?.let {
                        val notificationManager =
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(notificationId, it)
                    }

                    // ✅ Auto-reschedule if repeat type is not "None"
                    val repeatType = intent.getStringExtra("repeat_type") ?: "None"
                    if (repeatType != "None") {
                        val nextTrigger = FollowUpRepeatUtils.calculateNextTriggerTime(
                            System.currentTimeMillis(),
                            repeatType
                        )
                        if (nextTrigger > 0) {
                            NotificationHelper(context).scheduleFollowUpNotification(
                                notificationId,
                                message,
                                nextTrigger,
                                repeatType,
                                contactName = intent.getStringExtra("contact_name"),
                                companyName = intent.getStringExtra("company_name")
                            )
                        }
                    }
                }
            }
        }catch(e: Exception){
            Log.e("FollowUpReceiver", "Error handling notification action", e)
        }


    }

}