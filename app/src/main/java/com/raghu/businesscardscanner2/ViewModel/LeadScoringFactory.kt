package com.raghu.businesscardscanner2.ViewModel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.raghu.businesscardscanner2.RoomDB.DataBase.AppDatabase
import com.raghu.businesscardscanner2.RoomDB.ProvideDB.BusinessCardRepository

// Create a new file LeadScoringViewModelFactory.kt
class LeadScoringViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LeadScoringViewModel::class.java)) {
            val database = AppDatabase.getDatabase(context)
            val repository = BusinessCardRepository(database.businessCardDao())
            @Suppress("UNCHECKED_CAST")
            return LeadScoringViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}