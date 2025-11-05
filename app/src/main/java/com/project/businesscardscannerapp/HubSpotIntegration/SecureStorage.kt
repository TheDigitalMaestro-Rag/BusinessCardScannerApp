package com.project.businesscardscannerapp.HubSpotIntegration

import android.content.Context
import android.content.pm.PackageManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class SecureStorage(private val context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        "notion_secure_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Add biometric protection for sensitive operations
    private fun hasBiometricAuth(): Boolean {
        val pm = context.packageManager
        return pm.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) ||
                pm.hasSystemFeature(PackageManager.FEATURE_FACE) ||
                pm.hasSystemFeature(PackageManager.FEATURE_IRIS)
    }

    private suspend fun authenticateWithBiometric(): Boolean {
        return try {
            // Implement biometric authentication
            true
        } catch (e: Exception) {
            false
        }
    }
    fun saveHubSpotAccessToken(accessToken: String) {
        encryptedPrefs.edit().putString("hubspot_access_token", accessToken).apply()
    }

    fun getHubSpotAccessToken(): String? {
        return encryptedPrefs.getString("hubspot_access_token", null)
    }

    fun clearHubSpotCredentials() {
        encryptedPrefs.edit().remove("hubspot_access_token").apply()
    }
}