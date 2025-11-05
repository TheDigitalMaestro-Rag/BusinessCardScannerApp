package com.project.businesscardscannerapp.Workers

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.businesscardscannerapp.MainActivity
import com.project.businesscardscannerapp.R
import com.project.businesscardscannerapp.AI.InsightsGenerator
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import com.project.businesscardscannerapp.Utils.NotificationUtils
import com.project.businesscardscannerapp.Utils.NotificationIdGenerator
import com.project.businesscardscannerapp.ViewModel.InsightsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InsightsWorker(
    appCtx: Context,
    params: WorkerParameters
) : CoroutineWorker(appCtx, params) {

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                val database = AppDatabase.getDatabase(applicationContext)
                val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao())

                // Create InsightsViewModel with Application context
                val insightsViewModel = InsightsViewModel(applicationContext.applicationContext as android.app.Application)

                // Load insights
                insightsViewModel.loadInsights()
                val insights = insightsViewModel.insights.value

                if (insights == null) {
                    return@withContext Result.retry()
                }

                val summaryText = InsightsGenerator.insightsToText(insights).joinToString("\n")

                // Create intent for navigation
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    putExtra("navigateToInsights", true)
                }
                val pendingIntent: PendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Show smart notification using the enhanced NotificationUtils
                NotificationUtils.showSmartNotification(
                    context = applicationContext,
                    title = "📈 Weekly Business Card Insights",
                    message = summaryText,
                    notificationId = NotificationIdGenerator.getUniqueId(),
                    priority = NotificationCompat.PRIORITY_DEFAULT
                )

                Result.success()
            }
        } catch (e: Exception) {
            // Log the error for debugging
            android.util.Log.e("InsightsWorker", "Error generating insights notification", e)
            Result.failure()
        }
    }
}