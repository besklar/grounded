package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.Instant

class SituationSummaryCalculatorTest {
    private val calculator = SituationSummaryCalculator()
    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `summarizes global activity without location`() {
        assertEquals(SituationSummary.GlobalActivity(1), calculator.calculate(snapshot(), refreshFailed = false))
    }

    @Test
    fun `failed refresh describes existing data as saved`() {
        assertEquals(SituationSummary.SavedActivity(1), calculator.calculate(snapshot(), refreshFailed = true))
    }

    @Test
    fun `empty result is intentional no activity state`() {
        val empty = EarthquakeSnapshot(emptyList(), "past_24_hours", now, now, null, "USGS")
        assertSame(SituationSummary.NoActivity, calculator.calculate(empty, refreshFailed = false))
    }

    private fun snapshot() = EarthquakeSnapshot(
        listOf(Earthquake("id", 2.0, null, null, now, now, null, null, null, null, null, null, null, null)),
        "past_24_hours",
        now,
        now,
        null,
        "USGS",
    )
}
