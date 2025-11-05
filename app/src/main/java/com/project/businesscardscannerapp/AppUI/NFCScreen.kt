package com.project.businesscardscannerapp.AppUI

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.businesscardscannerapp.NFC.NFCService
import com.project.businesscardscannerapp.R
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NFCScreen(
    navController: NavController,
    viewModel: BusinessCardViewModel,
    nfcIntent: Intent?
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var nfcCard by remember { mutableStateOf<com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard?>(null) }
    var isWritingMode by remember { mutableStateOf(false) }
    var writeSuccess by remember { mutableStateOf<Boolean?>(null) }

    val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
    val isNFCSupported = nfcAdapter != null
    val isNFCEnabled = nfcAdapter?.isEnabled == true

    // Process NFC intent when screen is launched or intent changes
    LaunchedEffect(nfcIntent) {
        nfcIntent?.let { intent ->
            if (intent.action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                intent.action == NfcAdapter.ACTION_TECH_DISCOVERED) {

                val card = NFCService.parseNdefMessage(intent)
                nfcCard = card
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NFC Business Card") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // NFC Status
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Nfc,
                            contentDescription = "NFC",
                            tint = if (isNFCEnabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                        Text(
                            "NFC Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (!isNFCSupported) {
                        Text(
                            "NFC is not supported on this device",
                            color = MaterialTheme.colorScheme.error
                        )
                    } else if (!isNFCEnabled) {
                        Text(
                            "NFC is disabled. Please enable NFC in settings.",
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = {
                            context.startActivity(Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                        }) {
                            Text("Enable NFC")
                        }
                    } else {
                        Text(
                            "NFC is enabled and ready",
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Read NFC Card Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Read Business Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Tap an NFC business card to read contact information",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (nfcCard != null) {
                        BusinessCardForm(
                            card = nfcCard!!,
                            onSave = { card ->
                                coroutineScope.launch {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        viewModel.insert(card)
                                    }
                                    navController.popBackStack()
                                }
                            },
                            onCancel = {
                                nfcCard = null
                            },
                            capturedImage = null,
                            showImageSection = false
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Default.Nfc,
                                    contentDescription = "Tap NFC",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text("Tap an NFC business card")
                            }
                        }
                    }
                }
            }

            // Write to NFC Card Section
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Write Business Card",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "Select a card to write to an NFC tag",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (isWritingMode) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Ready to write - Tap an empty NFC tag",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(onClick = { isWritingMode = false }) {
                                Text("Cancel Writing")
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                // Navigate to card selection
                                navController.navigate("cardSelection")
                            },
                            enabled = isNFCEnabled
                        ) {
                            Text("Select Card to Write")
                        }
                    }

                    writeSuccess?.let { success ->
                        Text(
                            if (success) "Card written successfully!"
                            else "Failed to write card",
                            color = if (success) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            // Instructions
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Instructions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text("• Ensure NFC is enabled in device settings")
                    Text("• Hold device close to NFC tag/card (1-2 cm)")
                    Text("• For reading: Tap any NFC business card")
                    Text("• For writing: Use writable NFC tags")
                    Text("• Writing will overwrite existing data on tag")
                }
            }
        }
    }
}

// Helper function to check if we're handling an NFC intent
fun isNfcIntent(intent: Intent): Boolean {
    return intent.action?.let { action ->
        action == NfcAdapter.ACTION_NDEF_DISCOVERED ||
                action == NfcAdapter.ACTION_TAG_DISCOVERED ||
                action == NfcAdapter.ACTION_TECH_DISCOVERED
    } ?: false
}