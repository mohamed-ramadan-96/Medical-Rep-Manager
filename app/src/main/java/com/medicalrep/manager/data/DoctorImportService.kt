package com.medicalrep.manager.data

import androidx.room.withTransaction
import com.medicalrep.manager.domain.ContactPhoneRules

data class DoctorImportResult(
    val added: Int,
    val skippedDuplicates: Int
)

/**
 * Coordinates duplicate detection and database writes for doctor imports.
 * The whole import is atomic: a failed insert cannot leave a partially imported workbook.
 */
class DoctorImportService(
    private val database: MedicalRepDatabase
) {
    suspend fun import(rows: List<ImportedDoctorRow>): DoctorImportResult =
        database.withTransaction {
            val dao = database.doctorDao()
            val specialtyDao = database.specialtyDao()
            val specialtiesByName = specialtyDao.getAll()
                .associateBy { it.name.trim().lowercase() }
                .toMutableMap()
            val knownPhones = dao.getAllForImport()
                .flatMap { ContactPhoneRules.split(it.phoneNumbers) }
                .mapNotNull(ContactPhoneRules::normalize)
                .toMutableSet()

            var added = 0
            var skipped = 0
            rows.forEach { row ->
                val rowPhones = ContactPhoneRules.split(row.phone)
                    .mapNotNull(ContactPhoneRules::normalize)
                    .toSet()
                if (rowPhones.isNotEmpty() && rowPhones.any(knownPhones::contains)) {
                    skipped++
                } else {
                    val specialtyName = row.specialty.trim()
                    val specialtyId = if (specialtyName.isBlank()) {
                        null
                    } else {
                        val key = specialtyName.lowercase()
                        specialtiesByName[key]?.id ?: specialtyDao.insert(
                            SpecialtyEntity(name = specialtyName)
                        ).also { insertedId ->
                            specialtiesByName[key] = SpecialtyEntity(id = insertedId, name = specialtyName)
                        }
                    }
                    dao.insert(
                        DoctorEntity(
                            name = row.name,
                            phoneNumbers = row.phone,
                            specialty = specialtyName,
                            specialtyId = specialtyId,
                            area = row.area
                        )
                    )
                    knownPhones.addAll(rowPhones)
                    added++
                }
            }
            DoctorImportResult(added = added, skippedDuplicates = skipped)
        }
}
