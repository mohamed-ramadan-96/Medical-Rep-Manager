package com.medicalrep.manager.ui

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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.geometry.Size
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
    Canvas(Modifier.size(34.dp)) {
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
                    drawRoundRect(color, androidx.compose.ui.geometry.Offset(15f, 4f), androidx.compose.ui.Size(19f, 40f), androidx.compose.ui.geometry.CornerRadius(3f), style = stroke)
                    val wings = Path().apply {
                        moveTo(15f, 16f); lineTo(10f, 16f); quadraticTo(6f, 16f, 6f, 20f)
                        lineTo(6f, 39f); quadraticTo(6f, 43f, 10f, 43f); lineTo(15f, 43f)
                        moveTo(34f, 16f); lineTo(38f, 16f); quadraticTo(42f, 16f, 42f, 20f)
                        lineTo(42f, 39f); quadraticTo(42f, 43f, 38f, 43f); lineTo(34f, 43f)
                    }
                    drawPath(wings, color, style = stroke)
                    drawLine(color, androidx.compose.ui.geometry.Offset(24.5f, 11f), androidx.compose.ui.geometry.Offset(24.5f, 21f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(20f, 16f), androidx.compose.ui.geometry.Offset(29f, 16f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(20f, 28f), androidx.compose.ui.geometry.Offset(29f, 28f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(20f, 35f), androidx.compose.ui.geometry.Offset(29f, 35f), 2.8f, cap = StrokeCap.Round)
                }
                "calendar" -> {
                    drawRoundRect(color, androidx.compose.ui.geometry.Offset(6f, 9f), androidx.compose.ui.Size(36f, 34f), androidx.compose.ui.geometry.CornerRadius(4f), style = stroke)
                    drawLine(color, androidx.compose.ui.geometry.Offset(6f, 19f), androidx.compose.ui.geometry.Offset(42f, 19f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(16f, 5f), androidx.compose.ui.geometry.Offset(16f, 14f), 2.8f, cap = StrokeCap.Round)
                    drawLine(color, androidx.compose.ui.geometry.Offset(32f, 5f), androidx.compose.ui.geometry.Offset(32f, 14f), 2.8f, cap = StrokeCap.Round)
                    listOf(14f, 24f, 34f).forEach { x -> listOf(25f, 34f).forEach { y -> drawCircle(color, 1.8f, androidx.compose.ui.geometry.Offset(x, y)) } }
                }
                "followup" -> {
                    drawRoundRect(color, androidx.compose.ui.geometry.Offset(9f, 8f), androidx.compose.ui.Size(30f, 36f), androidx.compose.ui.geometry.CornerRadius(4f), style = stroke)
                    drawRoundRect(color, androidx.compose.ui.geometry.Offset(17f, 5f), androidx.compose.ui.Size(14f, 7f), androidx.compose.ui.geometry.CornerRadius(2f), style = stroke)
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
                            colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent)
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
                        animationSpec = tween(durationMillis = 220),
                        initialOffsetX = { width -> offset * (width / 12) }
                    ) + fadeIn(animationSpec = tween(durationMillis = 180)))
                        .togetherWith(
                            slideOutHorizontally(
                                animationSpec = tween(durationMillis = 180),
                                targetOffsetX = { width -> -offset * (width / 16) }
                            ) + fadeOut(animationSpec = tween(durationMillis = 140))
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
    }
}

@Composable
private fun FollowUpScreen(isArabic: Boolean, padding: PaddingValues) {
    var showPlan by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        modifier = Modifier.padding(padding),
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(selected = showPlan, onClick = { showPlan = true }, label = { Text(if (isArabic) "الخطة" else "Plan") }, leadingIcon = { Icon(Icons.Outlined.CalendarMonth, contentDescription = null) }, modifier = Modifier.weight(1f))
                FilterChip(selected = !showPlan, onClick = { showPlan = false }, label = { Text(if (isArabic) "الزيارات" else "Visits") }, leadingIcon = { Icon(Icons.Filled.History, contentDescription = null) }, modifier = Modifier.weight(1f))
            }
        }
    ) { innerPadding ->
        if (showPlan) WeeklyPlanScreen(isArabic = isArabic, padding = innerPadding)
        else VisitsScreen(isArabic = isArabic, padding = innerPadding)
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
