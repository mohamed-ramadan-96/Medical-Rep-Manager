package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppSelectionRowColor

import androidx.compose.material3.Surface

import androidx.compose.ui.Alignment

import androidx.compose.material3.OutlinedTextField

import androidx.compose.material3.Checkbox
import androidx.compose.material3.Switch

import androidx.compose.foundation.layout.heightIn

import androidx.core.content.FileProvider

import android.content.Intent

import com.medicalrep.manager.ui.theme.AppDialogContainerColor


import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor

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
import com.medicalrep.manager.data.SharePackageExporter
import com.medicalrep.manager.data.SharePackageImporter
import com.medicalrep.manager.data.SharePlan
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

private const val NO_SPECIALTY_EXPORT_ID = -1L

private data class DashboardBackupResult(
    val title: String,
    val message: String,
    val refreshAfterAcknowledge: Boolean = false
)

private data class DashboardSharePackageResult(
    val title: String,
    val message: String
)

@Composable
fun DashboardScreen(isArabic: Boolean, padding: PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val doctors by database.doctorDao().observeAll().collectAsState(initial = emptyList())
    val specialties by database.specialtyDao().observeAll().collectAsState(initial = emptyList())
    val centers by database.medicalCenterDao().observeAll().collectAsState(initial = emptyList())
    val visits by database.doctorVisitDao().observeAll().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var exportMenuExpanded by remember { mutableStateOf(false) }
    var importMenuExpanded by remember { mutableStateOf(false) }
    var pendingRestoreUri by remember { mutableStateOf<Uri?>(null) }
    var pendingSharePackageUri by remember { mutableStateOf<Uri?>(null) }
    var sharePackagePlan by remember { mutableStateOf<SharePlan?>(null) }
    var sharePackageImportBusy by remember { mutableStateOf(false) }
    var sharePackageImportResult by remember { mutableStateOf<DashboardSharePackageResult?>(null) }
    var localBackupBusy by remember { mutableStateOf(false) }
    var localBackupBusyTitle by remember { mutableStateOf("") }
    var localBackupResult by remember { mutableStateOf<DashboardBackupResult?>(null) }
    var showExcelExportConfirmation by remember { mutableStateOf(false) }
    var showSharePackageExportConfirmation by remember { mutableStateOf(false) }
    var includeSharePackageAttachments by remember { mutableStateOf(false) }
    var sharePackageExportBusy by remember { mutableStateOf(false) }
    var exportDoctorSearch by remember { mutableStateOf("") }
    var selectedExportSpecialtyIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    var selectedExportDoctorIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val specialtyNames = remember(specialties) { specialties.associate { it.id to it.name } }
    val doctorsWithoutSpecialty = remember(doctors, specialtyNames) {
        doctors.filter { doctor ->
            doctor.specialty.trim().ifBlank { doctor.specialtyId?.let(specialtyNames::get)?.trim().orEmpty() }.isBlank()
        }
    }
    val filteredExportDoctors = remember(doctors, specialties, selectedExportSpecialtyIds, selectedExportDoctorIds) {
        if (selectedExportSpecialtyIds.isEmpty() && selectedExportDoctorIds.isEmpty()) {
            doctors
        } else {
            doctors.filter { doctor ->
                val resolvedSpecialty = doctor.specialty.trim()
                    .ifBlank { doctor.specialtyId?.let(specialtyNames::get)?.trim().orEmpty() }
                doctor.id in selectedExportDoctorIds ||
                    (NO_SPECIALTY_EXPORT_ID in selectedExportSpecialtyIds && resolvedSpecialty.isBlank()) ||
                    selectedExportSpecialtyIds.any { specialtyId ->
                        specialtyId != NO_SPECIALTY_EXPORT_ID &&
                            (doctor.specialtyId == specialtyId ||
                                resolvedSpecialty.equals(specialtyNames[specialtyId], ignoreCase = true))
                    }
            }
        }
    }
    val visibleExportDoctors = remember(doctors, exportDoctorSearch) {
        val query = exportDoctorSearch.trim()
        if (query.isBlank()) doctors else doctors.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.phoneNumbers.contains(query, ignoreCase = true) ||
                it.specialty.contains(query, ignoreCase = true) ||
                it.area.contains(query, ignoreCase = true)
        }
    }
    var showExcelImportConfirmation by remember { mutableStateOf(false) }

    val excelExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri ->
        if (uri != null) scope.launch {
            try {
                withContext(Dispatchers.IO) { ExcelExporter.export(context, uri, filteredExportDoctors, isArabic, specialtyNames) }
                Toast.makeText(context, if (isArabic) "تم تصدير بيانات ${filteredExportDoctors.size} طبيب بنجاح" else "Exported ${filteredExportDoctors.size} doctors successfully", Toast.LENGTH_LONG).show()
            } catch (error: Exception) {
                Toast.makeText(context, (if (isArabic) "فشل تصدير Excel: " else "Excel export failed: ") + (error.localizedMessage ?: ""), Toast.LENGTH_LONG).show()
            }
        }
    }
    val sharePackageExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            sharePackageExportBusy = true
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        SharePackageExporter.export(
                            context, database, filteredExportDoctors.map { it.id }.toSet(),
                            includeSharePackageAttachments, output, isArabic
                        )
                    } ?: error("Unable to open selected destination")
                }
                Toast.makeText(
                    context,
                    if (isArabic) "تم تصدير ملف المشاركة بنجاح" else "Share package exported successfully",
                    Toast.LENGTH_LONG
                ).show()
            } catch (error: Exception) {
                Toast.makeText(
                    context,
                    (if (isArabic) "فشل تصدير ملف المشاركة: " else "Share package export failed: ") +
                        (error.localizedMessage ?: ""),
                    Toast.LENGTH_LONG
                ).show()
            } finally { sharePackageExportBusy = false }
        }
    }
    val fullBackupExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) scope.launch {
            localBackupBusy = true
            localBackupBusyTitle = if (isArabic) "جارٍ إنشاء ملف النسخة الكاملة..." else "Creating full backup file..."
            try {
                withContext(Dispatchers.IO) {
                    val backup = DriveBackupEngine.createLocalBackup(context)
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            FileInputStream(backup).use { input -> input.copyTo(output) }
                        } ?: error("Unable to open selected destination")
                    } finally { backup.delete() }
                }
                localBackupResult = DashboardBackupResult(
                    title = if (isArabic) "تم تصدير النسخة بنجاح" else "Backup exported successfully",
                    message = if (isArabic)
                        "تم حفظ ملف النسخة الكاملة في المكان الذي اخترته.\n\nتتضمن النسخة: الأطباء، المراكز، التخصصات، جداول الإتاحة والمواعيد، الزيارات، الصور والمرفقات."
                    else
                        "The full backup file was saved to the location you selected.\n\nIt includes doctors, centers, specialties, availability schedules, appointments, visits, photos and attachments."
                )
            } catch (error: Exception) {
                localBackupResult = DashboardBackupResult(
                    title = if (isArabic) "فشل تصدير النسخة" else "Backup export failed",
                    message = (if (isArabic) "تعذر إنشاء ملف النسخة أو حفظه.\nالسبب: " else "The backup file could not be created or saved.\nReason: ") +
                        (error.localizedMessage ?: error.javaClass.simpleName)
                )
            } finally { localBackupBusy = false }
        }
    }
    val fullBackupImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> pendingRestoreUri = uri }
    val sharePackageImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingSharePackageUri = uri
            sharePackageImportBusy = true
            scope.launch {
                try {
                    sharePackagePlan = withContext(Dispatchers.IO) {
                        SharePackageImporter.plan(context, database, uri)
                    }
                } catch (error: Exception) {
                    pendingSharePackageUri = null
                    Toast.makeText(
                        context,
                        (if (isArabic) "تعذر فحص ملف المشاركة: " else "Could not inspect share package: ") +
                            (error.localizedMessage ?: error.javaClass.simpleName),
                        Toast.LENGTH_LONG
                    ).show()
                } finally { sharePackageImportBusy = false }
            }
        }
    }
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
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showExcelImportConfirmation = false },
            title = { Text(if (isArabic) "قبل استيراد ملف Excel" else "Before importing Excel") },
            text = {
                Text(if (isArabic)
                    "يُقبل ملف Excel بعمود واحد أو عمودين (اسم الطبيب ورقم الهاتف)، أو حتى 4 أعمدة بالترتيب: اسم الطبيب، رقم الهاتف، التخصص، المنطقة. عند وجود عناوين، يُقرأ العمود الثالث والرابع فقط إذا كان عنوانهما التخصص والمنطقة؛ أما الملفات بلا عناوين فتُقرأ الأعمدة الثالثة والرابعة كتخصص ومنطقة."
                    else "Excel files may contain one to four columns in this order: Doctor name, Phone number, Specialty, Area. With headers, columns three and four are read only when labeled Specialty and Area; without headers, columns three and four are treated as specialty and area.")
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
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = { showExcelExportConfirmation = false },
            title = { Text(if (isArabic) "تصدير قائمة الأطباء" else "Export doctors") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) {
                        item {
                            Text(
                                if (isArabic) "سيحتوي الملف على اسم الطبيب ورقم الهاتف والتخصص والمنطقة فقط."
                                else "The file will contain doctor name, phone number, specialty and area only."
                            )
                            Text(
                                if (isArabic) "التخصصات" else "Specialties",
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        item {
                            Column {
                                specialties.forEach { specialty ->
                                    Surface(
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = specialty.id in selectedExportSpecialtyIds,
                                                onCheckedChange = { checked ->
                                                    selectedExportSpecialtyIds = if (checked) {
                                                        selectedExportSpecialtyIds + specialty.id
                                                    } else {
                                                        selectedExportSpecialtyIds - specialty.id
                                                    }
                                                }
                                            )
                                            Text(specialty.name, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                if (doctorsWithoutSpecialty.isNotEmpty()) {
                                    Surface(
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = NO_SPECIALTY_EXPORT_ID in selectedExportSpecialtyIds,
                                                onCheckedChange = { checked ->
                                                    selectedExportSpecialtyIds = if (checked) {
                                                        selectedExportSpecialtyIds + NO_SPECIALTY_EXPORT_ID
                                                    } else {
                                                        selectedExportSpecialtyIds - NO_SPECIALTY_EXPORT_ID
                                                    }
                                                }
                                            )
                                            Text(
                                                if (isArabic) "بدون تخصص" else "No specialty",
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    if (isArabic) "اختيار الأطباء" else "Select doctors",
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = exportDoctorSearch,
                                    onValueChange = { exportDoctorSearch = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = AppFilledTextFieldColors(),
                                    placeholder = { Text(if (isArabic) "ابحث عن طبيب..." else "Search doctors...") }
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            selectedExportDoctorIds =
                                                selectedExportDoctorIds + visibleExportDoctors.map { it.id }
                                        }
                                    ) { Text(if (isArabic) "تحديد الكل" else "Select all") }
                                    androidx.compose.material3.TextButton(
                                        onClick = {
                                            selectedExportDoctorIds =
                                                selectedExportDoctorIds - visibleExportDoctors.map { it.id }.toSet()
                                        }
                                    ) { Text(if (isArabic) "مسح التحديد" else "Clear") }
                                }
                            }
                        }
                        items(visibleExportDoctors, key = { it.id }) { doctor ->
                            Surface(
                                color = AppSelectionRowColor(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = doctor.id in selectedExportDoctorIds,
                                        onCheckedChange = { checked ->
                                            selectedExportDoctorIds = if (checked) {
                                                selectedExportDoctorIds + doctor.id
                                            } else {
                                                selectedExportDoctorIds - doctor.id
                                            }
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doctor.name)
                                        val details = listOf(doctor.specialty, doctor.area)
                                            .filter { it.isNotBlank() }.joinToString(" • ")
                                        if (details.isNotBlank()) {
                                            Text(
                                                details,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Text(
                        (if (isArabic) "عدد الأطباء المُصدّرين: " else "Doctors to export: ") +
                            filteredExportDoctors.size,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Row {
                    androidx.compose.material3.TextButton(onClick = {
                        showExcelExportConfirmation = false
                        excelExportLauncher.launch("Doctors.xlsx")
                    }) { Text(if (isArabic) "حفظ الملف" else "Save file") }
                    androidx.compose.material3.TextButton(onClick = {
                        showExcelExportConfirmation = false
                        scope.launch {
                            try {
                                val sharedFile = withContext(Dispatchers.IO) {
                                    val directory = File(context.cacheDir, "shared_exports")
                                    if (!directory.exists() && !directory.mkdirs()) {
                                        error("Unable to create shared export directory")
                                    }
                                    val file = File(directory, "Doctors.xlsx")
                                    file.outputStream().use { output ->
                                        ExcelExporter.export(output, filteredExportDoctors, isArabic, specialtyNames)
                                    }
                                    file
                                }
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    sharedFile
                                )
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(
                                    Intent.createChooser(
                                        sendIntent,
                                        if (isArabic) "مشاركة ملف الأطباء" else "Share doctors file"
                                    )
                                )
                            } catch (error: Exception) {
                                Toast.makeText(
                                    context,
                                    (if (isArabic) "فشل تصدير Excel: " else "Excel export failed: ") +
                                        (error.localizedMessage ?: ""),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }) { Text(if (isArabic) "مشاركة" else "Share") }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showExcelExportConfirmation = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showSharePackageExportConfirmation) {
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = { if (!sharePackageExportBusy) showSharePackageExportConfirmation = false },
            title = { Text(if (isArabic) "مشاركة بيانات التطبيق" else "Share app data") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        if (isArabic) "لا يتضمن الزيارات أو الخطة الأسبوعية أو المتابعات"
                        else "Visits, weekly plan and follow-ups are not included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(if (isArabic) "التخصصات" else "Specialties", fontWeight = FontWeight.SemiBold)
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        item {
                            Column {
                                specialties.forEach { specialty ->
                                    Surface(
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = specialty.id in selectedExportSpecialtyIds,
                                                onCheckedChange = { checked ->
                                                    selectedExportSpecialtyIds = if (checked) selectedExportSpecialtyIds + specialty.id
                                                    else selectedExportSpecialtyIds - specialty.id
                                                }
                                            )
                                            Text(specialty.name, modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                                if (doctorsWithoutSpecialty.isNotEmpty()) {
                                    Surface(
                                        color = AppSelectionRowColor(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = NO_SPECIALTY_EXPORT_ID in selectedExportSpecialtyIds,
                                                onCheckedChange = { checked ->
                                                    selectedExportSpecialtyIds = if (checked) selectedExportSpecialtyIds + NO_SPECIALTY_EXPORT_ID
                                                    else selectedExportSpecialtyIds - NO_SPECIALTY_EXPORT_ID
                                                }
                                            )
                                            Text(if (isArabic) "بدون تخصص" else "No specialty", modifier = Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (isArabic) "اختيار الأطباء" else "Select doctors", fontWeight = FontWeight.SemiBold)
                                OutlinedTextField(
                                    value = exportDoctorSearch,
                                    onValueChange = { exportDoctorSearch = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = AppFilledTextFieldColors(),
                                    placeholder = { Text(if (isArabic) "ابحث عن طبيب..." else "Search doctors...") }
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    androidx.compose.material3.TextButton(
                                        onClick = { selectedExportDoctorIds = selectedExportDoctorIds + visibleExportDoctors.map { it.id } }
                                    ) { Text(if (isArabic) "تحديد الكل" else "Select all") }
                                    androidx.compose.material3.TextButton(
                                        onClick = { selectedExportDoctorIds = selectedExportDoctorIds - visibleExportDoctors.map { it.id }.toSet() }
                                    ) { Text(if (isArabic) "مسح التحديد" else "Clear") }
                                }
                            }
                        }
                        items(visibleExportDoctors, key = { it.id }) { doctor ->
                            Surface(
                                color = AppSelectionRowColor(),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = doctor.id in selectedExportDoctorIds,
                                        onCheckedChange = { checked ->
                                            selectedExportDoctorIds = if (checked) selectedExportDoctorIds + doctor.id
                                            else selectedExportDoctorIds - doctor.id
                                        }
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(doctor.name)
                                        val details = listOf(doctor.specialty, doctor.area).filter { it.isNotBlank() }.joinToString(" • ")
                                        if (details.isNotBlank()) {
                                            Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Surface(
                        color = AppSelectionRowColor(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (isArabic) "تضمين المرفقات والصور" else "Include attachments and photos",
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = includeSharePackageAttachments,
                                onCheckedChange = { includeSharePackageAttachments = it }
                            )
                        }
                    }
                    Text(
                        (if (isArabic) "عدد الأطباء المُشارَك بهم: " else "Doctors to share: ") + filteredExportDoctors.size,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            confirmButton = {
                Row {
                    androidx.compose.material3.TextButton(
                        enabled = !sharePackageExportBusy,
                        onClick = {
                            showSharePackageExportConfirmation = false
                            sharePackageExportLauncher.launch(SharePackageExporter.suggestedFileName())
                        }
                    ) { Text(if (isArabic) "حفظ الملف" else "Save file") }
                    androidx.compose.material3.TextButton(
                        enabled = !sharePackageExportBusy,
                        onClick = {
                            showSharePackageExportConfirmation = false
                            scope.launch {
                                sharePackageExportBusy = true
                                try {
                                    val sharedFile = withContext(Dispatchers.IO) {
                                        val directory = File(context.cacheDir, "shared_exports")
                                        if (!directory.exists() && !directory.mkdirs()) error("Unable to create shared export directory")
                                        val file = File(directory, SharePackageExporter.suggestedFileName())
                                        file.outputStream().use { output ->
                                            SharePackageExporter.export(
                                                context, database, filteredExportDoctors.map { it.id }.toSet(),
                                                includeSharePackageAttachments, output, isArabic
                                            )
                                        }
                                        file
                                    }
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", sharedFile)
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/zip"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, if (isArabic) "مشاركة ملف بيانات التطبيق" else "Share app data file"))
                                } catch (error: Exception) {
                                    Toast.makeText(
                                        context,
                                        (if (isArabic) "فشل تصدير ملف المشاركة: " else "Share package export failed: ") +
                                            (error.localizedMessage ?: ""),
                                        Toast.LENGTH_LONG
                                    ).show()
                                } finally { sharePackageExportBusy = false }
                            }
                        }
                    ) { Text(if (isArabic) "مشاركة" else "Share") }
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    enabled = !sharePackageExportBusy,
                    onClick = { showSharePackageExportConfirmation = false }
                ) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    sharePackagePlan?.let { plan ->
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = {
                if (!sharePackageImportBusy) {
                    sharePackagePlan = null
                    pendingSharePackageUri = null
                }
            },
            title = { Text(if (isArabic) "تأكيد استيراد ملف المشاركة" else "Confirm share package import") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (isArabic) "ملخص البيانات التي سيتم دمجها:" else "Summary of the data to be merged:")
                    Text(
                        (if (isArabic) "أطباء جدد: " else "New doctors: ") + plan.newDoctors
                    )
                    Text(
                        (if (isArabic) "أطباء سيتم تحديث بياناتهم: " else "Doctors to update: ") + plan.updatedDoctors
                    )
                    Text(
                        (if (isArabic) "أسماء متطابقة بأرقام مختلفة: " else "Same name, different phone: ") + plan.sameNameDifferentPhone
                    )
                    Text(
                        (if (isArabic) "مراكز جديدة: " else "New centers: ") + plan.newCenters
                    )
                    Text(
                        (if (isArabic) "مراكز سيتم تحديثها: " else "Centers to update: ") + plan.updatedCenters
                    )
                    Text(
                        (if (isArabic) "مواعيد الإتاحة المضافة (الأطباء): " else "Availability slots to add (doctors): ") + plan.doctorSlotsToAdd
                    )
                    Text(
                        (if (isArabic) "مواعيد الإتاحة المضافة (المراكز): " else "Availability slots to add (centers): ") + plan.centerSlotsToAdd
                    )
                    Text(
                        (if (isArabic) "يتضمن المرفقات والصور: " else "Includes attachments and photos: ") +
                            if (plan.includesAttachments) (if (isArabic) "نعم" else "Yes") else (if (isArabic) "لا" else "No")
                    )
                    Text(
                        if (isArabic) "لا يتضمن الزيارات أو الخطة الأسبوعية أو المتابعات"
                        else "Visits, weekly plan and follow-ups are not included.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (plan.warnings.isNotEmpty()) {
                        plan.warnings.forEach { warning ->
                            Text(warning, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = !sharePackageImportBusy,
                    onClick = {
                        val selectedUri = pendingSharePackageUri
                        if (selectedUri != null) {
                            sharePackagePlan = null
                            pendingSharePackageUri = null
                            sharePackageImportBusy = true
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        SharePackageImporter.apply(context, database, selectedUri)
                                    }
                                    sharePackageImportResult = DashboardSharePackageResult(
                                        title = if (isArabic) "تم استيراد ملف المشاركة" else "Share package imported",
                                        message = buildString {
                                            append(if (isArabic) "أطباء جدد: " else "New doctors: ")
                                            append(result.newDoctors)
                                            append("\n")
                                            append(if (isArabic) "أطباء تم تحديثهم: " else "Doctors updated: ")
                                            append(result.updatedDoctors)
                                            append("\n")
                                            append(if (isArabic) "أسماء متطابقة بأرقام مختلفة: " else "Same name, different phone: ")
                                            append(result.sameNameDifferentPhone)
                                            append("\n")
                                            append(if (isArabic) "مراكز جديدة: " else "New centers: ")
                                            append(result.newCenters)
                                            append("\n")
                                            append(if (isArabic) "مراكز تم تحديثها: " else "Centers updated: ")
                                            append(result.updatedCenters)
                                            append("\n")
                                            append(if (isArabic) "مواعيد إتاحة الأطباء المضافة: " else "Doctor availability slots added: ")
                                            append(result.doctorSlotsToAdd)
                                            append("\n")
                                            append(if (isArabic) "مواعيد إتاحة المراكز المضافة: " else "Center availability slots added: ")
                                            append(result.centerSlotsToAdd)
                                            append("\n")
                                            append(if (isArabic) "المرفقات والصور المنسوخة: " else "Attachments and photos copied: ")
                                            append(result.attachmentsToCopy)
                                        }
                                    )
                                } catch (error: Exception) {
                                    sharePackageImportResult = DashboardSharePackageResult(
                                        title = if (isArabic) "فشل استيراد ملف المشاركة" else "Share package import failed",
                                        message = error.localizedMessage ?: error.javaClass.simpleName
                                    )
                                } finally { sharePackageImportBusy = false }
                            }
                        }
                    }
                ) { Text(if (isArabic) "استيراد" else "Import") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    enabled = !sharePackageImportBusy,
                    onClick = {
                        sharePackagePlan = null
                        pendingSharePackageUri = null
                    }
                ) { Text(if (isArabic) "إلغاء" else "Cancel") }
            }
        )
    }

    if (!sharePackageImportBusy) sharePackageImportResult?.let { result ->
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = { sharePackageImportResult = null },
            title = { Text(result.title) },
            text = { Text(result.message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { sharePackageImportResult = null }) {
                    Text(if (isArabic) "موافق" else "OK")
                }
            }
        )
    }

    if (sharePackageImportBusy) {
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = {},
            title = { Text(if (isArabic) "جارٍ استيراد ملف المشاركة..." else "Importing share package...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (isArabic) "من فضلك انتظر حتى تنتهي العملية." else "Please wait for the operation to finish.")
                }
            },
            confirmButton = {}
        )
    }

    pendingRestoreUri?.let { selectedUri ->
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
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
                    localBackupBusy = true
                    localBackupBusyTitle = if (isArabic) "جارٍ استعادة النسخة..." else "Restoring backup..."
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
                            localBackupResult = DashboardBackupResult(
                                title = if (isArabic) "تمت استعادة النسخة بنجاح" else "Backup restored successfully",
                                message = if (isArabic)
                                    "تمت استعادة بيانات النسخة الكاملة، وتشمل:\n• الأطباء والمراكز\n• التخصصات وجداول الإتاحة والمواعيد\n• الزيارات\n• صور الأطباء ومرفقات الأطباء والمراكز\n\nاضغط موافق لتحديث التطبيق."
                                else
                                    "The full backup was restored, including:\n• Doctors and centers\n• Specialties, availability schedules and appointments\n• Visits\n• Doctor photos and doctor/center attachments\n\nPress OK to refresh the app.",
                                refreshAfterAcknowledge = true
                            )
                        } catch (error: Exception) {
                            val reason = error.localizedMessage ?: error.javaClass.simpleName
                            val rollbackIncomplete = reason.startsWith("Restore failed and rollback was incomplete:")
                            localBackupResult = DashboardBackupResult(
                                title = if (isArabic) "فشلت استعادة النسخة" else "Backup restore failed",
                                message = if (isArabic)
                                    "تعذر استعادة النسخة.\nالسبب: $reason\n\n" + if (rollbackIncomplete)
                                        "تعذر تأكيد اكتمال التراجع عن الاستعادة. تم الاحتفاظ بملفات الاسترداد داخل ذاكرة التطبيق."
                                    else "تم الاحتفاظ ببياناتك الحالية؛ لم تكتمل الاستعادة وتم التراجع عن التغييرات."
                                else
                                    "The backup could not be restored.\nReason: $reason\n\n" + if (rollbackIncomplete)
                                        "The rollback could not be fully confirmed. Recovery files were preserved in the app cache."
                                    else "Your existing data was kept; the restore did not complete and its changes were rolled back."
                            )
                        } finally { localBackupBusy = false }
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

    if (sharePackageExportBusy) {
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = {},
            title = { Text(if (isArabic) "جارٍ إنشاء ملف المشاركة..." else "Creating share package...") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (isArabic) "من فضلك انتظر حتى تنتهي العملية." else "Please wait for the operation to finish.")
                }
            },
            confirmButton = {}
        )
    }

    if (localBackupBusy) {
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = {},
            title = { Text(localBackupBusyTitle) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(if (isArabic) "من فضلك انتظر حتى تنتهي العملية. لا تغلق التطبيق." else "Please wait for the operation to finish. Do not close the app.")
                }
            },
            confirmButton = {}
        )
    }

    if (!localBackupBusy) localBackupResult?.let { result ->
        androidx.compose.material3.AlertDialog(
            containerColor = AppDialogContainerColor(),
            shape = RoundedCornerShape(10.dp),
            onDismissRequest = {},
            title = { Text(result.title) },
            text = { Text(result.message) },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    val refresh = result.refreshAfterAcknowledge
                    localBackupResult = null
                    if (refresh) findDashboardActivity(context)?.recreate()
                }) { Text(if (isArabic) "موافق" else "OK") }
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
                    Button(onClick = { exportMenuExpanded = true }, enabled = !localBackupBusy && !sharePackageExportBusy && !sharePackageImportBusy, modifier = Modifier.fillMaxWidth()) {
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
                            text = { Text(if (isArabic) "مشاركة بيانات التطبيق (ملف MRM)" else "Share app data (MRM file)") },
                            onClick = {
                                exportMenuExpanded = false
                                includeSharePackageAttachments = false
                                showSharePackageExportConfirmation = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isArabic) "نسخة كاملة من التطبيق" else "Full app backup") },
                            onClick = { exportMenuExpanded = false; fullBackupExportLauncher.launch("MedicalRepManager_FullBackup.zip") }
                        )
                    }
                }
                Box(modifier = Modifier.weight(1f)) {
                    Button(onClick = { importMenuExpanded = true }, enabled = !localBackupBusy && !sharePackageExportBusy && !sharePackageImportBusy, modifier = Modifier.fillMaxWidth()) {
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
                            text = { Text(if (isArabic) "استيراد ملف مشاركة" else "Import shared package") },
                            onClick = {
                                importMenuExpanded = false
                                sharePackageImportLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
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
                colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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
        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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
        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())) {
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
    Card(
        Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
    ) {
        Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}


private tailrec fun findDashboardActivity(context: Context): Activity? = when (context) {
    is Activity -> context
    is ContextWrapper -> findDashboardActivity(context.baseContext)
    else -> null
}
