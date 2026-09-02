package com.rewifi.app.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.rewifi.app.MainActivity
import com.rewifi.app.R
import com.rewifi.app.RewifiApp
import java.util.concurrent.TimeUnit

/**
 * Periodically evaluates local backup health and issues privacy-safe
 * reminder notifications when the vault is at risk.
 */
class BackupReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as RewifiApp
        val settings = app.settings

        if (!settings.backupRemindersEnabled.value || !settings.backupNotificationsEnabled.value) {
            return Result.success()
        }

        // Check POST_NOTIFICATIONS permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return Result.success()
            }
        }

        val vaultCount = app.repository.count()
        if (vaultCount <= 0) return Result.success()

        val reminder = settings.computeBackupReminder(vaultCount) ?: return Result.success()

        val now = System.currentTimeMillis()
        val lastNotified = settings.lastBackupReminderNotificationAt.value

        // Enforce anti-spam cadence
        val minIntervalMs = if (reminder.isEmergency) {
            24 * 60 * 60 * 1000L // Max 1 per day for emergency / failed
        } else {
            3 * 24 * 60 * 60 * 1000L // Every 3 days for stale / disconnected
        }

        if (now - lastNotified < minIntervalMs) {
            return Result.success()
        }

        postNotification(reminder)
        settings.setLastBackupReminderNotificationAt(now)
        return Result.success()
    }

    private fun postNotification(reminder: BackupReminderState) {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Backup Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders to back up your REWIFI vault"
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "backup")
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(reminder.title)
            .setContentText(reminder.subtitle)
            .setStyle(NotificationCompat.BigTextStyle().bigText(reminder.subtitle))
            .setPriority(if (reminder.isEmergency) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        nm.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private const val NAME = "rewifi-backup-reminder-worker"
        private const val CHANNEL_ID = "rewifi_backup_reminders"
        private const val NOTIFICATION_ID = 2001

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupReminderWorker>(24, TimeUnit.HOURS)
                .setConstraints(Constraints.NONE)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(NAME)
        }
    }
}
