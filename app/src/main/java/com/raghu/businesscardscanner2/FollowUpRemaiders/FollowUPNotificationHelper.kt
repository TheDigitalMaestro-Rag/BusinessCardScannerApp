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
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.raghu.businesscardscanner.MainActivity
import com.raghu.businesscardscanner.R
import com.raghu.businesscardscanner2.FollowUpRemaiders.FollowUpRemainderUtility.FollowUpRepeatUtils
import java.util.Date

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
                // Set the default sound for the channel
                val soundUri = Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}")
                setSound(soundUri, null) // Set sound for the channel
                enableVibration(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000) // Example vibration
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("ScheduleExactAlarm")
    fun scheduleFollowUpNotification(
        cardId: Int,
        message: String,
        triggerTime: Long,
        repeatType: String = "None",
        contactName: String? = null,
        companyName: String? = null,
        snoozeMinutes: Long = 10,
        customSoundUri: String? = null // New parameter for custom sound URI
    ) {
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

        // Full-Screen Intent (for alarm-like behavior)
        val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminderId", cardId)
            putExtra("destination", "reminders")
            putExtra("from_notification", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            cardId + 3000, // Unique request code
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        Log.d("NotificationHelper", "Scheduling notification for ID: $cardId at ${Date(triggerTime)}")


        cancelNotification(cardId)

        // Standard Content Intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("reminderId", cardId)
            putExtra("destination", "reminders")
        }

        val contentPendingIntent = PendingIntent.getActivity(
            context,
            cardId,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Mark Done Intent
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

        // Validate snooze minutes
        val validSnoozeMinutes = if (snoozeMinutes < 1) 10 else snoozeMinutes

        // Snooze Intent
        val snoozeIntent = Intent(context, FollowUpReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
            putExtra("snooze_minutes", validSnoozeMinutes)
            putExtra("repeat_type", repeatType)
            contactName?.let { putExtra("contact_name", it) }
            companyName?.let { putExtra("company_name", it) }
            customSoundUri?.let { putExtra("custom_sound_uri", it) }
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            cardId + 1000,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Determine sound URI
        val soundUri = customSoundUri?.let { Uri.parse(it) }
            ?: Uri.parse("android.resource://${context.packageName}/${R.raw.notification_sound}")

        val notificationBuilder = NotificationCompat.Builder(context, "follow_up_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_snooze,
                    "Snooze (${validSnoozeMinutes} min)",
                    snoozePendingIntent
                )
            )
            .addAction(R.drawable.ic_done, "Mark Done", markDonePendingIntent)
            .setOngoing(true)

        // Set sound for Android versions below O
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            notificationBuilder.setSound(soundUri)
            notificationBuilder.setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
        }

        val notification = notificationBuilder.build()

        val alarmIntent = Intent(context, FollowUpReceiver::class.java).apply {
            putExtra("notification", notification)
            putExtra("notification_id", cardId)
            putExtra("notification_message", message)
            putExtra("repeat_type", repeatType)
            contactName?.let { putExtra("contact_name", it) }
            companyName?.let { putExtra("company_name", it) }
            customSoundUri?.let { putExtra("custom_sound_uri", it) }
        }

        val pendingAlarmIntent = PendingIntent.getBroadcast(
            context,
            cardId, // Use the reminder ID as request code
            alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

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
        scheduleFollowUpNotification(
            cardId = reminder.id,
            message = reminder.message,
            triggerTime = reminder.dueDate,
            repeatType = reminder.repeatType,
            contactName = reminder.contactName,
            companyName = reminder.companyName,
            customSoundUri = reminder.notificationSoundUri
        )
    }

    fun cancelNotification(reminderId: Int) {
        Log.d("NotificationHelper", "Canceling notification for ID: $reminderId")
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.cancel(reminderId)

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


// Update FollowUpReceiver to pass customSoundUri during snooze and repeat
class FollowUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val action = intent.action
            val notificationId = intent.getIntExtra("notification_id", 0)
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.cancel(notificationId)

            val message = intent.getStringExtra("notification_message") ?: "Follow up reminder"
            val repeatType = intent.getStringExtra("repeat_type") ?: "None"
            val contactName = intent.getStringExtra("contact_name")
            val companyName = intent.getStringExtra("company_name")
            val customSoundUri = intent.getStringExtra("custom_sound_uri") // Retrieve custom sound URI

            when (action) {
                "ACTION_SNOOZE" -> {
                    val snoozeMinutes = intent.getLongExtra("snooze_minutes", 10)

                    notificationManager.cancel(notificationId)

                    ReminderActionService.snoozeReminder(
                        context,
                        notificationId,
                        snoozeMinutes * 60 * 1000
                    )

                    val snoozeMillis = System.currentTimeMillis() + snoozeMinutes * 60 * 1000
                    NotificationHelper(context).scheduleFollowUpNotification(
                        notificationId,
                        message,
                        snoozeMillis,
                        repeatType,
                        snoozeMinutes = snoozeMinutes,
                        contactName = contactName,
                        companyName = companyName,
                        customSoundUri = customSoundUri // Pass custom sound URI
                    )
                }

                "ACTION_MARK_DONE" -> {
                    notificationManager.cancel(notificationId)
                    ReminderActionService.markReminderAsDone(context, notificationId)
                }

                null, Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {

                    val notification = intent.getParcelableExtra<Notification>("notification")
                    notification?.let {
                        notificationManager.notify(notificationId, it)
                    }



                    if (repeatType != "None") {
                        val nextTrigger = FollowUpRepeatUtils.calculateNextTriggerTime(
                            System.currentTimeMillis(),
                            repeatType
                        )
                        if (nextTrigger > 0) {
                            NotificationHelper(context).scheduleFollowUpNotification(
                                cardId = notificationId,
                                message = message,
                                triggerTime = System.currentTimeMillis() + 1000, // Show now
                                repeatType = "None", // Prevent repeat chain
                                contactName = contactName,
                                companyName = companyName,
                                customSoundUri = customSoundUri
                            )

                        }
                    }
                }
                else -> {
                    Log.w("FollowUpReceiver", "Unhandled action: ${action}")
                }
            }
        } catch(e: Exception) {
            Log.e("FollowUpReceiver", "Error handling notification action", e)
        }
    }
}
