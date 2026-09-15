package com.besklar.grounded.ui.home

import com.besklar.grounded.model.EarthquakeSnapshot
import javax.inject.Inject

sealed interface SituationSummary {
    data class GlobalActivity(val eventCount: Int) : SituationSummary

    data object NoActivity : SituationSummary

    data class SavedActivity(val eventCount: Int) : SituationSummary
}

class SituationSummaryCalculator
@Inject
constructor() {
    fun calculate(snapshot: EarthquakeSnapshot?, refreshFailed: Boolean): SituationSummary = when {
        snapshot == null || snapshot.earthquakes.isEmpty() -> SituationSummary.NoActivity
        refreshFailed -> SituationSummary.SavedActivity(snapshot.earthquakes.size)
        else -> SituationSummary.GlobalActivity(snapshot.earthquakes.size)
    }
}
