package com.kabarinpacar.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.kabarinpacar.app.data.model.StatusUpdate

@Database(
    entities = [StatusUpdate::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun statusDao(): StatusDao
}
