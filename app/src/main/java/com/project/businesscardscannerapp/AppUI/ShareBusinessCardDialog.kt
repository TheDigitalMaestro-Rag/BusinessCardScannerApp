package com.project.businesscardscannerapp.AppUI

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import com.project.businesscardscannerapp.RoomDB.ProvideDB.CardSharingRepository
import com.project.businesscardscannerapp.ViewModel.BusinessCardViewModel

@Composable
fun ShareBusinessCardDialog2(
    card: BusinessCard,
    viewModel: BusinessCardViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<CardSharingRepository.User>()) }
    var isLoading by remember { mutableStateOf(false) }
    var includeImageInQr by remember { mutableStateOf(false) }
    var qrResult by remember { mutableStateOf<CardSharingRepository.QrResult?>(null) }
    var isGeneratingQr by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Load card image for QR
    val cardImage = remember(card.imagePath) {
        card.imagePath?.let { path ->
            loadImageFromStorage(context, path)
        }
    }

    // Generate QR when tab changes or includeImage changes
    LaunchedEffect(selectedTab, includeImageInQr) {
        if (selectedTab == 1) {
            isGeneratingQr = true
            viewModel.generateQrForCard(
                card,
                cardImage,
                includeImageInQr
            ) { result ->
                qrResult = result
                isGeneratingQr = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Share Business Card",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Tab Row

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("QR Code") }
                )


                Spacer(modifier = Modifier.height(16.dp))

                when (selectedTab) {
                    1 -> {
                        // QR Code Tab - UPDATED with proper async handling
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Image inclusion toggle
                            if (cardImage != null) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    Text(
                                        text = "Include Card Details Info in QR code",
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (isGeneratingQr) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Text("Generating QR code...")
                            } else {
                                when (val result = qrResult) {
                                    is CardSharingRepository.QrResult.Success -> {
                                        Image(
                                            bitmap = result.bitmap.asImageBitmap(),
                                            contentDescription = "QR Code",
                                            modifier = Modifier.size(200.dp)
                                        )
                                        Text(
                                            text = if (includeImageInQr) {
                                                "Scan this QR to get card Info"
                                            } else {
                                                "Scan this QR code to share the business card"
                                            },
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(16.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    is CardSharingRepository.QrResult.Error -> {
                                        Text(
                                            text = result.message,
                                            color = MaterialTheme.colorScheme.error,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    null -> {
                                        Text("Generate a QR code to share")
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

// Add this to your ScanScreen or create a new QR scanning screen
@androidx.annotation.OptIn(ExperimentalGetImage::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(
    onQrScanned: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var hasPermission by remember { mutableStateOf(false) }
    var qrScanned by remember { mutableStateOf(false) } // Track if QR has been scanned

    // Check camera permission
    LaunchedEffect(Unit) {
        hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasPermission && !qrScanned) {
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val executor = ContextCompat.getMainExecutor(context)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val barcodeScanner = BarcodeScanning.getClient()
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(executor) { imageProxy ->
                                        if (qrScanned) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        val image = InputImage.fromMediaImage(
                                            imageProxy.image!!,
                                            imageProxy.imageInfo.rotationDegrees
                                        )

                                        barcodeScanner.process(image)
                                            .addOnSuccessListener { barcodes ->
                                                barcodes.firstOrNull()?.rawValue?.let { qrData ->
                                                    // Set flag to prevent multiple scans
                                                    qrScanned = true
                                                    // Call the onQrScanned callback here
                                                    onQrScanned(qrData)
                                                }
                                            }
                                            .addOnCompleteListener {
                                                imageProxy.close()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("QRScan", "QR processing failed", e)
                                                imageProxy.close()
                                            }
                                    }
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    context as LifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("QRScan", "Use case binding failed", e)
                            }
                        }, executor)

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else if (qrScanned) {
                // Show loading state after scanning
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Processing QR code...")
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission is required to scan QR codes")
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        // Request camera permission
                        (context as Activity).requestPermissions(
                            arrayOf(Manifest.permission.CAMERA),
                            200
                        )
                    }) {
                        Text("Grant Permission")
                    }
                }
            }
        }
    }
}