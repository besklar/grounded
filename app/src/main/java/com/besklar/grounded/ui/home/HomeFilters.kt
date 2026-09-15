package com.besklar.grounded.ui.home

import com.besklar.grounded.location.RelativeLocationCalculator
import com.besklar.grounded.location.SearchScope
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import java.time.Duration
import java.time.Instant

enum class TimeRange(val duration: Duration) {
    PAST_HOUR(Duration.ofHours(1)),
    PAST_DAY(Duration.ofDays(1)),
    PAST_WEEK(Duration.ofDays(7)),
}

enum class MagnitudeFilter(val minimum: Double?) {
    ALL(null),
    TWO_POINT_FIVE(2.5),
    FOUR_POINT_FIVE(4.5),
}

enum class EarthquakeOrder {
    RECENT,
    NEAREST,
    STRONGEST,
}

data class HomeFilters(
    val timeRange: TimeRange = TimeRange.PAST_DAY,
    val magnitude: MagnitudeFilter = MagnitudeFilter.ALL,
    val order: EarthquakeOrder = EarthquakeOrder.RECENT,
) {
    val activeCount: Int
        get() =
            listOf(
                timeRange != DEFAULT.timeRange,
                magnitude != DEFAULT.magnitude,
                order != DEFAULT.order,
            ).count { it }

    companion object {
        val DEFAULT = HomeFilters()
    }
}

object EarthquakeFilter {
    fun apply(
        earthquakes: List<Earthquake>,
        filters: HomeFilters,
        now: Instant,
        userCoordinates: Coordinates?,
        searchScope: SearchScope? = null,
    ): List<Earthquake> {
        val earliest = now.minus(filters.timeRange.duration)
        val filtered =
            earthquakes.filter { earthquake ->
                !earthquake.occurredAt.isBefore(earliest) &&
                    filters.magnitude.minimum?.let { minimum ->
                        earthquake.magnitude?.let { it >= minimum } == true
                    } != false &&
                    searchScope?.let { scope ->
                        earthquake.coordinates?.let { coordinates ->
                            RelativeLocationCalculator.calculate(scope.center, coordinates).distanceKilometers <= scope.radiusKilometers
                        } == true
                    } != false
            }
        return when (filters.order) {
            EarthquakeOrder.RECENT -> filtered.sortedWith(recentComparator)
            EarthquakeOrder.STRONGEST ->
                filtered.sortedWith(
                    compareByDescending<Earthquake> { it.magnitude != null }
                        .thenByDescending { it.magnitude }
                        .then(recentComparator),
                )
            EarthquakeOrder.NEAREST ->
                userCoordinates?.let { user ->
                    filtered.sortedWith(
                        compareBy<Earthquake> { it.coordinates == null }
                            .thenBy {
                                it.coordinates?.let { coordinates ->
                                    RelativeLocationCalculator.calculate(user, coordinates).distanceKilometers
                                } ?: Double.POSITIVE_INFINITY
                            }.then(recentComparator),
                    )
                } ?: filtered.sortedWith(recentComparator)
        }
    }

    private val recentComparator = compareByDescending<Earthquake> { it.occurredAt }.thenBy { it.id }
}
