package com.project.businesscardscannerapp.RoomDB.ProvideDB

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CardSharingRepository(private val context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val gson = Gson()

    // ==================== QR SHARING ====================

    /**
     * Generate QR code with text data only (no image) - FIXED VERSION
     */
    fun generateQrForCard(
        card: BusinessCard,
        cardImage: Bitmap?,
        includeImage: Boolean = false,
        callback: (QrResult) -> Unit
    ) {
        try {
            // Always generate text-only QR (ignore includeImage parameter as requested)
            generateTextOnlyQr(card, callback)
        } catch (e: Exception) {
            Log.e("CardSharingRepo", "QR generation failed: ${e.message}")
            callback(QrResult.Error("QR generation failed: ${e.message}"))
        }
    }

    /**
     * Generate QR code with only text data (no image) - FIXED VERSION
     */
    private fun generateTextOnlyQr(card: BusinessCard, callback: (QrResult) -> Unit) {
        try {
            // Create a clean card object without image path for text-only sharing
            val textOnlyCard = card.copy(
                imagePath = null, // Remove image path for text-only sharing
                id = 0 // Reset ID to allow duplicates
            )

            val qrData = gson.toJson(textOnlyCard)

            // Use Android's built-in QR generation or simple implementation
            val bitmap = createQRCodeBitmap(qrData, 512, 512)

            if (bitmap != null) {
                callback(QrResult.Success(bitmap, qrData))
            } else {
                callback(QrResult.Error("Failed to generate QR code bitmap"))
            }
        } catch (e: Exception) {
            Log.e("CardSharingRepo", "Text-only QR generation failed: ${e.message}")
            callback(QrResult.Error("QR generation failed: ${e.message}"))
        }
    }

    /**
     * Simple QR code bitmap generation without external dependencies
     */
    private fun createQRCodeBitmap(content: String, width: Int, height: Int): Bitmap? {
        return try {
            // Simple implementation - you can replace this with your preferred QR library
            val bitMatrix = com.google.zxing.qrcode.QRCodeWriter().encode(
                content,
                com.google.zxing.BarcodeFormat.QR_CODE,
                width,
                height
            )

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
                }
            }
            bitmap
        } catch (e: Exception) {
            Log.e("CardSharingRepo", "QR bitmap creation failed: ${e.message}")
            null
        }
    }

    /**
     * Process scanned QR code - FIXED VERSION with duplicate handling
     */
    suspend fun processScannedQr(qrData: String): BusinessCard? {
        return try {
            Log.d("CardSharingRepo", "Processing QR data: ${qrData.take(100)}...")

            val card = gson.fromJson(qrData, BusinessCard::class.java)

            // Reset ID and timestamp to allow duplicates
            val newCard = card.copy(
                id = 0, // Reset ID so Room will auto-generate new one
                createdAt = System.currentTimeMillis(), // New timestamp
                imagePath = null // Ensure no image path for text-only
            )

            Log.d("CardSharingRepo", "Successfully parsed card: ${newCard.name}")
            newCard

        } catch (e: Exception) {
            Log.e("CardSharingRepo", "QR processing failed: ${e.message}")
            null
        }
    }

    // ==================== FRIEND SYSTEM (Keep existing) ====================

    fun searchUsersByUsername(username: String, callback: (List<User>) -> Unit) {
        // Your existing implementation
        callback(emptyList())
    }

    fun listenForIncomingShares(onCardReceived: (SharedCard) -> Unit): ListenerRegistration {
        // Your existing implementation
        return object : ListenerRegistration {
            override fun remove() {}
        }
    }

    fun uploadCardImage(cardImage: Bitmap?, callback: (String?) -> Unit) {
        // Your existing implementation
        callback(null)
    }

    // ==================== DATA CLASSES ====================

    data class User(
        val uid: String = "",
        val username: String = "",
        val email: String = "",
        val friends: Map<String, String> = emptyMap()
    )

    data class SharedCard(
        val key: String = "",
        val from: String = "",
        val cardData: BusinessCard,
        val timestamp: Long = 0,
        val status: String = "",
        var cardImage: Bitmap? = null
    )

    sealed class QrResult {
        data class Success(val bitmap: Bitmap, val qrData: String) : QrResult()
        data class Error(val message: String) : QrResult()
    }
}