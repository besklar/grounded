package com.besklar.grounded.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.besklar.grounded.BuildConfig
import com.besklar.grounded.R
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.clustering.Cluster
import com.google.maps.android.clustering.ClusterItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MapsComposeExperimentalApi
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.clustering.Clustering
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlin.math.cos

@OptIn(MapsComposeExperimentalApi::class)
@Composable
fun EarthquakeMap(
    earthquakes: List<Earthquake>,
    userCoordinates: Coordinates?,
    selectedEventId: String?,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
    mapsConfigured: Boolean = BuildConfig.MAPS_CONFIGURED,
    mapType: MapType = MapType.NORMAL,
    cameraFocus: Coordinates? = null,
    cameraFocusKey: Any? = null,
    cameraFocusRadiusKilometers: Double = 805.0,
    onViewportChanged: (MapViewport) -> Unit = {},
    showRecenterButton: Boolean = true,
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
    val currentViewportCallback by rememberUpdatedState(onViewportChanged)

    LaunchedEffect(mapLoaded, mappable, initialCameraSet, cameraFocus) {
        if (mapLoaded && !initialCameraSet && cameraFocus == null && mappable.isNotEmpty()) {
            val update = initialCameraUpdate(mappable, userCoordinates)
            cameraState.move(update)
            initialCameraSet = true
        }
    }

    LaunchedEffect(mapLoaded, cameraFocusKey) {
        if (mapLoaded && cameraFocus != null && cameraFocusKey != null) {
            cameraState.move(focusCameraUpdate(cameraFocus, cameraFocusRadiusKilometers))
            initialCameraSet = true
        }
    }

    LaunchedEffect(mapLoaded, cameraState) {
        if (!mapLoaded) return@LaunchedEffect
        snapshotFlow { cameraState.isMoving to initialCameraSet }
            .distinctUntilChanged()
            .filter { (isMoving, cameraWasSet) -> !isMoving && cameraWasSet }
            .collect {
                cameraState.projection?.visibleRegion?.latLngBounds?.let { bounds ->
                    currentViewportCallback(MapViewport.from(bounds))
                }
            }
    }

    Box(modifier = modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraState,
            properties = MapProperties(isMyLocationEnabled = false, mapType = mapType),
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
                    onOpenDetails(item.id)
                    true
                },
                clusterContent = { cluster -> ClusterMarker(cluster.size) },
                clusterItemContent = { item ->
                    EarthquakeMarker(
                        magnitude = item.displayMagnitude,
                        severe = (item.magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5,
                        selected = item.id == selectedEventId,
                    )
                },
            )
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
        userCoordinates?.takeIf { showRecenterButton }?.let { user ->
            FloatingActionButton(
                onClick = {
                    cameraState.move(CameraUpdateFactory.newLatLngZoom(LatLng(user.latitude, user.longitude), 6f))
                },
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.recenter_map))
            }
        }
    }
}

@Composable
private fun ClusterMarker(count: Int) {
    Surface(
        modifier = Modifier.size(48.dp),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(3.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = 5.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (count > 99) "99+" else count.toString(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun EarthquakeMarker(
    magnitude: String,
    severe: Boolean,
    selected: Boolean,
) {
    Surface(
        modifier = Modifier.widthIn(min = 44.dp).heightIn(min = 34.dp),
        shape = RoundedCornerShape(50),
        color = if (severe) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        contentColor = if (severe) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimary,
        border = BorderStroke(if (selected) 4.dp else 2.dp, MaterialTheme.colorScheme.surface),
        shadowElevation = if (selected) 8.dp else 3.dp,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)) {
            Text(
                text = "M$magnitude",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Black,
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

internal data class MapEarthquake(
    val id: String,
    val mapPosition: LatLng,
    val magnitude: Double?,
    val place: String?,
    val displayMagnitude: String,
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
                displayMagnitude = magnitude,
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

private fun focusCameraUpdate(center: Coordinates, radiusKilometers: Double): com.google.android.gms.maps.CameraUpdate {
    val safeRadius = radiusKilometers.coerceIn(1.0, 20_000.0)
    val latitudeDelta = safeRadius / 111.32
    val longitudeScale = cos(Math.toRadians(center.latitude)).coerceAtLeast(0.01)
    val longitudeDelta = (safeRadius / (111.32 * longitudeScale)).coerceAtMost(180.0)
    val south = (center.latitude - latitudeDelta).coerceAtLeast(-85.0)
    val north = (center.latitude + latitudeDelta).coerceAtMost(85.0)
    val west = wrapLongitude(center.longitude - longitudeDelta)
    val east = wrapLongitude(center.longitude + longitudeDelta)
    return CameraUpdateFactory.newLatLngBounds(
        LatLngBounds(LatLng(south, west), LatLng(north, east)),
        96,
    )
}

private fun wrapLongitude(longitude: Double): Double = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0
