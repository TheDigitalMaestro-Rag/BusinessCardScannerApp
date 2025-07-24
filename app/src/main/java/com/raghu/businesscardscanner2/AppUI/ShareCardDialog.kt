package com.raghu.businesscardscanner2.AppUI

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.raghu.businesscardscanner.R
import com.raghu.businesscardscanner2.RoomDB.Entity.BusinessCard

@Composable
fun ShareBusinessCardDialog(
    card: BusinessCard,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var includeImage by remember { mutableStateOf(true) }
    var includeQRCode by remember { mutableStateOf(true) }
    var includeVCard by remember { mutableStateOf(true) }
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_business_card)) },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState())
            ) {
                // QR Code Preview
                if (includeQRCode) {
                    val qrBitmap = remember(card) {
                        BusinessCardSharer.generateQRCodeImageBitmap(
                            BusinessCardSharer.buildShareText(card),
                            200
                        )
                    }

                    qrBitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 16.dp)
                                .size(200.dp)
                        )
                    }
                }

                // Sharing options
                Text("Sharing Options:", style = MaterialTheme.typography.labelLarge)

                // Quick share buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            BusinessCardSharer.shareBusinessCard(
                                context,
                                card,
                                ShareType.VCARD
                            )
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Contacts,
                                contentDescription = "Share as Contact",
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Contact", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(
                        onClick = {
                            BusinessCardSharer.shareBusinessCard(
                                context,
                                card,
                                ShareType.WHATSAPP
                            )
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Chat,
                                contentDescription = "Share via WhatsApp",
                                modifier = Modifier.size(48.dp)
                            )
                            Text("WhatsApp", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(
                        onClick = {
                            BusinessCardSharer.shareBusinessCard(
                                context,
                                card,
                                ShareType.TELEGRAM
                            )
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Send,
                                contentDescription = "Share via Telegram",
                                modifier = Modifier.size(48.dp)
                            )
                            Text("Telegram", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    IconButton(
                        onClick = {
                            BusinessCardSharer.shareBusinessCard(
                                context,
                                card,
                                ShareType.QR_CODE
                            )
                        }
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.QrCode,
                                contentDescription = "Share QR Code",
                                modifier = Modifier.size(48.dp)
                            )
                            Text("QR Code", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Advanced options
                Text("Advanced Options:", style = MaterialTheme.typography.labelLarge)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = includeImage,
                        onCheckedChange = { includeImage = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Include Image", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = includeQRCode,
                        onCheckedChange = { includeQRCode = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Include QR Code", style = MaterialTheme.typography.bodyLarge)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    Checkbox(
                        checked = includeVCard,
                        onCheckedChange = { includeVCard = it },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text("Include Contact (vCard)", style = MaterialTheme.typography.bodyLarge)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    BusinessCardSharer.shareBusinessCard(
                        context = context,
                        card = card,
                        shareType = ShareType.ALL,
                        includeImage = includeImage,
                        includeQRCode = includeQRCode,
                        includeVCard = includeVCard
                    )
                    onDismiss()
                }
            ) {
                Text("Share All")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}