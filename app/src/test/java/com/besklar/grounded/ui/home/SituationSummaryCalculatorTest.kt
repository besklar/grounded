package com.besklar.grounded.ui.home

import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.model.Coordinates
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

    @Test
    fun `significant nearby event wins with location`() {
        val event =
            Earthquake(
                "nearby",
                4.8,
                "mw",
                "Nearby",
                now,
                now,
                Coordinates(40.0, -105.0),
                5.0,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        val snapshot = EarthquakeSnapshot(listOf(event), "past_24_hours", now, now, null, "USGS")
        val location = LocationContext.Available(Coordinates(39.7, -105.0), now)

        val result = calculator.calculate(snapshot, refreshFailed = false, locationContext = location)

        assertEquals("nearby", (result as SituationSummary.NearbySignificant).earthquake.id)
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
