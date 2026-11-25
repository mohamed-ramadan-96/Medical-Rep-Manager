package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Surface
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Divider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.distinctUntilChanged
import androidx.compose.runtime.snapshotFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import java.util.UUID
import java.util.Locale
import kotlinx.coroutines.launch

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

@OptIn(ExperimentalMaterial3Api::class)
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
    var appointmentSearch by remember { mutableStateOf("") }
    var activeFilterSheet by remember { mutableStateOf<String?>(null) }
    var filterSearch by remember { mutableStateOf("") }
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
    val filteredGroups = remember(groupedSlots, doctorsById, selectedDay, selectedCenterId, selectedSpecialty, appointmentSearch) {
        groupedSlots.mapNotNull { group ->
            val matching = group.filter { slot ->
                val doctor = doctorsById[slot.doctorId]
                (selectedDay == 0 || slot.dayOfWeek == selectedDay) &&
                (selectedCenterId == 0L || slot.centerId == selectedCenterId) &&
                (selectedSpecialty == "all" || doctor?.specialty.equals(selectedSpecialty, ignoreCase = true)) &&
                (appointmentSearch.isBlank() || normalizePickerSearch(doctor?.name.orEmpty()).contains(normalizePickerSearch(appointmentSearch)))
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
        val listState = rememberLazyListState()
        var searchHeaderExpanded by remember { mutableStateOf(true) }
        var scheduleTitleVisible by remember { mutableStateOf(true) }
        val scrollDirectionThreshold = with(LocalDensity.current) { 8.dp.roundToPx() }
        LaunchedEffect(listState) {
            var previousPosition: Int? = null
            var accumulatedDelta = 0
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull()
                    ?.let { it.index * 100000 - it.offset }
                    ?: 0
            }.distinctUntilChanged().collect { position ->
                val previous = previousPosition
                if (previous == null) {
                    previousPosition = position
                    if (position == 0) searchHeaderExpanded = true
                    return@collect
                }
                accumulatedDelta += position - previous
                if (accumulatedDelta >= scrollDirectionThreshold) {
                    searchHeaderExpanded = false
                    scheduleTitleVisible = false
                    accumulatedDelta = 0
                } else if (accumulatedDelta <= -scrollDirectionThreshold) {
                    searchHeaderExpanded = true
                    scheduleTitleVisible = true
                    accumulatedDelta = 0
                }
                if (position == 0) {
                    searchHeaderExpanded = true
                    scheduleTitleVisible = true
                    accumulatedDelta = 0
                }
                previousPosition = position
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
        ) {
            item {
            AnimatedVisibility(
                visible = scheduleTitleVisible,
                enter = slideInVertically(animationSpec = tween(AppMotion.Emphasized)) { -it }
            } +
                    expandVertically(animationSpec = tween(AppMotion.Emphasized), expandFrom = Alignment.Top) +
                    fadeIn(animationSpec = tween(AppMotion.Emphasized)),
                exit = slideOutVertically(animationSpec = tween(AppMotion.Emphasized)) { -it } +
                    shrinkVertically(animationSpec = tween(AppMotion.Emphasized), shrinkTowards = Alignment.Top) +
                    fadeOut(animationSpec = tween(AppMotion.Emphasized))
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
            }
            stickyHeader {
                BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp)) {            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val filterWidth by animateDpAsState(if (searchHeaderExpanded) 60.dp else 0.dp, tween(AppMotion.Emphasized), label = "scheduleFilterWidth")
                val filterAlpha by animateFloatAsState(if (searchHeaderExpanded) 1f else 0f, tween(AppMotion.Fast), label = "scheduleFilterAlpha")
                val searchWidth by animateDpAsState(if (searchHeaderExpanded) maxWidth - 68.dp else 44.dp, tween(AppMotion.Emphasized), label = "scheduleSearchWidth")
                val controlHeight by animateDpAsState(if (searchHeaderExpanded) 60.dp else 44.dp, tween(AppMotion.Emphasized), label = "scheduleSearchHeight")
                Row(
                    Modifier.fillMaxWidth().height(controlHeight),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.width(searchWidth).height(controlHeight)) {
                        Surface(
                            onClick = if (searchHeaderExpanded) ({}) else ({ searchHeaderExpanded = true }),
                            shape = if (searchHeaderExpanded) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Row(modifier = Modifier.padding(horizontal = if (searchHeaderExpanded) 18.dp else 10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Search, contentDescription = if (isArabic) "بحث" else "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(if (searchHeaderExpanded) 24.dp else 20.dp))
                                if (searchHeaderExpanded) {
                                    BasicTextField(
                                        value = appointmentSearch,
                                        onValueChange = { appointmentSearch = it },
                                        modifier = Modifier.weight(1f).padding(start = 12.dp),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                                        decorationBox = { innerTextField ->
                                            Box(contentAlignment = Alignment.CenterStart) {
                                                if (appointmentSearch.isEmpty()) Text(if (isArabic) "ابحث عن مواعيد باسم الطبيب..." else "Search appointments by doctor...", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                innerTextField()
                                            }
                                        }
                                    )
                                } else {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                        }
                    }
                    Box(Modifier.width(filterWidth).height(controlHeight).clipToBounds()) {
                        if (searchHeaderExpanded) {
                            val filterIconColor = MaterialTheme.colorScheme.onSurface
                            Surface(
                                onClick = { showFilters = !showFilters },
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.size(60.dp).alpha(filterAlpha)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Canvas(Modifier.size(28.dp)) {
                                        val stroke = 2.dp.toPx()
                                        val left = 2.dp.toPx()
                                        val right = size.width - 2.dp.toPx()
                                        val railYs = listOf(6.dp.toPx(), 14.dp.toPx(), 22.dp.toPx())
                                        val knobXs = listOf(17.dp.toPx(), 9.dp.toPx(), 20.dp.toPx())
                                        railYs.forEachIndexed { index, y ->
                                            val knobX = knobXs[index]
                                            val gap = 2.4.dp.toPx()
                                            drawLine(filterIconColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(knobX - gap, y), stroke, cap = StrokeCap.Round)
                                            drawLine(filterIconColor, androidx.compose.ui.geometry.Offset(knobX + gap, y), androidx.compose.ui.geometry.Offset(right, y), stroke, cap = StrokeCap.Round)
                                            drawLine(filterIconColor, androidx.compose.ui.geometry.Offset(knobX, y - 3.dp.toPx()), androidx.compose.ui.geometry.Offset(knobX, y + 3.dp.toPx()), stroke, cap = StrokeCap.Round)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            item {
            AnimatedVisibility(
                visible = showFilters,
                enter = expandVertically(animationSpec = tween(AppMotion.Emphasized)) + fadeIn(animationSpec = tween(AppMotion.Standard)),
                exit = shrinkVertically(animationSpec = tween(AppMotion.Standard)) + fadeOut(animationSpec = tween(AppMotion.Fast))
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedSelectionField(label = if (isArabic) "اليوم" else "Day", value = if (selectedDay == 0) { if (isArabic) "كل الأيام" else "All days" } else weekDays.firstOrNull { it.first == selectedDay }?.second?.let { if (isArabic) it.second else it.first }.orEmpty(), onClick = { filterSearch = ""; activeFilterSheet = "day" })
                }
                Column(modifier = Modifier.weight(1f)) {
                    OutlinedSelectionField(label = if (isArabic) "التخصص" else "Specialty", value = if (selectedSpecialty == "all") { if (isArabic) "كل التخصصات" else "All specialties" } else selectedSpecialty, onClick = { filterSearch = ""; activeFilterSheet = "specialty" })
                }
            }
            OutlinedSelectionField(label = if (isArabic) "المركز" else "Center", value = if (selectedCenterId == 0L) { if (isArabic) "كل المراكز" else "All centers" } else centers.firstOrNull { it.id == selectedCenterId }?.name.orEmpty(), onClick = { filterSearch = ""; activeFilterSheet = "center" })

                }
            }
            }
            item {
            if (activeFilterSheet != null) {
                ModalBottomSheet(
                    onDismissRequest = { activeFilterSheet = null },
                    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val sheetTitle = when (activeFilterSheet) {
                            "day" -> if (isArabic) "اختيار اليوم" else "Choose day"
                            "specialty" -> if (isArabic) "اختيار التخصص" else "Choose specialty"
                            else -> if (isArabic) "اختيار المركز" else "Choose center"
                        }
                        Text(sheetTitle, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = filterSearch,
                            onValueChange = { filterSearch = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            placeholder = {
                                Text(
                                    when (activeFilterSheet) {
                                        "day" -> if (isArabic) "ابحث عن يوم..." else "Search days..."
                                        "specialty" -> if (isArabic) "ابحث عن تخصص..." else "Search specialties..."
                                        else -> if (isArabic) "ابحث عن مركز..." else "Search centers..."
                                    }
                                )
                            },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                            when (activeFilterSheet) {
                                "day" -> {
                                    item {
                                        FilterPickerRow(if (isArabic) "كل الأيام" else "All days") {
                                            selectedDay = 0
                                            activeFilterSheet = null
                                        }
                                    }
                                    items(weekDays.filter { (_, names) ->
                                        normalizePickerSearch(if (isArabic) names.second else names.first).contains(normalizePickerSearch(filterSearch))
                                    }, key = { it.first }) { (day, names) ->
                                        FilterPickerRow(if (isArabic) names.second else names.first) {
                                            selectedDay = day
                                            activeFilterSheet = null
                                        }
                                    }
                                }
                                "specialty" -> {
                                    item {
                                        FilterPickerRow(if (isArabic) "كل التخصصات" else "All specialties") {
                                            selectedSpecialty = "all"
                                            activeFilterSheet = null
                                        }
                                    }
                                    items(specialties.map { it.name }.distinct().sorted().filter {
                                        normalizePickerSearch(it).contains(normalizePickerSearch(filterSearch))
                                    }) { name ->
                                        FilterPickerRow(name) {
                                            selectedSpecialty = name
                                            activeFilterSheet = null
                                        }
                                    }
                                }
                                "center" -> {
                                    item {
                                        FilterPickerRow(if (isArabic) "كل المراكز" else "All centers") {
                                            selectedCenterId = 0L
                                            activeFilterSheet = null
                                        }
                                    }
                                    items(centers.filter {
                                        normalizePickerSearch(it.name).contains(normalizePickerSearch(filterSearch))
                                    }, key = { it.id }) { center ->
                                        FilterPickerRow(center.name) {
                                            selectedCenterId = center.id
                                            activeFilterSheet = null
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            }
            item {
            if (doctors.isEmpty()) {
                Text(if (isArabic) "أضف طبيباً أولاً حتى تتمكن من تحديد مواعيده." else "Add a doctor first to define their availability.", modifier = Modifier.padding(top = 20.dp))
            }
            } else if (groupedSlots.isEmpty()) {
                Text(if (isArabic) "لم تتم إضافة مواعيد متاحة بعد. اضغط + للبدء." else "No availability added yet. Tap + to get started.", modifier = Modifier.padding(top = 20.dp))
            } else if (filteredGroups.isEmpty()) {
                Text(if (isArabic) "لا توجد مواعيد مطابقة للاختيارات." else "No availability matches these filters.", modifier = Modifier.padding(top = 20.dp))
            } else {
                
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

@OptIn(ExperimentalMaterial3Api::class)
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
    var doctorSearch by remember { mutableStateOf("") }
    var centerSearch by remember { mutableStateOf("") }
    var selectedCenterId by remember(initialGroup, doctors, doctorCenterLinks) {
        mutableStateOf(initial?.centerId ?: doctorCenterLinks.firstOrNull { it.doctorId == selectedDoctorId }?.centerId ?: doctors.firstOrNull { it.id == selectedDoctorId }?.centerId)
    }
    var error by remember { mutableStateOf<String?>(null) }
    var timePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val linkedCenters = centers.filter { center ->
        doctorCenterLinks.any { it.doctorId == selectedDoctorId && it.centerId == center.id } ||
            (doctorCenterLinks.none { it.doctorId == selectedDoctorId } && doctors.firstOrNull { it.id == selectedDoctorId }?.centerId == center.id)
    }
    val selectableCenters = linkedCenters.ifEmpty { centers }
    val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
    val hasInvalidDays = drafts.any { it.days.isEmpty() }
    val hasInvalidTime = drafts.any { !validTime.matches(it.startTime) || !validTime.matches(it.endTime) || it.endTime <= it.startTime }
    val overlappingPeriod = drafts.indices.firstOrNull { index ->
        val current = drafts[index]
        drafts.indices.any { otherIndex ->
            otherIndex != index && current.days.intersect(drafts[otherIndex].days).isNotEmpty() &&
                current.startTime < drafts[otherIndex].endTime && drafts[otherIndex].startTime < current.endTime
        }
    }
    val formError = when {
        selectedDoctorId == null -> if (isArabic) "اختر الطبيب أولاً" else "Choose a doctor first"
        hasInvalidDays -> if (isArabic) "اختر يومًا واحدًا على الأقل لكل فترة" else "Select at least one day for each period"
        hasInvalidTime -> if (isArabic) "وقت النهاية لازم يكون بعد البداية" else "End time must be after start time"
        else -> null
    }
    val canSave = formError == null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (initialGroup.isEmpty()) { if (isArabic) "إضافة مواعيد للطبيب" else "Add doctor availability" }
                else { if (isArabic) "تعديل المواعيد" else "Edit availability" },
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AvailabilitySelectionField(
                    label = if (isArabic) "الطبيب" else "Doctor",
                    value = doctors.firstOrNull { it.id == selectedDoctorId }?.name ?: if (isArabic) "اختر الطبيب" else "Choose doctor",
                    onClick = { doctorSearch = ""; doctorMenu = true }
                )
                if (doctorMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { doctorMenu = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(if (isArabic) "اختيار الطبيب" else "Choose doctor", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = doctorSearch,
                                onValueChange = { doctorSearch = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text(if (isArabic) "ابحث عن دكتور..." else "Search doctors...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                            )
                            val filteredDoctors = doctors.filter { normalizePickerSearch(it.name).contains(normalizePickerSearch(doctorSearch)) }
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                                items(filteredDoctors, key = { it.id }) { doctor ->
                                    Surface(
                                        onClick = {
                                            selectedDoctorId = doctor.id
                                            selectedCenterId = doctorCenterLinks.firstOrNull { it.doctorId == doctor.id }?.centerId ?: doctor.centerId
                                            doctorMenu = false
                                        },
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(doctor.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                                if (filteredDoctors.isEmpty()) item {
                                    Text(if (isArabic) "لا توجد نتائج" else "No results", modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                AvailabilitySelectionField(
                    label = if (isArabic) "المركز" else "Center",
                    value = selectableCenters.firstOrNull { it.id == selectedCenterId }?.name
                        ?: if (isArabic) "اختر المركز" else "Choose center",
                    onClick = { centerSearch = ""; centerMenu = true }
                )
                if (centerMenu) {
                    ModalBottomSheet(
                        onDismissRequest = { centerMenu = false },
                        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(if (isArabic) "اختيار المركز" else "Choose center", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            OutlinedTextField(
                                value = centerSearch,
                                onValueChange = { centerSearch = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                placeholder = { Text(if (isArabic) "ابحث عن مركز..." else "Search centers...") },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                            )
                            val filteredCenters = selectableCenters.filter { normalizePickerSearch(it.name).contains(normalizePickerSearch(centerSearch)) }
                            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                                items(filteredCenters, key = { it.id }) { center ->
                                    Surface(
                                        onClick = { selectedCenterId = center.id; centerMenu = false },
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(center.name, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                                        }
                                    }
                                }
                                if (filteredCenters.isEmpty()) item {
                                    Text(if (isArabic) "لا توجد نتائج" else "No results", modifier = Modifier.fillMaxWidth().padding(20.dp), textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                drafts.forEachIndexed { index, draft ->
                    val overlap = drafts.indices.any { otherIndex ->
                        otherIndex != index && draft.days.intersect(drafts[otherIndex].days).isNotEmpty() &&
                            draft.startTime < drafts[otherIndex].endTime && drafts[otherIndex].startTime < draft.endTime
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text((if (isArabic) "الفترة " else "Period ") + (index + 1), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                if (drafts.size > 1) {
                                    TextButton(onClick = {
                                        val removed = drafts[index]
                                        drafts = drafts.filterIndexed { i, _ -> i != index }
                                        error = null
                                        scope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = if (isArabic) "تم حذف الفترة ${index + 1}" else "Period ${index + 1} deleted",
                                                actionLabel = if (isArabic) "تراجع" else "Undo",
                                                withDismissAction = true
                                            )
                                            if (result == SnackbarResult.ActionPerformed) drafts = (drafts.toMutableList().also { it.add(index.coerceAtMost(it.size), removed) })
                                        }
                                    }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف الفترة" else "Delete period", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                        Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (isArabic) "أيام التواجد" else "Available days", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TextButton(
                                    onClick = {
                                        val allDays = weekDays.map { it.first }.toSet()
                                        val updated = if (draft.days == allDays) emptySet() else allDays
                                        drafts = drafts.toMutableList().also { it[index] = draft.copy(days = updated) }
                                        error = null
                                    },
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        if (draft.days == weekDays.map { it.first }.toSet()) {
                                            if (isArabic) "إلغاء تحديد الكل" else "Clear all"
                                        } else {
                                            if (isArabic) "تحديد الكل" else "Select all"
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                maxItemsInEachRow = 4,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                weekDays.forEach { (day, names) ->
                                    val selected = day in draft.days
                                    Surface(
                                        modifier = Modifier.weight(1f).height(40.dp),
                                        onClick = {
                                            val updated = if (selected) draft.days - day else draft.days + day
                                            drafts = drafts.toMutableList().also { it[index] = draft.copy(days = updated) }
                                            error = null
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp)
                                        ) {
                                            Text(
                                                if (isArabic) names.second else names.first.take(3),
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                            if (draft.days.isEmpty()) Text(if (isArabic) "اختر يوم واحد على الأقل" else "Select at least one day", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    AvailabilitySelectionField(
                                        label = if (isArabic) "من" else "From",
                                        value = displayAvailabilityTime(draft.startTime, isArabic),
                                        onClick = { timePickerTarget = index to true }
                                    )
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    AvailabilitySelectionField(
                                        label = if (isArabic) "إلى" else "To",
                                        value = displayAvailabilityTime(draft.endTime, isArabic),
                                        isError = draft.endTime <= draft.startTime,
                                        onClick = { timePickerTarget = index to false }
                                    )
                                }
                            }
                            if (draft.endTime <= draft.startTime) Text(if (isArabic) "وقت النهاية لازم يكون بعد البداية" else "End time must be after start time", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            if (overlap) Text(if (isArabic) "تحذير: هذه الفترة تتداخل مع فترة أخرى في نفس اليوم" else "Warning: this period overlaps another period on the same day", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            val startMinutes = draft.startTime.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 9) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
                            val endMinutes = draft.endTime.split(":").let { (it.getOrNull(0)?.toIntOrNull() ?: 17) * 60 + (it.getOrNull(1)?.toIntOrNull() ?: 0) }
                            val hours = (endMinutes - startMinutes) / 60.0
                            val daySummary = when (draft.days) {
                                weekDays.map { it.first }.toSet() -> if (isArabic) "كل الأيام" else "All days"
                                setOf(6, 7, 1, 2, 3, 4) -> if (isArabic) "أيام العمل" else "Work days"
                                else -> weekDays.filter { it.first in draft.days }.joinToString("، ") { if (isArabic) it.second.second else it.second.first }
                            }
                            if (!overlap && draft.days.isNotEmpty() && draft.endTime > draft.startTime) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        "✓ $daySummary، ${if (isArabic) "من" else "from"} ${displayAvailabilityTime(draft.startTime, isArabic)} ${if (isArabic) "إلى" else "to"} ${displayAvailabilityTime(draft.endTime, isArabic)} (${String.format(Locale.US, "%.1f", hours).trimEnd('0').trimEnd('.')} ${if (isArabic) "ساعات" else "hours"})",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            OutlinedTextField(
                                value = draft.notes,
                                onValueChange = { value -> drafts = drafts.toMutableList().also { it[index] = draft.copy(notes = value) } },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                label = { Text(if (isArabic) "ملاحظات الفترة (اختياري)" else "Period notes (optional)") },
                                placeholder = { Text(if (isArabic) "مثال: بالحجز فقط" else "e.g. By appointment only") }
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = { drafts = drafts + AvailabilityDraft(); error = null },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(if (isArabic) "إضافة فترة بمواعيد مختلفة" else "Add period with different hours")
                }
                SnackbarHost(hostState = snackbarHostState)
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(
                    enabled = canSave,
                    onClick = {
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
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) { Text(if (isArabic) "حفظ المواعيد" else "Save availability", fontWeight = FontWeight.Bold) }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(10.dp)) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        },
        dismissButton = {}
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

@Composable
private fun FilterPickerRow(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun AvailabilitySelectionField(
    label: String,
    value: String,
    onClick: () -> Unit,
    isError: Boolean = false,
    isRequired: Boolean = false
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(
            if (isError || isRequired) 1.5.dp else 1.dp,
            when {
                isError -> MaterialTheme.colorScheme.error
                isRequired -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = if (isRequired) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun normalizePickerSearch(value: String): String =
    value.lowercase()
        .replace('أ', 'ا')
        .replace('إ', 'ا')
        .replace('آ', 'ا')
        .replace('ى', 'ي')
        .replace('ة', 'ه')
        .trim()

private fun displayAvailabilityTime(value: String, isArabic: Boolean): String {
    val pieces = value.split(":")
    val hour = pieces.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = pieces.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val suffix = if (hour < 12) (if (isArabic) "ص" else "AM") else (if (isArabic) "م" else "PM")
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
