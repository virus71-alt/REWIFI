package com.rewifi.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Muted
import com.rewifi.app.ui.theme.Yellow

@Composable
fun LockScreen(onUnlock: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Yellow).padding(28.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(90.dp).clip(RoundedCornerShape(24.dp))
                    .background(com.rewifi.app.ui.theme.Snow)
                    .border(3.dp, Ink, RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Lock, null, tint = Ink, modifier = Modifier.size(44.dp)) }
            Spacer(Modifier.height(24.dp))
            Text("REWIFI", fontWeight = FontWeight.Black, fontSize = 40.sp, color = Ink)
            Spacer(Modifier.height(8.dp))
            Text("Your vault is locked.\nUnlock to view saved networks.",
                color = Ink.copy(alpha = 0.7f), fontWeight = FontWeight.Medium,
                fontSize = 14.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            BrutalButton("UNLOCK", bg = Ink, fg = Yellow, onClick = onUnlock)
        }
    }
}
