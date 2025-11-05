package com.project.businesscardscannerapp.ViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// Create a new file LeadScoringViewModel.kt
class LeadScoringViewModel(private val repository: BusinessCardRepository) : ViewModel() {

    init {
        viewModelScope.launch {
            repository.scoreAndUpdateAllLeads()
        }
    }


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