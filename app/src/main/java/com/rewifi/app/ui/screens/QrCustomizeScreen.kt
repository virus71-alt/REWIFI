package com.rewifi.app.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.print.PrintHelper
import com.rewifi.app.data.PrintCardSize
import com.rewifi.app.data.QrConfig
import com.rewifi.app.data.QrGenerator
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow
import java.io.File
import java.io.FileOutputStream

@Composable
fun QrCustomizeScreen(
    cred: WifiCred,
    settings: SettingsStore,
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val colors = RewifiTheme.colors

    val qrDarkTheme by settings.qrDarkTheme.collectAsState()
    val qrShowLogo by settings.qrShowLogo.collectAsState()
    val qrShowSsid by settings.qrShowSsid.collectAsState()
    val qrShowSecurity by settings.qrShowSecurity.collectAsState()
    val qrShowBranding by settings.qrShowBranding.collectAsState()

    var printSize by remember { mutableStateOf(PrintCardSize.MEDIUM) }

    val config = remember(qrDarkTheme, qrShowLogo, qrShowSsid, qrShowSecurity, qrShowBranding) {
        QrConfig(
            isDark = qrDarkTheme,
            showLogo = qrShowLogo,
            showSsid = qrShowSsid,
            showSecurity = qrShowSecurity,
            showBranding = qrShowBranding
        )
    }

    // Lightweight live preview card bitmap (800px width)
    val previewBitmap = remember(config, cred.ssid, cred.password, printSize) {
        runCatching {
            QrGenerator.buildCardBitmap(ctx, cred.ssid, cred.password, config, canvasWidth = 800, printSize = printSize)
        }.getOrNull()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Top Bar
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
                Column {
                    Text("CUSTOMIZE QR", fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
                    Text(cred.ssid, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = colors.textSecondary, maxLines = 1)
                }
            }

            Box(
                Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceVariant)
                    .border(2.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                    .clickable { settings.resetQrStyle() }
                    .padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.RestartAlt, "Reset", tint = colors.textPrimary, modifier = Modifier.size(16.dp))
                    Text("RESET", fontWeight = FontWeight.Black, fontSize = 11.sp, color = colors.textPrimary)
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
            // Live Preview Card
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "LIVE PREVIEW",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Customized QR Preview",
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .aspectRatio(1f / 1.25f)
                                .clip(RoundedCornerShape(8.dp))
                                .border(2.dp, colors.border.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        )
                    } else {
                        Box(
                            Modifier
                                .fillMaxWidth(0.85f)
                                .height(260.dp)
                                .background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Generating preview…", color = colors.textSecondary)
                        }
                    }
                }
            }

            // Theme Switcher: Light / Dark
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "QR THEME",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeOptionButton(
                            label = "LIGHT",
                            selected = !qrDarkTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { settings.setQrDarkTheme(false) }
                        )
                        ThemeOptionButton(
                            label = "DARK",
                            selected = qrDarkTheme,
                            modifier = Modifier.weight(1f),
                            onClick = { settings.setQrDarkTheme(true) }
                        )
                    }
                }
            }

            // Visual Element Toggles
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "ELEMENTS",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )

                    ToggleRow(
                        label = "SHOW REWIFI LOGO",
                        subtitle = "Centered badge with safe error correction",
                        checked = qrShowLogo,
                        onCheckedChange = { settings.setQrShowLogo(it) }
                    )

                    ToggleRow(
                        label = "SHOW WIFI NAME (SSID)",
                        subtitle = "Clean text label below QR",
                        checked = qrShowSsid,
                        onCheckedChange = { settings.setQrShowSsid(it) }
                    )

                    ToggleRow(
                        label = "SHOW SECURITY TYPE",
                        subtitle = "Displays WPA2/WPA3 or Open",
                        checked = qrShowSecurity,
                        onCheckedChange = { settings.setQrShowSecurity(it) }
                    )

                    ToggleRow(
                        label = "SHOW REWIFI BRANDING",
                        subtitle = "Header banner on card and poster",
                        checked = qrShowBranding,
                        onCheckedChange = { settings.setQrShowBranding(it) }
                    )
                }
            }

            // Print Size Selection
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "PRINT CARD SCALE",
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        letterSpacing = 1.sp
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PrintCardSize.values().forEach { size ->
                            val isSelected = printSize == size
                            Box(
                                Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Yellow else colors.surfaceVariant)
                                    .border(
                                        2.dp,
                                        if (isSelected) colors.border else colors.border.copy(alpha = 0.3f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { printSize = size }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    size.label.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Ink else colors.textPrimary,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        // Action Toolbar: Share, Export HD, Print
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BrutalButton(
                    text = "SHARE",
                    modifier = Modifier.weight(1f),
                    bg = colors.surface,
                    fg = colors.textPrimary
                ) {
                    shareCustomQr(ctx, cred.ssid, cred.password, config, printSize)
                }

                BrutalButton(
                    text = "EXPORT HD",
                    modifier = Modifier.weight(1f),
                    bg = Green,
                    fg = Ink
                ) {
                    exportHdQr(ctx, cred.ssid, cred.password, config, printSize)
                }
            }

            BrutalButton(
                text = "PRINT WIFI CARD",
                modifier = Modifier.fillMaxWidth(),
                bg = Yellow,
                fg = Ink
            ) {
                printWifiCard(ctx, cred.ssid, cred.password, config, printSize)
            }
        }
    }
}

@Composable
private fun ThemeOptionButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Yellow else colors.surfaceVariant)
            .border(
                2.5.dp,
                if (selected) colors.border else colors.border.copy(alpha = 0.3f),
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (selected) {
                Icon(Icons.Default.Check, "Selected", tint = Ink, modifier = Modifier.size(16.dp))
            }
            Text(
                label,
                fontWeight = FontWeight.Black,
                fontSize = 13.sp,
                color = if (selected) Ink else colors.textPrimary
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = RewifiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = colors.textPrimary)
            Text(subtitle, fontSize = 11.sp, color = colors.textSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (checked) Yellow else colors.surfaceVariant)
                .border(
                    2.dp,
                    if (checked) colors.border else colors.border.copy(alpha = 0.4f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(Icons.Default.Check, "Checked", tint = Ink, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Share current customized QR image. */
private fun shareCustomQr(
    ctx: Context,
    ssid: String,
    password: String,
    config: QrConfig,
    printSize: PrintCardSize
) {
    runCatching {
        val bmp = QrGenerator.buildCardBitmap(ctx, ssid, password, config, canvasWidth = 1200, printSize = printSize)
        val dir = File(ctx.cacheDir, "qr").apply { mkdirs() }
        val file = File(dir, "rewifi-qr-share.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Join \"$ssid\" — scan this WiFi QR")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Share WiFi QR"))
    }.onFailure {
        Toast.makeText(ctx, "Share failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

/** Export genuine high-resolution PNG (2048 x 2560 px) directly rendered without interpolation. */
private fun exportHdQr(
    ctx: Context,
    ssid: String,
    password: String,
    config: QrConfig,
    printSize: PrintCardSize
) {
    runCatching {
        val bmp = QrGenerator.buildCardBitmap(ctx, ssid, password, config, canvasWidth = 2048, printSize = printSize)
        val dir = File(ctx.cacheDir, "qr").apply { mkdirs() }
        val cleanName = ssid.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(dir, "rewifi-hd-$cleanName.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "HD WiFi QR - $ssid")
            putExtra(Intent.EXTRA_TEXT, "High-Resolution WiFi QR for \"$ssid\"")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(send, "Export HD WiFi QR"))
    }.onFailure {
        Toast.makeText(ctx, "HD Export failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}

/** Print high-resolution WiFi card using Android standard PrintHelper. */
private fun printWifiCard(
    ctx: Context,
    ssid: String,
    password: String,
    config: QrConfig,
    printSize: PrintCardSize
) {
    runCatching {
        if (!PrintHelper.systemSupportsPrint()) {
            Toast.makeText(ctx, "Printing is not supported on this device", Toast.LENGTH_SHORT).show()
            return
        }
        val printHelper = PrintHelper(ctx).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT
        }
        val bmp = QrGenerator.buildCardBitmap(ctx, ssid, password, config, canvasWidth = 2048, printSize = printSize)
        val printJobName = "REWIFI_${ssid.replace(' ', '_')}"
        printHelper.printBitmap(printJobName, bmp)
    }.onFailure {
        Toast.makeText(ctx, "Print failed: ${it.message}", Toast.LENGTH_LONG).show()
    }
}
