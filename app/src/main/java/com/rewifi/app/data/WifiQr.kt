package com.rewifi.app.data

/** Parsed result of a scanned WiFi QR. */
data class ScannedWifi(val ssid: String, val password: String, val security: String)

object WifiQr {

    /**
     * Parse the standard WiFi QR payload, e.g.
     *   WIFI:T:WPA;S:My Cafe;P:p4ss\;word;H:false;;
     * Returns null if it isn't a WiFi QR.
     */
    fun parse(raw: String): ScannedWifi? {
        val text = raw.trim()
        if (!text.startsWith("WIFI:", ignoreCase = true)) return null
        val body = text.substring(5)

        var ssid = ""
        var pass = ""
        var type = "WPA"

        for (field in splitUnescaped(body, ';')) {
            val sep = indexOfUnescaped(field, ':')
            if (sep < 0) continue
            val key = field.substring(0, sep).uppercase()
            val value = unescape(field.substring(sep + 1))
            when (key) {
                "S" -> ssid = value
                "P" -> pass = value
                "T" -> if (value.isNotBlank()) type = value
            }
        }

        if (ssid.isBlank()) return null
        return ScannedWifi(ssid, pass, type)
    }

    /** Split on [delim] characters that are not escaped by a preceding backslash. */
    private fun splitUnescaped(s: String, delim: Char): List<String> {
        val out = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { sb.append(c).append(s[i + 1]); i += 2; continue }
            if (c == delim) { out.add(sb.toString()); sb.clear(); i++; continue }
            sb.append(c); i++
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }

    private fun indexOfUnescaped(s: String, target: Char): Int {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\') { i += 2; continue }
            if (c == target) return i
            i++
        }
        return -1
    }

    private fun unescape(s: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) { sb.append(s[i + 1]); i += 2; continue }
            sb.append(c); i++
        }
        return sb.toString()
    }
}
