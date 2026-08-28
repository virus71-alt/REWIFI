package com.rewifi.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import java.util.concurrent.Executors

/**
 * In-app QR scanner built on CameraX + ML Kit. Binds the standard back camera
 * ([CameraSelector.DEFAULT_BACK_CAMERA] — the regular lens, not the ultra-wide
 * one the old ZXing activity grabbed) and reports the first WiFi QR it sees.
 *
 * Because it's a real Compose screen on the back stack, the system Back button
 * pops to the vault instead of dropping the user out to the home screen.
 */
@Composable
fun ScannerScreen(
    onBack: () -> Unit,
    onResult: (ssid: String, password: String, security: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

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

    // Guard so we only act on the first valid WiFi QR.
    var handled by remember { mutableStateOf(false) }

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
                    val analysisExecutor = Executors.newSingleThreadExecutor()
                    val scanner = BarcodeScanning.getClient(
                        BarcodeScannerOptions.Builder()
                            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                            .build()
                    )

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
        Row(
            Modifier.fillMaxWidth().systemBarsPadding().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Snow)
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Yellow)
                    .border(3.dp, Ink, RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) { Text("POINT AT A WIFI QR", color = Ink, fontWeight = FontWeight.Black, fontSize = 13.sp) }
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
