package com.drelabs.datacheck.data

import java.time.LocalDate

/**
 * Pure logic for the bundle card baseline (ISSUE-007).
 *
 * Entering a bundle figure means "this is my current remaining balance,
 * as of now": bundle usage counts only from the entry timestamp forward,
 * never from the renewal-cycle calendar start. The renewal day remains a
 * separate, informational feature (days-to-renewal display only).
 */
object BundleLogic {

    /** Row shape for the pure usage-window computation (DAO-shaped). */
    data class UsageRow(
        val tickStart: Long,
        val total: Long,
    )

    /** UI state for the bundle card. */
    sealed interface CardState {
        /**
         * No bundle entered (bundleBytes <= 0), or the persisted figure
         * was never stamped with an entry time (entryAtMs <= 0) — the
         * card must not be shown.
         */
        data object Hidden : CardState

        /** Bundle entered and stamped; usage counted since entry. */
        data class Values(
            val bundleBytes: Long,
            val usedBytes: Long,
            val leftBytes: Long,
            /** Fraction of the bundle used, coerced to [0, 1]. */
            val pct: Float,
            /** Informational: days from today to the next renewal date. */
            val daysLeft: Long,
        ) : CardState
    }

    /**
     * Sum of usage rows with tickStart >= [entryAtMs]: usage before the
     * entry is not part of the bundle baseline. Executable spec for the
     * DAO call totalsSince(entryAtMs) the dashboard makes in phase 2.
     *
     * STUB (ISSUE-007 phase 1 RED): always returns 0 so the tests fail
     * at runtime; phase 2 implements the real window filter.
     */
    fun usageSinceEntry(rows: List<UsageRow>, entryAtMs: Long): Long {
        return 0L
    }

    /**
     * Builds the card state from the persisted bundle figure, its entry
     * timestamp, and the usage summed since that timestamp:
     * - bundleBytes <= 0 or entryAtMs <= 0 → [CardState.Hidden]
     * - entryAtMs > nowMs (clock skew) → used = 0
     * - leftBytes = max(bundleBytes - usedBytes, 0)
     * - pct = usedBytes / bundleBytes coerced to [0, 1]
     * - daysLeft is derived from today + renewalDay only, never from
     *   the entry timestamp.
     *
     * STUB (ISSUE-007 phase 1 RED): always returns a zeroed Values so
     * the tests fail at runtime; phase 2 implements the real
     * computation with these exact signatures.
     */
    fun buildState(
        bundleBytes: Long,
        entryAtMs: Long,
        usedSinceEntryBytes: Long,
        nowMs: Long,
        today: LocalDate,
        renewalDay: Int,
    ): CardState {
        return CardState.Values(
            bundleBytes = 0L,
            usedBytes = 0L,
            leftBytes = 0L,
            pct = 0f,
            daysLeft = 0L,
        )
    }
}
