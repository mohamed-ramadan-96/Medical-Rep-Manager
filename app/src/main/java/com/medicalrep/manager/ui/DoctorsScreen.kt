package com.medicalrep.manager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.medicalrep.manager.data.DoctorAvailabilityEntity
import com.medicalrep.manager.data.DoctorCenterLinkEntity
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.data.SpecialtyEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DoctorsScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val dao = database.doctorDao()
    val centerDao = database.medicalCenterDao()
    val specialtyDao = database.specialtyDao()
    val availability by database.doctorAvailabilityDao().observeAll().collectAsState(initial = emptyList())
    val doctorCenterLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val visits by database.doctorVisitDao().observeAll().collectAsState(initial = emptyList())
    val specialties by specialtyDao.observeAll().collectAsState(initial = emptyList())
    val centers by centerDao.observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var searchText by remember { mutableStateOf("") }
    val doctors by remember(searchText) {
        if (searchText.isBlank()) dao.observeAll() else dao.search(searchText.trim())
    }.collectAsState(initial = emptyList())
    var editingDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoctorEntity?>(null) }
    var attachmentsDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var detailsDoctor by remember { mutableStateOf<DoctorEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editingDoctor = null
                showForm = true
            }) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة طبيب" else "Add doctor", modifier = Modifier.size(20.dp))
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isArabic) "إدارة الأطباء" else "Doctors",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(if (isArabic) "ابحث بالاسم أو التخصص أو الهاتف" else "Search name, specialty or phone") }
            )
            Text(
                if (isArabic) "عدد الأطباء: ${doctors.size}" else "Doctors: ${doctors.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (doctors.isEmpty()) {
                Text(
                    if (isArabic) "لا يوجد أطباء حتى الآن. اضغط + لإضافة أول طبيب." else "No doctors yet. Tap + to add your first doctor.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 24.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(doctors, key = { it.id }) { doctor ->
                        DoctorCard(
                            doctor = doctor,
                            centerName = centers.filter { center -> doctorCenterLinks.any { it.doctorId == doctor.id && it.centerId == center.id } }.joinToString("، ") { it.name }.ifBlank { centers.firstOrNull { it.id == doctor.centerId }?.name ?: doctor.medicalCenter },
                            isArabic = isArabic,
                            onEdit = {
                                editingDoctor = doctor
                                showForm = true
                            },
                            onDelete = { pendingDelete = doctor },
                            onAttachments = { attachmentsDoctor = doctor },
                            onOpen = { detailsDoctor = doctor }
                        )
                    }
                }
            }
        }
    }

    if (showForm) {
        DoctorFormDialog(
            isArabic = isArabic,
            initial = editingDoctor,
            centers = centers,
            specialties = specialties,
            initialCenterIds = editingDoctor?.let { doctor -> doctorCenterLinks.filter { it.doctorId == doctor.id }.map { it.centerId }.toSet() }.orEmpty(),
            onDismiss = { showForm = false },
            onSave = { doctor, selectedCenterIds ->
                scope.launch {
                    val doctorId = if (doctor.id == 0L) dao.insert(doctor) else { dao.update(doctor); doctor.id }
                    database.doctorCenterLinkDao().deleteForDoctor(doctorId)
                    if (selectedCenterIds.isNotEmpty()) database.doctorCenterLinkDao().insertAll(selectedCenterIds.map { DoctorCenterLinkEntity(doctorId, it) })
                }
                showForm = false
            }
        )
    }

    detailsDoctor?.let { doctor ->
        DoctorDetailsDialog(
            doctor = doctor,
            centers = centers.filter { center -> doctorCenterLinks.any { it.doctorId == doctor.id && it.centerId == center.id } || (doctorCenterLinks.none { it.doctorId == doctor.id } && center.id == doctor.centerId) },
            availability = availability.filter { it.doctorId == doctor.id },
            visits = visits.filter { it.doctorId == doctor.id },
            isArabic = isArabic,
            onDismiss = { detailsDoctor = null }
        )
    }

    attachmentsDoctor?.let { doctor ->
        DoctorAttachmentsDialog(
            doctor = doctor,
            isArabic = isArabic,
            database = database,
            onDismiss = { attachmentsDoctor = null }
        )
    }

    pendingDelete?.let { doctor ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الطبيب؟" else "Delete doctor?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف بيانات ${doctor.name} من هذا الجهاز." else "${doctor.name}'s data will be deleted from this device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val attachments = database.doctorAttachmentDao().getForDoctor(doctor.id)
                        withContext(Dispatchers.IO) {
                            attachments.forEach { File(it.localPath).delete() }
                        }
                        dao.delete(doctor)
                    }
                    pendingDelete = null
                }) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun DoctorCard(
    doctor: DoctorEntity,
    centerName: String,
    isArabic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachments: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    if (doctor.photoUri.isNullOrBlank()) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                    } else {
                        AsyncImage(
                            model = doctor.photoUri,
                            contentDescription = if (isArabic) "صورة الطبيب" else "Doctor photo",
                            modifier = Modifier.size(56.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(doctor.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (doctor.specialty.isNotBlank()) {
                            Text(doctor.specialty, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                if (doctor.phoneNumbers.isNotBlank()) {
                    Text(
                        (if (isArabic) "الهاتف: " else "Phone: ") + doctor.phoneNumbers.replace("\\n", " • "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (centerName.isNotBlank()) {
                    Text(
                        (if (isArabic) "المركز: " else "Center: ") + centerName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (doctor.notes.isNotBlank()) {
                    Text(doctor.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(modifier = Modifier.size(40.dp), onClick = onAttachments) {
                    Icon(Icons.Default.AttachFile, contentDescription = if (isArabic) "المرفقات" else "Attachments", modifier = Modifier.size(20.dp))
                }
                IconButton(modifier = Modifier.size(40.dp), onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(20.dp))
                }
                IconButton(modifier = Modifier.size(40.dp), onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
private fun DoctorDetailsDialog(
    doctor: DoctorEntity,
    centers: List<MedicalCenterEntity>,
    availability: List<DoctorAvailabilityEntity>,
    visits: List<DoctorVisitEntity>,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val days = listOf(
        1 to (if (isArabic) "الاثنين" else "Monday"),
        2 to (if (isArabic) "الثلاثاء" else "Tuesday"),
        3 to (if (isArabic) "الأربعاء" else "Wednesday"),
        4 to (if (isArabic) "الخميس" else "Thursday"),
        5 to (if (isArabic) "الجمعة" else "Friday"),
        6 to (if (isArabic) "السبت" else "Saturday"),
        7 to (if (isArabic) "الأحد" else "Sunday")
    )
    val upcomingFollowUps = visits.filter { (it.nextFollowUpAt ?: 0L) >= System.currentTimeMillis() }
        .sortedBy { it.nextFollowUpAt }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(doctor.name, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                if (doctor.specialty.isNotBlank()) item {
                    Text((if (isArabic) "التخصص: " else "Specialty: ") + doctor.specialty)
                }
                if (doctor.phoneNumbers.isNotBlank()) item {
                    Text((if (isArabic) "الهاتف: " else "Phone: ") + doctor.phoneNumbers.replace("\n", " • "))
                }
                item {
                    Text(if (isArabic) "المراكز ومواعيد التواجد" else "Centers and availability", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (centers.isEmpty()) {
                    item { Text(if (isArabic) "لم يتم ربط الطبيب بأي مركز." else "No centers linked to this doctor.") }
                } else {
                    items(centers, key = { it.id }) { center ->
                        val centerSlots = availability.filter { it.centerId == center.id || (it.centerId == null && center.id == doctor.centerId) }
                        val centerGroups = centerSlots.groupBy { it.slotGroupId }.values.toList()
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(center.name, fontWeight = FontWeight.SemiBold)
                            if (center.address.isNotBlank()) Text((if (isArabic) "العنوان: " else "Address: ") + center.address)
                            if (center.phone.isNotBlank()) Text((if (isArabic) "الهاتف: " else "Phone: ") + center.phone)
                            if (centerGroups.isEmpty()) {
                                Text(if (isArabic) "لم تُحدد مواعيد لهذا المركز." else "No hours set for this center.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else {
                                centerGroups.forEach { group ->
                                    val first = group.first()
                                    val dayNames = group.map { slot -> days.firstOrNull { it.first == slot.dayOfWeek }?.second.orEmpty() }.joinToString("، ")
                                    Text(dayNames + " • " + first.startTime + " – " + first.endTime)
                                    if (first.notes.isNotBlank()) Text(first.notes, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
                if (doctor.notes.isNotBlank()) item {
                    Text((if (isArabic) "ملاحظات: " else "Notes: ") + doctor.notes)
                }
                item {
                    Text(if (isArabic) "المواعيد المحددة للمتابعة" else "Scheduled follow-ups", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                if (upcomingFollowUps.isEmpty()) {
                    item { Text(if (isArabic) "لا توجد متابعة قادمة مسجلة." else "No upcoming follow-up recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(upcomingFollowUps) { visit ->
                        val followUp = visit.nextFollowUpAt
                        if (followUp != null) {
                            Text(
                                (if (isArabic) "موعد المتابعة: " else "Follow-up: ") +
                                    dateFormat.format(Date(followUp)) +
                                    (if (visit.notes.isNotBlank()) " • ${visit.notes}" else "")
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إغلاق" else "Close") }
        }
    )
}

@Composable
private fun DoctorFormDialog(
    isArabic: Boolean,
    initial: DoctorEntity?,
    centers: List<MedicalCenterEntity>,
    specialties: List<SpecialtyEntity>,
    initialCenterIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (DoctorEntity, Set<Long>) -> Unit
) {
    val context = LocalContext.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var selectedSpecialtyId by remember(initial, specialties) { mutableStateOf(initial?.specialtyId ?: specialties.firstOrNull { it.name == initial?.specialty }?.id) }
    var showSpecialtyPicker by remember { mutableStateOf(false) }
    var phones by remember(initial) { mutableStateOf(initial?.phoneNumbers.orEmpty()) }
    var selectedCenterIds by remember(initial, initialCenterIds, centers) { mutableStateOf(initialCenterIds.ifEmpty { setOfNotNull(initial?.centerId ?: centers.firstOrNull { it.name == initial?.medicalCenter }?.id) }) }
    var showCenterPicker by remember { mutableStateOf(false) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var photoUri by remember(initial) { mutableStateOf(initial?.photoUri) }
    var nameError by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
                // Some picker providers grant temporary access only; keep the returned URI.
            } catch (_: IllegalArgumentException) {
                // Provider does not support persistable grants.
            }
            photoUri = uri.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (isArabic) "إضافة طبيب" else "Add doctor" } else { if (isArabic) "تعديل بيانات الطبيب" else "Edit doctor" }) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (photoUri.isNullOrBlank()) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(72.dp), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = if (isArabic) "صورة الطبيب" else "Doctor photo",
                                modifier = Modifier.size(88.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Text(if (isArabic) "اختيار صورة" else "Choose photo")
                            }
                            if (!photoUri.isNullOrBlank()) {
                                TextButton(onClick = { photoUri = null }) {
                                    Text(if (isArabic) "إزالة الصورة" else "Remove photo")
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "اسم الطبيب *" else "Doctor name *") },
                        isError = nameError
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (isArabic) "التخصص" else "Specialty", style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { showSpecialtyPicker = true }) {
                            Text(specialties.firstOrNull { it.id == selectedSpecialtyId }?.name ?: if (isArabic) "بدون تخصص — اختر من القائمة" else "No specialty — choose one")
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = phones, onValueChange = { phones = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        label = { Text(if (isArabic) "أرقام الهاتف (كل رقم في سطر)" else "Phone numbers (one per line)") }
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (isArabic) "المركز أو العيادة" else "Medical center / clinic", style = MaterialTheme.typography.labelLarge)
                        TextButton(onClick = { showCenterPicker = true }) {
                            Text(if (selectedCenterIds.isEmpty()) { if (isArabic) "اختر المراكز" else "Choose centers" } else centers.filter { it.id in selectedCenterIds }.joinToString("، ") { it.name })
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        label = { Text(if (isArabic) "ملاحظات" else "Notes") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else {
                    onSave(
                        DoctorEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            specialty = specialties.firstOrNull { it.id == selectedSpecialtyId }?.name.orEmpty(),
                            specialtyId = selectedSpecialtyId,
                            phoneNumbers = phones.trim(),
                            medicalCenter = centers.firstOrNull { it.id == selectedCenterIds.firstOrNull() }?.name.orEmpty(),
                            centerId = selectedCenterIds.firstOrNull(),
                            notes = notes.trim(),
                            photoUri = photoUri,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis()
                        ),
                        selectedCenterIds
                    )
                }
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )

    if (showSpecialtyPicker) {
        AlertDialog(
            onDismissRequest = { showSpecialtyPicker = false },
            title = { Text(if (isArabic) "اختر التخصص" else "Choose a specialty") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        TextButton(onClick = { selectedSpecialtyId = null; showSpecialtyPicker = false }) {
                            Text(if (isArabic) "بدون تخصص" else "No specialty")
                        }
                    }
                    items(specialties, key = { it.id }) { specialty ->
                        TextButton(onClick = { selectedSpecialtyId = specialty.id; showSpecialtyPicker = false }) {
                            Text(specialty.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpecialtyPicker = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }

    if (showCenterPicker) {
        AlertDialog(
            onDismissRequest = { showCenterPicker = false },
            title = { Text(if (isArabic) "اختر مراكز عمل الطبيب" else "Select doctor's centers") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    item {
                        TextButton(onClick = { selectedCenterIds = emptySet() }) {
                            Text(if (isArabic) "مسح الاختيار" else "Clear selection")
                        }
                    }
                    items(centers, key = { it.id }) { center ->
                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            Checkbox(checked = center.id in selectedCenterIds, onCheckedChange = { checked -> selectedCenterIds = if (checked) selectedCenterIds + center.id else selectedCenterIds - center.id })
                            Text(center.name)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCenterPicker = false }) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        )
    }
}
