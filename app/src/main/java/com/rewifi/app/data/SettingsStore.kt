package com.rewifi.app.data

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Tiny SharedPreferences-backed settings, surfaced as [StateFlow]s so Compose
 * re-reads them reactively. Everything defaults to off — the app lock is fully
 * optional and nothing is enabled until the user opts in from the Settings tab.
 */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("rewifi_settings", Context.MODE_PRIVATE)

    private val _appLock = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK, false))
    val appLockEnabled: StateFlow<Boolean> = _appLock.asStateFlow()

    /** Minutes the app can be backgrounded before it re-locks. 0 = immediately. */
    private val _autoLockMinutes = MutableStateFlow(prefs.getInt(KEY_AUTO_LOCK_MIN, 1))
    val autoLockMinutes: StateFlow<Int> = _autoLockMinutes.asStateFlow()

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

    fun setAppLock(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_APP_LOCK, enabled) }
        _appLock.value = enabled
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

    private companion object {
        const val KEY_APP_LOCK = "app_lock_enabled"
        const val KEY_AUTO_LOCK_MIN = "auto_lock_minutes"
        const val KEY_AUTO_BACKUP = "auto_backup_enabled"
        const val KEY_INTRO_DONE = "intro_done"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_BACKUP_PASS = "backup_passphrase_enc"
        const val KEY_DRIVE_EMAIL = "drive_email"
        const val KEY_LAST_BACKUP = "last_backup_at"
    }
}
