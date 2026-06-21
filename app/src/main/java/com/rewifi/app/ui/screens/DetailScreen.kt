package com.rewifi.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.QrGenerator
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

@Composable
fun DetailScreen(
    cred: WifiCred,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWriteNfc: () -> Unit
) {
    val ctx = LocalContext.current
    var reveal by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var bigQr by remember { mutableStateOf(false) }
    val qr = remember(cred.id, cred.ssid, cred.password) {
        runCatching { QrGenerator.build(ctx, cred.ssid, cred.password) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().background(Paper).systemBarsPadding().padding(20.dp)
    ) {
      Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.height(44.dp).background(Snow, RoundedCornerShape(12.dp))
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, "Back", tint = Ink) }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.height(44.dp).background(Red, RoundedCornerShape(12.dp))
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable { confirmDelete = true }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Delete, "Delete", tint = Snow) }
        }

        Text(cred.ssid, fontWeight = FontWeight.Black, fontSize = 30.sp, color = Ink, maxLines = 2)

        // QR — scan with phone camera to join instantly (the "restore" for non-root)
        BrutalCard(Modifier.fillMaxWidth(), bg = Snow, padding = PaddingValues(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("SCAN TO CONNECT", fontWeight = FontWeight.Black, fontSize = 13.sp,
                    color = Muted, letterSpacing = 1.sp)
                Spacer(Modifier.height(14.dp))
                if (qr != null) {
                    Image(
                        bitmap = qr,
                        contentDescription = "WiFi QR",
                        filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { bigQr = true }
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("Tap to enlarge · share for guests", color = Muted,
                        fontWeight = FontWeight.Medium, fontSize = 12.sp)
                } else {
                    Text("Could not render QR", color = Muted)
                }
            }
        }

        // Share QR + write to an NFC tag.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrutalButton("SHARE QR", Modifier.weight(1f), bg = Green, fg = Ink) {
                shareQrImage(ctx, cred.ssid, cred.password)
            }
            BrutalButton("NFC TAG", Modifier.weight(1f), bg = Snow, fg = Ink, onClick = onWriteNfc)
        }

        // Password reveal + copy
        BrutalCard(Modifier.fillMaxWidth(), bg = Yellow, padding = PaddingValues(16.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text("PASSWORD", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Ink, letterSpacing = 1.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (reveal) cred.password else "•".repeat(cred.password.length.coerceIn(6, 16)),
                    fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Black,
                    fontSize = 22.sp, color = Ink, maxLines = 2
                )
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill(if (reveal) "HIDE" else "REVEAL", Snow) { reveal = !reveal }
                    Pill("COPY", Green) { copy(ctx, cred.password) }
                }
            }
        }

        if (!cred.note.isNullOrBlank()) {
            BrutalCard(Modifier.fillMaxWidth(), bg = Snow, padding = PaddingValues(14.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text("NOTE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Muted, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(cred.note, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = Ink)
                }
            }
        }
      }

      // Pinned at the bottom, always visible regardless of content length.
      Spacer(Modifier.height(14.dp))
      BrutalButton("EDIT", Modifier.fillMaxWidth(), bg = Ink, fg = Snow, onClick = onEdit)
    }

    if (confirmDelete) {
        DeleteDialog(cred.ssid, onCancel = { confirmDelete = false }) {
            confirmDelete = false; onDelete(); onBack()
        }
    }

    if (bigQr && qr != null) {
        Box(
            Modifier.fillMaxSize().background(Ink.copy(alpha = 0.85f))
                .clickable { bigQr = false }.padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = qr,
                    contentDescription = "WiFi QR",
                    filterQuality = androidx.compose.ui.graphics.FilterQuality.None,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f)
                        .clip(RoundedCornerShape(12.dp)).background(Snow).padding(16.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(cred.ssid, color = Snow, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Point a camera here to join", color = Snow.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
    }
}

/** Render the WiFi QR to a PNG and open the system share sheet. */
private fun shareQrImage(ctx: Context, ssid: String, password: String) {
    runCatching {
        val bmp = QrGenerator.buildBitmap(ctx, ssid, password, size = 800)
        val dir = java.io.File(ctx.cacheDir, "qr").apply { mkdirs() }
        val file = java.io.File(dir, "rewifi-qr.png")
        java.io.FileOutputStream(file).use { bmp.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
        val uri = androidx.core.content.FileProvider.getUriForFile(
            ctx, "${ctx.packageName}.fileprovider", file
        )
        val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            putExtra(android.content.Intent.EXTRA_TEXT, "Join \"$ssid\" — scan this WiFi QR")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(android.content.Intent.createChooser(send, "Share WiFi QR"))
    }.onFailure {
        Toast.makeText(ctx, "Share failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

@Composable
private fun Pill(text: String, bg: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Box(
        Modifier.background(bg, RoundedCornerShape(10.dp))
            .border(3.dp, Ink, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) { Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp, color = Ink) }
}

@Composable
private fun DeleteDialog(ssid: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        BrutalCard(Modifier.fillMaxWidth().padding(28.dp), bg = Snow, padding = PaddingValues(22.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text("DELETE “$ssid”?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text("This removes the saved password from your vault. Cannot be undone.",
                    color = Muted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("CANCEL", Snow, onCancel)
                    Pill("DELETE", Red, onConfirm)
                }
            }
        }
    }
}

private fun copy(ctx: Context, text: String) {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("wifi password", text))
    Toast.makeText(ctx, "Password copied", Toast.LENGTH_SHORT).show()
}
