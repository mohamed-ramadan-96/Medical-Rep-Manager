package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing

import com.medicalrep.manager.ui.theme.AppMotion

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.PeopleAlt
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import com.medicalrep.manager.AppUpdateInfo
import com.medicalrep.manager.checkForAppUpdate
import com.medicalrep.manager.downloadAppUpdate
import com.medicalrep.manager.getAppUpdateDownloadProgress
import com.medicalrep.manager.AppUpdateDownloadProgress
import android.app.DownloadManager
import kotlinx.coroutines.delay
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class AppLanguage { English, Arabic }

private data class Tab(
    val titleEn: String,
    val titleAr: String,
    val icon: String
)

private val tabs = listOf(
    Tab("Home", "الرئيسية", "home"),
    Tab("Doctors", "الأطباء", "doctors"),
    Tab("Centers", "المراكز", "centers"),
    Tab("Hours", "المواعيد", "calendar"),
    Tab("Follow-up", "المتابعة", "followup")
)

@Composable
private fun ReferenceNavigationIcon(name: String) {
    val color = MaterialTheme.colorScheme.onSurface
    Canvas(Modifier.size(24.dp)) {
        val unit = size.minDimension / 48f
        scale(unit, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            val stroke = Stroke(width = 2.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            when (name) {
                "home" -> {
                    val p = Path().apply {
                        moveTo(5f, 22f); lineTo(24f, 5f); lineTo(43f, 22f)
                        moveTo(9f, 20f); lineTo(9f, 42f); quadraticTo(9f, 44f, 11f, 44f)
                        lineTo(37f, 44f); quadraticTo(39f, 44f, 39f, 42f); lineTo(39f, 20f)
                        moveTo(20f, 44f); lineTo(20f, 29f); quadraticTo(20f, 27f, 22f, 27f)
                        lineTo(26f, 27f); quadraticTo(28f, 27f, 28f, 29f); lineTo(28f, 44f)
                    }
                    drawPath(p, color, style = stroke)
                }
                "doctors" -> {
                    drawCircle(color, 5.3f, androidx.compose.ui.geometry.Offset(15f, 13f), style = stroke)
                    drawCircle(color, 5.3f, androidx.compose.ui.geometry.Offset(33f, 13f), style = stroke)
                    val p = Path().apply {
                        moveTo(4f, 39f); cubicTo(4f, 30f, 9f, 26f, 15f, 26f)
                        cubicTo(21f, 26f, 26f, 30f, 26f, 39f)
                        moveTo(22f, 28f); cubicTo(24f, 26f, 28f, 25f, 33f, 26f)
                        cubicTo(40f, 27f, 44f, 32f, 44f, 39f)
                    }
                    drawPath(p, color, style = stroke)
                }
                "centers" -> {
                    drawPath(Path().apply { moveTo(18f, 4f); lineTo(31f, 4f); quadraticTo(34f, 4f, 34f, 7f); lineTo(34f, 41f); quadraticTo(34f, 44f, 31f, 44f); lineTo(18f, 44f); quadraticTo(15f, 44f, 15f, 41f); lineTo(15f, 7f); quadraticTo(15f, 4f, 18f, 4f); close() }, color, style = stroke)
                    val wings = Path().apply {
                        moveTo(15f, 16f); lineTo(10f, 16f); quadraticTo(6f, 16f, 6f, 20f)
                        lineTo(6f, 39f); quadraticTo(6f, 43f, 10f, 43f); lineTo(15f, 43f)
                        moveTo(34f, 16f); lineTo(38f, 16f); quadraticTo(42f, 16f, 42f, 20f)
                        lineTo(42f, 39f); quadraticTo(42f, 43f, 38f, 43f); lineTo(34f, 43f)
                    }
                    drawPath(wings, color, style = stroke)
                    drawLine(color, androidx.compose.ui.geometry.Offset(20f, 28f), androidx.compose.ui.geometry.Offset(29f, 28f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(20f, 35f), androidx.compose.ui.geometry.Offset(29f, 35f), 2.8f, cap = StrokeCap.Round)
                }
                "calendar" -> {
                    drawPath(Path().apply { moveTo(10f, 9f); lineTo(38f, 9f); quadraticTo(42f, 9f, 42f, 13f); lineTo(42f, 39f); quadraticTo(42f, 43f, 38f, 43f); lineTo(10f, 43f); quadraticTo(6f, 43f, 6f, 39f); lineTo(6f, 13f); quadraticTo(6f, 9f, 10f, 9f); close() }, color, style = stroke)
                    drawLine(color, androidx.compose.ui.geometry.Offset(6f, 19f), androidx.compose.ui.geometry.Offset(42f, 19f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(16f, 5f), androidx.compose.ui.geometry.Offset(16f, 14f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(32f, 5f), androidx.compose.ui.geometry.Offset(32f, 14f), 2.8f, cap = StrokeCap.Round)
                    listOf(14f, 24f, 34f).forEach { x -> listOf(25f, 34f).forEach { y -> drawCircle(color, 1.8f, androidx.compose.ui.geometry.Offset(x, y)) } }
                }
                "followup" -> {
                    drawPath(Path().apply { moveTo(13f, 8f); lineTo(35f, 8f); quadraticTo(39f, 8f, 39f, 12f); lineTo(39f, 40f); quadraticTo(39f, 44f, 35f, 44f); lineTo(13f, 44f); quadraticTo(9f, 44f, 9f, 40f); lineTo(9f, 12f); quadraticTo(9f, 8f, 13f, 8f); close() }, color, style = stroke)
                    drawPath(Path().apply { moveTo(19f, 5f); lineTo(29f, 5f); quadraticTo(31f, 5f, 31f, 7f); lineTo(31f, 10f); quadraticTo(31f, 12f, 29f, 12f); lineTo(19f, 12f); quadraticTo(17f, 12f, 17f, 10f); lineTo(17f, 7f); quadraticTo(17f, 5f, 19f, 5f); close() }, color, style = stroke)
                    listOf(18f, 28f).forEach { y ->
                        drawCircle(color, 1.5f, androidx.compose.ui.geometry.Offset(16f, y))
                        drawLine(color, androidx.compose.ui.geometry.Offset(22f, y), androidx.compose.ui.geometry.Offset(33f, y), 2.8f, cap = StrokeCap.Round)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRepApp() {
    val context = LocalContext.current
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var updateDownloadId by rememberSaveable { mutableStateOf<Long?>(null) }
    var updateDownloadProgress by remember { mutableStateOf<AppUpdateDownloadProgress?>(null) }

    LaunchedEffect(updateDownloadId) {
        val downloadId = updateDownloadId ?: return@LaunchedEffect
        while (true) {
            val progress = getAppUpdateDownloadProgress(context, downloadId)
            updateDownloadProgress = progress
            if (progress == null ||
                progress.status == DownloadManager.STATUS_SUCCESSFUL ||
                progress.status == DownloadManager.STATUS_FAILED
            ) break
            delay(500)
        }
    }

    LaunchedEffect(Unit) {
        availableUpdate = checkForAppUpdate(context)
    }
    val preferences = remember(context) {
        context.getSharedPreferences("app_settings", android.content.Context.MODE_PRIVATE)
    }
    var language by rememberSaveable {
        mutableStateOf(if (preferences.getBoolean("arabic_language", true)) AppLanguage.Arabic else AppLanguage.English)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var showSpecialtiesManagement by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (selectedTab !in tabs.indices) selectedTab = 0 }
    val isArabic = language == AppLanguage.Arabic

    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                if (isArabic) "مدير المندوب الطبي" else "Medical Rep Manager",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (isArabic) "دليل بيانات الأطباء ومواعيدهم" else "Doctor directory and availability",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            language = if (isArabic) AppLanguage.English else AppLanguage.Arabic
                            preferences.edit().putBoolean("arabic_language", language == AppLanguage.Arabic).apply()
                        }) {
                            Icon(Icons.Filled.Language, contentDescription = "Change language")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index; showSpecialtiesManagement = false },
                            icon = { ReferenceNavigationIcon(tab.icon) },
                            label = { Text(if (isArabic) tab.titleAr else tab.titleEn, maxLines = 1, softWrap = false) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            AnimatedContent(
                targetState = selectedTab,
                label = "main_tab_transition",
                transitionSpec = {
                    val forward = targetState > initialState
                    val rtlDirection = if (isArabic) -1 else 1
                    val offset = if (forward) rtlDirection else -rtlDirection
                    (slideInHorizontally(
                        animationSpec = tween(durationMillis = AppMotion.Emphasized),
                        initialOffsetX = { width -> offset * (width / 12) }
                    ) + fadeIn(animationSpec = tween(durationMillis = AppMotion.Standard)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = AppMotion.Standard),
                                targetOffsetX = { width -> -offset * (width / 16) }
                            ) + fadeOut(animationSpec = tween(durationMillis = AppMotion.Fast))
                        )
                }
            ) { tab ->
                when (tab) {
                    0 -> DashboardScreen(isArabic, innerPadding)
                    1 -> if (showSpecialtiesManagement) SpecialtiesScreen(isArabic = isArabic, padding = innerPadding) else DoctorsScreen(isArabic = isArabic, padding = innerPadding, onManageSpecialties = { showSpecialtiesManagement = true })
                    2 -> CentersScreen(isArabic = isArabic, padding = innerPadding)
                    3 -> ScheduleScreen(isArabic = isArabic, padding = innerPadding)
                    4 -> FollowUpScreen(isArabic = isArabic, padding = innerPadding)
                    else -> DashboardScreen(isArabic, innerPadding)
                }
            }
        }
        availableUpdate?.let { update ->
            val progress = updateDownloadProgress
            val isDownloading = updateDownloadId != null &&
                progress?.status != DownloadManager.STATUS_SUCCESSFUL &&
                progress?.status != DownloadManager.STATUS_FAILED
            val downloadComplete = progress?.status == DownloadManager.STATUS_SUCCESSFUL
            val downloadFailed = progress?.status == DownloadManager.STATUS_FAILED

            AlertDialog(
                onDismissRequest = {
                    if (!isDownloading) {
                        availableUpdate = null
                        updateDownloadId = null
                        updateDownloadProgress = null
                    }
                },
                title = {
                    Text(
                        when {
                            isDownloading -> if (isArabic) "جارٍ تحميل التحديث" else "Downloading update"
                            downloadComplete -> if (isArabic) "اكتمل تحميل التحديث" else "Update downloaded"
                            downloadFailed -> if (isArabic) "فشل تحميل التحديث" else "Download failed"
                            else -> if (isArabic) "تحديث جديد متاح" else "Update available"
                        }
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                        Text(
                            if (isArabic) "الإصدار الجديد: ${update.versionName}" else "New version: ${update.versionName}",
                            fontWeight = FontWeight.SemiBold
                        )

                        if (updateDownloadId == null && update.releaseNotes.isNotBlank()) {
                            Text(update.releaseNotes, style = MaterialTheme.typography.bodyMedium)
                        }

                        when {
                            isDownloading -> {
                                val percent = progress?.percent
                                LinearProgressIndicator(
                                    progress = { (percent ?: 0) / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    if (percent != null) {
                                        if (isArabic) "تم تحميل $percent٪" else "Downloaded $percent%"
                                    } else {
                                        if (isArabic) "جاري تحميل الملف..." else "Downloading file…"
                                    },
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (progress != null && progress.totalBytes > 0L) {
                                    Text(
                                        "${android.text.format.Formatter.formatShortFileSize(context, progress.downloadedBytes)} / ${android.text.format.Formatter.formatShortFileSize(context, progress.totalBytes)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            downloadComplete -> Text(
                                if (isArabic) "تم تنزيل الملف، وسيتم فتح شاشة تثبيت Android." else "Download complete. Android installer will open.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            downloadFailed -> Text(
                                if (isArabic) "تعذر تنزيل التحديث. تقدر تحاول مرة تانية." else "The download failed. You can try again.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            else -> Text(
                                if (isArabic) "سيظهر تقدم التحميل هنا، وبعدها سيطلب Android تأكيد التثبيت." else "Download progress will appear here, then Android will ask you to confirm installation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                confirmButton = {
                    when {
                        updateDownloadId == null || downloadFailed -> Button(onClick = {
                            updateDownloadProgress = null
                            updateDownloadId = downloadAppUpdate(context, update)
                        }) {
                            Text(if (isArabic) "تحميل التحديث" else "Download update")
                        }
                        downloadComplete -> TextButton(onClick = {
                            availableUpdate = null
                            updateDownloadId = null
                            updateDownloadProgress = null
                        }) {
                            Text(if (isArabic) "إغلاق" else "Close")
                        }
                    }
                },
                dismissButton = {
                    if (!isDownloading && !downloadComplete) {
                        TextButton(onClick = {
                            availableUpdate = null
                            updateDownloadId = null
                            updateDownloadProgress = null
                        }) {
                            Text(if (isArabic) "لاحقًا" else "Later")
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun FollowUpScreen(isArabic: Boolean, padding: PaddingValues) {
    var showPlan by rememberSaveable { mutableStateOf(true) }
    var pendingPlanVisit by remember { mutableStateOf<Pair<Long, String>?>(null) }
    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = showPlan, onClick = { showPlan = true }, label = { Text(if (isArabic) "الخطة" else "Plan") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) }, modifier = Modifier.weight(1f))
                FilterChip(selected = !showPlan, onClick = { showPlan = false }, label = { Text(if (isArabic) "الزيارات" else "Visits") }, leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) }, modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = showPlan,
            label = "plan_visits_transition",
            transitionSpec = {
                val forward = targetState
                val direction = if (isArabic) -1 else 1
                val offset = if (forward) direction else -direction
                (slideInHorizontally(
                    animationSpec = tween(durationMillis = AppMotion.Emphasized),
                    initialOffsetX = { width -> offset * (width / 12) }
                ) + fadeIn(animationSpec = tween(durationMillis = AppMotion.Standard)))
                    .togetherWith(
                        slideOutHorizontally(
                            animationSpec = tween(durationMillis = AppMotion.Standard),
                            targetOffsetX = { width -> -offset * (width / 16) }
                        ) + fadeOut(animationSpec = tween(durationMillis = AppMotion.Fast))
                    )
            }
        ) { planVisible ->
            if (planVisible) {
                WeeklyPlanScreen(
                    isArabic = isArabic,
                    padding = innerPadding,
                    onRecordVisit = { doctorId, visitDate ->
                        pendingPlanVisit = doctorId to visitDate
                        showPlan = false
                    }
                )
            } else {
                VisitsScreen(
                    isArabic = isArabic,
                    padding = innerPadding,
                    initialDoctorId = pendingPlanVisit?.first,
                    initialVisitDate = pendingPlanVisit?.second,
                    onInitialVisitHandled = { pendingPlanVisit = null }
                )
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, description: String, padding: PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            description,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
