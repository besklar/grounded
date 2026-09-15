package com.besklar.grounded.ui.home

import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.SearchScope
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import java.time.Duration
import java.time.Instant

enum class HomeMode {
    MAP,
    LIST,
}

sealed interface RefreshStatus {
    data object Idle : RefreshStatus

    data object Refreshing : RefreshStatus

    data class Success(val newCount: Int) : RefreshStatus

    data object Failed : RefreshStatus
}

enum class SearchFailure {
    NOT_FOUND,
    PROVIDER_UNAVAILABLE,
    FAILED,
}

sealed interface SearchStatus {
    data object Idle : SearchStatus

    data object Searching : SearchStatus

    data object Resolved : SearchStatus

    data class Error(val failure: SearchFailure) : SearchStatus
}

data class HomeUiState(
    val snapshot: EarthquakeSnapshot? = null,
    val mode: HomeMode = HomeMode.MAP,
    val refreshStatus: RefreshStatus = RefreshStatus.Idle,
    val initialAttemptFinished: Boolean = false,
    val selectedEventId: String? = null,
    val newEventIds: Set<String> = emptySet(),
    val locationContext: LocationContext = LocationContext.NotRequested,
    val dataAge: Duration? = null,
    val filters: HomeFilters = HomeFilters.DEFAULT,
    val searchQuery: String = "",
    val searchScope: SearchScope? = null,
    val searchStatus: SearchStatus = SearchStatus.Idle,
    val mapEarthquakes: List<Earthquake> = snapshot?.earthquakes.orEmpty(),
    val resultEarthquakes: List<Earthquake> = snapshot?.earthquakes.orEmpty(),
    val mapViewport: MapViewport? = null,
    val asOf: Instant = snapshot?.lastSuccessfulRetrieval ?: Instant.EPOCH,
    val summary: SituationSummary =
        SituationSummaryCalculator().calculate(
            snapshot?.copy(earthquakes = resultEarthquakes),
            refreshStatus is RefreshStatus.Failed,
            locationContext,
        ),
) {
    val isInitialLoading: Boolean
        get() = snapshot == null && !initialAttemptFinished

    val isFullScreenFailure: Boolean
        get() = snapshot == null && initialAttemptFinished && refreshStatus is RefreshStatus.Failed

    val filtersExcludedAll: Boolean
        get() = snapshot?.earthquakes?.isNotEmpty() == true && resultEarthquakes.isEmpty()
}

sealed interface HomeEffect {
    data object NewEarthquakes : HomeEffect
}
