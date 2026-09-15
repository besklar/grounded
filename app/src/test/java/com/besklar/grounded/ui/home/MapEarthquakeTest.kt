package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class MapEarthquakeTest {
    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `maps valid domain coordinates without leaking domain object`() {
        val result = MapEarthquake.from(earthquake(Coordinates(38.0, -122.0)))!!
        assertEquals(38.0, result.position.latitude, 0.0)
        assertEquals(-122.0, result.position.longitude, 0.0)
        assertEquals("event", result.id)
    }

    @Test
    fun `event without coordinates is not mappable`() {
        assertNull(MapEarthquake.from(earthquake(null)))
    }

    private fun earthquake(coordinates: Coordinates?) = Earthquake("event", 3.2, "ml", "Test", now, now, coordinates, 3.0, null, null, null, null, null, null)
}
