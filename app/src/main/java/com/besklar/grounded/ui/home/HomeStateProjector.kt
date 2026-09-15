package com.besklar.grounded.ui.home

import com.besklar.grounded.location.LocationContext
import java.time.Duration
import java.time.Instant

internal object HomeStateProjector {
    private val summaryCalculator = SituationSummaryCalculator()

    fun project(
        state: HomeUiState,
        now: Instant,
    ): HomeUiState {
        val snapshot = state.snapshot
        val userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates
        val mapEarthquakes =
            EarthquakeFilter.apply(
                earthquakes = snapshot?.earthquakes.orEmpty(),
                filters = state.filters,
                now = now,
                userCoordinates = userCoordinates,
                searchScope = null,
            )
        val resultEarthquakes =
            state.mapViewport?.let { viewport ->
                mapEarthquakes.filter { earthquake -> earthquake.coordinates?.let(viewport::contains) == true }
            } ?: EarthquakeFilter.apply(
                earthquakes = snapshot?.earthquakes.orEmpty(),
                filters = state.filters,
                now = now,
                userCoordinates = userCoordinates,
                searchScope = state.searchScope,
            )
        val resultSnapshot = snapshot?.copy(earthquakes = resultEarthquakes)
        val resultIds = resultEarthquakes.asSequence().map { it.id }.toSet()

        return state.copy(
            mapEarthquakes = mapEarthquakes,
            resultEarthquakes = resultEarthquakes,
            asOf = now,
            dataAge = snapshot?.let { Duration.between(it.lastSuccessfulRetrieval, now).coerceAtLeast(Duration.ZERO) },
            summary =
            summaryCalculator.calculate(
                snapshot = resultSnapshot,
                refreshFailed = state.refreshStatus is RefreshStatus.Failed,
                locationContext = state.locationContext,
            ),
            selectedEventId = state.selectedEventId?.takeIf(resultIds::contains),
        )
    }
}
