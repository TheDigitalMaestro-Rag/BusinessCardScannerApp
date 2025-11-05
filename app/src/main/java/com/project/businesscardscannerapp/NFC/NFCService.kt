package com.project.businesscardscannerapp.NFC

import android.app.Activity
import android.content.Intent
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.util.Log
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.nio.charset.Charset

class NFCService {

    companion object {
        private const val TAG = "NFCService"

        fun parseNdefMessage(intent: Intent): BusinessCard? {
            return try {
                val rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                rawMessages?.firstOrNull() as? NdefMessage ?: return null

                val ndefMessage = rawMessages.first() as NdefMessage
                parseVCardFromNdef(ndefMessage)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing NDEF message", e)
                null
            }
        }

        private fun parseVCardFromNdef(ndefMessage: NdefMessage): BusinessCard? {
            return try {
                for (record in ndefMessage.records) {
                    if (record.toUri() != null) {
                        // Handle URI records
                        val uri = record.toUri().toString()
                        if (uri.startsWith("tel:") || uri.startsWith("mailto:") || uri.startsWith("http")) {
                            // Create basic card from URI
                            return BusinessCard(
                                name = "NFC Contact",
                                company = "",
                                position = "",
                                phones = if (uri.startsWith("tel:")) listOf(uri.removePrefix("tel:")) else emptyList(),
                                email = if (uri.startsWith("mailto:")) uri.removePrefix("mailto:") else "",
                                address = "",
                                website = if (uri.startsWith("http")) uri else "",
                                notes = "Imported via NFC",
                                tags = emptyList()
                            )
                        }
                    }

                    // Handle text/vcard MIME type
                    if (record.toMimeType() == "text/vcard") {
                        val vcardData = String(record.payload, Charset.forName("UTF-8"))
                        return parseVCardData(vcardData)
                    }

                    // Handle text/plain with vCard data
                    if (record.toMimeType() == "text/plain") {
                        val textData = String(record.payload, Charset.forName("UTF-8"))
                        if (textData.contains("BEGIN:VCARD")) {
                            return parseVCardData(textData)
                        }
                    }
                }
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing vCard from NDEF", e)
                null
            }
        }

        private fun parseVCardData(vcardData: String): BusinessCard {
            var name = ""
            var company = ""
            var position = ""
            val phones = mutableListOf<String>()
            var email = ""
            var address = ""
            var website = ""

            val lines = vcardData.split("\n")
            for (line in lines) {
                when {
                    line.startsWith("FN:") -> name = line.removePrefix("FN:").trim()
                    line.startsWith("ORG:") -> company = line.removePrefix("ORG:").trim()
                    line.startsWith("TITLE:") -> position = line.removePrefix("TITLE:").trim()
                    line.startsWith("TEL;") -> {
                        val phone = line.substringAfter(":").trim()
                        if (phone.isNotBlank()) phones.add(phone)
                    }
                    line.startsWith("EMAIL;") -> email = line.substringAfter(":").trim()
                    line.startsWith("ADR;") -> address = line.substringAfter(":").replace(";", ", ").trim()
                    line.startsWith("URL:") -> website = line.removePrefix("URL:").trim()
                }
            }

            return BusinessCard(
                name = name.ifBlank { "NFC Contact" },
                company = company,
                position = position,
                phones = phones,
                email = email,
                address = address,
                website = website,
                notes = "Imported via NFC",
                tags = emptyList()
            )
        }

        fun writeBusinessCardToTag(activity: Activity, card: BusinessCard): Boolean {
            return try {
                val intent = activity.intent
                val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG)
                tag?.let {
                    writeNdefToTag(it, createVCardNdefMessage(card))
                } ?: false
            } catch (e: Exception) {
                Log.e(TAG, "Error writing to NFC tag", e)
                false
            }
        }

        private fun createVCardNdefMessage(card: BusinessCard): NdefMessage {
            val vcard = buildString {
                append("BEGIN:VCARD\n")
                append("VERSION:3.0\n")
                append("FN:${card.name}\n")
                if (card.company.isNotBlank()) append("ORG:${card.company}\n")
                if (card.position.isNotBlank()) append("TITLE:${card.position}\n")
                card.phones.forEach { phone ->
                    append("TEL;TYPE=WORK:${phone}\n")
                }
                if (card.email.isNotBlank()) append("EMAIL:${card.email}\n")
                if (card.website.isNotBlank()) append("URL:${card.website}\n")
                if (card.address.isNotBlank()) append("ADR:${card.address.replace(",", ";")}\n")
                append("END:VCARD")
            }

            val vcardRecord = NdefRecord.createMime("text/vcard", vcard.toByteArray(Charset.forName("UTF-8")))
            return NdefMessage(arrayOf(vcardRecord))
        }

        private fun writeNdefToTag(tag: Tag, ndefMessage: NdefMessage): Boolean {
            return try {
                val ndef = Ndef.get(tag)
                ndef.connect()
                ndef.writeNdefMessage(ndefMessage)
                ndef.close()
                true
            } catch (e: Exception) {
                Log.e(TAG, "Error writing NDEF to tag", e)
                false
            }
        }

        fun isNfcSupported(activity: Activity): Boolean {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            return nfcAdapter != null
        }

        fun isNfcEnabled(activity: Activity): Boolean {
            val nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
            return nfcAdapter?.isEnabled == true
        }
    }
}