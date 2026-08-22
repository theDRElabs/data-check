package com.drelabs.datacheck.data

object Attribution {
    data class Interval(val startMs: Long, val endMs: Long)

    fun merge(raw: List<Interval>): List<Interval> {
        if (raw.isEmpty()) return emptyList()
        val sorted = raw.sortedBy { it.startMs }
        val out = ArrayList<Interval>()
        var cur = sorted[0]
        for (i in 1 until sorted.size) {
            val next = sorted[i]
            cur = if (next.startMs <= cur.endMs) {
                Interval(cur.startMs, maxOf(cur.endMs, next.endMs))
            } else {
                out.add(cur)
                next
            }
        }
        out.add(cur)
        return out
    }

    fun overlapMs(interval: Interval, windowStartMs: Long, windowEndMs: Long): Long {
        val s = maxOf(interval.startMs, windowStartMs)
        val e = minOf(interval.endMs, windowEndMs)
        return (e - s).coerceAtLeast(0L)
    }

    fun foregroundFraction(
        intervals: List<Interval>,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Double {
        val window = (windowEndMs - windowStartMs).coerceAtLeast(1L)
        val fg = intervals.sumOf { overlapMs(it, windowStartMs, windowEndMs) }
        return (fg.toDouble() / window).coerceIn(0.0, 1.0)
    }

    fun split(totalBytes: Long, fraction: Double): Pair<Long, Long> {
        require(totalBytes >= 0)
        val fg = (totalBytes * fraction).toLong().coerceIn(0L, totalBytes)
        return fg to (totalBytes - fg)
    }
}
