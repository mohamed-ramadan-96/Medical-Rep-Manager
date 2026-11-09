package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.History
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
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun visitDateText(timestamp: Long, pattern: String = "yyyy-MM-dd"): String =
    SimpleDateFormat(pattern, Locale.US).format(Date(timestamp))

private fun parseVisitDate(date: String, time: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        .parse("$date $time")?.time
} catch (_: Exception) {
    null
}

@Composable
fun VisitsScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val visitDao = database.doctorVisitDao()
    val doctorDao = database.doctorDao()
    val visits by visitDao.observeAll().collectAsState(initial = emptyList())
    val doctors by doctorDao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<DoctorVisitEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoctorVisitEntity?>(null) }
    var search by remember { mutableStateOf("") }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            if (doctors.isNotEmpty()) {
                FloatingActionButton(onClick = { editing = null; showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة زيارة" else "Add visit", modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                if (isArabic) "سجل زيارات الأطباء" else "Doctor visit history",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (isArabic) "سجّل نتيجة كل زيارة وملاحظاتها وموعد المتابعة." else "Record each visit outcome, notes, and next follow-up.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (isArabic) "ابحث باسم الطبيب أو النتيجة" else "Search doctor or outcome") }
            )
            val visibleVisits = visits.filter { visit ->
                val doctorName = doctors.firstOrNull { it.id == visit.doctorId }?.name.orEmpty()
                doctorName.contains(search, ignoreCase = true) ||
                    visit.outcome.contains(search, ignoreCase = true) ||
                    visit.notes.contains(search, ignoreCase = true)
            }
            if (doctors.isEmpty()) {
                Text(
                    if (isArabic) "أضف طبيباً أولاً لتسجيل الزيارات." else "Add a doctor first to record visits.",
                    modifier = Modifier.padding(top = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (visibleVisits.isEmpty()) {
                Text(
                    if (isArabic) "لا توجد زيارات مسجلة بعد. اضغط + لإضافة أول زيارة." else "No visits recorded yet. Tap + to add the first visit.",
                    modifier = Modifier.padding(top = 18.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleVisits, key = { it.id }) { visit ->
                        val doctor = doctors.firstOrNull { it.id == visit.doctorId }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Text(
                                        doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "${visitDateText(visit.visitedAt, "yyyy-MM-dd  HH:mm")} • ${visit.outcome}",
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    if (visit.notes.isNotBlank()) Text(visit.notes, style = MaterialTheme.typography.bodyMedium)
                                    visit.nextFollowUpAt?.let {
                                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                            Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(20.dp))
                                            Text(
                                                (if (isArabic) "المتابعة: " else "Follow-up: ") + visitDateText(it),
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                                Column {
                                    IconButton(modifier = Modifier.size(40.dp), onClick = { editing = visit; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(modifier = Modifier.size(40.dp), onClick = { pendingDelete = visit }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        VisitFormDialog(
            isArabic = isArabic,
            initial = editing,
            doctors = doctors,
            onDismiss = { showForm = false },
            onSave = { visit ->
                scope.launch {
                    if (visit.id == 0L) visitDao.insert(visit) else visitDao.update(visit)
                }
                showForm = false
            }
        )
    }

    pendingDelete?.let { visit ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الزيارة؟" else "Delete visit?") },
            text = { Text(if (isArabic) "سيتم حذف سجل هذه الزيارة فقط." else "Only this visit record will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { visitDao.delete(visit) }
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
private fun VisitFormDialog(
    isArabic: Boolean,
    initial: DoctorVisitEntity?,
    doctors: List<DoctorEntity>,
    onDismiss: () -> Unit,
    onSave: (DoctorVisitEntity) -> Unit
) {
    var selectedDoctorId by remember(initial, doctors) {
        mutableStateOf(initial?.doctorId ?: doctors.firstOrNull()?.id)
    }
    var date by remember(initial) {
        mutableStateOf(initial?.let { visitDateText(it.visitedAt) } ?: visitDateText(System.currentTimeMillis()))
    }
    var time by remember(initial) {
        mutableStateOf(initial?.let { visitDateText(it.visitedAt, "HH:mm") } ?: visitDateText(System.currentTimeMillis(), "HH:mm"))
    }
    var outcome by remember(initial) { mutableStateOf(initial?.outcome ?: if (isArabic) "تمت الزيارة" else "Completed") }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var followUpDate by remember(initial) {
        mutableStateOf(initial?.nextFollowUpAt?.let { visitDateText(it) }.orEmpty())
    }
    var doctorMenu by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isArabic) "تسجيل زيارة جديدة" else "Record a visit"
                } else {
                    if (isArabic) "تعديل الزيارة" else "Edit visit"
                }
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedSelectionField(
                    label = if (isArabic) "الطبيب" else "Doctor",
                    value = doctors.firstOrNull { it.id == selectedDoctorId }?.name
                        ?: if (isArabic) "اختر الطبيب" else "Choose doctor",
                    onClick = { doctorMenu = true }
                )
                DropdownMenu(expanded = doctorMenu, onDismissRequest = { doctorMenu = false }) {
                    doctors.forEach { doctor ->
                        DropdownMenuItem(
                            text = { Text(doctor.name) },
                            onClick = { selectedDoctorId = doctor.id; doctorMenu = false }
                        )
                    }
                }
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "تاريخ الزيارة (YYYY-MM-DD)" else "Visit date (YYYY-MM-DD)") }
                )
                OutlinedTextField(
                    value = time,
                    onValueChange = { time = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "الوقت (HH:MM)" else "Time (HH:MM)") }
                )
                OutlinedTextField(
                    value = outcome,
                    onValueChange = { outcome = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "نتيجة الزيارة" else "Visit outcome") }
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (isArabic) "ملاحظات الزيارة" else "Visit notes") }
                )
                OutlinedTextField(
                    value = followUpDate,
                    onValueChange = { followUpDate = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "موعد المتابعة (اختياري YYYY-MM-DD)" else "Follow-up date (optional YYYY-MM-DD)") }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val visitedAt = parseVisitDate(date, time)
                val followUpAt = if (followUpDate.isBlank()) null else parseVisitDate(followUpDate, "09:00")
                when {
                    selectedDoctorId == null -> error = if (isArabic) "اختر الطبيب." else "Choose a doctor."
                    visitedAt == null -> error = if (isArabic) "تأكد من التاريخ والوقت." else "Check the date and time."
                    outcome.isBlank() -> error = if (isArabic) "اكتب نتيجة الزيارة." else "Enter the visit outcome."
                    followUpDate.isNotBlank() && followUpAt == null -> error = if (isArabic) "تأكد من تاريخ المتابعة." else "Check the follow-up date."
                    followUpAt != null && followUpAt < visitedAt -> error = if (isArabic) "المتابعة لا يمكن أن تسبق الزيارة." else "Follow-up cannot be before the visit."
                    else -> onSave(
                        DoctorVisitEntity(
                            id = initial?.id ?: 0L,
                            doctorId = selectedDoctorId!!,
                            visitedAt = visitedAt,
                            outcome = outcome.trim(),
                            notes = notes.trim(),
                            nextFollowUpAt = followUpAt,
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
