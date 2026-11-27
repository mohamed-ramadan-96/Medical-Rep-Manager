package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppDialogContainerColor

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material3.AlertDialog

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.room.withTransaction
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.medicalrep.manager.data.MedicalRepDatabase
import com.medicalrep.manager.data.SpecialtyEntity
import kotlinx.coroutines.launch

@Composable
fun SpecialtiesScreen(isArabic: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val database = remember { MedicalRepDatabase.getInstance(context) }
    val dao = database.specialtyDao()
    val doctorDao = database.doctorDao()
    val scope = rememberCoroutineScope()
    val specialties by dao.observeAll().collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf<SpecialtyEntity?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<SpecialtyEntity?>(null) }

    Scaffold(
        modifier = Modifier.padding(padding),
        floatingActionButton = {
            FloatingActionButton(onClick = { editing = null; showForm = true }) {
                Icon(Icons.Default.Add, contentDescription = if (isArabic) "إضافة تخصص" else "Add specialty", modifier = Modifier.size(20.dp))
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier.fillMaxSize().padding(inner).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                if (isArabic) "التخصصات الطبية" else "Medical specialties",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp)
            )
            Text(
                if (isArabic) "عدد التخصصات: ${specialties.size}" else "Specialties: ${specialties.size}",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (specialties.isEmpty()) {
                Text(
                    if (isArabic) "لم تتم إضافة تخصصات بعد. اضغط + لإضافة أول تخصص." else "No specialties yet. Tap + to add your first specialty.",
                    modifier = Modifier.padding(top = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.md)
                ) {
                    items(specialties, key = { it.id }) { specialty ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Text(
                                    specialty.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(modifier = Modifier.size(40.dp), onClick = { editing = specialty; showForm = true }) {
                                    Icon(Icons.Default.Edit, contentDescription = if (isArabic) "تعديل" else "Edit", modifier = Modifier.size(20.dp))
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = { pendingDelete = specialty }) {
                                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        var name by remember(editing) { mutableStateOf(editing?.name.orEmpty()) }
        var nameError by remember { mutableStateOf(false) }
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { showForm = false },
            title = {
                Text(if (editing == null) {
                    if (isArabic) "إضافة تخصص" else "Add specialty"
                } else {
                    if (isArabic) "تعديل التخصص" else "Edit specialty"
                })
            },
            text = {
                OutlinedTextField(
                    colors = AppFilledTextFieldColors(),
                    value = name,
                    onValueChange = { name = it; nameError = false },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text(if (isArabic) "اسم التخصص" else "Specialty name") },
                    isError = nameError
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (name.isBlank()) {
                        nameError = true
                    } else {
                        scope.launch {
                            val item = SpecialtyEntity(
                                id = editing?.id ?: 0L,
                                name = name.trim(),
                                createdAt = editing?.createdAt ?: System.currentTimeMillis()
                            )
                            if (item.id == 0L) {
                                dao.insert(item)
                            } else {
                                database.withTransaction {
                                    dao.update(item)
                                    doctorDao.renameLegacySpecialty(item.id, item.name)
                                }
                            }
                        }
                        showForm = false
                    }
                }) { Text(if (isArabic) "حفظ" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = { showForm = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    pendingDelete?.let { specialty ->
        AlertDialog(
        containerColor = AppDialogContainerColor(),
            onDismissRequest = { pendingDelete = null },
            title = { Text(if (isArabic) "حذف التخصص؟" else "Delete specialty?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف التخصص وفصل الأطباء المرتبطين به، دون حذف بياناتهم." else "This removes the specialty and unlinks its doctors without deleting their records."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        database.withTransaction {
                            doctorDao.detachSpecialty(specialty.id)
                            dao.delete(specialty)
                        }
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
