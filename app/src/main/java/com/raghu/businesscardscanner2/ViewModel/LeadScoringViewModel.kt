package com.raghu.businesscardscanner2.ViewModel

import androidx.lifecycle.ViewModel
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import com.raghu.businesscardscanner2.RoomDB.ProvideDB.BusinessCardRepository
import kotlinx.coroutines.flow.Flow

// Create a new file LeadScoringViewModel.kt
class LeadScoringViewModel(private val repository: BusinessCardRepository) : ViewModel() {

    val allLeads: Flow<List<BusinessCard>> = repository.getCardsSortedByLeadScore()

    fun getLeadsByCategory(category: String): Flow<List<BusinessCard>> {
        return repository.getCardsByLeadCategory(category)
    }

    suspend fun scoreAndUpdateLead(card: BusinessCard): BusinessCard {
        return repository.scoreAndUpdateLead(card)
    }

    suspend fun scoreAndUpdateAllLeads() {
        repository.scoreAndUpdateAllLeads()
    }

    fun getLeadCategories(): List<String> {
        return listOf("Hot Lead", "Warm Lead", "Cool Lead", "Cold Lead", "Poor Lead")
    }
}