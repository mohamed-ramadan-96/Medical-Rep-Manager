package com.medicalrep.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import java.util.UUID

@Composable
private fun FilterActionButton(isArabic: Boolean, expanded: Boolean, onClick: () -> Unit) {
    val iconColor = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.size(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(36.dp)) {
                val stroke = 2.6.dp.toPx()
                val left = 3.dp.toPx()
                val right = size.width - 3.dp.toPx()
                val railYs = listOf(9.dp.toPx(), 18.dp.toPx(), 27.dp.toPx())
                val knobXs = listOf(22.dp.toPx(), 11.dp.toPx(), 25.dp.toPx())
                railYs.forEachIndexed { index, y ->
                    val knobX = knobXs[index]
                    val gap = 3.4.dp.toPx()
                    drawLine(iconColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(knobX - gap, y), stroke, cap = StrokeCap.Round)
                    drawLine(iconColor, androidx.compose.ui.geometry.Offset(knobX + gap, y), androidx.compose.ui.geometry.Offset(right, y), stroke, cap = StrokeCap.Round)
                    drawLine(
                        iconColor,
                        androidx.compose.ui.geometry.Offset(knobX, y - 4.2.dp.toPx()),
                        androidx.compose.ui.geometry.Offset(knobX, y + 4.2.dp.toPx()),
                        stroke,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

@Composable
private fun DoctorSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    isArabic: Boolean,
    compact: Boolean
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().height(60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                if (compact) { if (isArabic) "ابحث عن طبيب..." else "Search doctors..." }
                                else { if (isArabic) "ابحث عن طبيب..." else "Search doctors..." },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        innerTextField()
                    }
                }
            )
        }
    }
}

@Composable
fun DoctorsScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onManageSpecialties: () -> Unit = {}) {
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
    var selectedArea by remember { mutableStateOf("all") }
    var selectedSpecialty by remember { mutableStateOf("all") }
    var selectedCenter by remember { mutableStateOf("all") }
    var areaMenu by remember { mutableStateOf(false) }
    var specialtyMenu by remember { mutableStateOf(false) }
    var centerMenu by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    val doctors by remember(searchText) {
        if (searchText.isBlank()) dao.observeAll() else dao.search(searchText.trim())
    }.collectAsState(initial = emptyList())
    val availableAreas = doctors.map { it.area.trim() }.filter { it.isNotBlank() }.distinct().sorted()
    val filteredDoctors = doctors.filter { doctor ->
        (selectedArea == "all" || doctor.area.trim().equals(selectedArea, ignoreCase = true)) &&
        (selectedSpecialty == "all" || doctor.specialty.equals(selectedSpecialty, ignoreCase = true)) &&
        (selectedCenter == "all" || doctorCenterLinks.any { it.doctorId == doctor.id && it.centerId.toString() == selectedCenter } ||
            (doctorCenterLinks.none { it.doctorId == doctor.id } && doctor.centerId?.toString() == selectedCenter))
    }
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
        val listState = rememberLazyListState()
        var searchHeaderExpanded by remember { mutableStateOf(true) }
        val scrollDirectionThreshold = with(androidx.compose.ui.platform.LocalDensity.current) { 8.dp.roundToPx() }
        LaunchedEffect(listState) {
            var previousPosition = 0
            var accumulatedDelta = 0
            snapshotFlow { listState.firstVisibleItemIndex * 100000 + listState.firstVisibleItemScrollOffset }
                .distinctUntilChanged()
                .collect { position ->
                    val delta = position - previousPosition
                    if (previousPosition != 0 || position != 0) {
                        accumulatedDelta += delta
                        if (accumulatedDelta >= scrollDirectionThreshold) {
                            searchHeaderExpanded = false
                            accumulatedDelta = 0
                        } else if (accumulatedDelta <= -scrollDirectionThreshold) {
                            searchHeaderExpanded = true
                            accumulatedDelta = 0
                        }
                    }
                    if (position == 0) {
                        searchHeaderExpanded = true
                        accumulatedDelta = 0
                    }
                    previousPosition = position
                }
        }
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            AnimatedVisibility(
                visible = listState.firstVisibleItemIndex > 0,
                enter = fadeIn(tween(140)),
                exit = fadeOut(tween(100))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    AnimatedContent(
                        targetState = searchHeaderExpanded,
                        transitionSpec = {
                            (fadeIn(tween(170)) + scaleIn(initialScale = 0.94f, animationSpec = tween(170))) togetherWith
                                (fadeOut(tween(110)) + scaleOut(targetScale = 0.96f, animationSpec = tween(110)))
                        },
                        label = "doctorSearchCollapse"
                    ) { expanded ->
                        if (expanded) {
                            Row(
                                Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(Modifier.weight(1f)) {
                                    DoctorSearchField(searchText, { searchText = it }, isArabic, compact = true)
                                }
                                FilterActionButton(isArabic = isArabic, expanded = showFilters, onClick = {
                                    scope.launch {
                                        listState.animateScrollToItem(0)
                                        showFilters = !showFilters
                                    }
                                })
                            }
                        } else {
                            Surface(
                                onClick = { searchHeaderExpanded = true },
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Search, contentDescription = if (isArabic) "بحث" else "Search", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "إدارة الأطباء" else "Doctors", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        TextButton(onClick = onManageSpecialties) { Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp)); Text(if (isArabic) "التخصصات" else "Specialties") }
                    }
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(Modifier.weight(1f)) { DoctorSearchField(searchText, { searchText = it }, isArabic, compact = false) }
                        FilterActionButton(isArabic = isArabic, expanded = showFilters, onClick = { showFilters = !showFilters })
                    }
                    Text(if (isArabic) "${filteredDoctors.size} من ${doctors.size} طبيب" else "${filteredDoctors.size} of ${doctors.size} doctors", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
                    AnimatedVisibility(visible = showFilters, enter = expandVertically(tween(220)) + fadeIn(tween(180)), exit = shrinkVertically(tween(180)) + fadeOut(tween(140))) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    OutlinedSelectionField(label = if (isArabic) "المنطقة" else "Area", value = if (selectedArea == "all") { if (isArabic) "كل المناطق" else "All areas" } else selectedArea, onClick = { areaMenu = true })
                                    DropdownMenu(expanded = areaMenu, onDismissRequest = { areaMenu = false }) {
                                        DropdownMenuItem(text = { Text(if (isArabic) "كل المناطق" else "All areas") }, onClick = { selectedArea = "all"; areaMenu = false })
                                        availableAreas.forEach { area -> DropdownMenuItem(text = { Text(area) }, onClick = { selectedArea = area; areaMenu = false }) }
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    OutlinedSelectionField(label = if (isArabic) "التخصص" else "Specialty", value = if (selectedSpecialty == "all") { if (isArabic) "كل التخصصات" else "All specialties" } else selectedSpecialty, onClick = { specialtyMenu = true })
                                    DropdownMenu(expanded = specialtyMenu, onDismissRequest = { specialtyMenu = false }) {
                                        DropdownMenuItem(text = { Text(if (isArabic) "كل التخصصات" else "All specialties") }, onClick = { selectedSpecialty = "all"; specialtyMenu = false })
                                        specialties.map { it.name }.plus(doctors.map { it.specialty }).filter { it.isNotBlank() }.distinct().sorted().forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { selectedSpecialty = name; specialtyMenu = false }) }
                                    }
                                }
                            }
                            OutlinedSelectionField(label = if (isArabic) "المركز" else "Center", value = if (selectedCenter == "all") { if (isArabic) "كل المراكز" else "All centers" } else centers.firstOrNull { it.id.toString() == selectedCenter }?.name.orEmpty(), onClick = { centerMenu = true })
                            DropdownMenu(expanded = centerMenu, onDismissRequest = { centerMenu = false }) {
                                DropdownMenuItem(text = { Text(if (isArabic) "كل المراكز" else "All centers") }, onClick = { selectedCenter = "all"; centerMenu = false })
                                centers.forEach { center -> DropdownMenuItem(text = { Text(center.name) }, onClick = { selectedCenter = center.id.toString(); centerMenu = false }) }
                            }
                        }
                    }
                }
                if (filteredDoctors.isEmpty()) item { Text(if (doctors.isEmpty()) { if (isArabic) "لا يوجد أطباء حتى الآن. اضغط + لإضافة أول طبيب." else "No doctors yet. Tap + to add your first doctor." } else { if (isArabic) "لا توجد نتائج مطابقة للفلاتر المحددة." else "No doctors match the selected filters." }, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)) }
                else items(filteredDoctors, key = { it.id }) { doctor ->
                    DoctorCard(doctor = doctor, centerName = centers.filter { center -> doctorCenterLinks.any { it.doctorId == doctor.id && it.centerId == center.id } }.joinToString("، ") { it.name }.ifBlank { centers.firstOrNull { it.id == doctor.centerId }?.name ?: doctor.medicalCenter }, isArabic = isArabic, onEdit = { editingDoctor = doctor; showForm = true }, onDelete = { pendingDelete = doctor }, onAttachments = { attachmentsDoctor = doctor }, onOpen = { detailsDoctor = doctor })
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
            initialAvailability = editingDoctor?.let { doctor -> availability.filter { it.doctorId == doctor.id } }.orEmpty(),
            onDismiss = { showForm = false },
            onSave = { doctor, selectedCenterIds, updatedAvailability ->
                scope.launch {
                    val doctorId = if (doctor.id == 0L) dao.insert(doctor) else { dao.update(doctor); doctor.id }
                    database.doctorCenterLinkDao().deleteForDoctor(doctorId)
                    if (selectedCenterIds.isNotEmpty()) database.doctorCenterLinkDao().insertAll(selectedCenterIds.map { DoctorCenterLinkEntity(doctorId, it) })
                    availability.filter { it.doctorId == doctorId }.forEach { database.doctorAvailabilityDao().delete(it) }
                    updatedAvailability.forEach { slot -> database.doctorAvailabilityDao().insert(slot.copy(doctorId = doctorId)) }
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
            onDismiss = { detailsDoctor = null },
            onUpdateCenterLocation = { center, latitude, longitude, mapUrl ->
                scope.launch { centerDao.update(center.copy(latitude = latitude, longitude = longitude, mapUrl = mapUrl)) }
            },
            onUpdateDoctorLocation = { updatedDoctor ->
                scope.launch { dao.update(updatedDoctor) }
            }
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
        shape = RoundedCornerShape(12.dp),
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
                        if (doctor.area.isNotBlank()) {
                            Text((if (isArabic) "المنطقة: " else "Area: ") + doctor.area, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    onDismiss: () -> Unit,
    onUpdateCenterLocation: (MedicalCenterEntity, Double?, Double?, String?) -> Unit,
    onUpdateDoctorLocation: (DoctorEntity) -> Unit
) {
    val context = LocalContext.current
    var mapPickerCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var directionsCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var mapPickerDoctor by remember { mutableStateOf(false) }
    var directionsDoctor by remember { mutableStateOf(false) }
    var doctorLatitude by remember(doctor) { mutableStateOf(doctor.latitude) }
    var doctorLongitude by remember(doctor) { mutableStateOf(doctor.longitude) }
    var doctorMapUrl by remember(doctor) { mutableStateOf(doctor.mapUrl) }
    var showDoctorLocationActions by remember { mutableStateOf(false) }
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
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (doctor.specialty.isNotBlank()) {
                                Text(if (isArabic) "التخصص" else "Specialty", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(doctor.specialty, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (doctor.phoneNumbers.isNotBlank()) {
                                Text(if (isArabic) "أرقام الهاتف" else "Phone numbers", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                Text(doctor.phoneNumbers.replace("\\n", " • "), style = MaterialTheme.typography.bodyLarge)
                            }
                            if (doctor.specialty.isBlank() && doctor.phoneNumbers.isBlank()) {
                                Text(if (isArabic) "لا توجد بيانات تواصل مضافة." else "No contact information added.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(if (isArabic) "موقع الطبيب" else "Doctor location", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            val hasDoctorLocation = (doctorLatitude != null && doctorLongitude != null) || !doctorMapUrl.isNullOrBlank()
                            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { showDoctorLocationActions = !showDoctorLocationActions }
                                ) {
                                    Text(if (isArabic) "إدارة الموقع" else "Manage location")
                                    Icon(
                                        if (showDoctorLocationActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                                AnimatedLocationActions(visible = showDoctorLocationActions) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (hasDoctorLocation) {
                                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                                doctorLatitude = null
                                                doctorLongitude = null
                                                doctorMapUrl = null
                                                onUpdateDoctorLocation(doctor.copy(latitude = null, longitude = null, mapUrl = null))
                                                showDoctorLocationActions = false
                                            }) { Text(if (isArabic) "مسح الموقع" else "Remove location") }
                                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { mapPickerDoctor = true }) {
                                                Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                            }
                                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { directionsDoctor = true }) {
                                                Text(if (isArabic) "فتح الاتجاهات على الخريطة" else "Open directions in Maps")
                                            }
                                        } else {
                                            CurrentLocationButton(isArabic = isArabic) { lat, lon ->
                                                doctorLatitude = lat
                                                doctorLongitude = lon
                                                doctorMapUrl = null
                                                onUpdateDoctorLocation(doctor.copy(latitude = lat, longitude = lon, mapUrl = null))
                                                showDoctorLocationActions = false
                                            }
                                            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { mapPickerDoctor = true }) {
                                                Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                            }
                                        }
                                    }
                                }
                            }
                            if (doctorLatitude != null && doctorLongitude != null) {
                                Text(String.format(Locale.US, "%.6f, %.6f", doctorLatitude, doctorLongitude), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            } else if (!doctorMapUrl.isNullOrBlank()) {
                                Text(if (isArabic) "تم حفظ رابط موقع الطبيب." else "Doctor's map link is saved.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Text(
                        if (isArabic) "المراكز ومواعيد التواجد" else "Centers and availability",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (centers.isEmpty()) {
                    item { Text(if (isArabic) "لم يتم ربط الطبيب بأي مركز." else "No centers linked to this doctor.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                } else {
                    items(centers, key = { it.id }) { center ->
                        val centerSlots = availability.filter { it.centerId == center.id || (it.centerId == null && center.id == doctor.centerId) }
                        val centerGroups = centerSlots.groupBy { it.slotGroupId }.values.toList()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(center.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (center.address.isNotBlank()) {
                                    Text(if (isArabic) "العنوان" else "Address", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(center.address)
                                }
                                if (center.phone.isNotBlank()) {
                                    Text(if (isArabic) "الهاتف" else "Phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(center.phone)
                                }
                                val hasCenterLocation = (center.latitude != null && center.longitude != null) || !center.mapUrl.isNullOrBlank()
                                var showCenterLocationActions by remember(center.id, center.latitude, center.longitude, center.mapUrl) { mutableStateOf(false) }
                                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = { showCenterLocationActions = !showCenterLocationActions }
                                    ) {
                                        Text(if (isArabic) "إدارة موقع المركز" else "Manage center location")
                                        Icon(
                                            if (showCenterLocationActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            modifier = Modifier.padding(start = 8.dp)
                                        )
                                    }
                                    AnimatedLocationActions(visible = showCenterLocationActions) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (hasCenterLocation) {
                                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                                    onUpdateCenterLocation(center, null, null, null)
                                                    showCenterLocationActions = false
                                                }) { Text(if (isArabic) "مسح الموقع" else "Remove location") }
                                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { mapPickerCenter = center }) {
                                                    Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                                }
                                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { directionsCenter = center }) {
                                                    Text(if (isArabic) "فتح الاتجاهات على الخريطة" else "Open directions in Maps")
                                                }
                                            } else {
                                                CurrentLocationButton(isArabic = isArabic) { lat, lon ->
                                                    onUpdateCenterLocation(center, lat, lon, null)
                                                    showCenterLocationActions = false
                                                }
                                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { mapPickerCenter = center }) {
                                                    Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                                }
                                            }
                                        }
                                    }
                                }
                                androidx.compose.material3.HorizontalDivider()
                                Text(if (isArabic) "مواعيد التواجد" else "Availability", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                                if (centerGroups.isEmpty()) {
                                    Text(if (isArabic) "لم تُحدد مواعيد لهذا المركز." else "No hours set for this center.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    centerGroups.forEach { group ->
                                        val first = group.first()
                                        val dayNames = group.map { slot -> days.firstOrNull { it.first == slot.dayOfWeek }?.second.orEmpty() }.joinToString("، ")
                                        Text("$dayNames  •  ${first.startTime} – ${first.endTime}", style = MaterialTheme.typography.bodyMedium)
                                        if (first.notes.isNotBlank()) Text(first.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                if (doctor.notes.isNotBlank()) item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isArabic) "ملاحظات" else "Notes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(doctor.notes)
                        }
                    }
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
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text((if (isArabic) "موعد المتابعة: " else "Follow-up: ") + dateFormat.format(Date(followUp)), fontWeight = FontWeight.SemiBold)
                                    if (visit.notes.isNotBlank()) Text(visit.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إغلاق" else "Close") }
        }
    )

    directionsCenter?.let { center ->
        AlertDialog(
            onDismissRequest = { directionsCenter = null },
            title = { Text(if (isArabic) "فتح الاتجاهات؟" else "Open directions?") },
            text = {
                Text(
                    if (isArabic) "هل تريد عرض الاتجاهات إلى ${center.name} على Google Maps؟"
                    else "Would you like to open directions to ${center.name} in Google Maps?"
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

    mapPickerCenter?.let { center ->
        CenterMapPickerDialog(
            isArabic = isArabic,
            initialLatitude = center.latitude,
            initialLongitude = center.longitude,
            initialMapUrl = center.mapUrl,
            onDismiss = { mapPickerCenter = null },
            onConfirm = { latitude, longitude, mapUrl ->
                onUpdateCenterLocation(center, latitude, longitude, mapUrl)
                mapPickerCenter = null
            }
        )
    }

    directionsDoctor.let { show ->
        if (show) {
            AlertDialog(
                onDismissRequest = { directionsDoctor = false },
                title = { Text(if (isArabic) "فتح الاتجاهات؟" else "Open directions?") },
                text = {
                    Text(
                        if (isArabic) "هل تريد إظهار الاتجاهات إلى موقع الطبيب على Google Maps؟"
                        else "Would you like to show directions to the doctor's location in Google Maps?"
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val lat = doctorLatitude
                        val lon = doctorLongitude
                        directionsDoctor = false
                        openSavedMapLocation(context, lat, lon, doctorMapUrl)
                    }) { Text(if (isArabic) "نعم" else "Yes") }
                },
                dismissButton = {
                    TextButton(onClick = { directionsDoctor = false }) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                }
            )
        }
    }

    if (mapPickerDoctor) {
        CenterMapPickerDialog(
            isArabic = isArabic,
            initialLatitude = doctorLatitude,
            initialLongitude = doctorLongitude,
            initialMapUrl = doctorMapUrl,
            onDismiss = { mapPickerDoctor = false },
            onConfirm = { latitude, longitude, mapUrl ->
                doctorLatitude = latitude
                doctorLongitude = longitude
                doctorMapUrl = mapUrl
                onUpdateDoctorLocation(doctor.copy(latitude = latitude, longitude = longitude, mapUrl = mapUrl))
                mapPickerDoctor = false
            }
        )
    }

}

private data class DoctorAvailabilityDraft(
    val groupId: String = UUID.randomUUID().toString(),
    val days: Set<Int> = setOf(6),
    val startTime: String = "09:00",
    val endTime: String = "17:00",
    val notes: String = "",
    val centerId: Long? = null,
    val existing: List<DoctorAvailabilityEntity> = emptyList()
)

@Composable
private fun DoctorFormDialog(
    isArabic: Boolean,
    initial: DoctorEntity?,
    centers: List<MedicalCenterEntity>,
    specialties: List<SpecialtyEntity>,
    initialCenterIds: Set<Long>,
    initialAvailability: List<DoctorAvailabilityEntity>,
    onDismiss: () -> Unit,
    onSave: (DoctorEntity, Set<Long>, List<DoctorAvailabilityEntity>) -> Unit
) {
    val context = LocalContext.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var selectedSpecialtyId by remember(initial, specialties) { mutableStateOf(initial?.specialtyId ?: specialties.firstOrNull { it.name == initial?.specialty }?.id) }
    var showSpecialtyPicker by remember { mutableStateOf(false) }
    var phones by remember(initial) { mutableStateOf(initial?.phoneNumbers.orEmpty()) }
    var area by remember(initial) { mutableStateOf(initial?.area.orEmpty()) }
    var selectedCenterIds by remember(initial, initialCenterIds, centers) { mutableStateOf(initialCenterIds.ifEmpty { setOfNotNull(initial?.centerId ?: centers.firstOrNull { it.name == initial?.medicalCenter }?.id) }) }
    var showCenterPicker by remember { mutableStateOf(false) }
    var centersExpanded by remember { mutableStateOf(true) }
    var availabilityExpanded by remember(initial, initialAvailability) { mutableStateOf(initialAvailability.isNotEmpty()) }
    var availabilityDrafts by remember(initial, initialAvailability) {
        mutableStateOf(
            initialAvailability.groupBy { it.slotGroupId.ifBlank { "legacy-" + it.id } + "|" + (it.centerId?.toString() ?: "general") }
                .values.map { group ->
                    val first = group.first()
                    DoctorAvailabilityDraft(
                        groupId = first.slotGroupId.ifBlank { UUID.randomUUID().toString() },
                        days = group.map { it.dayOfWeek }.toSet(),
                        startTime = first.startTime,
                        endTime = first.endTime,
                        notes = first.notes,
                        centerId = first.centerId,
                        existing = group
                    )
                }
        )
    }
    var centerMenuForIndex by remember { mutableStateOf<Int?>(null) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var photoUri by remember(initial) { mutableStateOf(initial?.photoUri) }
    var latitude by remember(initial) { mutableStateOf(initial?.latitude) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitude) }
    var mapUrl by remember(initial) { mutableStateOf(initial?.mapUrl) }
    var showDoctorMapPicker by remember { mutableStateOf(false) }
    var editDoctorLocation by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf<String?>(null) }

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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
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
                    OutlinedSelectionField(
                        label = if (isArabic) "التخصص" else "Specialty",
                        value = specialties.firstOrNull { it.id == selectedSpecialtyId }?.name
                            ?: if (isArabic) "بدون تخصص — اختر من القائمة" else "No specialty — choose one",
                        onClick = { showSpecialtyPicker = true }
                    )
                }
                item {
                    OutlinedTextField(
                        value = phones, onValueChange = { phones = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        label = { Text(if (isArabic) "أرقام الهاتف (كل رقم في سطر)" else "Phone numbers (one per line)") }
                    )
                }
                item {
                    OutlinedTextField(value = area, onValueChange = { area = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, label = { Text(if (isArabic) "المنطقة / المدينة (مثال: الزقازيق)" else "Area / City (e.g. Zagazig)") })
                }
                item {
                    TextButton(onClick = { centersExpanded = !centersExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isArabic) "مراكز عمل الطبيب" else "Doctor's centers", modifier = Modifier.weight(1f))
                        Icon(if (centersExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(visible = centersExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedSelectionField(
                                label = if (isArabic) "المراكز المرتبط بها" else "Linked centers",
                                value = if (selectedCenterIds.isEmpty()) if (isArabic) "بدون مركز مسجل" else "No linked center"
                                    else centers.filter { it.id in selectedCenterIds }.joinToString("، ") { it.name },
                                onClick = { showCenterPicker = true }
                            )
                            Text(if (isArabic) "يمكن إضافة مواعيد عامة حتى لو الطبيب غير مرتبط بمركز." else "General availability is allowed even when no center is linked.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                item {
                    TextButton(onClick = { availabilityExpanded = !availabilityExpanded }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (isArabic) "مواعيد تواجد الطبيب" else "Doctor availability", modifier = Modifier.weight(1f))
                        Icon(if (availabilityExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                    }
                    AnimatedVisibility(visible = availabilityExpanded) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(if (isArabic) "كل فترة مستقلة: عامة أو مرتبطة بمركز من المراكز المختارة." else "Each slot is independent: general or linked to one of the selected centers.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            availabilityDrafts.forEachIndexed { index, draft ->
                                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                    Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                            Text((if (isArabic) "الفترة " else "Slot ") + (index + 1), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                            if (availabilityDrafts.size > 1) TextButton(onClick = { availabilityDrafts = availabilityDrafts.filterIndexed { i, _ -> i != index } }) { Text(if (isArabic) "حذف" else "Remove") }
                                        }
                                        OutlinedSelectionField(
                                            label = if (isArabic) "نوع الموعد / المركز" else "Availability type / center",
                                            value = draft.centerId?.let { id -> centers.firstOrNull { it.id == id }?.name } ?: if (isArabic) "موعد عام — بدون مركز" else "General — no center",
                                            onClick = { centerMenuForIndex = index }
                                        )
                                        DropdownMenu(expanded = centerMenuForIndex == index, onDismissRequest = { centerMenuForIndex = null }) {
                                            DropdownMenuItem(text = { Text(if (isArabic) "موعد عام — بدون مركز" else "General — no center") }, onClick = {
                                                availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(centerId = null) }
                                                centerMenuForIndex = null
                                            })
                                            centers.filter { it.id in selectedCenterIds }.forEach { center ->
                                                DropdownMenuItem(text = { Text(center.name) }, onClick = {
                                                    availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(centerId = center.id) }
                                                    centerMenuForIndex = null
                                                })
                                            }
                                        }
                                        Text(if (isArabic) "أيام التواجد" else "Available days", style = MaterialTheme.typography.labelMedium)
                                        listOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة").chunked(2).forEach { pair ->
                                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                pair.forEach { (day, label) ->
                                                    Row(Modifier.weight(1f), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                                        Checkbox(checked = day in draft.days, onCheckedChange = { checked ->
                                                            val next = if (checked) draft.days + day else draft.days - day
                                                            availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(days = next) }
                                                        })
                                                        Text(if (isArabic) label else listOf("Saturday","Sunday","Monday","Tuesday","Wednesday","Thursday","Friday")[listOf(6,7,1,2,3,4,5).indexOf(day)], style = MaterialTheme.typography.bodySmall)
                                                    }
                                                }
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedTextField(value = draft.startTime, onValueChange = { value -> availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(startTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(if (isArabic) "من HH:mm" else "From HH:mm") })
                                            OutlinedTextField(value = draft.endTime, onValueChange = { value -> availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(endTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(if (isArabic) "إلى HH:mm" else "To HH:mm") })
                                        }
                                        OutlinedTextField(value = draft.notes, onValueChange = { value -> availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(notes = value) } }, modifier = Modifier.fillMaxWidth(), label = { Text(if (isArabic) "ملاحظات الفترة" else "Slot notes") })
                                    }
                                }
                            }
                            OutlinedButton(onClick = { availabilityDrafts = availabilityDrafts + DoctorAvailabilityDraft() }, modifier = Modifier.fillMaxWidth()) { Text(if (isArabic) "+ إضافة فترة" else "+ Add slot") }
                        }
                    }
                }
                availabilityError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(if (isArabic) "موقع الطبيب" else "Doctor location", style = MaterialTheme.typography.labelLarge)
                        val hasDoctorLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasDoctorLocation) {
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    latitude = null; longitude = null; mapUrl = null; editDoctorLocation = false
                                }) { Text(if (isArabic) "مسح الموقع" else "Remove location") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { editDoctorLocation = !editDoctorLocation }) {
                                    Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                }
                            }
                            AnimatedLocationActions(visible = !hasDoctorLocation || editDoctorLocation) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CurrentLocationButton(isArabic = isArabic) { lat, lon ->
                                    latitude = lat; longitude = lon; mapUrl = null; editDoctorLocation = false
                                }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showDoctorMapPicker = true }) {
                                    Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                }
                                }
                            }
                        }
                        if (latitude != null && longitude != null) {
                            Text(
                                String.format(Locale.US, "%.6f, %.6f", latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                    val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
                    val invalidSlots = availabilityDrafts.any { it.days.isEmpty() || !validTime.matches(it.startTime) || !validTime.matches(it.endTime) || it.endTime <= it.startTime || (it.centerId != null && it.centerId !in selectedCenterIds) }
                    if (invalidSlots) { availabilityError = if (isArabic) "راجع أيام وساعات كل فترة، وتأكد أن المركز المختار مرتبط بالطبيب." else "Check each slot day and time, and make sure its center is linked to the doctor." } else { availabilityError = null; onSave(
                        DoctorEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            specialty = specialties.firstOrNull { it.id == selectedSpecialtyId }?.name.orEmpty(),
                            specialtyId = selectedSpecialtyId,
                            phoneNumbers = phones.trim(),
                            area = area.trim(),
                            medicalCenter = centers.firstOrNull { it.id == selectedCenterIds.firstOrNull() }?.name.orEmpty(),
                            centerId = selectedCenterIds.firstOrNull(),
                            notes = notes.trim(),
                            photoUri = photoUri,
                            createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                            latitude = latitude,
                            longitude = longitude,
                            mapUrl = mapUrl
                        ),
                        selectedCenterIds,
                        availabilityDrafts.flatMap { draft ->
                            draft.days.map { day ->
                                val old = draft.existing.firstOrNull { it.dayOfWeek == day }
                                DoctorAvailabilityEntity(
                                    id = old?.id ?: 0L,
                                    doctorId = initial?.id ?: 0L,
                                    dayOfWeek = day,
                                    startTime = draft.startTime,
                                    endTime = draft.endTime,
                                    notes = draft.notes.trim(),
                                    createdAt = old?.createdAt ?: System.currentTimeMillis(),
                                    slotGroupId = draft.groupId,
                                    centerId = draft.centerId
                                )
                            }
                        }
                    ) }
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

    if (showDoctorMapPicker) {
        CenterMapPickerDialog(
            isArabic = isArabic,
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialMapUrl = mapUrl,
            onDismiss = { showDoctorMapPicker = false },
            onConfirm = { lat, lon, url ->
                latitude = lat
                longitude = lon
                mapUrl = url
                showDoctorMapPicker = false
            }
        )
    }
}
