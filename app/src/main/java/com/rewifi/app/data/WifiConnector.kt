package com.rewifi.app.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Connects the phone to a scanned WiFi network, picking the right API per version:
 *  - API 30+ : hands a suggestion to the system "Add network?" dialog (clean, no
 *              risky runtime permissions; the user taps Save once).
 *  - API 29  : registers a network suggestion (phone joins when the network is in range).
 *  - API 28  : legacy addNetwork + enableNetwork (immediate connect).
 *
 * WEP is treated as open/unsupported — it's effectively dead and the modern APIs
 * don't accept it. WPA/WPA2/WPA3-personal and open networks are handled.
 */
object WifiConnector {

    sealed interface Result {
        /** Connected, or will auto-join, with no further taps. */
        data object Connected : Result
        /** The system add-network dialog was shown; the user taps Save to finish. */
        data object PromptShown : Result
        data class Failed(val reason: String) : Result
    }

    fun connect(context: Context, ssid: String, password: String, security: String): Result {
        val open = password.isEmpty() || security.isBlank() || security.equals("nopass", true)
        return try {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                    addViaDialog(context, ssid, password, open)
                Build.VERSION.SDK_INT == Build.VERSION_CODES.Q ->
                    addViaSuggestion(context, ssid, password, open)
                else ->
                    addLegacy(context, ssid, password, open)
            }
        } catch (e: Exception) {
            Result.Failed(e.message ?: "Couldn't connect")
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun addViaDialog(context: Context, ssid: String, password: String, open: Boolean): Result {
        val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
        if (!open) builder.setWpa2Passphrase(password)
        val intent = Intent(Settings.ACTION_WIFI_ADD_NETWORKS)
            .putParcelableArrayListExtra(
                Settings.EXTRA_WIFI_NETWORK_LIST, arrayListOf(builder.build())
            )
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        return Result.PromptShown
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun addViaSuggestion(context: Context, ssid: String, password: String, open: Boolean): Result {
        val builder = WifiNetworkSuggestion.Builder().setSsid(ssid)
        if (!open) builder.setWpa2Passphrase(password)
        val wm = context.applicationContext.getSystemService(WifiManager::class.java)
        val status = wm.addNetworkSuggestions(listOf(builder.build()))
        return if (
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS ||
            status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE
        ) Result.Connected else Result.Failed("System rejected the network ($status)")
    }

    @Suppress("DEPRECATION")
    @SuppressLint("MissingPermission")
    private fun addLegacy(context: Context, ssid: String, password: String, open: Boolean): Result {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        if (!wm.isWifiEnabled) wm.isWifiEnabled = true
        val config = WifiConfiguration().apply {
            SSID = "\"$ssid\""
            if (open) allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            else preSharedKey = "\"$password\""
        }
        val netId = wm.addNetwork(config)
        if (netId == -1) return Result.Failed("Couldn't add network")
        wm.disconnect()
        wm.enableNetwork(netId, true)
        wm.reconnect()
        return Result.Connected
    }
}
