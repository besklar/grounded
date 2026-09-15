package com.besklar.grounded.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UsgsFeedMapperTest {
    private val mapper = UsgsFeedMapper()

    @Test
    fun `maps complete and partial events without invented defaults`() {
        val feed =
            UsgsFeatureCollectionDto(
                metadata = UsgsMetadataDto(generated = 2_000, title = "USGS feed"),
                features =
                listOf(
                    event(id = "complete", time = 1_000, magnitude = -0.4),
                    event(id = "partial", time = 2_000, magnitude = null, coordinates = null),
                ),
            )

        val result = mapper.map(feed)

        assertEquals(listOf("partial", "complete"), result.earthquakes.map { it.id })
        assertNull(result.earthquakes.first().magnitude)
        assertNull(result.earthquakes.first().coordinates)
        assertEquals(-0.4, result.earthquakes.last().magnitude!!, 0.0)
        assertEquals("USGS feed", result.attribution)
    }

    @Test
    fun `skips malformed record while retaining valid records`() {
        val result =
            mapper.map(
                UsgsFeatureCollectionDto(
                    features = listOf(event(id = null, time = 1_000), event(id = "valid", time = 2_000)),
                ),
            )

        assertEquals(listOf("valid"), result.earthquakes.map { it.id })
        assertEquals(1, result.discardedCount)
    }

    @Test
    fun `rejects non-empty response with no usable records`() {
        assertThrows(InvalidFeedException::class.java) {
            mapper.map(UsgsFeatureCollectionDto(features = listOf(event(id = null, time = null))))
        }
    }

    @Test
    fun `keeps newest revision for duplicate id`() {
        val result =
            mapper.map(
                UsgsFeatureCollectionDto(
                    features =
                    listOf(
                        event(id = "same", time = 2_000, updated = 3_000, magnitude = 2.0),
                        event(id = "same", time = 2_000, updated = 4_000, magnitude = 3.0),
                    ),
                ),
            )

        assertEquals(1, result.earthquakes.size)
        assertEquals(3.0, result.earthquakes.single().magnitude!!, 0.0)
        assertEquals(1, result.discardedCount)
    }

    @Test
    fun `keeps invalid coordinates out of map model`() {
        val earthquake =
            mapper
                .map(
                    UsgsFeatureCollectionDto(
                        features = listOf(event(id = "bad-location", time = 1_000, coordinates = listOf(30.0, 95.0, 4.0))),
                    ),
                ).earthquakes
                .single()

        assertNull(earthquake.coordinates)
        assertEquals(4.0, earthquake.depthKilometers!!, 0.0)
        assertTrue(earthquake.tsunami == false)
    }

    private fun event(
        id: String?,
        time: Long?,
        updated: Long = time ?: 0,
        magnitude: Double? = 2.5,
        coordinates: List<Double?>? = listOf(-122.0, 38.0, 7.5),
    ) = UsgsFeatureDto(
        id = id,
        properties =
        UsgsPropertiesDto(
            mag = magnitude,
            place = "10 km from Test",
            time = time,
            updated = updated,
            url = "https://earthquake.usgs.gov/earthquakes/eventpage/$id",
            felt = 4,
            sig = 100,
            tsunami = 0,
            status = "reviewed",
            magnitudeType = "ml",
        ),
        geometry = UsgsGeometryDto(coordinates = coordinates),
    )
}
