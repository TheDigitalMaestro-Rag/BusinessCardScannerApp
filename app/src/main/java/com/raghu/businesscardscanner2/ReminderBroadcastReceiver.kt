// FileName: ReminderBroadcastReceiver.kt
package com.raghu.businesscardscanner2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.raghu.businesscardscanner.MainActivity
import com.raghu.businesscardscanner.R

// Constants for Intent extras and Notification IDs
const val REMINDER_CARD_ID = "reminder_card_id"
const val REMINDER_MESSAGE = "reminder_message"
const val REMINDER_NOTIFICATION_ID = "reminder_notification_id"
const val REMINDER_REQUEST_CODE_BASE = 1000 // Base for unique PendingIntent request codes
const val CHANNEL_ID = "business_card_reminder_channel"
const val CHANNEL_NAME = "Business Card Reminders"
const val CHANNEL_DESCRIPTION = "Notifications for business card follow-ups"

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getIntExtra(REMINDER_CARD_ID, -1)
        val message = intent.getStringExtra(REMINDER_MESSAGE) ?: "Time to follow up!"
        val notificationId = intent.getIntExtra(REMINDER_NOTIFICATION_ID, cardId)
        val cardName = intent.getStringExtra("cardName") ?: "Business Card"

        if (cardId != -1) {
            createNotificationChannel(context)
            showNotification(context, cardId, cardName, message, notificationId)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(
        context: Context,
        cardId: Int,
        cardName: String,
        message: String,
        notificationId: Int
    ) {
        // Create an explicit intent for an Activity in your app
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(REMINDER_CARD_ID, cardId) // Pass card ID to the activity
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            cardId, // Use cardId as request code for unique pending intent
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's icon
            .setContentTitle("Follow-up with $cardName")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent) // Set the intent that will fire when the user taps the notification
            .setAutoCancel(true) // Automatically removes the notification when the user taps it

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, builder.build())
    }
}
