package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import com.medicalrep.manager.domain.ContactPhoneRules
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

data class SharePlan(
    val newDoctors: Int,
    val updatedDoctors: Int,
    val sameNameDifferentPhone: Int,
    val newCenters: Int,
    val updatedCenters: Int,
    val doctorSlotsToAdd: Int,
    val centerSlotsToAdd: Int,
    val attachmentsToCopy: Int,
    val includesAttachments: Boolean,
    val warnings: List<String>
)

data class ShareApplyResult(
    val newDoctors: Int,
    val updatedDoctors: Int,
    val sameNameDifferentPhone: Int,
    val newCenters: Int,
    val updatedCenters: Int,
    val doctorSlotsToAdd: Int,
    val centerSlotsToAdd: Int,
    val attachmentsToCopy: Int,
    val includesAttachments: Boolean,
    val warnings: List<String>
)

object SharePackageImporter {
    private const val MAX_MANIFEST_BYTES = 20L * 1024L * 1024L

    private data class ParsedPackage(
        val includesAttachments: Boolean,
        val specialties: List<String>,
        val centers: JSONArray,
        val doctors: JSONArray
    )

    suspend fun plan(context: Context, database: MedicalRepDatabase, uri: Uri): SharePlan =
        matchAndPlan(database, readManifest(context, uri))

    suspend fun apply(context: Context, database: MedicalRepDatabase, uri: Uri): ShareApplyResult {
        throw NotImplementedError("Share package apply is implemented in the next stage.")
    }

    private suspend fun matchAndPlan(database: MedicalRepDatabase, parsed: ParsedPackage): SharePlan {
        val centers = database.medicalCenterDao().observeAll().first().toMutableList()
        val doctors = database.doctorDao().getAllForImport().toMutableList()
        var newDoctors = 0
        var updatedDoctors = 0
        var differentPhones = 0
        var newCenters = 0
        var updatedCenters = 0
        val centerMatches = linkedMapOf<String, MedicalCenterEntity?>()
        val doctorMatches = linkedMapOf<String, DoctorEntity?>()

        forEachObject(parsed.centers) { source ->
            val key = requiredString(source, "key")
            val name = normalize(requiredString(source, "name"))
            val area = normalize(source.optString("area"))
            val match = centers.firstOrNull {
                normalize(it.name) == name && normalize(it.area) == area
            }
            centerMatches[key] = match
            if (match == null) {
                newCenters++
                centers.add(MedicalCenterEntity(
                    id = -newCenters.toLong(),
                    name = source.optString("name"),
                    area = source.optString("area")
                ))
            } else {
                val merged = match.copy(
                    address = fill(match.address, source.optString("address")),
                    phone = fill(match.phone, source.optString("phone")),
                    notes = fill(match.notes, source.optString("notes")),
                    latitude = match.latitude ?: nullableDouble(source, "latitude"),
                    longitude = match.longitude ?: nullableDouble(source, "longitude"),
                    mapUrl = if (match.mapUrl.isNullOrBlank()) nullableString(source, "mapUrl") else match.mapUrl
                )
                if (merged != match) updatedCenters++
                val index = centers.indexOfFirst { it.id == match.id }
                if (index >= 0) centers[index] = merged
            }
        }

        forEachObject(parsed.doctors) { source ->
            val phones = ContactPhoneRules.split(source.optString("phoneNumbers"))
                .mapNotNull(ContactPhoneRules::normalize).toSet()
            val name = normalize(requiredString(source, "name"))
            val area = normalize(source.optString("area"))
            val match = if (phones.isNotEmpty()) {
                doctors.firstOrNull { existing ->
                    ContactPhoneRules.split(existing.phoneNumbers)
                        .mapNotNull(ContactPhoneRules::normalize).any(phones::contains)
                }
            } else {
                doctors.firstOrNull { existing ->
                    normalize(existing.name) == name && area.isNotBlank() &&
                        normalize(existing.area).isNotBlank() && normalize(existing.area) == area
                }
            }
            val hasDifferentPhone = phones.isNotEmpty() && doctors.any { existing ->
                normalize(existing.name) == name &&
                    ContactPhoneRules.split(existing.phoneNumbers)
                        .mapNotNull(ContactPhoneRules::normalize).none(phones::contains)
            }
            val key = requiredString(source, "key")
            doctorMatches[key] = match
            if (match == null) {
                newDoctors++
                if (hasDifferentPhone) differentPhones++
                doctors.add(DoctorEntity(
                    id = -newDoctors.toLong(),
                    name = source.optString("name"),
                    specialty = source.optString("specialty"),
                    phoneNumbers = source.optString("phoneNumbers"),
                    notes = source.optString("notes"),
                    area = source.optString("area")
                ))
            } else {
                val merged = match.copy(
                    specialty = fill(match.specialty, source.optString("specialty")),
                    area = fill(match.area, source.optString("area")),
                    latitude = match.latitude ?: nullableDouble(source, "latitude"),
                    longitude = match.longitude ?: nullableDouble(source, "longitude"),
                    mapUrl = if (match.mapUrl.isNullOrBlank()) nullableString(source, "mapUrl") else match.mapUrl,
                    phoneNumbers = appendPhones(match.phoneNumbers, source.optString("phoneNumbers")),
                    notes = if (match.notes.isBlank()) source.optString("notes") else match.notes
                )
                if (merged != match) updatedDoctors++
                val index = doctors.indexOfFirst { it.id == match.id }
                if (index >= 0) doctors[index] = merged
            }
        }

        var doctorSlots = 0
        val doctorSlotKeys = mutableSetOf<String>()
        forEachObject(parsed.doctors) { doctor ->
            val key = requiredString(doctor, "key")
            val matched = doctorMatches[key]
            val owner = matched?.id ?: -(doctorMatches.keys.indexOf(key) + 1L)
            if (matched != null) {
                database.doctorAvailabilityDao().getForDoctor(matched.id).forEach { slot ->
                    doctorSlotKeys.add(owner.toString() + "|" + slot.dayOfWeek + "|" + slot.startTime + "|" + slot.endTime + "|" + (slot.centerId ?: 0L))
                }
            }
            forEachObject(doctor.optJSONArray("availability") ?: JSONArray()) { slot ->
                val centerKey = nullableString(slot, "centerKey")
                val centerId = if (centerKey == null) 0L else
                    (centerMatches[centerKey]?.id ?: -(centerMatches.keys.indexOf(centerKey) + 1L))
                val signature = owner.toString() + "|" + slot.optInt("dayOfWeek") + "|" +
                    slot.optString("startTime") + "|" + slot.optString("endTime") + "|" + centerId
                if (doctorSlotKeys.add(signature)) doctorSlots++
            }
        }

        var centerSlots = 0
        val centerSlotKeys = mutableSetOf<String>()
        forEachObject(parsed.centers) { center ->
            val key = requiredString(center, "key")
            val matched = centerMatches[key]
            val owner = matched?.id ?: -(centerMatches.keys.indexOf(key) + 1L)
            if (matched != null) {
                database.centerAvailabilityDao().getForCenter(matched.id).forEach { slot ->
                    centerSlotKeys.add(owner.toString() + "|" + slot.dayOfWeek + "|" + slot.startTime + "|" + slot.endTime)
                }
            }
            forEachObject(center.optJSONArray("availability") ?: JSONArray()) { slot ->
                val signature = owner.toString() + "|" + slot.optInt("dayOfWeek") + "|" +
                    slot.optString("startTime") + "|" + slot.optString("endTime")
                if (centerSlotKeys.add(signature)) centerSlots++
            }
        }

        var attachmentCount = 0
        if (parsed.includesAttachments) {
            forEachObject(parsed.doctors) { doctor ->
                attachmentCount += doctor.optJSONArray("attachments")?.length() ?: 0
                if (!doctor.isNull("photoEntry")) attachmentCount++
            }
            forEachObject(parsed.centers) { center ->
                attachmentCount += center.optJSONArray("attachments")?.length() ?: 0
            }
        }

        return SharePlan(
            newDoctors = newDoctors,
            updatedDoctors = updatedDoctors,
            sameNameDifferentPhone = differentPhones,
            newCenters = newCenters,
            updatedCenters = updatedCenters,
            doctorSlotsToAdd = doctorSlots,
            centerSlotsToAdd = centerSlots,
            attachmentsToCopy = attachmentCount,
            includesAttachments = parsed.includesAttachments,
            warnings = emptyList()
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
                    if (entry.isDirectory) {
                        zip.closeEntry()
                        continue
                    }
                    if (entry.name == "manifest.json") {
                        if (bytes != null) throw IllegalStateException("The package contains duplicate manifest entries.")
                        val output = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        var size = 0L
                        while (true) {
                            val count = zip.read(buffer)
                            if (count < 0) break
                            size += count
                            if (size > MAX_MANIFEST_BYTES) {
                                throw IllegalStateException("The manifest exceeds the 20 MB limit.")
                            }
                            output.write(buffer, 0, count)
                        }
                        bytes = output.toByteArray()
                    } else {
                        while (zip.read() != -1) Unit
                    }
                    zip.closeEntry()
                }
            }
        } catch (error: IllegalStateException) {
            throw error
        } catch (error: Exception) {
            throw IllegalStateException("The selected file is not a valid MRM share package.", error)
        }

        val manifestBytes = bytes ?: throw IllegalStateException("The package is missing manifest.json.")
        val manifest = try {
            JSONObject(String(manifestBytes, Charsets.UTF_8))
        } catch (error: Exception) {
            throw IllegalStateException("The share package manifest is invalid.", error)
        }
        if (manifest.optString("format") != "mrm-share") {
            throw IllegalStateException("This file is not an MRM share package.")
        }
        val version = if (manifest.has("formatVersion")) manifest.optInt("formatVersion", -1) else -1
        if (version !in 1..1) throw IllegalStateException("The share package version is not supported.")
        val centers = manifest.optJSONArray("centers")
            ?: throw IllegalStateException("The package is missing its centers list.")
        val doctors = manifest.optJSONArray("doctors")
            ?: throw IllegalStateException("The package is missing its doctors list.")
        val specialtyArray = manifest.optJSONArray("specialties") ?: JSONArray()
        val specialties = (0 until specialtyArray.length())
            .map { specialtyArray.optString(it) }.filter { it.isNotBlank() }
        return ParsedPackage(
            manifest.optBoolean("includesAttachments", false),
            specialties,
            centers,
            doctors
        )
    }

    private fun validateEntryName(name: String) {
        if (name.isBlank() || name.contains("..") || name.startsWith("/") || name.contains('\\')) {
            throw IllegalStateException("The package contains an unsafe ZIP entry name.")
        }
    }

    private fun normalize(value: String): String =
        value.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun fill(existing: String, incoming: String): String =
        if (existing.isBlank()) incoming.trim() else existing

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
        if (value.isBlank() || value == "null") {
            throw IllegalStateException("A package item is missing " + key + ".")
        }
        return value
    }

    private fun nullableString(json: JSONObject, key: String): String? =
        if (!json.has(key) || json.isNull(key)) null else json.optString(key).takeIf { it.isNotBlank() }

    private fun nullableDouble(json: JSONObject, key: String): Double? =
        if (!json.has(key) || json.isNull(key)) null else json.optDouble(key).takeIf { it.isFinite() }

    private inline fun forEachObject(array: JSONArray, action: (JSONObject) -> Unit) {
        for (index in 0 until array.length()) {
            val item = array.optJSONObject(index)
                ?: throw IllegalStateException("The package contains an invalid list item.")
            action(item)
        }
    }
}
