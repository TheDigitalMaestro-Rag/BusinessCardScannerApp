package com.project.businesscardscannerapp.AI

data class Insights(
    val scannedThisMonth: Int,
    val scannedChangePct: Int,
    val topIndustries: List<String>,
    val bestHour: Int?, // 0..23
    val overdue: Int, // Changed from Flow<List<FollowUp>> to Int
    val bucketShare: Map<String, Int>
)

object InsightsGenerator {
    fun insightsToText(i: Insights): List<String> {
        val tips = mutableListOf<String>()
        tips += "You scanned ${i.scannedThisMonth} cards this month (${if (i.scannedChangePct >= 0) "+" else ""}${i.scannedChangePct}% vs last month)."
        if (i.topIndustries.isNotEmpty())
            tips += "Most scans are in ${i.topIndustries.joinToString(", ")}."
        if (i.bestHour != null)
            tips += "Best response hour: ${String.format("%02d:00", i.bestHour)}–${String.format("%02d:00", (i.bestHour + 1) % 24)}."
        if (i.overdue > 0)  // Now this works
            tips += "${i.overdue} follow-ups are overdue. Consider a batch catch-up."
        val hot = i.bucketShare["Hot Lead"] ?: 0
        if (hot > 0) tips += "Prioritize Hot leads first; they convert faster—schedule them within 24 hours."
        return tips
    }
}