package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.DoctorAvailabilityEntity
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.DoctorCenterLinkEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.Locale

private val weekDays = listOf(
    6 to ("Saturday" to "السبت"),
    7 to ("Sunday" to "الأحد"),
    1 to ("Monday" to "الاثنين"),
    2 to ("Tuesday" to "الثلاثاء"),
    3 to ("Wednesday" to "الأربعاء"),
    4 to ("Thursday" to "الخميس"),
    5 to ("Friday" to "الجمعة")
)

private data class AvailabilityDraft(
    val days: Set<Int> = setOf(6),
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val notes: String = "",
    val groupId: String = UUID.randomUUID().toString()
)

@Composable
fun ScheduleScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val availabilityDao = database.doctorAvailabilityDao()
    val doctorDao = database.doctorDao()
    val slots by availabilityDao.observeAll().collectAsState(initial = emptyList())
    val doctors by doctorDao.observeAll().collectAsState(initial = emptyList())
    val centers by database.medicalCenterDao().observeAll().collectAsState(initial = emptyList())
    val specialties by database.specialtyDao().observeAll().collectAsState(initial = emptyList())
    val doctorCenterLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editingGroup by remember { mutableStateOf<List<DoctorAvailabilityEntity>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<DoctorAvailabilityEntity>?>(null) }
    var selectedDay by remember { mutableStateOf(0) }
    var selectedCenterId by remember { mutableStateOf(0L) }
    var selectedSpecialty by remember { mutableStateOf("all") }
    var dayMenu by remember { mutableStateOf(false) }
    var centerFilterMenu by remember { mutableStateOf(false) }
    var specialtyFilterMenu by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    var showPageDescription by remember { mutableStateOf(false) }
    var expandedAvailabilityGroups by remember { mutableStateOf(emptySet<String>()) }

    val doctorsById = remember(doctors) { doctors.associateBy { it.id } }
    val groupedSlots = remember(slots) {
        slots.groupBy { it.slotGroupId.ifBlank { "legacy-${it.id}" } }
            .values.sortedWith(compareBy(
                { group -> weekDays.indexOfFirst { it.first == group.firstOrNull()?.dayOfWeek }.let { if (it < 0) Int.MAX_VALUE else it } },
                { group -> group.firstOrNull()?.startTime.orEmpty() }
            ))
    }
    val filteredGroups = remember(groupedSlots, doctorsById, selectedDay, selectedCenterId, selectedSpecialty) {
        groupedSlots.mapNotNull { group ->
            val matching = group.filter { slot ->
                val doctor = doctorsById[slot.doctorId]
                (selectedDay == 0 || slot.dayOfWeek == selectedDay) &&
                (selectedCenterId == 0L || slot.centerId == selectedCenterId) &&
                (selectedSpecialty == "all" || doctor?.specialty.equals(selectedSpecialty, ignoreCase = true))
            }
            matching.takeIf { it.isNotEmpty() }
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (doctors.isNotEmpty()) {
                FloatingActionButton(onClick = { editingGroup = emptyList(); showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة مواعيد متاحة" else "Add availability", modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (isArabic) "جدول المواعيد المتاحة" else "Weekly availability",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showPageDescription = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.HelpOutline, contentDescription = if (isArabic) "عن جدول المواعيد" else "About availability", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Surface(
                onClick = { showFilters = !showFilters },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(if (isArabic) "الفلاتر" else "Filters", modifier = Modifier.padding(horizontal = AppSpacing.md), style = MaterialTheme.typography.labelLarge)
                    Icon(if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            }
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(animationSpec = tween(AppMotion.Emphasized)) + fadeIn(animationSpec = tween(AppMotion.Standard)),
                exit = shrinkVertically(animationSpec = tween(AppMotion.Standard)) + fadeOut(animationSpec = tween(AppMotion.Fast))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedSelectionField(label = if (isArabic) "اليوم" else "Day", value = if (selectedDay == 0) { if (isArabic) "كل الأيام" else "All days" } else weekDays.firstOrNull { it.first == selectedDay }?.second?.let { if (isArabic) it.second else it.first }.orEmpty(), onClick = { dayMenu = true })
                    DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                        DropdownMenuItem(text = { Text(if (isArabic) "كل الأيام" else "All days") }, onClick = { selectedDay = 0; dayMenu = false })
                        weekDays.forEach { (day, names) -> DropdownMenuItem(text = { Text(if (isArabic) names.second else names.first) }, onClick = { selectedDay = day; dayMenu = false }) }
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedSelectionField(label = if (isArabic) "التخصص" else "Specialty", value = if (selectedSpecialty == "all") { if (isArabic) "كل التخصصات" else "All specialties" } else selectedSpecialty, onClick = { specialtyFilterMenu = true })
                    DropdownMenu(expanded = specialtyFilterMenu, onDismissRequest = { specialtyFilterMenu = false }) {
                        DropdownMenuItem(text = { Text(if (isArabic) "كل التخصصات" else "All specialties") }, onClick = { selectedSpecialty = "all"; specialtyFilterMenu = false })
                        specialties.map { it.name }.distinct().sorted().forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { selectedSpecialty = name; specialtyFilterMenu = false }) }
                    }
                }
            }
            OutlinedSelectionField(label = if (isArabic) "المركز" else "Center", value = if (selectedCenterId == 0L) { if (isArabic) "كل المراكز" else "All centers" } else centers.firstOrNull { it.id == selectedCenterId }?.name.orEmpty(), onClick = { centerFilterMenu = true })
            DropdownMenu(expanded = centerFilterMenu, onDismissRequest = { centerFilterMenu = false }) {
                DropdownMenuItem(text = { Text(if (isArabic) "كل المراكز" else "All centers") }, onClick = { selectedCenterId = 0L; centerFilterMenu = false })
                centers.forEach { center -> DropdownMenuItem(text = { Text(center.name) }, onClick = { selectedCenterId = center.id; centerFilterMenu = false }) }
            }

                }
            }
            if (doctors.isEmpty()) {
                Text(if (isArabic) "أضف طبيباً أولاً حتى تتمكن من تحديد مواعيده." else "Add a doctor first to define their availability.", modifier = Modifier.padding(top = 20.dp))
            } else if (groupedSlots.isEmpty()) {
                Text(if (isArabic) "لم تتم إضافة مواعيد متاحة بعد. اضغط + للبدء." else "No availability added yet. Tap + to get started.", modifier = Modifier.padding(top = 20.dp))
            } else if (filteredGroups.isEmpty()) {
                Text(if (isArabic) "لا توجد مواعيد مطابقة للاختيارات." else "No availability matches these filters.", modifier = Modifier.padding(top = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                    items(filteredGroups, key = { group -> group.first().id }) { group ->
                        val slot = group.first()
                        val groupKey = slot.slotGroupId.ifBlank { "legacy-${slot.id}" }
                        val expanded = groupKey in expandedAvailabilityGroups
                        val expandRotation by animateFloatAsState(if (expanded) 180f else 0f, animationSpec = tween(AppMotion.Standard), label = "availabilityExpandRotation")
                        val doctor = doctors.firstOrNull { it.id == slot.doctorId }
                        val centerName = centers.firstOrNull { it.id == slot.centerId }?.name
                        val dayNames = weekDays.mapNotNull { (number, names) ->
                            group.firstOrNull { it.dayOfWeek == number }?.let { if (isArabic) names.second else names.first }
                        }.joinToString("، ")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = AppSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        if (!centerName.isNullOrBlank()) {
                                            Text(centerName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        } else {
                                            Text(if (isArabic) "موعد عام" else "General availability", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    IconButton(
                                        modifier = Modifier.size(32.dp),
                                        onClick = { expandedAvailabilityGroups = if (expanded) expandedAvailabilityGroups - groupKey else expandedAvailabilityGroups + groupKey }
                                    ) {
                                        Icon(
                                            Icons.Default.ExpandMore,
                                            contentDescription = if (isArabic) if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل" else if (expanded) "Hide details" else "Show details",
                                            modifier = Modifier.size(20.dp).rotate(expandRotation)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(dayNames, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${slot.startTime} – ${slot.endTime}", style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { editingGroup = group; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { pendingDelete = group }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically(tween(AppMotion.Emphasized)) + fadeIn(tween(AppMotion.Standard)),
                                    exit = shrinkVertically(tween(AppMotion.Standard)) + fadeOut(tween(AppMotion.Fast))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 2.dp, bottom = 4.dp)) {
                                        if (slot.notes.isNotBlank()) {
                                            Text(slot.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Text(if (isArabic) "لا توجد ملاحظات إضافية." else "No additional notes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showPageDescription) {
        AlertDialog(
            onDismissRequest = { showPageDescription = false },
            title = { Text(if (isArabic) "جدول المواعيد المتاحة" else "Weekly availability") },
            text = { Text(if (isArabic) "استعرض الأطباء حسب اليوم أو المركز أو التخصص، ويمكنك إضافة وتعديل مواعيد التواجد." else "Browse doctors by day, center or specialty, and add or edit their availability.") },
            confirmButton = { TextButton(onClick = { showPageDescription = false }) { Text(if (isArabic) "حسناً" else "Got it") } }
        )
    }

    if (showForm) {
        AvailabilityFormDialog(
            isArabic = isArabic,
            initialGroup = editingGroup,
            doctors = doctors,
            centers = centers,
            doctorCenterLinks = doctorCenterLinks,
            onDismiss = { showForm = false },
            onSave = { newSlots ->
                scope.launch {
                    editingGroup.forEach { availabilityDao.delete(it) }
                    newSlots.forEach { availabilityDao.insert(it) }
                }
                showForm = false
            }
        )
    }

    pendingDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الفترة؟" else "Delete time slot?") },
            text = { Text(if (isArabic) "سيتم حذف الأيام التابعة لهذه الفترة فقط." else "Only the days belonging to this time slot will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { group.forEach { availabilityDao.delete(it) } }
                    pendingDelete = null
                }) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
private fun AvailabilityFormDialog(
    isArabic: Boolean,
    initialGroup: List<DoctorAvailabilityEntity>,
    doctors: List<DoctorEntity>,
    centers: List<MedicalCenterEntity>,
    doctorCenterLinks: List<DoctorCenterLinkEntity>,
    onDismiss: () -> Unit,
    onSave: (List<DoctorAvailabilityEntity>) -> Unit
) {
    val initial = initialGroup.firstOrNull()
    var selectedDoctorId by remember(initialGroup, doctors) { mutableStateOf(initial?.doctorId ?: doctors.firstOrNull()?.id) }
    var drafts by remember(initialGroup) {
        mutableStateOf(
            if (initialGroup.isEmpty()) listOf(AvailabilityDraft())
            else listOf(AvailabilityDraft(
                days = initialGroup.map { it.dayOfWeek }.toSet(),
                startTime = initial!!.startTime,
                endTime = initial.endTime,
                notes = initial.notes,
                groupId = initial.slotGroupId
            ))
        )
    }
    var doctorMenu by remember { mutableStateOf(false) }
    var centerMenu by remember { mutableStateOf(false) }
    var selectedCenterId by remember(initialGroup, doctors, doctorCenterLinks) {
        mutableStateOf(initial?.centerId ?: doctorCenterLinks.firstOrNull { it.doctorId == selectedDoctorId }?.centerId ?: doctors.firstOrNull { it.id == selectedDoctorId }?.centerId)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var timePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val linkedCenters = centers.filter { center ->
        doctorCenterLinks.any { it.doctorId == selectedDoctorId && it.centerId == center.id } ||
            (doctorCenterLinks.none { it.doctorId == selectedDoctorId } && doctors.firstOrNull { it.id == selectedDoctorId }?.centerId == center.id)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialGroup.isEmpty()) { if (isArabic) "إضافة مواعيد للطبيب" else "Add doctor availability" } else { if (isArabic) "تعديل الفترة" else "Edit time slot" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedSelectionField(
                    label = if (isArabic) "الطبيب" else "Doctor",
                    value = doctors.firstOrNull { it.id == selectedDoctorId }?.name
                        ?: if (isArabic) "اختر الطبيب" else "Choose doctor",
                    onClick = { doctorMenu = true }
                )
                DropdownMenu(expanded = doctorMenu, onDismissRequest = { doctorMenu = false }) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(text = { Text(doctor.name) }, onClick = { selectedDoctorId = doctor.id; selectedCenterId = doctorCenterLinks.firstOrNull { it.doctorId == doctor.id }?.centerId ?: doctor.centerId; doctorMenu = false })
                    }
                }
                OutlinedSelectionField(
                    label = if (isArabic) "المركز" else "Center",
                    value = linkedCenters.firstOrNull { it.id == selectedCenterId }?.name
                        ?: if (isArabic) "اختر المركز" else "Choose center",
                    onClick = { centerMenu = true },
                    enabled = linkedCenters.isNotEmpty()
                )
                DropdownMenu(expanded = centerMenu, onDismissRequest = { centerMenu = false }) {
                    linkedCenters.forEach { center ->
                        DropdownMenuItem(text = { Text(center.name) }, onClick = { selectedCenterId = center.id; centerMenu = false })
                    }
                }
                Text(
                    if (isArabic) "كل فترة لها أيامها وساعاتها المستقلة" else "Each time slot has its own days and hours",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f, fill = false)) {
                    items(drafts.size) { index ->
                        val draft = drafts[index]
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        (if (isArabic) "الفترة " else "Time slot ") + (index + 1),
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (drafts.size > 1) {
                                        TextButton(onClick = { drafts = drafts.filterIndexed { i, _ -> i != index }; error = null }) {
                                            Text(if (isArabic) "حذف الفترة" else "Remove")
                                        }
                                    }
                                }
                                Text(if (isArabic) "أيام التواجد" else "Available days", style = MaterialTheme.typography.labelMedium)
                                weekDays.chunked(2).forEach { dayPair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        dayPair.forEach { (number, names) ->
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = number in draft.days,
                                                    onCheckedChange = { checked ->
                                                        val updated = if (checked) draft.days + number else draft.days - number
                                                        drafts = drafts.toMutableList().also { it[index] = draft.copy(days = updated) }
                                                        error = null
                                                    }
                                                )
                                                Text(if (isArabic) names.second else names.first)
                                            }
                                        }
                                        if (dayPair.size == 1) Spacer(Modifier.weight(1f))
                                    }
                                }
                                OutlinedSelectionField(
                                    label = if (isArabic) "من" else "From",
                                    value = displayAvailabilityTime(draft.startTime),
                                    onClick = { timePickerTarget = index to true }
                                )
                                OutlinedSelectionField(
                                    label = if (isArabic) "إلى" else "To",
                                    value = displayAvailabilityTime(draft.endTime),
                                    onClick = { timePickerTarget = index to false }
                                )
                                OutlinedTextField(
                                    value = draft.notes,
                                    onValueChange = { value -> drafts = drafts.toMutableList().also { it[index] = draft.copy(notes = value) } },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text(if (isArabic) "ملاحظات للفترة (اختياري)" else "Slot notes (optional)") }
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = { drafts = drafts + AvailabilityDraft(); error = null }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(if (isArabic) "إضافة فترة بمواعيد مختلفة" else "Add another time slot")
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
                when {
                    selectedDoctorId == null -> error = if (isArabic) "اختر الطبيب." else "Choose a doctor."
                    linkedCenters.isNotEmpty() && selectedCenterId == null -> error = if (isArabic) "اختر المركز." else "Choose a center."
                    drafts.isEmpty() -> error = if (isArabic) "أضف فترة واحدة على الأقل." else "Add at least one time slot."
                    drafts.any { it.days.isEmpty() } -> error = if (isArabic) "اختار يومًا واحدًا على الأقل لكل فترة." else "Select at least one day for every time slot."
                    drafts.any { !validTime.matches(it.startTime) || !validTime.matches(it.endTime) } -> error = if (isArabic) "اكتب كل الأوقات بصيغة HH:MM." else "Enter every time in HH:MM format."
                    drafts.any { it.endTime <= it.startTime } -> error = if (isArabic) "وقت النهاية يجب أن يكون بعد البداية في كل فترة." else "Each end time must be after its start time."
                    else -> {
                        val oldByDay = initialGroup.associateBy { it.dayOfWeek }
                        val result = drafts.flatMap { draft ->
                            draft.days.sorted().map { day ->
                                val old = oldByDay[day].takeIf { drafts.size == 1 }
                                DoctorAvailabilityEntity(
                                    id = old?.id ?: 0L,
                                    doctorId = selectedDoctorId!!,
                                    dayOfWeek = day,
                                    startTime = draft.startTime,
                                    endTime = draft.endTime,
                                    notes = draft.notes.trim(),
                                    createdAt = old?.createdAt ?: System.currentTimeMillis(),
                                    slotGroupId = draft.groupId,
                                    centerId = selectedCenterId
                                )
                            }
                        }
                        onSave(result)
                    }
                }
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") } }
    )

    timePickerTarget?.let { (draftIndex, isStart) ->
        val current = drafts.getOrNull(draftIndex)
        if (current != null) {
            AvailabilityTimePickerDialog(
                isArabic = isArabic,
                initialValue = if (isStart) current.startTime else current.endTime,
                onDismiss = { timePickerTarget = null },
                onConfirm = { hour, minute ->
                    val value = String.format(Locale.US, "%02d:%02d", hour, minute)
                    drafts = drafts.toMutableList().also { list ->
                        val draft = list[draftIndex]
                        list[draftIndex] = if (isStart) draft.copy(startTime = value) else draft.copy(endTime = value)
                    }
                    error = null
                    timePickerTarget = null
                }
            )
        }
    }
}

private fun displayAvailabilityTime(value: String): String {
    val pieces = value.split(":")
    val hour = pieces.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = pieces.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val suffix = if (hour < 12) "AM" else "PM"
    val displayHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvailabilityTimePickerDialog(
    isArabic: Boolean,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val pieces = initialValue.split(":")
    val initialHour = pieces.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val initialMinute = pieces.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "اختيار الوقت" else "Choose time") },
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(if (isArabic) "تأكيد" else "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}
