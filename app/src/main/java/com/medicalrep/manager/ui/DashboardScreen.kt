package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(isArabic: Boolean, padding: PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val doctors by database.doctorDao().observeAll().collectAsState(initial = emptyList())
    val centers by database.medicalCenterDao().observeAll().collectAsState(initial = emptyList())
    val visits by database.doctorVisitDao().observeAll().collectAsState(initial = emptyList())
    val todayStart = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val tomorrowStart = remember(todayStart) { Calendar.getInstance().apply {
        timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis }
    val todayVisits = visits.count { it.visitedAt >= todayStart && it.visitedAt < tomorrowStart }
    val upcoming = visits.filter { it.nextFollowUpAt != null && it.nextFollowUpAt >= todayStart }
        .sortedBy { it.nextFollowUpAt }.take(5)
    val recent = visits.take(5)
    val doctorNames = doctors.associate { it.id to it.name }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isArabic) "لوحة المتابعة" else "Dashboard",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(if (isArabic) "ملخص نشاطك وبياناتك المسجلة" else "A live overview of your work and records",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Modifier.weight(1f), Icons.Default.PeopleAlt, doctors.size.toString(),
                    if (isArabic) "الأطباء" else "Doctors")
                MetricCard(Modifier.weight(1f), Icons.Default.LocalHospital, centers.size.toString(),
                    if (isArabic) "المراكز" else "Centers")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Modifier.weight(1f), Icons.Default.Schedule, todayVisits.toString(),
                    if (isArabic) "زيارات اليوم" else "Today's visits")
                MetricCard(Modifier.weight(1f), Icons.Default.History, visits.size.toString(),
                    if (isArabic) "إجمالي الزيارات" else "Total visits")
            }
        }
        item {
            SectionHeading(if (isArabic) "المتابعات القادمة" else "Upcoming follow-ups",
                if (isArabic) "مواعيد المتابعة المسجلة ولم تنتهِ بعد" else "Scheduled follow-ups from today onward")
        }
        if (upcoming.isEmpty()) {
            item { EmptyDashboardCard(if (isArabic) "لا توجد مواعيد متابعة قادمة." else "No upcoming follow-ups.") }
        } else {
            items(upcoming, key = { "follow-${it.id}" }) { visit ->
                VisitSummaryCard(visit, doctorNames[visit.doctorId], isArabic, true)
            }
        }
        item {
            SectionHeading(if (isArabic) "أحدث الزيارات" else "Recent visits",
                if (isArabic) "آخر الزيارات المسجلة" else "Your latest recorded visits")
        }
        if (recent.isEmpty()) {
            item { EmptyDashboardCard(if (isArabic) "سجّل أول زيارة ليظهر نشاطك هنا." else "Record your first visit to see activity here.") }
        } else {
            items(recent, key = { "recent-${it.id}" }) { visit ->
                VisitSummaryCard(visit, doctorNames[visit.doctorId], isArabic, false)
            }
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VisitSummaryCard(visit: DoctorVisitEntity, doctorName: String?, isArabic: Boolean, followUp: Boolean) {
    val timestamp = if (followUp) visit.nextFollowUpAt ?: visit.visitedAt else visit.visitedAt
    val date = remember(timestamp) { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp)) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(if (followUp) Icons.Default.EventRepeat else Icons.Default.CalendarMonth,
                contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(doctorName ?: if (isArabic) "طبيب غير موجود" else "Unknown doctor",
                    fontWeight = FontWeight.SemiBold)
                Text(if (followUp) {
                    (if (isArabic) "موعد المتابعة: " else "Follow-up: ") + date
                } else "$date • ${visit.outcome}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!followUp && visit.notes.isNotBlank()) {
                    Text(visit.notes, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyDashboardCard(message: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
