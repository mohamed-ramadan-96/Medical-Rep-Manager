package com.medicalrep.manager.ui

import com.medicalrep.manager.ui.theme.AppDialogContainerColor

import com.medicalrep.manager.ui.theme.AppSpacing
import com.medicalrep.manager.ui.theme.AppCardContainerColor

import android.app.Activity
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkManager
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.medicalrep.manager.backup.DriveBackupEngine
import com.medicalrep.manager.backup.DriveBackupItem
import com.medicalrep.manager.backup.DriveBackupScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GoogleDriveBackupCard(isArabic: Boolean) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val prefs = remember { context.getSharedPreferences(DriveBackupScheduler.PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean(DriveBackupScheduler.KEY_ENABLED, false)) }
    var interval by remember { mutableStateOf(prefs.getInt(DriveBackupScheduler.KEY_INTERVAL, 1)) }
    var menuExpanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var token by remember { mutableStateOf<String?>(null) }
    var pendingAction by remember { mutableStateOf<((String) -> Unit)?>(null) }
    var showBackups by remember { mutableStateOf(false) }
    var backupPendingDelete by remember { mutableStateOf<DriveBackupItem?>(null) }
    var backupPendingDeleteAll by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var backups by remember { mutableStateOf<List<DriveBackupItem>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun notify(message: String) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    fun showError(message: String) { errorMessage = message }
    fun onAuthorized(result: AuthorizationResult) {
        val accessToken = result.accessToken
        if (!accessToken.isNullOrBlank()) token = accessToken
    }
    val authorizationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            try {
                val authorization = Identity.getAuthorizationClient(activity).getAuthorizationResultFromIntent(result.data!!)
                onAuthorized(authorization)
                authorization.accessToken?.let { access -> pendingAction?.also { action -> pendingAction = null; action(access) } }
            } catch (error: Exception) {
                showError((if (isArabic) "تعذر ربط Google Drive: " else "Google Drive authorization failed: ") + (error.localizedMessage ?: error.javaClass.simpleName))
            }
        }
    }

    fun authorize(after: (String) -> Unit) {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope("https://www.googleapis.com/auth/drive.file")))
            .build()
        Identity.getAuthorizationClient(activity).authorize(request)
            .addOnSuccessListener { result ->
                if (result.hasResolution()) {
                    val pending = result.pendingIntent
                    if (pending != null) {
                        pendingAction = after
                        authorizationLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
                    } else {
                        showError(if (isArabic) "تعذر بدء طلب إذن Google Drive." else "Could not start Google Drive authorization.")
                    }
                } else {
                    val access = result.accessToken
                    if (!access.isNullOrBlank()) {
                        token = access
                        after(access)
                    } else showError(if (isArabic) "لم يتم الحصول على تصريح Google Drive." else "Google Drive did not return an access token.")
                }
            }
            .addOnFailureListener { error ->
                showError((if (isArabic) "فشل الاتصال: " else "Connection failed: ") + (error.localizedMessage ?: error.javaClass.simpleName))
            }
    }

    fun runBackup(access: String) {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val local = DriveBackupEngine.createLocalBackup(context)
                    try { DriveBackupEngine.upload(context, access, local) } finally { local.delete() }
                }
                prefs.edit().putLong(DriveBackupScheduler.KEY_LAST_BACKUP, System.currentTimeMillis()).remove(DriveBackupScheduler.KEY_LAST_ERROR).apply()
                notify(if (isArabic) "تم حفظ النسخة الاحتياطية على Google Drive." else "Backup saved to Google Drive.")
                activity.recreate()
            } catch (error: Exception) {
                showError((if (isArabic) "فشل النسخ الاحتياطي: " else "Backup failed: ") + (error.localizedMessage ?: error.javaClass.simpleName))
            } finally { busy = false }
        }
    }

    fun loadBackups(access: String) {
        scope.launch {
            busy = true
            try {
                backups = withContext(Dispatchers.IO) { DriveBackupEngine.list(access) }
                showBackups = true
            } catch (error: Exception) {
                showError((if (isArabic) "تعذر تحميل النسخ: " else "Could not load backups: ") + (error.localizedMessage ?: error.javaClass.simpleName))
            } finally { busy = false }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (isArabic) "النسخ الاحتياطي على Google Drive" else "Google Drive Backup", style = MaterialTheme.typography.titleMedium)
            Text(if (isArabic) "احفظ قاعدة البيانات والصور ومرفقات الأطباء والمراكز في حسابك." else "Back up your database, doctor photos, and doctor/center attachments to your own Drive account.")
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(if (isArabic) "النسخ التلقائي" else "Automatic backup")
                Switch(checked = enabled, onCheckedChange = {
                    enabled = it
                    DriveBackupScheduler.configure(context, enabled, interval)
                })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)
            ) {
                Text(
                    if (isArabic) "تكرار النسخ:" else "Backup frequency:",
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.foundation.layout.Box {
                    OutlinedButton(onClick = { menuExpanded = true }) {
                        Text(when (interval) {
                            1 -> if (isArabic) "يوميًا" else "Daily"
                            2 -> if (isArabic) "كل يومين" else "Every 2 days"
                            else -> if (isArabic) "أسبوعيًا" else "Weekly"
                        })
                    }
                    DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                        listOf(1, 2, 7).forEach { days ->
                            DropdownMenuItem(
                                text = { Text(when (days) { 1 -> if (isArabic) "يوميًا" else "Daily"; 2 -> if (isArabic) "كل يومين" else "Every 2 days"; else -> if (isArabic) "أسبوعيًا" else "Weekly" }) },
                                onClick = {
                                    interval = days
                                    menuExpanded = false
                                    DriveBackupScheduler.configure(context, enabled, interval)
                                }
                            )
                        }
                    }
                }
            }
            val lastBackup = prefs.getLong(DriveBackupScheduler.KEY_LAST_BACKUP, 0L)
            val lastError = prefs.getString(DriveBackupScheduler.KEY_LAST_ERROR, null)
            Text(
                if (lastBackup == 0L) {
                    if (isArabic) "لم يتم إنشاء نسخة احتياطية بعد." else "No backup has been completed yet."
                } else {
                    (if (isArabic) "آخر نسخة ناجحة: " else "Last successful backup: ") +
                        SimpleDateFormat("yyyy-MM-dd  HH:mm", Locale.getDefault()).format(Date(lastBackup))
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!lastError.isNullOrBlank()) {
                Text(
                    (if (isArabic) "آخر خطأ في النسخ التلقائي: " else "Last automatic backup error: ") + lastError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    if (token != null) runBackup(token!!)
                    else authorize { runBackup(it) }
                }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(if (busy) (if (isArabic) "جارٍ التنفيذ..." else "Working...") else (if (isArabic) "نسخ الآن" else "Back up now"))
                }
                OutlinedButton(onClick = {
                    if (token != null) loadBackups(token!!)
                    else authorize { loadBackups(it) }
                }, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(if (isArabic) "استعادة" else "Restore")
                }
            }
            Text(if (isArabic) "سيُطلب إذن Google Drive عند أول استخدام. النسخ التلقائي يعمل عند توفر الإنترنت والتصريح." else "Google Drive permission is requested on first use. Automatic backups run when internet and authorization are available.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    errorMessage?.let { message ->
        AlertDialog(
        colors = AlertDialogDefaults.colors(containerColor = AppDialogContainerColor()),
            onDismissRequest = { errorMessage = null },
            title = { Text(if (isArabic) "تفاصيل الخطأ" else "Error details") },
            text = { Text(message) },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Medical Rep Manager error", message))
                        errorMessage = null
                        notify(if (isArabic) "تم نسخ تفاصيل الخطأ." else "Error details copied.")
                    }) { Text(if (isArabic) "نسخ الخطأ" else "Copy error") }
                    TextButton(onClick = { errorMessage = null }) { Text(if (isArabic) "إغلاق" else "Close") }
                }
            }
        )
    }

    if (backupPendingDeleteAll) {
        AlertDialog(
        colors = AlertDialogDefaults.colors(containerColor = AppDialogContainerColor()),
            onDismissRequest = { if (!busy) backupPendingDeleteAll = false },
            title = { Text(if (isArabic) "حذف كل النسخ الاحتياطية؟" else "Delete all backups?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف جميع النسخ الاحتياطية وعددها ${backups.size} من Google Drive نهائيًا. لن تتمكن من استعادتها بعد الحذف. تأكد من أنك لا تحتاج إلى أي نسخة منها."
                    else "All ${backups.size} backups will be permanently deleted from Google Drive. They cannot be restored afterward. Make sure you no longer need any of them."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy && backups.isNotEmpty(),
                    onClick = {
                        val access = token
                        if (access != null) {
                            scope.launch {
                                busy = true
                                try {
                                    val deleted = withContext(Dispatchers.IO) { DriveBackupEngine.deleteAll(access) }
                                    backups = emptyList()
                                    backupPendingDeleteAll = false
                                    notify(
                                        if (isArabic) "تم حذف $deleted نسخة احتياطية."
                                        else "$deleted backups deleted."
                                    )
                                } catch (error: Exception) {
                                    try {
                                        backups = withContext(Dispatchers.IO) { DriveBackupEngine.list(access) }
                                    } catch (_: Exception) { }
                                    backupPendingDeleteAll = false
                                    showError(
                                        (if (isArabic) "تعذر إكمال حذف كل النسخ. تم تحديث القائمة بالنسخ المتبقية إن أمكن. التفاصيل: "
                                         else "Could not delete all backups. The list was refreshed with any remaining backups when possible. Details: ") +
                                            (error.localizedMessage ?: error.javaClass.simpleName)
                                    )
                                } finally { busy = false }
                            }
                        }
                    }
                ) { Text(if (isArabic) "حذف الكل نهائيًا" else "Delete all permanently") }
            },
            dismissButton = {
                TextButton(onClick = { backupPendingDeleteAll = false }, enabled = !busy) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    backupPendingDelete?.let { item ->
        AlertDialog(
        colors = AlertDialogDefaults.colors(containerColor = AppDialogContainerColor()),
            onDismissRequest = { if (!busy) backupPendingDelete = null },
            title = { Text(if (isArabic) "حذف النسخة الاحتياطية؟" else "Delete backup?") },
            text = {
                Text(
                    if (isArabic) "سيتم حذف النسخة المحددة نهائيًا من Google Drive، ولن يمكن استعادتها بعد ذلك.\n\n${item.name}"
                    else "This backup will be permanently deleted from Google Drive and cannot be restored.\n\n${item.name}"
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        val access = token
                        if (access != null) {
                            scope.launch {
                                busy = true
                                try {
                                    withContext(Dispatchers.IO) { DriveBackupEngine.delete(access, item) }
                                    backups = backups.filterNot { it.id == item.id }
                                    backupPendingDelete = null
                                    notify(if (isArabic) "تم حذف النسخة الاحتياطية." else "Backup deleted.")
                                } catch (error: Exception) {
                                    showError((if (isArabic) "تعذر حذف النسخة: " else "Could not delete backup: ") + (error.localizedMessage ?: error.javaClass.simpleName))
                                } finally { busy = false }
                            }
                        }
                    }
                ) { Text(if (isArabic) "حذف نهائي" else "Delete permanently") }
            },
            dismissButton = {
                TextButton(onClick = { backupPendingDelete = null }, enabled = !busy) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    if (showBackups) {
        AlertDialog(
        colors = AlertDialogDefaults.colors(containerColor = AppDialogContainerColor()),
            onDismissRequest = { showBackups = false },
            title = { Text(if (isArabic) "النسخ المتاحة" else "Available backups") },
            text = {
                if (backups.isEmpty()) Text(if (isArabic) "لا توجد نسخ احتياطية." else "No backups found.")
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            OutlinedButton(
                                onClick = { backupPendingDeleteAll = true },
                                enabled = !busy && backups.isNotEmpty()
                            ) {
                                Text(if (isArabic) "حذف كل النسخ (${backups.size})" else "Delete all (${backups.size})")
                            }
                        }
                    }
                    items(backups, key = { it.id }) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = AppCardContainerColor())
                        ) {
                            Column(Modifier.padding(AppSpacing.md), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                Text(item.modifiedTime, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = {
                                            scope.launch {
                                                busy = true
                                                try {
                                                    withContext(Dispatchers.IO) { DriveBackupEngine.restore(context, token!!, item) }
                                                    prefs.edit().putLong(DriveBackupScheduler.KEY_LAST_BACKUP, System.currentTimeMillis()).apply()
                                                    showBackups = false
                                                    notify(if (isArabic) "تمت استعادة النسخة، سيتم تحديث التطبيق الآن." else "Backup restored. The app will refresh now.")
                                                    activity.recreate()
                                                } catch (error: Exception) {
                                                    showError((if (isArabic) "فشلت الاستعادة: " else "Restore failed: ") + (error.localizedMessage ?: error.javaClass.simpleName))
                                                    activity.recreate()
                                                } finally { busy = false }
                                            }
                                        },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (isArabic) "استعادة" else "Restore") }
                                    OutlinedButton(
                                        onClick = { backupPendingDelete = item },
                                        enabled = !busy,
                                        modifier = Modifier.weight(1f)
                                    ) { Text(if (isArabic) "حذف" else "Delete") }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBackups = false }) { Text(if (isArabic) "إغلاق" else "Close") } }
        )
    }
}

private tailrec fun Context.findActivity(): Activity =
    when (this) {
        is Activity -> this
        is android.content.ContextWrapper -> baseContext.findActivity()
        else -> error("Activity context is unavailable")
    }
