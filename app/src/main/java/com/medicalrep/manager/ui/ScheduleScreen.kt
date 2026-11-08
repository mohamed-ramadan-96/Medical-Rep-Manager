package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch

private val weekDays = listOf(
    1 to ("Monday" to "الاثنين"),
    2 to ("Tuesday" to "الثلاثاء"),
    3 to ("Wednesday" to "الأربعاء"),
    4 to ("Thursday" to "الخميس"),
    5 to ("Friday" to "الجمعة"),
    6 to ("Saturday" to "السبت"),
    7 to ("Sunday" to "الأحد")
)

@Composable
fun ScheduleScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val availabilityDao = database.doctorAvailabilityDao()
    val doctorDao = database.doctorDao()
    val slots by availabilityDao.observeAll().collectAsState(initial = emptyList())
    val doctors by doctorDao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editingGroup by remember { mutableStateOf<List<DoctorAvailabilityEntity>>(emptyList()) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<DoctorAvailabilityEntity>?>(null) }

    val groupedSlots = slots.groupBy { "${it.doctorId}|${it.startTime}|${it.endTime}|${it.notes}" }
        .values.sortedWith(compareBy({ it.firstOrNull()?.dayOfWeek ?: 0 }, { it.firstOrNull()?.startTime.orEmpty() }))

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            if (doctors.isNotEmpty()) {
                FloatingActionButton(onClick = { editingGroup = emptyList(); showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة موعد متاح" else "Add availability")
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
                if (isArabic) "اختار أكتر من يوم لنفس الفترة، أو أضف كل يوم بميعاد مختلف." else "Select multiple days for one time slot, or add days separately with different times.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (doctors.isEmpty()) {
                Text(
                    if (isArabic) "أضف طبيباً أولاً حتى تتمكن من تحديد مواعيده." else "Add a doctor first to define their availability.",
                    modifier = Modifier.padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (groupedSlots.isEmpty()) {
                Text(
                    if (isArabic) "لم تتم إضافة مواعيد متاحة بعد. اضغط + للبدء." else "No availability added yet. Tap + to get started.",
                    modifier = Modifier.padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(groupedSlots, key = { group -> group.first().id }) { group ->
                        val slot = group.first()
                        val doctor = doctors.firstOrNull { it.id == slot.doctorId }
                        val days = group.map { it.dayOfWeek }.distinct().sorted()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Text(
                                        days.mapNotNull { day -> weekDays.firstOrNull { it.first == day }?.second?.let { if (isArabic) it.second else it.first } }.joinToString("، "),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                        Text("${slot.startTime} – ${slot.endTime}")
                                    }
                                    if (slot.notes.isNotBlank()) Text(slot.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { editingGroup = group; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit")
                                }
                                IconButton(onClick = { pendingDelete = group }) {
                                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete")
                                }
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
            title = { Text(if (isArabic) "حذف المواعيد؟" else "Delete availability?") },
            text = { Text(if (isArabic) "سيتم حذف الأيام المحددة في هذه المجموعة فقط." else "Only the days in this grouped time slot will be deleted.") },
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
    onDismiss: () -> Unit,
    onSave: (List<DoctorAvailabilityEntity>) -> Unit
) {
    val initial = initialGroup.firstOrNull()
    var selectedDoctorId by remember(initialGroup, doctors) {
        mutableStateOf(initial?.doctorId ?: doctors.firstOrNull()?.id)
    }
    var selectedDays by remember(initialGroup) {
        mutableStateOf(initialGroup.map { it.dayOfWeek }.toSet().ifEmpty { setOf(1) })
    }
    var startTime by remember(initialGroup) { mutableStateOf(initial?.startTime ?: "09:00") }
    var endTime by remember(initialGroup) { mutableStateOf(initial?.endTime ?: "17:00") }
    var notes by remember(initialGroup) { mutableStateOf(initial?.notes.orEmpty()) }
    var doctorMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (initialGroup.isEmpty()) {
                if (isArabic) "إضافة مواعيد متاحة" else "Add availability"
            } else {
                if (isArabic) "تعديل المواعيد" else "Edit availability"
            })
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    Text(if (isArabic) "الطبيب" else "Doctor", style = MaterialTheme.typography.labelLarge)
                    TextButton(onClick = { doctorMenu = true }) {
                        Text(doctors.firstOrNull { it.id == selectedDoctorId }?.name ?: if (isArabic) "اختر الطبيب" else "Choose doctor")
                    }
                    DropdownMenu(expanded = doctorMenu, onDismissRequest = { doctorMenu = false }) {
                        doctors.forEach { doctor ->
                            DropdownMenuItem(text = { Text(doctor.name) }, onClick = { selectedDoctorId = doctor.id; doctorMenu = false })
                        }
                    }
                }
                item {
                    Text(if (isArabic) "أيام التواجد (يمكن اختيار أكثر من يوم)" else "Available days (select more than one)", style = MaterialTheme.typography.labelLarge)
                }
                items(weekDays) { (number, names) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = number in selectedDays,
                            onCheckedChange = { checked ->
                                selectedDays = if (checked) selectedDays + number else selectedDays - number
                                error = null
                            }
                        )
                        Text(if (isArabic) names.second else names.first)
                    }
                }
                item {
                    OutlinedTextField(
                        value = startTime, onValueChange = { startTime = it; error = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "من (HH:MM)" else "From (HH:MM)") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = endTime, onValueChange = { endTime = it; error = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "إلى (HH:MM)" else "To (HH:MM)") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isArabic) "ملاحظات (اختياري)" else "Notes (optional)") }
                    )
                }
                item { error?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
                when {
                    selectedDoctorId == null -> error = if (isArabic) "اختر الطبيب." else "Choose a doctor."
                    selectedDays.isEmpty() -> error = if (isArabic) "اختار يوم واحد على الأقل." else "Select at least one day."
                    !validTime.matches(startTime) || !validTime.matches(endTime) -> error = if (isArabic) "اكتب الوقت بصيغة HH:MM." else "Enter time in HH:MM format."
                    endTime <= startTime -> error = if (isArabic) "وقت النهاية يجب أن يكون بعد البداية." else "End time must be after start time."
                    else -> {
                        val oldByDay = initialGroup.associateBy { it.dayOfWeek }
                        onSave(selectedDays.sorted().map { day ->
                            val old = oldByDay[day]
                            DoctorAvailabilityEntity(
                                id = old?.id ?: 0L,
                                doctorId = selectedDoctorId!!,
                                dayOfWeek = day,
                                startTime = startTime,
                                endTime = endTime,
                                notes = notes.trim(),
                                createdAt = old?.createdAt ?: System.currentTimeMillis()
                            )
                        })
                    }
                }
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}
