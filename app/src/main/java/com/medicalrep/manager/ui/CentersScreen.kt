package com.medicalrep.manager.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.animateContentSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CentersScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val dao = database.medicalCenterDao()
    val doctorDao = database.doctorDao()
    val scope = rememberCoroutineScope()
    val centers by dao.observeAll().collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var attachmentsCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }
    var directionsCenter by remember { mutableStateOf<MedicalCenterEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة مركز" else "Add center")
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isArabic) "المراكز والعيادات" else "Medical centers",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (isArabic) "عدد المراكز: ${centers.size}" else "Centers: ${centers.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (centers.isEmpty()) {
                Text(
                    if (isArabic) "لم تتم إضافة مراكز بعد. اضغط + لإضافة أول مركز." else "No centers yet. Tap + to add your first center.",
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(centers, key = { it.id }) { center ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocalHospital, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(center.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    if (center.address.isNotBlank()) Text(center.address, style = MaterialTheme.typography.bodyMedium)
                                    if (center.phone.isNotBlank()) Text(center.phone, style = MaterialTheme.typography.bodyMedium)
                                    if (center.notes.isNotBlank()) Text(center.notes, style = MaterialTheme.typography.bodySmall)
                                }
                                if ((center.latitude != null && center.longitude != null) || !center.mapUrl.isNullOrBlank()) {
                                    IconButton(
                                        modifier = Modifier.size(40.dp),
                                        onClick = { directionsCenter = center }
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = if (isArabic) "الاتجاهات إلى المركز" else "Directions to center",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { attachmentsCenter = center }) {
                                    Icon(Icons.Default.AttachFile, contentDescription = if (isArabic) "المرفقات" else "Attachments", modifier = Modifier.size(20.dp))
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { editing = center; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { pendingDelete = center }) {
                                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(20.dp))
                                }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        CenterFormDialog(
            isArabic = isArabic,
            initial = editing,
            onDismiss = { showForm = false },
            onSave = { center ->
                scope.launch {
                    if (center.id == 0L) dao.insert(center) else dao.update(center)
                }
                showForm = false
            }
        )
    }

    attachmentsCenter?.let { center ->
        CenterAttachmentsDialog(center = center, isArabic = isArabic, database = database, onDismiss = { attachmentsCenter = null })
    }

    directionsCenter?.let { center ->
        AlertDialog(
            onDismissRequest = { directionsCenter = null },
            title = { Text(if (isArabic) "فتح الاتجاهات؟" else "Open directions?") },
            text = {
                Text(
                    if (isArabic) "هل تريد إظهار الاتجاهات إلى ${center.name} على Google Maps؟"
                    else "Would you like to show directions to ${center.name} in Google Maps?"
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

    pendingDelete?.let { center ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف المركز؟" else "Delete center?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف المركز من القائمة وفصل الأطباء المرتبطين به، دون حذف بياناتهم." else "This removes the center and unlinks its doctors. Doctor records will not be deleted."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val attachments = database.centerAttachmentDao().getForCenter(center.id)
                        withContext(Dispatchers.IO) { attachments.forEach { File(it.localPath).delete() } }
                        doctorDao.detachCenter(center.id)
                        dao.delete(center)
                    }
                    pendingDelete = null
                }) { Text(if (isArabic) "حذف" else "Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}

@Composable
private fun CenterFormDialog(
    isArabic: Boolean,
    initial: MedicalCenterEntity?,
    onDismiss: () -> Unit,
    onSave: (MedicalCenterEntity) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }
    var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }
    var notes by remember(initial) { mutableStateOf(initial?.notes.orEmpty()) }
    var latitude by remember(initial) { mutableStateOf(initial?.latitude) }
    var longitude by remember(initial) { mutableStateOf(initial?.longitude) }
    var mapUrl by remember(initial) { mutableStateOf(initial?.mapUrl) }
    var showMapPicker by remember { mutableStateOf(false) }
    var editCenterLocation by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) { if (isArabic) "إضافة مركز أو عيادة" else "Add center or clinic" } else { if (isArabic) "تعديل المركز" else "Edit center" }) },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it; nameError = false },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "اسم المركز *" else "Center name *") },
                        isError = nameError
                    )
                }
                item {
                    OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (isArabic) "العنوان" else "Address") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone") }
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes, onValueChange = { notes = it },
                        modifier = Modifier.fillMaxWidth(), minLines = 2,
                        label = { Text(if (isArabic) "ملاحظات" else "Notes") }
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (isArabic) "موقع المركز على الخريطة" else "Center map location", style = MaterialTheme.typography.labelLarge)
                        if (latitude != null && longitude != null) {
                            CenterMapPreview(
                                latitude = latitude,
                                longitude = longitude,
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                            )
                            Text(
                                String.format(java.util.Locale.US, "%.6f, %.6f", latitude, longitude),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) }
                            ) {
                                Text(if (isArabic) "عرض المكان على الخريطة" else "View place on map")
                            }
                        } else if (!mapUrl.isNullOrBlank()) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { openSavedMapLocation(context, latitude, longitude, mapUrl) }
                            ) {
                                Text(if (isArabic) "عرض المكان على الخريطة" else "View place on map")
                            }
                        } else {
                            Text(
                                if (isArabic) "لم يتم تحديد موقع لهذا المركز بعد." else "No map location has been set for this center.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val hasCenterLocation = (latitude != null && longitude != null) || !mapUrl.isNullOrBlank()
                        Column(modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(220)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (hasCenterLocation) {
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                                    latitude = null; longitude = null; mapUrl = null; editCenterLocation = false
                                }) { Text(if (isArabic) "مسح الموقع" else "Clear location") }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { editCenterLocation = !editCenterLocation }) {
                                    Text(if (isArabic) "تعديل الموقع" else "Edit location")
                                }
                            }
                            AnimatedVisibility(
                                visible = !hasCenterLocation || editCenterLocation,
                                enter = fadeIn(tween(180)) + expandVertically(tween(220)),
                                exit = fadeOut(tween(120)) + shrinkVertically(tween(180))
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                CurrentLocationButton(isArabic = isArabic) { lat, lon ->
                                    latitude = lat; longitude = lon; mapUrl = null; editCenterLocation = false
                                }
                                OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showMapPicker = true }) {
                                    Text(if (isArabic) "اختيار من الخريطة" else "Choose on map")
                                }
                            }
                        }
                    }
                }

            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.isBlank()) nameError = true else onSave(
                    MedicalCenterEntity(
                        id = initial?.id ?: 0L,
                        name = name.trim(),
                        address = address.trim(),
                        phone = phone.trim(),
                        notes = notes.trim(),
                        createdAt = initial?.createdAt ?: System.currentTimeMillis(),
                        latitude = latitude,
                        longitude = longitude,
                        mapUrl = mapUrl
                    )
                )
            }) { Text(if (isArabic) "حفظ" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(if (isArabic) "إلغاء" else "Cancel") }
        }
    )

    if (showMapPicker) {
        CenterMapPickerDialog(
            isArabic = isArabic,
            initialLatitude = latitude,
            initialLongitude = longitude,
            initialMapUrl = mapUrl,
            onDismiss = { showMapPicker = false },
            onConfirm = { lat, lon, url ->
                latitude = lat
                longitude = lon
                mapUrl = url
                showMapPicker = false
            }
        )
    }
}
