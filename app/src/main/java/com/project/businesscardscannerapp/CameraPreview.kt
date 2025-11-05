package com.project.businesscardscannerapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.documentscanner.GmsDocumentScanner
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

//@Composable
//fun CameraPreview(
//    onImageCaptured: (Bitmap) -> Unit,
//    onError: (Throwable) -> Unit
//) {
//    val context = LocalContext.current
//    val lifecycleOwner = LocalLifecycleOwner.current
//    val activity = context as Activity
//    val coroutineScope = rememberCoroutineScope()
//
//    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
//    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
//
//    // Gallery launcher
//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.GetContent(),
//        onResult = { uri ->
//            uri?.let {
//                coroutineScope.launch {
//                    try {
//                        val bitmap = withContext(Dispatchers.IO) {
//                            context.contentResolver.openInputStream(uri)?.use { stream ->
//                                BitmapFactory.decodeStream(stream)
//                            }
//                        }
//                        bitmap?.let { onImageCaptured(it) }
//                    } catch (e: Exception) {
//                        onError(e)
//                    }
//                }
//            }
//        }
//    )
//
//    Box(modifier = Modifier.fillMaxSize()) {
//        // PreviewView to show camera
//        AndroidView(
//            factory = { ctx ->
//                val previewView = PreviewView(ctx).apply {
//                    scaleType = PreviewView.ScaleType.FILL_CENTER
//                }
//
//                val cameraProvider = cameraProviderFuture.get()
//                val preview = Preview.Builder().build().also {
//                    it.setSurfaceProvider(previewView.surfaceProvider)
//                }
//
//                imageCapture = ImageCapture.Builder()
//                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                    .build()
//
//                try {
//                    cameraProvider.unbindAll()
//                    cameraProvider.bindToLifecycle(
//                        lifecycleOwner,
//                        CameraSelector.DEFAULT_BACK_CAMERA,
//                        preview,
//                        imageCapture
//                    )
//                } catch (e: Exception) {
//                    onError(e)
//                }
//
//                previewView
//            },
//            modifier = Modifier.fillMaxSize()
//        )
//
//        // Buttons
//        Row(
//            modifier = Modifier
//                .align(Alignment.BottomCenter)
//                .padding(24.dp),
//            horizontalArrangement = Arrangement.spacedBy(16.dp)
//        ) {
//            Button(onClick = {
//                launcher.launch("image/*")
//            }) {
//                Icon(Icons.Default.Image, contentDescription = "Gallery")
//                Text(" Gallery")
//            }
//
//            Button(onClick = {
//                val imageCapture = imageCapture ?: return@Button
//                coroutineScope.launch {
//                    val photoFile = createImageFile(context)
//
//                    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
//
//                    imageCapture.takePicture(
//                        outputOptions,
//                        ContextCompat.getMainExecutor(context),
//                        object : ImageCapture.OnImageSavedCallback {
//                            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
//                                if (bitmap != null) {
//                                    onImageCaptured(bitmap)
//                                } else {
//                                    onError(Exception("Failed to decode captured image"))
//                                }
//                            }
//
//                            override fun onError(exc: ImageCaptureException) {
//                                onError(exc)
//                            }
//                        }
//                    )
//                }
//            }) {
//                Icon(Icons.Default.Camera, contentDescription = "Capture")
//                Text(" Capture")
//            }
//        }
//    }
//}
//
//
//
//private fun createImageFile(context: Context): File {
//    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
//    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
//    return File.createTempFile(
//        "JPEG_${timeStamp}_",
//        ".jpg",
//        storageDir
//    ).also {
//        // Add to gallery
//        MediaScannerConnection.scanFile(
//            context,
//            arrayOf(it.absolutePath),
//            arrayOf("image/jpeg"),
//            null
//        )
//    }
//}

@Composable
fun CameraPreview(
    onImageCaptured: (Bitmap) -> Unit,
    onError: (Throwable) -> Unit
) {
    val context = LocalContext.current
    val activity = context as Activity
    val scannerOptions = remember {
        GmsDocumentScannerOptions.Builder()
            .setGalleryImportAllowed(true)
            .setPageLimit(1)
            .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
            .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
            .build()
    }
    val scanner = remember { GmsDocumentScanning.getClient(scannerOptions) }
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            val pages = scanningResult?.pages
            if (!pages.isNullOrEmpty()) {
                val imageUri = pages[0].imageUri
                // Convert imageUri to Bitmap and pass to onImageCaptured
            } else {
                onError(Exception("No pages scanned"))
            }
        } else {
            onError(Exception("Scan canceled or failed"))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Text("Tap below to scan or pick from gallery", modifier = Modifier.align(Alignment.Center))
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = {
                scanner.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        onError(e)
                    }
            }) {
                Icon(Icons.Default.Camera, contentDescription = "Scan")
                Text(" Scan Card")
            }
        }
    }
}
