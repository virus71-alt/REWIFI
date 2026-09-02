package com.rewifi.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rewifi.app.data.BackupReminderState
import com.rewifi.app.data.ReminderType
import com.rewifi.app.data.WifiCred
import com.rewifi.app.ui.components.BrutalButton
import com.rewifi.app.ui.components.BrutalCard
import com.rewifi.app.util.TimeFormatter
import com.rewifi.app.vault.Flash
import com.rewifi.app.vault.SyncState
import com.rewifi.app.vault.VaultFilter
import com.rewifi.app.vault.VaultSort
import com.rewifi.app.ui.theme.Blue
import com.rewifi.app.ui.theme.Green
import com.rewifi.app.ui.theme.Ink
import com.rewifi.app.ui.theme.Red
import com.rewifi.app.ui.theme.RewifiTheme
import com.rewifi.app.ui.theme.Snow
import com.rewifi.app.ui.theme.Yellow

private val accents = listOf(Blue, Green, Red, Yellow)

@Composable
fun VaultScreen(
    creds: List<WifiCred>,
    totalCount: Int,
    searchQuery: String,
    filter: VaultFilter,
    sort: VaultSort,
    categoryFilter: String,
    allCategories: List<String>,
    showRecentNetworks: Boolean = true,
    syncState: SyncState,
    flash: Flash?,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (VaultFilter) -> Unit,
    onSortChange: (VaultSort) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onClearFilters: () -> Unit,
    onToggleFavorite: (WifiCred) -> Unit,
    onAdd: () -> Unit,
    onOpen: (WifiCred) -> Unit,
    onBackup: () -> Unit,
    onScan: () -> Unit,
    onNearby: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit,
    selectedIds: Set<Long> = emptySet(),
    onToggleSelect: (Long) -> Unit = {},
    onSelectAll: () -> Unit = {},
    onClearSelection: () -> Unit = {},
    onBulkFavorite: (Boolean) -> Unit = {},
    onBulkCategory: (String) -> Unit = {},
    onBulkDelete: () -> Unit = {},
    onBulkExport: (String) -> Unit = {},
    backupReminder: BackupReminderState? = null,
    isBackingUp: Boolean = false,
    onBackupNow: () -> Unit = {},
    onSnoozeReminder: (durationMs: Long, priority: Int) -> Unit = { _, _ -> }
) {
    var showAddMenu by remember { mutableStateOf(false) }
    var showSnoozeDialog by remember { mutableStateOf(false) }
    val colors = RewifiTheme.colors
    val isSelectionMode = selectedIds.isNotEmpty()
    var showCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = isSelectionMode) {
        onClearSelection()
    }

    val isFiltered = searchQuery.isNotBlank() || filter != VaultFilter.ALL || !categoryFilter.equals("ALL", ignoreCase = true)
    val recentNetworks = remember(creds) {
        creds.filter { it.lastConnectedAt != null && it.lastConnectedAt > 0L }
            .sortedByDescending { it.lastConnectedAt }
            .take(3)
    }

    Box(Modifier.fillMaxSize().background(colors.background).systemBarsPadding()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(20.dp, 24.dp, 20.dp, 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                if (isSelectionMode) {
                    SelectionHeader(
                        selectedCount = selectedIds.size,
                        totalCount = creds.size,
                        isAllSelected = creds.isNotEmpty() && creds.all { it.id in selectedIds },
                        onSelectAll = onSelectAll,
                        onClearSelection = onClearSelection
                    )
                } else {
                    Header(
                        totalCount = totalCount,
                        filteredCount = creds.size,
                        isFiltered = isFiltered,
                        onBackup = onBackup,
                        onSettings = onSettings,
                        onSync = onSync
                    )
                }
            }

            // Backup Warning Banner (if eligible and not in selection mode)
            if (!isSelectionMode && backupReminder != null) {
                item {
                    BackupReminderBanner(
                        reminder = backupReminder,
                        isBackingUp = isBackingUp,
                        onBackupNow = onBackupNow,
                        onLater = { showSnoozeDialog = true },
                        onViewBackup = onBackup
                    )
                }
            }

            // Nearby WiFi discovery banner (hidden during selection mode)
            if (!isSelectionMode) {
                item {
                    NearbyWifiBanner(onClick = onNearby)
                }
            }

            // Always show search + filter bar if vault has entries or user is currently searching/filtering
            if (totalCount > 0 || isFiltered) {
                item {
                    SearchBarAndFilterControls(
                        searchQuery = searchQuery,
                        filter = filter,
                        sort = sort,
                        categoryFilter = categoryFilter,
                        allCategories = allCategories,
                        onSearchQueryChange = onSearchQueryChange,
                        onFilterChange = onFilterChange,
                        onSortChange = onSortChange,
                        onCategoryFilterChange = onCategoryFilterChange,
                        onClearFilters = onClearFilters
                    )
                }
            }

            if (showRecentNetworks && recentNetworks.isNotEmpty() && !isFiltered && !isSelectionMode) {
                item {
                    RecentNetworksSection(
                        recents = recentNetworks,
                        onOpen = onOpen
                    )
                }
            }

            if (totalCount == 0 && !isFiltered) {
                item { EmptyState() }
            } else if (creds.isEmpty()) {
                item {
                    FilteredEmptyState(query = searchQuery, onClear = onClearFilters)
                }
            } else {
                items(creds, key = { it.id }) { c ->
                    WifiRow(
                        c = c,
                        accent = accents[(c.id % accents.size).toInt()],
                        isSelectionMode = isSelectionMode,
                        isSelected = c.id in selectedIds,
                        onToggleSelect = { onToggleSelect(c.id) },
                        onToggleFavorite = { onToggleFavorite(c) }
                    ) { onOpen(c) }
                }
            }
        }

        if (!isSelectionMode) {
            // FAB — opens the "manual or scan" chooser.
            SquareButton(
                icon = Icons.Default.Add,
                bg = Yellow,
                iconTint = Ink,
                size = 64.dp,
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp)
            ) { showAddMenu = true }

            if (showAddMenu) {
                AddMenu(
                    onDismiss = { showAddMenu = false },
                    onManual = { showAddMenu = false; onAdd() },
                    onScan = { showAddMenu = false; onScan() },
                    onNearby = { showAddMenu = false; onNearby() }
                )
            }
        } else {
            // Bulk Action Toolbar
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                BulkActionToolbar(
                    selectedCount = selectedIds.size,
                    onFavorite = { onBulkFavorite(true) },
                    onUnfavorite = { onBulkFavorite(false) },
                    onChangeCategory = { showCategoryDialog = true },
                    onExport = { showExportDialog = true },
                    onDelete = { showDeleteConfirmDialog = true }
                )
            }
        }

        if (syncState != SyncState.IDLE) SyncOverlay(syncState)

        if (showCategoryDialog) {
            BulkCategoryDialog(
                selectedCount = selectedIds.size,
                allCategories = allCategories,
                onDismiss = { showCategoryDialog = false },
                onSelect = {
                    showCategoryDialog = false
                    onBulkCategory(it)
                }
            )
        }

        if (showDeleteConfirmDialog) {
            BulkDeleteDialog(
                selectedCount = selectedIds.size,
                onDismiss = { showDeleteConfirmDialog = false },
                onConfirm = {
                    showDeleteConfirmDialog = false
                    onBulkDelete()
                }
            )
        }

        if (showExportDialog) {
            BulkExportDialog(
                selectedCount = selectedIds.size,
                onDismiss = { showExportDialog = false },
                onExport = { passphrase ->
                    showExportDialog = false
                    onBulkExport(passphrase)
                }
            )
        }

        // Transient banner (e.g. after scanning + connecting to a network).
        flash?.let { f ->
            Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.BottomCenter) {
                BrutalCard(bg = if (f.ok) Green else Red, padding = PaddingValues(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (f.ok) Icons.Default.Check else Icons.Default.Close,
                            null, tint = Ink, modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(f.title, fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink)
                    }
                }
            }
        }

        if (showSnoozeDialog && backupReminder != null) {
            ReminderSnoozeDialog(
                priority = backupReminder.priority,
                onDismiss = { showSnoozeDialog = false },
                onSnooze = { durationMs, priority ->
                    showSnoozeDialog = false
                    onSnoozeReminder(durationMs, priority)
                }
            )
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SearchBarAndFilterControls(
    searchQuery: String,
    filter: VaultFilter,
    sort: VaultSort,
    categoryFilter: String,
    allCategories: List<String>,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (VaultFilter) -> Unit,
    onSortChange: (VaultSort) -> Unit,
    onCategoryFilterChange: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    val colors = RewifiTheme.colors
    var showPanel by remember { mutableStateOf(false) }
    val isCategoryActive = !categoryFilter.equals("ALL", ignoreCase = true)
    val isFilterActive = filter != VaultFilter.ALL || sort != VaultSort.NAME_AZ || isCategoryActive

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Search bar row
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f)) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                "Search SSID, notes, category…",
                                color = colors.textSecondary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            singleLine = true,
                            cursorBrush = SolidColor(colors.textPrimary),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Clear search",
                            tint = colors.textPrimary,
                            modifier = Modifier
                                .size(20.dp)
                                .clickable { onSearchQueryChange("") }
                        )
                    }
                }
            }

            // Filter / Sort toggle button
            Box(
                Modifier.size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (showPanel || isFilterActive) Yellow else colors.surface)
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable { showPanel = !showPanel },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = "Filter and Sort",
                    tint = if (showPanel || isFilterActive) Ink else colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Active filter pills (shown when panel is closed but filters are active)
        if (!showPanel && (isFilterActive || searchQuery.isNotEmpty())) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (isCategoryActive) {
                    ActiveBadge(
                        label = categoryFilter.uppercase(),
                        onClear = { onCategoryFilterChange("ALL") }
                    )
                }
                if (filter != VaultFilter.ALL) {
                    ActiveBadge(
                        label = filter.name,
                        onClear = { onFilterChange(VaultFilter.ALL) }
                    )
                }
                if (sort != VaultSort.NAME_AZ) {
                    val sortLabel = when (sort) {
                        VaultSort.RECENTLY_ADDED -> "RECENT"
                        VaultSort.RECENTLY_UPDATED -> "UPDATED"
                        VaultSort.NAME_ZA -> "Z → A"
                        else -> ""
                    }
                    ActiveBadge(
                        label = sortLabel,
                        onClear = { onSortChange(VaultSort.NAME_AZ) }
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "RESET",
                    color = Red,
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable {
                            onClearFilters()
                            onSortChange(VaultSort.NAME_AZ)
                        }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }

        // Animated options panel
        AnimatedVisibility(
            visible = showPanel,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Category row in panel
                    Column {
                        Text(
                            "CATEGORY",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            FilterChip("ALL", categoryFilter.equals("ALL", ignoreCase = true)) {
                                onCategoryFilterChange("ALL")
                            }
                            allCategories.forEach { cat ->
                                FilterChip(cat.uppercase(), categoryFilter.equals(cat, ignoreCase = true)) {
                                    onCategoryFilterChange(cat)
                                }
                            }
                        }
                    }

                    // Security Filter row
                    Column {
                        Text(
                            "FILTER NETWORKS",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip("ALL", filter == VaultFilter.ALL, Modifier.weight(1f)) {
                                onFilterChange(VaultFilter.ALL)
                            }
                            FilterChip("OPEN", filter == VaultFilter.OPEN, Modifier.weight(1f)) {
                                onFilterChange(VaultFilter.OPEN)
                            }
                            FilterChip("SECURED", filter == VaultFilter.SECURED, Modifier.weight(1f)) {
                                onFilterChange(VaultFilter.SECURED)
                            }
                        }
                    }

                    // Sort row
                    Column {
                        Text(
                            "SORT ORDER",
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip("ADDED", sort == VaultSort.RECENTLY_ADDED, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.RECENTLY_ADDED)
                                }
                                FilterChip("UPDATED", sort == VaultSort.RECENTLY_UPDATED, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.RECENTLY_UPDATED)
                                }
                                FilterChip("CONNECTED", sort == VaultSort.RECENTLY_CONNECTED, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.RECENTLY_CONNECTED)
                                }
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilterChip("MOST USED", sort == VaultSort.MOST_USED, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.MOST_USED)
                                }
                                FilterChip("A → Z", sort == VaultSort.NAME_AZ, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.NAME_AZ)
                                }
                                FilterChip("Z → A", sort == VaultSort.NAME_ZA, Modifier.weight(1f), horizontalPadding = 4.dp) {
                                    onSortChange(VaultSort.NAME_ZA)
                                }
                            }
                        }
                    }

                    if (isFilterActive || searchQuery.isNotEmpty()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                "RESET ALL",
                                color = Red,
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .clickable {
                                        onClearFilters()
                                        onSortChange(VaultSort.NAME_AZ)
                                    }
                                    .padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    horizontalPadding: androidx.compose.ui.unit.Dp = 12.dp,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    val bg = if (selected) Yellow else colors.surfaceVariant
    val fg = if (selected) Ink else colors.textPrimary
    val border = if (selected) colors.border else colors.border.copy(alpha = 0.45f)

    Box(
        modifier
            .defaultMinSize(minHeight = 42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(2.dp, border, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = horizontalPadding, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Bold,
            fontSize = 12.sp,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ActiveBadge(label: String, onClear: () -> Unit) {
    val colors = RewifiTheme.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Yellow)
            .border(2.dp, colors.border, RoundedCornerShape(6.dp))
            .clickable(onClick = onClear)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            color = Ink,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            maxLines = 1,
            softWrap = false
        )
        Icon(Icons.Default.Close, contentDescription = "Clear", tint = Ink, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun FilteredEmptyState(query: String, onClear: () -> Unit) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                Modifier.size(60.dp).clip(CircleShape).background(colors.surfaceVariant)
                    .border(3.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.SearchOff, null, tint = colors.textPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text("NO MATCHING NETWORKS", fontWeight = FontWeight.Black, fontSize = 16.sp, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text(
                if (query.isNotBlank()) "No networks match “$query” with the active filters."
                else "No networks match the active filters.",
                color = colors.textSecondary,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            BrutalButton(
                "CLEAR FILTERS",
                modifier = Modifier.fillMaxWidth(0.75f),
                bg = Yellow,
                fg = Ink,
                onClick = onClear
            )
        }
    }
}

/** Full-screen "Syncing… / Synced" feedback for the manual sync button. */
@Composable
private fun SyncOverlay(state: SyncState) {
    val colors = RewifiTheme.colors
    Box(
        Modifier.fillMaxSize().background(Ink.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        BrutalCard(bg = colors.surface, padding = PaddingValues(32.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                when (state) {
                    SyncState.SYNCING -> CircularProgressIndicator(color = Yellow, strokeWidth = 5.dp)
                    SyncState.SYNCED -> StatusBubble(Icons.Default.Check, Green, Ink)
                    SyncState.FAILED -> StatusBubble(Icons.Default.Close, Red, Snow)
                    SyncState.IDLE -> {}
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    when (state) {
                        SyncState.SYNCING -> "SYNCING…"
                        SyncState.SYNCED -> "SYNCED"
                        SyncState.FAILED -> "SYNC FAILED"
                        SyncState.IDLE -> ""
                    },
                    fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun StatusBubble(icon: ImageVector, bg: Color, iconTint: Color) {
    val colors = RewifiTheme.colors
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(bg).border(3.dp, colors.border, CircleShape),
        contentAlignment = Alignment.Center
    ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(28.dp)) }
}

/** Bottom-anchored chooser shown when the + is tapped. */
@Composable
private fun AddMenu(onDismiss: () -> Unit, onManual: () -> Unit, onScan: () -> Unit, onNearby: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Ink.copy(alpha = 0.45f)).clickable(onClick = onDismiss)) {
        Column(
            Modifier.align(Alignment.BottomEnd).padding(24.dp).padding(bottom = 78.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AddOption(Icons.Default.Wifi, "NEARBY WIFI", Green, Ink, onNearby)
            AddOption(Icons.Default.PhotoCamera, "SCAN QR", Blue, Snow, onScan)
            AddOption(Icons.Default.Edit, "ADD MANUALLY", Yellow, Ink, onManual)
        }
    }
}

@Composable
private fun NearbyWifiBanner(onClick: () -> Unit) {
    val colors = RewifiTheme.colors
    BrutalCard(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        padding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Yellow)
                    .border(2.5.dp, colors.border, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Wifi, null, tint = Ink, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "NEARBY WIFI",
                    fontWeight = FontWeight.Black,
                    fontSize = 15.sp,
                    color = colors.textPrimary,
                    letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Find and connect to networks around you",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .border(1.5.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    "SCAN",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = colors.textPrimary
                )
            }
        }
    }
}

@Composable
private fun AddOption(icon: ImageVector, label: String, bg: Color, fg: Color, onClick: () -> Unit) {
    val colors = RewifiTheme.colors
    Box {
        Box(Modifier.matchParentSize().offset(4.dp, 4.dp).clip(RoundedCornerShape(14.dp)).background(colors.shadow))
        Row(
            Modifier.clip(RoundedCornerShape(14.dp)).background(bg)
                .border(3.dp, colors.border, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text(label, color = fg, fontWeight = FontWeight.Black, fontSize = 15.sp)
        }
    }
}

/** Brutalist square icon button with a hard offset shadow (used for the FAB + backup). */
@Composable
private fun SquareButton(
    icon: ImageVector,
    bg: Color,
    iconTint: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Box(modifier.size(size)) {
        Box(Modifier.matchParentSize().offset(5.dp, 5.dp).clip(RoundedCornerShape(18.dp)).background(colors.shadow))
        Box(
            Modifier.size(size).clip(RoundedCornerShape(18.dp)).background(bg)
                .border(3.dp, colors.border, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(size * 0.46f)) }
    }
}

@Composable
private fun Header(
    totalCount: Int,
    filteredCount: Int,
    isFiltered: Boolean,
    onBackup: () -> Unit,
    onSettings: () -> Unit,
    onSync: () -> Unit
) {
    val colors = RewifiTheme.colors
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(Yellow)
                    .border(3.dp, colors.border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) { Text("REWIFI", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Ink) }
            Spacer(Modifier.weight(1f))
            // Sync + settings + backup = matching square brutalist buttons, mirroring the FAB.
            SquareButton(Icons.Default.Sync, Green, Ink, 46.dp, onClick = onSync)
            Spacer(Modifier.width(10.dp))
            SquareButton(Icons.Default.Settings, colors.surface, colors.textPrimary, 46.dp, onClick = onSettings)
            Spacer(Modifier.width(10.dp))
            SquareButton(Icons.Default.Upload, colors.surface, colors.textPrimary, 46.dp, onClick = onBackup)
        }
        Spacer(Modifier.height(14.dp))
        Text("YOUR\nWIFI VAULT", fontWeight = FontWeight.Black, fontSize = 38.sp,
            color = colors.textPrimary, lineHeight = 40.sp)
        Spacer(Modifier.height(6.dp))
        val countText = if (isFiltered) {
            "Showing $filteredCount of $totalCount saved network${if (totalCount == 1) "" else "s"}"
        } else {
            "$totalCount saved network${if (totalCount == 1) "" else "s"} · encrypted"
        }
        Text(countText, color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 13.sp)
        Spacer(Modifier.height(4.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WifiRow(
    c: WifiCred,
    accent: Color,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onToggleSelect: () -> Unit = {},
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    BrutalCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onToggleSelect()
                    } else {
                        onClick()
                    }
                },
                onLongClick = onToggleSelect
            ),
        bg = if (isSelected) Yellow.copy(alpha = 0.18f) else colors.surface,
        borderColor = if (isSelected) Yellow else colors.border,
        padding = PaddingValues(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isSelectionMode) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Yellow else colors.surfaceVariant)
                        .border(
                            2.dp,
                            if (isSelected) colors.border else colors.border.copy(alpha = 0.4f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Ink, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(accent)
                    .border(3.dp, colors.border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Wifi, null, tint = Ink, modifier = Modifier.size(24.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(c.ssid, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, color = colors.textPrimary, maxLines = 1)
                Spacer(Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("•••••••••", color = colors.textSecondary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(colors.surfaceVariant)
                            .border(1.5.dp, colors.border.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            c.category.uppercase(),
                            color = colors.textPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                    if (c.lastConnectedAt != null && c.lastConnectedAt > 0L) {
                        Text(
                            "• " + TimeFormatter.formatRelative(c.lastConnectedAt),
                            color = colors.textSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            // Pin / Favorite action button (hidden in selection mode)
            if (!isSelectionMode) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (c.isFavorite) Yellow else colors.surfaceVariant)
                        .border(
                            2.dp,
                            if (c.isFavorite) colors.border else colors.border.copy(alpha = 0.35f),
                            RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = onToggleFavorite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (c.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = if (c.isFavorite) "Unpin network" else "Pin network",
                        tint = if (c.isFavorite) Ink else colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectionHeader(
    selectedCount: Int,
    totalCount: Int,
    isAllSelected: Boolean,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit
) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(14.dp), bg = Yellow) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .clickable(onClick = onClearSelection),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close selection", tint = Snow, modifier = Modifier.size(20.dp))
                }

                Column {
                    Text(
                        "$selectedCount SELECTED",
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp,
                        color = Ink
                    )
                    Text(
                        "of $totalCount visible",
                        fontSize = 11.sp,
                        color = Ink.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Ink)
                        .clickable(onClick = onSelectAll)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        if (isAllSelected) "SELECTED ALL" else "SELECT ALL",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Snow,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Snow)
                        .border(2.dp, Ink, RoundedCornerShape(8.dp))
                        .clickable(onClick = onClearSelection)
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Text(
                        "CLEAR",
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = Ink,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun BulkActionToolbar(
    selectedCount: Int,
    onFavorite: () -> Unit,
    onUnfavorite: () -> Unit,
    onChangeCategory: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BulkActionButton(Icons.Default.Star, "STAR", Yellow, Ink, onFavorite)
            BulkActionButton(Icons.Default.StarBorder, "UNSTAR", colors.surfaceVariant, colors.textPrimary, onUnfavorite)
            BulkActionButton(Icons.Default.Label, "TAG", colors.surfaceVariant, colors.textPrimary, onChangeCategory)
            BulkActionButton(Icons.Default.Upload, "EXPORT", Green, Ink, onExport)
            BulkActionButton(Icons.Default.Delete, "DELETE", Red, Ink, onDelete)
        }
    }
}

@Composable
private fun BulkActionButton(
    icon: ImageVector,
    label: String,
    bg: Color,
    fg: Color,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(18.dp))
        Text(
            label,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp,
            color = fg,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun BulkCategoryDialog(
    selectedCount: Int,
    allCategories: List<String>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val colors = RewifiTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "ASSIGN CATEGORY",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    "Assign category to $selectedCount selected network${if (selectedCount == 1) "" else "s"}",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                LazyColumn(
                    Modifier.fillMaxWidth().heightIn(max = 280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allCategories) { cat ->
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceVariant)
                                .border(1.5.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { onSelect(cat) }
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Text(
                                cat.uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = colors.textPrimary
                            )
                        }
                    }
                }

                BrutalButton("CANCEL", Modifier.fillMaxWidth(), bg = colors.surface, fg = colors.textPrimary, onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun BulkDeleteDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = RewifiTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "DELETE $selectedCount NETWORK${if (selectedCount == 1) "" else "S"}?",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    "This permanently removes the selected WiFi entries from your vault.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surface, fg = colors.textPrimary, onClick = onDismiss)
                    BrutalButton("DELETE", Modifier.weight(1f), bg = Red, fg = Ink, onClick = onConfirm)
                }
            }
        }
    }
}

@Composable
private fun BulkExportDialog(
    selectedCount: Int,
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    val colors = RewifiTheme.colors
    var passphrase by remember { mutableStateOf("") }
    var confirmPassphrase by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "EXPORT $selectedCount NETWORK${if (selectedCount == 1) "" else "S"}",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    "Enter a passphrase to encrypt the selected backup.",
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                // Passphrase field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("PASSPHRASE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.textSecondary)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .border(1.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = passphrase,
                            onValueChange = { passphrase = it; error = null },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Confirm passphrase field
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("CONFIRM PASSPHRASE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = colors.textSecondary)
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surfaceVariant)
                            .border(1.5.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        BasicTextField(
                            value = confirmPassphrase,
                            onValueChange = { confirmPassphrase = it; error = null },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            textStyle = TextStyle(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (error != null) {
                    Text(error!!, color = Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BrutalButton("CANCEL", Modifier.weight(1f), bg = colors.surface, fg = colors.textPrimary, onClick = onDismiss)
                    BrutalButton("EXPORT", Modifier.weight(1f), bg = Green, fg = Ink, onClick = {
                        if (passphrase.isBlank()) {
                            error = "Passphrase cannot be empty"
                        } else if (passphrase != confirmPassphrase) {
                            error = "Passphrases do not match"
                        } else {
                            onExport(passphrase)
                        }
                    })
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(28.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(Yellow)
                    .border(3.dp, colors.border, CircleShape),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Default.Wifi, null, tint = Ink, modifier = Modifier.size(36.dp)) }
            Spacer(Modifier.height(16.dp))
            Text("NO NETWORKS YET", fontWeight = FontWeight.Black, fontSize = 18.sp, color = colors.textPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Tap + to save your first cafe WiFi.\nIt survives every phone reset.",
                color = colors.textSecondary, fontWeight = FontWeight.Medium, fontSize = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun RecentNetworksSection(
    recents: List<WifiCred>,
    onOpen: (WifiCred) -> Unit
) {
    val colors = RewifiTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                "RECENTLY CONNECTED",
                fontWeight = FontWeight.Black,
                fontSize = 11.sp,
                color = colors.textSecondary,
                letterSpacing = 1.sp
            )
        }

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            recents.forEach { cred ->
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (cred.isFavorite) Yellow.copy(alpha = 0.2f) else colors.surface)
                        .border(2.dp, colors.border, RoundedCornerShape(10.dp))
                        .clickable { onOpen(cred) }
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Column {
                        Text(
                            cred.ssid,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            TimeFormatter.formatRelative(cred.lastConnectedAt ?: 0L),
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            color = colors.textSecondary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BackupReminderBanner(
    reminder: BackupReminderState,
    isBackingUp: Boolean,
    onBackupNow: () -> Unit,
    onLater: () -> Unit,
    onViewBackup: () -> Unit
) {
    val colors = RewifiTheme.colors
    val isEmergency = reminder.isEmergency

    BrutalCard(
        modifier = Modifier.fillMaxWidth(),
        bg = if (isEmergency) Red.copy(alpha = 0.12f) else colors.surface,
        borderColor = if (isEmergency) Red else colors.border,
        padding = PaddingValues(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEmergency) Red else Yellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isEmergency) Icons.Default.Warning else Icons.Default.CloudQueue,
                        contentDescription = null,
                        tint = if (isEmergency) Snow else Ink,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        reminder.title,
                        fontWeight = FontWeight.Black,
                        fontSize = 12.sp,
                        color = if (isEmergency) Red else colors.textPrimary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        reminder.subtitle,
                        fontSize = 11.sp,
                        color = colors.textSecondary,
                        maxLines = 2,
                        lineHeight = 14.sp
                    )
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Primary Action button
                Box(
                    Modifier
                        .weight(1.2f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isEmergency) Red else Yellow)
                        .border(2.dp, colors.border, RoundedCornerShape(8.dp))
                        .clickable(enabled = !isBackingUp) {
                            if (reminder.type == ReminderType.AUTO_BACKUP_OFF || reminder.type == ReminderType.DRIVE_DISCONNECTED) {
                                onViewBackup()
                            } else {
                                onBackupNow()
                            }
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (isBackingUp) "BACKING UP…" else reminder.actionText,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        color = if (isEmergency) Snow else Ink,
                        maxLines = 1,
                        softWrap = false
                    )
                }

                // Secondary Action: LATER or DETAILS
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceVariant)
                        .border(2.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .clickable {
                            if (reminder.type == ReminderType.BACKUP_FAILED) {
                                onViewBackup()
                            } else {
                                onLater()
                            }
                        }
                        .padding(horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (reminder.type == ReminderType.BACKUP_FAILED) "DETAILS" else "LATER",
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
}

@Composable
private fun ReminderSnoozeDialog(
    priority: Int,
    onDismiss: () -> Unit,
    onSnooze: (durationMs: Long, priority: Int) -> Unit
) {
    val colors = RewifiTheme.colors
    Dialog(onDismissRequest = onDismiss) {
        BrutalCard(Modifier.fillMaxWidth(), padding = PaddingValues(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "REMIND ME LATER",
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )

                Text(
                    "Snooze backup warnings. If your backup risk increases significantly, REWIFI will alert you sooner.",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    lineHeight = 16.sp
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SnoozeOptionRow(
                        label = "REMIND TOMORROW",
                        sublabel = "24 hours",
                        onClick = { onSnooze(24 * 60 * 60 * 1000L, priority) }
                    )
                    SnoozeOptionRow(
                        label = "REMIND IN 3 DAYS",
                        sublabel = "72 hours",
                        onClick = { onSnooze(3 * 24 * 60 * 60 * 1000L, priority) }
                    )
                    SnoozeOptionRow(
                        label = "REMIND IN 7 DAYS",
                        sublabel = "1 week",
                        onClick = { onSnooze(7 * 24 * 60 * 60 * 1000L, priority) }
                    )
                }

                BrutalButton(
                    text = "CANCEL",
                    modifier = Modifier.fillMaxWidth(),
                    bg = colors.surface,
                    fg = colors.textPrimary,
                    onClick = onDismiss
                )
            }
        }
    }
}

@Composable
private fun SnoozeOptionRow(
    label: String,
    sublabel: String,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(colors.surfaceVariant)
            .border(1.5.dp, colors.border.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Black, fontSize = 12.sp, color = colors.textPrimary)
        Text(sublabel, fontSize = 11.sp, color = colors.textSecondary, fontWeight = FontWeight.Medium)
    }
}



