package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.google.maps.android.clustering.algo.NonHierarchicalDistanceBasedAlgorithm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun `cluster item exposes a useful map label`() {
        val result =
            MapEarthquake.from(
                earthquake = earthquake(Coordinates(38.0, -122.0)),
                magnitudeDescription = "magnitude",
                magnitudeValue = { "3.2" },
            )!!

        assertEquals("Test", result.title)
        assertEquals("magnitude 3.2", result.snippet)
        assertEquals("Test, magnitude 3.2", result.contentDescription)
    }

    @Test
    fun `nearby events progressively separate as zoom increases`() {
        val items =
            listOf(
                earthquake("one", 39.7392, -104.9903),
                earthquake("two", 39.8392, -104.9903),
                earthquake("three", 39.7392, -104.8903),
                earthquake("four", 39.8392, -104.8903),
            ).mapNotNull(MapEarthquake::from)
        val algorithm = NonHierarchicalDistanceBasedAlgorithm<MapEarthquake>().apply { addItems(items) }

        val zoomedOut = algorithm.getClusters(4f)
        val zoomedIn = algorithm.getClusters(18f)

        assertTrue(zoomedOut.size < zoomedIn.size)
        assertEquals(items.size, zoomedOut.sumOf { it.size })
        assertEquals(items.size, zoomedIn.sumOf { it.size })
    }

    @Test
    fun `events at identical coordinates remain represented in one cluster`() {
        val items =
            listOf(
                earthquake("one", 39.7392, -104.9903),
                earthquake("two", 39.7392, -104.9903),
            ).mapNotNull(MapEarthquake::from)
        val algorithm = NonHierarchicalDistanceBasedAlgorithm<MapEarthquake>().apply { addItems(items) }

        val clusters = algorithm.getClusters(20f)

        assertEquals(1, clusters.size)
        assertEquals(2, clusters.single().size)
    }

    @Test
    fun `large result set preserves every event across zoom levels`() {
        val items =
            (0 until 2_000).map { index ->
                val latitude = -60.0 + (index % 120) * 1.0
                val longitude = -170.0 + (index % 340) * 1.0
                MapEarthquake.from(earthquake("event-$index", latitude, longitude))!!
            }
        val algorithm = NonHierarchicalDistanceBasedAlgorithm<MapEarthquake>().apply { addItems(items) }

        listOf(2f, 6f, 12f, 18f).forEach { zoom ->
            val representedIds = algorithm.getClusters(zoom).flatMap { cluster -> cluster.items }.map { it.id }
            assertEquals(items.size, representedIds.size)
            assertEquals(items.map { it.id }.toSet(), representedIds.toSet())
        }
    }

    private fun earthquake(coordinates: Coordinates?) = earthquake("event", coordinates?.latitude, coordinates?.longitude)

    private fun earthquake(
        id: String,
        latitude: Double?,
        longitude: Double?,
    ) = Earthquake(
        id,
        3.2,
        "ml",
        "Test",
        now,
        now,
        if (latitude != null && longitude != null) Coordinates(latitude, longitude) else null,
        3.0,
        null,
        null,
        null,
        null,
        null,
        null,
    )
}
