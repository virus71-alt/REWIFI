package com.rewifi.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Yellow

@Composable
fun DuplicateNetworkDialog(
    newSsid: String,
    newPassword: String,
    newSecurity: String? = null,
    newCategory: String = "Other",
    newNote: String? = null,
    existingMatches: List<WifiCred>,
    onUpdateExisting: (WifiCred) -> Unit,
    onSaveAsNew: () -> Unit,
    onCancel: () -> Unit
) {
    val colors = RewifiTheme.colors
    var selectedTargetId by remember { mutableStateOf(existingMatches.firstOrNull()?.id ?: 0L) }
    var revealedPasswords by remember { mutableStateOf<Set<Long>>(emptySet()) }

    val newSecLabel = when {
        !newSecurity.isNullOrBlank() && !newSecurity.equals("nopass", ignoreCase = true) -> newSecurity.uppercase()
        newPassword.isNotBlank() -> "SECURED"
        else -> "OPEN"
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            BrutalCard(
                Modifier.fillMaxWidth(),
                padding = PaddingValues(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Yellow)
                                .border(2.dp, colors.border, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Ink, modifier = Modifier.size(20.dp))
                        }
                        Column {
                            Text(
                                "DUPLICATE NETWORK",
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = colors.textPrimary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                "\"$newSsid\" is already saved",
                                color = colors.textSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }

                    // New entry info pill
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .border(2.dp, colors.border.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "NEW INCOMING DATA",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 10.sp,
                                    color = colors.textSecondary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "Security: $newSecLabel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = colors.textPrimary
                                )
                            }
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Yellow)
                                    .border(1.5.dp, colors.border, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    newCategory.uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Ink
                                )
                            }
                        }
                    }

                    // Existing matches selection
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (existingMatches.size > 1) "SELECT EXISTING TO UPDATE (${existingMatches.size} MATCHES):"
                            else "EXISTING VAULT ENTRY:",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                        ) {
                            items(existingMatches, key = { it.id }) { match ->
                                val isSelected = match.id == selectedTargetId
                                val isRevealed = revealedPasswords.contains(match.id)
                                val matchSecLabel = if (match.password.isBlank()) "OPEN" else "SECURED"
                                val isSecDifferent = !matchSecLabel.equals(newSecLabel, ignoreCase = true)

                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Yellow.copy(alpha = 0.15f) else colors.surface)
                                        .border(
                                            2.5.dp,
                                            if (isSelected) Yellow else colors.border.copy(alpha = 0.4f),
                                            RoundedCornerShape(10.dp)
                                        )
                                        .clickable { selectedTargetId = match.id }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (existingMatches.size > 1) {
                                                // Radio indicator
                                                Box(
                                                    Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) Yellow else colors.surfaceVariant)
                                                        .border(2.dp, colors.border, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isSelected) {
                                                        Box(Modifier.size(8.dp).clip(CircleShape).background(Ink))
                                                    }
                                                }
                                                Spacer(Modifier.width(10.dp))
                                            }

                                            Text(
                                                match.ssid,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 14.sp,
                                                color = colors.textPrimary,
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Category badge
                                            Box(
                                                Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(colors.surfaceVariant)
                                                    .border(1.5.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    match.category.uppercase(),
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 9.sp,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                        }

                                        // Password & Security comparison row
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    if (isRevealed) match.password else "••••••••",
                                                    color = colors.textSecondary,
                                                    fontSize = 13.sp,
                                                    fontFamily = if (isRevealed) FontFamily.Monospace else FontFamily.Default,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                if (match.password.isNotBlank()) {
                                                    Icon(
                                                        if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                        contentDescription = "Toggle password visibility",
                                                        tint = colors.textSecondary,
                                                        modifier = Modifier
                                                            .size(16.dp)
                                                            .clickable {
                                                                revealedPasswords = if (isRevealed) {
                                                                    revealedPasswords - match.id
                                                                } else {
                                                                    revealedPasswords + match.id
                                                                }
                                                            }
                                                    )
                                                }
                                            }

                                            Text(
                                                "Type: $matchSecLabel",
                                                color = if (isSecDifferent) Red else colors.textSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSecDifferent) FontWeight.ExtraBold else FontWeight.Medium
                                            )
                                        }

                                        if (!match.note.isNullOrBlank()) {
                                            Text(
                                                "Note: ${match.note}",
                                                color = colors.textSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Action buttons
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BrutalButton(
                            text = "UPDATE EXISTING",
                            modifier = Modifier.fillMaxWidth(),
                            bg = Yellow,
                            fg = Ink
                        ) {
                            val chosen = existingMatches.firstOrNull { it.id == selectedTargetId }
                                ?: existingMatches.first()
                            onUpdateExisting(chosen)
                        }

                        BrutalButton(
                            text = "SAVE AS NEW",
                            modifier = Modifier.fillMaxWidth(),
                            bg = colors.surfaceVariant,
                            fg = colors.textPrimary
                        ) {
                            onSaveAsNew()
                        }

                        BrutalButton(
                            text = "CANCEL",
                            modifier = Modifier.fillMaxWidth(),
                            bg = colors.surface,
                            fg = colors.textSecondary
                        ) {
                            onCancel()
                        }
                    }
                }
            }
        }
    }
}
