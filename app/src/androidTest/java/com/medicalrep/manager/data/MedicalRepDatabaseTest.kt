package com.medicalrep.manager.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
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

        assertTrue(database.doctorVisitDao().observeAll().toListBlocking().isEmpty())
        assertTrue(database.weeklyPlanDao().observeAll().toListBlocking().isEmpty())
    }
}
