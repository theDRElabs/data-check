package com.drelabs.datacheck.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ticks")
data class TickEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMs: Long,
    val endMs: Long,
    val deviceRx: Long,
    val deviceTx: Long,
)
