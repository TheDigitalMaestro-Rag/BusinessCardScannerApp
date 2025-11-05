package com.project.businesscardscannerapp.AppUI

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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import ezvcard.VCard
import ezvcard.parameter.AddressType
import ezvcard.parameter.EmailType
import ezvcard.parameter.TelephoneType
import ezvcard.property.*


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
            ShareType.ALL -> shareAllFormats(context, card, includeImage, includeQRCode, includeVCard)
        }
    }

    // General share function that combines text and image sharing
    fun shareBusinessCard(context: Context, card: BusinessCard) {
        val shareText = buildString {
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
                        "${context.packageName}.provider",
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

    // Share as plain text
    private fun shareAsText(context: Context, card: BusinessCard) {
        shareBusinessCard(context, card) // Use the general share function
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
        shareBusinessCard(context, card)
    }

    // Share as vCard (iPhone-like contact sharing)
    private fun shareAsVCard(context: Context, card: BusinessCard) {
        try {
            val vcardFile = generateVCardFile(context, card)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                vcardFile
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/x-vcard"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share as Contact"))
        } catch (e: Exception) {
            Toast.makeText(context, "Error sharing contact: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }

    // Share as QR code
    private fun shareAsQRCode(context: Context, card: BusinessCard) {
        val qrBitmap = generateQRCode(buildShareText(card), 500, 500) ?: run {
            Toast.makeText(context, "Failed to generate QR code.", Toast.LENGTH_SHORT).show()
            return
        }
        val file = File(context.cacheDir, "${card.name}_qr.png")
        FileOutputStream(file).use { out ->
            qrBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Share QR Code"))
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
            try {
                val vcfFile = generateVCardFile(context, card)
                files.add(vcfFile)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to generate vCard for zip: ${e.message}", Toast.LENGTH_SHORT).show()
                e.printStackTrace()
            }
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

    private fun generateVCardFile(context: Context, card: BusinessCard): File {
        val vcard = VCard()

        // Set name
        vcard.structuredName = StructuredName().apply {
            val parts = card.name.split(" ")
            family = parts.lastOrNull()
            given = parts.dropLast(1).joinToString(" ")
        }

        // Add phone numbers
        card.phones.forEach { phone ->
            vcard.addTelephoneNumber(Telephone(phone).apply {
                types.add(TelephoneType.WORK)
            })
        }

        // Add email
        card.email?.takeIf { it.isNotBlank() }?.let { email ->
            vcard.addEmail(Email(email).apply {
                types.add(EmailType.WORK)
            })
        }

        // Add organization
        card.company?.takeIf { it.isNotBlank() }?.let { company ->
            val org = Organization()
            org.values.add(company)
            vcard.organization = org
        }

        // Add title/position
        card.position?.takeIf { it.isNotBlank() }?.let { position ->
            vcard.addTitle(position)
        }

        // Add website URL
        card.website?.takeIf { it.isNotBlank() }?.let { url ->
            vcard.addUrl(url)
        }

        // Add address
        card.address?.takeIf { it.isNotBlank() }?.let { address ->
            vcard.addAddress(ezvcard.property.Address().apply {
                streetAddress = address
                types.add(AddressType.WORK)
            })
        }

        // Add notes
        card.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            vcard.addNote(notes)
        }

        val file = File(context.cacheDir, "${card.name.replace(" ", "_")}.vcf")
        FileOutputStream(file).use { fos ->
            ezvcard.Ezvcard.write(vcard).go(fos)
        }
        return file
    }

    fun generateQRCode(content: String, width: Int, height: Int): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            BarcodeEncoder().createBitmap(bitMatrix)
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
    TEXT, IMAGE, VCARD, QR_CODE, ALL
}