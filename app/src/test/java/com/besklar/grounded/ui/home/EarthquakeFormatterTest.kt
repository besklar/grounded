package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class EarthquakeFormatterTest {
    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `unknown magnitude is explicit`() {
        assertEquals("—", EarthquakeFormatter.magnitude(earthquake(magnitude = null), Locale.US))
    }

    @Test
    fun `missing place falls back to coordinates`() {
        assertEquals("38.12, -122.46", EarthquakeFormatter.place(earthquake(place = null), Locale.US))
    }

    @Test
    fun `relative time transitions from minutes to hours to date`() {
        assertEquals("5 min ago", relative(now.minusSeconds(300)))
        assertEquals("2 hr ago", relative(now.minusSeconds(7_200)))
        assertTrue(relative(now.minusSeconds(90_000)).contains("2026"))
    }

    private fun relative(occurredAt: Instant) = EarthquakeFormatter.relativeTime(occurredAt, now, Locale.US, ZoneOffset.UTC)

    private fun earthquake(magnitude: Double? = 2.5, place: String? = "Test") = Earthquake(
        "id",
        magnitude,
        "ml",
        place,
        now,
        now,
        Coordinates(38.123, -122.456),
        7.0,
        null,
        null,
        null,
        null,
        null,
        null,
    )
}
