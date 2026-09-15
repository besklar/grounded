package com.besklar.grounded.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class RoomEarthquakeStoreTest {
    private lateinit var database: GroundedDatabase
    private lateinit var store: RoomEarthquakeStore

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), GroundedDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        store = RoomEarthquakeStore(database, database.earthquakeDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun replacementPublishesOnlyTheNewSnapshot() = runTest {
        store.replace(snapshot(listOf(earthquake("old"))))
        store.replace(snapshot(listOf(earthquake("new"))))

        val observed = store.observeSnapshot().first { it != null }!!

        assertEquals(listOf("new"), observed.earthquakes.map(Earthquake::id))
        assertEquals(listOf("new"), store.currentEarthquakes().map(Earthquake::id))
    }

    @Test
    fun observationNeverMixesEventsAndMetadataFromDifferentReplacements() = runTest {
        val oldRetrieval = now.minusSeconds(60)
        val newRetrieval = now
        val observed = mutableListOf<EarthquakeSnapshot>()
        val observation = launch {
            store.observeSnapshot().filterNotNull().collect(observed::add)
        }

        store.replace(snapshot(listOf(earthquake("old")), oldRetrieval))
        withTimeout(5_000) {
            while (observed.none { it.earthquakes.singleOrNull()?.id == "old" }) {
                kotlinx.coroutines.yield()
            }
        }
        store.replace(snapshot(listOf(earthquake("new")), newRetrieval))
        withTimeout(5_000) {
            while (observed.none { it.earthquakes.singleOrNull()?.id == "new" }) {
                kotlinx.coroutines.yield()
            }
        }
        observation.cancelAndJoin()

        assertEquals(
            setOf("old" to oldRetrieval, "new" to newRetrieval),
            observed.map { it.earthquakes.single().id to it.lastSuccessfulRetrieval }.toSet(),
        )
    }

    @Test
    fun successfulEmptyReplacementClearsEventsAndKeepsMetadata() = runTest {
        store.replace(snapshot(listOf(earthquake("old"))))
        store.replace(snapshot(emptyList()))

        val observed = store.observeSnapshot().first { it != null }!!

        assertEquals(emptyList<Earthquake>(), observed.earthquakes)
        assertEquals("past_24_hours", observed.queryWindow)
        assertEquals(now, observed.lastSuccessfulRetrieval)
    }

    private val now = Instant.parse("2026-09-13T20:00:00Z")

    private fun snapshot(
        events: List<Earthquake>,
        retrievedAt: Instant = now,
    ) = EarthquakeSnapshot(events, "past_24_hours", retrievedAt, now, null, "USGS")

    private fun earthquake(id: String) = Earthquake(id, 2.5, "ml", "Test", now, now, null, 5.0, null, null, null, null, null, null)
}
