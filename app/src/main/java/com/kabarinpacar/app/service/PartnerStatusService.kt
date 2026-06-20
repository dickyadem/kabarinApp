package com.kabarinpacar.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.kabarinpacar.app.MainActivity
import com.kabarinpacar.app.R
import com.kabarinpacar.app.data.repository.StatusRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PartnerStatusService : Service() {

    @Inject lateinit var firestore: FirebaseFirestore
    @Inject lateinit var statusRepository: StatusRepository

    private var listenerRegistration: ListenerRegistration? = null
    private var lastKnownTimestamp: Long = 0L

    companion object {
        const val CHANNEL_PARTNER = "partner_status_channel"
        const val CHANNEL_FOREGROUND = "foreground_channel"
        const val NOTIF_ID_FOREGROUND = 1001
        const val NOTIF_ID_PARTNER = 1002

        fun start(context: Context) {
            val intent = Intent(context, PartnerStatusService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, PartnerStatusService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIF_ID_FOREGROUND, buildForegroundNotification())
        startListening()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        listenerRegistration?.remove()
        super.onDestroy()
    }

    private fun startListening() {
        val pairId = statusRepository.pairId
        val partnerId = statusRepository.partnerId
        if (pairId.isEmpty() || partnerId.isEmpty()) return

        listenerRegistration?.remove()
        listenerRegistration = firestore
            .collection("pairs")
            .document(pairId)
            .collection("status")
            .document(partnerId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener
                val timestamp = snapshot.getLong("timestamp") ?: 0L
                if (timestamp > lastKnownTimestamp && lastKnownTimestamp != 0L) {
                    val activity = snapshot.getString("activity") ?: ""
                    val note = snapshot.getString("note") ?: ""
                    val location = snapshot.getString("locationName") ?: ""
                    showPartnerNotification(activity, note, location)
                }
                lastKnownTimestamp = timestamp
            }
    }

    private fun showPartnerNotification(activity: String, note: String, location: String) {
        val activityLabel = mapActivityToLabel(activity)
        val contentText = buildString {
            append(activityLabel)
            if (location.isNotEmpty()) append(" • $location")
            if (note.isNotEmpty()) append("\n$note")
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_PARTNER)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pasangan update status 💬")
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID_PARTNER, notification)
    }

    private fun buildForegroundNotification() = NotificationCompat.Builder(this, CHANNEL_FOREGROUND)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Kabarin Pacar aktif")
        .setContentText("Menunggu update dari pasangan...")
        .setPriority(NotificationCompat.PRIORITY_MIN)
        .setSilent(true)
        .build()

    private fun createNotificationChannels() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_PARTNER, "Status Pasangan", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Notifikasi saat pasangan update status"
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_FOREGROUND, "Layanan Aktif", NotificationManager.IMPORTANCE_MIN).apply {
                description = "Layanan background untuk memantau status pasangan"
            }
        )
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
