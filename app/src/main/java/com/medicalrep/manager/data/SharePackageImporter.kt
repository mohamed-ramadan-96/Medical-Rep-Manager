package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.medicalrep.manager.domain.ContactPhoneRules
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

data class SharePlan(
    val newDoctors: Int, val updatedDoctors: Int, val sameNameDifferentPhone: Int,
    val newCenters: Int, val updatedCenters: Int, val doctorSlotsToAdd: Int,
    val centerSlotsToAdd: Int, val attachmentsToCopy: Int,
    val includesAttachments: Boolean, val warnings: List<String>
)

data class ShareApplyResult(
    val newDoctors: Int, val updatedDoctors: Int, val sameNameDifferentPhone: Int,
    val newCenters: Int, val updatedCenters: Int, val doctorSlotsToAdd: Int,
    val centerSlotsToAdd: Int, val attachmentsToCopy: Int,
    val includesAttachments: Boolean, val warnings: List<String>
)

object SharePackageImporter {
    private const val MAX_MANIFEST_BYTES = 20L * 1024L * 1024L

    private data class ParsedPackage(
        val includesAttachments: Boolean, val specialties: List<String>,
        val centers: JSONArray, val doctors: JSONArray
    )
    private data class CenterMerge(val key: String, val localId: Long, val existing: Boolean, val entity: MedicalCenterEntity)
    private data class DoctorMerge(
        val key: String, val localId: Long, val existing: Boolean, val entity: DoctorEntity,
        val specialtyName: String, val centerKeys: List<String>, val primaryCenterKey: String?
    )
    private data class MergeResult(
        val centers: List<CenterMerge>, val doctors: List<DoctorMerge>,
        val doctorSlots: List<DoctorAvailabilityEntity>, val centerSlots: List<CenterAvailabilityEntity>,
        val links: Set<Pair<Long, Long>>, val localIds: Map<String, Long>, val specialtyNames: List<String>,
        val newDoctors: Int, val updatedDoctors: Int, val sameNameDifferentPhone: Int,
        val newCenters: Int, val updatedCenters: Int,
        val includesAttachments: Boolean, val warnings: List<String>
    ) {
        fun toPlan() = SharePlan(
            newDoctors, updatedDoctors, sameNameDifferentPhone, newCenters, updatedCenters,
            doctorSlots.size, centerSlots.size, 0, includesAttachments, warnings
        )
    }

    suspend fun plan(context: Context, database: MedicalRepDatabase, uri: Uri): SharePlan =
        computeMerge(database, readManifest(context, uri)).toPlan()

    private const val MAX_ENTRY_BYTES = 32L * 1024L * 1024L
    private const val MAX_TOTAL_ATTACHMENT_BYTES = 128L * 1024L * 1024L

    private data class PackageFile(
        val entry: String,
        val ownerKey: String,
        val displayName: String,
        val mimeType: String,
        val isPhoto: Boolean,
        val shouldCopy: Boolean,
        var file: File? = null,
        var byteSize: Long = 0L
    )

    suspend fun apply(context: Context, database: MedicalRepDatabase, uri: Uri): ShareApplyResult {
        val parsed = readManifest(context, uri)
        val merged = computeMerge(database, parsed)
        val createdFiles = mutableListOf<File>()
        var copiedCount = 0
        try {
            val packageFiles = if (merged.includesAttachments) {
                buildPackageFiles(parsed, merged, database)
            } else emptyList()
            if (merged.includesAttachments) {
                copiedCount = copyPackageFiles(context, uri, packageFiles, createdFiles)
            }

            database.withTransaction {
                val specialtyDao = database.specialtyDao()
                val specialtiesByName = specialtyDao.getAll()
                    .associateBy { normalize(it.name) }
                    .toMutableMap()
                val specialtyIds = linkedMapOf<String, Long>()
                merged.specialtyNames.forEach { rawName ->
                    val name = rawName.trim()
                    if (name.isNotBlank()) {
                        val key = normalize(name)
                        val existing = specialtiesByName[key]
                        val id = existing?.id ?: specialtyDao.insert(SpecialtyEntity(name = name)).also {
                            specialtiesByName[key] = SpecialtyEntity(id = it, name = name)
                        }
                        specialtyIds[key] = id
                    }
                }

                val centerIdMap = linkedMapOf<Long, Long>()
                merged.centers.groupBy { it.localId }.values.map { it.last() }.forEach { plan ->
                    val entity = plan.entity
                    val realId = if (plan.existing) {
                        database.medicalCenterDao().update(entity)
                        plan.localId
                    } else {
                        database.medicalCenterDao().insert(entity.copy(id = 0L))
                    }
                    centerIdMap[plan.localId] = realId
                }

                val doctorIdMap = linkedMapOf<Long, Long>()
                merged.doctors.groupBy { it.localId }.values.map { it.last() }.forEach { plan ->
                    val entity = plan.entity
                    val specialtyKey = normalize(plan.specialtyName)
                    val specialtyId = if (specialtyKey.isBlank()) null else specialtyIds[specialtyKey]
                    val mappedCenterId = entity.centerId?.let { centerIdMap[it] ?: it }
                    val mergedEntity = entity.copy(
                        specialtyId = specialtyId ?: entity.specialtyId,
                        centerId = mappedCenterId
                    )
                    val realId = if (plan.existing) {
                        database.doctorDao().update(mergedEntity)
                        plan.localId
                    } else {
                        database.doctorDao().insert(mergedEntity.copy(id = 0L))
                    }
                    doctorIdMap[plan.localId] = realId
                }

                val linksToInsert = merged.links.mapNotNull { (doctorId, centerId) ->
                    val realDoctorId = doctorIdMap[doctorId] ?: doctorId
                    val realCenterId = centerIdMap[centerId] ?: centerId
                    DoctorCenterLinkEntity(realDoctorId, realCenterId)
                }.distinct()
                if (linksToInsert.isNotEmpty()) {
                    database.doctorCenterLinkDao().insertAll(linksToInsert)
                }

                merged.doctorSlots.forEach { slot ->
                    val doctorId = doctorIdMap[slot.doctorId] ?: slot.doctorId
                    val centerId = slot.centerId?.let { centerIdMap[it] ?: it }
                    database.doctorAvailabilityDao().insert(
                        slot.copy(id = 0L, doctorId = doctorId, centerId = centerId)
                    )
                }
                merged.centerSlots.forEach { slot ->
                    val centerId = centerIdMap[slot.centerId] ?: slot.centerId
                    database.centerAvailabilityDao().insert(
                        slot.copy(id = 0L, centerId = centerId)
                    )
                }

                packageFiles.filter { it.file != null }.forEach { item ->
                    val file = item.file ?: return@forEach
                    if (item.isPhoto) {
                        val packageKey = item.ownerKey.removePrefix("d:")
                        val localId = merged.localIds["doctor:$packageKey"] ?: return@forEach
                        val doctorId = doctorIdMap[localId] ?: localId
                        val doctor = database.doctorDao().getById(doctorId) ?: return@forEach
                        if (doctor.photoUri.isNullOrBlank()) {
                            database.doctorDao().update(
                                doctor.copy(photoUri = Uri.fromFile(file).toString())
                            )
                        } else {
                            file.delete()
                            createdFiles.remove(file)
                            copiedCount--
                        }
                    } else if (item.ownerKey.startsWith("d:")) {
                        val packageKey = item.ownerKey.removePrefix("d:")
                        val localId = merged.localIds["doctor:$packageKey"] ?: return@forEach
                        val doctorId = doctorIdMap[localId] ?: localId
                        val existing = database.doctorAttachmentDao().getForDoctor(doctorId)
                        val duplicate = existing.any { attachment ->
                            attachment.displayName == item.displayName &&
                                File(attachment.localPath).length() == item.byteSize
                        }
                        if (duplicate) {
                            file.delete()
                            createdFiles.remove(file)
                            copiedCount--
                        } else {
                            database.doctorAttachmentDao().insert(
                                DoctorAttachmentEntity(
                                    doctorId = doctorId, displayName = item.displayName,
                                    mimeType = item.mimeType, localPath = file.absolutePath
                                )
                            )
                        }
                    } else {
                        val packageKey = item.ownerKey.removePrefix("c:")
                        val localId = merged.localIds["center:$packageKey"] ?: return@forEach
                        val centerId = centerIdMap[localId] ?: localId
                        val existing = database.centerAttachmentDao().getForCenter(centerId)
                        val duplicate = existing.any { attachment ->
                            attachment.displayName == item.displayName &&
                                File(attachment.localPath).length() == item.byteSize
                        }
                        if (duplicate) {
                            file.delete()
                            createdFiles.remove(file)
                            copiedCount--
                        } else {
                            database.centerAttachmentDao().insert(
                                CenterAttachmentEntity(
                                    centerId = centerId, displayName = item.displayName,
                                    mimeType = item.mimeType, localPath = file.absolutePath
                                )
                            )
                        }
                    }
                }
            }

            return ShareApplyResult(
                newDoctors = merged.newDoctors,
                updatedDoctors = merged.updatedDoctors,
                sameNameDifferentPhone = merged.sameNameDifferentPhone,
                newCenters = merged.newCenters,
                updatedCenters = merged.updatedCenters,
                doctorSlotsToAdd = merged.doctorSlots.size,
                centerSlotsToAdd = merged.centerSlots.size,
                attachmentsToCopy = copiedCount,
                includesAttachments = merged.includesAttachments,
                warnings = merged.warnings
            )
        } catch (error: Exception) {
            createdFiles.forEach { file -> runCatching { file.delete() } }
            throw error
        }
    }

    private suspend fun buildPackageFiles(
        parsed: ParsedPackage,
        merged: MergeResult,
        database: MedicalRepDatabase
    ): List<PackageFile> {
        val files = mutableListOf<PackageFile>()
        val doctorByKey = merged.doctors.associateBy { it.key }
        val centerByKey = merged.centers.associateBy { it.key }
        val doctorAttachments = database.doctorAttachmentDao().getAll()
        val centerAttachments = database.centerAttachmentDao().getAll()
        parsed.doctors.let { array ->
            forEachObject(array) { doctor ->
                val key = requiredString(doctor, "key")
                val plan = doctorByKey[key] ?: return@forEachObject
                val existing = if (plan.existing) doctorAttachments.filter { it.doctorId == plan.localId } else emptyList()
                val attachments = doctor.optJSONArray("attachments") ?: JSONArray()
                forEachObject(attachments) { attachment ->
                    val entry = requiredString(attachment, "entry")
                    validateEntryName(entry)
                    val name = requiredString(attachment, "displayName")
                    files.add(PackageFile(
                        entry, "d:$key", name, attachment.optString("mimeType").ifBlank { "application/octet-stream" },
                        false, true
                    ))
                }
                nullableString(doctor, "photoEntry")?.let { entry ->
                    validateEntryName(entry)
                    val currentPhoto = plan.entity.photoUri
                    files.add(PackageFile(entry, "d:$key", "photo", "image/jpeg", true, currentPhoto.isNullOrBlank()))
                }
            }
        }
        parsed.centers.let { array ->
            forEachObject(array) { center ->
                val key = requiredString(center, "key")
                val plan = centerByKey[key] ?: return@forEachObject
                val existing = if (plan.existing) centerAttachments.filter { it.centerId == plan.localId } else emptyList()
                val attachments = center.optJSONArray("attachments") ?: JSONArray()
                forEachObject(attachments) { attachment ->
                    val entry = requiredString(attachment, "entry")
                    validateEntryName(entry)
                    val name = requiredString(attachment, "displayName")
                    files.add(PackageFile(
                        entry, "c:$key", name, attachment.optString("mimeType").ifBlank { "application/octet-stream" },
                        false, true
                    ))
                }
            }
        }
        if (files.map { it.entry }.distinct().size != files.size) {
            throw IllegalStateException("The package references the same attachment entry more than once.")
        }
        return files
    }

    private fun copyPackageFiles(
        context: Context,
        uri: Uri,
        packageFiles: List<PackageFile>,
        createdFiles: MutableList<File>
    ): Int {
        if (packageFiles.isEmpty()) return 0
        val byEntry = packageFiles.associateBy { it.entry }
        val seenEntries = mutableSetOf<String>()
        var totalBytes = 0L
        var copied = 0
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("Cannot reopen the selected share package.")
        try {
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    validateEntryName(entry.name)
                    if (!entry.isDirectory && !seenEntries.add(entry.name)) {
                        throw IllegalStateException("The package contains duplicate ZIP entries.")
                    }
                    val item = byEntry[entry.name]
                    if (item == null || entry.isDirectory) {
                        while (zip.read() != -1) Unit
                    } else {
                        val directory = when {
                            item.isPhoto -> File(context.filesDir, "doctor_photos")
                            item.ownerKey.startsWith("d:") -> File(context.filesDir, "doctor_attachments")
                            else -> File(context.filesDir, "center_attachments")
                        }
                        if (item.shouldCopy) {
                            if (!directory.exists() && !directory.mkdirs()) {
                                throw IllegalStateException("Cannot create the private attachment folder.")
                            }
                            val extension = item.displayName.substringAfterLast('.', "")
                                .lowercase(Locale.ROOT).filter { it.isLetterOrDigit() }.take(8)
                                .ifBlank { entry.name.substringAfterLast('.', "").filter { it.isLetterOrDigit() }.take(8) }
                            val file = File(directory, UUID.randomUUID().toString() + if (extension.isBlank()) "" else ".$extension")
                            if (!file.createNewFile()) throw IllegalStateException("Cannot create an imported attachment file.")
                            createdFiles.add(file)
                            item.file = file
                        }
                        val output = item.file?.let { FileOutputStream(it) }
                        try {
                            val buffer = ByteArray(8192)
                            var entryBytes = 0L
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                entryBytes += count
                                totalBytes += count
                                if (entryBytes > MAX_ENTRY_BYTES) {
                                    throw IllegalStateException("An attachment exceeds the 32 MB size limit.")
                                }
                                if (totalBytes > MAX_TOTAL_ATTACHMENT_BYTES) {
                                    throw IllegalStateException("Attachments exceed the 128 MB total size limit.")
                                }
                                output?.write(buffer, 0, count)
                            }
                            item.byteSize = entryBytes
                            if (item.file != null) copied++
                        } finally {
                            output?.close()
                        }
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException("The package attachments could not be extracted.", error)
        }
        val missing = byEntry.keys - seenEntries
        if (missing.isNotEmpty()) throw IllegalStateException("The package is missing a referenced attachment.")
        return copied
    }

    private suspend fun computeMerge(database: MedicalRepDatabase, parsed: ParsedPackage): MergeResult {
        val centers = database.medicalCenterDao().observeAll().first().toMutableList()
        val doctors = database.doctorDao().getAllForImport().toMutableList()
        val centerPlans = linkedMapOf<String, CenterMerge>()
        val doctorPlansById = linkedMapOf<Long, DoctorMerge>()
        val doctorPlansByKey = linkedMapOf<String, DoctorMerge>()
        val localIds = linkedMapOf<String, Long>()
        var nextTemporaryId = -1L
        var newCenters = 0
        var updatedCenters = 0
        var newDoctors = 0
        var updatedDoctors = 0
        var differentPhones = 0
        val updatedCenterIds = mutableSetOf<Long>()
        val updatedDoctorIds = mutableSetOf<Long>()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

        fun allocate(type: String, key: String, existingId: Long?): Long =
            localIds.getOrPut("$type:$key") { existingId ?: nextTemporaryId-- }

        forEachObject(parsed.centers) { source ->
            val key = requiredString(source, "key")
            val name = normalize(requiredString(source, "name"))
            val area = normalize(source.optString("area"))
            val sameName = centers.filter { normalize(it.name) == name }
            val match = sameName.firstOrNull { existing ->
                val oldArea = normalize(existing.area)
                if (area.isNotBlank() && oldArea.isNotBlank()) area == oldArea else sameName.size == 1
            }
            val localId = allocate("center", key, match?.id)
            val entity = if (match == null) {
                newCenters++
                MedicalCenterEntity(
                    id = localId, name = source.optString("name").trim(),
                    address = source.optString("address").trim(), phone = source.optString("phone").trim(),
                    notes = source.optString("notes").trim(), area = source.optString("area").trim(),
                    latitude = nullableDouble(source, "latitude"), longitude = nullableDouble(source, "longitude"),
                    mapUrl = nullableString(source, "mapUrl")
                )
            } else {
                match.copy(
                    address = fill(match.address, source.optString("address")),
                    phone = fill(match.phone, source.optString("phone")),
                    notes = mergeNotes(match.notes, source.optString("notes"), today),
                    latitude = match.latitude ?: nullableDouble(source, "latitude"),
                    longitude = match.longitude ?: nullableDouble(source, "longitude"),
                    mapUrl = if (match.mapUrl.isNullOrBlank()) nullableString(source, "mapUrl") else match.mapUrl
                ).also { if (match.id > 0 && it != match && updatedCenterIds.add(match.id)) updatedCenters++ }
            }
            centerPlans[key] = CenterMerge(key, localId, match != null && match.id > 0, entity)
            val index = centers.indexOfFirst { it.id == localId }
            if (index >= 0) centers[index] = entity else centers.add(entity)
        }

        forEachObject(parsed.doctors) { source ->
            val key = requiredString(source, "key")
            val phones = ContactPhoneRules.split(source.optString("phoneNumbers"))
                .mapNotNull(ContactPhoneRules::normalize).toSet()
            val name = normalize(requiredString(source, "name"))
            val area = normalize(source.optString("area"))
            val match = if (phones.isNotEmpty()) {
                doctors.firstOrNull { existing ->
                    ContactPhoneRules.split(existing.phoneNumbers).mapNotNull(ContactPhoneRules::normalize).any(phones::contains)
                }
            } else {
                doctors.firstOrNull { existing ->
                    normalize(existing.name) == name && area.isNotBlank() &&
                        normalize(existing.area).isNotBlank() && normalize(existing.area) == area
                }
            }
            val hasDifferentPhone = phones.isNotEmpty() && doctors.any { existing ->
                normalize(existing.name) == name &&
                    ContactPhoneRules.split(existing.phoneNumbers).mapNotNull(ContactPhoneRules::normalize).none(phones::contains)
            }
            val localId = allocate("doctor", key, match?.id)
            val incomingSpecialty = source.optString("specialty").trim()
            val centerKeys = stringArray(source.optJSONArray("centerKeys"))
            val primaryCenterKey = nullableString(source, "primaryCenterKey")
            val entity = if (match == null) {
                newDoctors++
                if (hasDifferentPhone) differentPhones++
                DoctorEntity(
                    id = localId, name = source.optString("name").trim(), specialty = incomingSpecialty,
                    phoneNumbers = source.optString("phoneNumbers").trim(), notes = source.optString("notes").trim(),
                    area = source.optString("area").trim(),
                    centerId = primaryCenterKey?.let { localIds["center:$it"] },
                    latitude = nullableDouble(source, "latitude"), longitude = nullableDouble(source, "longitude"),
                    mapUrl = nullableString(source, "mapUrl")
                )
            } else {
                match.copy(
                    specialty = fill(match.specialty, incomingSpecialty),
                    area = fill(match.area, source.optString("area")),
                    latitude = match.latitude ?: nullableDouble(source, "latitude"),
                    longitude = match.longitude ?: nullableDouble(source, "longitude"),
                    mapUrl = if (match.mapUrl.isNullOrBlank()) nullableString(source, "mapUrl") else match.mapUrl,
                    phoneNumbers = appendPhones(match.phoneNumbers, source.optString("phoneNumbers")),
                    notes = mergeNotes(match.notes, source.optString("notes"), today),
                    centerId = match.centerId ?: primaryCenterKey?.let { localIds["center:$it"] }
                ).also { if (match.id > 0 && it != match && updatedDoctorIds.add(match.id)) updatedDoctors++ }
            }
            val plan = DoctorMerge(key, localId, match != null && match.id > 0, entity, entity.specialty.trim(), centerKeys, primaryCenterKey)
            doctorPlansByKey[key] = plan
            doctorPlansById[localId] = plan
            val index = doctors.indexOfFirst { it.id == localId }
            if (index >= 0) doctors[index] = entity else doctors.add(entity)
        }

        val existingLinks = mutableSetOf<Pair<Long, Long>>()
        doctorPlansById.values.filter { it.existing }.forEach { doctor ->
            database.doctorCenterLinkDao().getForDoctor(doctor.localId).forEach {
                existingLinks.add(doctor.localId to it.centerId)
            }
        }
        val links = linkedSetOf<Pair<Long, Long>>()
        doctorPlansByKey.values.forEach { doctor ->
            doctor.centerKeys.forEach { centerKey ->
                val centerId = localIds["center:$centerKey"]
                    ?: throw IllegalStateException("A doctor references an unknown center key.")
                if ((doctor.localId to centerId) !in existingLinks) links.add(doctor.localId to centerId)
            }
            doctor.entity.centerId?.let { centerId ->
                if ((doctor.localId to centerId) !in existingLinks) links.add(doctor.localId to centerId)
            }
        }

        val doctorSlotKeys = mutableSetOf<String>()
        val doctorSlots = mutableListOf<DoctorAvailabilityEntity>()
        doctorPlansByKey.values.forEach { doctor ->
            if (doctor.existing) {
                database.doctorAvailabilityDao().getForDoctor(doctor.localId).forEach { slot ->
                    doctorSlotKeys.add(doctorSlotSignature(doctor.localId, slot.dayOfWeek, slot.startTime, slot.endTime, slot.centerId))
                }
            }
            val source = findObject(parsed.doctors, doctor.key)
            forEachObject(source.optJSONArray("availability") ?: JSONArray()) { slot ->
                val centerKey = nullableString(slot, "centerKey")
                val centerId = centerKey?.let { localIds["center:$it"] }
                if (centerKey != null && centerId == null) throw IllegalStateException("A doctor availability slot references an unknown center key.")
                val day = slot.optInt("dayOfWeek")
                val start = slot.optString("startTime")
                val end = slot.optString("endTime")
                if (doctorSlotKeys.add(doctorSlotSignature(doctor.localId, day, start, end, centerId))) {
                    doctorSlots.add(DoctorAvailabilityEntity(
                        doctorId = doctor.localId, dayOfWeek = day, startTime = start, endTime = end,
                        notes = slot.optString("notes"), slotGroupId = nullableString(slot, "slotGroupId") ?: "",
                        centerId = centerId
                    ))
                }
            }
        }

        val centerSlotKeys = mutableSetOf<String>()
        val centerSlots = mutableListOf<CenterAvailabilityEntity>()
        centerPlans.values.forEach { center ->
            if (center.existing) {
                database.centerAvailabilityDao().getForCenter(center.localId).forEach { slot ->
                    centerSlotKeys.add(centerSlotSignature(center.localId, slot.dayOfWeek, slot.startTime, slot.endTime))
                }
            }
            val source = findObject(parsed.centers, center.key)
            forEachObject(source.optJSONArray("availability") ?: JSONArray()) { slot ->
                val day = slot.optInt("dayOfWeek")
                val start = slot.optString("startTime")
                val end = slot.optString("endTime")
                if (centerSlotKeys.add(centerSlotSignature(center.localId, day, start, end))) {
                    centerSlots.add(CenterAvailabilityEntity(
                        centerId = center.localId, dayOfWeek = day, startTime = start, endTime = end,
                        notes = slot.optString("notes"), slotGroupId = nullableString(slot, "slotGroupId") ?: ""
                    ))
                }
            }
        }

        return MergeResult(
            centerPlans.values.toList(), doctorPlansByKey.values.toList(), doctorSlots, centerSlots,
            links, localIds.toMap(), (parsed.specialties + doctorPlansByKey.values.map { it.specialtyName }).filter { it.isNotBlank() }.distinctBy(::normalize),
            newDoctors, updatedDoctors, differentPhones, newCenters, updatedCenters, parsed.includesAttachments, emptyList()
        )
    }

    private fun readManifest(context: Context, uri: Uri): ParsedPackage {
        var bytes: ByteArray? = null
        try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("Cannot open the selected share package.")
            ZipInputStream(BufferedInputStream(stream)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    validateEntryName(entry.name)
                    if (entry.isDirectory) { zip.closeEntry(); continue }
                    if (entry.name == "manifest.json") {
                        if (bytes != null) throw IllegalStateException("The package contains duplicate manifest entries.")
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var size = 0L
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            size += count
                            if (size > MAX_MANIFEST_BYTES) throw IllegalStateException("The manifest exceeds the 20 MB limit.")
                            output.write(buffer, 0, count)
                        }
                        bytes = output.toByteArray()
                    } else while (zip.read() != -1) Unit
                    zip.closeEntry()
                }
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException("The selected file is not a valid MRM share package.", error)
        }
        val manifestBytes = bytes ?: throw IllegalStateException("The package is missing manifest.json.")
        val manifest = try { JSONObject(String(manifestBytes, Charsets.UTF_8)) }
        catch (error: Exception) { throw IllegalStateException("The share package manifest is invalid.", error) }
        if (manifest.optString("format") != "mrm-share") throw IllegalStateException("This file is not an MRM share package.")
        val version = if (manifest.has("formatVersion")) manifest.optInt("formatVersion", -1) else -1
        if (version !in 1..1) throw IllegalStateException("The share package version is not supported.")
        val centers = manifest.optJSONArray("centers") ?: throw IllegalStateException("The package is missing its centers list.")
        val doctors = manifest.optJSONArray("doctors") ?: throw IllegalStateException("The package is missing its doctors list.")
        val specialtyArray = manifest.optJSONArray("specialties") ?: JSONArray()
        val specialties = (0 until specialtyArray.length()).map { specialtyArray.optString(it) }.filter { it.isNotBlank() }
        return ParsedPackage(manifest.optBoolean("includesAttachments", false), specialties, centers, doctors)
    }

    private fun validateEntryName(name: String) {
        if (name.isBlank() || name.contains("..") || name.startsWith("/") || name.contains('\\')) {
            throw IllegalStateException("The package contains an unsafe ZIP entry name.")
        }
    }
    private fun normalize(value: String): String = value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    private fun fill(existing: String, incoming: String): String = if (existing.isBlank()) incoming.trim() else existing
    private fun mergeNotes(existing: String, incoming: String, date: String): String {
        val value = incoming.trim()
        if (value.isBlank()) return existing
        if (existing.isBlank()) return value
        if (existing.contains(value, ignoreCase = true)) return existing
        return "$existing\n— imported $date:\n$value"
    }
    private fun appendPhones(existing: String, incoming: String): String {
        val result = ContactPhoneRules.split(existing).toMutableList()
        val known = result.mapNotNull(ContactPhoneRules::normalize).toMutableSet()
        ContactPhoneRules.split(incoming).forEach { phone ->
            val normalized = ContactPhoneRules.normalize(phone)
            if (normalized != null && known.add(normalized)) result.add(phone)
        }
        return result.joinToString(", ")
    }
    private fun requiredString(json: JSONObject, key: String): String {
        val value = json.optString(key).trim()
        if (value.isBlank() || value == "null") throw IllegalStateException("A package item is missing $key.")
        return value
    }
    private fun nullableString(json: JSONObject, key: String): String? =
        if (!json.has(key) || json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() }
    private fun nullableDouble(json: JSONObject, key: String): Double? =
        if (!json.has(key) || json.isNull(key)) null else json.optDouble(key).takeIf { it.isFinite() }
    private fun stringArray(array: JSONArray?): List<String> =
        if (array == null) emptyList() else (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    private fun findObject(array: JSONArray, key: String): JSONObject {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: continue
            if (item.optString("key") == key) return item
        }
        throw IllegalStateException("A planned package item could not be found.")
    }
    private fun doctorSlotSignature(doctorId: Long, day: Int, start: String, end: String, centerId: Long?) =
        "$doctorId|$day|$start|$end|" + (centerId ?: 0L)
    private fun centerSlotSignature(centerId: Long, day: Int, start: String, end: String) =
        "$centerId|$day|$start|$end"
    private inline fun forEachObject(array: JSONArray, action: (JSONObject) -> Unit) {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index) ?: throw IllegalStateException("The package contains an invalid list item.")
            action(item)
        }
    }
}
