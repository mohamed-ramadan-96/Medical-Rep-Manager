package com.medicalrep.manager.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.Button
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.medicalrep.manager.data.ExcelExporter
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
import androidx.compose.material3.LinearProgressIndicator
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
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { ExcelExporter.export(context, uri, doctors, centers, visits, isArabic) }
                Toast.makeText(context, if (isArabic) "تم تصدير ملف Excel بنجاح" else "Excel workbook exported successfully", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, (if (isArabic) "فشل التصدير: " else "Export failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
    val todayStart = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val tomorrowStart = remember(todayStart) { Calendar.getInstance().apply {
        timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis }
    val todayVisits = visits.count { it.visitedAt >= todayStart && it.visitedAt < tomorrowStart }
    val monthStart = remember { Calendar.getInstance().apply {
        set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val nextMonthStart = remember(monthStart) { Calendar.getInstance().apply {
        timeInMillis = monthStart; add(Calendar.MONTH, 1)
    }.timeInMillis }
    val yearStart = remember { Calendar.getInstance().apply {
        set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val nextYearStart = remember(yearStart) { Calendar.getInstance().apply {
        timeInMillis = yearStart; add(Calendar.YEAR, 1)
    }.timeInMillis }
    val monthVisits = visits.count { it.visitedAt >= monthStart && it.visitedAt < nextMonthStart }
    val yearVisits = visits.count { it.visitedAt >= yearStart && it.visitedAt < nextYearStart }
    val monthFormatter = remember(isArabic) { SimpleDateFormat("MMM", if (isArabic) Locale("ar") else Locale.US) }
    val monthlyTrend = remember(visits, isArabic) {
        (5 downTo 0).map { offset ->
            val month = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0); add(Calendar.MONTH, -offset)
            }
            val start = month.timeInMillis
            val end = (month.clone() as Calendar).apply { add(Calendar.MONTH, 1) }.timeInMillis
            monthFormatter.format(month.time) to visits.count { it.visitedAt >= start && it.visitedAt < end }
        }
    }
    val trendMaximum = monthlyTrend.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    val outcomeSummary = visits.filter { it.visitedAt >= yearStart && it.visitedAt < nextYearStart }
        .groupingBy { it.outcome.ifBlank { if (isArabic) "بدون نتيجة" else "No outcome" } }
        .eachCount().entries.sortedByDescending { it.value }.take(3)
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
            Button(onClick = { exportLauncher.launch("MedicalRepManager_Export.xlsx") }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Text(if (isArabic) "تصدير بيانات Excel (الأطباء والزيارات والمراكز)" else "Export Excel (Doctors, Visits & Centers)", modifier = Modifier.padding(start = 8.dp))
            }
        }
        item {
            GoogleDriveBackupCard(isArabic = isArabic)
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
            SectionHeading(if (isArabic) "تقارير الأداء" else "Performance reports",
                if (isArabic) "ملخص الشهر والسنة واتجاه الزيارات خلال آخر 6 أشهر" else "Monthly and yearly summary with the last 6 months of activity")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(Modifier.weight(1f), Icons.Default.CalendarMonth, monthVisits.toString(),
                    if (isArabic) "زيارات هذا الشهر" else "This month")
                MetricCard(Modifier.weight(1f), Icons.Default.History, yearVisits.toString(),
                    if (isArabic) "زيارات هذه السنة" else "This year")
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(if (isArabic) "نشاط الزيارات - آخر 6 أشهر" else "Visit activity - last 6 months",
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                    monthlyTrend.forEach { (label, count) ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(count.toString(), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = count.toFloat() / trendMaximum.toFloat(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    if (outcomeSummary.isNotEmpty()) {
                        Text(if (isArabic) "أبرز نتائج الزيارات هذا العام" else "Top visit outcomes this year",
                            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                        outcomeSummary.forEach { entry ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(entry.key, modifier = Modifier.weight(1f))
                                Text(entry.value.toString(), fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text(if (isArabic) "لا توجد زيارات مسجلة هذا العام حتى الآن." else "No visits recorded this year yet.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
