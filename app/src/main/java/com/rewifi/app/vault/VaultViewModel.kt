package com.rewifi.app.vault

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rewifi.app.data.DriveAuth
import com.rewifi.app.data.DriveBackup
import com.rewifi.app.data.CloudBackupMeta
import com.rewifi.app.data.BackupVerificationResult
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.data.VaultRepository
import com.rewifi.app.data.WifiCred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val vaultFilter: StateFlow<VaultFilter> = settings.vaultFilter
    val vaultSort: StateFlow<VaultSort> = settings.vaultSort
    val categoryFilter: StateFlow<String> = settings.categoryFilter
    val customCategories: StateFlow<List<String>> = settings.customCategories

    private val _selectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val selectedIds: StateFlow<Set<Long>> = _selectedIds.asStateFlow()

    fun getAllCategories(): List<String> = settings.allCategories()

    private val filterConfig = combine(
        settings.vaultFilter,
        settings.vaultSort,
        settings.categoryFilter
    ) { f, s, c -> Triple(f, s, c) }

    val filteredCreds: StateFlow<List<WifiCred>> =
        combine(
            creds,
            _searchQuery,
            filterConfig,
            settings.updatedAtMap
        ) { list, query, (filter, sort, categoryFilter), updatedMap ->
            VaultFilterSort.filterAndSort(list, query, filter, sort, categoryFilter, updatedMap)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: VaultFilter) {
        settings.setVaultFilter(filter)
    }

    fun setSort(sort: VaultSort) {
        settings.setVaultSort(sort)
    }

    fun setCategoryFilter(category: String) {
        settings.setCategoryFilter(category)
    }

    fun addCategory(name: String): Boolean {
        val ok = settings.addCustomCategory(name)
        if (ok) settings.incrementVaultChanges()
        return ok
    }

    fun renameCategory(oldName: String, newName: String): Boolean {
        val ok = settings.renameCustomCategory(oldName, newName)
        if (ok) {
            settings.incrementVaultChanges()
            viewModelScope.launch {
                repo.renameCategory(oldName, newName.trim())
            }
        }
        return ok
    }

    fun deleteCategory(name: String) {
        settings.deleteCustomCategory(name)
        settings.incrementVaultChanges()
        viewModelScope.launch {
            repo.reassignCategoryToOther(name)
        }
    }

    fun clearFilters() {
        _searchQuery.value = ""
        settings.setVaultFilter(VaultFilter.ALL)
        settings.setCategoryFilter("ALL")
    }

    fun toggleSelect(id: Long) {
        val current = _selectedIds.value
        _selectedIds.value = if (id in current) current - id else current + id
    }

    fun selectAll(visibleIds: List<Long>) {
        _selectedIds.value = _selectedIds.value + visibleIds
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun bulkSetFavorite(isFavorite: Boolean) {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        val count = ids.size
        viewModelScope.launch {
            settings.incrementVaultChanges()
            repo.setFavoriteBulk(ids, isFavorite)
            showFlash(if (isFavorite) "Pinned $count network${if (count == 1) "" else "s"}" else "Unpinned $count network${if (count == 1) "" else "s"}", ok = true)
            clearSelection()
            autoBackupIfEnabled()
            driveSyncIfEnabled()
        }
    }

    fun bulkSetCategory(category: String) {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        val count = ids.size
        viewModelScope.launch {
            settings.incrementVaultChanges()
            repo.setCategoryBulk(ids, category)
            showFlash("Moved $count network${if (count == 1) "" else "s"} to $category", ok = true)
            clearSelection()
            autoBackupIfEnabled()
            driveSyncIfEnabled()
        }
    }

    fun bulkDelete() {
        val ids = _selectedIds.value
        if (ids.isEmpty()) return
        val count = ids.size
        viewModelScope.launch {
            settings.incrementVaultChanges()
            repo.deleteBulk(ids)
            showFlash("Deleted $count network${if (count == 1) "" else "s"}", ok = true)
            clearSelection()
            autoBackupIfEnabled()
            driveSyncIfEnabled()
        }
    }

    suspend fun exportSelected(passphrase: String): ByteArray {
        val ids = _selectedIds.value
        return repo.exportSelectedEncrypted(ids, passphrase, customCategories.value)
    }

    fun save(id: Long, ssid: String, password: String, note: String?, category: String = "Other") = viewModelScope.launch {
        repo.upsert(id, ssid, password, note, category)
        if (id != 0L) {
            settings.recordUpdated(id, System.currentTimeMillis())
        }
        settings.incrementVaultChanges()
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun delete(id: Long) = viewModelScope.launch {
        repo.delete(id)
        settings.incrementVaultChanges()
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun toggleFavorite(id: Long, isFavorite: Boolean) = viewModelScope.launch {
        repo.setFavorite(id, isFavorite)
        settings.incrementVaultChanges()
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun findDuplicates(ssid: String, excludeId: Long = 0L): List<WifiCred> {
        val clean = ssid.trim()
        if (clean.isEmpty()) return emptyList()
        return creds.value.filter {
            it.id != excludeId && it.ssid.trim().equals(clean, ignoreCase = true)
        }
    }

    fun recordConnection(id: Long) = viewModelScope.launch {
        repo.recordConnection(id, System.currentTimeMillis())
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun recordConnectionForSsid(ssid: String) = viewModelScope.launch {
        repo.recordConnectionForSsid(ssid, System.currentTimeMillis())
        autoBackupIfEnabled()
        driveSyncIfEnabled()
    }

    fun updateExisting(
        targetId: Long,
        newPassword: String,
        newNote: String?,
        newCategory: String? = null,
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {
        repo.updateExisting(targetId, newPassword, newNote, newCategory)
        settings.recordUpdated(targetId, System.currentTimeMillis())
        settings.incrementVaultChanges()
        autoBackupIfEnabled()
        driveSyncIfEnabled()
        onDone()
    }

    /** Save a freshly scanned network (deduped by SSID) and kick off a Drive sync. */
    fun saveScanned(ssid: String, password: String, onDone: (added: Boolean) -> Unit = {}) =
        viewModelScope.launch {
            val added = repo.addIfNew(ssid, password)
            if (added) {
                settings.incrementVaultChanges()
            }
            autoBackupIfEnabled()
            driveSyncIfEnabled()
            onDone(added)
        }

    /** Write a fresh encrypted snapshot to local storage (used when the toggle flips on). */
    fun writeAutoBackupNow() = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            autoBackupFile.parentFile?.mkdirs()
            autoBackupFile.writeBytes(repo.autoBackupBlob(settings.customCategories.value))
        }
    }

    private fun autoBackupIfEnabled() {
        if (settings.autoBackupEnabled.value) writeAutoBackupNow()
    }

    // --- Google Drive sync -------------------------------------------------

    /** Upload the encrypted vault to Drive. No-op if Drive isn't connected or the vault is empty. */
    private fun mapSafeError(e: Throwable): String = when {
        e is java.net.UnknownHostException || e is java.net.SocketTimeoutException -> "Network unavailable"
        e.message?.contains("401") == true || e.message?.contains("403") == true -> "Drive authentication required"
        e.message?.contains("empty", ignoreCase = true) == true -> "Vault is empty"
        else -> e.message?.take(50) ?: "Backup upload failed"
    }

    fun syncToDriveNow() = viewModelScope.launch(Dispatchers.IO) {
        val account = DriveAuth.account(appContext) ?: return@launch
        if (repo.count() == 0) return@launch
        settings.recordBackupAttempt("VAULT_CHANGE")
        runCatching {
            DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account), settings.customCategories.value))
            settings.recordBackupSuccess("VAULT_CHANGE")
        }.onFailure { err ->
            settings.recordBackupFailure("VAULT_CHANGE", mapSafeError(err))
        }
    }

    /** Manual "Sync now" — uploads immediately and reports a user-facing result. */
    fun syncNow(onDone: (String) -> Unit) = viewModelScope.launch {
        val msg = withContext(Dispatchers.IO) {
            settings.recordBackupAttempt("MANUAL")
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Connect Google Drive first")
                if (repo.count() == 0) error("Vault is empty — nothing to back up")
                DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account), settings.customCategories.value))
                settings.recordBackupSuccess("MANUAL")
                "Backup complete"
            }.getOrElse {
                val reason = mapSafeError(it)
                settings.recordBackupFailure("MANUAL", reason)
                "Backup failed: $reason"
            }
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
        settings.recordBackupAttempt("MANUAL")
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val account = DriveAuth.account(appContext) ?: error("Connect Google Drive first")
                if (repo.count() == 0) error("Vault is empty — nothing to back up")
                DriveBackup.upload(appContext, account, repo.exportEncrypted(DriveBackup.keyFor(account), settings.customCategories.value))
                settings.recordBackupSuccess("MANUAL")
            }.onFailure {
                settings.recordBackupFailure("MANUAL", mapSafeError(it))
            }.isSuccess
        }
        _syncState.value = if (ok) SyncState.SYNCED else SyncState.FAILED
        delay(1400)
        _syncState.value = SyncState.IDLE
    }

    suspend fun getCloudBackupMeta(): CloudBackupMeta? = withContext(Dispatchers.IO) {
        val account = DriveAuth.account(appContext) ?: return@withContext null
        DriveBackup.getBackupMeta(appContext, account)
    }

    suspend fun verifyCloudBackup(): BackupVerificationResult = withContext(Dispatchers.IO) {
        val account = DriveAuth.account(appContext) ?: return@withContext BackupVerificationResult(
            verified = false,
            status = "NOT CONNECTED",
            message = "Google Drive is not connected."
        )
        DriveBackup.verifyBackup(appContext, account)
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
                        val n = repo.importEncrypted(remote, key) { cats ->
                            cats.forEach { settings.addCustomCategory(it) }
                        }
                        "Drive connected · restored $n network${if (n == 1) "" else "s"}"
                    }
                    // No backup the app can read. Only seed Drive from a NON-empty local
                    // vault — never upload an empty one, or we'd clobber a backup that exists
                    // but isn't currently visible to this install (drive.file after reinstall).
                    repo.count() > 0 -> {
                        DriveBackup.upload(appContext, account, repo.exportEncrypted(key, settings.customCategories.value))
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
                val n = repo.importEncrypted(bytes, DriveBackup.keyFor(account)) { cats ->
                    cats.forEach { settings.addCustomCategory(it) }
                }
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
            val result = withContext(Dispatchers.IO) { runCatching { repo.exportEncrypted(passphrase, settings.customCategories.value) } }
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
                    val n = repo.importEncrypted(bytes, passphrase) { cats ->
                        cats.forEach { settings.addCustomCategory(it) }
                    }
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
