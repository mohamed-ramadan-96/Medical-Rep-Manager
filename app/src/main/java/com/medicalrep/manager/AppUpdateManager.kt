package com.medicalrep.manager

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionName: String,
    val versionCode: Long,
    val apkUrl: String,
    val releaseNotes: String
)

private const val RELEASE_API_URL =
    "https://api.github.com/repos/mohamed-ramadan-96/Medical-Rep-Manager/releases/latest"

suspend fun checkForAppUpdate(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
    var releaseConnection: HttpURLConnection? = null
    var manifestConnection: HttpURLConnection? = null
    try {
        releaseConnection = (URL(RELEASE_API_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Medical-Rep-Manager-Android")
        }
        if (releaseConnection.responseCode !in 200..299) return@withContext null
        val releaseJson = JSONObject(releaseConnection.inputStream.bufferedReader().use { it.readText() })
        if (releaseJson.optBoolean("draft", true) || releaseJson.optBoolean("prerelease", true)) {
            return@withContext null
        }

        val assets = releaseJson.optJSONArray("assets") ?: return@withContext null
        var manifestUrl: String? = null
        for (index in 0 until assets.length()) {
            val asset = assets.optJSONObject(index) ?: continue
            if (asset.optString("name") == "latest.json") {
                manifestUrl = asset.optString("browser_download_url").takeIf { it.startsWith("https://") }
                break
            }
        }
        val url = manifestUrl ?: return@withContext null

        manifestConnection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("User-Agent", "Medical-Rep-Manager-Android")
        }
        if (manifestConnection.responseCode !in 200..299) return@withContext null
        val manifest = JSONObject(manifestConnection.inputStream.bufferedReader().use { it.readText() })
        val remoteVersionCode = manifest.optLong("versionCode", -1L)
        val remoteVersionName = manifest.optString("versionName").takeIf { it.isNotBlank() } ?: return@withContext null
        val apkUrl = manifest.optString("apkUrl").takeIf { it.startsWith("https://") } ?: return@withContext null
        val currentVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toLong()
        }
        if (remoteVersionCode <= currentVersionCode) return@withContext null

        AppUpdateInfo(
            versionName = remoteVersionName,
            versionCode = remoteVersionCode,
            apkUrl = apkUrl,
            releaseNotes = manifest.optString("releaseNotes").ifBlank { releaseJson.optString("body") }
        )
    } catch (_: Exception) {
        null
    } finally {
        releaseConnection?.disconnect()
        manifestConnection?.disconnect()
    }
}

fun downloadAppUpdate(context: Context, update: AppUpdateInfo): Long? {
    return try {
        val request = DownloadManager.Request(Uri.parse(update.apkUrl)).apply {
            setTitle("Medical Rep Manager ${update.versionName}")
            setDescription("Downloading app update")
            setMimeType("application/vnd.android.package-archive")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalPublicDir(
                android.os.Environment.DIRECTORY_DOWNLOADS,
                "MedicalRepManager-${update.versionName}.apk"
            )
        }
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request).also {
            context.getSharedPreferences("app_update", Context.MODE_PRIVATE)
                .edit().putLong("pending_download_id", it).apply()
            Toast.makeText(context, "Downloading update…", Toast.LENGTH_LONG).show()
        }
    } catch (_: Exception) {
        Toast.makeText(context, "Could not start update download.", Toast.LENGTH_LONG).show()
        null
    }
}
