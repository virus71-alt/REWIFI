package com.rewifi.app

import android.app.Application
import com.rewifi.app.data.AppDatabase
import com.rewifi.app.data.DriveBackupWorker
import com.rewifi.app.data.SettingsStore
import com.rewifi.app.data.VaultRepository

class RewifiApp : Application() {
    lateinit var repository: VaultRepository
        private set
    lateinit var settings: SettingsStore
        private set

    override fun onCreate() {
        super.onCreate()
        repository = VaultRepository(AppDatabase.get(this).wifiDao())
        settings = SettingsStore(this)
        // Keep the daily Drive backup scheduled whenever an account is linked.
        if (settings.driveEmail.value != null) DriveBackupWorker.schedule(this)
    }
}
