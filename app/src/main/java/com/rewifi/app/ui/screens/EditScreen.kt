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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.ui.components.BrutalField
import com.rewifi.app.ui.components.DuplicateNetworkDialog
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Yellow

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun EditScreen(
    existing: WifiCred?,
    categories: List<String>,
    onBack: () -> Unit,
    onSave: (id: Long, ssid: String, password: String, note: String?, category: String) -> Unit,
    onCreateCategory: (String) -> Boolean,
    onCheckDuplicates: (ssid: String, excludeId: Long) -> List<WifiCred> = { _, _ -> emptyList() },
    onUpdateExisting: (targetId: Long, password: String, note: String?, category: String?) -> Unit = { _, _, _, _ -> },
    prefillSsid: String? = null,
    prefillPass: String? = null
) {
    val colors = RewifiTheme.colors
    var ssid by rememberSaveable { mutableStateOf(existing?.ssid ?: prefillSsid ?: "") }
    var pass by rememberSaveable { mutableStateOf(existing?.password ?: prefillPass ?: "") }
    var note by rememberSaveable { mutableStateOf(existing?.note ?: "") }
    var selectedCategory by rememberSaveable { mutableStateOf(existing?.category ?: "Home") }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var newCategoryError by remember { mutableStateOf<String?>(null) }
    var pendingDuplicates by remember { mutableStateOf<List<WifiCred>?>(null) }
    val valid = ssid.isNotBlank() && pass.isNotBlank()

    Column(
        Modifier.fillMaxSize().background(colors.background).systemBarsPadding().padding(20.dp)
    ) {
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            TopBar(if (existing == null) "ADD NETWORK" else "EDIT NETWORK", onBack)

            BrutalField("NETWORK NAME (SSID)", ssid, { ssid = it }, "Cafe_Latte_5G")

            BrutalField("PASSWORD", pass, { pass = it }, "type the WiFi password", isPassword = true)

            // Category picker
            Column {
                Text(
                    "CATEGORY",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) Yellow else colors.surfaceVariant)
                                .border(2.dp, if (isSelected) colors.border else colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                cat.uppercase(),
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isSelected) Ink else colors.textPrimary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface)
                            .border(2.dp, colors.border, RoundedCornerShape(8.dp))
                            .clickable {
                                newCategoryText = ""
                                newCategoryError = null
                                showNewCategoryDialog = true
                            }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "+ NEW",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            letterSpacing = 0.5.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            BrutalField("NOTE (OPTIONAL)", note, { note = it }, "e.g. cafe near park, ask waiter")
        }

        Spacer(Modifier.height(14.dp))
        BrutalButton(
            if (existing == null) "SAVE TO VAULT" else "UPDATE",
            Modifier.fillMaxWidth(),
            bg = if (valid) Yellow else colors.surfaceVariant,
            fg = if (valid) Ink else colors.textSecondary
        ) {
            if (valid) {
                val cleanSsid = ssid.trim()
                val duplicates = onCheckDuplicates(cleanSsid, existing?.id ?: 0L)
                if (duplicates.isNotEmpty()) {
                    pendingDuplicates = duplicates
                } else {
                    onSave(existing?.id ?: 0L, cleanSsid, pass, note.ifBlank { null }, selectedCategory)
                    onBack()
                }
            }
        }
    }

    if (pendingDuplicates != null) {
        DuplicateNetworkDialog(
            newSsid = ssid.trim(),
            newPassword = pass,
            newCategory = selectedCategory,
            newNote = note.ifBlank { null },
            existingMatches = pendingDuplicates!!,
            onUpdateExisting = { targetCred ->
                onUpdateExisting(targetCred.id, pass, note.ifBlank { null }, selectedCategory)
                pendingDuplicates = null
                onBack()
            },
            onSaveAsNew = {
                onSave(0L, ssid.trim(), pass, note.ifBlank { null }, selectedCategory)
                pendingDuplicates = null
                onBack()
            },
            onCancel = {
                pendingDuplicates = null
            }
        )
    }

    if (showNewCategoryDialog) {
        Dialog(onDismissRequest = { showNewCategoryDialog = false }) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        "NEW CATEGORY",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = colors.textPrimary
                    )
                    BrutalField(
                        label = "CATEGORY NAME",
                        value = newCategoryText,
                        onValueChange = {
                            newCategoryText = it
                            newCategoryError = null
                        },
                        hint = "e.g. Gym, Library"
                    )
                    if (newCategoryError != null) {
                        Text(newCategoryError!!, color = Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        BrutalButton(
                            "CANCEL",
                            Modifier.weight(1f),
                            bg = colors.surfaceVariant,
                            fg = colors.textPrimary
                        ) {
                            showNewCategoryDialog = false
                        }
                        BrutalButton(
                            "CREATE",
                            Modifier.weight(1f),
                            bg = Yellow,
                            fg = Ink
                        ) {
                            val clean = newCategoryText.trim()
                            if (clean.isBlank()) {
                                newCategoryError = "Name cannot be empty"
                            } else if (categories.any { it.equals(clean, ignoreCase = true) }) {
                                newCategoryError = "Category already exists"
                            } else {
                                val success = onCreateCategory(clean)
                                if (success) {
                                    selectedCategory = clean
                                    showNewCategoryDialog = false
                                } else {
                                    newCategoryError = "Invalid or duplicate category"
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBack: () -> Unit) {
    val colors = RewifiTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.height(44.dp).background(colors.surface, RoundedCornerShape(12.dp))
                .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                .clickable(onClick = onBack).padding(horizontal = 10.dp),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.ArrowBack, "Back", tint = colors.textPrimary) }
        Text("  $title", fontWeight = FontWeight.Black, fontSize = 20.sp, color = colors.textPrimary)
    }
}

