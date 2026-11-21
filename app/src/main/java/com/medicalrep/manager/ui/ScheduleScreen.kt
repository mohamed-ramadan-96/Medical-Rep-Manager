package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    val days: Set<Int> = setOf(1),
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
    val doctorCenterLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editingGroup by remember { mutableStateOf<List<DoctorAvailabilityEntity>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<DoctorAvailabilityEntity>?>(null) }

    val groupedSlots = slots.groupBy { it.slotGroupId }
        .values.sortedWith(compareBy({ it.firstOrNull()?.dayOfWeek ?: 0 }, { it.firstOrNull()?.startTime.orEmpty() }))

    Scaffold(
        modifier = Modifier.padding(padding),
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
            Text(
                if (isArabic) "جدول المواعيد المتاحة" else "Weekly availability",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (isArabic) "أضف أكثر من فترة للطبيب نفسه، ولكل فترة أيام وساعات مستقلة." else "Add multiple independent time slots for the same doctor, each with its own days and hours.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (doctors.isEmpty()) {
                Text(if (isArabic) "أضف طبيباً أولاً حتى تتمكن من تحديد مواعيده." else "Add a doctor first to define their availability.", modifier = Modifier.padding(top = 20.dp))
            } else if (groupedSlots.isEmpty()) {
                Text(if (isArabic) "لم تتم إضافة مواعيد متاحة بعد. اضغط + للبدء." else "No availability added yet. Tap + to get started.", modifier = Modifier.padding(top = 20.dp))
            } else {
                LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(groupedSlots, key = { group -> group.first().id }) { group ->
                        val slot = group.first()
                        val doctor = doctors.firstOrNull { it.id == slot.doctorId }
                        val centerName = centers.firstOrNull { it.id == slot.centerId }?.name
                        val dayNames = group.mapNotNull { item -> weekDays.firstOrNull { it.first == item.dayOfWeek }?.second?.let { if (isArabic) it.second else it.first } }.joinToString("، ")
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (!centerName.isNullOrBlank()) Text(centerName, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                                    Text(dayNames, color = MaterialTheme.colorScheme.primary)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Text("${slot.startTime} – ${slot.endTime}")
                                    }
                                    if (slot.notes.isNotBlank()) Text(slot.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { editingGroup = group; showForm = true }) { Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(20.dp)) }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { pendingDelete = group }) { Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(20.dp)) }
                            }
                        }
                    }
                }
            }
        }
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
                            Column(modifier = Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                                weekDays.forEach { (number, names) ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                                OutlinedTextField(
                                    value = displayAvailabilityTime(draft.startTime),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { timePickerTarget = index to true },
                                    singleLine = true,
                                    label = { Text(if (isArabic) "من — اختر الوقت (AM / PM)" else "From — choose time (AM / PM)") }
                                )
                                OutlinedTextField(
                                    value = displayAvailabilityTime(draft.endTime),
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier.fillMaxWidth().clickable { timePickerTarget = index to false },
                                    singleLine = true,
                                    label = { Text(if (isArabic) "إلى — اختر الوقت (AM / PM)" else "To — choose time (AM / PM)") }
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
