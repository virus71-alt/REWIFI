package com.rewifi.app.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.BackupHealthInfo
import com.rewifi.app.data.BackupHealthStatus
import com.rewifi.app.data.BackupHistoryItem
import com.rewifi.app.data.BackupVerificationResult
import com.rewifi.app.data.CloudBackupMeta
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import com.rewifi.app.util.TimeFormatter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BackupScreen(
    settings: SettingsStore,
    onBack: () -> Unit,
    onBackupNow: ((onComplete: (String) -> Unit) -> Unit)? = null,
    onRestoreDrive: ((onComplete: (String) -> Unit) -> Unit)? = null,
    onVerifyBackup: (suspend () -> BackupVerificationResult)? = null,
    onFetchCloudMeta: (suspend () -> CloudBackupMeta?)? = null,
    onExport: (passphrase: String) -> Unit,
    onImport: (uri: Uri, passphrase: String) -> Unit
) {
    val ctx = LocalContext.current
    val colors = RewifiTheme.colors
    val scope = rememberCoroutineScope()

    var pass by remember { mutableStateOf("") }
    val valid = pass.length >= 6

    var isBackingUp by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isRestoring by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<BackupVerificationResult?>(null) }
    var cloudMeta by remember { mutableStateOf<CloudBackupMeta?>(null) }
    var showRestoreConfirmDialog by remember { mutableStateOf(false) }

    // Re-read settings reactively
    val driveEmail by settings.driveEmail.collectAsState()
    val autoBackup by settings.autoBackupEnabled.collectAsState()
    val lastBackupAt by settings.lastBackupAt.collectAsState()
    val lastAttemptAt by settings.lastBackupAttemptAt.collectAsState()
    val lastSuccess by settings.lastBackupSuccess.collectAsState()
    val failureReason by settings.lastBackupFailureReason.collectAsState()
    val history by settings.backupHistory.collectAsState()

    val healthInfo = remember(driveEmail, autoBackup, lastBackupAt, lastAttemptAt, lastSuccess, failureReason) {
        settings.computeBackupHealth()
    }

    // Fetch cloud metadata on enter
    LaunchedEffect(driveEmail) {
        if (driveEmail != null && onFetchCloudMeta != null) {
            cloudMeta = onFetchCloudMeta()
        }
    }

    // File picker for manual import
    val pickFile = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> if (uri != null) onImport(uri, pass) }

    fun guard(action: () -> Unit) {
        if (!valid) {
            Toast.makeText(ctx, "Set a passphrase (6+ chars) first", Toast.LENGTH_SHORT).show()
            return
        }
        runCatching { action() }.onFailure {
            Toast.makeText(ctx, "Couldn't open: ${it.message}", Toast.LENGTH_LONG).show()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .height(44.dp)
                        .background(colors.surface, RoundedCornerShape(12.dp))
                        .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable(onClick = onBack)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = colors.textPrimary)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    "BACKUP & RESTORE",
                    fontWeight = FontWeight.Black,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
            }

            if (driveEmail != null && onFetchCloudMeta != null) {
                Box(
                    Modifier
                        .height(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceVariant)
                        .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .clickable {
                            scope.launch {
                                cloudMeta = onFetchCloudMeta()
                                Toast.makeText(ctx, "Refreshed cloud backup info", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Refresh, "Refresh", tint = colors.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. BACKUP HEALTH CARD
            BackupHealthCard(
                health = healthInfo,
                isBackingUp = isBackingUp,
                onBackupNow = {
                    if (onBackupNow != null && !isBackingUp) {
                        isBackingUp = true
                        onBackupNow { msg ->
                            isBackingUp = false
                            Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
                            scope.launch {
                                if (onFetchCloudMeta != null) {
                                    cloudMeta = onFetchCloudMeta()
                                }
                            }
                        }
                    }
                }
            )

            // 2. RESTORE STATUS / CONFIDENCE CARD
            if (driveEmail != null) {
                RestoreStatusCard(
                    cloudMeta = cloudMeta,
                    verification = verificationResult,
                    isVerifying = isVerifying,
                    isRestoring = isRestoring,
                    onVerify = {
                        if (onVerifyBackup != null && !isVerifying) {
                            isVerifying = true
                            scope.launch {
                                verificationResult = onVerifyBackup()
                                isVerifying = false
                            }
                        }
                    },
                    onRestore = {
                        showRestoreConfirmDialog = true
                    }
                )
            }

            // 3. RECENT BACKUPS HISTORY
            RecentBackupsCard(history = history)

            // 4. MANUAL ENCRYPTED BACKUP CARD
            ManualBackupSection(
                pass = pass,
                onPassChange = { pass = it },
                onExport = { guard { onExport(pass) } },
                onImport = { guard { pickFile.launch("*/*") } }
            )

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showRestoreConfirmDialog) {
        RestoreConfirmDialog(
            onDismiss = { showRestoreConfirmDialog = false },
            onConfirm = {
                showRestoreConfirmDialog = false
                if (onRestoreDrive != null) {
                    isRestoring = true
                    onRestoreDrive { msg ->
                        isRestoring = false
                        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show()
                    }
                }
            }
        )
    }
}

@Composable
private fun BackupHealthCard(
    health: BackupHealthInfo,
    isBackingUp: Boolean,
    onBackupNow: () -> Unit
) {
    val colors = RewifiTheme.colors

    val (badgeBg, badgeFg, icon, statusText) = when (health.status) {
        BackupHealthStatus.PROTECTED -> Quad(Green, Ink, Icons.Default.CheckCircle, "PROTECTED")
        BackupHealthStatus.BACKUP_DUE -> Quad(Yellow, Ink, Icons.Default.Warning, "BACKUP DUE")
        BackupHealthStatus.BACKUP_FAILED -> Quad(Red, Snow, Icons.Default.Error, "BACKUP FAILED")
        BackupHealthStatus.DRIVE_NOT_CONNECTED -> Quad(colors.surfaceVariant, colors.textSecondary, Icons.Default.CloudOff, "DRIVE NOT CONNECTED")
        BackupHealthStatus.NEVER_BACKED_UP -> Quad(colors.surfaceVariant, colors.textSecondary, Icons.Default.CloudQueue, "NEVER BACKED UP")
        BackupHealthStatus.AUTO_BACKUP_OFF -> Quad(Yellow, Ink, Icons.Default.CloudOff, "AUTO BACKUP OFF")
    }

    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Header + Badge
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "BACKUP HEALTH",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )

                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeBg)
                        .border(1.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(icon, contentDescription = null, tint = badgeFg, modifier = Modifier.size(14.dp))
                        Text(
                            statusText,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = badgeFg
                        )
                    }
                }
            }

            if (health.status == BackupHealthStatus.BACKUP_FAILED && !health.failureReason.isNullOrBlank()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Red.copy(alpha = 0.15f))
                        .border(1.dp, Red.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Error: ${health.failureReason}",
                        color = colors.textPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Key Metrics
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                HealthMetricRow(
                    label = "LAST SUCCESSFUL BACKUP",
                    value = if (health.lastSuccessfulAt > 0L) TimeFormatter.formatRelative(health.lastSuccessfulAt) else "Never"
                )

                if (health.lastAttemptAt > 0L && health.lastAttemptAt != health.lastSuccessfulAt) {
                    HealthMetricRow(
                        label = "LAST ATTEMPT",
                        value = "${TimeFormatter.formatRelative(health.lastAttemptAt)} (${if (health.lastAttemptSuccess) "Success" else "Failed"})"
                    )
                }

                HealthMetricRow(
                    label = "AUTO BACKUP",
                    value = if (health.autoBackupEnabled) "ON (DAILY)" else "OFF"
                )

                HealthMetricRow(
                    label = "GOOGLE DRIVE",
                    value = health.driveEmail ?: "Not connected"
                )
            }

            // Primary Action Button
            if (health.driveEmail != null) {
                BrutalButton(
                    text = if (isBackingUp) "BACKING UP…" else if (health.status == BackupHealthStatus.BACKUP_FAILED) "RETRY BACKUP" else "BACK UP NOW",
                    modifier = Modifier.fillMaxWidth(),
                    bg = if (health.status == BackupHealthStatus.BACKUP_FAILED) Red else Yellow,
                    fg = if (health.status == BackupHealthStatus.BACKUP_FAILED) Snow else Ink,
                    onClick = {
                        if (!isBackingUp) onBackupNow()
                    }
                )
            }
        }
    }
}

@Composable
private fun RestoreStatusCard(
    cloudMeta: CloudBackupMeta?,
    verification: BackupVerificationResult?,
    isVerifying: Boolean,
    isRestoring: Boolean,
    onVerify: () -> Unit,
    onRestore: () -> Unit
) {
    val colors = RewifiTheme.colors

    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "RESTORE STATUS",
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )

                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (cloudMeta != null) Green else colors.surfaceVariant)
                        .border(1.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (cloudMeta != null) "BACKUP AVAILABLE" else "SEARCHING CLOUD…",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (cloudMeta != null) Ink else colors.textSecondary
                    )
                }
            }

            if (cloudMeta != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HealthMetricRow(
                        label = "LAST CLOUD BACKUP",
                        value = if (cloudMeta.modifiedTimeMs > 0L) {
                            SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(cloudMeta.modifiedTimeMs))
                        } else "Available"
                    )

                    HealthMetricRow(
                        label = "APPROX. SIZE",
                        value = "${cloudMeta.sizeBytes / 1024} KB"
                    )

                    HealthMetricRow(
                        label = "FORMAT",
                        value = "REWIFI1 Encrypted Container"
                    )
                }
            }

            // Verification Result display
            if (verification != null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (verification.verified) Green.copy(alpha = 0.18f) else Red.copy(alpha = 0.18f))
                        .border(1.5.dp, if (verification.verified) Green else Red, RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(
                                if (verification.verified) Icons.Default.Verified else Icons.Default.Error,
                                contentDescription = null,
                                tint = if (verification.verified) Green else Red,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                verification.status,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = colors.textPrimary
                            )
                        }
                        Text(verification.message, fontSize = 11.sp, color = colors.textSecondary)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrutalButton(
                    text = if (isVerifying) "VERIFYING…" else "VERIFY BACKUP",
                    modifier = Modifier.weight(1f),
                    bg = colors.surface,
                    fg = colors.textPrimary,
                    onClick = onVerify
                )

                BrutalButton(
                    text = if (isRestoring) "RESTORING…" else "RESTORE",
                    modifier = Modifier.weight(1f),
                    bg = colors.surfaceVariant,
                    fg = colors.textPrimary,
                    onClick = onRestore
                )
            }
        }
    }
}

@Composable
private fun RecentBackupsCard(history: List<BackupHistoryItem>) {
    val colors = RewifiTheme.colors

    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "RECENT BACKUPS",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = colors.textSecondary,
                letterSpacing = 1.sp
            )

            if (history.isEmpty()) {
                Text(
                    "No recent backup attempts recorded yet.",
                    color = colors.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    history.take(5).forEach { item ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                                .border(1.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(item.timestamp)),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    item.trigger.replace('_', ' '),
                                    fontSize = 10.sp,
                                    color = colors.textSecondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (item.success) Green else Red)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (item.success) "SUCCESS" else "FAILED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (item.success) Ink else Snow
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualBackupSection(
    pass: String,
    onPassChange: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "MANUAL ENCRYPTED BACKUP",
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = colors.textSecondary,
                letterSpacing = 1.sp
            )

            Text(
                "Export or import an AES-256 encrypted file protected with a custom passphrase.",
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            BrutalField("PASSPHRASE", pass, onPassChange, "min 6 characters", isPassword = true)

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                BrutalButton("EXPORT FILE", Modifier.weight(1f), bg = Green, fg = Ink, onClick = onExport)
                BrutalButton("IMPORT FILE", Modifier.weight(1f), bg = colors.surface, fg = colors.textPrimary, onClick = onImport)
            }
        }
    }
}

@Composable
private fun HealthMetricRow(label: String, value: String) {
    val colors = RewifiTheme.colors
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.textSecondary)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textPrimary, maxLines = 1)
    }
}

@Composable
private fun RestoreConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = RewifiTheme.colors
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "RESTORE FROM DRIVE?",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    "This merges all networks from your Google Drive backup into your current vault. Existing entries will be preserved.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surface, fg = colors.textPrimary, onClick = onDismiss)
                    BrutalButton("RESTORE", Modifier.weight(1f), bg = Yellow, fg = Ink, onClick = onConfirm)
                }
            }
        }
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
