package com.medicalrep.manager.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppDialogContainerColor
import com.medicalrep.manager.ui.theme.AppCardContainerColor
import com.medicalrep.manager.ui.theme.AppSelectionRowColor
import com.medicalrep.manager.ui.theme.AppLargeCardBorder
import com.medicalrep.manager.ui.theme.AppLargeCardContainerColor

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.room.withTransaction
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.medicalrep.manager.data.DoctorAvailabilityEntity
import com.medicalrep.manager.data.DoctorCenterLinkEntity
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.domain.ContactPhoneRules
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.data.SpecialtyEntity
import com.medicalrep.manager.util.ImageCompression
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private fun doctorAreas(value: String): List<String> =
    value.split("|").map { it.trim() }.filter { it.isNotBlank() }.distinctBy { it.lowercase(Locale.ROOT) }

private fun lastVisitRelativeLabel(visitedAt: Long, now: Long = System.currentTimeMillis()): String {
    val zone = java.util.TimeZone.getDefault()
    val today = java.util.Calendar.getInstance().apply {
        timeInMillis = now
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val visitDay = java.util.Calendar.getInstance().apply {
        timeInMillis = visitedAt
        set(java.util.Calendar.HOUR_OF_DAY, 0)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
    }
    val days = ((today.timeInMillis - visitDay.timeInMillis +
        zone.getOffset(today.timeInMillis) - zone.getOffset(visitDay.timeInMillis)) / 86_400_000L).toInt()
    val count = java.text.NumberFormat.getIntegerInstance(Locale("ar"))
    return when {
        days <= 0 -> "اليوم"
        days == 1 -> "أمس"
        days < 7 -> "من ${count.format(days)} أيام"
        days < 30 -> "من ${count.format(days / 7)} أسابيع"
        else -> "من ${count.format(days / 30)} شهور"
    }
}

private data class DoctorAvatarPalette(
    val lightBackground: Color,
    val lightForeground: Color,
    val darkBackground: Color,
    val darkForeground: Color
)

private val doctorAvatarPalettes = listOf(
    DoctorAvatarPalette(Color(0xFFE9DFF5), Color(0xFF49365F), Color(0xFF55436D), Color(0xFFF2E9FF)),
    DoctorAvatarPalette(Color(0xFFDCEAF7), Color(0xFF294B68), Color(0xFF36536D), Color(0xFFE4F3FF)),
    DoctorAvatarPalette(Color(0xFFDDF0E7), Color(0xFF285844), Color(0xFF315B4A), Color(0xFFE3F8EC)),
    DoctorAvatarPalette(Color(0xFFF8E7D6), Color(0xFF6A4328), Color(0xFF70513A), Color(0xFFFFF0E0)),
    DoctorAvatarPalette(Color(0xFFF5DFE5), Color(0xFF6A3448), Color(0xFF6A3E50), Color(0xFFFFEAF0)),
    DoctorAvatarPalette(Color(0xFFE3E8F7), Color(0xFF35456F), Color(0xFF3E4D78), Color(0xFFEAF0FF)),
    DoctorAvatarPalette(Color(0xFFE8EBD5), Color(0xFF4D542D), Color(0xFF555C35), Color(0xFFF4F6DD)),
    DoctorAvatarPalette(Color(0xFFDDECEF), Color(0xFF31545A), Color(0xFF34575D), Color(0xFFE4F8FA)),
    DoctorAvatarPalette(Color(0xFFF1E3D4), Color(0xFF62482E), Color(0xFF66503A), Color(0xFFFFF1DE)),
    DoctorAvatarPalette(Color(0xFFE8E0D8), Color(0xFF514438), Color(0xFF55483B), Color(0xFFF6EDE3))
)

private fun doctorAvatarColors(name: String, isDarkTheme: Boolean): Pair<Color, Color> {
    val index = Math.floorMod(name.trim().lowercase(Locale.ROOT).hashCode(), doctorAvatarPalettes.size)
    val palette = doctorAvatarPalettes[index]
    return if (isDarkTheme) {
        palette.darkBackground to palette.darkForeground
    } else {
        palette.lightBackground to palette.lightForeground
    }
}

@Composable
private fun DoctorPickerBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(AppMotion.Fast)),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(animationSpec = tween(AppMotion.Fast))
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth().heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.78f).dp),
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = AppDialogContainerColor(),
                    tonalElevation = 8.dp
                ) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(title, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        content()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorFormBaseBottomSheet(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = AppDialogContainerColor()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            content()
        }
    }
}
@Composable
private fun CompactOutlinedRemovableItem(
    text: String,
    contentDescription: String,
    onRemove: () -> Unit,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = contentDescription,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
internal fun FormExpandableSection(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = AppLargeCardContainerColor(),
        border = AppLargeCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(AppMotion.Emphasized)) + fadeIn(animationSpec = tween(AppMotion.Standard)),
                exit = shrinkVertically(animationSpec = tween(AppMotion.Standard)) + fadeOut(animationSpec = tween(AppMotion.Fast))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = AppSpacing.lg, end = AppSpacing.lg, bottom = AppSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun FilterActionButton(isArabic: Boolean, expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val iconColor = MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier.size(60.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.size(28.dp)) {
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
private fun DoctorSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    isArabic: Boolean,
    compact: Boolean,
    onExpand: () -> Unit = {}
) {
    val height by animateDpAsState(if (compact) 44.dp else 60.dp, tween(AppMotion.Emphasized), label = "searchHeight")
    val textAlpha by animateFloatAsState(if (compact) 0f else 1f, tween(AppMotion.Fast), label = "searchTextAlpha")
    Surface(
        onClick = if (compact) onExpand else ({ }),
        shape = RoundedCornerShape(10.dp),
        color = AppCardContainerColor(),
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
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) Text(
                                if (isArabic) "ابحث عن طبيب..." else "Search doctors...",
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
fun DoctorsScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onManageSpecialties: () -> Unit = {}) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val dao = database.doctorDao()
    val allDoctors by dao.observeAll().collectAsState(initial = emptyList())
    val centerDao = database.medicalCenterDao()
    val specialtyDao = database.specialtyDao()
    val availability by database.doctorAvailabilityDao().observeAll().collectAsState(initial = emptyList())
    val doctorCenterLinks by database.doctorCenterLinkDao().observeAll().collectAsState(initial = emptyList())
    val visits by database.doctorVisitDao().observeAll().collectAsState(initial = emptyList())
    val pinnedPreferences = remember(context) {
        context.getSharedPreferences("doctor_pins", android.content.Context.MODE_PRIVATE)
    }
    var pinnedDoctorIds by remember(pinnedPreferences) {
        mutableStateOf(pinnedPreferences.getStringSet("pinned_doctor_ids", emptySet()).orEmpty().toSet())
    }
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
    val availableAreas = remember(allDoctors) {
        allDoctors.flatMap { doctorAreas(it.area) }
            .distinctBy { it.lowercase(Locale.ROOT) }.sorted()
    }
    val centerIdsByDoctor = remember(doctorCenterLinks) {
        doctorCenterLinks.groupBy({ it.doctorId }, { it.centerId })
    }
    val centerNameByDoctor = remember(centers, centerIdsByDoctor, allDoctors) {
        allDoctors.associate { doctor ->
            val linkedIds = centerIdsByDoctor[doctor.id].orEmpty().toSet()
            val names = centers.filter { center ->
                center.id in linkedIds || (linkedIds.isEmpty() && center.id == doctor.centerId)
            }.joinToString("، ") { it.name }
            doctor.id to names.ifBlank { doctor.medicalCenter }
        }
    }
    val filteredDoctors = remember(doctors, selectedArea, selectedSpecialty, selectedCenter, centerIdsByDoctor, pinnedDoctorIds) {
        doctors.filter { doctor ->
            (selectedArea == "all" || doctorAreas(doctor.area).any { it.equals(selectedArea, ignoreCase = true) }) &&
            (selectedSpecialty == "all" || doctor.specialty.equals(selectedSpecialty, ignoreCase = true)) &&
            (selectedCenter == "all" || (centerIdsByDoctor[doctor.id]?.any { it.toString() == selectedCenter }
                ?: (doctor.centerId?.toString() == selectedCenter)))
        }.sortedByDescending { it.id.toString() in pinnedDoctorIds }
    }
    var editingDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<DoctorEntity?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var attachmentsDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var detailsDoctor by remember { mutableStateOf<DoctorEntity?>(null) }
    var showSpecialtiesManager by remember { mutableStateOf(false) }
    var showAddSpecialtyDialog by remember { mutableStateOf(false) }
    var newSpecialtyName by remember { mutableStateOf("") }
    var specialtyPendingDelete by remember { mutableStateOf<SpecialtyEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
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
        val scrollDirectionThreshold = with(LocalDensity.current) { 8.dp.roundToPx() }
        LaunchedEffect(listState) {
            var previousPosition: Int? = null
            var accumulatedDelta = 0
            snapshotFlow {
                listState.layoutInfo.visibleItemsInfo
                    .firstOrNull { it.index > 1 }
                    ?.let { it.index * 100000 - it.offset }
                    ?: 0
            }
                .distinctUntilChanged()
                .collect { position ->
                    val previous = previousPosition
                    if (previous == null) {
                        // Ignore the initial layout measurement so the search starts expanded.
                        previousPosition = position
                        if (position == 0) searchHeaderExpanded = true
                        return@collect
                    }

                    val delta = position - previous
                    accumulatedDelta += delta
                    if (accumulatedDelta >= scrollDirectionThreshold) {
                        searchHeaderExpanded = false
                        accumulatedDelta = 0
                    } else if (accumulatedDelta <= -scrollDirectionThreshold) {
                        searchHeaderExpanded = true
                        accumulatedDelta = 0
                    }
                    if (position == 0) {
                        searchHeaderExpanded = true
                        accumulatedDelta = 0
                    }
                    previousPosition = position
                }
        }
        val fadeHeaderOffset = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }?.offset ?: Int.MAX_VALUE
        val fadeStartOffset = with(LocalDensity.current) { 48.dp.roundToPx() }
        val fadeAlpha by animateFloatAsState(
            targetValue = ((fadeStartOffset - fadeHeaderOffset).toFloat() / fadeStartOffset).coerceIn(0f, 1f),
            animationSpec = tween(AppMotion.Emphasized),
            label = "doctorsFadeAlpha"
        )
        val doctorsFadeColor = MaterialTheme.colorScheme.background
        Column(modifier = Modifier.fillMaxSize().padding(inner)) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().drawWithContent {
                drawContent()
                val stickyHeader = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 1 }
                if (stickyHeader != null && fadeAlpha > 0f) {
                    val fadeTop = (stickyHeader.offset + stickyHeader.size).toFloat()
                    val solidFadeHeight = 20.dp.toPx()
                    val fadeHeight = 72.dp.toPx()
                    drawRect(
                        color = doctorsFadeColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, fadeTop),
                        size = androidx.compose.ui.geometry.Size(size.width, solidFadeHeight)
                    )
                    drawRect(
                        brush = Brush.verticalGradient(
                            0f to doctorsFadeColor,
                            0.65f to doctorsFadeColor.copy(alpha = 0.96f),
                            1f to doctorsFadeColor.copy(alpha = 0f),
                            startY = fadeTop + solidFadeHeight,
                            endY = fadeTop + fadeHeight
                        ),
                        topLeft = androidx.compose.ui.geometry.Offset(0f, fadeTop + solidFadeHeight),
                        size = androidx.compose.ui.geometry.Size(size.width, fadeHeight - solidFadeHeight)
                    )
                }
            }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                item {
                    Row(Modifier.fillMaxWidth().padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(if (isArabic) "إدارة الأطباء" else "Doctors", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedButton(
                            onClick = { showSpecialtiesManager = true },
                            shape = MaterialTheme.shapes.small,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Default.MedicalServices, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isArabic) "إدارة التخصصات" else "Manage specialties", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
                stickyHeader {
                    BoxWithConstraints(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background).padding(vertical = 4.dp)) {
                        val filterWidth by animateDpAsState(if (searchHeaderExpanded) 60.dp else 0.dp, tween(AppMotion.Emphasized), label = "filterWidth")
                        val filterAlpha by animateFloatAsState(if (searchHeaderExpanded) 1f else 0f, tween(AppMotion.Fast), label = "filterAlpha")
                        val searchWidth by animateDpAsState(if (searchHeaderExpanded) maxWidth - 68.dp else 44.dp, tween(AppMotion.Emphasized), label = "searchWidth")
                        val controlHeight by animateDpAsState(if (searchHeaderExpanded) 60.dp else 44.dp, tween(AppMotion.Emphasized), label = "searchControlHeight")
                        Row(
                            Modifier.fillMaxWidth().height(controlHeight),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.width(searchWidth).height(controlHeight)) {
                                DoctorSearchField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    isArabic = isArabic,
                                    compact = !searchHeaderExpanded,
                                    onExpand = { searchHeaderExpanded = true }
                                )
                            }
                            Box(Modifier.width(filterWidth).height(controlHeight).clipToBounds()) {
                                if (searchHeaderExpanded) FilterActionButton(
                                    isArabic = isArabic,
                                    expanded = showFilters,
                                    onClick = { showFilters = !showFilters },
                                    modifier = Modifier.alpha(filterAlpha)
                                )
                            }
                        }
                    }
                }
                if (selectedArea != "all" || selectedSpecialty != "all" || selectedCenter != "all") {
                    item {
                        Text(
                            if (isArabic) "${filteredDoctors.size} من ${doctors.size} طبيب" else "${filteredDoctors.size} of ${doctors.size} doctors",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 8.dp)
                        )
                    }
                }
                item {
                    AnimatedVisibility(visible = showFilters, enter = expandVertically(tween(AppMotion.Emphasized)) + fadeIn(tween(AppMotion.Standard)), exit = shrinkVertically(tween(AppMotion.Standard)) + fadeOut(tween(AppMotion.Fast))) {
                        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.weight(1f)) {
                                    OutlinedSelectionField(label = if (isArabic) "المنطقة" else "Area", value = if (selectedArea == "all") { if (isArabic) "كل المناطق" else "All areas" } else selectedArea, onClick = { areaMenu = true })

                                }
                                Column(Modifier.weight(1f)) {
                                    OutlinedSelectionField(label = if (isArabic) "التخصص" else "Specialty", value = if (selectedSpecialty == "all") { if (isArabic) "كل التخصصات" else "All specialties" } else selectedSpecialty, onClick = { specialtyMenu = true })

                                }
                            }
                            OutlinedSelectionField(label = if (isArabic) "المركز" else "Center", value = if (selectedCenter == "all") { if (isArabic) "كل المراكز" else "All centers" } else centers.firstOrNull { it.id.toString() == selectedCenter }?.name.orEmpty(), onClick = { centerMenu = true })

                        }
                    }
                }
                if (filteredDoctors.isEmpty()) item { Text(if (doctors.isEmpty()) { if (isArabic) "لا يوجد أطباء حتى الآن. اضغط + لإضافة أول طبيب." else "No doctors yet. Tap + to add your first doctor." } else { if (isArabic) "لا توجد نتائج مطابقة للفلاتر المحددة." else "No doctors match the selected filters." }, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp)) }
                else items(filteredDoctors, key = { it.id }) { doctor ->
                    DoctorCard(
                        doctor = doctor,
                        centerName = centerNameByDoctor[doctor.id].orEmpty(),
                        lastVisitAt = visits.asSequence().filter { it.doctorId == doctor.id }.maxOfOrNull { it.visitedAt },
                        isPinned = doctor.id.toString() in pinnedDoctorIds,
                        isArabic = isArabic,
                        onTogglePin = {
                            val doctorIdKey = doctor.id.toString()
                            val updatedPinnedIds = if (doctorIdKey in pinnedDoctorIds) {
                                pinnedDoctorIds - doctorIdKey
                            } else {
                                pinnedDoctorIds + doctorIdKey
                            }
                            pinnedDoctorIds = updatedPinnedIds
                            pinnedPreferences.edit()
                                .putStringSet("pinned_doctor_ids", updatedPinnedIds.toSet())
                                .apply()
                        },
                        onEdit = { editingDoctor = doctor; showForm = true },
                        onDelete = { pendingDelete = doctor },
                        onAttachments = { attachmentsDoctor = doctor },
                        onOpen = { detailsDoctor = doctor }
                    )
                }
            }
        }    }
    if (areaMenu) {
        AppSelectionPickerSheet(
            title = if (isArabic) "اختيار المنطقة" else "Choose area",
            allOptionLabel = if (isArabic) "كل المناطق" else "All areas",
            allOptionValue = "all",
            options = availableAreas.map { it to it },
            searchPlaceholder = if (isArabic) "ابحث عن منطقة..." else "Search areas...",
            noResultsLabel = if (isArabic) "لا توجد نتائج" else "No results",
            onDismiss = { areaMenu = false },
            onSelect = { selectedArea = it; areaMenu = false }
        )
    }
    if (specialtyMenu) {
        AppSelectionPickerSheet(
            title = if (isArabic) "اختيار التخصص" else "Choose specialty",
            allOptionLabel = if (isArabic) "كل التخصصات" else "All specialties",
            allOptionValue = "all",
            options = specialties.map { it.name }
                .plus(doctors.map { it.specialty })
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .map { it to it },
            searchPlaceholder = if (isArabic) "ابحث عن تخصص..." else "Search specialties...",
            noResultsLabel = if (isArabic) "لا توجد نتائج" else "No results",
            onDismiss = { specialtyMenu = false },
            onSelect = { selectedSpecialty = it; specialtyMenu = false }
        )
    }
    if (centerMenu) {
        AppSelectionPickerSheet(
            title = if (isArabic) "اختيار المركز" else "Choose center",
            allOptionLabel = if (isArabic) "كل المراكز" else "All centers",
            allOptionValue = "all",
            options = centers.map { it.id.toString() to it.name },
            searchPlaceholder = if (isArabic) "ابحث عن مركز..." else "Search centers...",
            noResultsLabel = if (isArabic) "لا توجد نتائج" else "No results",
            onDismiss = { centerMenu = false },
            onSelect = { selectedCenter = it; centerMenu = false }
        )
    }

    if (showForm) {
        DoctorFormDialog(
            isArabic = isArabic,
            initial = editingDoctor,
            centers = centers,
            specialties = specialties,
            existingAreas = availableAreas,
            initialCenterIds = editingDoctor?.let { doctor -> doctorCenterLinks.filter { it.doctorId == doctor.id }.map { it.centerId }.toSet() }.orEmpty(),
            initialAvailability = editingDoctor?.let { doctor -> availability.filter { it.doctorId == doctor.id } }.orEmpty(),
            onDismiss = { showForm = false },
            onDeleteArea = { areaToDelete ->
                scope.launch {
                    allDoctors.filter { d -> doctorAreas(d.area).any { it.equals(areaToDelete, ignoreCase = true) } }.forEach { d ->
                        dao.update(d.copy(area = doctorAreas(d.area).filterNot { it.equals(areaToDelete, ignoreCase = true) }.joinToString("|")))
                    }
                }
            },
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
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف الطبيب؟" else "Delete doctor?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف بيانات ${doctor.name} من هذا الجهاز." else "${doctor.name}'s data will be deleted from this device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = if (isArabic) "سيتم حذف ${doctor.name}" else "${doctor.name} will be deleted",
                            actionLabel = if (isArabic) "تراجع" else "Undo",
                            duration = SnackbarDuration.Short
                        )
                        if (result != SnackbarResult.ActionPerformed) {
                            val attachments = database.doctorAttachmentDao().getForDoctor(doctor.id)
                            withContext(Dispatchers.IO) {
                                attachments.forEach { File(it.localPath).delete() }
                            }
                            dao.delete(doctor)
                        }
                    }
                }) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
    if (showSpecialtiesManager) {
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showSpecialtiesManager = false },
            title = { Text(if (isArabic) "إدارة التخصصات" else "Manage specialties") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (specialties.isEmpty()) {
                        Text(
                            if (isArabic) "لا توجد تخصصات مضافة حتى الآن." else "No specialties added yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                            items(specialties, key = { it.id }) { specialty ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(specialty.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                                    IconButton(onClick = { specialtyPendingDelete = specialty }) {
                                        Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف التخصص" else "Delete specialty", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                                androidx.compose.material3.HorizontalDivider()
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        newSpecialtyName = ""
                        showAddSpecialtyDialog = true
                    }) {
                        Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة تخصص" else "Add specialty", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    }
                    TextButton(onClick = { showSpecialtiesManager = false }) { Text(if (isArabic) "إغلاق" else "Close") }
                }
            }
        )
    }

    if (showAddSpecialtyDialog) {
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showAddSpecialtyDialog = false },
            title = { Text(if (isArabic) "إضافة تخصص جديد" else "Add new specialty") },
            text = {
                OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = newSpecialtyName,
                    onValueChange = { newSpecialtyName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "اسم التخصص" else "Specialty name") }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val name = newSpecialtyName.trim()
                    if (name.isNotBlank() && specialties.none { it.name.equals(name, true) }) {
                        scope.launch {
                            specialtyDao.insert(SpecialtyEntity(id = 0L, name = name, createdAt = System.currentTimeMillis()))
                        }
                        showAddSpecialtyDialog = false
                    }
                }) { Text(if (isArabic) "إضافة" else "Add") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSpecialtyDialog = false }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    specialtyPendingDelete?.let { specialty ->
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { specialtyPendingDelete = null },
            title = { Text(if (isArabic) "حذف التخصص؟" else "Delete specialty?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف «${specialty.name}» وفصل الأطباء المرتبطين به، دون حذف بياناتهم."
                    else "This removes “${specialty.name}” and unlinks its doctors without deleting their records."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.withTransaction {
                            dao.detachSpecialty(specialty.id)
                            specialtyDao.delete(specialty)
                        }
                    }
                    specialtyPendingDelete = null
                }) { Text(if (isArabic) "حذف" else "Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { specialtyPendingDelete = null }) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }
}

@Composable
private fun DoctorCard(
    doctor: DoctorEntity,
    centerName: String,
    lastVisitAt: Long?,
    isPinned: Boolean,
    isArabic: Boolean,
    onTogglePin: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAttachments: () -> Unit,
    onOpen: () -> Unit
) {
    val context = LocalContext.current
    val firstPhone = doctor.phoneNumbers
        .split(Regex("\\r?\\n|\\\\n"))
        .firstOrNull { it.trim().isNotEmpty() }
        ?.trim()
    val phoneDigits = firstPhone.orEmpty().filter { it.isDigit() }
    val whatsappNumber = when {
        phoneDigits.startsWith("0") -> "20" + phoneDigits.drop(1)
        else -> phoneDigits
    }
    val hasPhone = firstPhone != null && phoneDigits.isNotEmpty()
    var expanded by remember(doctor.id) { mutableStateOf(false) }
    val cardInteractionSource = remember(doctor.id) { MutableInteractionSource() }
    val cardPressed by cardInteractionSource.collectIsPressedAsState()
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (cardPressed) 0.99f else 1f,
        animationSpec = androidx.compose.animation.core.spring(dampingRatio = 0.78f, stiffness = 850f),
        label = "doctorCardPressScale"
    )
    val expandRotation by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = androidx.compose.animation.core.tween(AppMotion.Standard),
        label = "doctorExpandRotation"
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .clickable(
                interactionSource = cardInteractionSource,
                indication = LocalIndication.current,
                onClick = onOpen
            ),
        shape = MaterialTheme.shapes.small,
        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = AppSpacing.md),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (doctor.photoUri.isNullOrBlank()) {
                    val avatarInitial = doctor.name.trim().firstOrNull()?.toString()?.uppercase(Locale.ROOT)
                    if (avatarInitial.isNullOrEmpty()) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                    } else {
                        val (avatarBackground, avatarForeground) = doctorAvatarColors(
                            doctor.name,
                            MaterialTheme.colorScheme.background.luminance() < 0.5f
                        )
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = avatarBackground
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    avatarInitial,
                                    color = avatarForeground,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    AsyncImage(
                        model = doctor.photoUri,
                        contentDescription = if (isArabic) "صورة الطبيب" else "Doctor photo",
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(doctor.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
                    if (doctor.specialty.isNotBlank()) {
                        Text(doctor.specialty, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                    if (doctor.area.isNotBlank()) {
                        Text(
                            (if (isArabic) "المنطقة: " else "Area: ") + doctorAreas(doctor.area).joinToString("، "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = onTogglePin
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = if (isArabic) {
                            if (isPinned) "إزالة التثبيت" else "تثبيت الطبيب"
                        } else {
                            if (isPinned) "Unpin doctor" else "Pin doctor"
                        },
                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    modifier = Modifier.size(32.dp),
                    onClick = { expanded = !expanded }
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
                horizontalArrangement = Arrangement.spacedBy(2.dp, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(modifier = Modifier.size(32.dp), onClick = onAttachments) {
                    Icon(Icons.Default.AttachFile, contentDescription = if (isArabic) "المرفقات" else "Attachments", modifier = Modifier.size(18.dp))
                }
                IconButton(modifier = Modifier.size(32.dp), onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(modifier = Modifier.size(32.dp), onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(18.dp))
                }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = androidx.compose.animation.core.tween(AppMotion.Emphasized)) +
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(AppMotion.Standard)),
                exit = shrinkVertically(animationSpec = androidx.compose.animation.core.tween(AppMotion.Standard)) +
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(AppMotion.Fast))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(start = 52.dp, top = 2.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        if (lastVisitAt != null) {
                            "آخر زيارة: ${lastVisitRelativeLabel(lastVisitAt)}"
                        } else {
                            "لا توجد زيارات"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (doctor.phoneNumbers.isNotBlank()) {
                        Text(
                            (if (isArabic) "الهاتف: " else "Phone: ") + doctor.phoneNumbers.replace("\\n", " • "),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (centerName.isNotBlank()) {
                        Text(
                            (if (isArabic) "المركز: " else "Center: ") + centerName,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (doctor.notes.isNotBlank()) {
                        Text(doctor.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}
                        ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$firstPhone")))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, if (isArabic) "لا يوجد تطبيق للاتصال" else "No dialer app available", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, if (isArabic) "تعذر فتح الاتصال" else "Unable to open dialer", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasPhone,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (isArabic) "اتصال" else "Call", maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$whatsappNumber")))
                                } catch (_: ActivityNotFoundException) {
                                    Toast.makeText(context, if (isArabic) "تعذر فتح واتساب أو المتصفح" else "Unable to open WhatsApp or browser", Toast.LENGTH_SHORT).show()
                                } catch (_: Exception) {
                                    Toast.makeText(context, if (isArabic) "تعذر فتح واتساب" else "Unable to open WhatsApp", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = hasPhone,
                            modifier = Modifier.weight(1f).height(40.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text(if (isArabic) "واتساب" else "WhatsApp", maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DoctorDetailsDialog(
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
        containerColor = AppDialogContainerColor(),
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
                        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                    ) {
                        Column(Modifier.fillMaxWidth().padding(AppSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
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
                            colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(center.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (center.address.isNotBlank()) {
                                    Text(if (isArabic) "العنوان" else "Address", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(center.address)
                                }
                                if (center.phone.isNotBlank()) {
                                    Text(if (isArabic) "الهاتف" else "Phone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                    Text(ContactPhoneRules.split(center.phone).joinToString(" • "))
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
                                        Text("$dayNames  •  ${doctorDisplayTime(first.startTime, isArabic)} – ${doctorDisplayTime(first.endTime, isArabic)}", style = MaterialTheme.typography.bodyMedium)
                                        if (first.notes.isNotBlank()) Text(first.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
                if (doctor.notes.isNotBlank()) item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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
        containerColor = AppDialogContainerColor(),
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
        containerColor = AppDialogContainerColor(),
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


@Composable
private fun DoctorTimeSelectionField(
    label: String,
    value: String,
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
                value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun doctorDisplayTime(value: String, isArabic: Boolean): String {
    val parts = value.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val minute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val suffix = if (hour < 12) (if (isArabic) "ص" else "AM") else (if (isArabic) "م" else "PM")
    val displayHour = when (hour % 12) { 0 -> 12; else -> hour % 12 }
    return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, suffix)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorAvailabilityTimePickerDialog(
    isArabic: Boolean,
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val parts = initialValue.split(":")
    val initialHour = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(0, 23) ?: 9
    val initialMinute = parts.getOrNull(1)?.toIntOrNull()?.coerceIn(0, 59) ?: 0
    val state = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = AppDialogContainerColor(),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabic) "اختيار الوقت" else "Choose time",
                    style = MaterialTheme.typography.titleLarge
                )
                TimePicker(state = state)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isArabic) "إلغاء" else "Cancel")
                    }
                    TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                        Text(if (isArabic) "تأكيد" else "Confirm")
                    }
                }
            }
        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DoctorFormDialog(
    isArabic: Boolean,
    initial: DoctorEntity?,
    centers: List<MedicalCenterEntity>,
    specialties: List<SpecialtyEntity>,
    existingAreas: List<String>,
    onDeleteArea: (String) -> Unit,
    initialCenterIds: Set<Long>,
    initialAvailability: List<DoctorAvailabilityEntity>,
    onDismiss: () -> Unit,
    onSave: (DoctorEntity, Set<Long>, List<DoctorAvailabilityEntity>) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var photoCompressing by remember { mutableStateOf(false) }
    var photoError by remember { mutableStateOf<String?>(null) }
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var selectedSpecialtyId by remember(initial, specialties) { mutableStateOf(initial?.specialtyId ?: specialties.firstOrNull { it.name == initial?.specialty }?.id) }
    var showSpecialtyPicker by remember { mutableStateOf(false) }
    var phoneNumbers by remember(initial) { mutableStateOf(initial?.phoneNumbers.orEmpty().split(Regex("\\r?\\n|\\\\n")).map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { listOf("") }) }
    var phonesExpanded by remember { mutableStateOf(false) }
    var selectedAreas by remember(initial) { mutableStateOf(doctorAreas(initial?.area.orEmpty()).toSet()) }
    var areaOptions by remember(existingAreas) { mutableStateOf(existingAreas.distinct()) }
    var newArea by remember { mutableStateOf("") }
    var showAreaPicker by remember { mutableStateOf(false) }
    var selectedCenterIds by remember(initial, initialCenterIds, centers) { mutableStateOf(initialCenterIds.ifEmpty { setOfNotNull(initial?.centerId ?: centers.firstOrNull { it.name == initial?.medicalCenter }?.id) }) }
    var showCenterPicker by remember { mutableStateOf(false) }
    var centerSearchExpanded by remember { mutableStateOf(false) }
    var centerSearchQuery by remember { mutableStateOf("") }
    var centersExpanded by remember { mutableStateOf(false) }
    var availabilityExpanded by remember(initial, initialAvailability) { mutableStateOf(initialAvailability.isNotEmpty()) }
    var editingAvailabilityIndex by remember(initial, initialAvailability) { mutableStateOf<Int?>(null) }
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
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var photoUri by remember(initial) { mutableStateOf(initial?.photoUri) }
    var latitude by remember(initial) { mutableStateOf(initial?.latitude) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitude) }
    var mapUrl by remember(initial) { mutableStateOf(initial?.mapUrl) }
    var showDoctorMapPicker by remember { mutableStateOf(false) }
    var editDoctorLocation by remember { mutableStateOf(false) }
    var locationExpanded by remember(initial) { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var availabilityError by remember { mutableStateOf<String?>(null) }
    var availabilityTimePickerTarget by remember { mutableStateOf<Pair<Int, Boolean>?>(null) }
    var showAreaRequiredDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                photoCompressing = true
                photoError = null
                try {
                    val compressedFile = runCatching {
                        withContext(Dispatchers.IO) {
                            val originalSize = ImageCompression.uriLength(context, uri)
                            val directory = File(context.filesDir, "doctor_photos").apply { mkdirs() }
                            val output = File(directory, "doctor_${initial?.id ?: "new"}_${UUID.randomUUID()}.jpg")
                            if (originalSize != null &&
                                ImageCompression.compressUriToJpeg(context, uri, output, 1000, 80) &&
                                output.length() < originalSize
                            ) output else { output.delete(); null }
                        }
                    }.getOrNull()
                    if (compressedFile != null) {
                        photoUri = Uri.fromFile(compressedFile).toString()
                    } else {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (_: SecurityException) {
                        } catch (_: IllegalArgumentException) {
                        }
                        photoUri = uri.toString()
                        photoError = if (isArabic) {
                            "تعذر ضغط الصورة؛ تم الاحتفاظ بالصورة الأصلية."
                        } else {
                            "Could not compress the photo; the original was kept."
                        }
                    }
                } finally {
                    photoCompressing = false
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AppDialogContainerColor(),
        title = { Text(if (initial == null) { if (isArabic) "إضافة طبيب" else "Add doctor" } else { if (isArabic) "تعديل بيانات الطبيب" else "Edit doctor" }, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (photoUri.isNullOrBlank()) {
                            Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
                        } else {
                            AsyncImage(
                                model = photoUri,
                                contentDescription = if (isArabic) "صورة الطبيب" else "Doctor photo",
                                modifier = Modifier.size(72.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                enabled = !photoCompressing,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(if (isArabic) "اختيار صورة" else "Choose photo")
                            }
                            if (!photoUri.isNullOrBlank()) {
                                TextButton(onClick = { photoUri = null }, enabled = !photoCompressing) {
                                    Text(if (isArabic) "إزالة الصورة" else "Remove photo")
                                }
                            }
                        }
                        if (photoCompressing) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text(if (isArabic) "جارٍ ضغط الصورة..." else "Compressing photo...", modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                        photoError?.let {
                            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item {
                    OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    singleLine = true,
                    label = { Text(if (isArabic) "اسم الطبيب" else "Doctor name") },
                    isError = nameError
                )
                }
                item {
                    OutlinedSelectionField(
                        label = if (isArabic) "التخصص" else "Specialty",
                        value = specialties.firstOrNull { it.id == selectedSpecialtyId }?.name
                            ?: "",
                        onClick = { showSpecialtyPicker = true }
                    )
                }
                item {
                    FormExpandableSection(
                        title = if (isArabic) "أرقام الهاتف" else "Phone numbers",
                        expanded = phonesExpanded,
                        onToggle = { phonesExpanded = !phonesExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            phoneNumbers.forEachIndexed { index, phone ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color.Transparent,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                    modifier = Modifier.fillMaxWidth().height(40.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(40.dp)
                                            .padding(horizontal = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        BasicTextField(
                                            value = phone,
                                            onValueChange = { value ->
                                                phoneNumbers = phoneNumbers.toMutableList().also { it[index] = value }
                                            },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                                color = MaterialTheme.colorScheme.onSurface
                                            ),
                                            decorationBox = { innerTextField ->
                                                Box(contentAlignment = Alignment.CenterStart) {
                                                    if (phone.isEmpty()) {
                                                        Text(
                                                            if (isArabic) "رقم الهاتف ${index + 1}" else "Phone number ${index + 1}",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                    innerTextField()
                                                }
                                            }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clickable {
                                                    phoneNumbers = phoneNumbers.toMutableList().also { list ->
                                                        if (list.size > 1) list.removeAt(index) else list[0] = ""
                                                    }
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.Delete,
                                                contentDescription = if (isArabic) "حذف الرقم" else "Delete number",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { phoneNumbers = phoneNumbers + "" },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(if (isArabic) "إضافة رقم هاتف" else "Add phone number")
                            }
                        }
                    }
                }
                item {
                    OutlinedSelectionField(
                        label = if (isArabic) "المنطقة" else "Area",
                        value = if (selectedAreas.isEmpty()) {
                            ""
                        } else selectedAreas.sorted().joinToString("، "),
                        onClick = { showAreaPicker = true }
                    )
                }
                item {
                    FormExpandableSection(
                        title = if (isArabic) "مراكز عمل الطبيب" else "Doctor's centers",
                        expanded = centersExpanded,
                        onToggle = { centersExpanded = !centersExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { showCenterPicker = true },
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null)
                                Text(if (isArabic) "إضافة مركز" else "Add center")
                            }
                            centers.filter { it.id in selectedCenterIds }.forEach { center ->
                                CompactOutlinedRemovableItem(
                                    text = center.name,
                                    contentDescription = if (isArabic) "إزالة المركز" else "Remove center",
                                    onRemove = { selectedCenterIds = selectedCenterIds - center.id }
                                )
                            }
                        }
                    }
                }
                item {
                    FormExpandableSection(
                        title = if (isArabic) "مواعيد تواجد الطبيب" else "Doctor availability",
                        expanded = availabilityExpanded,
                        onToggle = { availabilityExpanded = !availabilityExpanded }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                if (isArabic) "كل فترة مستقلة: عامة أو مرتبطة بمركز من المراكز المختارة." else "Each slot is independent: general or linked to one of the selected centers.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            availabilityDrafts.forEachIndexed { index, draft ->
                                val dayNames = draft.days.sorted().joinToString(if (isArabic) "، " else ", ") { day ->
                                    when (day) {
                                        6 -> if (isArabic) "السبت" else "Sat"
                                        7 -> if (isArabic) "الأحد" else "Sun"
                                        1 -> if (isArabic) "الاثنين" else "Mon"
                                        2 -> if (isArabic) "الثلاثاء" else "Tue"
                                        3 -> if (isArabic) "الأربعاء" else "Wed"
                                        4 -> if (isArabic) "الخميس" else "Thu"
                                        5 -> if (isArabic) "الجمعة" else "Fri"
                                        else -> ""
                                    }
                                }
                                val centerName = centers.firstOrNull { it.id == draft.centerId }?.name
                                val availabilitySummary = buildString {
                                    append(dayNames.ifBlank { if (isArabic) "بدون أيام" else "No days" })
                                    append(" • ")
                                    append(doctorDisplayTime(draft.startTime, isArabic))
                                    append(" – ")
                                    append(doctorDisplayTime(draft.endTime, isArabic))
                                    if (!centerName.isNullOrBlank()) {
                                        append(" • ")
                                        append(centerName)
                                    }
                                }
                                CompactOutlinedRemovableItem(
                                    text = availabilitySummary,
                                    contentDescription = if (isArabic) "حذف موعد التواجد" else "Remove availability",
                                    onRemove = {
                                        availabilityDrafts = availabilityDrafts.filterIndexed { i, _ -> i != index }
                                        editingAvailabilityIndex = when {
                                            editingAvailabilityIndex == index -> null
                                            editingAvailabilityIndex != null && editingAvailabilityIndex!! > index -> editingAvailabilityIndex!! - 1
                                            else -> editingAvailabilityIndex
                                        }
                                    },
                                    onClick = { editingAvailabilityIndex = if (editingAvailabilityIndex == index) null else index }
                                )
                                if (editingAvailabilityIndex == index) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = CardDefaults.cardColors(containerColor = AppLargeCardContainerColor()),
                                    border = AppLargeCardBorder()
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                        ) {
                                            Text(
                                                (if (isArabic) "الفترة " else "Slot ") + (index + 1),
                                                modifier = Modifier.weight(1f),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            if (isArabic) "أيام التواجد" else "Available days",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
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
                                                val selected = day in draft.days
                                                Surface(
                                                    modifier = Modifier.weight(1f).height(40.dp),
                                                    onClick = {
                                                        val next = if (selected) draft.days - day else draft.days + day
                                                        availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(days = next) }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (selected) MaterialTheme.colorScheme.primary else AppCardContainerColor(),
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
                                            DoctorTimeSelectionField(
                                                label = if (isArabic) "من" else "From",
                                                value = doctorDisplayTime(draft.startTime, isArabic),
                                                modifier = Modifier.weight(1f),
                                                onClick = { availabilityTimePickerTarget = index to true }
                                            )
                                            DoctorTimeSelectionField(
                                                label = if (isArabic) "إلى" else "To",
                                                value = doctorDisplayTime(draft.endTime, isArabic),
                                                modifier = Modifier.weight(1f),
                                                onClick = { availabilityTimePickerTarget = index to false }
                                            )
                                        }
                                        OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = draft.notes,
                    onValueChange = { value ->
                                                availabilityDrafts = availabilityDrafts.toMutableList().also { it[index] = draft.copy(notes = value) }
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
                            OutlinedButton(
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                onClick = {
                                    val newIndex = availabilityDrafts.size
                                    availabilityDrafts = availabilityDrafts + DoctorAvailabilityDraft()
                                    editingAvailabilityIndex = newIndex
                                },
                                modifier = Modifier.fillMaxWidth().height(40.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(if (isArabic) "إضافة موعد" else "Add availability")
                            }
                        }
                    }
                }
                availabilityError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) } }
                item {
                    val hasDoctorLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                    FormExpandableSection(
                        title = if (isArabic) {
                            if (hasDoctorLocation) "موقع الطبيب • تم تحديد الموقع" else "موقع الطبيب"
                        } else {
                            if (hasDoctorLocation) "Doctor location • Set" else "Doctor location"
                        },
                        expanded = locationExpanded,
                        onToggle = { locationExpanded = !locationExpanded }
                    ) {
                        if (hasDoctorLocation) {
                            OutlinedButton(
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    latitude = null
                                    longitude = null
                                    mapUrl = null
                                    editDoctorLocation = false
                                }
                            ) { Text(if (isArabic) "مسح الموقع المحدد" else "Clear selected location") }
                        }
                        CurrentLocationButton(
                            isArabic = isArabic,
                            containerColor = AppCardContainerColor()
                        ) { lat, lon ->
                            latitude = lat
                            longitude = lon
                            mapUrl = null
                            editDoctorLocation = false
                            locationExpanded = false
                        }
                        OutlinedButton(
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { showDoctorMapPicker = true }
                        ) {
                            Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
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
                    colors = AppFilledTextFieldColors(),
                    shape = RoundedCornerShape(10.dp),
                    value = notes,
                    onValueChange = { notes = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    label = { Text(if (isArabic) "ملاحظات" else "Notes") }
                )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = {
                if (name.isBlank()) {
                    nameError = true
                } else if (selectedAreas.isEmpty()) {
                    showAreaRequiredDialog = true
                } else {
                    val validTime = Regex("(?:[01]\\d|2[0-3]):[0-5]\\d")
                    val invalidSlots = availabilityDrafts.any { it.days.isEmpty() || !validTime.matches(it.startTime) || !validTime.matches(it.endTime) || it.endTime <= it.startTime || (it.centerId != null && it.centerId !in selectedCenterIds) }
                    if (invalidSlots) { availabilityError = if (isArabic) "راجع أيام وساعات كل فترة، وتأكد أن المركز المختار مرتبط بالطبيب." else "Check each slot day and time, and make sure its center is linked to the doctor." } else { availabilityError = null; onSave(
                        DoctorEntity(
                            id = initial?.id ?: 0L,
                            name = name.trim(),
                            specialty = specialties.firstOrNull { it.id == selectedSpecialtyId }?.name.orEmpty(),
                            specialtyId = selectedSpecialtyId,
                            phoneNumbers = phoneNumbers.map { it.trim() }.filter { it.isNotBlank() }.joinToString("\\n"),
                            area = selectedAreas.joinToString("|"),
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
            },
                enabled = !photoCompressing,
                modifier = Modifier.height(48.dp),
                shape = RoundedCornerShape(10.dp)
            ) { Text(if (isArabic) "حفظ" else "Save") }
            OutlinedButton(onClick = onDismiss, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(10.dp)) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        },
        dismissButton = { }
    )

    if (showAreaPicker) {
                    DoctorFormBaseBottomSheet(title = if (isArabic) "اختيار المناطق" else "Choose areas", onDismiss = { showAreaPicker = false }) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            LazyColumn(modifier = Modifier.height(240.dp)) {
                                items(areaOptions, key = { it }) { areaOption ->
                                    Surface(
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(checked = selectedAreas.any { it.equals(areaOption, true) }, onCheckedChange = { checked ->
                                                selectedAreas = if (checked) selectedAreas + areaOption else selectedAreas.filterNot { it.equals(areaOption, true) }.toSet()
                                            })
                                            Text(areaOption, modifier = Modifier.weight(1f))
                                            IconButton(onClick = {
                                                areaOptions = areaOptions.filterNot { it.equals(areaOption, true) }
                                                selectedAreas = selectedAreas.filterNot { it.equals(areaOption, true) }.toSet()
                                                if (existingAreas.any { it.equals(areaOption, true) }) onDeleteArea(areaOption)
                                            }) { Icon(Icons.Default.Close, contentDescription = if (isArabic) "حذف المنطقة" else "Delete area") }
                                        }
                                    }
                                }
                            }
                            selectedAreas.sorted().forEach { selectedArea ->
                                CompactOutlinedRemovableItem(
                                    text = selectedArea,
                                    contentDescription = if (isArabic) "إزالة المنطقة المختارة" else "Remove selected area",
                                    onRemove = {
                                        selectedAreas = selectedAreas.filterNot { it.equals(selectedArea, true) }.toSet()
                                    }
                                )
                            }
                            OutlinedTextField(
                    colors = AppFilledTextFieldColors(AppCardContainerColor()),
                    shape = RoundedCornerShape(10.dp),
                    value = newArea,
                    onValueChange = { newArea = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "إضافة منطقة جديدة" else "Add a new area") },
                    trailingIcon = {
                                    IconButton(onClick = {
                                        val value = newArea.trim()
                                        if (value.isNotBlank() && areaOptions.none { it.equals(value, true) }) { areaOptions = areaOptions + value; newArea = "" }
                                    }, enabled = newArea.isNotBlank()) { Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة منطقة" else "Add area") }
                                }
                )
                        }
                    }
    }

    availabilityTimePickerTarget?.let { (draftIndex, isStart) ->
        val currentDraft = availabilityDrafts.getOrNull(draftIndex)
        if (currentDraft != null) {
            DoctorAvailabilityTimePickerDialog(
                isArabic = isArabic,
                initialValue = if (isStart) currentDraft.startTime else currentDraft.endTime,
                onDismiss = { availabilityTimePickerTarget = null },
                onConfirm = { hour, minute ->
                    val value = String.format(Locale.US, "%02d:%02d", hour, minute)
                    availabilityDrafts = availabilityDrafts.toMutableList().also { list ->
                        val current = list[draftIndex]
                        list[draftIndex] = if (isStart) current.copy(startTime = value) else current.copy(endTime = value)
                    }
                    availabilityError = null
                    availabilityTimePickerTarget = null
                }
            )
        }
    }

    if (showSpecialtyPicker) {
        ModalBottomSheet(
            onDismissRequest = { showSpecialtyPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppDialogContainerColor()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabic) "اختر التخصص" else "Choose a specialty",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Surface(
                            onClick = { selectedSpecialtyId = null; showSpecialtyPicker = false },
                            color = AppSelectionRowColor(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    if (isArabic) "بدون تخصص" else "No specialty",
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    items(specialties, key = { it.id }) { specialty ->
                        Surface(
                            onClick = { selectedSpecialtyId = specialty.id; showSpecialtyPicker = false },
                            color = AppSelectionRowColor(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    specialty.name,
                                    textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCenterPicker) {
        ModalBottomSheet(
            onDismissRequest = {
                showCenterPicker = false
                centerSearchExpanded = false
                centerSearchQuery = ""
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = AppDialogContainerColor()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    if (isArabic) "اختر مراكز عمل الطبيب" else "Select doctor's centers",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    item {
                        Surface(
                            onClick = { selectedCenterIds = emptySet() },
                            color = AppSelectionRowColor(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        centerSearchExpanded = !centerSearchExpanded
                                        if (!centerSearchExpanded) centerSearchQuery = ""
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        if (centerSearchExpanded) Icons.Default.Close else Icons.Default.Search,
                                        contentDescription = if (isArabic) "بحث في المراكز" else "Search centers",
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier.weight(1f).padding(vertical = 15.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        if (isArabic) "مسح الاختيار" else "Clear selection",
                                        textAlign = TextAlign.Center,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                    }
                    if (centerSearchExpanded) {
                        item {
                            OutlinedTextField(
                    colors = AppFilledTextFieldColors(AppCardContainerColor()),
                    value = centerSearchQuery,
                    onValueChange = { centerSearchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    placeholder = { Text(if (isArabic) "ابحث في المراكز" else "Search centers") },
                    leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                },
                    trailingIcon = {
                                    if (centerSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { centerSearchQuery = "" }) {
                                            Icon(Icons.Default.Close, contentDescription = if (isArabic) "مسح البحث" else "Clear search")
                                        }
                                    }
                                }
                )
                        }
                    }
                    items(
                        centers.filter { it.name.contains(centerSearchQuery.trim(), ignoreCase = true) },
                        key = { it.id }
                    ) { center ->
                        Surface(
                            color = AppSelectionRowColor(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable {
                                    selectedCenterIds = if (center.id in selectedCenterIds) {
                                        selectedCenterIds - center.id
                                    } else {
                                        selectedCenterIds + center.id
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = center.id in selectedCenterIds,
                                    onCheckedChange = { checked ->
                                        selectedCenterIds = if (checked) {
                                            selectedCenterIds + center.id
                                        } else {
                                            selectedCenterIds - center.id
                                        }
                                    }
                                )
                                Text(
                                    center.name,
                                    modifier = Modifier.weight(1f),
                                    textAlign = if (isArabic) TextAlign.End else TextAlign.Start,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAreaRequiredDialog) {
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showAreaRequiredDialog = false },
            title = { Text(if (isArabic) "المنطقة مطلوبة" else "Area required") },
            text = { Text(if (isArabic) "من فضلك اختار منطقة واحدة على الأقل للطبيب قبل الحفظ." else "Please select at least one area for this doctor before saving.") },
            confirmButton = {
                TextButton(onClick = {
                    showAreaRequiredDialog = false
                    showAreaPicker = true
                }) { Text(if (isArabic) "اختيار المنطقة" else "Choose area") }
            },
            dismissButton = {
                TextButton(onClick = { showAreaRequiredDialog = false }) { Text(if (isArabic) "حسنًا" else "OK") }
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