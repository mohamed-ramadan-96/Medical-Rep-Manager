package com.medicalrep.manager.followup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.medicalrep.manager.MainActivity
import com.medicalrep.manager.R
import com.medicalrep.manager.data.DoctorVisitEntity
import com.medicalrep.manager.data.MedicalRepDatabase
import java.util.concurrent.TimeUnit

object FollowUpReminderScheduler {
    private const val CHANNEL_ID = "followup_reminders"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val preferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val channelName = if (preferences.getBoolean("arabic_language", true)) {
            "تذكير المتابعة"
        } else {
            "Follow-up reminders"
        }
        val channel = NotificationChannel(
            CHANNEL_ID,
            channelName,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Reminders for scheduled doctor follow-ups"
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    fun schedule(context: Context, visit: DoctorVisitEntity) {
        val followUpAt = visit.nextFollowUpAt
        if (followUpAt == null || followUpAt <= System.currentTimeMillis() || visit.id <= 0L) {
            cancel(context, visit.id)
            return
        }
        val request = OneTimeWorkRequestBuilder<FollowUpReminderWorker>()
            .setInitialDelay(
                (followUpAt - System.currentTimeMillis()).coerceAtLeast(0L),
                TimeUnit.MILLISECONDS
            )
            .setInputData(
                Data.Builder()
                    .putLong(FollowUpReminderWorker.KEY_VISIT_ID, visit.id)
                    .putLong(FollowUpReminderWorker.KEY_DOCTOR_ID, visit.doctorId)
                    .putLong(FollowUpReminderWorker.KEY_EXPECTED_FOLLOW_UP_AT, followUpAt)
                    .build()
            )
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "followup_${visit.id}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancel(context: Context, visitId: Long) {
        if (visitId <= 0L) return
        WorkManager.getInstance(context).cancelUniqueWork("followup_$visitId")
        NotificationManagerCompat.from(context).cancel(visitId.toInt())
    }

    suspend fun rescheduleAll(context: Context) {
        val visits = MedicalRepDatabase.getInstance(context).doctorVisitDao()
            .getUpcomingFollowUps(System.currentTimeMillis())
        visits.forEach { schedule(context, it) }
    }
}

class FollowUpReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val visitId = inputData.getLong(KEY_VISIT_ID, -1L)
        val doctorId = inputData.getLong(KEY_DOCTOR_ID, -1L)
        val expectedFollowUpAt = inputData.getLong(KEY_EXPECTED_FOLLOW_UP_AT, -1L)
        if (visitId <= 0L || doctorId <= 0L || expectedFollowUpAt <= 0L) {
            return Result.success()
        }

        val database = MedicalRepDatabase.getInstance(applicationContext)
        val visit = database.doctorVisitDao().getById(visitId) ?: return Result.success()
        if (
            visit.doctorId != doctorId ||
            visit.nextFollowUpAt != expectedFollowUpAt
        ) {
            return Result.success()
        }
        val doctor = database.doctorDao().getById(doctorId) ?: return Result.success()
        if (!NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
            return Result.success()
        }

        FollowUpReminderScheduler.ensureChannel(applicationContext)
        val notificationText = buildString {
            append("متابعة الدكتور ")
            append(doctor.name)
            append(" اليوم")
            if (visit.notes.isNotBlank()) {
                val shortenedNotes = visit.notes.trim().let {
                    if (it.length > 80) it.take(80).trimEnd() + "…" else it
                }
                append(" — ")
                append(shortenedNotes)
            }
        }
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("open_doctor_id", doctorId)
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            visitId.toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("تذكير متابعة")
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()

        runCatching {
            NotificationManagerCompat.from(applicationContext).notify(visitId.toInt(), notification)
        }
        return Result.success()
    }

    companion object {
        const val KEY_VISIT_ID = "visit_id"
        const val KEY_DOCTOR_ID = "doctor_id"
        const val KEY_EXPECTED_FOLLOW_UP_AT = "expected_follow_up_at"
        private const val CHANNEL_ID = "followup_reminders"
    }
}
