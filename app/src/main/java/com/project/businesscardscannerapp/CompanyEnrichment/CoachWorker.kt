// File: CoachWorker.kt
package com.project.businesscardscannerapp.Workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.Utils.NotificationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CoachWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    private val db = AppDatabase.getDatabase(ctx)

    override suspend fun doWork(): Result {
        return try {
            withContext(Dispatchers.IO) {
                // Get insights from the database
                val bestHour = db.goalDao().bestHour()
                val bestDay = db.goalDao().bestDay()

                // Generate coaching message based on insights
                val message = generateCoachingMessage(bestDay, bestHour)

                // Show notification with coaching advice
                NotificationUtils.showCoachNotification(applicationContext, message)
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun generateCoachingMessage(bestDay: String?, bestHour: String?): String {
        return when {
            bestDay != null && bestHour != null -> {
                val dayName = getDayName(bestDay)
                "Your contacts are most responsive on $dayName around ${bestHour}:00. Schedule follow-ups then!"
            }
            bestDay != null -> {
                val dayName = getDayName(bestDay)
                "Your contacts are most responsive on $dayName. Plan your outreach for this day!"
            }
            bestHour != null -> {
                "Your contacts are most responsive around ${bestHour}:00. This is the best time to reach out!"
            }
            else -> {
                "Keep tracking your interactions to receive personalized coaching advice!"
            }
        }
    }

    private fun getDayName(dayNumber: String): String {
        return when (dayNumber) {
            "0" -> "Sunday"
            "1" -> "Monday"
            "2" -> "Tuesday"
            "3" -> "Wednesday"
            "4" -> "Thursday"
            "5" -> "Friday"
            "6" -> "Saturday"
            else -> "Unknown day"
        }
    }
}
// Rest of the file remains the same...
val tagRules = mapOf(
    "Investor" to listOf("vc", "capital", "angel", "investor"),
    "Recruiter" to listOf("hr", "talent", "recruit"),
    "Supplier" to listOf("vendor", "logistics", "supply"),
    "Client" to listOf("ceo", "cto", "founder", "manager")
)

fun suggestTags(title: String?, company: String?): List<String> {
    val input = "${title ?: ""} ${company ?: ""}".lowercase()
    return tagRules.filter { (_, keywords) -> keywords.any { input.contains(it) } }.keys.toList()
}

// Note: You'll need to implement the exportContactsToExcel function properly
// This requires adding Apache POI dependencies to your project