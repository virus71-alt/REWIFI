package com.rewifi.app.ui.screens

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.vault.Flash
import com.rewifi.app.vault.SyncState
import com.rewifi.app.ui.theme.Blue
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

private val accents = listOf(Blue, Green, Red, Yellow)

@Composable
fun VaultScreen(
    creds: List<WifiCred>,
    syncState: SyncState,
    flash: Flash?,
    onAdd: () -> Unit,
    onOpen: (WifiCred) -> Unit,
    onBackup: () -> Unit,
    onScan: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Paper).systemBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Header(creds.size, onBackup, onSettings, onSync) }
            if (creds.isEmpty()) {
                item { EmptyState() }
            } else {
                items(creds, key = { it.id }) { c ->
                    WifiRow(c, accents[(c.id % accents.size).toInt()]) { onOpen(c) }
                }
            }
        }

        // FAB — opens the "manual or scan" chooser.
        SquareButton(
            icon = Icons.Default.Add,
            bg = Yellow,
            iconTint = Ink,
            size = 64.dp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
        ) { showAddMenu = true }

        if (showAddMenu) {
            AddMenu(
                onDismiss = { showAddMenu = false },
                onManual = { showAddMenu = false; onAdd() },
                onScan = { showAddMenu = false; onScan() }
            )
        }

        if (syncState != SyncState.IDLE) SyncOverlay(syncState)

        // Transient banner (e.g. after scanning + connecting to a network).
        flash?.let { f ->
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomCenter) {
                BrutalCard(bg = if (f.ok) Green else Red, padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (f.ok) Icons.Default.Check else Icons.Default.Close,
                            null, tint = Ink, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(f.title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink)
                    }
                }
            }
        }
    }
}

/** Full-screen "Syncing… / Synced" feedback for the manual sync button. */
@Composable
private fun SyncOverlay(state: SyncState) {
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        BrutalCard(bg = Snow, padding = PaddingValues(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    SyncState.SYNCING ->
                        CircularProgressIndicator(color = Ink, strokeWidth = 4.dp, modifier = Modifier.size(48.dp))
                    SyncState.SYNCED -> StatusBubble(Icons.Default.Check, Green, Ink)
                    SyncState.FAILED -> StatusBubble(Icons.Default.Close, Red, Snow)
                    SyncState.IDLE -> {}
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    when (state) {
                        SyncState.SYNCING -> "SYNCING…"
                        SyncState.SYNCED -> "SYNCED"
                        SyncState.FAILED -> "SYNC FAILED"
                        SyncState.IDLE -> ""
                    },
                    fontWeight = FontWeight.Black, fontSize = 18.sp, color = Ink
                )
            }
        }
    }
}

@Composable
private fun StatusBubble(icon: ImageVector, bg: Color, iconTint: Color) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(bg).border(3.dp, Ink, CircleShape),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp)) }
}

/** Bottom-anchored chooser shown when the + is tapped. */
@Composable
private fun AddMenu(onDismiss: () -> Unit, onManual: () -> Unit, onScan: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f)).clickable(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).padding(bottom = 78.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AddOption(Icons.Default.PhotoCamera, "SCAN QR", Blue, Snow, onScan)
            AddOption(Icons.Default.Edit, "ADD MANUALLY", Yellow, Ink, onManual)
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    Box {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(Ink))
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
                .border(3.dp, Ink, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = fg, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

/** Brutalist square icon button with a hard offset shadow (used for the FAB + backup). */
@Composable
private fun SquareButton(
    icon: ImageVector,
    bg: Color,
    iconTint: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier.size(size)) {
        Box(Modifier.matchParentSize().offset(5.dp, 5.dp).clip(RoundedCornerShape(18.dp)).background(Ink))
        Box(
            Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(bg)
                .border(3.dp, Ink, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(size * 0.46f)) }
    }
}

@Composable
private fun Header(count: Int, onBackup: () -> Unit, onSettings: () -> Unit, onSync: () -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Yellow)
                    .border(3.dp, Ink, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("REWIFI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink) }
            Spacer(Modifier.weight(1f))
            // Sync + settings + backup = matching square brutalist buttons, mirroring the FAB.
            SquareButton(Icons.Default.Sync, Green, Ink, 46.dp, onClick = onSync)
            Spacer(Modifier.width(10.dp))
            SquareButton(Icons.Default.Settings, Snow, Ink, 46.dp, onClick = onSettings)
            Spacer(Modifier.width(10.dp))
            SquareButton(Icons.Default.Upload, Snow, Ink, 46.dp, onClick = onBackup)
        }
        Spacer(Modifier.height(14.dp))
        Text("YOUR\nWIFI VAULT", fontWeight = FontWeight.Black, fontSize = 38.sp,
            color = Ink, lineHeight = 40.sp)
        Spacer(Modifier.height(6.dp))
        Text("$count saved network${if (count == 1) "" else "s"} · encrypted",
            color = Muted, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun WifiRow(c: WifiCred, accent: Color, onClick: () -> Unit) {
    BrutalCard(Modifier.fillMaxWidth().clickable(onClick = onClick), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent)
                    .border(3.dp, Ink, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Wifi, null, tint = Ink, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.ssid, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = Ink, maxLines = 1)
                Text("•••••••••", color = Muted, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Text("OPEN ›", fontWeight = FontWeight.Black, fontSize = 12.sp, color = Ink)
        }
    }
}

@Composable
private fun EmptyState() {
    BrutalCard(Modifier.fillMaxWidth(), bg = Snow, padding = PaddingValues(28.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(Yellow)
                    .border(3.dp, Ink, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Wifi, null, tint = Ink, modifier = Modifier.size(36.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("NO NETWORKS YET", fontWeight = FontWeight.Black, fontSize = 18.sp, color = Ink)
            Spacer(Modifier.height(6.dp))
            Text("Tap + to save your first cafe WiFi.\nIt survives every phone reset.",
                color = Muted, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}
