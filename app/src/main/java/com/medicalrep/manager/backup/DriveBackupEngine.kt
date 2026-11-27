package com.medicalrep.manager.backup

import android.content.Context
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.util.JsonWriter
import android.util.Log
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
import java.time.OffsetDateTime
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class DriveBackupItem(val id: String, val name: String, val modifiedTime: String, val createdTime: String = "")

object DriveBackupEngine {
    private const val MAX_BACKUPS = 5
    private const val DRIVE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD = "https://www.googleapis.com/upload/drive/v3"
    private const val FOLDER_NAME = "Medical Rep Manager"

    suspend fun createLocalBackup(context: Context): File {
        // Compress existing images before the database snapshot is taken. A compression
        // failure must never prevent a backup from being created.
        try {
            ExistingImageCompression.run(context, MedicalRepDatabase.getInstance(context)) { _, _ -> }
        } catch (error: Exception) {
            Log.w("DriveBackupEngine", "Pre-backup image compression failed; continuing with the backup", error)
        }
        val database = MedicalRepDatabase.getInstance(context)
        val dbFile = context.getDatabasePath("medical_rep_manager.db")
        val snapshot = File(context.cacheDir, "medical-rep-db-snapshot-${UUID.randomUUID()}.db")
        val photoRows = mutableListOf<Pair<Long, String>>()

        // Read photo references and checkpoint WAL while Room is still open.
        database.openHelper.readableDatabase.query(
            "SELECT id, photoUri FROM doctors WHERE photoUri IS NOT NULL AND photoUri != ''"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val rawUri = cursor.getString(1) ?: continue
                photoRows += cursor.getLong(0) to rawUri
            }
        }
        database.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
            check(cursor.moveToFirst() && cursor.getInt(0) == 0) {
                "Could not finish database WAL checkpoint"
            }
        }

        // A standalone copy of the main DB is only safe after Room has closed and
        // SQLite has merged its WAL. Reopen Room immediately after taking the copy.
        MedicalRepDatabase.closeInstance()
        try {
            check(dbFile.isFile && dbFile.length() > 0) { "Database file not found" }
            FileInputStream(dbFile).use { input ->
                FileOutputStream(snapshot).use { output -> input.copyTo(output) }
            }
        } finally {
            MedicalRepDatabase.getInstance(context)
        }

        try {
            val checkDb = SQLiteDatabase.openDatabase(snapshot.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                val requiredTables = listOf(
                    "doctors", "medical_centers", "specialties", "doctor_attachments",
                    "center_attachments", "doctor_availability", "doctor_visits", "doctor_center_links",
                    "center_availability"
                )
                requiredTables.forEach { table ->
                    checkDb.rawQuery(
                        "SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                        arrayOf(table)
                    ).use { cursor ->
                        check(cursor.moveToFirst()) { "Database snapshot is missing table: $table" }
                    }
                }
                checkDb.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "ok") {
                        "Database snapshot integrity check failed"
                    }
                }
            } finally {
                checkDb.close()
            }

            val output = File(context.cacheDir, "medical-rep-backup-${System.currentTimeMillis()}.zip")
            val manifest = JSONObject().put("version", 1)
            val photos = JSONArray()
            val attachments = JSONArray()
            ZipOutputStream(FileOutputStream(output)).use { zip ->
                zip.putNextEntry(ZipEntry("db/medical_rep_manager.db"))
                FileInputStream(snapshot).use { it.copyTo(zip) }
                zip.closeEntry()

                listOf("doctor_attachments", "center_attachments").forEach { folder ->
                    val directory = File(context.filesDir, folder)
                    if (directory.exists()) addDirectory(zip, directory, "files/$folder", attachments)
                }

                photoRows.forEach { (doctorId, rawUri) ->
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
                    } catch (error: Exception) {
                        throw java.io.IOException("Could not include the photo for doctor #$doctorId in the backup. Backup cancelled to prevent silent photo loss.", error)
                    }
                }
                manifest.put("photos", photos).put("attachments", attachments)
                zip.putNextEntry(ZipEntry("backup-manifest.json"))
                zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            return output
        } finally {
            snapshot.delete()
        }
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

    fun upload(context: Context, token: String, backup: File, onProgress: ((Long, Long) -> Unit)? = null): DriveBackupItem {
        var stage = "Drive folder lookup"
        var targetHost = "www.googleapis.com"
        try {
            val folderId = getOrCreateFolder(token)
            val stamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val name = "MedicalRepManager_$stamp.zip"
            val metadata = JSONObject().put("name", name).put("mimeType", "application/zip")
                .put("parents", JSONArray().put(folderId))

            stage = "starting resumable upload session"
            val initUrl = "$UPLOAD/files?uploadType=resumable&fields=id,name,modifiedTime,createdTime"
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
            val totalBytes = backup.length()
            onProgress?.invoke(0L, totalBytes)
            FileInputStream(backup).use { input ->
                put.outputStream.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var uploadedBytes = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        uploadedBytes += count
                        onProgress?.invoke(uploadedBytes, totalBytes)
                    }
                }
            }
            check(put.responseCode in 200..299) { "Backup upload failed: ${responseText(put)}" }
            val result = JSONObject(put.inputStream.bufferedReader().use { it.readText() })
            put.disconnect()
            return DriveBackupItem(result.getString("id"), result.optString("name", name), result.optString("modifiedTime", ""), result.optString("createdTime", ""))
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

    /**
     * Uploads a backup, then best-effort prunes the Drive folder to the five newest
     * backups. The just-uploaded item is never eligible for deletion.
     */
    fun uploadAndRetain(
        context: Context,
        token: String,
        backup: File,
        onProgress: ((Long, Long) -> Unit)? = null
    ): DriveBackupItem {
        val uploaded = upload(context, token, backup, onProgress)
        try {
            val listed = list(token)
            val candidates = if (listed.any { it.id == uploaded.id }) listed else listed + uploaded
            val oldestFirst = candidates.sortedWith(
                compareBy<DriveBackupItem>({ backupTimestamp(it) }, { it.name }, { it.id })
            )
            var remaining = candidates.size
            for (item in oldestFirst) {
                if (remaining <= MAX_BACKUPS) break
                if (item.id == uploaded.id) continue
                try {
                    delete(token, item)
                    remaining--
                } catch (error: Exception) {
                    Log.w("DriveBackupEngine", "Could not remove an expired backup: " + item.id, error)
                }
            }
        } catch (error: Exception) {
            // Cleanup is deliberately non-fatal: the upload has already succeeded.
            Log.w("DriveBackupEngine", "Backup retention cleanup failed", error)
        }
        return uploaded
    }

    private fun backupTimestamp(item: DriveBackupItem): Long {
        val remoteTime = item.createdTime.ifBlank { item.modifiedTime }
        runCatching { OffsetDateTime.parse(remoteTime).toInstant().toEpochMilli() }.getOrNull()?.let { return it }
        val stamp = item.name.removePrefix("MedicalRepManager_").removeSuffix(".zip")
        return runCatching {
            SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).parse(stamp)?.time
        }.getOrNull() ?: Long.MIN_VALUE
    }

    fun list(token: String): List<DriveBackupItem> {
        val folder = findFolder(token) ?: return emptyList()
        val q = URLEncoder.encode("'$folder' in parents and trashed = false", "UTF-8")
        val result = mutableListOf<DriveBackupItem>()
        var pageToken: String? = null
        do {
            val page = pageToken?.let { "&pageToken=${URLEncoder.encode(it, "UTF-8")}" } ?: ""
            val url = URL("$DRIVE/files?q=$q&orderBy=modifiedTime%20desc&pageSize=100&fields=nextPageToken,files(id,name,modifiedTime,createdTime)$page")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 20000
            connection.readTimeout = 30000
            connection.setRequestProperty("Authorization", "Bearer $token")
            try {
                check(connection.responseCode in 200..299) { "Could not list Drive backups: ${responseText(connection)}" }
                val response = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                val files = response.optJSONArray("files") ?: JSONArray()
                for (index in 0 until files.length()) {
                    val item = files.getJSONObject(index)
                    result += DriveBackupItem(item.getString("id"), item.optString("name"), item.optString("modifiedTime"), item.optString("createdTime"))
                }
                pageToken = response.optString("nextPageToken").takeIf { it.isNotBlank() && it != "null" }
            } finally {
                connection.disconnect()
            }
        } while (pageToken != null)
        return result
    }

    fun deleteAll(token: String): Int {
        val items = list(token)
        var deleted = 0
        items.forEach { item ->
            delete(token, item)
            deleted++
        }
        return deleted
    }

    fun delete(token: String, item: DriveBackupItem) {
        val connection = URL("$DRIVE/files/${URLEncoder.encode(item.id, "UTF-8")}").openConnection() as HttpURLConnection
        connection.requestMethod = "DELETE"
        connection.connectTimeout = 20000
        connection.readTimeout = 30000
        connection.setRequestProperty("Authorization", "Bearer $token")
        try {
            val code = connection.responseCode
            check(code in 200..299 || code == HttpURLConnection.HTTP_NO_CONTENT) {
                "Could not delete backup: ${responseText(connection)}"
            }
        } finally {
            connection.disconnect()
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

    fun restoreFromLocalFile(context: Context, archive: File) {
        require(archive.isFile && archive.length() > 0) { "Selected backup file is empty" }
        restoreArchive(context, archive)
    }

    private fun restoreArchive(context: Context, archive: File) {
        require(archive.length() <= 2L * 1024L * 1024L * 1024L) { "Backup archive is too large" }
        val staging = File(context.cacheDir, "restore-stage-${UUID.randomUUID()}").apply { mkdirs() }
        val rollback = File(context.cacheDir, "restore-rollback-${UUID.randomUUID()}").apply { mkdirs() }
        val dbName = "medical_rep_manager.db"
        val attachmentFolders = listOf("doctor_attachments", "center_attachments")
        val dbTarget = context.getDatabasePath(dbName)
        val dbPrepared = File(dbTarget.parentFile, "$dbName.restore-new")
        val dbOld = File(rollback, dbName)
        val oldDbSidecars = listOf("-wal", "-shm").map { File(dbTarget.path + it) }
        val newAttachmentDirs = attachmentFolders.associateWith { File(context.filesDir, "$it.restore-new") }
        val oldAttachmentDirs = attachmentFolders.associateWith { File(rollback, it) }
        var oldDbMoved = false
        val movedSidecars = mutableListOf<Pair<File, File>>()
        val movedAttachmentDirs = mutableListOf<String>()
        var newDbInstalled = false
        var preserveRollback = false
        var roomClosed = false
        val installedAttachmentDirs = mutableListOf<String>()

        try {
            // Extract into private staging and reject traversal, duplicate paths, and oversized archives.
            var extractedBytes = 0L
            var entryCount = 0
            val seenEntries = HashSet<String>()
            ZipInputStream(FileInputStream(archive)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    entryCount++
                    check(entryCount <= 10000) { "Backup archive contains too many entries" }
                    val normalized = entry.name.replace('\\', '/')
                    check(normalized.isNotBlank() && !normalized.startsWith("/") &&
                        normalized.split('/').none { it == ".." || it.isBlank() }) { "Invalid backup archive path" }
                    check(seenEntries.add(normalized)) { "Backup archive contains duplicate entries" }
                    val target = File(staging, normalized)
                    check(target.canonicalPath.startsWith(staging.canonicalPath + File.separator)) { "Invalid backup archive path" }
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { output ->
                            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                extractedBytes += count
                                check(extractedBytes <= 2L * 1024L * 1024L * 1024L) { "Backup archive is too large" }
                                output.write(buffer, 0, count)
                            }
                        }
                    }
                    zip.closeEntry()
                }
            }

            val dbSource = File(staging, "db/$dbName")
            val manifestFile = File(staging, "backup-manifest.json")
            check(dbSource.isFile && dbSource.length() > 0 && manifestFile.isFile) { "Backup archive is incomplete" }
            val manifest = JSONObject(manifestFile.readText())
            check(manifest.optInt("version") == 1) { "Unsupported backup version" }
            check(manifest.has("photos") && manifest.has("attachments")) { "Backup manifest is incomplete" }
            val photos = manifest.getJSONArray("photos")
            val attachments = manifest.getJSONArray("attachments")

            // Prepare all restored files without touching the currently installed data.
            attachmentFolders.forEach { folder ->
                val source = File(staging, "files/$folder")
                val prepared = newAttachmentDirs.getValue(folder)
                prepared.deleteRecursively()
                if (source.exists()) {
                    check(source.isDirectory) { "Invalid $folder backup directory" }
                    source.copyRecursively(prepared, overwrite = true)
                } else prepared.mkdirs()
            }

            // Prepare and validate the replacement database in a separate file.
            dbPrepared.delete()
            dbSource.copyTo(dbPrepared, overwrite = true)
            val writable = SQLiteDatabase.openDatabase(dbPrepared.path, null, SQLiteDatabase.OPEN_READWRITE)
            try {
                fun hasTable(name: String): Boolean {
                    writable.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", arrayOf(name)).use { return it.moveToFirst() }
                }
                val requiredTables = listOf(
                    "doctors", "medical_centers", "specialties", "doctor_attachments",
                    "center_attachments", "doctor_availability", "doctor_visits", "doctor_center_links"
                )
                requiredTables.forEach { table ->
                    check(hasTable(table)) { "Backup database is missing required table: $table" }
                }
                writable.rawQuery("PRAGMA user_version", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getInt(0) in 12..15) {
                        "Unsupported database schema version; expected version 12 through 15"
                    }
                }
                writable.rawQuery("PRAGMA user_version", null).use { cursor ->
                    check(cursor.moveToFirst())
                    if (cursor.getInt(0) >= 15) {
                        check(hasTable("center_availability")) { "Backup database is missing center availability" }
                    }
                }
                writable.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "Backup database integrity check failed" }
                }
                writable.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                    check(!cursor.moveToFirst()) { "Backup database contains invalid relationships" }
                }

                // Never leave references to photos from the device that created the backup.
                writable.execSQL("UPDATE doctors SET photoUri = NULL")
                val restoredPhotoIds = HashSet<Long>()
                for (i in 0 until photos.length()) {
                    val photo = photos.getJSONObject(i)
                    val entry = photo.getString("entry")
                    check(entry.startsWith("photos/") && !entry.contains("..")) { "Invalid photo entry in manifest" }
                    val source = File(staging, entry)
                    check(source.isFile && source.length() > 0) { "A doctor photo is missing from the backup" }
                    val doctorId = photo.getLong("doctorId")
                    val target = File(File(context.filesDir, "doctor_photos.restore-new"), source.name)
                    target.parentFile?.mkdirs()
                    source.copyTo(target, overwrite = true)
                    val restoredUri = Uri.fromFile(File(File(context.filesDir, "doctor_photos"), source.name)).toString()
                    writable.execSQL("UPDATE doctors SET photoUri = ? WHERE id = ?",
                        arrayOf(restoredUri, doctorId))
                    writable.rawQuery("SELECT photoUri FROM doctors WHERE id = ?", arrayOf(doctorId.toString())).use { cursor ->
                        check(cursor.moveToFirst() && cursor.getString(0) == restoredUri) {
                            "Backup photo refers to a doctor that does not exist"
                        }
                    }
                    check(restoredPhotoIds.add(doctorId)) { "Backup manifest contains duplicate doctor photos" }
                }

                // Restore attachment paths in the database and verify every referenced file exists.
                val mappedOldPaths = HashSet<String>()
                for (i in 0 until attachments.length()) {
                    val mapping = attachments.getJSONObject(i)
                    val oldPath = mapping.getString("oldPath")
                    val entry = mapping.getString("entry")
                    check(entry.startsWith("files/") && !entry.contains("..")) { "Invalid attachment entry in manifest" }
                    val relative = entry.removePrefix("files/")
                    check(relative.startsWith("doctor_attachments/") || relative.startsWith("center_attachments/")) {
                        "Invalid attachment folder in backup"
                    }
                    val source = File(staging, entry)
                    check(source.isFile && source.length() > 0) { "An attachment is missing from the backup" }
                    val folder = relative.substringBefore('/')
                    val target = File(context.filesDir, relative)
                    check(target.canonicalPath.startsWith(File(context.filesDir, folder).canonicalPath + File.separator)) {
                        "Invalid attachment destination"
                    }
                    val table = if (folder == "doctor_attachments") "doctor_attachments" else "center_attachments"
                    writable.execSQL("UPDATE $table SET localPath = ? WHERE localPath = ?", arrayOf(target.absolutePath, oldPath))
                    mappedOldPaths.add(oldPath)
                }

                // Validate every database attachment reference against the prepared payload.
                for (table in attachmentFolders) {
                    writable.rawQuery("SELECT localPath FROM $table", null).use { cursor ->
                        while (cursor.moveToNext()) {
                            val path = cursor.getString(0)
                            val relative = path.substringAfterLast("/$table/", "")
                            check(relative.isNotBlank()) { "Backup contains an invalid $table path" }
                            val stagedFile = File(newAttachmentDirs.getValue(table), relative)
                            check(stagedFile.isFile) { "Backup is missing an attachment referenced by the database" }
                        }
                    }
                }
            } finally {
                writable.close()
            }

            // Close Room only after all archive and database validation has passed.
            MedicalRepDatabase.closeInstance()
            roomClosed = true
            dbTarget.parentFile?.mkdirs()
            dbOld.delete()
            if (dbTarget.exists()) {
                check(dbTarget.renameTo(dbOld)) { "Could not preserve the current database before restore" }
                oldDbMoved = true
            }
            oldDbSidecars.forEach { sidecar ->
                if (sidecar.exists()) {
                    val saved = File(rollback, sidecar.name)
                    check(sidecar.renameTo(saved)) { "Could not preserve database journal before restore" }
                    movedSidecars.add(sidecar to saved)
                }
            }
            attachmentFolders.forEach { folder ->
                val current = File(context.filesDir, folder)
                val saved = oldAttachmentDirs.getValue(folder)
                saved.parentFile?.mkdirs()
                if (current.exists()) {
                    check(current.renameTo(saved)) { "Could not preserve existing $folder before restore" }
                    movedAttachmentDirs.add(folder)
                }
            }
            val photosCurrent = File(context.filesDir, "doctor_photos")
            val photosPrepared = File(context.filesDir, "doctor_photos.restore-new")
            val photosOld = File(rollback, "doctor_photos")
            if (photosCurrent.exists()) {
                check(photosCurrent.renameTo(photosOld)) { "Could not preserve existing doctor photos before restore" }
            }
            if (photosPrepared.exists()) {
                check(photosPrepared.renameTo(photosCurrent)) { "Could not install restored doctor photos" }
                installedAttachmentDirs.add("doctor_photos")
            }

            check(dbPrepared.renameTo(dbTarget)) { "Could not install restored database" }
            newDbInstalled = true
            attachmentFolders.forEach { folder ->
                val prepared = newAttachmentDirs.getValue(folder)
                val current = File(context.filesDir, folder)
                check(prepared.renameTo(current)) { "Could not install restored $folder" }
                installedAttachmentDirs.add(folder)
            }

            // Re-open the installed file directly and run final integrity checks before commit.
            val installed = SQLiteDatabase.openDatabase(dbTarget.path, null, SQLiteDatabase.OPEN_READONLY)
            try {
                installed.rawQuery("PRAGMA integrity_check", null).use { cursor ->
                    check(cursor.moveToFirst() && cursor.getString(0) == "ok") { "Restored database failed final integrity check" }
                }
                installed.rawQuery("PRAGMA foreign_key_check", null).use { cursor ->
                    check(!cursor.moveToFirst()) { "Restored database failed relationship validation" }
                }
            } finally { installed.close() }
        } catch (error: Exception) {
            // Roll back every installed component and restore the previous database/files.
            try {
                if (roomClosed) MedicalRepDatabase.closeInstance()
                installedAttachmentDirs.asReversed().forEach { File(context.filesDir, it).deleteRecursively() }
                if (newDbInstalled) dbTarget.delete()
                movedAttachmentDirs.asReversed().forEach { folder ->
                    val saved = oldAttachmentDirs.getValue(folder)
                    val current = File(context.filesDir, folder)
                    if (saved.exists()) {
                        current.deleteRecursively()
                        check(saved.renameTo(current)) { "Rollback failed for $folder" }
                    }
                }
                val oldPhotos = File(rollback, "doctor_photos")
                if (oldPhotos.exists()) {
                    File(context.filesDir, "doctor_photos").deleteRecursively()
                    check(oldPhotos.renameTo(File(context.filesDir, "doctor_photos"))) { "Rollback failed for doctor photos" }
                }
                movedSidecars.asReversed().forEach { (original, saved) ->
                    if (saved.exists()) check(saved.renameTo(original)) { "Rollback failed for database journal" }
                }
                if (oldDbMoved && dbOld.exists()) check(dbOld.renameTo(dbTarget)) { "Rollback failed for database" }
            } catch (rollbackError: Exception) {
                preserveRollback = true
                throw java.io.IOException("Restore failed and rollback was incomplete: ${rollbackError.localizedMessage}. Recovery files were preserved in app cache.", error)
            }
            throw error
        } finally {
            dbPrepared.delete()
            newAttachmentDirs.values.forEach { it.deleteRecursively() }
            File(context.filesDir, "doctor_photos.restore-new").deleteRecursively()
            staging.deleteRecursively()
            if (!preserveRollback) rollback.deleteRecursively()
            if (roomClosed) runCatching { MedicalRepDatabase.getInstance(context) }
        }
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
