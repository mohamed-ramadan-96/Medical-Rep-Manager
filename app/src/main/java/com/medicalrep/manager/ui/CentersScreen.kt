package com.medicalrep.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.CenterAvailabilityEntity
import androidx.room.withTransaction
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CentersScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val dao = database.medicalCenterDao()
    val doctorDao = database.doctorDao()
    val centerAvailabilityDao = database.centerAvailabilityDao()
    val centerAvailability by centerAvailabilityDao.observeAll().collectAsState(initial = emptyList())
    val doctorCenterLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    val centers by dao.observeAll().collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var attachmentsCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var directionsCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var expandedCenterIds by remember { mutableStateOf(emptySet<Long>()) }

    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة مركز" else "Add center")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isArabic) "المراكز والعيادات" else "Medical centers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (isArabic) "عدد المراكز: ${centers.size}" else "Centers: ${centers.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (centers.isEmpty()) {
                Text(
                    if (isArabic) "لم تتم إضافة مراكز بعد. اضغط + لإضافة أول مركز." else "No centers yet. Tap + to add your first center.",
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(centers, key = { it.id }) { center ->
                        val expanded = center.id in expandedCenterIds
                        val centerSlots = centerAvailability.filter { it.centerId == center.id }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(center.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        if (center.address.isNotBlank()) {
                                            Text(center.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                        }
                                        if (center.phone.isNotBlank()) {
                                            Text(center.phone, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                        }
                                    }
                                    IconButton(
                                        modifier = Modifier.size(34.dp),
                                        onClick = { expandedCenterIds = if (expanded) expandedCenterIds - center.id else expandedCenterIds + center.id }
                                    ) {
                                        Icon(
                                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = if (isArabic) {
                                                if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل"
                                            } else {
                                                if (expanded) "Hide details" else "Show details"
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    if ((center.latitude != null && center.longitude != null) || !center.mapUrl.isNullOrBlank()) {
                                        IconButton(modifier = Modifier.size(34.dp), onClick = { directionsCenter = center }) {
                                            Icon(Icons.Default.LocationOn, contentDescription = if (isArabic) "الاتجاهات إلى المركز" else "Directions to center", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(modifier = Modifier.size(34.dp), onClick = { attachmentsCenter = center }) {
                                        Icon(Icons.Default.AttachFile, contentDescription = if (isArabic) "المرفقات" else "Attachments", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(34.dp), onClick = { editing = center; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(34.dp), onClick = { pendingDelete = center }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically(animationSpec = tween(220)) + fadeIn(animationSpec = tween(180)),
                                    exit = shrinkVertically(animationSpec = tween(180)) + fadeOut(animationSpec = tween(120))
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 28.dp, top = 4.dp, bottom = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        if (center.notes.isNotBlank()) Text(center.notes, style = MaterialTheme.typography.bodySmall)
                                        Text(
                                            (if (isArabic) "عدد الأطباء غير المسجلين: " else "Unregistered doctors: ") + center.unregisteredDoctorCount,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            (if (isArabic) "أطباء بملفات مسجلة: " else "Registered doctor profiles: ") + doctorCenterLinks.count { it.centerId == center.id },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (centerSlots.isNotEmpty()) {
                                            Text(if (isArabic) "مواعيد عمل المركز" else "Center working hours", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                            centerSlots.groupBy { it.slotGroupId }.values.forEach { group ->
                                                val first = group.first()
                                                val daysText = group.map { slot ->
                                                    mapOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة")[slot.dayOfWeek].orEmpty()
                                                }.joinToString("، ")
                                                Text(daysText + " • " + first.startTime + " – " + first.endTime, style = MaterialTheme.typography.bodySmall)
                                                if (first.notes.isNotBlank()) Text(first.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        CenterFormDialog(
            isArabic = isArabic,
            initial = editing,
            initialAvailability = editing?.let { item -> centerAvailability.filter { it.centerId == item.id } }.orEmpty(),
            onDismiss = { showForm = false },
            onSave = { center, slots ->
                scope.launch {
                    if (center.id == 0L) {
                        database.withTransaction {
                            val centerId = dao.insert(center)
                            slots.forEach { centerAvailabilityDao.insert(it.copy(centerId = centerId)) }
                        }
                    } else {
                        database.withTransaction {
                            dao.update(center)
                            doctorDao.renameLegacyCenter(center.id, center.name)
                            centerAvailability.filter { it.centerId == center.id }.forEach { centerAvailabilityDao.delete(it) }
                            slots.forEach { centerAvailabilityDao.insert(it.copy(centerId = center.id)) }
                        }
                    }
                }
                showForm = false
            }
        )
    }

    attachmentsCenter?.let { center ->
        CenterAttachmentsDialog(center = center, isArabic = isArabic, database = database, onDismiss = { attachmentsCenter = null })
    }

    directionsCenter?.let { center ->
        AlertDialog(
            onDismissRequest = { directionsCenter = null },
            title = { Text(if (isArabic) "فتح الاتجاهات؟" else "Open directions?") },
            text = {
                Text(
                    if (isArabic) "هل تريد إظهار الاتجاهات إلى ${center.name} على Google Maps؟"
                    else "Would you like to show directions to ${center.name} in Google Maps?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val latitude = center.latitude
                    val longitude = center.longitude
                    directionsCenter = null
                    openSavedMapLocation(context, latitude, longitude, center.mapUrl)
                }) { Text(if (isArabic) "نعم" else "Yes") }
            },
            dismissButton = {
                TextButton(onClick = { directionsCenter = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    pendingDelete?.let { center ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف المركز؟" else "Delete center?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف المركز من القائمة وفصل الأطباء المرتبطين به، دون حذف بياناتهم." else "This removes the center and unlinks its doctors. Doctor records will not be deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val attachments = database.centerAttachmentDao().getForCenter(center.id)
                        withContext(Dispatchers.IO) { attachments.forEach { File(it.localPath).delete() } }
                        database.withTransaction {
                            doctorDao.detachCenter(center.id)
                            dao.delete(center)
                        }
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

private data class CenterWorkingHoursDraft(
    val groupId: String = java.util.UUID.randomUUID().toString(),
    val days: Set<Int> = emptySet(),
    val startTime: String = "09:00",
    val endTime: String = "22:00",
    val notes: String = "",
    val existing: List<CenterAvailabilityEntity> = emptyList()
)

@Composable
private fun CenterFormDialog(
    isArabic: Boolean,
    initial: MedicalCenterEntity?,
    initialAvailability: List<CenterAvailabilityEntity>,
    onDismiss: () -> Unit,
    onSave: (MedicalCenterEntity, List<CenterAvailabilityEntity>) -> Unit
) {
    val context = LocalContext.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var workSlots by remember(initial, initialAvailability) {
        mutableStateOf(
            initialAvailability.groupBy { it.slotGroupId.ifBlank { "legacy-" + it.id } }.values.map { group ->
                val first = group.first()
                CenterWorkingHoursDraft(
                    groupId = first.slotGroupId.ifBlank { java.util.UUID.randomUUID().toString() },
                    days = group.map { it.dayOfWeek }.toSet(),
                    startTime = first.startTime,
                    endTime = first.endTime,
                    notes = first.notes,
                    existing = group
                )
            }
        )
    }
    var unregisteredDoctorCount by remember(initial) { mutableStateOf(initial?.unregisteredDoctorCount?.toString() ?: "0") }
    var latitude by remember(initial) { mutableStateOf(initial?.latitude) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitude) }
    var mapUrl by remember(initial) { mutableStateOf(initial?.mapUrl) }
    var showMapPicker by remember { mutableStateOf(false) }
    var editCenterLocation by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (isArabic) "إضافة مركز أو عيادة" else "Add center or clinic" } else { if (isArabic) "تعديل المركز" else "Edit center" }) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "اسم المركز *" else "Center name *") },
                        isError = nameError
                    )
                }
                item {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isArabic) "العنوان" else "Address") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone") }
                    )
                }
                item {
                    OutlinedTextField(value = unregisteredDoctorCount, onValueChange = { value -> unregisteredDoctorCount = value.filter { it.isDigit() } }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (isArabic) "عدد الدكاترة غير المسجلين" else "Unregistered doctors count") })
                }

                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(if (isArabic) "مواعيد عمل المركز" else "Center working hours", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Text(if (isArabic) "المواعيد دي تخص المركز نفسه، ومش محتاجة ملفات أطباء. تقدر تضيف أكتر من فترة." else "These hours belong to the center itself; no doctor profiles are required. Add multiple time slots if needed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        workSlots.forEachIndexed { index, slot ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text((if (isArabic) "فترة عمل " else "Working slot ") + (index + 1), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { workSlots = workSlots.filterIndexed { i, _ -> i != index } }) { Text(if (isArabic) "حذف" else "Remove") }
                                    }
                                    listOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة").chunked(2).forEach { pair ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            pair.forEach { (day, label) ->
                                                Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                    Checkbox(checked = day in slot.days, onCheckedChange = { checked ->
                                                        val days = if (checked) slot.days + day else slot.days - day
                                                        workSlots = workSlots.toMutableList().also { it[index] = slot.copy(days = days) }
                                                    })
                                                    Text(if (isArabic) label else listOf("Saturday","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday")[listOf(6,7,1,2,3,4,5).indexOf(day)], style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = slot.startTime, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(startTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(if (isArabic) "من HH:mm" else "From HH:mm") })
                                        OutlinedTextField(value = slot.endTime, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(endTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(if (isArabic) "إلى HH:mm" else "To HH:mm") })
                                    }
                                    OutlinedTextField(value = slot.notes, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(notes = value) } }, modifier = Modifier.fillMaxWidth(), label = { Text(if (isArabic) "ملاحظات الفترة" else "Slot notes") })
                                }
                            }
                        }
                        OutlinedButton(onClick = { workSlots = workSlots + CenterWorkingHoursDraft() }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (isArabic) "+ إضافة فترة عمل" else "+ Add working-hours slot")
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
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isArabic) "موقع المركز على الخريطة" else "Center map location", style = MaterialTheme.typography.labelLarge)
                        if (latitude != null && longitude != null) {
                            CenterMapPreview(
                                latitude = latitude,
                                longitude = longitude,
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                            )
                            Text(
                                String.format(java.util.Locale.US, "%.6f, %.6f", latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) }
                            ) {
                                Text(if (isArabic) "عرض المكان على الخريطة" else "View place on map")
                            }
                        } else if (!mapUrl.isNullOrBlank()) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) }
                            ) {
                                Text(if (isArabic) "عرض المكان على الخريطة" else "View place on map")
                            }
                        } else {
                            Text(
                                if (isArabic) "لم يتم تحديد موقع لهذا المركز بعد." else "No map location has been set for this center.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val hasCenterLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (hasCenterLocation) {
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        latitude = null
                                        longitude = null
                                        mapUrl = null
                                        editCenterLocation = false
                                    }
                                ) {
                                    Text(if (isArabic) "مسح الموقع" else "Clear location")
                                }
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { editCenterLocation = !editCenterLocation }
                                ) {
                                    Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                }
                            }
                            AnimatedLocationActions(visible = !hasCenterLocation || editCenterLocation) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CurrentLocationButton(isArabic = isArabic) { lat, lon ->
                                        latitude = lat
                                        longitude = lon
                                        mapUrl = null
                                        editCenterLocation = false
                                    }
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { showMapPicker = true }
                                    ) {
                                        Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) nameError = true else onSave(
                    MedicalCenterEntity(
                        id = initial?.id ?: 0L,
                        name = name.trim(),
                        address = address.trim(),
                        phone = phone.trim(),
                        notes = notes.trim(),
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        latitude = latitude,
                        longitude = longitude,
                        mapUrl = mapUrl,

                        unregisteredDoctorCount = unregisteredDoctorCount.toIntOrNull() ?: 0
                    ),
                    workSlots.flatMap { slot ->
                        slot.days.map { day ->
                            val old = slot.existing.firstOrNull { it.dayOfWeek == day }
                            CenterAvailabilityEntity(
                                id = old?.id ?: 0L,
                                centerId = initial?.id ?: 0L,
                                dayOfWeek = day,
                                startTime = slot.startTime,
                                endTime = slot.endTime,
                                notes = slot.notes.trim(),
                                createdAt = old?.createdAt ?: System.currentTimeMillis(),
                                slotGroupId = slot.groupId
                            )
                        }
                    }
                )
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )

    if (showMapPicker) {
        CenterMapPickerDialog(
            isArabic = isArabic,
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialMapUrl = mapUrl,
            onDismiss = { showMapPicker = false },
            onConfirm = { lat, lon, url ->
                latitude = lat
                longitude = lon
                mapUrl = url
                showMapPicker = false
            }
        )
    }
}
