package com.drelabs.datacheck.data

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.os.Process
import com.drelabs.datacheck.data.Attribution.Interval
import com.drelabs.datacheck.data.db.TickEntity
import com.drelabs.datacheck.data.db.UsageEntity
import com.drelabs.datacheck.data.db.UsageLogDb

class SamplingEngine(private val context: Context) {

    data class TickResult(val tick: TickEntity, val rows: List<UsageEntity>)

    private val prefs = Prefs(context)
    private val dao = UsageLogDb.get(context).usageLogDao()

    suspend fun runTick(nowMs: Long = System.currentTimeMillis()): TickResult? {
        val start = resolveWindowStart(nowMs) ?: return null
        val perUid = queryMobilePerUid(start, nowMs)
        if (perUid.isEmpty()) return null
        val device = queryDeviceTotal(start, nowMs)
        val fgByPkg = foregroundFractions(start, nowMs)
        val rows = perUid.map { (uid, bytes) ->
            val pkg = labelForUid(uid)
            val frac = fgByPkg[pkg] ?: 0.0
            val (fgRx, _) = Attribution.split(bytes.first, frac)
            val (fgTx, _) = Attribution.split(bytes.second, frac)
            UsageEntity(
                tickId = 0,
                tickStart = start,
                uid = uid,
                pkg = pkg,
                rx = bytes.first,
                tx = bytes.second,
                fgRx = fgRx,
                fgTx = fgTx,
            )
        }
        val result = saveTick(
            start,
            nowMs,
            device?.first ?: perUid.values.sumOf { it.first },
            device?.second ?: perUid.values.sumOf { it.second },
            rows,
        )
        prefs.rebootPending = false
        return result
    }

    private suspend fun saveTick(
        start: Long,
        end: Long,
        deviceRx: Long,
        deviceTx: Long,
        rows: List<UsageEntity>,
    ): TickResult {
        val tick = TickEntity(startMs = start, endMs = end, deviceRx = deviceRx, deviceTx = deviceTx)
        dao.insertTickWithUsages(tick, rows)
        dao.deleteOlderThan(end - RETENTION_MS)
        prefs.lastTickEndMs = end
        return TickResult(tick, rows)
    }

    private fun resolveWindowStart(nowMs: Long): Long? {
        val last = prefs.lastTickEndMs
        return when {
            last == 0L -> {
                prefs.lastTickEndMs = nowMs
                null
            }
            nowMs - last > MAX_WINDOW_MS -> {
                prefs.lastTickEndMs = nowMs - MAX_WINDOW_MS
                nowMs - MAX_WINDOW_MS
            }
            nowMs <= last -> null
            else -> last
        }
    }

    @Suppress("DEPRECATION")
    private fun queryMobilePerUid(startMs: Long, endMs: Long): Map<Int, Pair<Long, Long>> {
        val nsm = context.getSystemService(NetworkStatsManager::class.java) ?: return emptyMap()
        val out = HashMap<Int, LongArray>()
        try {
            val stats: NetworkStats =
                nsm.querySummary(ConnectivityManager.TYPE_MOBILE, null, startMs, endMs)
            val bucket = NetworkStats.Bucket()
            while (stats.hasNextBucket()) {
                stats.getNextBucket(bucket)
                val acc = out.getOrPut(bucket.uid) { longArrayOf(0L, 0L) }
                acc[0] += bucket.rxBytes.coerceAtLeast(0L)
                acc[1] += bucket.txBytes.coerceAtLeast(0L)
            }
            stats.close()
        } catch (_: SecurityException) {
            return emptyMap()
        } catch (_: Exception) {
            return emptyMap()
        }
        return out.mapValues { (_, v) -> v[0] to v[1] }.filterValues { it.first > 0 || it.second > 0 }
    }

    private fun queryDeviceTotal(startMs: Long, endMs: Long): Pair<Long, Long>? {
        val nsm = context.getSystemService(NetworkStatsManager::class.java) ?: return null
        return try {
            val b = nsm.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startMs, endMs)
            b.rxBytes.coerceAtLeast(0L) to b.txBytes.coerceAtLeast(0L)
        } catch (_: Exception) {
            null
        }
    }

    @Suppress("DEPRECATION")
    private fun foregroundFractions(startMs: Long, endMs: Long): Map<String, Double> {
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return emptyMap()
        val open = HashMap<String, MutableList<Long>>()
        val closed = HashMap<String, MutableList<Interval>>()
        try {
            val events = usm.queryEvents(startMs, endMs)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.MOVE_TO_FOREGROUND ->
                        open.getOrPut(event.packageName) { mutableListOf() }.add(event.timeStamp)

                    UsageEvents.Event.MOVE_TO_BACKGROUND ->
                        open[event.packageName]?.removeFirstOrNull()?.let { ts ->
                            closed.getOrPut(event.packageName) { mutableListOf() }
                                .add(Interval(ts, event.timeStamp))
                        }
                }
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        open.forEach { (pkg, starts) ->
            starts.forEach { ts ->
                closed.getOrPut(pkg) { mutableListOf() }.add(Interval(ts, endMs))
            }
        }
        return closed.mapValues { (_, intervals) ->
            Attribution.foregroundFraction(Attribution.merge(intervals), startMs, endMs)
        }.filterValues { it > 0.0 }
    }

    private fun labelForUid(uid: Int): String = when (uid) {
        Process.SYSTEM_UID, in 0 until 2000 -> context.packageManager.getNameForUid(uid) ?: "system"
        else -> context.packageManager.getNameForUid(uid)?.substringAfterLast(':') ?: "uid:$uid"
    }

    companion object {
        const val MAX_WINDOW_MS: Long = 6 * 60 * 60 * 1000L
        const val RETENTION_MS: Long = 30L * 24 * 60 * 60 * 1000L
    }
}
