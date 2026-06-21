package com.rewifi.app.ui.screens

import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

/**
 * The one-time "set up backup" flow. Shown after the splash on first launch
 * (skippable), and reachable later from Settings. Connecting Google Drive is the
 * whole setup now — the backup is encrypted automatically using the signed-in
 * account, so there's no passphrase to set or remember.
 */
@Composable
fun SetupScreen(
    firstRun: Boolean,
    driveEmail: String?,
    lastBackupAt: Long = 0L,
    onConnectDrive: () -> Unit,
    onDisconnectDrive: () -> Unit,
    onRestoreFromDrive: () -> Unit,
    onSyncNow: () -> Unit = {},
    onFinish: () -> Unit,
    onSkip: (() -> Unit)? = null
) {
    val driveConnected = driveEmail != null

    BackHandler { (onSkip ?: onFinish)() }

    Box(Modifier.fillMaxSize().background(Paper).systemBarsPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top bar — back arrow only when launched from Settings.
            if (!firstRun) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Snow)
                            .border(3.dp, Ink, RoundedCornerShape(12.dp))
                            .clickable(onClick = onFinish),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                    Spacer(Modifier.width(14.dp))
                    Text("BACKUP & SYNC", fontWeight = FontWeight.Black, fontSize = 24.sp, color = Ink)
                }
            } else {
                Box(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Yellow)
                        .border(3.dp, Ink, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) { Text("REWIFI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink) }
                Text(
                    "NEVER LOSE\nA WIFI AGAIN", fontWeight = FontWeight.Black, fontSize = 34.sp,
                    color = Ink, lineHeight = 36.sp
                )
                Text(
                    "Set up a backup so your saved networks survive a reset, crash, or new phone.",
                    color = Muted, fontWeight = FontWeight.Medium, fontSize = 13.sp
                )
            }

            // --- Google Drive ---
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudUpload, null, tint = Ink, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("GOOGLE DRIVE", fontWeight = FontWeight.Black, fontSize = 15.sp, color = Ink)
                        Spacer(Modifier.weight(1f))
                        if (driveConnected) StatusChip("ON", Green, check = true)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        if (driveConnected)
                            "Synced to $driveEmail. Backs up automatically after every change and once a day, into a REWIFI folder in your Drive."
                        else
                            "Connect your Google Drive and we'll keep an encrypted backup in a REWIFI folder — no passphrase needed. A new phone restores in seconds.",
                        color = Muted, fontWeight = FontWeight.Medium, fontSize = 12.sp
                    )
                    if (driveConnected) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (lastBackupAt > 0L)
                                "Last synced ${DateUtils.getRelativeTimeSpanString(lastBackupAt)}"
                            else
                                "Not synced yet — tap Sync now",
                            color = Ink, fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    if (!driveConnected) {
                        BrutalButton(
                            "CONNECT DRIVE", modifier = Modifier.fillMaxWidth(), bg = Yellow, fg = Ink
                        ) { onConnectDrive() }
                    } else {
                        // SYNC NOW is hidden during first-run sign-in/sign-up — connecting
                        // Drive already kicks off a backup, so it'd be redundant there.
                        // It stays available when this screen is opened from Settings.
                        if (!firstRun) {
                            BrutalButton(
                                "SYNC NOW", modifier = Modifier.fillMaxWidth(), bg = Yellow, fg = Ink
                            ) { onSyncNow() }
                            Spacer(Modifier.height(10.dp))
                        }
                        BrutalButton(
                            "RESTORE FROM DRIVE", modifier = Modifier.fillMaxWidth(), bg = Ink, fg = Yellow
                        ) { onRestoreFromDrive() }
                        Spacer(Modifier.height(10.dp))
                        Box(
                            Modifier.fillMaxWidth().clickable { onDisconnectDrive() }.padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("DISCONNECT", color = Red, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // --- Footer ---
            if (firstRun) {
                BrutalButton("CONTINUE", modifier = Modifier.fillMaxWidth(), bg = Ink, fg = Yellow) {
                    onFinish()
                }
                Box(
                    Modifier.fillMaxWidth().clickable { (onSkip ?: onFinish)() }.padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SKIP FOR NOW", color = Muted, fontWeight = FontWeight.Black, fontSize = 13.sp)
                }
            } else {
                BrutalButton("DONE", modifier = Modifier.fillMaxWidth(), bg = Ink, fg = Yellow) { onFinish() }
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, bg: androidx.compose.ui.graphics.Color, check: Boolean) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(bg)
            .border(2.dp, Ink, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (check) {
                Icon(Icons.Default.Check, null, tint = Ink, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
            }
            Text(text, color = Ink, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}
