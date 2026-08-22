package com.drelabs.datacheck.data.db

data class ExportRow(
    val tickStart: Long,
    val tickEnd: Long,
    val uid: Int,
    val pkg: String,
    val rx: Long,
    val tx: Long,
    val fgRx: Long,
    val fgTx: Long,
)
