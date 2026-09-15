package com.besklar.grounded.location

import com.besklar.grounded.model.Coordinates
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class RelativeLocation(
    val distanceKilometers: Double,
    val direction: CompassDirection,
)

enum class CompassDirection(val label: String) {
    NORTH("north"),
    NORTHEAST("northeast"),
    EAST("east"),
    SOUTHEAST("southeast"),
    SOUTH("south"),
    SOUTHWEST("southwest"),
    WEST("west"),
    NORTHWEST("northwest"),
}

object RelativeLocationCalculator {
    private const val EARTH_RADIUS_KILOMETERS = 6_371.0088

    fun calculate(from: Coordinates, to: Coordinates): RelativeLocation {
        val fromLatitude = Math.toRadians(from.latitude)
        val toLatitude = Math.toRadians(to.latitude)
        val latitudeDelta = toLatitude - fromLatitude
        val longitudeDelta = Math.toRadians(to.longitude - from.longitude)
        val haversine =
            sin(latitudeDelta / 2).let { it * it } +
                cos(fromLatitude) * cos(toLatitude) * sin(longitudeDelta / 2).let { it * it }
        val distance = EARTH_RADIUS_KILOMETERS * 2 * atan2(sqrt(haversine), sqrt(1 - haversine))

        val y = sin(longitudeDelta) * cos(toLatitude)
        val x = cos(fromLatitude) * sin(toLatitude) - sin(fromLatitude) * cos(toLatitude) * cos(longitudeDelta)
        val bearing = (Math.toDegrees(atan2(y, x)) + 360.0) % 360.0
        val direction = CompassDirection.entries[((bearing + 22.5) / 45.0).toInt() % 8]
        return RelativeLocation(distance, direction)
    }
}
