package com.besklar.grounded.ui.home

import com.besklar.grounded.location.SearchScope
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

    @Test
    fun `search scope includes only events within five hundred miles`() {
        val result =
            EarthquakeFilter.apply(
                earthquakes =
                listOf(
                    earthquake("inside", 3.0, 1, Coordinates(7.0, 0.0)),
                    earthquake("outside", 3.0, 1, Coordinates(8.0, 0.0)),
                    earthquake("missing", 3.0, 1),
                ),
                filters = HomeFilters(),
                now = now,
                userCoordinates = null,
                searchScope = SearchScope("Origin", Coordinates(0.0, 0.0)),
            )

        assertEquals(listOf("inside"), result.map(Earthquake::id))
    }

    @Test
    fun `search distance can narrow the geographic scope`() {
        val result =
            EarthquakeFilter.apply(
                earthquakes =
                listOf(
                    earthquake("inside-100", 3.0, 1, Coordinates(1.0, 0.0)),
                    earthquake("outside-100", 3.0, 1, Coordinates(2.0, 0.0)),
                ),
                filters = HomeFilters(distance = DistanceFilter.ONE_HUNDRED),
                now = now,
                userCoordinates = null,
                searchScope = SearchScope("Origin", Coordinates(0.0, 0.0)),
            )

        assertEquals(listOf("inside-100"), result.map(Earthquake::id))
    }

    @Test
    fun `search scope handles the antimeridian`() {
        val result =
            EarthquakeFilter.apply(
                earthquakes = listOf(earthquake("across-date-line", 3.0, 1, Coordinates(0.0, -179.9))),
                filters = HomeFilters(),
                now = now,
                userCoordinates = null,
                searchScope = SearchScope("Date line", Coordinates(0.0, 179.9)),
            )

        assertEquals(listOf("across-date-line"), result.map(Earthquake::id))
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
