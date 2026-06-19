package com.kabarinpacar.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kabarinpacar.app.MainActivity
import com.kabarinpacar.app.data.repository.StatusRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class ReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val statusRepository: StatusRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "kabarin_reminder"
        const val CHANNEL_NAME = "Pengingat Status"
        const val NOTIFICATION_ID = 1001
        const val WORK_NAME = "kabarin_reminder_work"
    }

    override suspend fun doWork(): Result {
        val reminderIntervalMs = statusRepository.reminderIntervalHours * 60 * 60 * 1000L
        val latestStatus = statusRepository.getLatestStatus()
        val now = System.currentTimeMillis()

        val shouldRemind = latestStatus == null ||
                (now - latestStatus.timestamp) > reminderIntervalMs

        if (shouldRemind) {
            sendReminderNotification()
        }

        return Result.success()
    }

    private fun sendReminderNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Pengingat untuk update status ke pasangan"
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Kabarin Pacar")
            .setContentText("Jangan lupa update status ke pasanganmu! 💕")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
