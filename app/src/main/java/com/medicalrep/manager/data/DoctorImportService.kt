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
                    dao.insert(DoctorEntity(name = row.name, phoneNumbers = row.phone))
                    knownPhones.addAll(rowPhones)
                    added++
                }
            }
            DoctorImportResult(added = added, skippedDuplicates = skipped)
        }
}
