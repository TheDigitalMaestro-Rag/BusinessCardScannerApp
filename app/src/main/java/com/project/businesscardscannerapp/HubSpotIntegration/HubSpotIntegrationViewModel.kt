// HubSpotIntegrationViewModel.kt - FIXED version
package com.project.businesscardscannerapp.HubSpotIntegration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HubSpotTestResult(
    val success: Boolean,
    val error: String? = null
)

class HubSpotIntegrationViewModel(
    private val hubSpotManager: HubSpotManager,
    private val hubSpotService: HubSpotServiceManager
) : ViewModel() {

    private val _integrationState = MutableStateFlow<HubSpotIntegrationState>(HubSpotIntegrationState.Idle)
    val integrationState: StateFlow<HubSpotIntegrationState> = _integrationState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        checkConnectionStatus()
    }

    private fun checkConnectionStatus() {
        viewModelScope.launch {
            _isConnected.value = hubSpotManager.isConfigured()
        }
    }

    fun testConnection(onResult: (HubSpotTestResult) -> Unit) {
        viewModelScope.launch {
            _integrationState.value = HubSpotIntegrationState.Loading("Testing connection...")

            try {
                val accessToken = hubSpotManager.getAccessToken()
                if (accessToken == null) {
                    onResult(HubSpotTestResult(false, "HubSpot not configured"))
                    _integrationState.value = HubSpotIntegrationState.Error("Please configure HubSpot first")
                    return@launch
                }

                val success = hubSpotService.testConnection(accessToken)
                if (success) {
                    onResult(HubSpotTestResult(true))
                    _integrationState.value = HubSpotIntegrationState.Success("Connection test successful!")
                    _isConnected.value = true
                } else {
                    onResult(HubSpotTestResult(false, "Failed to connect to HubSpot"))
                    _integrationState.value = HubSpotIntegrationState.Error("Connection test failed")
                }
            } catch (e: Exception) {
                onResult(HubSpotTestResult(false, e.message ?: "Unknown error"))
                _integrationState.value = HubSpotIntegrationState.Error("Connection test failed: ${e.message}")
            }
        }
    }

    fun configureHubSpot(accessToken: String) {
        viewModelScope.launch {
            _integrationState.value = HubSpotIntegrationState.Loading("Configuring HubSpot...")

            try {
                // Test the token first
                val isValid = hubSpotService.testConnection(accessToken)

                if (isValid) {
                    val success = hubSpotManager.saveConfiguration(accessToken)
                    if (success) {
                        _isConnected.value = true
                        _integrationState.value = HubSpotIntegrationState.Success("HubSpot configured successfully!")
                    } else {
                        _integrationState.value = HubSpotIntegrationState.Error("Failed to save configuration")
                    }
                } else {
                    _integrationState.value = HubSpotIntegrationState.Error("Invalid access token. Please check your token and try again.")
                }
            } catch (e: Exception) {
                _integrationState.value = HubSpotIntegrationState.Error("Configuration failed: ${e.message}")
            }
        }
    }

    fun saveToHubSpot(businessCard: BusinessCard) {
        viewModelScope.launch {
            _integrationState.value = HubSpotIntegrationState.Loading("Saving to HubSpot...")

            try {
                val accessToken = hubSpotManager.getAccessToken()
                if (accessToken == null) {
                    _integrationState.value = HubSpotIntegrationState.Error("HubSpot not configured")
                    return@launch
                }

                // Check for duplicate by email
                if (!businessCard.email.isNullOrBlank()) {
                    val existingContact = hubSpotService.searchContactByEmail(businessCard.email, accessToken)
                    if (existingContact != null) {
                        _integrationState.value = HubSpotIntegrationState.DuplicateFound(existingContact)
                        return@launch
                    }
                }

                // Create new contact
                val success = hubSpotService.createContact(businessCard, accessToken)

                if (success) {
                    _integrationState.value = HubSpotIntegrationState.Success("Contact saved to HubSpot!")
                } else {
                    _integrationState.value = HubSpotIntegrationState.Error("Failed to save contact to HubSpot")
                }

            } catch (e: Exception) {
                _integrationState.value = HubSpotIntegrationState.Error("Error: ${e.message ?: "Unknown error"}")
            }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            hubSpotManager.clearConfig()
            _isConnected.value = false
            _integrationState.value = HubSpotIntegrationState.Idle
        }
    }

    fun resetState() {
        _integrationState.value = HubSpotIntegrationState.Idle
    }
}

sealed class HubSpotIntegrationState {
    object Idle : HubSpotIntegrationState()
    data class Loading(val message: String) : HubSpotIntegrationState()
    data class Success(val message: String) : HubSpotIntegrationState()
    data class Error(val message: String) : HubSpotIntegrationState()
    data class DuplicateFound(val contactId: String) : HubSpotIntegrationState()
}