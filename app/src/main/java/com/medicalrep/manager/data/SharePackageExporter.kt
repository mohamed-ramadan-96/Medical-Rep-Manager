package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import com.medicalrep.manager.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object SharePackageExporter {

    @Suppress("UNUSED_PARAMETER")
    suspend fun export(
        context: Context,
        database: MedicalRepDatabase,
        doctorIds: Set<Long>,
        includeAttachments: Boolean,
        output: OutputStream,
        isArabic: Boolean = false
    ) {
        val doctors = if (doctorIds.isEmpty()) {
            emptyList()
        } else {
            database.doctorDao().getByIds(doctorIds.toList())
                .sortedWith(compareBy<DoctorEntity> { it.name.lowercase(Locale.ROOT) }.thenBy { it.id })
        }
        val specialtiesById = database.specialtyDao().getAll().associateBy { it.id }
        val linkedCenterIds = linkedSetOf<Long>()
        val linksByDoctor = linkedMapOf<Long, List<DoctorCenterLinkEntity>>()
        val doctorAvailabilityByDoctor = linkedMapOf<Long, List<DoctorAvailabilityEntity>>()
        val visitsByDoctor = linkedMapOf<Long, List<DoctorVisitEntity>>()
        val doctorAttachmentsByDoctor = linkedMapOf<Long, List<DoctorAttachmentEntity>>()

        doctors.forEach { doctor ->
            val links = database.doctorCenterLinkDao().getForDoctor(doctor.id)
            val availability = database.doctorAvailabilityDao().getForDoctor(doctor.id)
            linksByDoctor[doctor.id] = links
            doctorAvailabilityByDoctor[doctor.id] = availability
            visitsByDoctor[doctor.id] = database.doctorVisitDao().getForDoctor(doctor.id)
            links.forEach { linkedCenterIds.add(it.centerId) }
            doctor.centerId?.let { linkedCenterIds.add(it) }
            availability.mapNotNullTo(linkedCenterIds) { it.centerId }
            if (includeAttachments) {
                doctorAttachmentsByDoctor[doctor.id] =
                    database.doctorAttachmentDao().getForShare(doctor.id)
            }
        }

        val centersById = linkedMapOf<Long, MedicalCenterEntity>()
        if (linkedCenterIds.isNotEmpty()) {
            database.medicalCenterDao().getByIds(linkedCenterIds.toList())
                .forEach { centersById[it.id] = it }
        }
        val centerKeys = centersById.keys.sortedWith(
            compareBy<Long> { centersById.getValue(it).name.lowercase(Locale.ROOT) }.thenBy { it }
        ).mapIndexed { index, id -> id to "c${index + 1}" }.toMap()
        val doctorKeys = doctors.mapIndexed { index, doctor -> doctor.id to "d${index + 1}" }.toMap()

        val centerAvailability = linkedMapOf<Long, List<CenterAvailabilityEntity>>()
        val centerAttachments = linkedMapOf<Long, List<CenterAttachmentEntity>>()
        centersById.keys.forEach { centerId ->
            centerAvailability[centerId] = database.centerAvailabilityDao().getForShare(centerId)
            if (includeAttachments) {
                centerAttachments[centerId] = database.centerAttachmentDao().getForShare(centerId)
            }
        }

        val specialtiesUsed = doctors.mapNotNull { doctor ->
            val resolved = doctor.specialty.trim().ifBlank {
                specialtiesById[doctor.specialtyId]?.name?.trim().orEmpty()
            }
            resolved.takeIf { it.isNotBlank() }
        }.distinct().sortedWith(String.CASE_INSENSITIVE_ORDER)

        val manifest = JSONObject()
            .put("format", "mrm-share")
            .put("formatVersion", 1)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("createdAt", System.currentTimeMillis())
            .put("includesAttachments", includeAttachments)
            .put("specialties", JSONArray().apply { specialtiesUsed.forEach { put(it) } })

        // The wrapper deliberately remains open so ownership of the caller's stream is preserved.
        val zip = ZipOutputStream(output)
        val centerArray = JSONArray()
        centersById.keys.sortedBy { centerKeys.getValue(it) }.forEach { centerId ->
            val center = centersById.getValue(centerId)
            val centerJson = JSONObject()
                .put("key", centerKeys.getValue(centerId))
                .put("name", center.name)
                .put("address", center.address)
                .put("phone", center.phone)
                .put("notes", center.notes)
                .put("area", center.area)
                .putNullable("latitude", center.latitude)
                .putNullable("longitude", center.longitude)
                .putNullable("mapUrl", center.mapUrl)
                .put("unregisteredDoctorCount", center.unregisteredDoctorCount)
                .put("availability", JSONArray().apply {
                    centerAvailability[centerId].orEmpty().forEach { slot ->
                        put(JSONObject()
                            .put("dayOfWeek", slot.dayOfWeek)
                            .put("startTime", slot.startTime)
                            .put("endTime", slot.endTime)
                            .put("notes", slot.notes))
                    }
                })
                .put("attachments", JSONArray())

            if (includeAttachments) {
                val exportedAttachments = JSONArray()
                centerAttachments[centerId].orEmpty().forEachIndexed { index, attachment ->
                    val file = File(attachment.localPath)
                    if (file.isFile) {
                        val entry = "attachments/${centerKeys.getValue(centerId)}/${index + 1}_${sanitizeFileName(attachment.displayName)}"
                        addFile(zip, file, entry)
                        exportedAttachments.put(JSONObject()
                            .put("displayName", attachment.displayName)
                            .put("mimeType", attachment.mimeType)
                            .put("entry", entry))
                    }
                }
                centerJson.put("attachments", exportedAttachments)
            }
            centerArray.put(centerJson)
        }

        val doctorArray = JSONArray()
        doctors.forEach { doctor ->
            val doctorKey = doctorKeys.getValue(doctor.id)
            val doctorLinkedCenterIds = linkedSetOf<Long>().apply {
                linksByDoctor[doctor.id].orEmpty().forEach { add(it.centerId) }
                doctor.centerId?.let(::add)
                doctorAvailabilityByDoctor[doctor.id].orEmpty().mapNotNullTo(this) { it.centerId }
            }
            val specialty = doctor.specialty.trim().ifBlank {
                specialtiesById[doctor.specialtyId]?.name?.trim().orEmpty()
            }
            val doctorJson = JSONObject()
                .put("key", doctorKey)
                .put("name", doctor.name)
                .put("specialty", specialty)
                .put("phoneNumbers", doctor.phoneNumbers)
                .put("notes", doctor.notes)
                .put("area", doctor.area)
                .putNullable("latitude", doctor.latitude)
                .putNullable("longitude", doctor.longitude)
                .putNullable("mapUrl", doctor.mapUrl)
                .put("centerKeys", JSONArray().apply {
                    doctorLinkedCenterIds.mapNotNull { centerKeys[it] }.sorted().forEach { put(it) }
                })
                .put("primaryCenterKey", doctor.centerId?.let { centerKeys[it] } ?: JSONObject.NULL)
                .put("availability", JSONArray().apply {
                    doctorAvailabilityByDoctor[doctor.id].orEmpty().forEach { slot ->
                        put(JSONObject()
                            .put("dayOfWeek", slot.dayOfWeek)
                            .put("startTime", slot.startTime)
                            .put("endTime", slot.endTime)
                            .put("notes", slot.notes)
                            .put("centerKey", slot.centerId?.let { centerKeys[it] } ?: JSONObject.NULL))
                    }
                })
                .put("visits", JSONArray().apply {
                    visitsByDoctor[doctor.id].orEmpty().forEach { visit ->
                        put(JSONObject()
                            .put("visitedAt", visit.visitedAt)
                            .put("outcome", visit.outcome)
                            .put("notes", visit.notes)
                            .putNullable("nextFollowUpAt", visit.nextFollowUpAt)
                            .put("createdAt", visit.createdAt))
                    }
                })
                .put("attachments", JSONArray())

            if (includeAttachments) {
                val exportedAttachments = JSONArray()
                doctorAttachmentsByDoctor[doctor.id].orEmpty().forEachIndexed { index, attachment ->
                    val file = File(attachment.localPath)
                    if (file.isFile) {
                        val entry = "attachments/$doctorKey/${index + 1}_${sanitizeFileName(attachment.displayName)}"
                        addFile(zip, file, entry)
                        exportedAttachments.put(JSONObject()
                            .put("displayName", attachment.displayName)
                            .put("mimeType", attachment.mimeType)
                            .put("entry", entry))
                    }
                }
                doctorJson.put("attachments", exportedAttachments)
                val photoEntry = doctor.photoUri?.takeIf { it.isNotBlank() }?.let { rawUri ->
                    copyPhoto(context, zip, doctorKey, rawUri)
                }
                doctorJson.put("photoEntry", photoEntry ?: JSONObject.NULL)
            }
            doctorArray.put(doctorJson)
        }

        manifest.put("centers", centerArray).put("doctors", doctorArray)
        zip.putNextEntry(ZipEntry("manifest.json").apply { time = 0L })
        zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
        zip.closeEntry()
        zip.finish()
        zip.flush()
    }

    fun suggestedFileName(): String =
        "MRM-share-${SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())}.zip"

    private fun addFile(zip: ZipOutputStream, file: File, entryName: String) {
        zip.putNextEntry(ZipEntry(entryName).apply { time = 0L })
        FileInputStream(file).use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun copyPhoto(
        context: Context,
        zip: ZipOutputStream,
        doctorKey: String,
        rawUri: String
    ): String? {
        return try {
            val uri = Uri.parse(rawUri)
            val extension = (uri.lastPathSegment?.substringAfterLast('.', "jpg") ?: "jpg")
                .substringAfterLast('/').let(::sanitizeExtension).ifBlank { "jpg" }
            val entryName = "photos/$doctorKey.$extension"
            val input: InputStream? = if (uri.scheme == "file") {
                uri.path?.let { path ->
                    File(path).takeIf { it.isFile }?.let(::FileInputStream)
                }
            } else {
                context.contentResolver.openInputStream(uri)
            }
            if (input == null) return null
            val photoBytes = input.use { stream ->
                ByteArrayOutputStream().also { buffer -> stream.copyTo(buffer) }.toByteArray()
            }
            zip.putNextEntry(ZipEntry(entryName).apply { time = 0L })
            zip.write(photoBytes)
            zip.closeEntry()
            entryName
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeFileName(name: String): String {
        val cleaned = name.substringAfterLast('/').substringAfterLast('\\')
            .replace(Regex("[^A-Za-z0-9._-]"), "_")
            .trim('.', '_', '-')
            .take(100)
        return cleaned.ifBlank { "file" }
    }

    private fun sanitizeExtension(extension: String): String =
        extension.lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8)

    private fun JSONObject.putNullable(key: String, value: Any?): JSONObject =
        put(key, value ?: JSONObject.NULL)
}
