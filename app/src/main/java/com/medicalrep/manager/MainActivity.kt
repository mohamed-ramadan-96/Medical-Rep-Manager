package com.medicalrep.manager

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.medicalrep.manager.ui.MedicalRepApp
import com.medicalrep.manager.ui.handleMapShareIntent
import com.medicalrep.manager.ui.theme.MedicalRepTheme

class MainActivity : ComponentActivity() {
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
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        handleMapShareIntent(intent)
        setContent {
            MedicalRepTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalRepApp()
                }
            }
        }
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

    private fun handleCompletedUpdate(downloadId: Long, requestInstallPermission: Boolean) {
        val manager = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(downloadId)
        manager.query(query).use { cursor ->
            if (!cursor.moveToFirst()) return
            val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            if (status == DownloadManager.STATUS_FAILED) {
                getSharedPreferences("app_update", Context.MODE_PRIVATE).edit()
                    .remove("pending_download_id").apply()
                Toast.makeText(this, "Update download failed. Please try again.", Toast.LENGTH_LONG).show()
                return
            }
            if (status != DownloadManager.STATUS_SUCCESSFUL) return
        }

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
        if (apkUri == null) {
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
            Toast.makeText(this, "Android could not open the APK installer.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMapShareIntent(intent)
    }
}
