package com.besklar.grounded.data.repository

import com.besklar.grounded.data.remote.EarthquakeRemoteSource
import com.besklar.grounded.data.remote.NormalizedFeed
import com.besklar.grounded.data.remote.RemoteFeedResult
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class OfflineFirstEarthquakeRepositoryTest {
    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `successful refresh atomically replaces snapshot and detects new ids`() = runTest {
        val old = earthquake("old", updated = now.minusSeconds(60))
        val revised = earthquake("revised", updated = now.minusSeconds(60))
        val store = FakeStore(snapshot(listOf(old, revised)))
        val remote =
            FakeRemote(
                RemoteFeedResult.Success(
                    NormalizedFeed(
                        earthquakes = listOf(earthquake("new"), earthquake("revised", updated = now)),
                        generatedAt = now,
                        sourceUrl = null,
                        attribution = "USGS",
                        discardedCount = 2,
                    ),
                ),
            )
        val repository = repository(remote, store)

        val result = repository.refresh() as RefreshResult.Success

        assertEquals(setOf("new"), result.newEventIds)
        assertEquals(1, result.revisedCount)
        assertEquals(listOf("new", "revised"), store.value.value!!.earthquakes.map { it.id })
        assertEquals(now, store.value.value!!.lastSuccessfulRetrieval)
    }

    @Test
    fun `failure leaves cached snapshot untouched`() = runTest {
        val cached = snapshot(listOf(earthquake("cached")))
        val store = FakeStore(cached)
        val repository = repository(FakeRemote(RemoteFeedResult.TransportFailure(IOException())), store)

        val result = repository.refresh()

        assertSame(RefreshResult.TransportFailure, result)
        assertSame(cached, store.value.value)
    }

    @Test
    fun `successful empty response intentionally clears events`() = runTest {
        val store = FakeStore(snapshot(listOf(earthquake("cached"))))
        val remote = FakeRemote(RemoteFeedResult.Success(NormalizedFeed(emptyList(), now, null, "USGS", 0)))

        repository(remote, store).refresh()

        assertEquals(emptyList<Earthquake>(), store.value.value!!.earthquakes)
    }

    private fun repository(remote: EarthquakeRemoteSource, store: EarthquakeStore) = OfflineFirstEarthquakeRepository(remote, store, Clock.fixed(now, ZoneOffset.UTC))

    private fun snapshot(events: List<Earthquake>) = EarthquakeSnapshot(events, "past_24_hours", now.minusSeconds(300), now, null, "USGS")

    private fun earthquake(id: String, updated: Instant = now) = Earthquake(id, 2.5, "ml", "Test", now, updated, null, 5.0, null, null, null, null, null, null)

    private class FakeRemote(private val result: RemoteFeedResult) : EarthquakeRemoteSource {
        override suspend fun fetch() = result
    }

    private class FakeStore(initial: EarthquakeSnapshot?) : EarthquakeStore {
        val value = MutableStateFlow(initial)

        override fun observeSnapshot(): Flow<EarthquakeSnapshot?> = value

        override suspend fun currentEarthquakes() = value.value?.earthquakes.orEmpty()

        override suspend fun replace(snapshot: EarthquakeSnapshot) {
            value.value = snapshot
        }
    }
}
