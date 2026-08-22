package com.drelabs.datacheck

import com.drelabs.datacheck.data.Attribution
import org.junit.Assert.assertEquals
import org.junit.Test

class AttributionTest {

    @Test
    fun `merges overlapping intervals`() {
        val merged = Attribution.merge(
            listOf(
                Attribution.Interval(0, 100),
                Attribution.Interval(50, 150),
            ),
        )
        assertEquals(listOf(Attribution.Interval(0, 150)), merged)
    }

    @Test
    fun `keeps disjoint intervals sorted`() {
        val merged = Attribution.merge(
            listOf(
                Attribution.Interval(200, 300),
                Attribution.Interval(0, 100),
            ),
        )
        assertEquals(
            listOf(
                Attribution.Interval(0, 100),
                Attribution.Interval(200, 300),
            ),
            merged,
        )
    }

    @Test
    fun `clamps fraction to one`() {
        val frac = Attribution.foregroundFraction(
            listOf(Attribution.Interval(0, 10_000)),
            windowStartMs = 5_000,
            windowEndMs = 8_000,
        )
        assertEquals(1.0, frac, 1e-9)
    }

    @Test
    fun `computes partial overlap fraction`() {
        val frac = Attribution.foregroundFraction(
            listOf(Attribution.Interval(0, 2_000)),
            windowStartMs = 0,
            windowEndMs = 8_000,
        )
        assertEquals(0.25, frac, 1e-9)
    }

    @Test
    fun `split never exceeds total and preserves bytes`() {
        val (fg, bg) = Attribution.split(totalBytes = 1000, fraction = 0.3)
        assertEquals(300L, fg)
        assertEquals(700L, bg)
    }

    @Test
    fun `split with zero total yields zeros`() {
        val (fg, bg) = Attribution.split(totalBytes = 0, fraction = 1.0)
        assertEquals(0L to 0L, fg to bg)
    }
}
