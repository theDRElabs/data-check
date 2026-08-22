package com.drelabs.datacheck.data.db

import android.content.Context
import androidx.room.Room

object UsageLogDb {
    private const val NAME = "datacheck.db"

    @Volatile
    private var instance: UsageLogDatabase? = null

    fun get(context: Context): UsageLogDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                UsageLogDatabase::class.java,
                NAME,
            ).build().also { instance = it }
        }
}

@androidx.room.Database(entities = [TickEntity::class, UsageEntity::class], version = 1)
abstract class UsageLogDatabase : androidx.room.RoomDatabase() {
    abstract fun usageLogDao(): UsageLogDao
}
