package com.besklar.grounded.data.repository

import com.besklar.grounded.data.remote.EarthquakeRemoteSource
import com.besklar.grounded.data.remote.RemoteFeedResult
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Clock
import javax.inject.Inject

internal class OfflineFirstEarthquakeRepository
@Inject
constructor(
    private val remote: EarthquakeRemoteSource,
    private val store: EarthquakeStore,
    private val clock: Clock,
) : EarthquakeRepository {
    private val refreshMutex = Mutex()

    override fun observeSnapshot(): Flow<EarthquakeSnapshot?> = store.observeSnapshot()

    override suspend fun refresh(): RefreshResult = refreshMutex.withLock {
        when (val remoteResult = remote.fetch()) {
            is RemoteFeedResult.Success -> {
                val previousById = store.currentEarthquakes().associateBy { it.id }
                val current = remoteResult.feed.earthquakes
                val newIds = current.asSequence().map { it.id }.filterNot(previousById::containsKey).toSet()
                val revisedCount =
                    current.count { event ->
                        previousById[event.id]?.updatedAt?.let(event.updatedAt::isAfter) == true
                    }
                store.replace(
                    EarthquakeSnapshot(
                        earthquakes = current,
                        queryWindow = "past_24_hours",
                        lastSuccessfulRetrieval = clock.instant(),
                        responseGeneratedAt = remoteResult.feed.generatedAt,
                        sourceUrl = remoteResult.feed.sourceUrl,
                        attribution = remoteResult.feed.attribution,
                    ),
                )
                RefreshResult.Success(
                    newEventIds = newIds,
                    revisedCount = revisedCount,
                    discardedCount = remoteResult.feed.discardedCount,
                )
            }

            is RemoteFeedResult.HttpFailure -> RefreshResult.HttpFailure(remoteResult.code)
            is RemoteFeedResult.TransportFailure -> RefreshResult.TransportFailure
            is RemoteFeedResult.DecodingFailure -> RefreshResult.DecodingFailure
        }
    }
}
