package com.project.businesscardscannerapp

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.project.businesscardscannerapp.Utils.NotificationUtils
import com.project.businesscardscannerapp.Utils.NotificationIdGenerator
import com.project.businesscardscannerapp.Workers.FollowUpActionReceiver
import com.project.businesscardscannerapp.ViewModel.NotificationConstants

class ReminderBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getIntExtra(NotificationConstants.REMINDER_CARD_ID, -1)
        val message = intent.getStringExtra(NotificationConstants.REMINDER_MESSAGE) ?: "Time to follow up!"
        val cardName = intent.getStringExtra("cardName") ?: "Business Card"

        if (cardId != -1) {
            // Create pending intent to open card details - this will be used when user clicks notification
            val mainIntent = Intent(context, MainActivity::class.java).apply {
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

            // Create action buttons
            val snooze1DayIntent = createSnoozePendingIntent(context, cardId, cardName, message, 1)
            val snooze3DaysIntent = createSnoozePendingIntent(context, cardId, cardName, message, 3)
            val doneIntent = createDonePendingIntent(context, cardId)

            // Use the enhanced alarm notification WITH ACTION BUTTONS
            NotificationUtils.showFollowUpAlarmNotificationWithActions(
                context = context,
                cardName = cardName,
                message = message,
                notificationId = NotificationIdGenerator.getFollowUpAlarmId(cardId),
                alarmSound = true,
                pendingIntent = pendingIntent, // User can click notification to navigate
                snooze1DayIntent = snooze1DayIntent,
                snooze3DaysIntent = snooze3DaysIntent,
                doneIntent = doneIntent
            )
        }
    }

    private fun createSnoozePendingIntent(context: Context, cardId: Int, cardName: String, message: String, days: Int): PendingIntent {
        val snoozeIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = FollowUpActionReceiver.ACTION_SNOOZE
            putExtra(NotificationConstants.REMINDER_CARD_ID, cardId)
            putExtra("snooze_days", days)
            putExtra("cardName", cardName)
            putExtra(NotificationConstants.REMINDER_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            generateSnoozeRequestCode(cardId, days),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDonePendingIntent(context: Context, cardId: Int): PendingIntent {
        val doneIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = FollowUpActionReceiver.ACTION_DONE
            putExtra(NotificationConstants.REMINDER_CARD_ID, cardId)
        }
        return PendingIntent.getBroadcast(
            context,
            generateDoneRequestCode(cardId),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun generateSnoozeRequestCode(cardId: Int, days: Int): Int {
        return cardId * 100 + days
    }

    private fun generateDoneRequestCode(cardId: Int): Int {
        return cardId * 100 + 99
    }
}