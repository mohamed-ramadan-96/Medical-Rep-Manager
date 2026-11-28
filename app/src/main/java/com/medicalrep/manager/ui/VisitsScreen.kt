package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppDialogContainerColor

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor
import com.medicalrep.manager.ui.theme.AppSelectionRowColor

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState

import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.followup.FollowUpReminderScheduler
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Date
import java.util.Locale
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState

private fun visitDateText(timestamp: Long, pattern: String = "yyyy-MM-dd"): String =
    SimpleDateFormat(pattern, Locale.US).format(Date(timestamp))

private fun parseVisitDate(date: String, time: String): Long? = try {
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }
        .parse("$date $time")?.time
} catch (_: Exception) {
    null
}

@Composable
fun VisitsScreen(
    isArabic: Boolean,
    padding: androidx.compose.foundation.layout.PaddingValues,
    initialDoctorId: Long? = null,
    initialVisitDate: String? = null,
    onInitialVisitHandled: () -> Unit = {}
) {
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
    var expandedVisitIds by remember { mutableStateOf(emptySet<Long>()) }
    var formDoctorId by remember { mutableStateOf<Long?>(null) }
    var formVisitDate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialDoctorId, initialVisitDate) {
        if (initialDoctorId != null && initialVisitDate != null) {
            editing = null
            formDoctorId = initialDoctorId
            formVisitDate = initialVisitDate
            showForm = true
            onInitialVisitHandled()
        }
    }

    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (doctors.isNotEmpty()) {
                FloatingActionButton(onClick = { editing = null; formDoctorId = null; formVisitDate = null; showForm = true }) {
                    Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة زيارة" else "Add visit", modifier = Modifier.size(20.dp))
                }
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            Text(
                if (isArabic) "سجل زيارات الأطباء" else "Doctor visit history",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                if (isArabic) "سجّل نتيجة كل زيارة وملاحظاتها وموعد المتابعة." else "Record each visit outcome, notes, and next follow-up.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                    colors = AppFilledTextFieldColors(AppCardContainerColor()),
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(if (isArabic) "ابحث باسم الطبيب أو النتيجة" else "Search doctor or outcome") }
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
                    modifier = Modifier.padding(top = AppSpacing.lg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (visibleVisits.isEmpty()) {
                Text(
                    if (isArabic) "لا توجد زيارات مسجلة بعد. اضغط + لإضافة أول زيارة." else "No visits recorded yet. Tap + to add the first visit.",
                    modifier = Modifier.padding(top = AppSpacing.lg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    items(visibleVisits, key = { it.id }) { visit ->
                        val doctor = doctors.firstOrNull { it.id == visit.doctorId }
                        val expanded = visit.id in expandedVisitIds
                        val expandRotation by androidx.compose.animation.core.animateFloatAsState(
                            if (expanded) 180f else 0f,
                            animationSpec = tween(AppMotion.Standard),
                            label = "visitExpandRotation"
                        )
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small,
                            colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = AppSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(8.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            doctor?.name ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                        Text(
                                            "${visitDateText(visit.visitedAt, "yyyy-MM-dd  HH:mm")} • ${visit.outcome}",
                                            color = MaterialTheme.colorScheme.primary,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    IconButton(
                                        modifier = Modifier.size(32.dp),
                                        onClick = { expandedVisitIds = if (expanded) expandedVisitIds - visit.id else expandedVisitIds + visit.id }
                                    ) {
                                        Icon(
                                            Icons.Default.ExpandMore,
                                            contentDescription = if (isArabic) if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل" else if (expanded) "Hide details" else "Show details",
                                            modifier = Modifier.size(20.dp).rotate(expandRotation)
                                        )
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { editing = visit; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { pendingDelete = visit }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically(tween(AppMotion.Emphasized)) + fadeIn(tween(AppMotion.Standard)),
                                    exit = shrinkVertically(tween(AppMotion.Standard)) + fadeOut(tween(AppMotion.Fast))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 2.dp, bottom = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (visit.notes.isNotBlank()) Text(visit.notes, style = MaterialTheme.typography.bodyMedium)
                                        visit.nextFollowUpAt?.let {
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.EventRepeat, contentDescription = null, modifier = Modifier.size(18.dp))
                                                Text(
                                                    (if (isArabic) "المتابعة: " else "Follow-up: ") + visitDateText(it),
                                                    style = MaterialTheme.typography.bodySmall
                                                )
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
    }

    if (showForm) {
        VisitFormDialog(
            isArabic = isArabic,
            initial = editing,
            doctors = doctors,
            initialDoctorId = formDoctorId,
            initialVisitDate = formVisitDate,
            onDismiss = { showForm = false },
            onSave = { visit ->
                scope.launch {
                    if (visit.id == 0L) {
                        val insertedId = visitDao.insert(visit)
                        FollowUpReminderScheduler.schedule(context, visit.copy(id = insertedId))
                    } else {
                        visitDao.update(visit)
                        FollowUpReminderScheduler.schedule(context, visit)
                    }
                }
                showForm = false
            }
        )
    }

    pendingDelete?.let { visit ->
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الزيارة؟" else "Delete visit?") },
            text = { Text(if (isArabic) "سيتم حذف سجل هذه الزيارة فقط." else "Only this visit record will be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        visitDao.delete(visit)
                        FollowUpReminderScheduler.cancel(context, visit.id)
                    }
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
private fun VisitFormSelectionField(
    label: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(10.dp),
        color = AppSelectionRowColor()
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (value.isBlank()) label else "$label: $value",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VisitFormDialog(
    isArabic: Boolean,
    initial: DoctorVisitEntity?,
    doctors: List<DoctorEntity>,
    initialDoctorId: Long? = null,
    initialVisitDate: String? = null,
    onDismiss: () -> Unit,
    onSave: (DoctorVisitEntity) -> Unit
) {
    var selectedDoctorId by remember(initial, doctors, initialDoctorId) {
        mutableStateOf(initial?.doctorId ?: initialDoctorId ?: doctors.firstOrNull()?.id)
    }
    var date by remember(initial, initialVisitDate) {
        mutableStateOf(initial?.let { visitDateText(it.visitedAt) } ?: initialVisitDate ?: visitDateText(System.currentTimeMillis()))
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
    var doctorSearch by remember { mutableStateOf("") }
    var showVisitDatePicker by remember { mutableStateOf(false) }
    var showVisitTimePicker by remember { mutableStateOf(false) }
    var showFollowUpDatePicker by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Reminder delivery is optional; saving the visit is never blocked. */ }

    AlertDialog(
        containerColor = AppDialogContainerColor(),
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initial == null) {
                    if (isArabic) "تسجيل زيارة جديدة" else "Record a visit"
                } else {
                    if (isArabic) "تعديل الزيارة" else "Edit visit"
                },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VisitFormSelectionField(
                    label = if (isArabic) "الطبيب" else "Doctor",
                    value = doctors.firstOrNull { it.id == selectedDoctorId }?.name
                        ?: "",
                    onClick = { doctorMenu = true }
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
                                if (isArabic) "اختيار الطبيب" else "Choose doctor",
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
                            val normalizedQuery = doctorSearch.trim()
                            val filteredDoctors = doctors.filter { doctor ->
                                normalizedQuery.isBlank() ||
                                    doctor.name.contains(normalizedQuery, ignoreCase = true) ||
                                    doctor.specialty.contains(normalizedQuery, ignoreCase = true) ||
                                    doctor.area.contains(normalizedQuery, ignoreCase = true)
                            }
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredDoctors, key = { it.id }) { doctor ->
                                    Surface(
                                        onClick = {
                                            selectedDoctorId = doctor.id
                                            doctorMenu = false
                                            doctorSearch = ""
                                        },
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        androidx.compose.foundation.layout.Box(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(doctor.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                                if (filteredDoctors.isEmpty()) item {
                                    Text(
                                        if (isArabic) "لا توجد نتائج" else "No results",
                                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
                VisitFormSelectionField(
                    label = if (isArabic) "تاريخ الزيارة" else "Visit date",
                    value = date,
                    onClick = { showVisitDatePicker = true }
                )
                VisitFormSelectionField(
                    label = if (isArabic) "الوقت" else "Time",
                    value = remember(time) {
                        runCatching {
                            val parsedTime = SimpleDateFormat("HH:mm", Locale.US).parse(time)
                            parsedTime?.let {
                                SimpleDateFormat("hh:mm a", Locale.US).format(it)
                            } ?: time
                        }.getOrDefault(time)
                    },
                    onClick = { showVisitTimePicker = true }
                )
                OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    value = outcome,
                    onValueChange = { outcome = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    placeholder = { Text(if (isArabic) "نتيجة الزيارة" else "Visit outcome") }
                )
                OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    singleLine = true,
                    placeholder = { Text(if (isArabic) "ملاحظات الزيارة" else "Visit notes") }
                )
                VisitFormSelectionField(
                    label = if (isArabic) "موعد المتابعة" else "Follow-up date",
                    value = followUpDate,
                    onClick = { showFollowUpDatePicker = true }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(onClick = {
                val visitedAt = parseVisitDate(date, time)
                val followUpAt = if (followUpDate.isBlank()) null else parseVisitDate(followUpDate, "09:00")
                when {
                    selectedDoctorId == null -> error = if (isArabic) "اختر الطبيب." else "Choose a doctor."
                    visitedAt == null -> error = if (isArabic) "تأكد من التاريخ والوقت." else "Check the date and time."
                    outcome.isBlank() -> error = if (isArabic) "اكتب نتيجة الزيارة." else "Enter the visit outcome."
                    followUpDate.isNotBlank() && followUpAt == null -> error = if (isArabic) "تأكد من تاريخ المتابعة." else "Check the follow-up date."
                    followUpAt != null && followUpAt < visitedAt -> error = if (isArabic) "المتابعة لا يمكن أن تسبق الزيارة." else "Follow-up cannot be before the visit."
                    else -> {
                        onSave(
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
                        if (
                            followUpAt != null &&
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            val permissionPreferences = context.getSharedPreferences(
                                "follow_up_notifications",
                                android.content.Context.MODE_PRIVATE
                            )
                            if (!permissionPreferences.getBoolean("permission_requested", false)) {
                                permissionPreferences.edit().putBoolean("permission_requested", true).apply()
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }
                }
                }, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) { Text(if (isArabic) "حفظ" else "Save") }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(10.dp)) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        },
        dismissButton = { }
    )

    if (showVisitDatePicker) {
        val initialDateMillis = runCatching {
            LocalDate.parse(date).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDateMillis
        )
        DatePickerDialog(
            onDismissRequest = { showVisitDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                        date = Instant.ofEpochMilli(selectedDateMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        error = null
                    }
                    showVisitDatePicker = false
                }) { Text(if (isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showVisitDatePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppDialogContainerColor())
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showFollowUpDatePicker) {
        val initialFollowUpMillis = runCatching {
            if (followUpDate.isBlank()) null
            else LocalDate.parse(followUpDate).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        }.getOrNull()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialFollowUpMillis
                ?: LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showFollowUpDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                        followUpDate = Instant.ofEpochMilli(selectedDateMillis)
                            .atZone(ZoneOffset.UTC)
                            .toLocalDate()
                            .toString()
                        error = null
                    }
                    showFollowUpDatePicker = false
                }) { Text(if (isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        followUpDate = ""
                        error = null
                        showFollowUpDatePicker = false
                    }) { Text(if (isArabic) "مسح" else "Clear") }
                    TextButton(onClick = { showFollowUpDatePicker = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            },
            colors = DatePickerDefaults.colors(containerColor = AppDialogContainerColor())
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showVisitTimePicker) {
        val timeParts = time.split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMinute,
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showVisitTimePicker = false },
            containerColor = AppDialogContainerColor(),
            title = { Text(if (isArabic) "اختيار الوقت" else "Choose time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    time = String.format(
                        Locale.US,
                        "%02d:%02d",
                        timePickerState.hour,
                        timePickerState.minute
                    )
                    error = null
                    showVisitTimePicker = false
                }) { Text(if (isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showVisitTimePicker = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
