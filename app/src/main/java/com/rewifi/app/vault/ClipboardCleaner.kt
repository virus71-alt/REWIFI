package com.rewifi.app.vault

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Handles copying WiFi passwords to the Android system clipboard with
 * privacy protections:
 * 1. Marks clip as sensitive (on Android 13+) so keyboards avoid previews.
 * 2. Automatically clears the clipboard after a configurable duration.
 * 3. Only clears if the clipboard content still exactly matches the copied password,
 *    preventing accidental erasure of subsequent user copies.
 * 4. Cancels prior clear timers when a new password is copied.
 */
object ClipboardCleaner {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var activeClearJob: Job? = null
    private var activeCopiedPassword: String? = null

    fun copyPassword(context: Context, password: String, clearDelaySeconds: Int) {
        // Cancel any pending clear operation for previous copies
        activeClearJob?.cancel()
        activeCopiedPassword = password

        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = ClipData.newPlainText("wifi password", password)

        // Mark as sensitive on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val bundle = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
            clipData.description.extras = bundle
        }

        cm.setPrimaryClip(clipData)

        // Show feedback adapted to clear duration
        val feedback = if (clearDelaySeconds > 0) {
            val durationLabel = when (clearDelaySeconds) {
                15 -> "15s"
                30 -> "30s"
                60 -> "1m"
                120 -> "2m"
                else -> "${clearDelaySeconds}s"
            }
            "PASSWORD COPIED • CLEARS IN $durationLabel"
        } else {
            "PASSWORD COPIED"
        }
        Toast.makeText(context, feedback, Toast.LENGTH_SHORT).show()

        // Schedule delayed clear if enabled
        if (clearDelaySeconds > 0) {
            activeClearJob = scope.launch {
                delay(clearDelaySeconds * 1000L)

                // Inspect current clipboard safely
                runCatching {
                    val primaryClip = cm.primaryClip
                    if (primaryClip != null && primaryClip.itemCount > 0) {
                        val currentText = primaryClip.getItemAt(0).text?.toString()
                        if (currentText == password && activeCopiedPassword == password) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                cm.clearPrimaryClip()
                            } else {
                                cm.setPrimaryClip(ClipData.newPlainText("", ""))
                            }
                        }
                    }
                }
            }
        }
    }
}
