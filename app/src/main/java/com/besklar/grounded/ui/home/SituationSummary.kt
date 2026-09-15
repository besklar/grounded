package com.besklar.grounded.ui.home

import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.location.RelativeLocationCalculator
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import javax.inject.Inject

sealed interface SituationSummary {
    data class GlobalActivity(val eventCount: Int) : SituationSummary

    data object NoActivity : SituationSummary

    data class SavedActivity(val eventCount: Int) : SituationSummary

    data class NearbySignificant(val earthquake: Earthquake, val relative: RelativeLocation) : SituationSummary

    data class NoSignificantNearby(val nearbyCount: Int) : SituationSummary
}

class SituationSummaryCalculator
@Inject
constructor() {
    fun calculate(
        snapshot: EarthquakeSnapshot?,
        refreshFailed: Boolean,
        locationContext: LocationContext = LocationContext.NotRequested,
    ): SituationSummary = when {
        snapshot == null || snapshot.earthquakes.isEmpty() -> SituationSummary.NoActivity
        refreshFailed -> SituationSummary.SavedActivity(snapshot.earthquakes.size)
        locationContext is LocationContext.Available -> calculateNearby(snapshot, locationContext)
        else -> SituationSummary.GlobalActivity(snapshot.earthquakes.size)
    }

    private fun calculateNearby(
        snapshot: EarthquakeSnapshot,
        location: LocationContext.Available,
    ): SituationSummary {
        val relativeEvents =
            snapshot.earthquakes.mapNotNull { earthquake ->
                earthquake.coordinates?.let { earthquake to RelativeLocationCalculator.calculate(location.coordinates, it) }
            }
        val significant =
            relativeEvents
                .filter { (earthquake, relative) ->
                    (earthquake.magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5 && relative.distanceKilometers <= 805.0
                }.minByOrNull { it.second.distanceKilometers }
        return significant?.let { SituationSummary.NearbySignificant(it.first, it.second) }
            ?: SituationSummary.NoSignificantNearby(relativeEvents.count { it.second.distanceKilometers <= 805.0 })
    }
}
