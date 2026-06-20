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

/**
 * Cek status pasangan secara berkala (hemat baterai, tanpa foreground service).
 * Bandingkan timestamp status pasangan dengan yang terakhir dilihat; jika lebih baru,
 * kirim notifikasi. Tidak ada notifikasi permanen.
 */
@HiltWorker
class PartnerCheckWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val statusRepository: StatusRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "partner_status_channel"
        const val CHANNEL_NAME = "Status Pasangan"
        const val NOTIFICATION_ID = 1002
        const val WORK_NAME = "partner_check_work"
    }

    override suspend fun doWork(): Result {
        if (!statusRepository.isPaired) return Result.success()

        val partner = statusRepository.fetchPartnerStatusOnce() ?: return Result.success()
        val lastSeen = statusRepository.partnerLastSeenTimestamp

        if (partner.timestamp > lastSeen && partner.activity.isNotEmpty()) {
            // Jangan notifikasi pada sinkronisasi pertama (lastSeen == 0)
            if (lastSeen != 0L) {
                sendPartnerNotification(partner.nickname, partner.activity, partner.note, partner.locationName)
            }
            statusRepository.setPartnerLastSeenTimestamp(partner.timestamp)
        }

        return Result.success()
    }

    private fun sendPartnerNotification(nickname: String, activity: String, note: String, location: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifikasi saat pasangan update status"
        }
        notificationManager.createNotificationChannel(channel)

        val activityLabel = mapActivityToLabel(activity)
        val contentText = buildString {
            append(activityLabel)
            if (location.isNotEmpty()) append(" • $location")
            if (note.isNotEmpty()) append("\n$note")
        }
        val who = nickname.ifEmpty { "Pasanganmu" }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$who update status 💬")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun mapActivityToLabel(activity: String) = when (activity) {
        "KERJA" -> "Lagi kerja 💼"
        "DI_JALAN" -> "Lagi di jalan 🚗"
        "MAKAN" -> "Lagi makan 🍽️"
        "ISTIRAHAT" -> "Lagi istirahat 😴"
        "LAINNYA" -> "Lagi ada kegiatan"
        else -> activity
    }
}
