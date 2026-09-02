package com.rewifi.app.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import com.rewifi.app.vault.NearbyNetwork
import com.rewifi.app.vault.NearbyScanState
import com.rewifi.app.vault.NearbyWifiManager

@Composable
fun NearbyScreen(
    vaultCreds: List<WifiCred>,
    onBack: () -> Unit,
    onConnectToNetwork: (ssid: String, cred: WifiCred) -> Unit,
    onAddNetwork: (ssid: String) -> Unit,
    onOpenCredDetail: (WifiCred) -> Unit
) {
    val context = LocalContext.current
    val colors = RewifiTheme.colors
    val scanner = remember { NearbyWifiManager(context) }
    val scanState by scanner.scanState.collectAsState()

    var showSavedOnly by remember { mutableStateOf(false) }

    // Launcher for requesting WiFi scan permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        scanner.startScan(vaultCreds)
    }

    // Single refresh on screen entry & cleanup on dispose
    LaunchedEffect(Unit) {
        scanner.startScan(vaultCreds)
    }

    DisposableEffect(Unit) {
        onDispose {
            scanner.unregister()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header row
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Column(Modifier.weight(1f)) {
                    Text(
                        "NEARBY WIFI",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = colors.textPrimary,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "Discover networks around you",
                        color = colors.textSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Controls row: [ REFRESH ] and [ SHOW SAVED ONLY ]
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Refresh Button
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surface)
                        .border(2.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable { scanner.refresh(vaultCreds) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (scanState is NearbyScanState.Scanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = colors.textPrimary
                            )
                        } else {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            "REFRESH",
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp,
                            color = colors.textPrimary
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                // Show Saved Only toggle chip
                Box(
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (showSavedOnly) Yellow else colors.surface)
                        .border(
                            2.dp,
                            if (showSavedOnly) colors.border else colors.border.copy(alpha = 0.45f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable { showSavedOnly = !showSavedOnly }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "SAVED ONLY",
                        fontWeight = if (showSavedOnly) FontWeight.Black else FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (showSavedOnly) Ink else colors.textPrimary,
                        maxLines = 1
                    )
                }
            }

            // Content Area based on state
            when (val state = scanState) {
                is NearbyScanState.WifiDisabled -> {
                    StateCard(
                        icon = Icons.Default.WifiOff,
                        title = "WIFI IS OFF",
                        description = "Turn on WiFi to scan and discover nearby networks.",
                        actionLabel = "OPEN WIFI SETTINGS",
                        onAction = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    )
                }

                is NearbyScanState.PermissionRequired -> {
                    StateCard(
                        icon = Icons.Default.Security,
                        title = "WIFI SCAN PERMISSION REQUIRED",
                        description = "REWIFI needs nearby WiFi access to show discoverable networks around you.",
                        actionLabel = "GRANT PERMISSION",
                        onAction = {
                            permissionLauncher.launch(scanner.getRequiredPermissions())
                        },
                        secondaryActionLabel = "APP SETTINGS",
                        onSecondaryAction = {
                            runCatching {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            }
                        }
                    )
                }

                is NearbyScanState.LocationDisabled -> {
                    StateCard(
                        icon = Icons.Default.LocationOff,
                        title = "TURN ON LOCATION TO SCAN FOR WIFI",
                        description = "Android requires location services enabled to scan for WiFi networks. REWIFI never collects or uploads your location.",
                        actionLabel = "OPEN LOCATION SETTINGS",
                        onAction = {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                            }
                        }
                    )
                }

                is NearbyScanState.Scanning -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                CircularProgressIndicator(color = Yellow, strokeWidth = 3.dp)
                                Text(
                                    "SCANNING FOR NEARBY WIFI...",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }

                is NearbyScanState.Error -> {
                    StateCard(
                        icon = Icons.Default.WifiOff,
                        title = "WIFI SCANNING UNAVAILABLE",
                        description = state.message,
                        actionLabel = "RETRY SCAN",
                        onAction = { scanner.refresh(vaultCreds) }
                    )
                }

                is NearbyScanState.Success -> {
                    val displayed = if (showSavedOnly) {
                        state.networks.filter { it.isSaved }
                    } else {
                        state.networks
                    }

                    if (displayed.isEmpty()) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        Modifier
                                            .size(56.dp)
                                            .clip(CircleShape)
                                            .background(colors.surfaceVariant)
                                            .border(2.dp, colors.border, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Wifi, null, tint = colors.textSecondary)
                                    }
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        if (showSavedOnly) "NO SAVED NETWORKS NEARBY" else "NO WIFI NETWORKS FOUND",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (showSavedOnly) "None of your saved networks are currently in range."
                                        else "Make sure you are in range of a broadcast network.",
                                        color = colors.textSecondary,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(displayed, key = { it.ssid }) { network ->
                                NearbyNetworkCard(
                                    network = network,
                                    onConnect = {
                                        if (network.savedCreds.size == 1) {
                                            onConnectToNetwork(network.ssid, network.savedCreds.first())
                                        } else if (network.savedCreds.isNotEmpty()) {
                                            // Open detail of first match or picker
                                            onOpenCredDetail(network.savedCreds.first())
                                        }
                                    },
                                    onAdd = {
                                        onAddNetwork(network.ssid)
                                    },
                                    onOpenDetail = {
                                        if (network.savedCreds.isNotEmpty()) {
                                            onOpenCredDetail(network.savedCreds.first())
                                        }
                                    }
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
private fun NearbyNetworkCard(
    network: NearbyNetwork,
    onConnect: () -> Unit,
    onAdd: () -> Unit,
    onOpenDetail: () -> Unit
) {
    val colors = RewifiTheme.colors

    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top row: Signal strength + Security badge + Status Badges
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Signal Indicator + Security tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Wifi,
                        contentDescription = null,
                        tint = when (network.level) {
                            4 -> Green
                            3 -> Yellow
                            2 -> Yellow
                            else -> Red
                        },
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        "${network.signalLabel.uppercase()} • ${network.rssi} dBm",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                // Status Badges: CONNECTED / SAVED / NOT SAVED
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (network.isConnected) {
                        Badge("CONNECTED", bg = Green, fg = Ink)
                    }
                    if (network.isSaved) {
                        Badge("SAVED", bg = Yellow, fg = Ink)
                    } else {
                        Badge("NOT SAVED", bg = colors.surfaceVariant, fg = colors.textSecondary)
                    }
                }
            }

            // Middle: SSID
            Text(
                network.ssid,
                fontWeight = FontWeight.Black,
                fontSize = 18.sp,
                color = colors.textPrimary,
                maxLines = 1
            )

            // Security info chip & Match info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceVariant)
                        .border(1.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        network.security.uppercase(),
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }

                if (network.savedCreds.size > 1) {
                    Text(
                        "${network.savedCreds.size} vault matches",
                        color = colors.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Action row: [ CONNECT ] or [ ADD TO VAULT ]
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (network.isSaved) {
                    BrutalButton(
                        text = "CONNECT",
                        modifier = Modifier.weight(1f),
                        bg = if (network.isConnected) Green else Yellow,
                        fg = Ink,
                        onClick = onConnect
                    )
                    BrutalButton(
                        text = "VIEW",
                        modifier = Modifier.weight(0.7f),
                        bg = colors.surface,
                        fg = colors.textPrimary,
                        onClick = onOpenDetail
                    )
                } else {
                    BrutalButton(
                        text = "ADD TO VAULT",
                        modifier = Modifier.weight(1f),
                        bg = colors.surface,
                        fg = colors.textPrimary,
                        onClick = onAdd
                    )
                }
            }
        }
    }
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    val colors = RewifiTheme.colors
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(1.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            color = fg,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun StateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    val colors = RewifiTheme.colors
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(Yellow)
                        .border(3.dp, colors.border, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = Ink, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    title,
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    color = colors.textPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    description,
                    color = colors.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(Modifier.height(18.dp))
                BrutalButton(
                    text = actionLabel,
                    modifier = Modifier.fillMaxWidth(),
                    bg = Yellow,
                    fg = Ink,
                    onClick = onAction
                )
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    Spacer(Modifier.height(10.dp))
                    BrutalButton(
                        text = secondaryActionLabel,
                        modifier = Modifier.fillMaxWidth(),
                        bg = colors.surface,
                        fg = colors.textPrimary,
                        onClick = onSecondaryAction
                    )
                }
            }
        }
    }
}
