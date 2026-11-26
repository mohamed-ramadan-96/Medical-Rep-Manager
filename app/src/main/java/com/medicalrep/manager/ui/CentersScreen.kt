package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.CenterAvailabilityEntity
import androidx.room.withTransaction
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File


@Composable
private fun CenterFilterActionButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconColor = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(60.dp)
    ) {
        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(28.dp)) {
                val stroke = 2.dp.toPx()
                val left = 2.dp.toPx()
                val right = size.width - 2.dp.toPx()
                val railYs = listOf(6.dp.toPx(), 14.dp.toPx(), 22.dp.toPx())
                val knobXs = listOf(17.dp.toPx(), 9.dp.toPx(), 20.dp.toPx())
                railYs.forEachIndexed { index, y ->
                    val knobX = knobXs[index]
                    val gap = 2.4.dp.toPx()
                    drawLine(iconColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(knobX - gap, y), stroke, cap = StrokeCap.Round)
                    drawLine(iconColor, androidx.compose.ui.geometry.Offset(knobX + gap, y), androidx.compose.ui.geometry.Offset(right, y), stroke, cap = StrokeCap.Round)
                    drawLine(iconColor, androidx.compose.ui.geometry.Offset(knobX, y - 3.dp.toPx()), androidx.compose.ui.geometry.Offset(knobX, y + 3.dp.toPx()), stroke, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun CenterSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    isArabic: Boolean,
    compact: Boolean,
    onExpand: () -> Unit
) {
    val height by animateDpAsState(if (compact) 44.dp else 60.dp, tween(AppMotion.Emphasized), label = "centerSearchHeight")
    Surface(
        onClick = if (compact) onExpand else ({ }),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().height(height)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (compact) 10.dp else 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = if (isArabic) "بحث" else "Search",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(if (compact) 20.dp else 24.dp)
            )
            if (!compact) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f).padding(start = 12.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                    decorationBox = { innerTextField ->
                        androidx.compose.foundation.layout.Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) Text(
                                if (isArabic) "ابحث عن مركز..." else "Search centers...",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
    val doctors by doctorDao.observeAll().collectAsState(initial = emptyList())
    val savedAreas = remember(doctors) {
        doctors.flatMap { it.area.split("|").map(String::trim).filter(String::isNotBlank) }
            .distinctBy { it.lowercase(java.util.Locale.ROOT) }.sorted()
    }
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
            centerListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.index > 1 }
                ?.let { it.index * 100000 - it.offset }
                ?: 0
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
    val centerSearchHeight by animateDpAsState(if (centerHeaderExpanded) 60.dp else 44.dp, animationSpec = tween(AppMotion.Emphasized), label = "centersSearchHeight")

    val fadeHeaderOffset = centerListState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }?.offset ?: Int.MAX_VALUE
        val fadeStartOffset = with(LocalDensity.current) { 48.dp.roundToPx() }
        val fadeAlpha by animateFloatAsState(
            targetValue = ((fadeStartOffset - fadeHeaderOffset).toFloat() / fadeStartOffset).coerceIn(0f, 1f),
            animationSpec = tween(AppMotion.Emphasized),
            label = "centersFadeAlpha"
        )
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
                if (stickyHeader != null && fadeAlpha > 0f) {
                    val fadeTop = (stickyHeader.offset + stickyHeader.size).toFloat()
                    val fadeHeight = 72.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to centerFadeColor.copy(alpha = fadeAlpha),
                            0.65f to centerFadeColor.copy(alpha = 0.96f * fadeAlpha),
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
                BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(top = 0.dp, bottom = 8.dp)) {
                    val filterWidth by animateDpAsState(if (centerHeaderExpanded) 60.dp else 0.dp, tween(AppMotion.Emphasized), label = "centerFilterWidth")
                    val filterAlpha by animateFloatAsState(if (centerHeaderExpanded) 1f else 0f, tween(AppMotion.Fast), label = "centerFilterAlpha")
                    val searchWidth by animateDpAsState(if (centerHeaderExpanded) maxWidth - 68.dp else 44.dp, tween(AppMotion.Emphasized), label = "centerSearchWidth")
                    val controlHeight by animateDpAsState(if (centerHeaderExpanded) 60.dp else 44.dp, tween(AppMotion.Emphasized), label = "centerSearchControlHeight")
                    Row(
                        Modifier.fillMaxWidth().height(controlHeight),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.width(searchWidth).height(controlHeight)) {
                            CenterSearchField(
                                value = centerSearch,
                                onValueChange = { centerSearch = it },
                                isArabic = isArabic,
                                compact = !centerHeaderExpanded,
                                onExpand = { centerHeaderExpanded = true }
                            )
                        }
                        Box(Modifier.width(filterWidth).height(controlHeight).clipToBounds()) {
                            if (centerHeaderExpanded) {
                                CenterFilterActionButton(
                                    onClick = { showAreaFilter = !showAreaFilter },
                                    modifier = Modifier.alpha(filterAlpha)
                                )
                            }
                        }
                        DropdownMenu(expanded = showAreaFilter, onDismissRequest = { showAreaFilter = false }) {
                            DropdownMenuItem(
                                text = { Text(if (isArabic) "كل المناطق" else "All areas") },
                                onClick = { selectedArea = "all"; showAreaFilter = false }
                            )
                            centerAreas.forEach { areaOption ->
                                DropdownMenuItem(text = { Text(areaOption) }, onClick = { selectedArea = areaOption; showAreaFilter = false })
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
            items(filteredCenters, key = { it.id }) { center ->
                        val expanded = center.id in expandedCenterIds
                        val centerSlots = centerAvailability.filter { it.centerId == center.id }
                        val centerIconColor = MaterialTheme.colorScheme.onPrimaryContainer
                        val expandRotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, animationSpec = tween(AppMotion.Standard), label = "centerExpandRotation")
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
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(36.dp),
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Canvas(Modifier.padding(8.dp).fillMaxSize()) {
                                            val unit = size.minDimension / 48f
                                            scale(scaleX = unit, scaleY = unit, pivot = androidx.compose.ui.geometry.Offset.Zero) {
                                                val stroke = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                                drawPath(Path().apply {
                                                    moveTo(18f, 4f); lineTo(31f, 4f); quadraticTo(34f, 4f, 34f, 7f); lineTo(34f, 41f); quadraticTo(34f, 44f, 31f, 44f); lineTo(18f, 44f); quadraticTo(15f, 44f, 15f, 41f); lineTo(15f, 7f); quadraticTo(15f, 4f, 18f, 4f); close()
                                                }, centerIconColor, style = stroke)
                                                drawPath(Path().apply {
                                                    moveTo(15f, 16f); lineTo(10f, 16f); quadraticTo(6f, 16f, 6f, 20f)
                                                    lineTo(6f, 39f); quadraticTo(6f, 43f, 10f, 43f); lineTo(15f, 43f)
                                                    moveTo(34f, 16f); lineTo(38f, 16f); quadraticTo(42f, 16f, 42f, 20f)
                                                    lineTo(42f, 39f); quadraticTo(42f, 43f, 38f, 43f); lineTo(34f, 43f)
                                                }, centerIconColor, style = stroke)
                                                drawLine(centerIconColor, androidx.compose.ui.geometry.Offset(20f, 28f), androidx.compose.ui.geometry.Offset(29f, 28f), 2.8f, cap = StrokeCap.Round)
                                                drawLine(centerIconColor, androidx.compose.ui.geometry.Offset(20f, 35f), androidx.compose.ui.geometry.Offset(29f, 35f), 2.8f, cap = StrokeCap.Round)
                                            }
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(center.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                                        if (center.area.isNotBlank()) Text(center.area, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1)
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
                                            Icons.Default.ExpandMore,
                                            contentDescription = if (isArabic) {
                                                if (expanded) "إخفاء التفاصيل" else "عرض التفاصيل"
                                            } else {
                                                if (expanded) "Hide details" else "Show details"
                                            },
                                            modifier = Modifier.size(20.dp).rotate(expandRotation)
                                        )
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if ((center.latitude != null && center.longitude != null) || !center.mapUrl.isNullOrBlank()) {
                                        IconButton(modifier = Modifier.size(32.dp), onClick = { directionsCenter = center }) {
                                            Icon(Icons.Default.LocationOn, contentDescription = if (isArabic) "الاتجاهات إلى المركز" else "Directions to center", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { attachmentsCenter = center }) {
                                        Icon(Icons.Default.AttachFile, contentDescription = if (isArabic) "المرفقات" else "Attachments", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { editing = center; showForm = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(modifier = Modifier.size(32.dp), onClick = { pendingDelete = center }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(18.dp))
                                    }
                                }
                                AnimatedVisibility(
                                    visible = expanded,
                                    enter = expandVertically(animationSpec = tween(AppMotion.Emphasized)) + fadeIn(animationSpec = tween(AppMotion.Standard)),
                                    exit = shrinkVertically(animationSpec = tween(AppMotion.Standard)) + fadeOut(animationSpec = tween(AppMotion.Fast))
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

    if (showForm) {
        CenterFormDialog(
            isArabic = isArabic,
            initial = editing,
            savedAreas = savedAreas + centers.map { it.area.trim() }.filter { it.isNotBlank() } + listOfNotNull(editing?.area?.takeIf { it.isNotBlank() }),
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
private fun CenterAreaPickerRow(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CenterWorkingHoursTimeField(
    label: String,
    value: String,
    isArabic: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                centerDisplayWorkingHoursTime(value, isArabic),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun centerDisplayWorkingHoursTime(value: String, isArabic: Boolean): String {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val suffix = if (hour < 12) (if (isArabic) "ص" else "AM") else (if (isArabic) "م" else "PM")
    val displayHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    return String.format(java.util.Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CenterWorkingHoursTimePickerDialog(
    isArabic: Boolean,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val parts = initialValue.split(":")
    val state = androidx.compose.material3.rememberTimePickerState(
        initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
        initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0,
        is24Hour = false
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isArabic) "اختيار الوقت" else "Choose time") },
        text = { androidx.compose.material3.TimePicker(state = state) },
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CenterFormDialog(
    isArabic: Boolean,
    initial: MedicalCenterEntity?,
    savedAreas: List<String>,
    initialAvailability: List<CenterAvailabilityEntity>,
    onDismiss: () -> Unit,
    onSave: (MedicalCenterEntity, List<CenterAvailabilityEntity>) -> Unit
) {
    val context = LocalContext.current
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var area by remember(initial) { mutableStateOf(initial?.area.orEmpty()) }
    var showAreaPicker by remember { mutableStateOf(false) }
    var showNewAreaDialog by remember { mutableStateOf(false) }
    var newAreaName by remember { mutableStateOf("") }
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
    var areaError by remember { mutableStateOf(false) }
    var showWorkingHoursInfo by remember { mutableStateOf(false) }
    var workingHoursTimePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }


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
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (isArabic) "إضافة مركز أو عيادة" else "Add center or clinic" } else { if (isArabic) "تعديل المركز" else "Edit center" }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                item {
                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "اسم المركز *" else "Center name *") },
                    isError = nameError
                )
                }
                item {
                    androidx.compose.foundation.layout.Box {
                        OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = area,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth().clickable { showAreaPicker = true },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "المنطقة *" else "Area *") },
                    isError = areaError,
                    trailingIcon = { Icon(Icons.Default.ExpandMore, contentDescription = null) }
                )
                        androidx.compose.foundation.layout.Box(
                            Modifier.matchParentSize().clickable { showAreaPicker = true }
                        )
                        if (showAreaPicker) {
                            ModalBottomSheet(
                                onDismissRequest = { showAreaPicker = false },
                                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                                containerColor = MaterialTheme.colorScheme.surface
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        if (isArabic) "اختيار المنطقة" else "Choose area",
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    LazyColumn(
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        items(savedAreas.distinctBy { it.trim().lowercase(java.util.Locale.ROOT) }.sorted(), key = { it.trim().lowercase(java.util.Locale.ROOT) }) { areaOption ->
                                            CenterAreaPickerRow(areaOption) {
                                                area = areaOption
                                                areaError = false
                                                showAreaPicker = false
                                            }
                                        }
                                        item {
                                            CenterAreaPickerRow(if (isArabic) "＋ إضافة منطقة جديدة" else "＋ Add new area") {
                                                newAreaName = ""
                                                showAreaPicker = false
                                                showNewAreaDialog = true
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "العنوان" else "Address") }
                )
                }
                item {
                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = phone,
                    onValueChange = { phone = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "رقم الهاتف" else "Phone") }
                )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                        Surface(
                            onClick = { workSlots = workSlots + CenterWorkingHoursDraft() },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text(if (isArabic) "إضافة فترة عمل" else "Add working-hours slot")
                                }
                                IconButton(
                                    onClick = { showWorkingHoursInfo = true },
                                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 14.dp).size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = if (isArabic) "معلومات مواعيد العمل" else "Working hours information",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        workSlots.forEachIndexed { index, slot ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            (if (isArabic) "فترة عمل " else "Working slot ") + (index + 1),
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold
                                        )
                                        TextButton(
                                            onClick = { workSlots = workSlots.filterIndexed { i, _ -> i != index } }
                                        ) {
                                            Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                            Text(if (isArabic) "حذف" else "Remove", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                    androidx.compose.foundation.layout.FlowRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        maxItemsInEachRow = 4,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(
                                            6 to "السبت", 7 to "الأحد", 1 to "الاثنين", 2 to "الثلاثاء",
                                            3 to "الأربعاء", 4 to "الخميس", 5 to "الجمعة"
                                        ).forEach { (day, arabicLabel) ->
                                            val selected = day in slot.days
                                            Surface(
                                                modifier = Modifier.weight(1f).height(40.dp),
                                                onClick = {
                                                    val days = if (selected) slot.days - day else slot.days + day
                                                    workSlots = workSlots.toMutableList().also { it[index] = slot.copy(days = days) }
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                                )
                                            ) {
                                                Box(
                                                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        if (isArabic) arabicLabel else listOf("Saturday", "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday")[listOf(6, 7, 1, 2, 3, 4, 5).indexOf(day)].take(3),
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CenterWorkingHoursTimeField(
                                            label = if (isArabic) "من" else "From",
                                            value = slot.startTime,
                                            isArabic = isArabic,
                                            modifier = Modifier.weight(1f),
                                            onClick = { workingHoursTimePickerTarget = index to true }
                                        )
                                        CenterWorkingHoursTimeField(
                                            label = if (isArabic) "إلى" else "To",
                                            value = slot.endTime,
                                            isArabic = isArabic,
                                            modifier = Modifier.weight(1f),
                                            onClick = { workingHoursTimePickerTarget = index to false }
                                        )
                                    }
                                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = slot.notes,
                    onValueChange = { value ->
                                            workSlots = workSlots.toMutableList().also { it[index] = slot.copy(notes = value) }
                                        },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "ملاحظات الفترة (اختياري)" else "Period notes (optional)") },
                    placeholder = { Text(if (isArabic) "مثال: بالحجز فقط" else "e.g. By appointment only") }
                )
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    minLines = 1,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "ملاحظات" else "Notes") }
                )
                }
                item {
                    val hasCenterLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = AppCardContainerColor()
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
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (hasCenterLocation) {
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) },
                                            shape = RoundedCornerShape(10.dp)
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
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(if (isArabic) "مسح الموقع" else "Clear location")
                                        }
                                        OutlinedButton(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { editCenterLocation = !editCenterLocation },
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                        }
                                    }
                                    AnimatedLocationActions(visible = !hasCenterLocation || editCenterLocation) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            CurrentLocationButton(
                                                isArabic = isArabic,
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                            ) { lat, lon ->
                                                latitude = lat
                                                longitude = lon
                                                mapUrl = null
                                                editCenterLocation = false
                                            }
                                            OutlinedButton(
                                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                                onClick = { showMapPicker = true },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                                )
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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                if (name.isBlank()) nameError = true
                else if (area.isBlank()) areaError = true
                else onSave(
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
                        area = area.trim(),
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
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isArabic) "حفظ" else "Save") }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(10.dp)) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        },
        dismissButton = { }
    )

    if (showNewAreaDialog) {
        AlertDialog(
            onDismissRequest = { showNewAreaDialog = false },
            title = { Text(if (isArabic) "إضافة منطقة جديدة" else "Add new area") },
            text = {
                OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = newAreaName,
                    onValueChange = { newAreaName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    label = { Text(if (isArabic) "اسم المنطقة" else "Area name") }
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newAreaName.isNotBlank(),
                    onClick = {
                        area = newAreaName.trim()
                        areaError = false
                        showNewAreaDialog = false
                    }
                ) { Text(if (isArabic) "إضافة" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showNewAreaDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    workingHoursTimePickerTarget?.let { (slotIndex, isStart) ->
        val slot = workSlots.getOrNull(slotIndex)
        if (slot != null) {
            CenterWorkingHoursTimePickerDialog(
                isArabic = isArabic,
                initialValue = if (isStart) slot.startTime else slot.endTime,
                onDismiss = { workingHoursTimePickerTarget = null },
                onConfirm = { hour, minute ->
                    val value = String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
                    workSlots = workSlots.toMutableList().also { list ->
                        val current = list[slotIndex]
                        list[slotIndex] = if (isStart) current.copy(startTime = value) else current.copy(endTime = value)
                    }
                    workingHoursTimePickerTarget = null
                }
            )
        }
    }

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