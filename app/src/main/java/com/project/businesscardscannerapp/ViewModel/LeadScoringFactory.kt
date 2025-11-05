package com.project.businesscardscannerapp.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.businesscardscannerapp.RoomDB.DataBase.AppDatabase
import com.project.businesscardscannerapp.RoomDB.ProvideDB.BusinessCardRepository

// In LeadScoringViewModelFactory:
class LeadScoringViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeadScoringViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = BusinessCardRepository(database.businessCardDao(), database.insightsDao()) // Pass insightsDao
            @Suppress("UNCHECKED_CAST")
            return LeadScoringViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}