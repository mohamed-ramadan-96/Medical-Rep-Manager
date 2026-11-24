package com.medicalrep.manager.data

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import com.medicalrep.manager.domain.WeeklyPlanRules
import com.medicalrep.manager.domain.ContactPhoneRules
import java.time.LocalDate
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MedicalRepDatabaseTest {
    private lateinit var database: MedicalRepDatabase

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            MedicalRepDatabase::class.java
        ).allowMainThreadQueries().build()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun excelImporterReadsNameAndPhoneColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = createWorkbook(
            context.cacheDir,
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                <row r="1"><c r="A1" t="inlineStr"><is><t>Doctor Name</t></is></c><c r="B1" t="inlineStr"><is><t>Phone Number</t></is></c></row>
                <row r="2"><c r="A2" t="inlineStr"><is><t>Dr Test</t></is></c><c r="B2" t="inlineStr"><is><t>01012345678</t></is></c></row>
            </sheetData></worksheet>"""
        )
        try {
            val rows = ExcelImporter.readDoctors(context.contentResolver, Uri.fromFile(file))
            assertEquals(listOf(ImportedDoctorRow("Dr Test", "01012345678")), rows)
        } finally {
            file.delete()
        }
    }

    @Test
    fun excelImporterRejectsMoreThanTwoColumns() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = createWorkbook(
            context.cacheDir,
            """<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                <row r="1"><c r="A1" t="inlineStr"><is><t>Doctor Name</t></is></c><c r="B1" t="inlineStr"><is><t>Phone Number</t></is></c><c r="C1" t="inlineStr"><is><t>Notes</t></is></c></row>
            </sheetData></worksheet>"""
        )
        try {
            try {
                ExcelImporter.readDoctors(context.contentResolver, Uri.fromFile(file))
                throw AssertionError("Expected importer to reject a third column")
            } catch (expected: IllegalStateException) {
                assertTrue(expected.message.orEmpty().contains("more than two columns"))
            }
        } finally {
            file.delete()
        }
    }

    private fun createWorkbook(directory: File, sheetXml: String): File {
        val file = File.createTempFile("doctor-import-test-", ".xlsx", directory)
        ZipOutputStream(file.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zip.write(sheetXml.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
        return file
    }

    @Test
    fun contactPhoneRulesNormalizeArabicAndPersianDigits() {
        assertEquals("01012345678", ContactPhoneRules.normalize("٠١٠١٢٣٤٥٦٧٨"))
        assertEquals("0123456789", ContactPhoneRules.normalize("۰۱۲۳۴۵۶۷۸۹"))
        assertEquals(listOf("0100", "0111", "0122"), ContactPhoneRules.split("0100 / 0111, 0122"))
    }

    @Test
    fun weeklyPlanWeekAlwaysStartsOnSaturday() {
        assertEquals(LocalDate.parse("2026-11-21"), WeeklyPlanRules.weekStart(LocalDate.parse("2026-11-21")))
        assertEquals(LocalDate.parse("2026-11-21"), WeeklyPlanRules.weekStart(LocalDate.parse("2026-11-22")))
        assertEquals(LocalDate.parse("2026-11-21"), WeeklyPlanRules.weekStart(LocalDate.parse("2026-11-27")))
        assertEquals(LocalDate.parse("2026-11-28"), WeeklyPlanRules.weekStart(LocalDate.parse("2026-11-28")))
    }

    @Test
    fun weeklyPlanAreaMatchingIsCaseInsensitiveAndDeduplicated() {
        assertEquals(listOf("Cairo", "Giza"), WeeklyPlanRules.splitAreas(" Cairo | Giza | cairo || "))
        assertTrue(WeeklyPlanRules.matchesArea("Cairo|Giza", "gIzA"))
        assertTrue(WeeklyPlanRules.matchesArea("Cairo|Giza", "__all__"))
    }

    @Test
    fun weeklyPlanRejectsDuplicateDoctorWithinSameWeek() = runBlocking {
        val doctorId = database.doctorDao().insert(DoctorEntity(name = "Test Doctor"))
        val first = WeeklyPlanEntryEntity(
            weekStartDate = "2026-11-21",
            visitDate = "2026-11-22",
            doctorId = doctorId,
            centerId = null,
            regionSnapshot = "Cairo",
            centerNameSnapshot = "",
            doctorNameSnapshot = "Test Doctor"
        )
        val duplicate = first.copy(visitDate = "2026-11-23")

        assertTrue(database.weeklyPlanDao().insert(first) > 0)
        assertEquals(-1L, database.weeklyPlanDao().insert(duplicate))
    }

    @Test
    fun deletingCenterPreservesWeeklyPlanAndClearsCenterReference() = runBlocking {
        val doctorId = database.doctorDao().insert(DoctorEntity(name = "Center Doctor"))
        val centerId = database.medicalCenterDao().insert(MedicalCenterEntity(name = "Temporary Center"))
        database.weeklyPlanDao().insert(
            WeeklyPlanEntryEntity(
                weekStartDate = "2026-11-21",
                visitDate = "2026-11-24",
                doctorId = doctorId,
                centerId = centerId,
                regionSnapshot = "Cairo",
                centerNameSnapshot = "Temporary Center",
                doctorNameSnapshot = "Center Doctor"
            )
        )

        database.medicalCenterDao().delete(
            MedicalCenterEntity(id = centerId, name = "Temporary Center")
        )

        val entry = database.weeklyPlanDao().observeAll().first().single()
        assertEquals(doctorId, entry.doctorId)
        assertEquals(null, entry.centerId)
        assertEquals("Temporary Center", entry.centerNameSnapshot)
    }

    @Test
    fun deletingDoctorCascadesToVisitsAndWeeklyPlan() = runBlocking {
        val doctorId = database.doctorDao().insert(DoctorEntity(name = "Cascade Doctor"))
        database.doctorVisitDao().insert(
            DoctorVisitEntity(
                doctorId = doctorId,
                visitedAt = 1_800_000_000_000L
            )
        )
        database.weeklyPlanDao().insert(
            WeeklyPlanEntryEntity(
                weekStartDate = "2026-11-21",
                visitDate = "2026-11-22",
                doctorId = doctorId,
                centerId = null,
                regionSnapshot = "",
                centerNameSnapshot = "",
                doctorNameSnapshot = "Cascade Doctor"
            )
        )

        val doctor = database.doctorDao().getAllForImport().single()
        database.doctorDao().delete(doctor)

        assertTrue(database.doctorVisitDao().observeAll().first().isEmpty())
        assertTrue(database.weeklyPlanDao().observeAll().toListBlocking().isEmpty())
    }
}
