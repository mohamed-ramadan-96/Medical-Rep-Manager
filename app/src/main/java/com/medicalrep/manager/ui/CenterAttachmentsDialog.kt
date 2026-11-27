package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppDialogContainerColor

import com.medicalrep.manager.ui.theme.AppSpacing

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.medicalrep.manager.data.CenterAttachmentEntity
import com.medicalrep.manager.data.MedicalCenterEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@Composable
fun CenterAttachmentsDialog(
    center: MedicalCenterEntity,
    isArabic: Boolean,
    database: MedicalRepDatabase,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember(database) { database.centerAttachmentDao() }
    val attachments by dao.observeForCenter(center.id).collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var isCopying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val documentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isCopying = true
                errorMessage = null
                try {
                    withContext(Dispatchers.IO) {
                        val resolver = context.contentResolver
                        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
                        val displayName = resolver.query(uri, null, null, null, null)?.use { cursor ->
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (cursor.moveToFirst() && nameIndex >= 0) cursor.getString(nameIndex) else null
                        } ?: "attachment"
                        val directory = File(context.filesDir, "center_attachments").apply { mkdirs() }
                        val extension = displayName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
                        val destination = File(directory, UUID.randomUUID().toString() + (extension?.let { ".$it" } ?: ""))
                        val input = resolver.openInputStream(uri) ?: error("Unable to read selected file")
                        input.use { source -> destination.outputStream().use { output -> source.copyTo(output) } }
                        dao.insert(
                            CenterAttachmentEntity(
                                centerId = center.id,
                                displayName = displayName,
                                mimeType = mimeType,
                                localPath = destination.absolutePath
                            )
                        )
                    }
                } catch (exception: Exception) {
                    errorMessage = exception.localizedMessage ?: if (isArabic) "تعذر حفظ الملف" else "Could not save file"
                } finally {
                    isCopying = false
                }
            }
        }
    }

    AlertDialog(
        colors = AlertDialogDefaults.colors(containerColor = AppDialogContainerColor()),
        onDismissRequest = onDismiss,
        title = { Text((if (isArabic) "مرفقات " else "Attachments: ") + center.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
                OutlinedButton(
                    onClick = { documentPicker.launch(arrayOf("*/*")) },
                    enabled = !isCopying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, contentDescription = null)
                    Text(if (isArabic) "إضافة ملف أو PDF" else "Add file or PDF", modifier = Modifier.padding(start = 8.dp))
                }
                if (isCopying) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator()
                        Text(if (isArabic) "جارٍ حفظ الملف..." else "Saving file...")
                    }
                }
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (attachments.isEmpty()) {
                    Text(
                        if (isArabic) "لا توجد مرفقات لهذا المركز." else "No attachments for this center.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        items(attachments, key = { it.id }) { attachment ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { openCenterAttachment(context, attachment) }.padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(attachment.displayName, modifier = Modifier.weight(1f).padding(horizontal = 8.dp), maxLines = 2)
                                IconButton(modifier = Modifier.size(40.dp), onClick = { openCenterAttachment(context, attachment) }) {
                                    Icon(Icons.Default.OpenInNew, contentDescription = if (isArabic) "فتح" else "Open")
                                }
                                IconButton(modifier = Modifier.size(40.dp), onClick = {
                                    scope.launch {
                                        dao.delete(attachment)
                                        withContext(Dispatchers.IO) { File(attachment.localPath).delete() }
                                    }
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = if (isArabic) "حذف" else "Delete")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(if (isArabic) "تم" else "Done") } }
    )
}

private fun openCenterAttachment(context: android.content.Context, attachment: CenterAttachmentEntity) {
    val file = File(attachment.localPath)
    if (!file.exists()) return
    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, attachment.mimeType)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, attachment.displayName))
}
