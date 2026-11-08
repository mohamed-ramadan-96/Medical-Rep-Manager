package com.medicalrep.manager.backup

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.util.JsonWriter
import androidx.room.RoomDatabase
import com.medicalrep.manager.data.MedicalRepDatabase
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class DriveBackupItem(val id: String, val name: String, val modifiedTime: String)

object DriveBackupEngine {
    private const val DRIVE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    private const val FOLDER_NAME = "Medical Rep Manager"

    fun createLocalBackup(context: Context): File {
        val database = MedicalRepDatabase.getInstance(context)
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { }
        val dbFile = context.getDatabasePath("medical_rep_manager.db")
        check(dbFile.exists()) { "Database file not found" }
        val output = File(context.cacheDir, "medical-rep-backup-${System.currentTimeMillis()}.zip")
        val manifest = JSONObject().put("version", 1)
        val photos = JSONArray()
        val attachments = JSONArray()
        ZipOutputStream(FileOutputStream(output)).use { zip ->
            zip.putNextEntry(ZipEntry("db/medical_rep_manager.db"))
            FileInputStream(dbFile).use { it.copyTo(zip) }
            zip.closeEntry()

            listOf("doctor_attachments", "center_attachments").forEach { folder ->
                val directory = File(context.filesDir, folder)
                if (directory.exists()) addDirectory(zip, directory, "files/$folder", attachments)
            }

            val cursor = database.openHelper.readableDatabase.query(
                "SELECT id, photoUri FROM doctors WHERE photoUri IS NOT NULL AND photoUri != ''"
            )
            cursor.use {
                while (it.moveToNext()) {
                    val doctorId = it.getLong(0)
                    val rawUri = it.getString(1) ?: continue
                    try {
                        val uri = Uri.parse(rawUri)
                        val extension = (uri.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg")
                            .substringAfterLast('/').take(8).ifBlank { "jpg" }
                        val entryName = "photos/doctor_$doctorId.$extension"
                        val input = if (uri.scheme == "file") FileInputStream(File(uri.path!!))
                            else context.contentResolver.openInputStream(uri)
                        if (input != null) {
                            zip.putNextEntry(ZipEntry(entryName))
                            input.use { it.copyTo(zip) }
                            zip.closeEntry()
                            photos.put(JSONObject().put("doctorId", doctorId).put("entry", entryName))
                        }
                    } catch (_: Exception) { }
                }
            }
            manifest.put("photos", photos).put("attachments", attachments)
            zip.putNextEntry(ZipEntry("backup-manifest.json"))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return output
    }

    private fun addDirectory(zip: ZipOutputStream, root: File, prefix: String, mappings: JSONArray) {
        root.walkTopDown().filter { it.isFile }.forEach { file ->
            val relative = file.relativeTo(root.parentFile!!).invariantSeparatorsPath
            val entry = "files/$relative"
            zip.putNextEntry(ZipEntry(entry))
            FileInputStream(file).use { it.copyTo(zip) }
            zip.closeEntry()
            mappings.put(JSONObject().put("oldPath", file.absolutePath).put("entry", entry))
        }
    }

    fun upload(context: Context, token: String, backup: File): DriveBackupItem {
        var stage = "Drive folder lookup"
        var targetHost = "www.googleapis.com"
        try {
            val folderId = getOrCreateFolder(token)
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val name = "MedicalRepManager_$stamp.zip"
            val metadata = JSONObject().put("name", name).put("mimeType", "application/zip")
                .put("parents", JSONArray().put(folderId))

            stage = "starting resumable upload session"
            val initUrl = "$UPLOAD/files?uploadType=resumable&fields=id,name,modifiedTime"
            targetHost = URL(initUrl).host
            val init = URL(initUrl).openConnection() as HttpURLConnection
            init.connectTimeout = 20000
            init.readTimeout = 30000
            init.requestMethod = "POST"
            init.doOutput = true
            init.setRequestProperty("Authorization", "Bearer $token")
            init.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            init.setRequestProperty("X-Upload-Content-Type", "application/zip")
            init.setRequestProperty("X-Upload-Content-Length", backup.length().toString())
            init.outputStream.use { it.write(metadata.toString().toByteArray(Charsets.UTF_8)) }
            check(init.responseCode in 200..299) { "Drive upload setup failed: ${responseText(init)}" }
            val uploadUrl = init.getHeaderField("Location") ?: error("Drive did not return an upload URL")
            init.disconnect()

            stage = "uploading backup archive"
            val parsedUploadUrl = URL(uploadUrl)
            targetHost = parsedUploadUrl.host
            require(parsedUploadUrl.protocol == "https") { "Google returned a non-HTTPS upload URL" }
            val put = parsedUploadUrl.openConnection() as HttpURLConnection
            put.connectTimeout = 20000
            put.readTimeout = 120000
            put.requestMethod = "PUT"
            put.doOutput = true
            put.setRequestProperty("Authorization", "Bearer $token")
            put.setRequestProperty("Content-Type", "application/zip")
            put.setFixedLengthStreamingMode(backup.length())
            FileInputStream(backup).use { input -> put.outputStream.use { input.copyTo(it) } }
            check(put.responseCode in 200..299) { "Backup upload failed: ${responseText(put)}" }
            val result = JSONObject(put.inputStream.bufferedReader().use { it.readText() })
            put.disconnect()
            return DriveBackupItem(result.getString("id"), result.optString("name", name), result.optString("modifiedTime", ""))
        } catch (error: Exception) {
            var cause: Throwable? = error
            var dnsFailure = false
            while (cause != null) {
                if (cause is java.net.UnknownHostException) dnsFailure = true
                cause = cause.cause
            }
            val detail = if (dnsFailure) {
                "DNS lookup failed during $stage (host: $targetHost)"
            } else {
                "Drive request failed during $stage (host: $targetHost): ${error.localizedMessage ?: error.javaClass.simpleName}"
            }
            throw java.io.IOException(detail, error)
        }
    }

    fun list(token: String): List<DriveBackupItem> {
        val folder = findFolder(token) ?: return emptyList()
        val q = URLEncoder.encode("'$folder' in parents and trashed = false", "UTF-8")
        val url = URL("$DRIVE/files?q=$q&orderBy=modifiedTime%20desc&pageSize=100&fields=files(id,name,modifiedTime)")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        check(connection.responseCode in 200..299) { "Could not list Drive backups: ${responseText(connection)}" }
        val files = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("files") ?: JSONArray()
        connection.disconnect()
        return (0 until files.length()).map { index ->
            val item = files.getJSONObject(index)
            DriveBackupItem(item.getString("id"), item.optString("name"), item.optString("modifiedTime"))
        }
    }

    fun restore(context: Context, token: String, item: DriveBackupItem) {
        val zipFile = File(context.cacheDir, "restore-${System.currentTimeMillis()}.zip")
        val url = URL("$DRIVE/files/${item.id}?alt=media")
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        check(connection.responseCode in 200..299) { "Could not download backup: ${responseText(connection)}" }
        connection.inputStream.use { input -> FileOutputStream(zipFile).use { input.copyTo(it) } }
        connection.disconnect()
        try { restoreArchive(context, zipFile) } finally { zipFile.delete() }
    }

    private fun restoreArchive(context: Context, archive: File) {
        val staging = File(context.cacheDir, "restore-stage-${System.currentTimeMillis()}").apply { mkdirs() }
        try {
            ZipInputStream(FileInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val target = File(staging, entry.name)
                    check(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "Invalid backup archive" }
                    if (entry.isDirectory) target.mkdirs() else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            val dbSource = File(staging, "db/medical_rep_manager.db")
            val manifestFile = File(staging, "backup-manifest.json")
            check(dbSource.isFile && manifestFile.isFile) { "Backup archive is incomplete" }
            val manifest = JSONObject(manifestFile.readText())
            check(manifest.optInt("version") == 1) { "Unsupported backup version" }

            MedicalRepDatabase.closeInstance()
            val dbTarget = context.getDatabasePath("medical_rep_manager.db")
            dbTarget.parentFile?.mkdirs()
            listOf(dbTarget, File(dbTarget.path + "-wal"), File(dbTarget.path + "-shm")).forEach { it.delete() }
            dbSource.copyTo(dbTarget, overwrite = true)

            listOf("doctor_attachments", "center_attachments").forEach { folder ->
                val source = File(staging, "files/$folder")
                val target = File(context.filesDir, folder)
                target.deleteRecursively()
                if (source.exists()) source.copyRecursively(target, overwrite = true)
            }

            val writable = SQLiteDatabase.openDatabase(dbTarget.path, null, SQLiteDatabase.OPEN_READWRITE)
            try {
                val photos = manifest.optJSONArray("photos") ?: JSONArray()
                for (i in 0 until photos.length()) {
                    val photo = photos.getJSONObject(i)
                    val source = File(staging, photo.getString("entry"))
                    if (source.isFile) {
                        val target = File(File(context.filesDir, "doctor_photos"), source.name).apply { parentFile?.mkdirs() }
                        source.copyTo(target, overwrite = true)
                        writable.execSQL("UPDATE doctors SET photoUri = ? WHERE id = ?",
                            arrayOf(Uri.fromFile(target).toString(), photo.getLong("doctorId")))
                    }
                }
                val attachments = manifest.optJSONArray("attachments") ?: JSONArray()
                for (i in 0 until attachments.length()) {
                    val mapping = attachments.getJSONObject(i)
                    val target = File(context.filesDir, mapping.getString("entry").removePrefix("files/"))
                    writable.execSQL("UPDATE doctor_attachments SET localPath = ? WHERE localPath = ?",
                        arrayOf(target.absolutePath, mapping.getString("oldPath")))
                    writable.execSQL("UPDATE center_attachments SET localPath = ? WHERE localPath = ?",
                        arrayOf(target.absolutePath, mapping.getString("oldPath")))
                }
            } finally { writable.close() }
        } finally { staging.deleteRecursively() }
    }

    private fun findFolder(token: String): String? {
        val q = URLEncoder.encode("name = '$FOLDER_NAME' and mimeType = 'application/vnd.google-apps.folder' and trashed = false", "UTF-8")
        val connection = URL("$DRIVE/files?q=$q&pageSize=10&fields=files(id,name)").openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $token")
        check(connection.responseCode in 200..299) { "Drive folder lookup failed: ${responseText(connection)}" }
        val files = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).optJSONArray("files") ?: JSONArray()
        connection.disconnect()
        return if (files.length() == 0) null else files.getJSONObject(0).getString("id")
    }

    private fun getOrCreateFolder(token: String): String {
        findFolder(token)?.let { return it }
        val connection = URL("$DRIVE/files?fields=id").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        val body = JSONObject().put("name", FOLDER_NAME).put("mimeType", "application/vnd.google-apps.folder")
        connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        check(connection.responseCode in 200..299) { "Could not create Drive folder: ${responseText(connection)}" }
        val id = JSONObject(connection.inputStream.bufferedReader().use { it.readText() }).getString("id")
        connection.disconnect()
        return id
    }

    private fun responseText(connection: HttpURLConnection): String =
        try { connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "HTTP ${connection.responseCode}" }
        catch (_: Exception) { "HTTP request failed" }
}
