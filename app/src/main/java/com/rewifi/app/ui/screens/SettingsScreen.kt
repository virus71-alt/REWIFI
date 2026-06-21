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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Snow

@Composable
fun SettingsScreen(
    appLock: Boolean,
    autoLockMinutes: Int,
    backupConfigured: Boolean,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onCycleAutoLock: () -> Unit,
    onOpenBackupSetup: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Paper).systemBarsPadding()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Snow)
                        .border(3.dp, Ink, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Ink) }
                Spacer(Modifier.width(14.dp))
                Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 30.sp, color = Ink)
            }

            SettingRow(
                title = "APP LOCK",
                subtitle = if (biometricAvailable)
                    "Require fingerprint / PIN to open the vault"
                else
                    "Set a screen lock on this device to use this",
                checked = appLock && biometricAvailable,
                enabled = biometricAvailable,
                onChange = onToggleAppLock
            )

            if (appLock && biometricAvailable) {
                NavRow(
                    title = "AUTO-LOCK",
                    subtitle = "Re-lock after " + when (autoLockMinutes) {
                        0 -> "leaving the app"
                        1 -> "1 minute"
                        else -> "$autoLockMinutes minutes"
                    } + " · tap to change",
                    onClick = onCycleAutoLock
                )
            }

            NavRow(
                title = "BACKUP & SYNC",
                subtitle = if (backupConfigured) "Drive connected · manage backup & restore"
                           else "Connect Google Drive to back up automatically",
                onClick = onOpenBackupSetup
            )
        }
    }
}

/** Tappable settings row that opens another screen. */
@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    BrutalCard(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        padding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text("›", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Ink)
        }
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    onChange: (Boolean) -> Unit
) {
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Muted, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Spacer(Modifier.width(14.dp))
            BrutalSwitch(checked, enabled, onChange)
        }
    }
}

/** Chunky brutalist on/off toggle. */
@Composable
private fun BrutalSwitch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val track = when {
        !enabled -> Muted.copy(alpha = 0.35f)
        checked -> Green
        else -> Muted
    }
    Box(
        Modifier.width(60.dp).height(34.dp).clip(RoundedCornerShape(17.dp))
            .background(track)
            .border(3.dp, Ink, RoundedCornerShape(17.dp))
            .clickable(enabled = enabled) { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier.padding(3.dp).size(24.dp).clip(CircleShape).background(Snow)
                .border(3.dp, Ink, CircleShape)
        )
    }
}
