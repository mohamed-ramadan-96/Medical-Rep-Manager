package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
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

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    var centerSearch by remember { mutableStateOf("") }
    var selectedArea by remember { mutableStateOf("all") }
    var showAreaFilter by remember { mutableStateOf(false) }
    val centerAreas = remember(centers) { centers.map { it.area.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase() }.sorted() }
    val filteredCenters = remember(centers, centerSearch, selectedArea) {
        centers.filter { center ->
            (selectedArea == "all" || center.area.equals(selectedArea, ignoreCase = true)) &&
                (centerSearch.isBlank() || listOf(center.name, center.address, center.phone).any { it.contains(centerSearch.trim(), ignoreCase = true) })
        }
    }
    val centerListState = rememberLazyListState()
    var centerHeaderExpanded by remember { mutableStateOf(true) }
    val centerScrollThreshold = with(LocalDensity.current) { 8.dp.roundToPx() }
    LaunchedEffect(centerListState) {
        var previousPosition: Int? = null
        var accumulatedDelta = 0
        snapshotFlow {
            centerListState.layoutInfo.visibleItemsInfo.firstOrNull()?.let { it.index * 100000 - it.offset } ?: 0
        }.distinctUntilChanged().collect { position ->
            val previous = previousPosition
            if (previous == null) {
                previousPosition = position
                return@collect
            }
            accumulatedDelta += position - previous
            if (accumulatedDelta >= centerScrollThreshold) {
                centerHeaderExpanded = false
                accumulatedDelta = 0
            } else if (accumulatedDelta <= -centerScrollThreshold) {
                centerHeaderExpanded = true
                accumulatedDelta = 0
            }
            if (position == 0) {
                centerHeaderExpanded = true
                accumulatedDelta = 0
            }
            previousPosition = position
        }
    }
    val centerHeaderHeight by animateDpAsState(if (centerHeaderExpanded) 76.dp else 48.dp, animationSpec = tween(AppMotion.Emphasized), label = "centersHeaderHeight")

    val centerFadeColor = MaterialTheme.colorScheme.background
    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة مركز" else "Add center")
            }
        }
    ) { inner ->
        LazyColumn(
            state = centerListState,
            modifier = Modifier.fillMaxSize().padding(inner).drawWithContent {
                drawContent()
                val stickyHeader = centerListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }
                if (stickyHeader != null) {
                    val fadeTop = (stickyHeader.offset + stickyHeader.size).toFloat()
                    val fadeHeight = 36.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to centerFadeColor,
                            1f to centerFadeColor.copy(alpha = 0f),
                            startY = fadeTop,
                            endY = fadeTop + fadeHeight
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, fadeTop),
                        size = androidx.compose.ui.geometry.Size(size.width, fadeHeight)
                    )
                }
            },
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
                Column(Modifier.fillMaxWidth().height(centerHeaderHeight), verticalArrangement = Arrangement.Center) {
                    Text(
                        if (isArabic) "المراكز والعيادات" else "Medical centers",
                        style = if (centerHeaderExpanded) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    AnimatedVisibility(
                        visible = centerHeaderExpanded,
                        enter = fadeIn(animationSpec = tween(AppMotion.Fast)) + expandVertically(animationSpec = tween(AppMotion.Emphasized)),
                        exit = fadeOut(animationSpec = tween(AppMotion.Fast)) + shrinkVertically(animationSpec = tween(AppMotion.Standard))
                    ) {
                        Text(if (isArabic) "عدد المراكز: ${centers.size}" else "Centers: ${centers.size}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            stickyHeader {
                Row(
                    modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = centerSearch,
                        onValueChange = { centerSearch = it },
                        modifier = Modifier.weight(1f).height(56.dp),
                        singleLine = true,
                        shape = MaterialTheme.shapes.small,
                        placeholder = { Text(if (isArabic) "ابحث عن مركز..." else "Search centers...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                    )
                    androidx.compose.foundation.layout.Box {
                        IconButton(onClick = { showAreaFilter = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = if (isArabic) "تصفية حسب المنطقة" else "Filter by area")
                        }
                        DropdownMenu(expanded = showAreaFilter, onDismissRequest = { showAreaFilter = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "كل المناطق" else "All areas") },
                                onClick = { selectedArea = "all"; showAreaFilter = false }
                            )
                            centerAreas.forEach { area ->
                                DropdownMenuItem(text = { Text(area) }, onClick = { selectedArea = area; showAreaFilter = false })
                            }
                        }
                    }
                }
            }
            if (centers.isEmpty()) {
                item {
                    Text(
                        if (isArabic) "لم تتم إضافة مراكز بعد. اضغط + لإضافة أول مركز." else "No centers yet. Tap + to add your first center.",
                        modifier = Modifier.padding(top = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredCenters.isEmpty()) {
                item {
                    Text(
                        if (isArabic) "لا توجد مراكز مطابقة للبحث أو المنطقة المحددة." else "No centers match this search or area.",
                        modifier = Modifier.padding(top = 24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
                    items(centers, key = { it.id }
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
    var latitude by remember(initial) { mutableStateOf(initial?.latitude) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitude) }
    var mapUrl by remember(initial) { mutableStateOf(initial?.mapUrl) }
    var showMapPicker by remember { mutableStateOf(false) }
    var editCenterLocation by remember { mutableStateOf(false) }
    var locationSectionExpanded by remember(initial) { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var showWorkingHoursInfo by remember { mutableStateOf(false) }

    val dialogMaxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.88f

    if (showWorkingHoursInfo) {
        AlertDialog(
            onDismissRequest = { showWorkingHoursInfo = false },
            title = { Text(if (isArabic) "مواعيد عمل المركز" else "Center working hours") },
            text = {
                Text(
                    if (isArabic) "المواعيد دي تخص المركز نفسه، ومش محتاجة ملفات أطباء. تقدر تضيف أكتر من فترة عمل، وتحدد أيام وساعات مستقلة لكل فترة."
                    else "These hours belong to the center itself and do not require doctor profiles. You can add multiple working periods, each with its own days and hours."
                )
            },
            confirmButton = {
                TextButton(onClick = { showWorkingHoursInfo = false }) {
                    Text(if (isArabic) "تمام" else "OK")
                }
            }
        )
    }

    AlertDialog(
        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(max = dialogMaxHeight),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (isArabic) "إضافة مركز أو عيادة" else "Add center or clinic" } else { if (isArabic) "تعديل المركز" else "Edit center" }, style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = dialogMaxHeight - 150.dp),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small,
                        label = { Text(if (isArabic) "اسم المركز *" else "Center name *") },
                        isError = nameError
                    )
                }
                item {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small,
                        label = { Text(if (isArabic) "العنوان" else "Address") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true, shape = MaterialTheme.shapes.small,
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone") }
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { workSlots = workSlots + CenterWorkingHoursDraft() },
                                modifier = Modifier.weight(1f),
                                shape = MaterialTheme.shapes.small
                            ) {
                                Text(if (isArabic) "+ إضافة فترة عمل" else "+ Add working-hours slot")
                            }
                            IconButton(onClick = { showWorkingHoursInfo = true }) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = if (isArabic) "معلومات مواعيد العمل" else "Working hours information",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        workSlots.forEachIndexed { index, slot ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                                Column(Modifier.fillMaxWidth().padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text((if (isArabic) "فترة عمل " else "Working slot ") + (index + 1), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                        TextButton(onClick = { workSlots = workSlots.filterIndexed { i, _ -> i != index } }) { Text(if (isArabic) "حذف" else "Remove") }
                                    }
                                    listOf(6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء", 3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة").chunked(2).forEach { pair ->
                                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                                        OutlinedTextField(value = slot.startTime, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(startTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small, label = { Text(if (isArabic) "من" else "From") })
                                        OutlinedTextField(value = slot.endTime, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(endTime = value) } }, modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.small, label = { Text(if (isArabic) "إلى" else "To") })
                                    }
                                    OutlinedTextField(value = slot.notes, onValueChange = { value -> workSlots = workSlots.toMutableList().also { it[index] = slot.copy(notes = value) } }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small, label = { Text(if (isArabic) "ملاحظات الفترة" else "Slot notes") })
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2, shape = MaterialTheme.shapes.small,
                        label = { Text(if (isArabic) "ملاحظات" else "Notes") }
                    )
                }
                item {
                    val hasCenterLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    locationSectionExpanded = !locationSectionExpanded
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(
                                        if (isArabic) "موقع المركز على الخريطة" else "Center map location",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        when {
                                            latitude != null && longitude != null ->
                                                String.format(java.util.Locale.US, "%.5f, %.5f", latitude, longitude)
                                            hasCenterLocation -> if (isArabic) "موقع محفوظ" else "Location saved"
                                            else -> if (isArabic) "لم يتم تحديد موقع" else "No location selected"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = if (locationSectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isArabic) "تفاصيل الموقع" else "Location details"
                                )
                            }
                            AnimatedVisibility(
                                visible = locationSectionExpanded,
                                enter = expandVertically(animationSpec = tween(AppMotion.Standard)) + fadeIn(animationSpec = tween(AppMotion.Standard)),
                                exit = shrinkVertically(animationSpec = tween(AppMotion.Standard)) + fadeOut(animationSpec = tween(AppMotion.Fast))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (hasCenterLocation) {
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) },
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(if (isArabic) "عرض المكان على الخريطة" else "View place on map")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                latitude = null
                                                longitude = null
                                                mapUrl = null
                                                editCenterLocation = false
                                            },
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(if (isArabic) "مسح الموقع" else "Clear location")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { editCenterLocation = !editCenterLocation },
                                            shape = MaterialTheme.shapes.small
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
                                                onClick = { showMapPicker = true },
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
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

                        unregisteredDoctorCount = initial?.unregisteredDoctorCount ?: 0
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
                },
                shape = MaterialTheme.shapes.small
            ) { Text(if (isArabic) "حفظ" else "Save") }
            OutlinedButton(onClick = onDismiss, shape = MaterialTheme.shapes.small) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        },
        dismissButton = { }
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