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
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Yellow

@Composable
fun SettingsScreen(
    appTheme: String,
    appLock: Boolean,
    autoLockMinutes: Int,
    backupConfigured: Boolean,
    biometricAvailable: Boolean,
    onBack: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onToggleAppLock: (Boolean) -> Unit,
    onCycleAutoLock: () -> Unit,
    onOpenBackupSetup: () -> Unit
) {
    val colors = RewifiTheme.colors

    Box(Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(colors.surface)
                        .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center
                ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary) }
                Spacer(Modifier.width(14.dp))
                Text("SETTINGS", fontWeight = FontWeight.Black, fontSize = 30.sp, color = colors.textPrimary)
            }

            ThemeSettingRow(appTheme = appTheme, onSelectTheme = onSelectTheme)

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

@Composable
private fun ThemeSettingRow(appTheme: String, onSelectTheme: (String) -> Unit) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column {
            Text("APP THEME", fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("Choose your visual style", color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ThemeOptionButton(
                    text = "LIGHT",
                    selected = appTheme == SettingsStore.THEME_LIGHT,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTheme(SettingsStore.THEME_LIGHT) }
                )
                ThemeOptionButton(
                    text = "DARK",
                    selected = appTheme == SettingsStore.THEME_DARK,
                    modifier = Modifier.weight(1f),
                    onClick = { onSelectTheme(SettingsStore.THEME_DARK) }
                )
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    text: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    val bg = if (selected) Yellow else colors.surfaceVariant
    val fg = if (selected) Ink else colors.textPrimary
    val borderColor = if (selected) colors.border else colors.border.copy(alpha = 0.5f)

    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(3.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = fg,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

/** Tappable settings row that opens another screen. */
@Composable
private fun NavRow(title: String, subtitle: String, onClick: () -> Unit) {
    val colors = RewifiTheme.colors
    BrutalCard(
        Modifier.fillMaxWidth().clickable(onClick = onClick),
        padding = PaddingValues(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Spacer(Modifier.width(14.dp))
            Text("›", fontWeight = FontWeight.Black, fontSize = 22.sp, color = colors.textPrimary)
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
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }
            Spacer(Modifier.width(14.dp))
            BrutalSwitch(checked, enabled, onChange)
        }
    }
}

/** Chunky brutalist on/off toggle. */
@Composable
private fun BrutalSwitch(checked: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) {
    val colors = RewifiTheme.colors
    val track = when {
        !enabled -> colors.textSecondary.copy(alpha = 0.35f)
        checked -> Green
        else -> colors.surfaceVariant
    }
    Box(
        Modifier.width(60.dp).height(34.dp).clip(RoundedCornerShape(17.dp))
            .background(track)
            .border(3.dp, colors.border, RoundedCornerShape(17.dp))
            .clickable(enabled = enabled) { onChange(!checked) },
        contentAlignment = if (checked) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Box(
            Modifier.padding(3.dp).size(24.dp).clip(CircleShape).background(colors.surface)
                .border(3.dp, colors.border, CircleShape)
        )
    }
}

