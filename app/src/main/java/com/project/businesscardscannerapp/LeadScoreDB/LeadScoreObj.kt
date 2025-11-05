package com.project.businesscardscannerapp.LeadScoreDB

import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard

// Create a new file LeadScorer.kt
object LeadScorer {

    // Weights for different scoring factors
    private const val COMPANY_WEIGHT = 3
    private const val JOB_TITLE_WEIGHT = 4
    private const val INDUSTRY_WEIGHT = 3
    private const val CONTACT_INFO_WEIGHT = 2
    private const val WEBSITE_WEIGHT = 2
    private const val RECENCY_WEIGHT = 1

    // Score a business card
    fun scoreLead(card: BusinessCard): BusinessCard {
        var score = 0

        // 1. Company scoring
        score += scoreCompany(card.company) * COMPANY_WEIGHT

        // 2. Job title scoring
        score += scoreJobTitle(card.position) * JOB_TITLE_WEIGHT

        // 3. Industry scoring
        score += scoreIndustry(card.industry) * INDUSTRY_WEIGHT

        // 4. Contact info completeness
        score += scoreContactInfo(card) * CONTACT_INFO_WEIGHT

        // 5. Website quality
        score += scoreWebsite(card.website) * WEBSITE_WEIGHT

        // 6. Recency (if we have last contact date)
        score += scoreRecency(card.lastContactDate ?: card.lastFollowUpDate) * RECENCY_WEIGHT

        val category = getLeadCategory(score)

        return card.copy(
            leadScore = score.coerceIn(0, 100),
            leadCategory = category
        )
    }

    private fun scoreCompany(company: String): Int {
        return when {
            company.isBlank() -> 0
            company.length > 30 -> 3 // Likely a well-established company
            company.length > 15 -> 2
            else -> 1
        }
    }

    private fun scoreJobTitle(title: String): Int {
        return when {
            title.isBlank() -> 0
            title.contains("CEO", true) ||
                    title.contains("Chief", true) ||
                    title.contains("President", true) -> 5
            title.contains("VP", true) ||
                    title.contains("Vice President", true) -> 4
            title.contains("Director", true) -> 3
            title.contains("Manager", true) -> 2
            else -> 1
        }
    }

    // In LeadScorer.kt
    private fun scoreIndustry(industry: String): Int {
        return when (industry.lowercase().trim()) {
            "technology", "it", "software" -> 5
            "finance", "banking", "insurance" -> 4
            "healthcare", "medical" -> 4
            "education", "university" -> 3
            "manufacturing", "production" -> 3
            "retail", "ecommerce" -> 2
            "" -> 0 // Handle empty industry
            else -> 1 // Default for unknown industries
        }
    }

    private fun scoreContactInfo(card: BusinessCard): Int {
        var points = 0

        // Email
        if (card.email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(card.email).matches()) {
            points += 2
        }

        // Phone
        if (card.phones.isNotEmpty() && card.phones.any { it.length >= 10 }) {
            points += 2
        }

        // Address
        if (card.address.isNotBlank()) {
            points += 1
        }

        // Normalize to 0-5 scale
        return points.coerceAtMost(5)
    }

    private fun scoreWebsite(website: String): Int {
        return when {
            website.isBlank() -> 0
            !website.startsWith("http") -> 1 // Probably incomplete
            website.contains(".com", true) -> 3
            website.contains(".org", true) ||
                    website.contains(".net", true) -> 2
            else -> 1
        }
    }

    private fun scoreRecency(lastContactDate: Long?): Int {
        if (lastContactDate == null) return 0

        val daysSinceContact = (System.currentTimeMillis() - lastContactDate) / (1000 * 60 * 60 * 24)

        return when {
            daysSinceContact < 7 -> 5   // Contacted within a week
            daysSinceContact < 30 -> 4  // Contacted within a month
            daysSinceContact < 90 -> 3  // Contacted within 3 months
            daysSinceContact < 180 -> 2 // Contacted within 6 months
            daysSinceContact < 365 -> 1 // Contacted within a year
            else -> 0                   // Over a year or never
        }
    }

    // Categorize the lead based on score
    // Update getLeadCategory to handle 0 score
    fun getLeadCategory(score: Int): String {
        return when {
            score == 0 -> "Not Scored"
            score >= 80 -> "Hot Lead"
            score >= 60 -> "Warm Lead"
            score >= 40 -> "Cool Lead"
            score >= 20 -> "Cold Lead"
            else -> "Poor Lead"
        }
    }
}