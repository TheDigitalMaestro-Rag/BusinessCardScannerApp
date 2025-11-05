package com.project.businesscardscannerapp.AppUI

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard

@Composable
fun ShareBusinessCardDialog(
    card: BusinessCard,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Business Card") },
        text = {
            Column(
                modifier = modifier
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Option 1: Image + Content
                Card(
                    onClick = {
                        BusinessCardSharer.shareBusinessCard(context, card)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Image",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Image + Content",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Share as text with image attachment",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Option 2: vCard Contact File
                Card(
                    onClick = {
                        BusinessCardSharer.shareBusinessCard(
                            context,
                            card,
                            ShareType.VCARD
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Contacts,
                            contentDescription = "Contact",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Contact File (.vcf)",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Share as iPhone/Android contact",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Option 3: QR Code
                Card(
                    onClick = {
                        BusinessCardSharer.shareBusinessCard(
                            context,
                            card,
                            ShareType.QR_CODE
                        )
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "QR Code",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = "Share as scannable QR code image",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // QR Code Preview (only shown if QR code option is visible)
                BusinessCardSharer.generateQRCodeImageBitmap(
                    BusinessCardSharer.buildShareText(card),
                    200
                )?.let { qrBitmap ->
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "QR Code Preview:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Image(
                        bitmap = qrBitmap,
                        contentDescription = "QR Code Preview",
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(200.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}