package com.drelabs.datacheck.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

data class AppUsageRow(val pkg: String, val total: Long, val fgTotal: Long)
data class TotalsRow(val total: Long, val fgTotal: Long)

@Dao
interface UsageLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTick(tick: TickEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsages(usages: List<UsageEntity>)

    @Transaction
    suspend fun insertTickWithUsages(tick: TickEntity, usages: List<UsageEntity>) {
        val id = insertTick(tick.copy(id = 0))
        insertUsages(usages.map { it.copy(tickId = id) })
    }

    @Query("SELECT MAX(endMs) FROM ticks")
    suspend fun latestEndMs(): Long?

    @Query(
        "SELECT pkg, SUM(rx + tx) AS total, SUM(fgRx + fgTx) AS fgTotal " +
            "FROM usage WHERE tickStart >= :sinceMs GROUP BY pkg " +
            "ORDER BY total DESC LIMIT :limit",
    )
    suspend fun topAppsSince(sinceMs: Long, limit: Int): List<AppUsageRow>

    @Query(
        "SELECT SUM(rx + tx) AS total, SUM(fgRx + fgTx) AS fgTotal FROM usage WHERE tickStart >= :sinceMs",
    )
    suspend fun totalsSince(sinceMs: Long): TotalsRow?

    @Query("DELETE FROM ticks WHERE endMs < :cutoffMs")
    suspend fun deleteTicksBefore(cutoffMs: Long)

    @Query("DELETE FROM usage WHERE tickStart < :cutoffMs")
    suspend fun deleteUsageBefore(cutoffMs: Long)

    @Transaction
    suspend fun deleteOlderThan(cutoffMs: Long) {
        deleteTicksBefore(cutoffMs)
        deleteUsageBefore(cutoffMs)
    }
}
