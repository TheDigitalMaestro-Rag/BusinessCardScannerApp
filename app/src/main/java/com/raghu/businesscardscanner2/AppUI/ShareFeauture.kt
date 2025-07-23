package com.raghu.businesscardscanner2.AppUI

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

// Add to your dependencies:
// implementation 'com.google.zxing:core:3.5.1'

object BusinessCardSharer {

    // Enhanced sharing function with multiple options
    fun shareBusinessCard(
        context: Context,
        card: BusinessCard,
        includeImage: Boolean = true,
        includeQRCode: Boolean = true,
        includeVCard: Boolean = true
    ) {
        val shareIntents = mutableListOf<Intent>()
        val shareText = buildShareText(card)

        // Create base text sharing intent
        val textIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, shareText)
            type = "text/plain"
        }
        shareIntents.add(textIntent)

        // Add image sharing if available and requested
        if (includeImage && card.imagePath != null) {
            val imageUri = getImageUri(context, card.imagePath)
            imageUri?.let { uri ->
                val imageIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "image/*"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                shareIntents.add(imageIntent)
            }
        }

        // Add QR code sharing if requested
        if (includeQRCode) {
            val qrBitmap = generateQRCode(shareText, 400, 400)
            qrBitmap?.let { bitmap ->
                val qrUri = saveBitmapAndGetUri(context, bitmap, "business_card_qr.png")
                qrUri?.let { uri ->
                    val qrIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    shareIntents.add(qrIntent)
                }
            }
        }

        // Add vCard sharing if requested (iPhone-like contact sharing)
        if (includeVCard) {
            val vCardText = generateVCard(card)
            val vCardUri = saveTextAndGetUri(context, vCardText, "contact.vcf")
            vCardUri?.let { uri ->
                val vCardIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, uri)
                    type = "text/x-vcard"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                shareIntents.add(vCardIntent)
            }
        }

        // Create chooser with all options
        val chooserIntent = Intent.createChooser(shareIntents.first(), "Share Business Card")
        chooserIntent.putExtra(
            Intent.EXTRA_INITIAL_INTENTS,
            shareIntents.drop(1).toTypedArray()
        )
        context.startActivity(chooserIntent)
    }

     fun buildShareText(card: BusinessCard): String {
        return buildString {
            append("Business Card Details\n\n")
            append("Name: ${card.name}\n")
            if (card.position.isNotBlank()) append("Position: ${card.position}\n")
            if (card.company.isNotBlank()) append("Company: ${card.company}\n")
            if (card.phones.isNotEmpty()) append("Phone: ${card.phones.joinToString(", ")}\n")
            if (card.email.isNotBlank()) append("Email: ${card.email}\n")
            if (card.website.isNotBlank()) append("Website: ${card.website}\n")
            if (card.address.isNotBlank()) append("Address: ${card.address}\n")
            if (card.notes.isNotBlank()) append("Notes: ${card.notes}\n")
        }
    }

    private fun generateVCard(card: BusinessCard): String {
        return buildString {
            append("BEGIN:VCARD\n")
            append("VERSION:3.0\n")
            append("FN:${card.name}\n")
            if (card.position.isNotBlank()) append("TITLE:${card.position}\n")
            if (card.company.isNotBlank()) append("ORG:${card.company}\n")
            card.phones.forEach { phone ->
                append("TEL;TYPE=WORK,VOICE:$phone\n")
            }
            if (card.email.isNotBlank()) append("EMAIL;TYPE=WORK:${card.email}\n")
            if (card.website.isNotBlank()) append("URL:${card.website}\n")
            if (card.address.isNotBlank()) append("ADR;TYPE=WORK:;;${card.address}\n")
            append("END:VCARD\n")
        }
    }

    private fun getImageUri(context: Context, imagePath: String): Uri? {
        val imageFile = File(imagePath)
        return if (imageFile.exists()) {
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imageFile
            )
        } else {
            null
        }
    }

    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    pixels[y * width + x] = if (bitMatrix.get(x, y)) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                }
            }
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, width, 0, 0, width, height)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveBitmapAndGetUri(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            }

            val resolver: ContentResolver = context.contentResolver
            var uri: Uri? = null
            try {
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                    }
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            // For older versions use file system
            val imagesDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFile = File(imagesDir, fileName)
            try {
                FileOutputStream(imageFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    private fun saveTextAndGetUri(context: Context, text: String, fileName: String): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "text/x-vcard")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS)
            }

            val resolver: ContentResolver = context.contentResolver
            var uri: Uri? = null
            try {
                uri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(text.toByteArray())
                    }
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            // For older versions use file system
            val filesDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            val textFile = File(filesDir, fileName)
            try {
                FileOutputStream(textFile).use { out ->
                    out.write(text.toByteArray())
                }
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    textFile
                )
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    // Function to generate QR code as ImageBitmap for preview in UI
    fun generateQRCodeImageBitmap(content: String, size: Int): ImageBitmap? {
        val bitmap = generateQRCode(content, size, size)
        return bitmap?.asImageBitmap()
    }
}


// Share business card function
fun shareBusinessCard(context: Context, card: BusinessCard) {
    val shareText = buildString {
        append("Business Card Details\n\n")
        append("Name: ${card.name}\n")
        card.position?.let { append("Position: $it\n") }
        card.company?.let { append("Company: $it\n") }
        card.phones?.let { append("Phone: $it\n") }
        card.email?.let { append("Email: $it\n") }
        card.website?.let { append("Website: $it\n") }
        card.address?.let { append("Address: $it\n") }
        card.notes?.let { append("Notes: $it\n") }
    }

    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"

        // Share image if available
        card.imagePath?.let { imagePath ->
            val imageFile = File(imagePath)
            if (imageFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }

    context.startActivity(Intent.createChooser(shareIntent, "Share Business Card"))
}
