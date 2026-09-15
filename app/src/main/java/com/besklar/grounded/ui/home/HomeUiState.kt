package com.besklar.grounded.ui.home

import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.model.EarthquakeSnapshot
import java.time.Duration

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

data class HomeUiState(
    val snapshot: EarthquakeSnapshot? = null,
    val mode: HomeMode = HomeMode.LIST,
    val refreshStatus: RefreshStatus = RefreshStatus.Idle,
    val initialAttemptFinished: Boolean = false,
    val selectedEventId: String? = null,
    val newEventIds: Set<String> = emptySet(),
    val locationContext: LocationContext = LocationContext.NotRequested,
    val dataAge: Duration? = null,
) {
    val isInitialLoading: Boolean
        get() = snapshot == null && !initialAttemptFinished

    val isFullScreenFailure: Boolean
        get() = snapshot == null && initialAttemptFinished && refreshStatus is RefreshStatus.Failed
}

sealed interface HomeEffect {
    data object NewEarthquakes : HomeEffect
}
