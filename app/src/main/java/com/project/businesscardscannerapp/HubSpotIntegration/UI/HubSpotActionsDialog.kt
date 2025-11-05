package com.project.businesscardscannerapp.HubSpotIntegration.UI

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard

@Composable
fun HubSpotActionsDialog(
    businessCard: BusinessCard,
    onDismiss: () -> Unit,
    onSaveToCRM: (BusinessCard) -> Unit,
    isHubSpotConnected: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save to HubSpot") },
        text = {
            Column {
                if (!isHubSpotConnected) {
                    Text(
                        "HubSpot is not configured. Please configure HubSpot in settings first.",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Go to HubSpot settings to connect your account.",
                        style = MaterialTheme.typography.bodySmall
                    )
                } else {
                    Text("Choose how to save ${businessCard.name} to HubSpot:")
                    Spacer(modifier = Modifier.height(16.dp))

                    // CRM Option
                    ActionOption(
                        icon = Icons.Default.Contacts,
                        title = "Save to CRM",
                        description = "Add as contact in HubSpot CRM",
                        onClick = { onSaveToCRM(businessCard) }
                    )
                }
            }
        },
        confirmButton = {
            if (isHubSpotConnected) {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("OK")
                }
            }
        }
    )
}

@Composable
fun ActionOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}