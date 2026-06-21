package com.rewifi.app.vault

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rewifi.app.data.DriveAuth
import com.rewifi.app.data.DriveBackup
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.data.VaultRepository
import com.rewifi.app.data.WifiCred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Drives the home-screen sync overlay: idle → syncing → synced/failed → idle. */
enum class SyncState { IDLE, SYNCING, SYNCED, FAILED }

/** A transient banner message (e.g. scan-and-connect result). */
data class Flash(val title: String, val ok: Boolean)

class VaultViewModel(
    private val repo: VaultRepository,
    private val settings: SettingsStore,
    private val appContext: Context,
    private val autoBackupFile: File
) : ViewModel() {

    val creds: StateFlow<List<WifiCred>> =
        repo.creds.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(id: Long, ssid: String, password: String, note: String?) = viewModelScope.launch {
        repo.upsert(id, ssid, password, note)
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    /** Save a freshly scanned network (deduped by SSID) and kick off a Drive sync. */
    fun saveScanned(ssid: String, password: String, onDone: (added: Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val added = repo.addIfNew(ssid, password)
            autoBackupIfEnabled()
            driveSyncIfEnabled()
            onDone(added)
        }

    /** Write a fresh encrypted snapshot to local storage (used when the toggle flips on). */
    fun writeAutoBackupNow() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            autoBackupFile.parentFile?.mkdirs()
            autoBackupFile.writeBytes(repo.autoBackupBlob())
        }
    }

    private fun autoBackupIfEnabled() {
        if (settings.autoBackupEnabled.value) writeAutoBackupNow()
    }

    // --- Google Drive sync -------------------------------------------------

    /** Upload the encrypted vault to Drive. No-op if Drive isn't connected or the vault is empty. */
    fun syncToDriveNow() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            val account = DriveAuth.account(appContext) ?: return@launch
            // Never overwrite a real Drive backup with an empty vault (e.g. right after a reinstall).
            if (repo.count() == 0) return@launch
            DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account)))
            settings.setLastBackupAt(System.currentTimeMillis())
        }
    }

    /** Manual "Sync now" — uploads immediately and reports a user-facing result. */
    fun syncNow(onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Connect Google Drive first")
                if (repo.count() == 0) error("Vault is empty — nothing to back up")
                DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account)))
                settings.setLastBackupAt(System.currentTimeMillis())
                "Synced to Drive"
            }.getOrElse { "Sync failed: ${it.message}" }
        }
        onDone(msg)
    }

    // Transient banner shown on the vault (e.g. after a scan-and-connect).
    private val _flash = MutableStateFlow<Flash?>(null)
    val flash: StateFlow<Flash?> = _flash.asStateFlow()

    /** Show a brief success/failure banner that auto-dismisses. */
    fun showFlash(title: String, ok: Boolean) = viewModelScope.launch {
        _flash.value = Flash(title, ok)
        delay(2200)
        _flash.value = null
    }

    // Home-screen sync button: drives the "Syncing… / Synced" overlay.
    private val _syncState = MutableStateFlow(SyncState.IDLE)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    /** Triggered by the home-screen sync icon. Shows progress, then auto-dismisses. */
    fun triggerSync() = viewModelScope.launch {
        if (_syncState.value == SyncState.SYNCING) return@launch
        _syncState.value = SyncState.SYNCING
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Connect Google Drive first")
                if (repo.count() == 0) error("Vault is empty — nothing to back up")
                DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account)))
                settings.setLastBackupAt(System.currentTimeMillis())
            }.isSuccess
        }
        _syncState.value = if (ok) SyncState.SYNCED else SyncState.FAILED
        delay(1400)
        _syncState.value = SyncState.IDLE
    }

    private fun driveSyncIfEnabled() {
        if (settings.driveEmail.value != null) syncToDriveNow()
    }

    /**
     * Called right after a successful Drive sign-in. Pulls any existing backup and
     * merges it (so reconnecting on a new phone gets your data) — and only uploads
     * the current vault if Drive had nothing yet. This avoids overwriting a full
     * Drive backup with an empty/fresh local vault.
     */
    fun onDriveConnected(onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Not signed in")
                val key = DriveBackup.keyFor(account)
                val remote = DriveBackup.download(appContext, account)
                when {
                    remote != null -> {
                        val n = repo.importEncrypted(remote, key)
                        "Drive connected · restored $n network${if (n == 1) "" else "s"}"
                    }
                    // No backup the app can read. Only seed Drive from a NON-empty local
                    // vault — never upload an empty one, or we'd clobber a backup that exists
                    // but isn't currently visible to this install (drive.file after reinstall).
                    repo.count() > 0 -> {
                        DriveBackup.upload(appContext, account, repo.exportEncrypted(key))
                        settings.setLastBackupAt(System.currentTimeMillis())
                        "Drive connected · backup uploaded"
                    }
                    else -> "Drive connected · no backup found yet"
                }
            }.getOrElse {
                if (it is javax.crypto.AEADBadTagException) "Existing Drive backup is from a different account"
                else "Drive connected, but sync failed: ${it.message}"
            }
        }
        onDone(msg)
    }

    /** Pull the latest Drive backup and merge it in. Reports a user-facing message. */
    fun restoreFromDrive(onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Not signed in to Drive")
                val bytes = DriveBackup.download(appContext, account) ?: error("No backup found in Drive")
                val n = repo.importEncrypted(bytes, DriveBackup.keyFor(account))
                "Restored $n network${if (n == 1) "" else "s"} from Drive"
            }.getOrElse {
                if (it is javax.crypto.AEADBadTagException) "Backup is from a different Google account"
                else "Drive restore failed: ${it.message}"
            }
        }
        onDone(msg)
    }

    /** Encrypt the whole vault with [passphrase]; hand the bytes back for sharing. */
    fun exportBytes(passphrase: String, onReady: (ByteArray?, String) -> Unit) =
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { repo.exportEncrypted(passphrase) } }
            result
                .onSuccess { onReady(it, "") }
                .onFailure { onReady(null, "Export failed: ${it.message}") }
        }

    /** Read the picked [uri], decrypt with [passphrase], and merge into the vault. */
    fun importFrom(resolver: ContentResolver, uri: Uri, passphrase: String, onDone: (String) -> Unit) =
        viewModelScope.launch {
            val msg = withContext(Dispatchers.IO) {
                runCatching {
                    val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Couldn't open the file")
                    val n = repo.importEncrypted(bytes, passphrase)
                    "Restored $n network${if (n == 1) "" else "s"}"
                }.getOrElse {
                    if (it is javax.crypto.AEADBadTagException) "Wrong passphrase"
                    else "Restore failed: ${it.message}"
                }
            }
            onDone(msg)
        }

    class Factory(
        private val repo: VaultRepository,
        private val settings: SettingsStore,
        private val appContext: Context,
        private val autoBackupFile: File
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            VaultViewModel(repo, settings, appContext, autoBackupFile) as T
    }
}
