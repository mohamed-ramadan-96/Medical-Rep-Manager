package com.medicalrep.manager.ui

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private enum class AppLanguage { English, Arabic }

private data class Tab(
    val titleEn: String,
    val titleAr: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val tabs = listOf(
    Tab("Home", "الرئيسية", Icons.Filled.Home),
    Tab("Doctors", "الأطباء", Icons.Filled.PeopleAlt),
    Tab("Centers", "المراكز", Icons.Filled.LocalHospital),
    Tab("Specialties", "التخصصات", Icons.Filled.MedicalServices),
    Tab("Schedule", "الجدول", Icons.Filled.CalendarMonth),
    Tab("Visits", "الزيارات", Icons.Filled.History)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRepApp() {
    var language by remember { mutableStateOf(AppLanguage.Arabic) }
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
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
                                if (isArabic) "نظّم زياراتك وعلاقاتك المهنية" else "Organize your professional visits",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            language = if (isArabic) AppLanguage.English else AppLanguage.Arabic
                        }) {
                            Icon(Icons.Filled.Language, contentDescription = "Change language")
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(if (isArabic) tab.titleAr else tab.titleEn) }
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
                    1 -> DoctorsScreen(isArabic = isArabic, padding = innerPadding)
                    2 -> CentersScreen(isArabic = isArabic, padding = innerPadding)
                    3 -> SpecialtiesScreen(isArabic = isArabic, padding = innerPadding)
                    4 -> ScheduleScreen(isArabic = isArabic, padding = innerPadding)
                    else -> VisitsScreen(isArabic = isArabic, padding = innerPadding)
                }
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
