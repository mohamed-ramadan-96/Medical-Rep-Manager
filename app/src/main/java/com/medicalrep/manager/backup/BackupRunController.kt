package com.medicalrep.manager.backup

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

sealed class BackupRunState {
    object Idle : BackupRunState()
    object Preparing : BackupRunState()
    data class Uploading(val progress: Float) : BackupRunState()
    data class Finished(val message: String) : BackupRunState()
    data class Failed(val message: String) : BackupRunState()
}

/**
 * Owns a manual Drive backup independently of any screen or composable lifecycle.
 * Its scope lives for the lifetime of the app process, so scrolling and tab changes
 * cannot cancel an upload or discard its progress/result.
 */
object BackupRunController {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runLock = AtomicBoolean(false)
    private val _state = MutableStateFlow<BackupRunState>(BackupRunState.Idle)
    val state: StateFlow<BackupRunState> = _state.asStateFlow()

    fun start(context: Context, accessToken: String, isArabic: Boolean): Boolean {
        if (!runLock.compareAndSet(false, true)) return false
        val appContext = context.applicationContext
        _state.value = BackupRunState.Preparing
        scope.launch {
            try {
                val localBackup = DriveBackupEngine.createLocalBackup(appContext)
                try {
                    _state.value = BackupRunState.Uploading(0f)
                    DriveBackupEngine.uploadAndRetain(appContext, accessToken, localBackup) { uploaded, total ->
                        val progress = if (total > 0L) {
                            (uploaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        _state.value = BackupRunState.Uploading(progress)
                    }
                } finally {
                    localBackup.delete()
                }
                appContext.getSharedPreferences(DriveBackupScheduler.PREFS, Context.MODE_PRIVATE)
                    .edit()
                    .putLong(DriveBackupScheduler.KEY_LAST_BACKUP, System.currentTimeMillis())
                    .remove(DriveBackupScheduler.KEY_LAST_ERROR)
                    .apply()
                _state.value = BackupRunState.Finished(
                    if (isArabic) "تم حفظ النسخة الاحتياطية على Google Drive."
                    else "Backup saved to Google Drive."
                )
            } catch (error: Exception) {
                _state.value = BackupRunState.Failed(
                    (if (isArabic) "فشل النسخ الاحتياطي: " else "Backup failed: ") +
                        (error.localizedMessage ?: error.javaClass.simpleName)
                )
            } finally {
                runLock.set(false)
            }
        }
        return true
    }

    /** Used by WorkManager so scheduled and manual backups cannot overlap. */
    fun tryAcquireScheduledRun(): Boolean = runLock.compareAndSet(false, true)

    fun releaseScheduledRun() {
        runLock.set(false)
    }

    /** Terminal results stay available until the UI explicitly acknowledges them. */
    fun consumeResult(result: BackupRunState) {
        if (result is BackupRunState.Finished || result is BackupRunState.Failed) {
            if (_state.value == result) _state.value = BackupRunState.Idle
        }
    }
}
