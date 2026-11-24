package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSpacing

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.medicalrep.manager.backup.DriveBackupEngine
import com.medicalrep.manager.data.ExcelExporter
import com.medicalrep.manager.data.ExcelImporter
import com.medicalrep.manager.data.DoctorEntity
import com.medicalrep.manager.data.DoctorImportService
import java.io.File
import java.io.FileInputStream
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.LayoutDirection
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
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var showExcelExportConfirmation by remember { mutableStateOf(false) }
    var showExcelImportConfirmation by remember { mutableStateOf(false) }

    val excelExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { ExcelExporter.export(context, uri, doctors, isArabic) }
                Toast.makeText(context, if (isArabic) "تم تصدير أسماء وأرقام الأطباء بنجاح" else "Doctor names and phone numbers exported", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, (if (isArabic) "فشل تصدير Excel: " else "Excel export failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
    val fullBackupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val backup = DriveBackupEngine.createLocalBackup(context)
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            FileInputStream(backup).use { input -> input.copyTo(output) }
                        } ?: error("Unable to open selected destination")
                    } finally { backup.delete() }
                }
                Toast.makeText(context, if (isArabic) "تم تصدير النسخة الكاملة بنجاح" else "Full app backup exported", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, (if (isArabic) "فشل تصدير النسخة: " else "Backup export failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
    val fullBackupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }
    val excelImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val imported = ExcelImporter.readDoctors(context.contentResolver, uri)
                    val importResult = DoctorImportService(database).import(imported)
                    importResult.added to importResult.skippedDuplicates
                }
                Toast.makeText(context, if (isArabic)
                    "تمت إضافة ${result.first} طبيب جديد، وتخطي ${result.second} مكرر."
                    else "Added ${result.first} new doctors; skipped ${result.second} duplicates.", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, (if (isArabic) "فشل استيراد Excel: " else "Excel import failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }

    if (showExcelImportConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExcelImportConfirmation = false },
            title = { Text(if (isArabic) "قبل استيراد ملف Excel" else "Before importing Excel") },
            text = {
                Text(if (isArabic)
                    "يُقبل ملف Excel بعمود واحد لأسماء الأطباء فقط، أو بعمودين: اسم الطبيب ورقم الهاتف. وجود أي عمود ثالث أو إضافي سيؤدي إلى فشل الاستيراد."
                    else "The Excel file may contain one column with doctor names only, or two columns: Doctor Name and Phone Number. Any third or additional column will cause the import to fail.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExcelImportConfirmation = false
                    excelImportLauncher.launch(arrayOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "application/octet-stream", "*/*"))
                }) { Text(if (isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExcelImportConfirmation = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showExcelExportConfirmation) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showExcelExportConfirmation = false },
            title = { Text(if (isArabic) "تأكيد تصدير Excel" else "Confirm Excel export") },
            text = {
                Text(if (isArabic)
                    "سيحتوي ملف Excel على اسم الطبيب ورقم الهاتف فقط، دون أي بيانات أخرى."
                    else "The Excel file will contain only the doctor's name and phone number, with no other data.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    showExcelExportConfirmation = false
                    excelExportLauncher.launch("Doctors.xlsx")
                }) { Text(if (isArabic) "تأكيد" else "Confirm") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExcelExportConfirmation = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    pendingRestoreUri?.let { selectedUri ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingRestoreUri = null },
            title = { Text(if (isArabic) "استعادة النسخة الكاملة؟" else "Restore full backup?") },
            text = {
                Text(if (isArabic)
                    "سيتم استبدال بيانات التطبيق الحالية بالكامل بالبيانات الموجودة في الملف المحدد، بما فيها الأطباء والمراكز والمواعيد والمرفقات والصور. يُفضّل تصدير نسخة من بياناتك الحالية أولاً."
                    else "This will replace all current app data with the selected backup, including doctors, centers, schedules, attachments and photos. Export a backup of your current data first if you may need it.")
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingRestoreUri = null
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                val archive = File(context.cacheDir, "local-restore-${System.currentTimeMillis()}.zip")
                                try {
                                    context.contentResolver.openInputStream(selectedUri)?.use { input ->
                                        archive.outputStream().use { output -> input.copyTo(output) }
                                    } ?: error("Unable to read selected backup")
                                    DriveBackupEngine.restoreFromLocalFile(context, archive)
                                } finally { archive.delete() }
                            }
                            Toast.makeText(context, if (isArabic) "تمت استعادة النسخة الكاملة." else "Full backup restored.", Toast.LENGTH_LONG).show()
                            findDashboardActivity(context)?.recreate()
                        } catch (error: Exception) {
                            Toast.makeText(context, (if (isArabic) "فشلت استعادة النسخة: " else "Restore failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }) { Text(if (isArabic) "استعادة واستبدال البيانات" else "Restore and replace") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingRestoreUri = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    val todayStart = remember { Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis }
    val tomorrowStart = remember(todayStart) { Calendar.getInstance().apply {
        timeInMillis = todayStart; add(Calendar.DAY_OF_YEAR, 1)
    }.timeInMillis }
    val todayVisits = remember(visits, todayStart, tomorrowStart) {
        visits.count { it.visitedAt >= todayStart && it.visitedAt < tomorrowStart }
    }
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
    val monthVisits = remember(visits, monthStart, nextMonthStart) {
        visits.count { it.visitedAt >= monthStart && it.visitedAt < nextMonthStart }
    }
    val yearVisits = remember(visits, yearStart, nextYearStart) {
        visits.count { it.visitedAt >= yearStart && it.visitedAt < nextYearStart }
    }
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
    val outcomeSummary = remember(visits, yearStart, nextYearStart, isArabic) {
        visits.asSequence().filter { it.visitedAt >= yearStart && it.visitedAt < nextYearStart }
            .groupingBy { it.outcome.ifBlank { if (isArabic) "بدون نتيجة" else "No outcome" } }
            .eachCount().entries.sortedByDescending { it.value }.take(3)
    }
    val upcoming = remember(visits, todayStart) {
        visits.asSequence().filter { it.nextFollowUpAt != null && it.nextFollowUpAt >= todayStart }
            .sortedBy { it.nextFollowUpAt }.take(5).toList()
    }
    val recent = remember(visits) { visits.take(5) }
    val doctorNames = remember(doctors) { doctors.associate { it.id to it.name } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(padding),
        contentPadding = PaddingValues(AppSpacing.lg),
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
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { exportMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Text(if (isArabic) "تصدير" else "Export", modifier = Modifier.padding(start = 4.dp))
                    }
                    DropdownMenu(expanded = exportMenuExpanded, onDismissRequest = { exportMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = {
                                if (isArabic) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("تصدير القائمة كملف")
                                            Text("Excel")
                                        }
                                    }
                                } else Text("Export list as Excel file")
                            },
                            onClick = { exportMenuExpanded = false; showExcelExportConfirmation = true }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "نسخة كاملة من التطبيق" else "Full app backup") },
                            onClick = { exportMenuExpanded = false; fullBackupExportLauncher.launch("MedicalRepManager_FullBackup.zip") }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { importMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Text(if (isArabic) "استيراد" else "Import", modifier = Modifier.padding(start = 4.dp))
                    }
                    DropdownMenu(expanded = importMenuExpanded, onDismissRequest = { importMenuExpanded = false }) {
                        DropdownMenuItem(
                            text = {
                                if (isArabic) {
                                    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("استيراد القائمة من ملف")
                                            Text("Excel")
                                        }
                                    }
                                } else Text("Import list from Excel file")
                            },
                            onClick = {
                                importMenuExpanded = false
                                showExcelImportConfirmation = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "استعادة نسخة التطبيق الكاملة" else "Restore full app backup") },
                            onClick = {
                                importMenuExpanded = false
                                fullBackupImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            }
                        )
                    }
                }
            }
        }
        item {
            GoogleDriveBackupCard(isArabic = isArabic)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                MetricCard(Modifier.weight(1f), Icons.Default.PeopleAlt, doctors.size.toString(),
                    if (isArabic) "الأطباء" else "Doctors")
                MetricCard(Modifier.weight(1f), Icons.Default.LocalHospital, centers.size.toString(),
                    if (isArabic) "المراكز" else "Centers")
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
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
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                MetricCard(Modifier.weight(1f), Icons.Default.CalendarMonth, monthVisits.toString(),
                    if (isArabic) "زيارات هذا الشهر" else "This month")
                MetricCard(Modifier.weight(1f), Icons.Default.History, yearVisits.toString(),
                    if (isArabic) "زيارات هذه السنة" else "This year")
            }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
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
    Card(modifier = modifier, shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Card(modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
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
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private tailrec fun findDashboardActivity(context: Context): Activity? = when (context) {
    is Activity -> context
    is ContextWrapper -> findDashboardActivity(context.baseContext)
    else -> null
}
