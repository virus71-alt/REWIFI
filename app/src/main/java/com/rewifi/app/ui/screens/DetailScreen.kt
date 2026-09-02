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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
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
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

@Composable
fun DetailScreen(
    cred: WifiCred,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onWriteNfc: () -> Unit,
    onToggleFavorite: (Boolean) -> Unit = {}
) {
    val ctx = LocalContext.current
    val colors = RewifiTheme.colors
    var reveal by remember { mutableStateOf(false) }
    var bigQr by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val qr = remember(cred.id, cred.ssid, cred.password) {
        runCatching { QrGenerator.build(ctx, cred.ssid, cred.password) }.getOrNull()
    }

    Column(
        Modifier.fillMaxSize().background(colors.background).systemBarsPadding().padding(20.dp)
    ) {
      Column(
        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(18.dp)
      ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.height(44.dp).background(colors.surface, RoundedCornerShape(12.dp))
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary) }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier.height(44.dp).background(if (cred.isFavorite) Yellow else colors.surface, RoundedCornerShape(12.dp))
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { onToggleFavorite(!cred.isFavorite) }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (cred.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    "Favorite",
                    tint = if (cred.isFavorite) Ink else colors.textPrimary
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(
                Modifier.height(44.dp).background(Red, RoundedCornerShape(12.dp))
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { confirmDelete = true }.padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Delete, "Delete", tint = Snow) }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(cred.ssid, fontWeight = FontWeight.Black, fontSize = 30.sp, color = colors.textPrimary, maxLines = 2, modifier = Modifier.weight(1f, fill = false))
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .border(2.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    cred.category.uppercase(),
                    color = colors.textPrimary,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // QR — scan with phone camera to join instantly (the "restore" path)
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("SCAN TO CONNECT", fontWeight = FontWeight.Black, fontSize = 13.sp,
                    color = colors.textSecondary, letterSpacing = 1.sp)
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
                    Text("Tap to enlarge · share for guests", color = colors.textSecondary,
                        fontWeight = FontWeight.Medium, fontSize = 12.sp)
                } else {
                    Text("Could not render QR", color = colors.textSecondary)
                }
            }
        }

        // Share QR + write to an NFC tag.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            BrutalButton("SHARE QR", Modifier.weight(1f), bg = Green, fg = Ink) {
                shareQrImage(ctx, cred.ssid, cred.password)
            }
            BrutalButton("NFC TAG", Modifier.weight(1f), bg = colors.surface, fg = colors.textPrimary, onClick = onWriteNfc)
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
                    Pill(if (reveal) "HIDE" else "REVEAL", colors.surface, colors.textPrimary) { reveal = !reveal }
                    Pill("COPY", Green, Ink) { copy(ctx, cred.password) }
                }
            }
        }

        if (!cred.note.isNullOrBlank()) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(Modifier.fillMaxWidth()) {
                    Text("NOTE", fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.textSecondary, letterSpacing = 1.sp)
                    Spacer(Modifier.height(6.dp))
                    Text(cred.note, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = colors.textPrimary)
                }
            }
        }
      }

      // Pinned at the bottom, always visible regardless of content length.
      Spacer(Modifier.height(14.dp))
      BrutalButton("EDIT", Modifier.fillMaxWidth(), bg = colors.surface, fg = colors.textPrimary, onClick = onEdit)
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
private fun Pill(
    text: String,
    bg: androidx.compose.ui.graphics.Color,
    fg: androidx.compose.ui.graphics.Color = RewifiTheme.colors.textPrimary,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Box(
        Modifier.background(bg, RoundedCornerShape(10.dp))
            .border(3.dp, colors.border, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 10.dp)
    ) { Text(text, fontWeight = FontWeight.Black, fontSize = 13.sp, color = fg) }
}

@Composable
private fun DeleteDialog(ssid: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    val colors = RewifiTheme.colors
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.55f))
            .clickable(onClick = onCancel),
        contentAlignment = Alignment.Center
    ) {
        BrutalCard(Modifier.fillMaxWidth().padding(28.dp), padding = PaddingValues(22.dp)) {
            Column(Modifier.fillMaxWidth()) {
                Text("DELETE “$ssid”?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                Spacer(Modifier.height(8.dp))
                Text("This removes the saved password from your vault. Cannot be undone.",
                    color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pill("CANCEL", colors.surfaceVariant, colors.textPrimary, onCancel)
                    Pill("DELETE", Red, Snow, onConfirm)
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
