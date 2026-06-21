package com.rewifi.app.ui.screens

import android.app.Activity
import android.nfc.NfcAdapter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.NfcWriter
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

private enum class NfcStatus { WAITING, DONE, FAILED, DISABLED, UNSUPPORTED }

/**
 * Writes a WiFi network to an NFC tag. While shown, it puts the activity into NFC
 * reader mode and writes a Wi-Fi Simple Config record to the first tag tapped.
 */
@Composable
fun NfcWriteScreen(
    ssid: String,
    password: String,
    security: String,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val activity = ctx as? Activity
    val adapter = remember { NfcAdapter.getDefaultAdapter(ctx) }
    val open = password.isEmpty() || security.isBlank() || security.equals("nopass", true)
    val message = remember(ssid, password) { NfcWriter.wifiNdef(ssid, password, open) }

    var status by remember {
        mutableStateOf(
            when {
                adapter == null -> NfcStatus.UNSUPPORTED
                !adapter.isEnabled -> NfcStatus.DISABLED
                else -> NfcStatus.WAITING
            }
        )
    }

    BackHandler { onBack() }

    DisposableEffect(adapter, status == NfcStatus.WAITING) {
        if (adapter != null && activity != null && adapter.isEnabled && status == NfcStatus.WAITING) {
            val callback = NfcAdapter.ReaderCallback { tag ->
                val ok = NfcWriter.write(tag, message)
                activity.runOnUiThread { status = if (ok) NfcStatus.DONE else NfcStatus.FAILED }
            }
            adapter.enableReaderMode(
                activity, callback,
                NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                    NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V or
                    NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                null
            )
        }
        onDispose { if (activity != null) adapter?.disableReaderMode(activity) }
    }

    Column(
        Modifier.fillMaxSize().background(Paper).systemBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Snow)
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
            Spacer(Modifier.width(14.dp))
            Text("WRITE NFC TAG", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Ink)
        }

        val (bg, title, body) = when (status) {
            NfcStatus.WAITING -> Triple(Yellow, "TAP A TAG",
                "Hold an empty NFC tag to the back of your phone to write \"$ssid\".")
            NfcStatus.DONE -> Triple(Green, "TAG WRITTEN",
                "Anyone can now tap their phone to this tag to join \"$ssid\".")
            NfcStatus.FAILED -> Triple(Red, "WRITE FAILED",
                "Couldn't write to that tag. Try another tag — it may be read-only or too small.")
            NfcStatus.DISABLED -> Triple(Red, "NFC IS OFF",
                "Turn on NFC in your phone settings, then come back.")
            NfcStatus.UNSUPPORTED -> Triple(Muted, "NO NFC",
                "This phone doesn't have NFC. You can still share the QR code instead.")
        }

        BrutalCard(Modifier.fillMaxWidth(), bg = bg, padding = PaddingValues(24.dp)) {
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Nfc, null, tint = Ink, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(16.dp))
                Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text(body, color = Ink, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                    textAlign = TextAlign.Center)
            }
        }

        if (status == NfcStatus.FAILED) {
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Snow)
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable { status = NfcStatus.WAITING }.padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) { Text("TRY AGAIN", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink) }
        }
    }
}
