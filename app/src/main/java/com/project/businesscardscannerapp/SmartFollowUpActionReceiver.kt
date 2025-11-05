package com.project.businesscardscannerapp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.ViewModel.NotificationConstants.REMINDER_MESSAGE
import com.project.businesscardscannerapp.Workers.SmartFollowUpWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class SmartFollowUpActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val cardId = intent.getIntExtra(REMINDER_CARD_ID, -1)
        if (cardId == -1) return

        val scope = CoroutineScope(Dispatchers.IO)
        val database = AppDatabase.getDatabase(context)
        val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

        when (intent.action) {
            "ACTION_SNOOZE_SMART" -> {
                val snoozeDays = intent.getIntExtra("snooze_days", 1)
                val cardName = intent.getStringExtra("cardName") ?: "Contact"
                val message = intent.getStringExtra(REMINDER_MESSAGE) ?: "Smart follow-up reminder"

                scope.launch {
                    repository.getLastFollowUpForCard(cardId)?.let { followUp ->
                        val newScheduledAt = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(snoozeDays.toLong())
                        repository.updateFollowUp(followUp.copy(scheduledAt = newScheduledAt, status = "snoozed"))
                        scheduleSmartFollowupWork(context, cardId, cardName, message, newScheduledAt)
                        Toast.makeText(context, "Snoozed for $snoozeDays day(s)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "ACTION_DONE_SMART" -> {
                scope.launch {
                    repository.getLastFollowUpForCard(cardId)?.let { followUp ->
                        repository.updateFollowUp(followUp.copy(status = "done", completedAt = System.currentTimeMillis()))
                        Toast.makeText(context, "Follow-up marked as done", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Navigate to card details
        navigateToCardDetails(context, cardId)
    }

    private fun scheduleSmartFollowupWork(context: Context, cardId: Int, cardName: String, message: String, whenMillis: Long) {
        val delay = maxOf(0, whenMillis - System.currentTimeMillis())
        val req = OneTimeWorkRequestBuilder<SmartFollowUpWorker>()
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                REMINDER_CARD_ID to cardId,
                "cardName" to cardName,
                REMINDER_MESSAGE to message
            ))
            .addTag("smart_followup_$cardId")
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "smart_followup_$cardId",
            ExistingWorkPolicy.REPLACE,
            req
        )
    }

    private fun navigateToCardDetails(context: Context, cardId: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra("navigateToDetails", true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)
    }
}