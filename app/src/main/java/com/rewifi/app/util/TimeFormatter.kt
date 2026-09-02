package com.rewifi.app.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TimeFormatter {

    /**
     * Formats a timestamp into a friendly, relative uppercase time string
     * for brutalist cards:
     * Examples: JUST NOW, 5 MIN AGO, 2 HOURS AGO, YESTERDAY, 4 DAYS AGO.
     */
    fun formatRelative(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = (now - timestamp).coerceAtLeast(0L)

        val minMs = 60_000L
        val hourMs = 3_600_000L
        val dayMs = 86_400_000L

        return when {
            diff < minMs -> "JUST NOW"
            diff < 2 * minMs -> "1 MIN AGO"
            diff < hourMs -> "${diff / minMs} MIN AGO"
            diff < 2 * hourMs -> "1 HOUR AGO"
            diff < dayMs -> "${diff / hourMs} HOURS AGO"
            diff < 2 * dayMs -> "YESTERDAY"
            diff < 7 * dayMs -> "${diff / dayMs} DAYS AGO"
            else -> {
                val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp)).uppercase(Locale.getDefault())
            }
        }
    }

    /**
     * Formats a timestamp into a detailed local date and time string for detail screens.
     * Returns "NEVER" if timestamp is null.
     */
    fun formatDetailed(timestamp: Long?): String {
        if (timestamp == null || timestamp <= 0L) return "NEVER"
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
