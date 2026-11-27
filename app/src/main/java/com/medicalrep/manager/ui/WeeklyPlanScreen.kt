package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppDialogContainerColor

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState

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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.domain.WeeklyPlanRules
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.MedicalCenterEntity
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

private const val ALL_REGIONS = "__all__"

private fun weeklyDoctorAreas(value: String): List<String> = WeeklyPlanRules.splitAreas(value)

private fun dayLabel(date: LocalDate, isArabic: Boolean): String {
    val day = planDays.firstOrNull { it.first == date.dayOfWeek.value }?.second
    return if (isArabic) day?.second.orEmpty() else day?.first.orEmpty()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyPlanScreen(
    isArabic: Boolean,
    padding: androidx.compose.foundation.layout.PaddingValues,
    onRecordVisit: (doctorId: Long, visitDate: String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val doctors by database.doctorDao().observeAll().collectAsState(initial = emptyList())
    val availability by database.doctorAvailabilityDao().observeAll().collectAsState(initial = emptyList())
    val centers by database.medicalCenterDao().observeAll().collectAsState(initial = emptyList())
    val centerLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val visits by database.doctorVisitDao().observeAll().collectAsState(initial = emptyList())
    val entries by database.weeklyPlanDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var month by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    val regions = remember(doctors) {
        doctors.flatMap { weeklyDoctorAreas(it.area) }.filter { it.isNotBlank() }
            .distinctBy { it.lowercase(Locale.ROOT) }.sortedBy { it.lowercase(Locale.ROOT) }
    }
    var selectedRegion by remember { mutableStateOf("") }
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

    val weekStart = WeeklyPlanRules.weekStart(selectedDate)
    val weekStartText = weekStart.toString()
    val monthEntries = remember(entries, month) {
        entries.filter {
            runCatching { YearMonth.from(LocalDate.parse(it.visitDate)) == month }.getOrDefault(false)
        }
    }
    val weekEntries = remember(entries, weekStartText) {
        entries.filter { it.weekStartDate == weekStartText }
    }
    val currentDayEntries = remember(entries, selectedDate) {
        entries.filter { it.visitDate == selectedDate.toString() }
            .sortedWith(compareBy({ it.centerNameSnapshot }, { it.doctorNameSnapshot }))
    }
    val assignedDoctorIdsThisWeek = remember(weekEntries) { weekEntries.map { it.doctorId }.toSet() }

    val selectedDayCode = selectedDate.dayOfWeek.value
    val eligibleDoctorIds = remember(availability, selectedDayCode) {
        availability.asSequence()
            .filter { it.dayOfWeek == selectedDayCode }
            .map { it.doctorId }
            .toSet()
    }
    val eligibleDoctors = remember(doctors, eligibleDoctorIds, selectedRegion, assignedDoctorIdsThisWeek) {
        doctors.filter { doctor ->
            doctor.id in eligibleDoctorIds &&
                selectedRegion.isNotBlank() &&
                WeeklyPlanRules.matchesArea(doctor.area, selectedRegion) &&
                doctor.id !in assignedDoctorIdsThisWeek
        }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }
    val entriesByDate = remember(entries) { entries.groupingBy { it.visitDate }.eachCount() }

    var selectedDoctorIds by remember(selectedDate, selectedRegion, eligibleDoctors) {
        mutableStateOf(emptySet<Long>())
    }
    var doctorMenu by remember { mutableStateOf(false) }
    var doctorSearch by remember { mutableStateOf("") }
    var showAllDoctorsDialog by remember { mutableStateOf(false) }
    var allDoctorsSearch by remember { mutableStateOf("") }
    var showPlanDescription by remember { mutableStateOf(false) }
    var detailsDoctorId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        if (isArabic) "خطة الزيارات الأسبوعية" else "Weekly visit plan",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { showPlanDescription = true }, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            Icons.Outlined.HelpOutline,
                            contentDescription = if (isArabic) "عن الخطة الأسبوعية" else "About weekly plan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { month = month.minusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(if (isArabic) Icons.Default.ChevronRight else Icons.Default.ChevronLeft, contentDescription = if (isArabic) "الشهر السابق" else "Previous month")
                            }
                            Text(
                                month.format(DateTimeFormatter.ofPattern(if (isArabic) "MMMM yyyy" else "MMMM yyyy", if (isArabic) Locale("ar") else Locale.ENGLISH)),
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            IconButton(onClick = { month = month.plusMonths(1); selectedDate = month.atDay(1) }) {
                                Icon(if (isArabic) Icons.Default.ChevronLeft else Icons.Default.ChevronRight, contentDescription = if (isArabic) "الشهر التالي" else "Next month")
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            planDays.forEach { (_, names) ->
                                Text(
                                    if (isArabic) names.second else names.first,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    softWrap = false,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.SemiBold),
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
                                    val count = entriesByDate[date.toString()] ?: 0
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (date == selectedDate) MaterialTheme.colorScheme.primary
                                                else if (count > 0) MaterialTheme.colorScheme.surfaceContainerHighest
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .clickable {
                                                selectedDate = date
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
                                                color = if (date == selectedDate) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
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
                        value = when (selectedRegion) {
                            ALL_REGIONS -> if (isArabic) "كل المناطق" else "All areas"
                            "" -> if (isArabic) "اختر المنطقة" else "Choose area"
                            else -> selectedRegion
                        },
                        onClick = { regionMenu = true },
                        enabled = regions.isNotEmpty()
                    )
                    DropdownMenu(expanded = regionMenu, onDismissRequest = { regionMenu = false }) {
                        DropdownMenuItem(text = { Text(if (isArabic) "كل المناطق" else "All areas") }, onClick = {
                            selectedRegion = ALL_REGIONS
                            selectedDoctorIds = emptySet()
                            regionMenu = false
                        })
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
                            else -> doctors.filter { it.id in selectedDoctorIds }.joinToString("، ") { it.name }
                        },
                        onClick = { doctorMenu = true },
                        enabled = selectedRegion.isNotBlank() && doctors.any { it.id !in assignedDoctorIdsThisWeek }
                    )
                    if (doctorMenu) {
                        ModalBottomSheet(
                            onDismissRequest = {
                                doctorMenu = false
                                doctorSearch = ""
                            },
                            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                            containerColor = AppDialogContainerColor()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    if (isArabic) "الأطباء المتاحون" else "Available doctors",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                OutlinedTextField(
                                    value = doctorSearch,
                                    onValueChange = { doctorSearch = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = AppFilledTextFieldColors(),
                                    placeholder = { Text(if (isArabic) "ابحث عن دكتور..." else "Search doctors...") },
                                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                                )
                                val filteredEligibleDoctors = eligibleDoctors.filter { doctor ->
                                    doctor.name.contains(doctorSearch.trim(), ignoreCase = true) ||
                                        doctor.specialty.contains(doctorSearch.trim(), ignoreCase = true)
                                }
                                LazyColumn(
                                    modifier = Modifier.fillMaxWidth().height(360.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    lazyItems(filteredEligibleDoctors, key = { it.id }) { doctor ->
                                        Surface(
                                            color = AppSelectionRowColor(),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().clickable {
                                                    selectedDoctorIds = if (doctor.id in selectedDoctorIds) selectedDoctorIds - doctor.id else selectedDoctorIds + doctor.id
                                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = doctor.id in selectedDoctorIds,
                                                    onCheckedChange = { checked ->
                                                        selectedDoctorIds = if (checked) selectedDoctorIds + doctor.id else selectedDoctorIds - doctor.id
                                                    }
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(doctor.name)
                                                    if (doctor.specialty.isNotBlank()) {
                                                        Text(doctor.specialty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    if (filteredEligibleDoctors.isEmpty()) item {
                                        Text(
                                            if (isArabic) "لا توجد نتائج" else "No results",
                                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    item {
                                        Surface(
                                            onClick = {
                                                doctorMenu = false
                                                doctorSearch = ""
                                                allDoctorsSearch = ""
                                                showAllDoctorsDialog = true
                                            },
                                            color = AppSelectionRowColor(),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                        ) {
                                            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), contentAlignment = Alignment.Center) {
                                                Text(if (isArabic) "دكتور آخر — من كل الأطباء" else "Other doctor — browse all doctors", textAlign = TextAlign.Center)
                                            }
                                        }
                                    }
                                }
                                Button(onClick = {
                                    doctorMenu = false
                                    doctorSearch = ""
                                }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (isArabic) "تم" else "Done")
                                }
                            }
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
                            val chosen = doctors.filter { it.id in selectedDoctorIds && it.id !in assignedDoctorIdsThisWeek }
                            scope.launch {
                                chosen.forEach { doctor ->
                                    database.weeklyPlanDao().insert(
                                        WeeklyPlanEntryEntity(
                                            weekStartDate = weekStartText,
                                            visitDate = selectedDate.toString(),
                                            doctorId = doctor.id,
                                            centerId = null,
                                            regionSnapshot = if (selectedRegion == ALL_REGIONS) weeklyDoctorAreas(doctor.area).joinToString("، ") else selectedRegion,
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
                        Text(if (isArabic) "إضافة ${selectedDoctorIds.size} طبيب للخطة" else "Add ${selectedDoctorIds.size} doctor(s) to plan")
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.weight(1f).clickable { detailsDoctorId = entry.doctorId }.padding(vertical = 2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    entry.doctorNameSnapshot,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    entry.regionSnapshot,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.centerNameSnapshot.isNotBlank()) {
                                    Text(
                                        entry.centerNameSnapshot,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(
                                onClick = { scope.launch { database.weeklyPlanDao().delete(entry) } },
                                modifier = Modifier.height(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = if (isArabic) "إزالة الطبيب من الخطة" else "Remove doctor from plan",
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                            Button(
                                onClick = { onRecordVisit(entry.doctorId, entry.visitDate) },
                                modifier = Modifier.height(40.dp),
                                shape = MaterialTheme.shapes.small,
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = AppSpacing.md, vertical = 0.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.EditNote,
                                    contentDescription = null,
                                    modifier = Modifier.height(17.dp)
                                )
                                Text(
                                    text = if (isArabic) "تسجيل زيارة" else "Record visit",
                                    fontWeight = FontWeight.Medium,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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

    detailsDoctorId?.let { doctorId ->
        val doctor = doctors.firstOrNull { it.id == doctorId }
        if (doctor != null) {
            val linkedCenterIds = centerLinks.filter { it.doctorId == doctorId }.map { it.centerId }.toSet()
            val doctorCenters = centers.filter { it.id in linkedCenterIds || (linkedCenterIds.isEmpty() && it.id == doctor.centerId) }
            DoctorDetailsDialog(
                doctor = doctor,
                centers = doctorCenters,
                availability = availability.filter { it.doctorId == doctorId },
                visits = visits.filter { it.doctorId == doctorId },
                isArabic = isArabic,
                onDismiss = { detailsDoctorId = null },
                onUpdateCenterLocation = { center, latitude, longitude, mapUrl ->
                    scope.launch {
                        database.medicalCenterDao().update(
                            center.copy(latitude = latitude, longitude = longitude, mapUrl = mapUrl)
                        )
                    }
                },
                onUpdateDoctorLocation = { updatedDoctor ->
                    scope.launch { database.doctorDao().update(updatedDoctor) }
                }
            )
        } else {
            detailsDoctorId = null
        }
    }

    if (showPlanDescription) {
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showPlanDescription = false },
            title = { Text(if (isArabic) "خطة الزيارات الأسبوعية" else "Weekly visit plan") },
            text = { Text(if (isArabic) "اختر التاريخ والمنطقة، ثم اختر الأطباء المتاحين في هذا اليوم." else "Choose a date and area, then select doctors available on that day.") },
            confirmButton = {
                TextButton(onClick = { showPlanDescription = false }) {
                    Text(if (isArabic) "حسناً" else "Got it")
                }
            }
        )
    }

    if (showAllDoctorsDialog) {
        val otherDoctors = doctors
            .filter { it.id !in assignedDoctorIdsThisWeek }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
        val filteredOtherDoctors = otherDoctors.filter { doctor ->
            doctor.name.contains(allDoctorsSearch.trim(), ignoreCase = true) ||
                doctor.area.contains(allDoctorsSearch.trim(), ignoreCase = true) ||
                doctor.specialty.contains(allDoctorsSearch.trim(), ignoreCase = true)
        }
        ModalBottomSheet(
            onDismissRequest = {
                showAllDoctorsDialog = false
                allDoctorsSearch = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppDialogContainerColor()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabic) "اختيار دكتور آخر" else "Choose another doctor",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = allDoctorsSearch,
                    onValueChange = { allDoctorsSearch = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = AppFilledTextFieldColors(),
                    placeholder = { Text(if (isArabic) "ابحث بالاسم أو المنطقة أو التخصص..." else "Search by name, area or specialty...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                if (otherDoctors.isEmpty()) {
                    Text(
                        if (isArabic) "لا يوجد أطباء آخرون متاحون للإضافة؛ تم إضافة باقي الأطباء بالفعل إلى خطة هذا الأسبوع." else "There are no other doctors to add; the remaining doctors are already assigned this week.",
                        modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().height(360.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        lazyItems(filteredOtherDoctors, key = { it.id }) { doctor ->
                            Surface(
                                color = AppSelectionRowColor(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedDoctorIds = if (doctor.id in selectedDoctorIds) selectedDoctorIds - doctor.id else selectedDoctorIds + doctor.id
                                    }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = doctor.id in selectedDoctorIds,
                                        onCheckedChange = { checked ->
                                            selectedDoctorIds = if (checked) selectedDoctorIds + doctor.id else selectedDoctorIds - doctor.id
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doctor.name)
                                        if (doctor.area.isNotBlank()) Text(doctor.area, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        if (doctor.specialty.isNotBlank()) Text(doctor.specialty, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                        if (filteredOtherDoctors.isEmpty()) item {
                            Text(
                                if (isArabic) "لا توجد نتائج" else "No results",
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Button(onClick = {
                    showAllDoctorsDialog = false
                    allDoctorsSearch = ""
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (isArabic) "تم" else "Done")
                }
            }
        }
    }

    showExportError?.let { message ->
        AlertDialog(
        containerColor = AppDialogContainerColor(),
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
