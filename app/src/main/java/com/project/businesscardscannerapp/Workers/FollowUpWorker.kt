package com.project.businesscardscannerapp.Workers

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.businesscardscannerapp.MainActivity
import com.project.businesscardscannerapp.R
import com.project.businesscardscannerapp.Utils.NotificationUtils
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.Utils.NotificationIdGenerator
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FollowUpWorker(
    appCtx: Context,
    params: WorkerParameters
) : CoroutineWorker(appCtx, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val cardId = inputData.getInt(REMINDER_CARD_ID, -1)
        val cardName = inputData.getString("cardName") ?: "Contact"
        val message = inputData.getString(REMINDER_MESSAGE) ?: "Time to follow up!"

        if (cardId == -1) {
            return Result.failure()
        }

        // Create a pending intent to open the DetailsActivity for this card
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra("navigateToDetails", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            cardId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Create snooze and done actions for normal follow-up
        val snooze1DayIntent = createSnoozePendingIntent(applicationContext, cardId, cardName, message, 1)
        val snooze3DaysIntent = createSnoozePendingIntent(applicationContext, cardId, cardName, message, 3)
        val doneIntent = createDonePendingIntent(applicationContext, cardId)

        // Show loud alarm notification WITH ACTION BUTTONS
        NotificationUtils.showFollowUpAlarmNotificationWithActions(
            context = applicationContext,
            cardName = cardName,
            message = message,
            notificationId = NotificationIdGenerator.getFollowUpAlarmId(cardId),
            alarmSound = true,
            pendingIntent = pendingIntent,
            snooze1DayIntent = snooze1DayIntent,
            snooze3DaysIntent = snooze3DaysIntent,
            doneIntent = doneIntent
        )

        // Get the database and update follow-up status
        val database = AppDatabase.getDatabase(applicationContext)
        val businessCardDao = database.businessCardDao()
        val insightsDao = database.insightsDao()
        val repository = BusinessCardRepository(businessCardDao, insightsDao)

        withContext(Dispatchers.IO) {
            repository.getLastFollowUpForCard(cardId)?.let { followUp ->
                if (followUp.status == "scheduled") {
                    repository.updateFollowUp(followUp.copy(status = "triggered"))
                }
            }
        }

        return Result.success()
    }

    private fun createSnoozePendingIntent(context: Context, cardId: Int, cardName: String, message: String, days: Int): PendingIntent {
        val snoozeIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE"
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra("snooze_days", days)
            putExtra("cardName", cardName)
            putExtra(REMINDER_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            cardId + 10000 + days, // Unique request code for each snooze option
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDonePendingIntent(context: Context, cardId: Int): PendingIntent {
        val doneIntent = Intent(context, FollowUpActionReceiver::class.java).apply {
            action = "ACTION_DONE"
            putExtra(REMINDER_CARD_ID, cardId)
        }
        return PendingIntent.getBroadcast(
            context,
            cardId + 20000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}