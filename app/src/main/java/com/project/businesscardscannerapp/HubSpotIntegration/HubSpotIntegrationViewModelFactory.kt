package com.project.businesscardscannerapp.HubSpotIntegration

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// HubSpotIntegrationViewModelFactory.kt - Updated
class HubSpotIntegrationViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HubSpotIntegrationViewModel::class.java)) {
            val hubSpotManager = HubSpotManager(context)
            val hubSpotService = HubSpotServiceManager()
            @Suppress("UNCHECKED_CAST")
            return HubSpotIntegrationViewModel(hubSpotManager, hubSpotService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}