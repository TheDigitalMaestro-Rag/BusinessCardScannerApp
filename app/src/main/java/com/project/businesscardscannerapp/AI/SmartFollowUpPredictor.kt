// SmartFollowUpPredictor.kt
package com.project.businesscardscannerapp.AI

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.RequiresApi
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.smartreply.SmartReply
import com.google.mlkit.nl.smartreply.SmartReplySuggestionResult
import com.google.mlkit.nl.smartreply.TextMessage
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_CARD_ID
import com.project.businesscardscannerapp.NotificationConstants.REMINDER_MESSAGE
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.SmartFollowUpActionReceiver
import kotlinx.coroutines.tasks.await
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class SmartFollowUpPredictor {
    private val smartReplyGenerator = SmartReply.getClient()

    // Main prediction function
    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun predictOptimalFollowUp(
        card: BusinessCard,
        interactionHistory: List<Interaction> = emptyList(),
        recentMessages: List<String> = emptyList(),
        userPreferences: UserPreferences = UserPreferences()
    ): SmartFollowUpPrediction {

        // Step 1: Extract ML Features
        val features = extractMLFeatures(card, interactionHistory, recentMessages)

        // Step 2: Calculate urgency from messages using ML Kit
        val urgencyScore = calculateUrgencyFromMessages(recentMessages)

        // Step 3: Generate smart message suggestions
        val smartMessage = generateSmartMessage(card, recentMessages)

        // Step 4: Determine optimal timing
        val optimalTime = calculateOptimalTime(features, urgencyScore, userPreferences)

        // Step 5: Calculate confidence
        val confidence = calculateConfidence(features, urgencyScore, interactionHistory.size)

        return SmartFollowUpPrediction(
            optimalTime = optimalTime,
            confidence = confidence,
            urgencyScore = urgencyScore,
            recommendedAction = determineRecommendedAction(features, urgencyScore),
            smartMessage = smartMessage,
            priorityLevel = determinePriorityLevel(features, urgencyScore),
            features = features
        )
    }

    private fun extractMLFeatures(
        card: BusinessCard,
        interactions: List<Interaction>,
        messages: List<String>
    ): MLFeatures {
        return MLFeatures(
            leadScore = calculateLeadScore(card),
            responsiveness = calculateResponsiveness(interactions),
            engagementLevel = calculateEngagementLevel(interactions, messages),
            timeSensitivity = calculateTimeSensitivity(card.industry),
            relationshipStrength = calculateRelationshipStrength(interactions)
        )
    }

    // Add these methods to SmartFollowUpPredictor class - FIXED VERSION
    fun getNotificationActions(context: Context, cardId: Int, cardName: String, smartMessage: String): Map<String, PendingIntent> {
        return createNotificationActions(context, cardId, cardName, smartMessage)
    }

    private fun createNotificationActions(
        context: Context,
        cardId: Int,
        cardName: String,
        smartMessage: String
    ): Map<String, PendingIntent> {
        val actions = mutableMapOf<String, PendingIntent>()

        // Snooze for 1 day
        actions["SNOOZE_1D"] = createSnoozePendingIntent(context, cardId, cardName, smartMessage, 1)

        // Snooze for 3 days
        actions["SNOOZE_3D"] = createSnoozePendingIntent(context, cardId, cardName, smartMessage, 3)

        // Mark as done
        actions["DONE"] = createDonePendingIntent(context, cardId)

        return actions
    }

    private fun createSnoozePendingIntent(
        context: Context,
        cardId: Int,
        cardName: String,
        message: String,
        days: Int
    ): PendingIntent {
        val snoozeIntent = Intent(context, SmartFollowUpActionReceiver::class.java).apply {
            action = "ACTION_SNOOZE_SMART"
            putExtra(REMINDER_CARD_ID, cardId)
            putExtra("snooze_days", days)
            putExtra("cardName", cardName)
            putExtra(REMINDER_MESSAGE, message)
        }
        return PendingIntent.getBroadcast(
            context,
            cardId + 30000 + days, // Unique request code
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun createDonePendingIntent(context: Context, cardId: Int): PendingIntent {
        val doneIntent = Intent(context, SmartFollowUpActionReceiver::class.java).apply {
            action = "ACTION_DONE_SMART"
            putExtra(REMINDER_CARD_ID, cardId)
        }
        return PendingIntent.getBroadcast(
            context,
            cardId + 40000,
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun calculateLeadScore(card: BusinessCard): Float {
        var score = 0f

        // Role-based scoring
        val roleWeights = mapOf(
            "ceo" to 0.9f, "founder" to 0.9f, "cto" to 0.8f,
            "director" to 0.7f, "manager" to 0.6f, "head" to 0.7f
        )

        card.position?.lowercase()?.let { position ->
            roleWeights.entries.find { (role, _) -> position.contains(role) }?.let {
                score += it.value * 0.6f // 60% weight to role
            }
        }

        // Industry-based scoring
        val industryWeights = mapOf(
            "technology" to 0.8f, "startup" to 0.7f, "it" to 0.7f,
            "finance" to 0.8f, "healthcare" to 0.6f, "consulting" to 0.6f
        )

        card.industry?.lowercase()?.let { industry ->
            industryWeights.entries.find { (ind, _) -> industry.contains(ind) }?.let {
                score += it.value * 0.4f // 40% weight to industry
            }
        }

        return score.coerceIn(0f, 1f)
    }

    private suspend fun calculateUrgencyFromMessages(messages: List<String>): Float {
        if (messages.isEmpty()) return 0.5f // Default medium urgency

        var totalUrgency = 0f
        var analyzedMessages = 0

        // Analyze each message for urgency indicators
        messages.forEach { message ->
            val urgencyWords = listOf("urgent", "asap", "deadline", "meeting", "tomorrow",
                "today", "important", "follow up", "discuss")
            val urgencyCount = urgencyWords.count { message.lowercase().contains(it) }

            // Use ML Kit to get smart replies as additional signal
            val conversation = listOf(
                TextMessage.createForLocalUser(message, System.currentTimeMillis())
            )

            try {
                val smartReplyResult = smartReplyGenerator.suggestReplies(conversation).await()
                if (smartReplyResult.status == SmartReplySuggestionResult.STATUS_SUCCESS) {
                    // If ML Kit suggests urgent responses, increase urgency
                    val suggestionTexts = smartReplyResult.suggestions.joinToString(" ") { it.text.lowercase() }
                    val mlUrgencyCount = urgencyWords.count { suggestionTexts.contains(it) }

                    totalUrgency += (urgencyCount + mlUrgencyCount) * 0.1f
                    analyzedMessages++
                }
            } catch (e: Exception) {
                // Fallback to keyword analysis
                totalUrgency += urgencyCount * 0.1f
                analyzedMessages++
            }
        }

        return if (analyzedMessages > 0) {
            (totalUrgency / analyzedMessages).coerceIn(0f, 1f)
        } else {
            0.5f
        }
    }

    private suspend fun generateSmartMessage(card: BusinessCard, recentMessages: List<String>): String {
        // Create conversation context for ML Kit
        val conversation = recentMessages.takeLast(3).mapIndexed { index, message ->
            TextMessage.createForLocalUser(
                message,
                System.currentTimeMillis() - (index * 60000L)
            )
        }

        return try {
            if (conversation.isNotEmpty()) {
                val smartReplyResult = smartReplyGenerator.suggestReplies(conversation).await()
                if (smartReplyResult.status == SmartReplySuggestionResult.STATUS_SUCCESS &&
                    smartReplyResult.suggestions.isNotEmpty()) {
                    // Use the most relevant smart reply
                    smartReplyResult.suggestions.first().text
                } else {
                    generateContextualMessage(card, recentMessages)
                }
            } else {
                generateContextualMessage(card, recentMessages)
            }
        } catch (e: Exception) {
            generateContextualMessage(card, recentMessages)
        }
    }

    private fun generateContextualMessage(card: BusinessCard, recentMessages: List<String>): String {
        val name = card.name ?: "there"
        val company = card.company?.let { " at $it" } ?: ""

        return when {
            recentMessages.isNotEmpty() -> "Hi $name, following up on our recent conversation$company."
            card.industry?.contains("tech", true) == true -> "Hi $name, hope you're doing well! Wanted to connect about potential collaboration$company."
            else -> "Hi $name, just following up on our connection$company. Hope you're doing well!"
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateOptimalTime(
        features: MLFeatures,
        urgencyScore: Float,
        preferences: UserPreferences
    ): ZonedDateTime {
        val baseTime = ZonedDateTime.now()
        val workingHoursStart = preferences.workingHoursStart ?: 9
        val workingHoursEnd = preferences.workingHoursEnd ?: 17

        // Calculate base timing based on ML features
        val baseDays = when {
            features.leadScore > 0.8f && urgencyScore > 0.7f -> 1 // Hot & urgent: 1 day
            features.leadScore > 0.8f -> 2 // Hot lead: 2 days
            urgencyScore > 0.7f -> 1 // Urgent: 1 day
            features.leadScore > 0.6f -> 3 // Warm lead: 3 days
            features.responsiveness > 0.7f -> 2 // Responsive contact: 2 days
            else -> 5 // Normal: 5 days
        }

        // Adjust for weekends and working hours
        var followUpTime = baseTime.plusDays(baseDays.toLong())
            .withHour(workingHoursStart + 1) // 1 hour after work start
            .withMinute(0)
            .withSecond(0)
            .withNano(0)

        // Avoid weekends
        if (followUpTime.dayOfWeek.value in 6..7) { // Saturday or Sunday
            followUpTime = followUpTime.with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY))
                .withHour(workingHoursStart + 1)
                .withMinute(0)
        }

        return followUpTime
    }

    private fun calculateConfidence(
        features: MLFeatures,
        urgencyScore: Float,
        interactionCount: Int
    ): Float {
        var confidence = 0.5f // Base confidence

        // Increase confidence with more data
        if (interactionCount > 0) confidence += 0.2f
        if (interactionCount > 2) confidence += 0.1f

        // High lead score increases confidence
        confidence += features.leadScore * 0.2f

        // Clear urgency signals increase confidence
        if (urgencyScore > 0.7f || urgencyScore < 0.3f) confidence += 0.1f

        return confidence.coerceIn(0.1f, 0.95f)
    }

    private fun determineRecommendedAction(features: MLFeatures, urgencyScore: Float): String {
        return when {
            urgencyScore > 0.8f -> "Call immediately"
            features.leadScore > 0.8f -> "Schedule meeting"
            features.responsiveness > 0.7f -> "Send detailed email"
            else -> "Send follow-up message"
        }
    }

    private fun determinePriorityLevel(features: MLFeatures, urgencyScore: Float): Priority {
        val priorityScore = (features.leadScore * 0.6f) + (urgencyScore * 0.4f)

        return when {
            priorityScore > 0.8f -> Priority.HIGH
            priorityScore > 0.6f -> Priority.MEDIUM
            else -> Priority.LOW
        }
    }

    // Helper calculations
    private fun calculateResponsiveness(interactions: List<Interaction>): Float {
        if (interactions.isEmpty()) return 0.5f
        val respondedInteractions = interactions.count { it.wasResponded }
        return respondedInteractions.toFloat() / interactions.size
    }

    private fun calculateEngagementLevel(interactions: List<Interaction>, messages: List<String>): Float {
        val interactionScore = interactions.size * 0.1f
        val messageScore = messages.size * 0.05f
        return (interactionScore + messageScore).coerceIn(0f, 1f)
    }

    private fun calculateTimeSensitivity(industry: String?): Float {
        return when {
            industry?.contains("tech", true) == true -> 0.8f
            industry?.contains("finance", true) == true -> 0.7f
            industry?.contains("health", true) == true -> 0.6f
            else -> 0.5f
        }
    }

    private fun calculateRelationshipStrength(interactions: List<Interaction>): Float {
        if (interactions.isEmpty()) return 0.3f
        return (interactions.size * 0.1f).coerceIn(0.3f, 0.9f)
    }
}

// Data Classes
data class SmartFollowUpPrediction(
    val optimalTime: ZonedDateTime,
    val confidence: Float,
    val urgencyScore: Float,
    val recommendedAction: String,
    val smartMessage: String,
    val priorityLevel: Priority,
    val features: MLFeatures
)

data class MLFeatures(
    val leadScore: Float,
    val responsiveness: Float,
    val engagementLevel: Float,
    val timeSensitivity: Float,
    val relationshipStrength: Float
)

data class UserPreferences(
    val workingHoursStart: Int? = null,
    val workingHoursEnd: Int? = null,
    val preferredContactMethods: List<String> = emptyList()
)

enum class Priority { LOW, MEDIUM, HIGH }

data class Interaction(
    val timestamp: ZonedDateTime,
    val type: String, // "call", "email", "message"
    val wasResponded: Boolean,
    val durationMinutes: Int? = null
)