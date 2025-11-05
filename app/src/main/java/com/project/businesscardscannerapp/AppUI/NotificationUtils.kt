// File: NotificationUtils.kt
package com.project.businesscardscannerapp.Utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.project.businesscardscannerapp.R

object NotificationUtils {
    private const val CHANNEL_ID_COACH = "coach_channel"
    private const val CHANNEL_NAME_COACH = "AI Coach"

    private const val CHANNEL_ID_FOLLOW_UP = "follow_up_alarm_channel"
    private const val CHANNEL_NAME_FOLLOW_UP = "Follow-up Alarms"

    private const val CHANNEL_ID_SMART = "smart_notifications"
    private const val CHANNEL_NAME_SMART = "Smart Notifications"

    fun showCoachNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_COACH,
                CHANNEL_NAME_COACH,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "AI Coach notifications and recommendations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_COACH)
            .setSmallIcon(R.drawable.ic_coach)
            .setContentTitle("AI Coach Recommendation")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NotificationIdGenerator.getUniqueId(), notification)
    }

    // Enhanced follow-up alarm notification
    fun showFollowUpAlarmNotification(
        context: Context,
        cardName: String,
        message: String,
        notificationId: Int,
        alarmSound: Boolean = true
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create high-priority alarm channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FOLLOW_UP,
                CHANNEL_NAME_FOLLOW_UP,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm notifications for follow-up reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000) // Vibrate pattern

                if (alarmSound) {
                    val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setSound(alarmSound, android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOLLOW_UP)
            .setSmallIcon(R.drawable.ic_notification) // Create this icon
            .setContentTitle("⏰ Follow-up Alarm: $cardName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply {
                if (alarmSound) {
                    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setSound(alarmSound)
                }
            }
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Add this method to NotificationUtils.kt

    fun showFollowUpAlarmNotificationWithActions(
        context: Context,
        cardName: String,
        message: String,
        notificationId: Int,
        alarmSound: Boolean = true,
        pendingIntent: PendingIntent,
        snooze1DayIntent: PendingIntent,
        snooze3DaysIntent: PendingIntent,
        doneIntent: PendingIntent
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create high-priority alarm channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_FOLLOW_UP,
                CHANNEL_NAME_FOLLOW_UP,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Loud alarm notifications for follow-up reminders"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(1000, 1000, 1000, 1000) // Vibrate pattern

                if (alarmSound) {
                    val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setSound(alarmSound, android.media.AudioAttributes.Builder()
                        .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                }
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_FOLLOW_UP)
            .setSmallIcon(R.drawable.ic_notification) // Create this icon
            .setContentTitle("⏰ Follow-up Alarm: $cardName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setOngoing(false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent) // Set the intent to open card details when clicked
            // ADD ACTION BUTTONS
            .addAction(
                R.drawable.ic_snooze, // Create this icon or use existing
                "Snooze 1 Day",
                snooze1DayIntent
            )
            .addAction(
                R.drawable.ic_snooze, // Create this icon or use existing
                "Snooze 3 Days",
                snooze3DaysIntent
            )
            .addAction(
                R.drawable.ic_done, // Create this icon or use existing
                "Mark Done",
                doneIntent
            )
            .apply {
                if (alarmSound) {
                    val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    setSound(alarmSound)
                }
            }
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Smart notification for daily digest and stale leads
    fun showSmartNotification(
        context: Context,
        title: String,
        message: String,
        notificationId: Int,
        priority: Int = NotificationCompat.PRIORITY_DEFAULT
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SMART,
                CHANNEL_NAME_SMART,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart follow-up and lead notifications"
                enableVibration(true)
                enableLights(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_SMART)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(priority)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    // Add to NotificationUtils class

    fun showSmartFollowUpNotification(
        context: Context,
        cardName: String,
        message: String,
        notificationId: Int,
        pendingIntent: PendingIntent,
        actions: Map<String, PendingIntent>
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for smart follow-ups
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_SMART_FOLLOW_UP,
                CHANNEL_NAME_SMART_FOLLOW_UP,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Smart follow-up notifications with AI suggestions"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID_SMART_FOLLOW_UP)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Create this icon
            .setContentTitle("🧠 Smart Follow-up: $cardName")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)

        // Add action buttons
        actions["SNOOZE_1D"]?.let { snooze1dIntent ->
            notificationBuilder.addAction(
                R.drawable.ic_snooze,
                "Snooze 1d",
                snooze1dIntent
            )
        }

        actions["SNOOZE_3D"]?.let { snooze3dIntent ->
            notificationBuilder.addAction(
                R.drawable.ic_snooze,
                "Snooze 3d",
                snooze3dIntent
            )
        }

        actions["DONE"]?.let { doneIntent ->
            notificationBuilder.addAction(
                R.drawable.ic_done,
                "Done",
                doneIntent
            )
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    // Add these constants to NotificationUtils
    private const val CHANNEL_ID_SMART_FOLLOW_UP = "smart_follow_up_channel"
    private const val CHANNEL_NAME_SMART_FOLLOW_UP = "Smart Follow-ups"
}

object NotificationIdGenerator {
    private var id = 1000 // Start from higher number to avoid conflicts
    fun getUniqueId(): Int = id++
// Add to NotificationIdGenerator object

    fun getSmartFollowUpId(cardId: Int): Int = 4000 + cardId
    // Specific ID generators for different notification types
    fun getFollowUpAlarmId(cardId: Int): Int = 2000 + cardId
    fun getDailyDigestId(): Int = 3000
    fun getStaleLeadsId(): Int = 3001
}