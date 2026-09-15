package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Coordinates
import com.google.android.gms.maps.model.LatLngBounds

/** The geographic rectangle currently visible inside the map. */
data class MapViewport(
    val south: Double,
    val west: Double,
    val north: Double,
    val east: Double,
) {
    fun contains(coordinates: Coordinates): Boolean {
        if (coordinates.latitude !in south..north) return false
        return if (west <= east) {
            coordinates.longitude in west..east
        } else {
            coordinates.longitude >= west || coordinates.longitude <= east
        }
    }

    companion object {
        fun from(bounds: LatLngBounds) = MapViewport(
            south = bounds.southwest.latitude,
            west = bounds.southwest.longitude,
            north = bounds.northeast.latitude,
            east = bounds.northeast.longitude,
        )
    }
}
