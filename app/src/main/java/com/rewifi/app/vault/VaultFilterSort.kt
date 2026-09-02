package com.rewifi.app.vault

import com.rewifi.app.data.WifiCred

enum class VaultFilter {
    ALL,
    OPEN,
    SECURED
}

enum class VaultSort {
    RECENTLY_ADDED,
    RECENTLY_UPDATED,
    NAME_AZ,
    NAME_ZA
}

object VaultFilterSort {

    /**
     * Filters and sorts WiFi credentials in memory without extra database queries.
     * Matches SSID and note case-insensitively, and evaluates open vs secured status.
     */
    fun filterAndSort(
        creds: List<WifiCred>,
        query: String,
        filter: VaultFilter,
        sort: VaultSort,
        updatedMap: Map<Long, Long> = emptyMap()
    ): List<WifiCred> {
        val q = query.trim().lowercase()

        return creds.asSequence()
            .filter { cred ->
                if (q.isNotEmpty()) {
                    val matchesSsid = cred.ssid.lowercase().contains(q)
                    val matchesNote = cred.note?.lowercase()?.contains(q) == true
                    matchesSsid || matchesNote
                } else {
                    true
                }
            }
            .filter { cred ->
                val isOpen = cred.password.isBlank() || cred.password.equals("nopass", true)
                when (filter) {
                    VaultFilter.ALL -> true
                    VaultFilter.OPEN -> isOpen
                    VaultFilter.SECURED -> !isOpen
                }
            }
            .sortedWith { a, b ->
                when (sort) {
                    VaultSort.RECENTLY_ADDED -> {
                        val cmp = b.createdAt.compareTo(a.createdAt)
                        if (cmp != 0) cmp else b.id.compareTo(a.id)
                    }
                    VaultSort.RECENTLY_UPDATED -> {
                        val timeA = updatedMap[a.id] ?: a.createdAt
                        val timeB = updatedMap[b.id] ?: b.createdAt
                        val cmp = timeB.compareTo(timeA)
                        if (cmp != 0) cmp else b.id.compareTo(a.id)
                    }
                    VaultSort.NAME_AZ -> String.CASE_INSENSITIVE_ORDER.compare(a.ssid, b.ssid)
                    VaultSort.NAME_ZA -> String.CASE_INSENSITIVE_ORDER.compare(b.ssid, a.ssid)
                }
            }
            .toList()
    }
}
