package com.rewifi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    onSync: () -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }
    val colors = RewifiTheme.colors
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
                Header(
                    totalCount = totalCount,
                    filteredCount = creds.size,
                    isFiltered = isFiltered,
                    onBackup = onBackup,
                    onSettings = onSettings,
                    onSync = onSync
                )
            }

            // Nearby WiFi discovery banner
            item {
                NearbyWifiBanner(onClick = onNearby)
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

            if (showRecentNetworks && recentNetworks.isNotEmpty() && !isFiltered) {
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
                        onToggleFavorite = { onToggleFavorite(c) }
                    ) { onOpen(c) }
                }
            }
        }

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

        if (syncState != SyncState.IDLE) SyncOverlay(syncState)

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

@Composable
private fun WifiRow(
    c: WifiCred,
    accent: Color,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val colors = RewifiTheme.colors
    BrutalCard(Modifier.fillMaxWidth().clickable(onClick = onClick), padding = PaddingValues(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            // Pin / Favorite action button
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


