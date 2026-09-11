package com.drelabs.datacheck.data

/**
 * Persistence surface required by [TickBookkeeper]. The production
 * implementation adapts the Room UsageLogDao; JVM tests provide a fake.
 */
interface TickStore {
    /** End time of the latest committed tick, or null when no tick exists. */
    suspend fun latestEndMs(): Long?

    /** Record the starting point when no tick exists yet (fresh install). */
    suspend fun stampStart(atMs: Long)

    /** Atomically persist one sampled window and its per-app rows. */
    suspend fun insertTick(
        startMs: Long,
        endMs: Long,
        deviceRx: Long,
        deviceTx: Long,
        usages: List<TickBookkeeper.WindowUsage>,
    )
}

/**
 * Cache-only view of the last tick end (SharedPreferences in production).
 * Never authoritative: the [TickStore] behind it wins on any disagreement.
 */
interface WindowCache {
    fun lastTickEndMs(): Long?
    fun storeLastTickEndMs(endMs: Long)
}

class TickBookkeeper(private val store: TickStore, private val cache: WindowCache) {

    data class WindowUsage(
        val uid: Int,
        val pkg: String,
        val rx: Long,
        val tx: Long,
        val fgRx: Long,
        val fgTx: Long,
    )

    data class TickDraft(
        val deviceRx: Long,
        val deviceTx: Long,
        val usages: List<WindowUsage>,
    )

    data class CommittedWindow(
        val startMs: Long,
        val endMs: Long,
        val deviceRx: Long,
        val deviceTx: Long,
        val usages: List<WindowUsage>,
    )

    /**
     * PHASE-1 STUB (ISSUE-006 RED): intentionally reproduces the buggy
     * read-then-write bookkeeping of SamplingEngine.resolveWindowStart —
     * cache-backed, no mutex, DB never consulted — so the phase-1 tests
     * fail at runtime. Phase 2 replaces the body: DB-backed resolution
     * under an app-scoped (companion-level) mutex, with resolve, sample
     * and insert as one critical section.
     */
    suspend fun runWindow(
        nowMs: Long,
        sample: suspend (startMs: Long, endMs: Long) -> TickDraft?,
    ): CommittedWindow? {
        val last = cache.lastTickEndMs() ?: 0L
        val start: Long = when {
            last == 0L -> {
                cache.storeLastTickEndMs(nowMs)
                return null
            }
            nowMs - last > MAX_WINDOW_MS -> nowMs - MAX_WINDOW_MS
            nowMs <= last -> return null
            else -> last
        }
        val draft = sample(start, nowMs) ?: return null
        store.insertTick(start, nowMs, draft.deviceRx, draft.deviceTx, draft.usages)
        cache.storeLastTickEndMs(nowMs)
        return CommittedWindow(start, nowMs, draft.deviceRx, draft.deviceTx, draft.usages)
    }

    companion object {
        const val MAX_WINDOW_MS: Long = 6 * 60 * 60 * 1000L
    }
}
