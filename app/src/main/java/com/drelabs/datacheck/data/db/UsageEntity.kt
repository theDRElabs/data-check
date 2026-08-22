package com.drelabs.datacheck.data.db

import androidx.room.Entity

@Entity(tableName = "usage", primaryKeys = ["tickId", "uid"])
data class UsageEntity(
    val tickId: Long,
    val tickStart: Long,
    val uid: Int,
    val pkg: String,
    val rx: Long,
    val tx: Long,
    val fgRx: Long,
    val fgTx: Long,
)
