package com.besklar.grounded.data.local

import androidx.room.withTransaction
import com.besklar.grounded.data.repository.EarthquakeStore
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject

internal class RoomEarthquakeStore
@Inject
constructor(
    private val database: GroundedDatabase,
    private val dao: EarthquakeDao,
) : EarthquakeStore {
    override fun observeSnapshot(): Flow<EarthquakeSnapshot?> = database.invalidationTracker
        .createFlow(EARTHQUAKES_TABLE, METADATA_TABLE)
        .map {
            database.withTransaction {
                dao.getMetadata()?.toModel(dao.getEarthquakes())
            }
        }.distinctUntilChanged()

    override suspend fun currentEarthquakes(): List<Earthquake> = dao.getEarthquakes().map(EarthquakeEntity::toModel)

    override suspend fun replace(snapshot: EarthquakeSnapshot) {
        database.withTransaction {
            dao.deleteEarthquakes()
            dao.insertEarthquakes(snapshot.earthquakes.map(Earthquake::toEntity))
            dao.upsertMetadata(snapshot.toMetadataEntity())
        }
    }

    private companion object {
        const val EARTHQUAKES_TABLE = "earthquakes"
        const val METADATA_TABLE = "snapshot_metadata"
    }
}

private fun SnapshotMetadataEntity.toModel(events: List<EarthquakeEntity>) = EarthquakeSnapshot(
    earthquakes = events.map(EarthquakeEntity::toModel),
    queryWindow = queryWindow,
    lastSuccessfulRetrieval = Instant.ofEpochMilli(lastSuccessfulRetrievalMillis),
    responseGeneratedAt = responseGeneratedAtMillis?.let(Instant::ofEpochMilli),
    sourceUrl = sourceUrl,
    attribution = attribution,
)

private fun EarthquakeEntity.toModel() = Earthquake(
    id = id,
    magnitude = magnitude,
    magnitudeType = magnitudeType,
    place = place,
    occurredAt = Instant.ofEpochMilli(occurredAtMillis),
    updatedAt = Instant.ofEpochMilli(updatedAtMillis),
    coordinates =
    if (latitude != null && longitude != null) {
        Coordinates(latitude = latitude, longitude = longitude)
    } else {
        null
    },
    depthKilometers = depthKilometers,
    detailUrl = detailUrl,
    feltReports = feltReports,
    significance = significance,
    alert = alert,
    tsunami = tsunami,
    reviewStatus = reviewStatus,
)

private fun Earthquake.toEntity() = EarthquakeEntity(
    id = id,
    magnitude = magnitude,
    magnitudeType = magnitudeType,
    place = place,
    occurredAtMillis = occurredAt.toEpochMilli(),
    updatedAtMillis = updatedAt.toEpochMilli(),
    latitude = coordinates?.latitude,
    longitude = coordinates?.longitude,
    depthKilometers = depthKilometers,
    detailUrl = detailUrl,
    feltReports = feltReports,
    significance = significance,
    alert = alert,
    tsunami = tsunami,
    reviewStatus = reviewStatus,
)

private fun EarthquakeSnapshot.toMetadataEntity() = SnapshotMetadataEntity(
    queryWindow = queryWindow,
    lastSuccessfulRetrievalMillis = lastSuccessfulRetrieval.toEpochMilli(),
    responseGeneratedAtMillis = responseGeneratedAt?.toEpochMilli(),
    sourceUrl = sourceUrl,
    attribution = attribution,
)
