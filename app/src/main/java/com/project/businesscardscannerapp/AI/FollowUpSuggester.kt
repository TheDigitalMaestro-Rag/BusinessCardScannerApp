package com.project.businesscardscannerapp.AI

import android.os.Build
import androidx.annotation.RequiresApi
import com.project.businesscardscannerapp.RoomDB.Entity.BanditArm
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.TemporalAdjusters
import kotlin.math.min

object FollowUpSuggester {

    @RequiresApi(Build.VERSION_CODES.O)
    fun suggestFollowup(
        card: BusinessCard,
        banditArms: List<BanditArm>,
        userTz: ZoneId,
        now: ZonedDateTime = ZonedDateTime.now(userTz)
    ): ZonedDateTime {

        // Determine contact's timezone (simplified: assume user's timezone for now, or derive from countryCode)
        // For a real app, you'd map countryCode to a ZoneId.
        val contactTz = userTz // Placeholder

        // 1) Base days by lead bucket
        var days = when (card.leadCategory) {
            "Hot Lead" -> 1
            "Warm Lead" -> 3
            "Cool Lead" -> 7
            "Cold Lead" -> 14
            else -> 10 // Default for "Poor Lead" or "Unknown"
        }

        // 2) Role/industry accelerators
        val accelRoles = listOf("Founder", "CEO", "CTO", "HR", "Recruit", "Sales", "Head")
        if (card.position?.let { pos -> accelRoles.any { pos.contains(it, true) } } == true) {
            days = min(days - 1, 1) // Ensure days doesn't go below 1
        }
        if (card.industry?.let { ind -> ind.contains("IT", true) || ind.contains("Startup", true) } == true) {
            days = min(days - 1, 1) // Ensure days doesn't go below 1
        }
        if (days < 1) days = 1

        // 3) Bandit: pick the arm with best success rate most of the time
        val arms = listOf("1d", "3d", "7d", "14d") // Corresponding arm IDs
        val epsilon = 0.15 // Exploration rate
        val chosenArmId: String = if (Math.random() < epsilon) {
            arms.random() // Explore
        } else {
            // Exploit: choose the arm with the highest success rate
            banditArms.maxByOrNull { b ->
                if (b.tries == 0) 0.5 else b.successes.toDouble() / b.tries
            }?.armId ?: "${days}d" // Fallback to rule-based days if no bandit data
        }

        val chosenDays = chosenArmId.replace("d", "").toIntOrNull() ?: days
        days = min(days, chosenDays) // Use the minimum of rule-based and bandit-chosen days

        // 4) Schedule at contact’s morning window (9–11 AM contact TZ)
        var candidate = now.withZoneSameInstant(contactTz).plusDays(days.toLong())
            .withHour(9).withMinute(30).withSecond(0).withNano(0)

        // If weekend → next Monday 9:30
        if (candidate.dayOfWeek == DayOfWeek.SATURDAY || candidate.dayOfWeek == DayOfWeek.SUNDAY) {
            candidate = candidate.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                .withHour(9).withMinute(30).withSecond(0).withNano(0)
        }

        // Return in USER TZ to store/schedule
        return candidate.withZoneSameInstant(userTz)
    }
}
