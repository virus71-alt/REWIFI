package com.rewifi.app.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rewifi.app.RewifiApp
import java.util.concurrent.TimeUnit

/**
 * Daily background upload of the encrypted vault to Drive. Runs even when the app
 * is closed, so the backup stays fresh without the user opening the app. Per-change
 * sync still happens live in the ViewModel; this is the safety net + the "within
 * 24h" guarantee, and it retries if the network was down.
 */
class DriveBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RewifiApp
        // Drive not connected → nothing to do (not a failure).
        if (app.settings.driveEmail.value == null) return Result.success()
        val account = DriveAuth.account(applicationContext) ?: return Result.success()
        // Don't let the daily job overwrite a real Drive backup with an empty vault.
        if (app.repository.count() == 0) return Result.success()
        return try {
            val bytes = app.repository.exportEncrypted(DriveBackup.keyFor(account))
            DriveBackup.upload(applicationContext, account, bytes)
            app.settings.setLastBackupAt(System.currentTimeMillis())
            Result.success()
        } catch (e: Exception) {
            // Network blip / transient Drive error — let WorkManager retry with backoff.
            Result.retry()
        }
    }

    companion object {
        private const val NAME = "rewifi-drive-backup"

        /** Enqueue the daily backup. Idempotent — keeps the existing schedule if present. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(24, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
