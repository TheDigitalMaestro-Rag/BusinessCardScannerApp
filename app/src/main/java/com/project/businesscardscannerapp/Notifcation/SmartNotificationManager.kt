// SmartNotificationManager.kt
package com.project.businesscardscannerapp.Notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.businesscardscannerapp.R
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.Entity.PipelineStage
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.Utils.NotificationUtils
import com.project.businesscardscannerapp.Utils.NotificationIdGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.concurrent.TimeUnit

class SmartNotificationManager(private val context: Context) {

    suspend fun showStaleLeadsNotification() {
        val database = AppDatabase.getDatabase(context)
        val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

        val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)

        val staleNegotiationLeads = withContext(Dispatchers.IO) {
            repository.getCardsByPipelineStage(PipelineStage.NEGOTIATION)
                .first()
                .filter { card ->
                    card.lastInteractionAt ?: 0L < sevenDaysAgo
                }
        }

        if (staleNegotiationLeads.isNotEmpty()) {
            NotificationUtils.showSmartNotification(
                context = context,
                title = "🚨 Stale Leads Alert",
                message = "You have ${staleNegotiationLeads.size} leads in Negotiation with no activity in 7 days. Review these leads: ${staleNegotiationLeads.joinToString { it.name }}",
                notificationId = NotificationIdGenerator.getStaleLeadsId(),
                priority = NotificationCompat.PRIORITY_HIGH
            )
        }
    }

    suspend fun showDailyDigestNotification() {
        val database = AppDatabase.getDatabase(context)
        val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

        val yesterday = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
        val now = System.currentTimeMillis()

        val (newLeads, missedFollowUps) = withContext(Dispatchers.IO) {
            val newLeadsCount = repository.allCards.first().count { it.createdAt > yesterday }
            val missedFollowUpsCount = repository.getOverdueFollowUpsCount(now)
            Pair(newLeadsCount, missedFollowUpsCount)
        }

        if (newLeads > 0 || missedFollowUps > 0) {
            NotificationUtils.showSmartNotification(
                context = context,
                title = "📊 Daily Business Card Digest",
                message = "📊 Daily Update:\n• $newLeads new leads added\n• $missedFollowUps follow-ups need attention",
                notificationId = NotificationIdGenerator.getDailyDigestId(),
                priority = NotificationCompat.PRIORITY_DEFAULT
            )
        }
    }
}

// Worker for scheduled notifications
class DailyDigestWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = SmartNotificationManager(applicationContext)
        notificationManager.showDailyDigestNotification()
        return Result.success()
    }
}

class StaleLeadsWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val notificationManager = SmartNotificationManager(applicationContext)
        notificationManager.showStaleLeadsNotification()
        return Result.success()
    }
}