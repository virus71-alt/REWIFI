package com.rewifi.app.data

import android.content.Context
import androidx.core.content.edit
import com.rewifi.app.vault.PinSecurity
import com.rewifi.app.vault.VaultFilter
import com.rewifi.app.vault.VaultSort
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class AppLockType {
    OFF,
    PIN,
    BIOMETRIC,
    PIN_AND_BIOMETRIC
}

/**
 * Tiny SharedPreferences-backed settings, surfaced as [StateFlow]s so Compose
 * re-reads them reactively. Everything defaults to off — the app lock is fully
 * optional and nothing is enabled until the user opts in from the Settings tab.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rewifi_settings", Context.MODE_PRIVATE)

    private val _appLockType = MutableStateFlow(loadInitialLockType())
    val appLockType: StateFlow<AppLockType> = _appLockType.asStateFlow()

    private val _appLock = MutableStateFlow(_appLockType.value != AppLockType.OFF)
    val appLockEnabled: StateFlow<Boolean> = _appLock.asStateFlow()

    private val _pinLength = MutableStateFlow(prefs.getInt(KEY_PIN_LENGTH, 4))
    val pinLength: StateFlow<Int> = _pinLength.asStateFlow()

    private val _hasPin = MutableStateFlow(prefs.contains(KEY_PIN_HASH))
    val hasPin: StateFlow<Boolean> = _hasPin.asStateFlow()

    private val _appTheme = MutableStateFlow(prefs.getString(KEY_APP_THEME, THEME_LIGHT) ?: THEME_LIGHT)
    val appTheme: StateFlow<String> = _appTheme.asStateFlow()

    private val _vaultSort = MutableStateFlow(
        runCatching {
            VaultSort.valueOf(prefs.getString(KEY_VAULT_SORT, VaultSort.NAME_AZ.name) ?: VaultSort.NAME_AZ.name)
        }.getOrDefault(VaultSort.NAME_AZ)
    )
    val vaultSort: StateFlow<VaultSort> = _vaultSort.asStateFlow()

    private val _vaultFilter = MutableStateFlow(
        runCatching {
            VaultFilter.valueOf(prefs.getString(KEY_VAULT_FILTER, VaultFilter.ALL.name) ?: VaultFilter.ALL.name)
        }.getOrDefault(VaultFilter.ALL)
    )
    val vaultFilter: StateFlow<VaultFilter> = _vaultFilter.asStateFlow()

    private val _categoryFilter = MutableStateFlow("ALL")
    val categoryFilter: StateFlow<String> = _categoryFilter.asStateFlow()

    private val _customCategories = MutableStateFlow(loadCustomCategories())
    val customCategories: StateFlow<List<String>> = _customCategories.asStateFlow()

    private val _updatedAtMap = MutableStateFlow(loadUpdatedMap())
    val updatedAtMap: StateFlow<Map<Long, Long>> = _updatedAtMap.asStateFlow()

    /** Minutes the app can be backgrounded before it re-locks. 0 = immediately. */
    private val _autoLockMinutes = MutableStateFlow(prefs.getInt(KEY_AUTO_LOCK_MIN, 1))
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

    /** Seconds before copied WiFi password is automatically cleared from clipboard. 0 = Off. Default: 30s. */
    private val _clipboardClearSeconds = MutableStateFlow(prefs.getInt(KEY_CLIPBOARD_CLEAR_SECONDS, 30))
    val clipboardClearSeconds: StateFlow<Int> = _clipboardClearSeconds.asStateFlow()

    /** Whether to show the compact Recent Networks section on the vault screen. Default: true. */
    private val _showRecentNetworks = MutableStateFlow(prefs.getBoolean(KEY_SHOW_RECENT_NETWORKS, true))
    val showRecentNetworks: StateFlow<Boolean> = _showRecentNetworks.asStateFlow()

    private val _autoBackup = MutableStateFlow(prefs.getBoolean(KEY_AUTO_BACKUP, false))
    val autoBackupEnabled: StateFlow<Boolean> = _autoBackup.asStateFlow()

    /** First-run flag: gates the 5-screen intro walkthrough (shown before setup). */
    private val _introDone = MutableStateFlow(prefs.getBoolean(KEY_INTRO_DONE, false))
    val introDone: StateFlow<Boolean> = _introDone.asStateFlow()

    /** First-run flag: gates the after-splash setup screen. */
    private val _onboardingDone = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_DONE, false))
    val onboardingDone: StateFlow<Boolean> = _onboardingDone.asStateFlow()

    /** True once the user has set a backup passphrase (the passphrase itself stays encrypted). */
    private val _hasPassphrase = MutableStateFlow(prefs.contains(KEY_BACKUP_PASS))
    val hasBackupPassphrase: StateFlow<Boolean> = _hasPassphrase.asStateFlow()

    /** Email of the connected Google Drive account, or null if Drive isn't linked. */
    private val _driveEmail = MutableStateFlow(prefs.getString(KEY_DRIVE_EMAIL, null))
    val driveEmail: StateFlow<String?> = _driveEmail.asStateFlow()

    /** Epoch millis of the last successful Drive upload, or 0 if never. */
    private val _lastBackupAt = MutableStateFlow(prefs.getLong(KEY_LAST_BACKUP, 0L))
    val lastBackupAt: StateFlow<Long> = _lastBackupAt.asStateFlow()

    private fun loadInitialLockType(): AppLockType {
        val stored = prefs.getString(KEY_APP_LOCK_TYPE, null)
        if (stored != null) {
            return runCatching { AppLockType.valueOf(stored) }.getOrDefault(AppLockType.OFF)
        }
        return if (prefs.getBoolean(KEY_APP_LOCK, false)) AppLockType.BIOMETRIC else AppLockType.OFF
    }

    fun setAppLockType(type: AppLockType) {
        prefs.edit {
            putString(KEY_APP_LOCK_TYPE, type.name)
            putBoolean(KEY_APP_LOCK, type != AppLockType.OFF)
        }
        _appLockType.value = type
        _appLock.value = type != AppLockType.OFF
    }

    fun setAppLock(enabled: Boolean) {
        if (!enabled) {
            setAppLockType(AppLockType.OFF)
        } else {
            val target = if (_hasPin.value) AppLockType.PIN_AND_BIOMETRIC else AppLockType.BIOMETRIC
            setAppLockType(target)
        }
    }

    /** Set or update the REWIFI PIN with salt + PBKDF2 hash. */
    fun setPin(pin: String, length: Int) {
        val salt = PinSecurity.generateSalt()
        val hash = PinSecurity.hashPin(pin, salt)
        prefs.edit {
            putString(KEY_PIN_SALT, PinSecurity.toBase64(salt))
            putString(KEY_PIN_HASH, PinSecurity.toBase64(hash))
            putInt(KEY_PIN_LENGTH, length)
            putInt(KEY_PIN_FAIL_COUNT, 0)
            putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        }
        _pinLength.value = length
        _hasPin.value = true
    }

    /** Verify entered PIN against stored salt + hash. */
    fun verifyPin(enteredPin: String): Boolean {
        val saltB64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val hashB64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = PinSecurity.fromBase64(saltB64)
        val hash = PinSecurity.fromBase64(hashB64)
        val valid = PinSecurity.verifyPin(enteredPin, salt, hash)
        if (valid) {
            resetFailedAttempts()
        }
        return valid
    }

    fun getFailedAttempts(): Int = prefs.getInt(KEY_PIN_FAIL_COUNT, 0)

    /** Record a failed PIN attempt and apply progressive lockout if needed. */
    fun recordFailedAttempt(): Long {
        val count = prefs.getInt(KEY_PIN_FAIL_COUNT, 0) + 1
        val now = System.currentTimeMillis()
        val lockoutDelayMs = when {
            count >= 8 -> 300_000L  // 5 minutes
            count == 7 -> 120_000L  // 2 minutes
            count == 6 -> 60_000L   // 1 minute
            count == 5 -> 30_000L   // 30 seconds
            else -> 0L
        }
        val lockoutUntil = if (lockoutDelayMs > 0L) now + lockoutDelayMs else 0L
        prefs.edit {
            putInt(KEY_PIN_FAIL_COUNT, count)
            putLong(KEY_PIN_LOCKOUT_UNTIL, lockoutUntil)
        }
        return lockoutUntil
    }

    fun resetFailedAttempts() {
        prefs.edit {
            putInt(KEY_PIN_FAIL_COUNT, 0)
            putLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        }
    }

    fun getRemainingLockoutSeconds(): Int {
        val until = prefs.getLong(KEY_PIN_LOCKOUT_UNTIL, 0L)
        val remaining = ((until - System.currentTimeMillis()) / 1000L).toInt()
        return if (remaining > 0) remaining else 0
    }

    fun clearPin() {
        prefs.edit {
            remove(KEY_PIN_SALT)
            remove(KEY_PIN_HASH)
            remove(KEY_PIN_LENGTH)
            remove(KEY_PIN_FAIL_COUNT)
            remove(KEY_PIN_LOCKOUT_UNTIL)
        }
        _hasPin.value = false
        if (_appLockType.value == AppLockType.PIN || _appLockType.value == AppLockType.PIN_AND_BIOMETRIC) {
            setAppLockType(AppLockType.OFF)
        }
    }

    fun setAppTheme(theme: String) {
        prefs.edit { putString(KEY_APP_THEME, theme) }
        _appTheme.value = theme
    }

    fun setClipboardClearSeconds(seconds: Int) {
        prefs.edit { putInt(KEY_CLIPBOARD_CLEAR_SECONDS, seconds) }
        _clipboardClearSeconds.value = seconds
    }

    fun setShowRecentNetworks(show: Boolean) {
        prefs.edit { putBoolean(KEY_SHOW_RECENT_NETWORKS, show) }
        _showRecentNetworks.value = show
    }

    fun setVaultSort(sort: VaultSort) {
        prefs.edit { putString(KEY_VAULT_SORT, sort.name) }
        _vaultSort.value = sort
    }

    fun setVaultFilter(filter: VaultFilter) {
        prefs.edit { putString(KEY_VAULT_FILTER, filter.name) }
        _vaultFilter.value = filter
    }

    fun setCategoryFilter(category: String) {
        _categoryFilter.value = category
    }

    fun allCategories(): List<String> {
        val list = mutableListOf<String>()
        list.addAll(BUILTIN_CATEGORIES)
        _customCategories.value.forEach { custom ->
            if (list.none { it.equals(custom, ignoreCase = true) }) {
                list.add(custom)
            }
        }
        return list
    }

    private fun loadCustomCategories(): List<String> {
        val raw = prefs.getString(KEY_CUSTOM_CATEGORIES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            val list = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                val name = arr.getString(i).trim()
                if (name.isNotEmpty() && BUILTIN_CATEGORIES.none { it.equals(name, ignoreCase = true) }) {
                    list.add(name)
                }
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun saveCustomCategories(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        prefs.edit { putString(KEY_CUSTOM_CATEGORIES, arr.toString()) }
        _customCategories.value = list
    }

    fun addCustomCategory(name: String): Boolean {
        val clean = name.trim()
        if (clean.isBlank()) return false
        if (BUILTIN_CATEGORIES.any { it.equals(clean, ignoreCase = true) }) return false
        if (_customCategories.value.any { it.equals(clean, ignoreCase = true) }) return false
        val updated = _customCategories.value + clean
        saveCustomCategories(updated)
        return true
    }

    fun renameCustomCategory(oldName: String, newName: String): Boolean {
        val clean = newName.trim()
        if (clean.isBlank()) return false
        if (BUILTIN_CATEGORIES.any { it.equals(clean, ignoreCase = true) }) return false
        if (_customCategories.value.any { it.equals(clean, ignoreCase = true) && !it.equals(oldName, ignoreCase = true) }) return false
        val updated = _customCategories.value.map { if (it.equals(oldName, ignoreCase = true)) clean else it }
        saveCustomCategories(updated)
        if (_categoryFilter.value.equals(oldName, ignoreCase = true)) {
            _categoryFilter.value = clean
        }
        return true
    }

    fun deleteCustomCategory(name: String) {
        val updated = _customCategories.value.filterNot { it.equals(name, ignoreCase = true) }
        saveCustomCategories(updated)
        if (_categoryFilter.value.equals(name, ignoreCase = true)) {
            _categoryFilter.value = "ALL"
        }
    }

    private fun loadUpdatedMap(): Map<Long, Long> {
        val raw = prefs.getString(KEY_UPDATED_MAP, null) ?: return emptyMap()
        return runCatching {
            val json = JSONObject(raw)
            val map = mutableMapOf<Long, Long>()
            val keys = json.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key.toLong()] = json.getLong(key)
            }
            map
        }.getOrDefault(emptyMap())
    }

    fun recordUpdated(id: Long, timestamp: Long) {
        val current = _updatedAtMap.value.toMutableMap()
        current[id] = timestamp
        val json = JSONObject()
        current.forEach { (k, v) -> json.put(k.toString(), v) }
        prefs.edit { putString(KEY_UPDATED_MAP, json.toString()) }
        _updatedAtMap.value = current
    }

    fun setAutoLockMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_AUTO_LOCK_MIN, minutes) }
        _autoLockMinutes.value = minutes
    }

    fun setAutoBackup(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_BACKUP, enabled) }
        _autoBackup.value = enabled
    }

    fun setIntroDone(done: Boolean) {
        prefs.edit { putBoolean(KEY_INTRO_DONE, done) }
        _introDone.value = done
    }

    fun setOnboardingDone(done: Boolean) {
        prefs.edit { putBoolean(KEY_ONBOARDING_DONE, done) }
        _onboardingDone.value = done
    }

    fun setDriveEmail(email: String?) {
        prefs.edit { if (email == null) remove(KEY_DRIVE_EMAIL) else putString(KEY_DRIVE_EMAIL, email) }
        _driveEmail.value = email
    }

    fun setLastBackupAt(ts: Long) {
        prefs.edit { putLong(KEY_LAST_BACKUP, ts) }
        _lastBackupAt.value = ts
    }

    /**
     * Store the backup passphrase encrypted with the hardware Keystore so silent
     * Drive uploads need no prompt. The Keystore key dies on factory reset — which
     * is fine: on a new device the user re-enters the passphrase to restore.
     */
    fun setBackupPassphrase(passphrase: String) {
        prefs.edit { putString(KEY_BACKUP_PASS, Crypto.encrypt(passphrase)) }
        _hasPassphrase.value = true
    }

    fun backupPassphrase(): String? =
        prefs.getString(KEY_BACKUP_PASS, null)?.let { runCatching { Crypto.decrypt(it) }.getOrNull() }

    fun clearBackupPassphrase() {
        prefs.edit { remove(KEY_BACKUP_PASS) }
        _hasPassphrase.value = false
    }

    companion object {
        val BUILTIN_CATEGORIES = listOf("Home", "Work", "Cafe", "Hotel", "Friends", "Other")
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        private const val KEY_APP_THEME = "app_theme"
        private const val KEY_VAULT_SORT = "vault_sort"
        private const val KEY_VAULT_FILTER = "vault_filter"
        private const val KEY_CUSTOM_CATEGORIES = "custom_categories"
        private const val KEY_UPDATED_MAP = "vault_updated_map"
        private const val KEY_APP_LOCK = "app_lock_enabled"
        private const val KEY_APP_LOCK_TYPE = "app_lock_type"
        private const val KEY_PIN_SALT = "pin_salt_b64"
        private const val KEY_PIN_HASH = "pin_hash_b64"
        private const val KEY_PIN_LENGTH = "pin_length"
        private const val KEY_PIN_FAIL_COUNT = "pin_fail_count"
        private const val KEY_PIN_LOCKOUT_UNTIL = "pin_lockout_until"
        private const val KEY_AUTO_LOCK_MIN = "auto_lock_minutes"
        private const val KEY_CLIPBOARD_CLEAR_SECONDS = "clipboard_clear_seconds"
        private const val KEY_SHOW_RECENT_NETWORKS = "show_recent_networks"
        private const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        private const val KEY_INTRO_DONE = "intro_done"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_BACKUP_PASS = "backup_passphrase_enc"
        private const val KEY_DRIVE_EMAIL = "drive_email"
        private const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
