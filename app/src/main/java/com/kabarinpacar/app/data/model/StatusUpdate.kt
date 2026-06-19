package com.kabarinpacar.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ActivityType(val label: String, val emoji: String) {
    KERJA("Kerja", "💼"),
    DI_JALAN("Di Jalan", "🚗"),
    MAKAN("Makan", "🍽️"),
    ISTIRAHAT("Istirahat", "😴"),
    LAINNYA("Lainnya", "✨")
}

@Entity(tableName = "status_updates")
data class StatusUpdate(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: String,
    val activity: String,
    val note: String = "",
    val locationName: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
