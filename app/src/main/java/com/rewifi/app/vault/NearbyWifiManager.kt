package com.rewifi.app.vault

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.rewifi.app.data.WifiCred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NearbyNetwork(
    val ssid: String,
    val rssi: Int,
    val level: Int, // 1..4
    val signalLabel: String, // "Excellent", "Strong", "Medium", "Weak"
    val security: String, // "Open", "WEP", "WPA", "WPA2", "WPA3", "WPA2/WPA3"
    val isSaved: Boolean,
    val savedCreds: List<WifiCred>,
    val isConnected: Boolean
)

sealed interface NearbyScanState {
    data object Scanning : NearbyScanState
    data object WifiDisabled : NearbyScanState
    data object PermissionRequired : NearbyScanState
    data object LocationDisabled : NearbyScanState
    data class Success(val networks: List<NearbyNetwork>) : NearbyScanState
    data class Error(val message: String) : NearbyScanState
}

class NearbyWifiManager(private val context: Context) {

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val _scanState = MutableStateFlow<NearbyScanState>(NearbyScanState.Scanning)
    val scanState: StateFlow<NearbyScanState> = _scanState.asStateFlow()

    private var scanReceiver: BroadcastReceiver? = null
    private var isReceiverRegistered = false

    fun getRequiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.NEARBY_WIFI_DEVICES,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun hasScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            val fineLocationGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return nearbyGranted || fineLocationGranted
        }
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isWifiEnabled(): Boolean {
        return runCatching { wifiManager.isWifiEnabled }.getOrDefault(false)
    }

    fun isLocationEnabled(): Boolean {
        // Location check is primarily necessary when relying on Fine Location for WiFi scanning on Android < 13
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val nearbyGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            if (nearbyGranted) return true
        }
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    fun startScan(vaultCreds: List<WifiCred>) {
        if (!isWifiEnabled()) {
            _scanState.value = NearbyScanState.WifiDisabled
            return
        }

        if (!hasScanPermission()) {
            _scanState.value = NearbyScanState.PermissionRequired
            return
        }

        if (!isLocationEnabled()) {
            _scanState.value = NearbyScanState.LocationDisabled
            return
        }

        _scanState.value = NearbyScanState.Scanning

        registerReceiver(vaultCreds)

        // Kick off scan. If throttled, immediate inspect cached scanResults below.
        val scanStarted = runCatching {
            @Suppress("DEPRECATION")
            wifiManager.startScan()
        }.getOrDefault(false)

        // Also query cached/current scanResults immediately
        readAndPostResults(vaultCreds)
    }

    private fun registerReceiver(vaultCreds: List<WifiCred>) {
        if (isReceiverRegistered) return
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                if (intent?.action == WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) {
                    readAndPostResults(vaultCreds)
                }
            }
        }
        scanReceiver = receiver
        context.registerReceiver(receiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        isReceiverRegistered = true
    }

    fun unregister() {
        if (isReceiverRegistered && scanReceiver != null) {
            runCatching { context.unregisterReceiver(scanReceiver) }
            isReceiverRegistered = false
            scanReceiver = null
        }
    }

    fun refresh(vaultCreds: List<WifiCred>) {
        startScan(vaultCreds)
    }

    private fun readAndPostResults(vaultCreds: List<WifiCred>) {
        runCatching {
            @Suppress("DEPRECATION")
            val results: List<ScanResult> = wifiManager.scanResults ?: emptyList()
            val connectedSsid = getCurrentlyConnectedSsid()
            val processed = processScanResults(results, vaultCreds, connectedSsid)
            _scanState.value = NearbyScanState.Success(processed)
        }.onFailure { error ->
            _scanState.value = NearbyScanState.Error(error.message ?: "Failed to read WiFi scan results")
        }
    }

    private fun getCurrentlyConnectedSsid(): String? {
        return runCatching {
            @Suppress("DEPRECATION")
            val info = wifiManager.connectionInfo
            if (info != null && info.networkId != -1) {
                val raw = info.ssid ?: ""
                val clean = raw.trim('\"')
                if (clean.isNotEmpty() && clean != "<unknown ssid>") clean else null
            } else null
        }.getOrNull()
    }

    companion object {
        fun parseSecurity(capabilities: String): String {
            val caps = capabilities.uppercase()
            return when {
                caps.contains("SAE") && (caps.contains("WPA2") || caps.contains("PSK")) -> "WPA2/WPA3"
                caps.contains("SAE") || caps.contains("WPA3") -> "WPA3"
                caps.contains("WPA2") || caps.contains("PSK") -> "WPA2"
                caps.contains("WPA") -> "WPA"
                caps.contains("WEP") -> "WEP"
                else -> "Open"
            }
        }

        fun parseSignal(rssi: Int): Pair<Int, String> {
            return when {
                rssi >= -55 -> 4 to "Excellent"
                rssi >= -67 -> 3 to "Strong"
                rssi >= -80 -> 2 to "Medium"
                else -> 1 to "Weak"
            }
        }

        fun processScanResults(
            scanResults: List<ScanResult>,
            vaultCreds: List<WifiCred>,
            connectedSsid: String?
        ): List<NearbyNetwork> {
            // Filter out blank or hidden SSIDs
            val valid = scanResults.filter { !it.SSID.isNullOrBlank() }
            // Group by trimmed SSID case-insensitively
            val groups = valid.groupBy { it.SSID.trim().lowercase() }

            return groups.values.mapNotNull { resultsForSsid ->
                val strongest = resultsForSsid.maxByOrNull { it.level } ?: return@mapNotNull null
                val rawSsid = strongest.SSID.trim()
                val (barLevel, signalLabel) = parseSignal(strongest.level)
                val security = parseSecurity(strongest.capabilities)

                val matchingCreds = vaultCreds.filter { it.ssid.trim().equals(rawSsid, ignoreCase = true) }
                val isConnected = connectedSsid != null && connectedSsid.equals(rawSsid, ignoreCase = true)

                NearbyNetwork(
                    ssid = rawSsid,
                    rssi = strongest.level,
                    level = barLevel,
                    signalLabel = signalLabel,
                    security = security,
                    isSaved = matchingCreds.isNotEmpty(),
                    savedCreds = matchingCreds,
                    isConnected = isConnected
                )
            }.sortedWith(
                compareByDescending<NearbyNetwork> { it.isConnected }
                    .thenByDescending { it.rssi }
            )
        }
    }
}
