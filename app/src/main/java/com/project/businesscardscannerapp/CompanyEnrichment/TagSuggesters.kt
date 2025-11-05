package com.project.businesscardscannerapp.CompanyEnrichment

import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard


object TagSuggester {
    // Map keywords to tags
    private val tagRules = mapOf(
        "invest" to "Investor",
        "vc" to "Investor",
        "capital" to "Investor",
        "recruit" to "Recruiter",
        "talent" to "Recruiter",
        "hr" to "Recruiter",
        "hiring" to "Recruiter",
        "supply" to "Supplier",
        "logistic" to "Supplier",
        "material" to "Supplier",
        "client" to "Potential Client",
        "customer" to "Potential Client",
        "sale" to "Potential Client",
        "partner" to "Potential Partner",
        "collaborat" to "Potential Partner",
        "tech" to "Tech",
        "software" to "Tech",
        "developer" to "Tech",
        "engineer" to "Tech"
    )

    // Function to suggest tags for a card
    fun suggestTags(card: BusinessCard): List<String> {
        val suggestedTags = mutableSetOf<String>()

        // Create a combined text field to search in
        val textToSearch = listOfNotNull(
            card.company,
            card.position,
            card.industry,
            card.notes
        ).joinToString(" ").lowercase()

        // Check each rule
        for ((keyword, tag) in tagRules) {
            if (textToSearch.contains(keyword, ignoreCase = true)) {
                suggestedTags.add(tag)
            }
        }

        // Add tags based on lead score/category
        when (card.leadCategory.lowercase()) {
            "hot", "warm" -> suggestedTags.add("Priority")
            "cold" -> suggestedTags.add("Reconnect Later")
        }

        // Add industry-based tags if not already covered
        card.industry?.takeIf { it.isNotBlank() }?.let { industry ->
            if (industry.contains("tech", ignoreCase = true) ||
                industry.contains("software", ignoreCase = true)) {
                suggestedTags.add("Tech")
            }
        }

        return suggestedTags.toList().take(3) // Limit to 3 most relevant tags
    }
}