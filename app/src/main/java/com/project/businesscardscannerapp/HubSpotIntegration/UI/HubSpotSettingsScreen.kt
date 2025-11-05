package com.project.businesscardscannerapp.HubSpotIntegration.UI

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotIntegrationState
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotIntegrationViewModel
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotIntegrationViewModelFactory
import com.project.businesscardscannerapp.HubSpotIntegration.HubSpotTestResult

// HubSpotSettingsScreen.kt - Updated version
@Composable
fun HubSpotSettingsScreen(
    viewModel: HubSpotIntegrationViewModel = viewModel(
        factory = HubSpotIntegrationViewModelFactory(LocalContext.current)
    )
) {
    val integrationState by viewModel.integrationState.collectAsState()
    val isConnected by viewModel.isConnected.collectAsState()

    var showConfigurationDialog by remember { mutableStateOf(false) }
    var showTestConnection by remember { mutableStateOf(false) }
    var accessToken by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Connection Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = "Connection Status",
                        tint = if (isConnected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isConnected) "Connected to HubSpot" else "Not Connected to HubSpot",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (isConnected) {
                    Column {
                        Button(
                            onClick = { viewModel.disconnect() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Disconnect HubSpot")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { showTestConnection = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Test Connection")
                        }
                    }
                } else {
                    Button(
                        onClick = { showConfigurationDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Connect to HubSpot")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions Card
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    "HubSpot CRM Integration",
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Connect your app to HubSpot to automatically sync business card contacts to your CRM.",
                    style = MaterialTheme.typography.bodySmall
                )

                if (!isConnected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "How to get your access token:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "1. Go to HubSpot Settings → Integrations → Private Apps\n" +
                                "2. Create a new private app\n" +
                                "3. Enable CRM scopes for contacts\n" +
                                "4. Copy the access token\n" +
                                "5. Paste it below",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Show current status
        when (val state = integrationState) {
            is HubSpotIntegrationState.Loading -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.message)
                    }
                }
            }
            is HubSpotIntegrationState.Success -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            is HubSpotIntegrationState.Error -> {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(state.message, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            else -> {}
        }
    }

    // Configuration Dialog
    if (showConfigurationDialog) {
        AlertDialog(
            onDismissRequest = { showConfigurationDialog = false },
            title = { Text("Connect to HubSpot") },
            text = {
                Column {
                    Text("Enter your HubSpot Private App Access Token:")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = accessToken,
                        onValueChange = { accessToken = it },
                        label = { Text("Access Token") },
                        placeholder = { Text("Paste your HubSpot access token here") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your token is stored securely on your device only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.configureHubSpot(accessToken.trim())
                        showConfigurationDialog = false
                    },
                    enabled = accessToken.isNotBlank()
                ) {
                    Text("Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigurationDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Test Connection Dialog
    if (showTestConnection) {
        var testResult by remember { mutableStateOf<HubSpotTestResult?>(null) }
        var isLoading by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showTestConnection = false },
            title = { Text("Test HubSpot Connection") },
            text = {
                Column {
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Testing connection to HubSpot...")
                        }
                    } else {
                        testResult?.let { result ->
                            if (result.success) {
                                Text("✅ Connection successful! You can now sync contacts to HubSpot.", color = androidx.compose.ui.graphics.Color.Green)
                            } else {
                                Column {
                                    Text("❌ Connection failed", color = androidx.compose.ui.graphics.Color.Red)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Error: ${result.error ?: "Unknown error"}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        } ?: Text("Test your connection to HubSpot")
                    }
                }
            },
            confirmButton = {
                if (!isLoading) {
                    Button(onClick = {
                        isLoading = true
                        viewModel.testConnection { result ->
                            testResult = result
                            isLoading = false
                        }
                    }) {
                        Text("Test Connection")
                    }
                }
                TextButton(onClick = { showTestConnection = false }) {
                    Text("Close")
                }
            }
        )
    }
}
@Composable
fun ConfigurationDialog(
    accessToken: String,
    onAccessTokenChange: (String) -> Unit,
    onConfigure: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure HubSpot") },
        text = {
            Column {
                Text("Enter your HubSpot Private App Access Token:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = accessToken,
                    onValueChange = onAccessTokenChange,
                    label = { Text("Access Token") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Paste your access token here") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Your token is stored securely on your device only.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfigure,
                enabled = accessToken.isNotBlank()
            ) {
                Text("Save Configuration")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun DuplicateContactDialog(
    contactId: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Already Exists") },
        text = {
            Text("This contact already exists in your HubSpot CRM.")
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
fun TestConnectionDialog(
    onDismiss: () -> Unit,
    viewModel: HubSpotIntegrationViewModel
) {
    var testResult by remember { mutableStateOf<HubSpotTestResult?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Test Connection") },
        text = {
            Column {
                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Testing connection...")
                    }
                } else {
                    testResult?.let { result ->
                        if (result.success) {
                            Text("✅ Connection successful!", color = androidx.compose.ui.graphics.Color.Green)
                        } else {
                            Text("❌ Connection failed: ${result.error}", color = androidx.compose.ui.graphics.Color.Red)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isLoading) {
                Button(onClick = {
                    isLoading = true
                    viewModel.testConnection { result ->
                        testResult = result
                        isLoading = false
                    }
                }) {
                    Text("Test Again")
                }
            }
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}