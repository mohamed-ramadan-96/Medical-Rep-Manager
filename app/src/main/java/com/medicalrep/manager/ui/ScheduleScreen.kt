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
    var editing by remember { mutableStateOf<DoctorAvailabilityEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoctorAvailabilityEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            if (doctors.isNotEmpty()) {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
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
                if (isArabic) "يمكنك إضافة أكثر من فترة متاحة لكل طبيب خلال الأسبوع." else "Add multiple available time slots for each doctor during the week.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (doctors.isEmpty()) {
                Text(
                    if (isArabic) "أضف طبيباً أولاً حتى تتمكن من تحديد مواعيده." else "Add a doctor first to define their availability.",
                    modifier = Modifier.padding(top = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (slots.isEmpty()) {
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
                    items(slots, key = { it.id }) { slot ->
                        val doctor = doctors.firstOrNull { it.id == slot.doctorId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        weekDays.firstOrNull { it.first == slot.dayOfWeek }?.second?.let { if (isArabic) it.second else it.first } ?: "",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(Icons.Default.Schedule, contentDescription = null)
                                        Text("${slot.startTime} – ${slot.endTime}")
                                    }
                                    if (slot.notes.isNotBlank()) Text(slot.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { editing = slot; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit")
                                }
                                IconButton(onClick = { pendingDelete = slot }) {
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
            initial = editing,
            doctors = doctors,
            onDismiss = { showForm = false },
            onSave = { slot ->
                scope.launch {
                    if (slot.id == 0L) availabilityDao.insert(slot) else availabilityDao.update(slot)
                }
                showForm = false
            }
        )
    }

    pendingDelete?.let { slot ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الفترة؟" else "Delete time slot?") },
            text = { Text(if (isArabic) "سيتم حذف هذه الفترة المتاحة فقط." else "Only this availability slot will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { availabilityDao.delete(slot) }
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
    initial: DoctorAvailabilityEntity?,
    doctors: List<DoctorEntity>,
    onDismiss: () -> Unit,
    onSave: (DoctorAvailabilityEntity) -> Unit
) {
    var selectedDoctorId by remember(initial, doctors) {
        mutableStateOf(initial?.doctorId ?: doctors.firstOrNull()?.id)
    }
    var selectedDay by remember(initial) { mutableStateOf(initial?.dayOfWeek ?: 1) }
    var startTime by remember(initial) { mutableStateOf(initial?.startTime ?: "09:00") }
    var endTime by remember(initial) { mutableStateOf(initial?.endTime ?: "17:00") }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var doctorMenu by remember { mutableStateOf(false) }
    var dayMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isArabic) "إضافة فترة متاحة" else "Add availability"
                } else {
                    if (isArabic) "تعديل الفترة" else "Edit availability"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (isArabic) "الطبيب" else "Doctor", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { doctorMenu = true }) {
                    Text(doctors.firstOrNull { it.id == selectedDoctorId }?.name ?: if (isArabic) "اختر الطبيب" else "Choose doctor")
                }
                DropdownMenu(expanded = doctorMenu, onDismissRequest = { doctorMenu = false }) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text(doctor.name) },
                            onClick = { selectedDoctorId = doctor.id; doctorMenu = false }
                        )
                    }
                }

                Text(if (isArabic) "اليوم" else "Day", style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = { dayMenu = true }) {
                    val day = weekDays.first { it.first == selectedDay }.second
                    Text(if (isArabic) day.second else day.first)
                }
                DropdownMenu(expanded = dayMenu, onDismissRequest = { dayMenu = false }) {
                    weekDays.forEach { (number, names) ->
                        DropdownMenuItem(
                            text = { Text(if (isArabic) names.second else names.first) },
                            onClick = { selectedDay = number; dayMenu = false }
                        )
                    }
                }

                OutlinedTextField(
                    value = startTime,
                    onValueChange = { startTime = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "من (HH:MM)" else "From (HH:MM)") },
                    placeholder = { Text("09:00") }
                )
                OutlinedTextField(
                    value = endTime,
                    onValueChange = { endTime = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "إلى (HH:MM)" else "To (HH:MM)") },
                    placeholder = { Text("17:00") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "ملاحظات (اختياري)" else "Notes (optional)") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
                when {
                    selectedDoctorId == null -> error = if (isArabic) "اختر الطبيب." else "Choose a doctor."
                    !validTime.matches(startTime) || !validTime.matches(endTime) -> error = if (isArabic) "اكتب الوقت بصيغة HH:MM." else "Enter time in HH:MM format."
                    endTime <= startTime -> error = if (isArabic) "وقت النهاية يجب أن يكون بعد البداية." else "End time must be after start time."
                    else -> onSave(
                        DoctorAvailabilityEntity(
                            id = initial?.id ?: 0L,
                            doctorId = selectedDoctorId!!,
                            dayOfWeek = selectedDay,
                            startTime = startTime,
                            endTime = endTime,
                            notes = notes.trim(),
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        )
                    )
                }
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )
}
