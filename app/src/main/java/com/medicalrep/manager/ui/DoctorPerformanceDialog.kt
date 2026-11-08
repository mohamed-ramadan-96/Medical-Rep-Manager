package com.medicalrep.manager.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.DoctorVisitEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DoctorPerformanceDialog(
    doctor: DoctorEntity,
    visits: List<DoctorVisitEntity>,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    val monthStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val monthCount = visits.count { it.visitedAt >= monthStart }
    val latestVisit = visits.maxByOrNull { it.visitedAt }
    val nextFollowUp = visits.mapNotNull { it.nextFollowUpAt }
        .filter { it >= now }
        .minOrNull()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(if (isArabic) "تقرير أداء الطبيب" else "Doctor performance report")
                Text(doctor.name, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary)
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReportMetric(
                            Modifier.weight(1f),
                            visits.size.toString(),
                            if (isArabic) "إجمالي الزيارات" else "Total visits"
                        )
                        ReportMetric(
                            Modifier.weight(1f),
                            monthCount.toString(),
                            if (isArabic) "زيارات الشهر" else "This month"
                        )
                    }
                }
                item {
                    ReportInfoCard(
                        title = if (isArabic) "آخر زيارة" else "Last visit",
                        value = latestVisit?.let { dateFormat.format(Date(it.visitedAt)) }
                            ?: if (isArabic) "لا توجد زيارات" else "No visits yet"
                    )
                }
                item {
                    ReportInfoCard(
                        title = if (isArabic) "المتابعة القادمة" else "Next follow-up",
                        value = nextFollowUp?.let { dateFormat.format(Date(it)) }
                            ?: if (isArabic) "لا توجد متابعة مجدولة" else "No follow-up scheduled"
                    )
                }
                item {
                    Text(
                        if (isArabic) "سجل الزيارات" else "Visit history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (visits.isEmpty()) {
                    item {
                        Text(
                            if (isArabic) "لم يتم تسجيل زيارات لهذا الطبيب بعد."
                            else "No visits have been recorded for this doctor.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(visits.sortedByDescending { it.visitedAt }.take(10), key = { it.id }) { visit ->
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(dateFormat.format(Date(visit.visitedAt)), fontWeight = FontWeight.SemiBold)
                            if (visit.outcome.isNotBlank()) Text(visit.outcome)
                            if (visit.notes.isNotBlank()) {
                                Text(visit.notes, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            visit.nextFollowUpAt?.let { followUp ->
                                Text(
                                    (if (isArabic) "المتابعة: " else "Follow-up: ") +
                                        dateFormat.format(Date(followUp)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isArabic) "إغلاق" else "Close")
            }
        }
    )
}

@Composable
private fun ReportMetric(modifier: Modifier, value: String, label: String) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ReportInfoCard(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}
