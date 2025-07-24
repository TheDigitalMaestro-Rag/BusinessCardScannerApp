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
import android.widget.Toast
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object BusinessCardSharer {

    // Main share function with all options
    fun shareBusinessCard(
        context: Context,
        card: BusinessCard,
        shareType: ShareType = ShareType.TEXT,
        includeImage: Boolean = true,
        includeQRCode: Boolean = true,
        includeVCard: Boolean = true
    ) {
        when (shareType) {
            ShareType.TEXT -> shareAsText(context, card)
            ShareType.IMAGE -> shareAsImage(context, card)
            ShareType.VCARD -> shareAsVCard(context, card)
            ShareType.QR_CODE -> shareAsQRCode(context, card)
            ShareType.WHATSAPP -> shareViaWhatsApp(context, card)
            ShareType.TELEGRAM -> shareViaTelegram(context, card)
            ShareType.ALL -> shareAllFormats(context, card, includeImage, includeQRCode, includeVCard)
        }
    }

    // Share as plain text
    private fun shareAsText(context: Context, card: BusinessCard) {
        val shareText = buildShareText(card)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share Business Card as Text"))
    }

    // Share as image (with text overlay if no image available)
    private fun shareAsImage(context: Context, card: BusinessCard) {
        card.imagePath?.let { path ->
            val imageFile = File(path)
            if (imageFile.exists()) {
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    imageFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share Business Card Image"))
                return
            }
        }
        // If no image available, share text instead
        shareAsText(context, card)
    }

    // Share as vCard (iPhone-like contact sharing)
    private fun shareAsVCard(context: Context, card: BusinessCard) {
        val vCardText = generateVCard(card)
        val file = File(context.cacheDir, "${card.name}.vcf")
        file.writeText(vCardText)

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/x-vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share as Contact"))
    }

    // Share as QR code
    private fun shareAsQRCode(context: Context, card: BusinessCard) {
        val qrBitmap = generateQRCode(buildShareText(card), 500, 500) ?: return
        val file = File(context.cacheDir, "${card.name}_qr.png")
        FileOutputStream(file).use { out ->
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",  // This must match the authority in AndroidManifest.xml
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
    }

    // Share via WhatsApp
    private fun shareViaWhatsApp(context: Context, card: BusinessCard) {
        val text = buildShareText(card)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            `package` = "com.whatsapp"
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed", Toast.LENGTH_SHORT).show()
        }
    }

    // Share via Telegram
    private fun shareViaTelegram(context: Context, card: BusinessCard) {
        val text = buildShareText(card)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            `package` = "org.telegram.messenger"
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Telegram not installed", Toast.LENGTH_SHORT).show()
        }
    }

    // Share all formats in a zip file
    private fun shareAllFormats(
        context: Context,
        card: BusinessCard,
        includeImage: Boolean,
        includeQRCode: Boolean,
        includeVCard: Boolean
    ) {
        val cacheDir = File(context.cacheDir, "shared_card")
        if (!cacheDir.exists()) cacheDir.mkdirs()

        val files = mutableListOf<File>()

        // Save text file
        val text = buildShareText(card)
        val textFile = File(cacheDir, "card.txt").apply {
            writeText(text)
        }
        files.add(textFile)

        // Save image if enabled
        if (includeImage && card.imagePath != null) {
            val imageFile = File(card.imagePath)
            if (imageFile.exists()) {
                val copiedImage = File(cacheDir, "card_image.jpg")
                imageFile.copyTo(copiedImage, overwrite = true)
                files.add(copiedImage)
            }
        }

        // Save QR code if enabled
        if (includeQRCode) {
            val qrBitmap = generateQRCode(text, 400, 400)
            qrBitmap?.let {
                val qrFile = File(cacheDir, "card_qr.png")
                FileOutputStream(qrFile).use { out ->
                    it.compress(Bitmap.CompressFormat.PNG, 100, out)
                }
                files.add(qrFile)
            }
        }

        // Save vCard if enabled
        if (includeVCard) {
            val vCardText = generateVCard(card)
            val vcfFile = File(cacheDir, "contact.vcf").apply {
                writeText(vCardText)
            }
            files.add(vcfFile)
        }

        // Zip all files
        val zipFile = File(context.cacheDir, "BusinessCard_${card.name}.zip")
        zipFiles(files, zipFile)

        // Share zip
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            zipFile
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/zip"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share Business Card"))
    }

    fun buildShareText(card: BusinessCard): String {
        return buildString {
            append("Business Card Details\n\n")
            append("Name: ${card.name}\n")
            if (!card.position.isNullOrBlank()) append("Position: ${card.position}\n")
            if (!card.company.isNullOrBlank()) append("Company: ${card.company}\n")
            if (card.phones.isNotEmpty()) append("Phone: ${card.phones.joinToString(", ")}\n")
            if (!card.email.isNullOrBlank()) append("Email: ${card.email}\n")
            if (!card.website.isNullOrBlank()) append("Website: ${card.website}\n")
            if (!card.address.isNullOrBlank()) append("Address: ${card.address}\n")
            if (!card.notes.isNullOrBlank()) append("Notes: ${card.notes}\n")
        }
    }

    private fun generateVCard(card: BusinessCard): String {
        return buildString {
            append("BEGIN:VCARD\n")
            append("VERSION:3.0\n")
            append("FN:${card.name}\n")
            if (!card.position.isNullOrBlank()) append("TITLE:${card.position}\n")
            if (!card.company.isNullOrBlank()) append("ORG:${card.company}\n")
            card.phones.forEach { phone ->
                append("TEL;TYPE=WORK,VOICE:$phone\n")
            }
            if (!card.email.isNullOrBlank()) append("EMAIL;TYPE=WORK:${card.email}\n")
            if (!card.website.isNullOrBlank()) append("URL:${card.website}\n")
            if (!card.address.isNullOrBlank()) append("ADR;TYPE=WORK:;;${card.address}\n")
            append("END:VCARD\n")
        }
    }

    fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
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

    // Function to generate QR code as ImageBitmap for preview in UI
    fun generateQRCodeImageBitmap(content: String, size: Int): ImageBitmap? {
        val bitmap = generateQRCode(content, size, size)
        return bitmap?.asImageBitmap()
    }

    private fun zipFiles(files: List<File>, outputZip: File) {
        ZipOutputStream(FileOutputStream(outputZip)).use { zipOut ->
            files.forEach { file ->
                FileInputStream(file).use { input ->
                    val entry = ZipEntry(file.name)
                    zipOut.putNextEntry(entry)
                    input.copyTo(zipOut)
                }
            }
        }
    }
}

enum class ShareType {
    TEXT, IMAGE, VCARD, QR_CODE, WHATSAPP, TELEGRAM, ALL
}