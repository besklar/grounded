package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewportTest {
    @Test
    fun `contains coordinates inside ordinary bounds`() {
        val viewport = MapViewport(south = 30.0, west = -110.0, north = 45.0, east = -90.0)

        assertTrue(viewport.contains(Coordinates(39.7, -104.9)))
        assertFalse(viewport.contains(Coordinates(46.0, -104.9)))
        assertFalse(viewport.contains(Coordinates(39.7, -80.0)))
    }

    @Test
    fun `contains coordinates on either side of antimeridian`() {
        val viewport = MapViewport(south = -20.0, west = 170.0, north = 20.0, east = -170.0)

        assertTrue(viewport.contains(Coordinates(0.0, 175.0)))
        assertTrue(viewport.contains(Coordinates(0.0, -175.0)))
        assertFalse(viewport.contains(Coordinates(0.0, 0.0)))
    }
}
