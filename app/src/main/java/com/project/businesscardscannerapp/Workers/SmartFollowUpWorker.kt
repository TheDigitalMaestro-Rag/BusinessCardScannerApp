package com.project.businesscardscannerapp.Workers

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.businesscardscannerapp.MainActivity
import com.project.businesscardscannerapp.AI.SmartFollowUpPredictor
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.Utils.NotificationUtils
import com.project.businesscardscannerapp.Utils.NotificationIdGenerator
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_MESSAGE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SmartFollowUpWorker(
    appCtx: Context,
    params: WorkerParameters
) : CoroutineWorker(appCtx, params) {

    private val smartPredictor = SmartFollowUpPredictor()

    override suspend fun doWork(): Result {
        val cardId = inputData.getInt(REMINDER_CARD_ID, -1)
        val cardName = inputData.getString("cardName") ?: "Contact"
        val message = inputData.getString(REMINDER_MESSAGE) ?: "Smart follow-up reminder"

        if (cardId == -1) {
            return Result.failure()
        }

        // Get notification actions
        // In SmartFollowUpWorker.kt - update this line:
        val notificationActions = smartPredictor.getNotificationActions(applicationContext, cardId, cardName, message)
        // Create pending intent to open card details
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra("navigateToDetails", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            cardId + 50000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show smart follow-up notification with actions
        NotificationUtils.showSmartFollowUpNotification(
            context = applicationContext,
            cardName = cardName,
            message = message,
            notificationId = NotificationIdGenerator.getSmartFollowUpId(cardId),
            pendingIntent = pendingIntent,
            actions = notificationActions
        )

        // Update follow-up status in database
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

        withContext(Dispatchers.IO) {
            repository.getLastFollowUpForCard(cardId)?.let { followUp ->
                if (followUp.status == "scheduled") {
                    repository.updateFollowUp(followUp.copy(status = "triggered"))
                }
            }
        }

        return Result.success()
    }
}