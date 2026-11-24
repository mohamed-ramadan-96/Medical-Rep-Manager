package com.medicalrep.manager.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Pure business rules for weekly visit planning.
 *
 * The business week starts on Saturday, independent of device locale.
 */
object WeeklyPlanRules {
    fun weekStart(date: LocalDate): LocalDate {
        val daysSinceSaturday = when (date.dayOfWeek) {
            DayOfWeek.SATURDAY -> 0
            DayOfWeek.SUNDAY -> 1
            DayOfWeek.MONDAY -> 2
            DayOfWeek.TUESDAY -> 3
            DayOfWeek.WEDNESDAY -> 4
            DayOfWeek.THURSDAY -> 5
            DayOfWeek.FRIDAY -> 6
        }
        return date.minusDays(daysSinceSaturday.toLong())
    }

    fun splitAreas(value: String): List<String> =
        value.split("|")
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy { it.lowercase(Locale.ROOT) }

    fun matchesArea(doctorAreas: String, selectedArea: String): Boolean =
        selectedArea == ALL_AREAS ||
            splitAreas(doctorAreas).any { it.equals(selectedArea, ignoreCase = true) }

    private const val ALL_AREAS = "__all__"
}
