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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
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
    Tab("Schedule", "الجدول", Icons.Filled.CalendarMonth)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicalRepApp() {
    var language by remember { mutableStateOf(AppLanguage.Arabic) }
    var selectedTab by remember { mutableIntStateOf(0) }
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
            when (selectedTab) {
                0 -> DashboardScreen(isArabic, innerPadding)
                1 -> DoctorsScreen(isArabic = isArabic, padding = innerPadding)
                2 -> CentersScreen(isArabic = isArabic, padding = innerPadding)
                3 -> SpecialtiesScreen(isArabic = isArabic, padding = innerPadding)
                else -> PlaceholderScreen(
                    title = if (isArabic) "جدول الزيارات" else "Visit Schedule",
                    description = if (isArabic) "حدد أكثر من يوم وفترة متاحة لكل طبيب." else "Set multiple available days and time slots for every doctor.",
                    padding = innerPadding
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(isArabic: Boolean, padding: PaddingValues) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                if (isArabic) "أهلاً بك 👋" else "Welcome 👋",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                if (isArabic) "كل بيانات عملك في مكان واحد." else "Your work, organized in one place.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.PeopleAlt,
                    value = "0",
                    label = if (isArabic) "الأطباء" else "Doctors"
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.LocalHospital,
                    value = "0",
                    label = if (isArabic) "المراكز" else "Centers"
                )
            }
        }
        item {
            SummaryCard(
                modifier = Modifier.fillMaxWidth(),
                icon = Icons.Filled.Schedule,
                value = "0",
                label = if (isArabic) "زيارات اليوم" else "Today's visits"
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null)
                    Text(
                        if (isArabic) "ابدأ بتنظيم جدولك" else "Start planning your schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        if (isArabic) "أضف الأطباء ومواعيدهم المتاحة، وسنساعدك على متابعة زياراتك." else "Add doctors and their available time slots to keep your visits organized.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
