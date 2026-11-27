package com.medicalrep.manager.backup

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Tasks
import java.util.concurrent.TimeUnit

object DriveBackupScheduler {
    const val PREFS = "drive_backup_settings"
    const val KEY_ENABLED = "automatic_enabled"
    const val KEY_INTERVAL = "interval_days"
    const val KEY_LAST_BACKUP = "last_backup"
    const val KEY_LAST_ERROR = "last_error"
    private const val UNIQUE_WORK = "medical_rep_manager_drive_backup"

    fun configure(context: Context, enabled: Boolean, intervalDays: Int) {
        val days = intervalDays.coerceIn(1, 7)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_ENABLED, enabled).putInt(KEY_INTERVAL, days).apply()
        val manager = WorkManager.getInstance(context)
        if (!enabled) {
            manager.cancelUniqueWork(UNIQUE_WORK)
            return
        }
        val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(days.toLong(), TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        manager.enqueueUniquePeriodicWork(UNIQUE_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }
}

class DriveBackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences(DriveBackupScheduler.PREFS, Context.MODE_PRIVATE)
        if (!prefs.getBoolean(DriveBackupScheduler.KEY_ENABLED, false)) return Result.success()
        if (!BackupRunController.tryAcquireScheduledRun()) return Result.retry()
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file")))
                .build()
            val authorization = Tasks.await(Identity.getAuthorizationClient(applicationContext).authorize(request))
            if (authorization.hasResolution()) {
                prefs.edit().putString(DriveBackupScheduler.KEY_LAST_ERROR, "Open the app to reconnect Google Drive").apply()
                return Result.retry()
            }
            val token = authorization.accessToken ?: return Result.retry()
            val file = DriveBackupEngine.createLocalBackup(applicationContext)
            try {
                DriveBackupEngine.uploadAndRetain(applicationContext, token, file)
                prefs.edit().putLong(DriveBackupScheduler.KEY_LAST_BACKUP, System.currentTimeMillis())
                    .remove(DriveBackupScheduler.KEY_LAST_ERROR).apply()
            } finally { file.delete() }
            Result.success()
        } catch (error: Exception) {
            prefs.edit().putString(DriveBackupScheduler.KEY_LAST_ERROR, error.localizedMessage ?: "Backup failed").apply()
            Result.retry()
        } finally {
            BackupRunController.releaseScheduledRun()
        }
    }
}
