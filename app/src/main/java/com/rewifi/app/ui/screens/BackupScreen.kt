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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

@Composable
fun BackupScreen(
    onBack: () -> Unit,
    onExport: (passphrase: String) -> Unit,
    onImport: (uri: Uri, passphrase: String) -> Unit
) {
    val ctx = LocalContext.current
    var pass by remember { mutableStateOf("") }
    val valid = pass.length >= 6

    // GetContent works far more broadly than OpenDocument on stripped/OEM ROMs.
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
        Modifier.fillMaxSize().background(com.rewifi.app.ui.theme.Paper).systemBarsPadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.height(44.dp).background(Snow, RoundedCornerShape(12.dp))
                    .border(3.dp, Ink, RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack).padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.ArrowBack, "Back", tint = Ink) }
            Text("  BACKUP & RESTORE", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Ink)
        }

        BrutalCard(Modifier.fillMaxWidth(), bg = Yellow, padding = PaddingValues(16.dp)) {
            Text(
                "Your backup is encrypted with this passphrase — not your phone. " +
                    "Remember it: it's the ONLY way to restore after a reset.",
                color = Ink, fontWeight = FontWeight.Bold, fontSize = 13.sp
            )
        }

        BrutalField("BACKUP PASSPHRASE", pass, { pass = it }, "min 6 characters", isPassword = true)

        Spacer(Modifier.weight(1f))
        BrutalButton("EXPORT  →  DRIVE / FILE", Modifier.fillMaxWidth(), bg = Green, fg = Ink) {
            guard { onExport(pass) }
        }
        BrutalButton("RESTORE FROM BACKUP", Modifier.fillMaxWidth(), bg = Snow, fg = Ink) {
            guard { pickFile.launch("*/*") }
        }
    }
}
