package com.project.businesscardscannerapp.Registration

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import java.text.SimpleDateFormat
import java.util.Date

@Composable
fun BackupScreen(viewModel: BusinessCardViewModel = viewModel()) {
    val backupStatus by viewModel.backupStatus.collectAsState()
    var showRestoreConfirmation by remember { mutableStateOf(false) }
    var showRestoreResult by remember { mutableStateOf(false) }
    var restoreResultMessage by remember { mutableStateOf("") }

    LaunchedEffect(backupStatus) {
        when (backupStatus) {
            is BusinessCardViewModel.BackupStatus.Success -> {
                showRestoreResult = true
                restoreResultMessage = "Restore completed successfully!"
            }
            is BusinessCardViewModel.BackupStatus.Error -> {
                showRestoreResult = true
                restoreResultMessage = (backupStatus as BusinessCardViewModel.BackupStatus.Error).message
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", color = MaterialTheme.colorScheme.onPrimary) },
                backgroundColor = MaterialTheme.colorScheme.primary
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // <- Fill entire screen background
    ) { padding ->
        // Wrap content in Box to ensure full screen background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background) // <- ensures edges filled
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Backup Status
                when (backupStatus) {
                    is BusinessCardViewModel.BackupStatus.Success -> {
                        Text(
                            "✓ Backup successful!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is BusinessCardViewModel.BackupStatus.Error -> {
                        Text(
                            "✗ Error: ${(backupStatus as BusinessCardViewModel.BackupStatus.Error).message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    is BusinessCardViewModel.BackupStatus.InProgress -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                    else -> {}
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Backup Button
                Button(
                    onClick = { viewModel.performBackup() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = backupStatus !is BusinessCardViewModel.BackupStatus.InProgress,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Backup")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Backup Now")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Restore Button
                Button(
                    onClick = { showRestoreConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = backupStatus !is BusinessCardViewModel.BackupStatus.InProgress,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = "Restore")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore from Backup")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto Backup Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Automatic Backup",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    var isChecked by remember { mutableStateOf(false) } // load saved value
                    Switch(
                        checked = isChecked,
                        onCheckedChange = {
                            isChecked = it
                            viewModel.enableBackup(it)
                        },
                        colors = androidx.compose.material.SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Automatically backup your data every 24 hours when connected to Wi-Fi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Restore Confirmation Dialog
        if (showRestoreConfirmation) {
            AlertDialog(
                onDismissRequest = { showRestoreConfirmation = false },
                title = { Text("Confirm Restoration", color = MaterialTheme.colorScheme.onBackground) },
                text = {
                    Text(
                        "This will replace all current data with your backup. This action cannot be undone.",
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.restoreFromBackup()
                            showRestoreConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Restore")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestoreConfirmation = false }) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onBackground)
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }

        // Restore Result Dialog
        if (showRestoreResult) {
            AlertDialog(
                onDismissRequest = { showRestoreResult = false },
                title = { Text("Restore Status", color = MaterialTheme.colorScheme.onBackground) },
                text = { Text(restoreResultMessage, color = MaterialTheme.colorScheme.onBackground) },
                confirmButton = {
                    Button(
                        onClick = { showRestoreResult = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("OK")
                    }
                },
                backgroundColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
