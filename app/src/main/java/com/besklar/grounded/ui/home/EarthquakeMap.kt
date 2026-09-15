package com.besklar.grounded.ui.home

import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import com.besklar.grounded.BuildConfig
import com.besklar.grounded.R
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun EarthquakeMap(
    earthquakes: List<Earthquake>,
    userCoordinates: Coordinates?,
    selectedEventId: String?,
    onEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    mapsConfigured: Boolean = BuildConfig.MAPS_CONFIGURED,
) {
    if (!mapsConfigured) {
        MapConfigurationMissing(modifier)
        return
    }

    val cameraState = rememberCameraPositionState()
    val locale = LocalConfiguration.current.locales[0]
    val unknownMagnitude = stringResource(R.string.unknown_magnitude)
    val unknownLocation = stringResource(R.string.location_unavailable_short)
    val magnitudeDescription = stringResource(R.string.magnitude_accessibility)
    val mappable =
        remember(earthquakes, locale, unknownMagnitude, unknownLocation, magnitudeDescription) {
            earthquakes.mapNotNull { earthquake ->
                MapEarthquake.from(
                    earthquake = earthquake,
                    unknownLocation = unknownLocation,
                    magnitudeDescription = magnitudeDescription,
                    unknownMagnitude = unknownMagnitude,
                    magnitudeValue = { EarthquakeFormatter.magnitudeValue(it, locale) },
                )
            }
        }
    var mapLoaded by remember { mutableStateOf(false) }
    var initialCameraSet by remember { mutableStateOf(false) }

    LaunchedEffect(mapLoaded, mappable, initialCameraSet) {
        if (mapLoaded && !initialCameraSet && mappable.isNotEmpty()) {
            val update = initialCameraUpdate(mappable, userCoordinates)
            cameraState.move(update)
            initialCameraSet = true
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = false),
            uiSettings = MapUiSettings(compassEnabled = true, myLocationButtonEnabled = false),
            onMapLoaded = { mapLoaded = true },
        ) {
            Clustering(
                items = mappable,
                onClusterClick = { cluster ->
                    cameraState.move(clusterCameraUpdate(cluster, cameraState.position.zoom))
                    true
                },
                onClusterItemClick = { item ->
                    onEventSelected(item.id)
                    true
                },
            )
            mappable.firstOrNull { it.id == selectedEventId }?.let { selected ->
                Marker(
                    state = MarkerState(selected.mapPosition),
                    title = selected.markerTitle,
                    snippet = selected.markerSnippet,
                    contentDescription = selected.contentDescription,
                    icon = rememberMarkerIcon(selected.magnitude, selected = true),
                    zIndex = 2f,
                )
            }
            userCoordinates?.let {
                Marker(
                    state = MarkerState(LatLng(it.latitude, it.longitude)),
                    title = stringResource(R.string.approximate_location),
                    contentDescription = stringResource(R.string.your_approximate_location),
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
                    zIndex = 3f,
                )
            }
        }
        userCoordinates?.let { user ->
            FloatingActionButton(
                onClick = {
                    cameraState.move(CameraUpdateFactory.newLatLngZoom(LatLng(user.latitude, user.longitude), 6f))
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.recenter_map))
            }
        }
        earthquakes.firstOrNull { it.id == selectedEventId }?.let { selected ->
            CompactMapSelection(
                earthquake = selected,
                onOpenDetails = { onOpenDetails(selected.id) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            )
        }
    }
}

@Composable
private fun MapConfigurationMissing(modifier: Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        androidx.compose.material3.Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
        Text(stringResource(R.string.map_configuration_needed), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.map_configuration_explanation),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun CompactMapSelection(
    earthquake: Earthquake,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locale = LocalConfiguration.current.locales[0]
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "M ${EarthquakeFormatter.magnitude(earthquake, locale)}",
                style = MaterialTheme.typography.headlineSmall,
            )
            Text(EarthquakeFormatter.place(earthquake, locale), style = MaterialTheme.typography.titleMedium)
            androidx.compose.material3.TextButton(onClick = onOpenDetails) {
                Text(stringResource(R.string.view_details))
            }
        }
    }
}

@Composable
private fun rememberMarkerIcon(magnitude: Double?, selected: Boolean): BitmapDescriptor {
    val density = LocalDensity.current.density
    val fill =
        if ((magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }
    val outline = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surface
    return remember(magnitude, selected, density, fill, outline) {
        val normalized = ((magnitude ?: 0.0) + 1.0).coerceIn(0.0, 8.0)
        val size = ((28.0 + normalized * 3.0) * density).toInt()
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius * 0.88f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = outline.toArgb() })
        canvas.drawCircle(radius, radius, radius * 0.68f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill.toArgb() })
        BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}

internal data class MapEarthquake(
    val id: String,
    val mapPosition: LatLng,
    val magnitude: Double?,
    val place: String?,
    val markerTitle: String,
    val markerSnippet: String,
    val contentDescription: String,
) : ClusterItem {
    override fun getPosition(): LatLng = mapPosition

    override fun getTitle(): String = markerTitle

    override fun getSnippet(): String = markerSnippet

    override fun getZIndex(): Float = 1f

    companion object {
        fun from(
            earthquake: Earthquake,
            unknownLocation: String = "Location unavailable",
            magnitudeDescription: String = "magnitude",
            unknownMagnitude: String = "unknown",
            magnitudeValue: (Double) -> String = Double::toString,
        ): MapEarthquake? {
            val coordinates = earthquake.coordinates ?: return null
            val place = earthquake.place ?: unknownLocation
            val magnitude = earthquake.magnitude?.let(magnitudeValue) ?: unknownMagnitude
            return MapEarthquake(
                id = earthquake.id,
                mapPosition = LatLng(coordinates.latitude, coordinates.longitude),
                magnitude = earthquake.magnitude,
                place = earthquake.place,
                markerTitle = place,
                markerSnippet = "$magnitudeDescription $magnitude",
                contentDescription = "$place, $magnitudeDescription $magnitude",
            )
        }
    }
}

private fun clusterCameraUpdate(cluster: Cluster<MapEarthquake>, currentZoom: Float) = clusterCameraUpdate(cluster.items.map(MapEarthquake::mapPosition), currentZoom)

internal fun clusterCameraUpdate(positions: Collection<LatLng>, currentZoom: Float) = if (positions.distinct().size <= 1) {
    CameraUpdateFactory.newLatLngZoom(positions.first(), (currentZoom + 2f).coerceAtMost(20f))
} else {
    val bounds = LatLngBounds.builder().apply { positions.forEach(::include) }.build()
    CameraUpdateFactory.newLatLngBounds(bounds, 96)
}

private fun initialCameraUpdate(events: List<MapEarthquake>, userCoordinates: Coordinates?) = if (events.size == 1 && userCoordinates == null) {
    CameraUpdateFactory.newLatLngZoom(events.single().mapPosition, 5f)
} else {
    val bounds =
        LatLngBounds
            .builder()
            .apply {
                events.forEach { include(it.mapPosition) }
                userCoordinates?.let { include(LatLng(it.latitude, it.longitude)) }
            }.build()
    CameraUpdateFactory.newLatLngBounds(bounds, 96)
}
