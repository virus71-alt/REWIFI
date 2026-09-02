package com.rewifi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.window.Dialog
import com.rewifi.app.data.AppLockType
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.components.PinSetupDialog
import com.rewifi.app.ui.components.PinSetupMode
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

@Composable
fun SettingsScreen(
    appTheme: String,
    appLockType: AppLockType,
    hasPin: Boolean,
    pinLength: Int,
    autoLockMinutes: Int,
    backupConfigured: Boolean,
    biometricAvailable: Boolean,
    customCategories: List<String>,
    clipboardClearSeconds: Int = 30,
    onBack: () -> Unit,
    onSelectTheme: (String) -> Unit,
    onSetAppLockType: (AppLockType) -> Unit,
    onSetPin: (String, Int) -> Unit,
    onVerifyPin: (String) -> Boolean,
    onClearPin: () -> Unit,
    onCycleAutoLock: () -> Unit,
    onSelectClipboardClearSeconds: (Int) -> Unit = {},
    onOpenBackupSetup: () -> Unit,
    onCreateCategory: (String) -> Boolean,
    onRenameCategory: (String, String) -> Boolean,
    onDeleteCategory: (String) -> Unit
) {
    val colors = RewifiTheme.colors
    var pinDialogMode by remember { mutableStateOf<PinSetupMode?>(null) }
    var pendingLockType by remember { mutableStateOf<AppLockType?>(null) }

    Box(Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
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

            CategoryManagementCard(
                customCategories = customCategories,
                onCreateCategory = onCreateCategory,
                onRenameCategory = onRenameCategory,
                onDeleteCategory = onDeleteCategory
            )

            AppLockManagementCard(
                appLockType = appLockType,
                hasPin = hasPin,
                pinLength = pinLength,
                autoLockMinutes = autoLockMinutes,
                biometricAvailable = biometricAvailable,
                onSelectLockType = { targetType ->
                    when (targetType) {
                        AppLockType.OFF -> {
                            if (hasPin && (appLockType == AppLockType.PIN || appLockType == AppLockType.PIN_AND_BIOMETRIC)) {
                                pendingLockType = AppLockType.OFF
                                pinDialogMode = PinSetupMode.VERIFY_TO_DISABLE
                            } else {
                                onSetAppLockType(AppLockType.OFF)
                            }
                        }
                        AppLockType.PIN, AppLockType.PIN_AND_BIOMETRIC -> {
                            if (!hasPin) {
                                pendingLockType = targetType
                                pinDialogMode = PinSetupMode.CREATE
                            } else {
                                onSetAppLockType(targetType)
                            }
                        }
                        AppLockType.BIOMETRIC -> {
                            if (hasPin && (appLockType == AppLockType.PIN || appLockType == AppLockType.PIN_AND_BIOMETRIC)) {
                                pendingLockType = AppLockType.BIOMETRIC
                                pinDialogMode = PinSetupMode.VERIFY_TO_DISABLE
                            } else {
                                onSetAppLockType(AppLockType.BIOMETRIC)
                            }
                        }
                    }
                },
                onRequestChangePin = {
                    pinDialogMode = PinSetupMode.CHANGE
                },
                onRequestDisablePin = { targetType ->
                    pendingLockType = targetType
                    pinDialogMode = PinSetupMode.VERIFY_TO_DISABLE
                },
                onCycleAutoLock = onCycleAutoLock
            )

            AutoClearClipboardCard(
                clipboardClearSeconds = clipboardClearSeconds,
                onSelectDuration = onSelectClipboardClearSeconds
            )

            NavRow(
                title = "BACKUP & SYNC",
                subtitle = if (backupConfigured) "Drive connected · manage backup & restore"
                           else "Connect Google Drive to back up automatically",
                onClick = onOpenBackupSetup
            )

            Spacer(Modifier.height(20.dp))
        }

        if (pinDialogMode != null) {
            PinSetupDialog(
                mode = pinDialogMode!!,
                currentPinLength = pinLength,
                onVerifyCurrentPin = onVerifyPin,
                onPinConfirmed = { pin, length ->
                    onSetPin(pin, length)
                    onSetAppLockType(pendingLockType ?: AppLockType.PIN)
                    pinDialogMode = null
                    pendingLockType = null
                },
                onVerifiedToDisable = {
                    onClearPin()
                    onSetAppLockType(pendingLockType ?: AppLockType.OFF)
                    pinDialogMode = null
                    pendingLockType = null
                },
                onDismiss = {
                    pinDialogMode = null
                    pendingLockType = null
                }
            )
        }
    }
}

@Composable
private fun AppLockManagementCard(
    appLockType: AppLockType,
    hasPin: Boolean,
    pinLength: Int,
    autoLockMinutes: Int,
    biometricAvailable: Boolean,
    onSelectLockType: (AppLockType) -> Unit,
    onRequestChangePin: () -> Unit,
    onRequestDisablePin: (targetType: AppLockType) -> Unit,
    onCycleAutoLock: () -> Unit
) {
    val colors = RewifiTheme.colors

    BrutalCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "APP LOCK",
                        fontWeight = FontWeight.Black,
                        fontSize = 16.sp,
                        color = colors.textPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        "Protect vault with PIN or Biometrics",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Status badge
                val statusText = when (appLockType) {
                    AppLockType.OFF -> "OFF"
                    AppLockType.PIN -> "PIN ($pinLength-D)"
                    AppLockType.BIOMETRIC -> "BIOMETRIC"
                    AppLockType.PIN_AND_BIOMETRIC -> "PIN + BIO"
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (appLockType == AppLockType.OFF) colors.surfaceVariant else Yellow)
                        .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        statusText,
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (appLockType == AppLockType.OFF) colors.textSecondary else Ink
                    )
                }
            }

            // 4 Options Grid (2x2)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LockModeButton(
                        title = "OFF",
                        selected = appLockType == AppLockType.OFF,
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        onSelectLockType(AppLockType.OFF)
                    }

                    LockModeButton(
                        title = "REWIFI PIN",
                        selected = appLockType == AppLockType.PIN,
                        enabled = true,
                        modifier = Modifier.weight(1f)
                    ) {
                        onSelectLockType(AppLockType.PIN)
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LockModeButton(
                        title = "BIOMETRIC",
                        selected = appLockType == AppLockType.BIOMETRIC,
                        enabled = biometricAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        onSelectLockType(AppLockType.BIOMETRIC)
                    }

                    LockModeButton(
                        title = "PIN + BIO",
                        selected = appLockType == AppLockType.PIN_AND_BIOMETRIC,
                        enabled = biometricAvailable,
                        modifier = Modifier.weight(1f)
                    ) {
                        onSelectLockType(AppLockType.PIN_AND_BIOMETRIC)
                    }
                }
            }

            // Change PIN Action if PIN is active
            if (hasPin && (appLockType == AppLockType.PIN || appLockType == AppLockType.PIN_AND_BIOMETRIC)) {
                BrutalButton(
                    text = "CHANGE REWIFI PIN",
                    modifier = Modifier.fillMaxWidth(),
                    bg = colors.surfaceVariant,
                    fg = colors.textPrimary,
                    onClick = onRequestChangePin
                )
            }

            // Auto-lock row if any lock is enabled
            if (appLockType != AppLockType.OFF) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant)
                        .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .clickable(onClick = onCycleAutoLock)
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                "AUTO-LOCK TIMEOUT",
                                fontWeight = FontWeight.Black,
                                fontSize = 11.sp,
                                color = colors.textSecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                when (autoLockMinutes) {
                                    0 -> "Re-lock immediately upon leaving"
                                    1 -> "Re-lock after 1 minute"
                                    else -> "Re-lock after $autoLockMinutes minutes"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                        }
                        Text("CHANGE", fontWeight = FontWeight.Black, fontSize = 11.sp, color = Yellow)
                    }
                }
            }
        }
    }
}

@Composable
private fun LockModeButton(
    title: String,
    selected: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                when {
                    !enabled -> colors.surfaceVariant.copy(alpha = 0.4f)
                    selected -> Yellow
                    else -> colors.surface
                }
            )
            .border(
                2.dp,
                if (selected) colors.border else colors.border.copy(alpha = 0.4f),
                RoundedCornerShape(10.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            title,
            fontWeight = FontWeight.Black,
            fontSize = 12.sp,
            color = when {
                !enabled -> colors.textSecondary.copy(alpha = 0.4f)
                selected -> Ink
                else -> colors.textPrimary
            }
        )
    }
}

@Composable
private fun AutoClearClipboardCard(
    clipboardClearSeconds: Int,
    onSelectDuration: (Int) -> Unit
) {
    val colors = RewifiTheme.colors
    val durations = listOf(0 to "OFF", 15 to "15s", 30 to "30s", 60 to "1m", 120 to "2m")

    BrutalCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "AUTO-CLEAR COPIED PASSWORD",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        if (clipboardClearSeconds == 0) "Clipboard clearing disabled"
                        else "Clears copied password after " + when (clipboardClearSeconds) {
                            15 -> "15 seconds"
                            30 -> "30 seconds"
                            60 -> "1 minute"
                            120 -> "2 minutes"
                            else -> "$clipboardClearSeconds seconds"
                        },
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durations.forEach { (sec, label) ->
                    val selected = clipboardClearSeconds == sec
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) Yellow else colors.surface)
                            .border(
                                2.dp,
                                if (selected) colors.border else colors.border.copy(alpha = 0.4f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectDuration(sec) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = if (selected) Ink else colors.textPrimary
                        )
                    }
                }
            }
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

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun CategoryManagementCard(
    customCategories: List<String>,
    onCreateCategory: (String) -> Boolean,
    onRenameCategory: (String, String) -> Boolean,
    onDeleteCategory: (String) -> Unit
) {
    val colors = RewifiTheme.colors
    var showCreateDialog by remember { mutableStateOf(false) }
    var createName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }

    var categoryToRename by remember { mutableStateOf<String?>(null) }
    var renameText by remember { mutableStateOf("") }
    var renameError by remember { mutableStateOf<String?>(null) }

    var categoryToDelete by remember { mutableStateOf<String?>(null) }

    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column {
                Text("CATEGORIES", fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
                Spacer(Modifier.height(4.dp))
                Text("Organize and tag your saved WiFi networks", color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 12.sp)
            }

            // Built-in categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("BUILT-IN (DEFAULT)", fontWeight = FontWeight.Black, fontSize = 11.sp, color = colors.textSecondary, letterSpacing = 1.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SettingsStore.BUILTIN_CATEGORIES.forEach { cat ->
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                                .border(2.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cat.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Custom categories
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CUSTOM CATEGORIES", fontWeight = FontWeight.Black, fontSize = 11.sp, color = colors.textSecondary, letterSpacing = 1.sp)
                if (customCategories.isEmpty()) {
                    Text("No custom categories added yet.", color = colors.textSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                } else {
                    customCategories.forEach { customCat ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.surfaceVariant)
                                .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                customCat.uppercase(),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.weight(1f)
                            )
                            // Edit
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(colors.surface)
                                    .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        categoryToRename = customCat
                                        renameText = customCat
                                        renameError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Rename", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            // Delete
                            Box(
                                Modifier.size(32.dp).clip(RoundedCornerShape(6.dp)).background(Red)
                                    .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .clickable {
                                        categoryToDelete = customCat
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Snow, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            BrutalButton(
                text = "+ ADD CATEGORY",
                modifier = Modifier.fillMaxWidth(),
                bg = Yellow,
                fg = Ink
            ) {
                createName = ""
                createError = null
                showCreateDialog = true
            }
        }
    }

    // Create Dialog
    if (showCreateDialog) {
        Dialog(onDismissRequest = { showCreateDialog = false }) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("ADD CUSTOM CATEGORY", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                    BrutalField("CATEGORY NAME", createName, { createName = it; createError = null }, "e.g. Office, School")
                    if (createError != null) {
                        Text(createError!!, color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surfaceVariant, fg = colors.textPrimary) {
                            showCreateDialog = false
                        }
                        BrutalButton("ADD", Modifier.weight(1f), bg = Yellow, fg = Ink) {
                            val clean = createName.trim()
                            if (clean.isBlank()) {
                                createError = "Name cannot be empty"
                            } else if (SettingsStore.BUILTIN_CATEGORIES.any { it.equals(clean, ignoreCase = true) }) {
                                createError = "Built-in category already exists"
                            } else if (customCategories.any { it.equals(clean, ignoreCase = true) }) {
                                createError = "Category already exists"
                            } else {
                                val ok = onCreateCategory(clean)
                                if (ok) showCreateDialog = false else createError = "Failed to add category"
                            }
                        }
                    }
                }
            }
        }
    }

    // Rename Dialog
    if (categoryToRename != null) {
        val old = categoryToRename!!
        Dialog(onDismissRequest = { categoryToRename = null }) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("RENAME CATEGORY", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                    BrutalField("NEW NAME", renameText, { renameText = it; renameError = null }, old)
                    if (renameError != null) {
                        Text(renameError!!, color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surfaceVariant, fg = colors.textPrimary) {
                            categoryToRename = null
                        }
                        BrutalButton("SAVE", Modifier.weight(1f), bg = Yellow, fg = Ink) {
                            val clean = renameText.trim()
                            if (clean.isBlank()) {
                                renameError = "Name cannot be empty"
                            } else if (SettingsStore.BUILTIN_CATEGORIES.any { it.equals(clean, ignoreCase = true) }) {
                                renameError = "Cannot rename to built-in category"
                            } else if (customCategories.any { it.equals(clean, ignoreCase = true) && !it.equals(old, ignoreCase = true) }) {
                                renameError = "Category name already in use"
                            } else {
                                val ok = onRenameCategory(old, clean)
                                if (ok) categoryToRename = null else renameError = "Failed to rename"
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (categoryToDelete != null) {
        val cat = categoryToDelete!!
        Dialog(onDismissRequest = { categoryToDelete = null }) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("DELETE CATEGORY?", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
                    Text(
                        "Deleting \"${cat}\" will not delete any saved networks.\nExisting entries using this category will be reassigned to \"Other\".",
                        color = colors.textSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surfaceVariant, fg = colors.textPrimary) {
                            categoryToDelete = null
                        }
                        BrutalButton("DELETE", Modifier.weight(1f), bg = Red, fg = Snow) {
                            onDeleteCategory(cat)
                            categoryToDelete = null
                        }
                    }
                }
            }
        }
    }
}

