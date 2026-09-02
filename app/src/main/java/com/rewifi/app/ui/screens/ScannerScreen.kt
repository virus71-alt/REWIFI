package com.rewifi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.rewifi.app.data.WifiQr
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

/**
 * In-app QR scanner built on CameraX + ML Kit with support for live camera
 * scanning as well as gallery QR import.
 */
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onResult: (ssid: String, password: String, security: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasPermission) permLauncher.launch(Manifest.permission.CAMERA)
    }

    // Shared scanner and analysis executor with proper lifecycle cleanup
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            scanner.close()
            analysisExecutor.shutdown()
        }
    }

    // Guard so we only act on the first valid WiFi QR.
    var handled by remember { mutableStateOf(false) }
    var isProcessingGallery by remember { mutableStateOf(false) }

    fun processGalleryUri(uri: Uri) {
        coroutineScope.launch(Dispatchers.IO) {
            val inputImage = try {
                InputImage.fromFilePath(context, uri)
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    isProcessingGallery = false
                    Toast.makeText(context, "NO QR CODE FOUND", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }

            scanner.process(inputImage)
                .addOnSuccessListener(ContextCompat.getMainExecutor(context)) { barcodes ->
                    isProcessingGallery = false
                    if (handled) return@addOnSuccessListener

                    if (barcodes.isEmpty()) {
                        Toast.makeText(context, "NO QR CODE FOUND", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    var foundWifi = false
                    for (barcode in barcodes) {
                        val raw = barcode.rawValue ?: continue
                        val wifi = WifiQr.parse(raw)
                        if (wifi != null) {
                            foundWifi = true
                            handled = true
                            onResult(wifi.ssid, wifi.password, wifi.security)
                            break
                        }
                    }

                    if (!foundWifi) {
                        Toast.makeText(context, "NOT A VALID WIFI QR", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener(ContextCompat.getMainExecutor(context)) {
                    isProcessingGallery = false
                    if (!handled) {
                        Toast.makeText(context, "NO QR CODE FOUND", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processGalleryUri(uri)
        } else {
            isProcessingGallery = false
        }
    }

    BackHandler { onBack() }

    Box(Modifier.fillMaxSize().background(Ink)) {
        if (hasPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                    }
                    val providerFuture = ProcessCameraProvider.getInstance(ctx)

                    providerFuture.addListener({
                        val provider = providerFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        val analysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        analysis.setAnalyzer(analysisExecutor) { proxy ->
                            scan(scanner, proxy) { raw ->
                                if (handled) return@scan
                                val wifi = WifiQr.parse(raw) ?: return@scan
                                handled = true
                                previewView.post { onResult(wifi.ssid, wifi.password, wifi.security) }
                            }
                        }
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                }
            )

            // Viewfinder frame.
            Box(
                Modifier.align(Alignment.Center).size(240.dp)
                    .border(4.dp, Yellow, RoundedCornerShape(20.dp))
            )
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("CAMERA ACCESS NEEDED", color = Snow, fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow camera access to scan a WiFi QR code.",
                    color = Snow.copy(alpha = 0.7f), fontSize = 14.sp, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(20.dp))
                BrutalButton("GRANT ACCESS", bg = Yellow, fg = Ink) {
                    permLauncher.launch(Manifest.permission.CAMERA)
                }
            }
        }

        // Top bar: back + prompt.
        val colors = com.rewifi.app.ui.theme.RewifiTheme.colors
        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary) }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Yellow)
                    .border(3.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("POINT AT A WIFI QR", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp) }
        }

        // Bottom bar: Gallery import button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            BrutalButton(
                text = if (isProcessingGallery) "SCANNING IMAGE..." else "IMPORT FROM GALLERY",
                modifier = Modifier.fillMaxWidth(),
                bg = Yellow,
                fg = Ink,
                onClick = {
                    if (isProcessingGallery || handled) return@BrutalButton
                    isProcessingGallery = true
                    galleryLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalGetImage::class)
private fun scan(scanner: BarcodeScanner, proxy: ImageProxy, onQr: (String) -> Unit) {
    val media = proxy.image
    if (media == null) { proxy.close(); return }
    val input = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
    scanner.process(input)
        .addOnSuccessListener { codes -> codes.firstOrNull()?.rawValue?.let(onQr) }
        .addOnCompleteListener { proxy.close() }
}

