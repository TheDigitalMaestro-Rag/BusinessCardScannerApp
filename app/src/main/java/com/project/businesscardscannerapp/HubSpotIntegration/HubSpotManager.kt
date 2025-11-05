// HubSpotManager.kt - Updated version
package com.project.businesscardscannerapp.HubSpotIntegration

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HubSpotManager(private val context: Context) {
    private val secureStorage = SecureStorage(context)
    private val tag = "HubSpotManager"
    private val hubSpotService = HubSpotServiceManager()

    suspend fun testConnection(accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            hubSpotService.testConnection(accessToken)
        }
    }

    suspend fun saveConfiguration(accessToken: String): Boolean {
        return try {
            secureStorage.saveHubSpotAccessToken(accessToken)
            true
        } catch (e: Exception) {
            android.util.Log.e(tag, "Failed to save configuration: ${e.message}")
            false
        }
    }

    suspend fun isConfigured(): Boolean {
        return secureStorage.getHubSpotAccessToken() != null
    }

    suspend fun getAccessToken(): String? {
        return secureStorage.getHubSpotAccessToken()
    }

    suspend fun clearConfig() {
        secureStorage.clearHubSpotCredentials()
        android.util.Log.d(tag, "HubSpot configuration cleared")
    }
}