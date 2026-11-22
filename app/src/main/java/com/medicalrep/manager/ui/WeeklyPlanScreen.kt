package com.medicalrep.manager.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.data.WeeklyPlanEntryEntity
import com.medicalrep.manager.data.WeeklyPlanExcelExporter
import com.medicalrep.manager.data.WeeklyPlanExportRow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val planDays = listOf(
    6 to ("Saturday" to "السبت"),
    7 to ("Sunday" to "الأحد"),
    1 to ("Monday" to "الاثنين"),
    2 to ("Tuesday" to "الثلاثاء"),
    3 to ("Wednesday" to "الأربعاء"),
    4 to ("Thursday" to "الخميس"),
    5 to ("Friday" to "الجمعة")
)

private fun planWeekStart(date: LocalDate): LocalDate {
    val offset = if (date.dayOfWeek.value >= 6) date.dayOfWeek.value - 6 else date.dayOfWeek.value + 1
    return date.minusDays(offset.toLong())
}

private fun dayLabel(date: LocalDate, isArabic: Boolean): String {
    val day = planDays.firstOrNull { it.first == date.dayOfWeek.value }?.second
    return if (isArabic) day?.second.orEmpty() else day?.first.orEmpty()
}

@Composable
fun WeeklyPlanScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val doctors by database.doctorDao().observeAll().collectAsState(initial = emptyList())
    val availability by database.doctorAvailabilityDao().observeAll().collectAsState(initial = emptyList())
    val entries by database.weeklyPlanDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val regions = remember(doctors) {
        doctors.map { it.area.trim() }.filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
    }
    var selectedRegion by remember(regions) { mutableStateOf(regions.firstOrNull().orEmpty()) }
    var regionMenu by remember { mutableStateOf(false) }
    var exportRows by remember { mutableStateOf<List<WeeklyPlanExportRow>>(emptyList()) }
    var exportFileName by remember { mutableStateOf("weekly-plan.xlsx") }
    var showExportError by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    )) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                WeeklyPlanExcelExporter.export(context, uri, exportRows, isArabic)
            }.onFailure {
                showExportError = if (isArabic) "تعذر تصدير ملف Excel: ${it.localizedMessage.orEmpty()}" else "Excel export failed: ${it.localizedMessage.orEmpty()}"
            }
        }
    }

    val weekStart = planWeekStart(selectedDate)
    val weekStartText = weekStart.toString()
    val monthEntries = entries.filter {
        runCatching { YearMonth.from(LocalDate.parse(it.visitDate)) == month }.getOrDefault(false)
    }
    val weekEntries = entries.filter { it.weekStartDate == weekStartText }
    val currentDayEntries = entries.filter { it.visitDate == selectedDate.toString() }
        .sortedWith(compareBy({ it.centerNameSnapshot }, { it.doctorNameSnapshot }))
    val assignedDoctorIdsThisWeek = weekEntries.map { it.doctorId }.toSet()

    val selectedDayCode = selectedDate.dayOfWeek.value
    val eligibleDoctorIds = availability.asSequence()
        .filter { it.dayOfWeek == selectedDayCode }
        .map { it.doctorId }
        .toSet()
    val eligibleDoctors = doctors.filter { doctor ->
        doctor.id in eligibleDoctorIds &&
            doctor.area.trim().equals(selectedRegion, ignoreCase = true) &&
            doctor.id !in assignedDoctorIdsThisWeek
    }.sortedBy { it.name.lowercase(Locale.ROOT) }

    var selectedDoctorIds by remember(selectedDate, selectedRegion, eligibleDoctors) {
        mutableStateOf(emptySet<Long>())
    }
    var doctorMenu by remember { mutableStateOf(false) }

    Scaffold(modifier = Modifier.padding(padding)) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    if (isArabic) "خطة الزيارات الأسبوعية" else "Weekly visit plan",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    if (isArabic) "اختر التاريخ والمنطقة، ثم اختر الأطباء المتاحين في هذا اليوم." else "Choose a date and area, then select doctors available on that day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { month = month.minusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = if (isArabic) "الشهر السابق" else "Previous month")
                            }
                            Text(
                                month.format(DateTimeFormatter.ofPattern(if (isArabic) "MMMM yyyy" else "MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = { month = month.plusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(Icons.Default.ChevronRight, contentDescription = if (isArabic) "الشهر التالي" else "Next month")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            planDays.forEach { (_, names) ->
                                Text(
                                    if (isArabic) names.second.take(1) else names.first.take(1),
                                    modifier = Modifier.weight(1f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        val firstDayOffset = (month.atDay(1).dayOfWeek.value + 1) % 7
                        val cells = List(firstDayOffset) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(7),
                            modifier = Modifier.fillMaxWidth().height((((cells.size + 6) / 7) * 44).dp),
                            userScrollEnabled = false,
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            gridItems(cells) { date ->
                                if (date == null) {
                                    Box(Modifier.height(40.dp))
                                } else {
                                    val count = entries.count { it.visitDate == date.toString() }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (date == selectedDate) MaterialTheme.colorScheme.primary
                                                else if (count > 0) MaterialTheme.colorScheme.secondaryContainer
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable {
                                                selectedDate = date
                                                selectedRegion = selectedRegion.ifBlank { regions.firstOrNull().orEmpty() }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                            Text(
                                                date.dayOfMonth.toString(),
                                                color = if (date == selectedDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            if (count > 0) Text(
                                                count.toString(),
                                                color = if (date == selectedDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Text(
                    (if (isArabic) "اليوم المحدد: " else "Selected date: ") +
                        selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    (if (isArabic) "أسبوع الخطة: " else "Plan week: ") + weekStart + " – " + weekStart.plusDays(6),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item {
                Box {
                    OutlinedSelectionField(
                        label = if (isArabic) "المنطقة الجغرافية" else "Geographic area",
                        value = selectedRegion.ifBlank { if (isArabic) "اختر المنطقة" else "Choose area" },
                        onClick = { regionMenu = true },
                        enabled = regions.isNotEmpty()
                    )
                    DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                        regions.forEach { region ->
                            DropdownMenuItem(text = { Text(region) }, onClick = {
                                selectedRegion = region
                                selectedDoctorIds = emptySet()
                                regionMenu = false
                            })
                        }
                    }
                }
            }
            item {
                Box {
                    OutlinedSelectionField(
                        label = if (isArabic) "الأطباء المتاحون" else "Available doctors",
                        value = when {
                            selectedDoctorIds.isEmpty() -> if (isArabic) "اضغط لاختيار الأطباء" else "Tap to choose doctors"
                            else -> eligibleDoctors.filter { it.id in selectedDoctorIds }.joinToString("، ") { it.name }
                        },
                        onClick = { doctorMenu = true },
                        enabled = selectedRegion.isNotBlank() && eligibleDoctors.isNotEmpty()
                    )
                    DropdownMenu(expanded = doctorMenu, onDismissRequest = { doctorMenu = false }) {
                        eligibleDoctors.forEach { doctor ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        androidx.compose.material3.Checkbox(
                                            checked = doctor.id in selectedDoctorIds,
                                            onCheckedChange = null
                                        )
                                        Column {
                                            Text(doctor.name)
                                            if (doctor.specialty.isNotBlank()) {
                                                Text(doctor.specialty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedDoctorIds = if (doctor.id in selectedDoctorIds) selectedDoctorIds - doctor.id else selectedDoctorIds + doctor.id
                                }
                            )
                        }
                    }
                }
            }
            if (selectedRegion.isNotBlank() && eligibleDoctors.isEmpty()) {
                item {
                    Text(
                        if (isArabic) "لا يوجد أطباء متاحون في هذا اليوم داخل المنطقة المختارة، أو تمت إضافتهم بالفعل للخطة هذا الأسبوع." else "No doctors are available on this day in the selected area, or they have already been added to this week's plan.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (selectedDoctorIds.isNotEmpty()) {
                item {
                    Button(
                        onClick = {
                            val chosen = eligibleDoctors.filter { it.id in selectedDoctorIds }
                            scope.launch {
                                chosen.forEach { doctor ->
                                    database.weeklyPlanDao().insert(
                                        WeeklyPlanEntryEntity(
                                            weekStartDate = weekStartText,
                                            visitDate = selectedDate.toString(),
                                            doctorId = doctor.id,
                                            centerId = null,
                                            regionSnapshot = selectedRegion,
                                            centerNameSnapshot = "",
                                            doctorNameSnapshot = doctor.name
                                        )
                                    )
                                }
                            }
                            selectedDoctorIds = emptySet()
                            doctorMenu = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isArabic) "إضافة \${selectedDoctorIds.size} طبيب للخطة" else "Add \${selectedDoctorIds.size} doctor(s) to plan")
                    }
                }
            }
            item {
                Text(if (isArabic) "الأطباء المضافون لهذا اليوم" else "Doctors planned for this date", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            if (currentDayEntries.isEmpty()) {
                item { Text(if (isArabic) "لم تتم إضافة أطباء لهذا اليوم بعد." else "No doctors have been added to this date yet.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                lazyItems(currentDayEntries, key = { it.id }) { entry ->
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(entry.doctorNameSnapshot, fontWeight = FontWeight.Bold)
                                if (entry.centerNameSnapshot.isNotBlank()) Text(entry.centerNameSnapshot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                Text(entry.regionSnapshot, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { scope.launch { database.weeklyPlanDao().delete(entry) } }) {
                                Icon(Icons.Default.Delete, contentDescription = if (isArabic) "إزالة الطبيب من الخطة" else "Remove doctor from plan")
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isArabic) "تصدير Excel" else "Export to Excel", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Button(
                            onClick = {
                                exportRows = buildExportRows(monthEntries, isArabic)
                                exportFileName = "weekly-plan-${month}.xlsx"
                                exportLauncher.launch(exportFileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = monthEntries.isNotEmpty()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Text(if (isArabic) "تصدير الشهر بالكامل" else "Export full month")
                        }
                        Button(
                            onClick = {
                                exportRows = buildExportRows(weekEntries, isArabic)
                                exportFileName = "weekly-plan-week-${weekStart}.xlsx"
                                exportLauncher.launch(exportFileName)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = weekEntries.isNotEmpty()
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null)
                            Text(if (isArabic) "تصدير الأسبوع المحدد" else "Export selected week")
                        }
                    }
                }
            }
        }
    }

    showExportError?.let { message ->
        AlertDialog(
            onDismissRequest = { showExportError = null },
            title = { Text(if (isArabic) "خطأ في التصدير" else "Export error") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { showExportError = null }) { Text(if (isArabic) "حسنًا" else "OK") } }
        )
    }
}

private fun buildExportRows(
    source: List<WeeklyPlanEntryEntity>,
    isArabic: Boolean
): List<WeeklyPlanExportRow> {
    return source.sortedWith(compareBy({ it.visitDate }, { it.centerNameSnapshot }, { it.doctorNameSnapshot })).map { entry ->
        val date = LocalDate.parse(entry.visitDate)
        WeeklyPlanExportRow(
            date = entry.visitDate,
            day = dayLabel(date, isArabic),
            region = entry.regionSnapshot,
            center = entry.centerNameSnapshot,
            doctor = entry.doctorNameSnapshot
        )
    }
}
