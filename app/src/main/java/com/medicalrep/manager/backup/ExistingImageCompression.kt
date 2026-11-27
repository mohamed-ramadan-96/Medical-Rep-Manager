package com.medicalrep.manager.backup

import android.content.Context
import android.net.Uri
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.util.ImageCompression
import java.io.File
import java.util.UUID

data class ExistingImageCompressionResult(
    val total: Int, val processed: Int, val compressed: Int,
    val skipped: Int, val failed: Int, val savedBytes: Long
)

object ExistingImageCompression {
    suspend fun run(context: Context, database: MedicalRepDatabase, onProgress: (Int, Int) -> Unit): ExistingImageCompressionResult {
        val doctors = database.doctorDao().getAllForImport()
        val doctorAttachments = database.doctorAttachmentDao().getAll()
        val centerAttachments = database.centerAttachmentDao().getAll()
        val photoDoctors = doctors.filter { !it.photoUri.isNullOrBlank() }
        val imageDoctorAttachments = doctorAttachments.filter { it.mimeType.startsWith("image/", true) }
        val imageCenterAttachments = centerAttachments.filter { it.mimeType.startsWith("image/", true) }
        val total = photoDoctors.size + imageDoctorAttachments.size + imageCenterAttachments.size
        var processed = 0
        var compressed = 0
        var skipped = 0
        var failed = 0
        var savedBytes = 0L
        fun advance() { processed++; onProgress(processed, total) }

        photoDoctors.forEach { doctor ->
            try {
                val rawUri = doctor.photoUri.orEmpty()
                val uri = Uri.parse(rawUri).let { if (it.scheme == null) Uri.fromFile(File(rawUri)) else it }
                val originalSize = ImageCompression.uriLength(context, uri)
                if (originalSize == null) skipped++
                else {
                    val directory = File(context.filesDir, "doctor_photos").apply { mkdirs() }
                    val target = File(directory, "doctor_${doctor.id}_${UUID.randomUUID()}.jpg")
                    val created = ImageCompression.compressUriToJpeg(context, uri, target, 1000, 80)
                    if (!created) { target.delete(); failed++ }
                    else if (target.length() >= originalSize) { target.delete(); skipped++ }
                    else try {
                        database.doctorDao().update(doctor.copy(photoUri = Uri.fromFile(target).toString()))
                        savedBytes += originalSize - target.length()
                        compressed++
                    } catch (_: Exception) { target.delete(); failed++ }
                }
            } catch (_: Exception) { failed++ }
            finally { advance() }
        }

        imageDoctorAttachments.forEach { attachment ->
            try {
                val saved = compressAttachment(context, File(attachment.localPath), 2000, 85) {
                    database.doctorAttachmentDao().update(attachment.copy(mimeType = "image/jpeg"))
                }
                when { saved == null -> failed++; saved == 0L -> skipped++; else -> { compressed++; savedBytes += saved } }
            } catch (_: Exception) { failed++ }
            finally { advance() }
        }
        imageCenterAttachments.forEach { attachment ->
            try {
                val saved = compressAttachment(context, File(attachment.localPath), 2000, 85) {
                    database.centerAttachmentDao().update(attachment.copy(mimeType = "image/jpeg"))
                }
                when { saved == null -> failed++; saved == 0L -> skipped++; else -> { compressed++; savedBytes += saved } }
            } catch (_: Exception) { failed++ }
            finally { advance() }
        }
        return ExistingImageCompressionResult(total, processed, compressed, skipped, failed, savedBytes)
    }

    private suspend fun compressAttachment(
        context: Context, original: File, maxLongSide: Int, quality: Int, updateRecord: suspend () -> Unit
    ): Long? {
        if (!original.isFile || original.length() <= 0L) return null
        val originalSize = original.length()
        val temporary = File(original.parentFile, ".${original.name}.${UUID.randomUUID()}.jpg")
        val backup = File(original.parentFile, ".${original.name}.${UUID.randomUUID()}.original")
        if (!ImageCompression.compressUriToJpeg(context, Uri.fromFile(original), temporary, maxLongSide, quality)) {
            temporary.delete()
            return null
        }
        if (temporary.length() >= originalSize) { temporary.delete(); return 0L }
        if (!runCatching { original.copyTo(backup, overwrite = false) }.isSuccess) {
            temporary.delete()
            backup.delete()
            return null
        }
        try {
            android.system.Os.rename(temporary.absolutePath, original.absolutePath)
        } catch (_: Exception) {
            temporary.delete()
            backup.delete()
            return null
        }
        return try {
            updateRecord()
            backup.delete()
            originalSize - original.length()
        } catch (error: Exception) {
            val restored = runCatching {
                android.system.Os.rename(backup.absolutePath, original.absolutePath)
            }.isSuccess || runCatching {
                backup.copyTo(original, overwrite = true)
                true
            }.getOrDefault(false)
            if (!restored) {
                throw java.io.IOException(
                    "Attachment metadata update failed and the original image could not be restored from ${backup.absolutePath}",
                    error
                )
            }
            backup.delete()
            throw error
        }
    }
}
