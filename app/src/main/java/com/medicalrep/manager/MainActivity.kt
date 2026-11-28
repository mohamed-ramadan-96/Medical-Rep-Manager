package com.medicalrep.manager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import java.io.File
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.medicalrep.manager.followup.FollowUpReminderScheduler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.medicalrep.manager.ui.MedicalRepApp
import com.medicalrep.manager.ui.handleMapShareIntent
import com.medicalrep.manager.ui.theme.MedicalRepTheme

class MainActivity : ComponentActivity() {
    private var themeMode by mutableStateOf("system")
    private var openDoctorId by mutableStateOf<Long?>(null)

    private val updateDownloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
            val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
            val preferences = getSharedPreferences("app_update", Context.MODE_PRIVATE)
            val expectedId = preferences.getLong("pending_download_id", -1L)
            if (completedId == expectedId && completedId != -1L) {
                handleCompletedUpdate(completedId, requestInstallPermission = true)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(
            this,
            updateDownloadReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
        cleanupCompletedAppUpdateDownloads()
        openDoctorId = intent.getLongExtra("open_doctor_id", -1L).takeIf { it > 0L }
        lifecycleScope.launch(Dispatchers.IO) {
            FollowUpReminderScheduler.rescheduleAll(applicationContext)
        }
        handleMapShareIntent(intent)
        themeMode = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            .getString("theme_mode", "system") ?: "system"
        setContent {
            MedicalRepTheme(themeMode = themeMode) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalRepApp(
                        themeMode = themeMode,
                        openDoctorId = openDoctorId,
                        onOpenDoctorHandled = { openDoctorId = null },
                        onThemeModeChange = { selectedMode ->
                            themeMode = selectedMode
                            getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                                .edit().putString("theme_mode", selectedMode).apply()
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openDoctorId = intent.getLongExtra("open_doctor_id", -1L).takeIf { it > 0L }
    }

    override fun onResume() {
        super.onResume()
        val preferences = getSharedPreferences("app_update", Context.MODE_PRIVATE)
        val pendingId = preferences.getLong("pending_download_id", -1L)
        if (pendingId != -1L && canInstallPackages()) {
            handleCompletedUpdate(pendingId, requestInstallPermission = false)
        }
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(updateDownloadReceiver)
        } catch (_: IllegalArgumentException) {
        }
        super.onDestroy()
    }

    private fun canInstallPackages(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || packageManager.canRequestPackageInstalls()

    private fun installedVersionName(): String =
        packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()

    private fun updateVersionFromTitle(title: String?): String? =
        title?.takeIf { it.startsWith("Medical Rep Manager ") }
            ?.removePrefix("Medical Rep Manager ")
            ?.takeIf { it.isNotBlank() }

    private fun compareVersionNames(first: String, second: String): Int {
        val firstParts = Regex("\\d+").findAll(first).mapNotNull { it.value.toLongOrNull() }.toList()
        val secondParts = Regex("\\d+").findAll(second).mapNotNull { it.value.toLongOrNull() }.toList()
        if (firstParts.isEmpty() || secondParts.isEmpty()) return first.compareTo(second, ignoreCase = true)
        for (index in 0 until maxOf(firstParts.size, secondParts.size)) {
            val comparison = (firstParts.getOrNull(index) ?: 0L).compareTo(secondParts.getOrNull(index) ?: 0L)
            if (comparison != 0) return comparison
        }
        return 0
    }

    private fun updateApkFile(versionName: String): File? =
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { File(it, "MRM-$versionName.apk") }

    private fun deleteUpdateApkFile(versionName: String?) {
        if (!versionName.isNullOrBlank()) updateApkFile(versionName)?.delete()
    }

    private fun cleanupCompletedAppUpdateDownloads() {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val preferences = getSharedPreferences("app_update", Context.MODE_PRIVATE)
        val pendingId = preferences.getLong("pending_download_id", -1L)
        val installedVersion = installedVersionName()
        var pendingVersion: String? = null

        if (pendingId != -1L) {
            manager.query(DownloadManager.Query().setFilterById(pendingId))?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                    if (titleIndex >= 0) pendingVersion = updateVersionFromTitle(cursor.getString(titleIndex))
                }
            }
        }

        val completedIds = mutableListOf<Long>()
        manager.query(DownloadManager.Query())?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)
            val statusIndex = cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                if (id == pendingId) continue
                val status = cursor.getInt(statusIndex)
                if (status != DownloadManager.STATUS_SUCCESSFUL && status != DownloadManager.STATUS_FAILED) continue
                val version = updateVersionFromTitle(cursor.getString(titleIndex)) ?: continue
                if (compareVersionNames(version, installedVersion) <= 0) completedIds += id
            }
        }
        if (completedIds.isNotEmpty()) manager.remove(*completedIds.toLongArray())

        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.listFiles()?.forEach { file ->
            if (!file.isFile || !file.name.startsWith("MRM-") || !file.name.endsWith(".apk")) return@forEach
            val version = file.name.removePrefix("MRM-").removeSuffix(".apk")
            if (version == pendingVersion) return@forEach
            if (compareVersionNames(version, installedVersion) <= 0) file.delete()
        }
    }

    private fun handleCompletedUpdate(downloadId: Long, requestInstallPermission: Boolean) {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        var downloadVersion: String? = null
        var status = -1
        val found = manager.query(query)?.use { cursor ->
            if (!cursor.moveToFirst()) {
                false
            } else {
                status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                if (titleIndex >= 0) downloadVersion = updateVersionFromTitle(cursor.getString(titleIndex))
                true
            }
        } ?: false

        if (!found) {
            manager.remove(downloadId)
            deleteUpdateApkFile(downloadVersion)
            getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
                .remove("pending_download_id").apply()
            Toast.makeText(this, "Downloaded APK could not be found.", Toast.LENGTH_LONG).show()
            return
        }
        if (status == DownloadManager.STATUS_FAILED) {
            manager.remove(downloadId)
            deleteUpdateApkFile(downloadVersion)
            getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
                .remove("pending_download_id").apply()
            Toast.makeText(this, "Update download failed. Please try again.", Toast.LENGTH_LONG).show()
            return
        }
        if (status != DownloadManager.STATUS_SUCCESSFUL) return

        if (!canInstallPackages()) {
            if (requestInstallPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:$packageName")
                )
                startActivity(settingsIntent)
                Toast.makeText(this, "Allow installs from this app, then return here.", Toast.LENGTH_LONG).show()
            }
            return
        }

        val apkUri = manager.getUriForDownloadedFile(downloadId)
            ?: downloadVersion?.let { version ->
                updateApkFile(version)?.takeIf { it.isFile }?.let {
                    FileProvider.getUriForFile(this, "$packageName.fileprovider", it)
                }
            }
        if (apkUri == null) {
            manager.remove(downloadId)
            deleteUpdateApkFile(downloadVersion)
            getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
                .remove("pending_download_id").apply()
            Toast.makeText(this, "Downloaded APK could not be opened.", Toast.LENGTH_LONG).show()
            return
        }
        getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
            .remove("pending_download_id").apply()
        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            startActivity(installIntent)
        } catch (_: Exception) {
            manager.remove(downloadId)
            deleteUpdateApkFile(downloadVersion)
            Toast.makeText(this, "Android could not open the APK installer.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMapShareIntent(intent)
    }
}
