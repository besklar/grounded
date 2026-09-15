package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class HomeFiltersTest {
    private val now = Instant.parse("2026-09-14T05:00:00Z")

    @Test
    fun `time magnitude and strongest rules combine over one dataset`() {
        val events =
            listOf(
                earthquake("recent-small", 2.0, minutesAgo = 10),
                earthquake("recent-strong", 5.1, minutesAgo = 20),
                earthquake("older-strongest", 6.0, minutesAgo = 90),
            )

        val result =
            EarthquakeFilter.apply(
                events,
                HomeFilters(TimeRange.PAST_HOUR, MagnitudeFilter.FOUR_POINT_FIVE, EarthquakeOrder.STRONGEST),
                now,
                null,
            )

        assertEquals(listOf("recent-strong"), result.map(Earthquake::id))
    }

    @Test
    fun `strongest puts unknown magnitudes after known values`() {
        val result =
            EarthquakeFilter.apply(
                listOf(earthquake("unknown", null, 5), earthquake("strong", 4.0, 10), earthquake("weak", 1.0, 1)),
                HomeFilters(order = EarthquakeOrder.STRONGEST),
                now,
                null,
            )

        assertEquals(listOf("strong", "weak", "unknown"), result.map(Earthquake::id))
    }

    @Test
    fun `nearest uses valid coordinates and leaves unmappable events last`() {
        val user = Coordinates(40.0, -105.0)
        val result =
            EarthquakeFilter.apply(
                listOf(
                    earthquake("far", 3.0, 1, Coordinates(45.0, -105.0)),
                    earthquake("missing", 3.0, 1),
                    earthquake("near", 3.0, 5, Coordinates(40.1, -105.0)),
                ),
                HomeFilters(order = EarthquakeOrder.NEAREST),
                now,
                user,
            )

        assertEquals(listOf("near", "far", "missing"), result.map(Earthquake::id))
    }

    @Test
    fun `nearest gracefully falls back to recent without location`() {
        val result =
            EarthquakeFilter.apply(
                listOf(earthquake("older", 3.0, 10), earthquake("newer", 3.0, 1)),
                HomeFilters(order = EarthquakeOrder.NEAREST),
                now,
                null,
            )

        assertEquals(listOf("newer", "older"), result.map(Earthquake::id))
    }

    private fun earthquake(
        id: String,
        magnitude: Double?,
        minutesAgo: Long,
        coordinates: Coordinates? = null,
    ) = Earthquake(
        id = id,
        magnitude = magnitude,
        magnitudeType = null,
        place = id,
        occurredAt = now.minusSeconds(minutesAgo * 60),
        updatedAt = now,
        coordinates = coordinates,
        depthKilometers = null,
        detailUrl = null,
        feltReports = null,
        significance = null,
        alert = null,
        tsunami = null,
        reviewStatus = null,
    )
}
