package com.besklar.grounded.location

import com.besklar.grounded.model.Coordinates
import java.time.Instant

sealed interface LocationContext {
    data object NotRequested : LocationContext

    data object Loading : LocationContext

    data class Available(
        val coordinates: Coordinates,
        val capturedAt: Instant,
    ) : LocationContext

    data class Denied(val permanently: Boolean) : LocationContext

    data object ServicesDisabled : LocationContext

    data object Unavailable : LocationContext
}
