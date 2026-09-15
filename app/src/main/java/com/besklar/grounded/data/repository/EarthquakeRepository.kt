package com.besklar.grounded.data.repository

import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.flow.Flow

interface EarthquakeRepository {
    fun observeSnapshot(): Flow<EarthquakeSnapshot?>

    suspend fun refresh(): RefreshResult
}

interface EarthquakeStore {
    fun observeSnapshot(): Flow<EarthquakeSnapshot?>

    suspend fun currentEarthquakes(): List<Earthquake>

    suspend fun replace(snapshot: EarthquakeSnapshot)
}

sealed interface RefreshResult {
    data class Success(
        val newEventIds: Set<String>,
        val revisedCount: Int,
        val discardedCount: Int,
    ) : RefreshResult

    data class HttpFailure(val code: Int) : RefreshResult

    data object TransportFailure : RefreshResult

    data object DecodingFailure : RefreshResult
}
