package com.raghu.businesscardscanner2.AppUI

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
        title = { Text("Share Business Card") },
        text = {
            Column(modifier = modifier) {
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
                        includeImage = includeImage,
                        includeQRCode = includeQRCode,
                        includeVCard = includeVCard
                    )
                    onDismiss()
                }
            ) {
                Text("Share")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}