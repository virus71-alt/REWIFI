package com.rewifi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Paper
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

@Composable
fun EditScreen(
    existing: WifiCred?,
    onBack: () -> Unit,
    onSave: (id: Long, ssid: String, password: String, note: String?) -> Unit,
    prefillSsid: String? = null,
    prefillPass: String? = null
) {
    var ssid by rememberSaveable { mutableStateOf(existing?.ssid ?: prefillSsid ?: "") }
    var pass by rememberSaveable { mutableStateOf(existing?.password ?: prefillPass ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    val valid = ssid.isNotBlank() && pass.isNotBlank()

    Column(
        Modifier.fillMaxSize().background(Paper).systemBarsPadding().padding(20.dp)
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TopBar(if (existing == null) "ADD NETWORK" else "EDIT NETWORK", onBack)

            BrutalField("NETWORK NAME (SSID)", ssid, { ssid = it }, "Cafe_Latte_5G")

            BrutalField("PASSWORD", pass, { pass = it }, "type the WiFi password", isPassword = true)

            BrutalField("NOTE (OPTIONAL)", note, { note = it }, "e.g. cafe near park, ask waiter")
        }

        Spacer(Modifier.height(14.dp))
        BrutalButton(
            if (existing == null) "SAVE TO VAULT" else "UPDATE",
            Modifier.fillMaxWidth(),
            bg = if (valid) Yellow else Snow, fg = Ink
        ) {
            if (valid) { onSave(existing?.id ?: 0L, ssid, pass, note); onBack() }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(44.dp).background(Snow, RoundedCornerShape(12.dp))
                .border(3.dp, Ink, RoundedCornerShape(12.dp))
                .clickable(onClick = onBack).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.ArrowBack, "Back", tint = Ink) }
        Text("  $title", fontWeight = FontWeight.Black, fontSize = 20.sp, color = Ink)
    }
}
