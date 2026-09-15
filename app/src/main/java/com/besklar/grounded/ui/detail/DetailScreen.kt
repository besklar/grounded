package com.besklar.grounded.ui.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.besklar.grounded.BuildConfig
import com.besklar.grounded.R
import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.ui.home.EarthquakeFormatter
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    earthquake: Earthquake?,
    relativeLocation: RelativeLocation?,
    isLoading: Boolean,
    asOf: Instant,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    mapsConfigured: Boolean = BuildConfig.MAPS_CONFIGURED,
    refreshing: Boolean = false,
    refreshFailed: Boolean = false,
    onRefresh: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.earthquake_details)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
            )
        },
    ) { padding ->
        if (earthquake == null && isLoading) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                CircularProgressIndicator()
                Text(stringResource(R.string.loading_earthquake_details))
            }
        } else if (earthquake == null) {
            MissingEvent(onBack = onBack, modifier = Modifier.padding(padding))
        } else {
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding).testTag("detail-pull-refresh"),
            ) {
                DetailContent(
                    earthquake = earthquake,
                    relativeLocation = relativeLocation,
                    mapsConfigured = mapsConfigured,
                    refreshFailed = refreshFailed,
                    asOf = asOf,
                )
            }
        }
    }
}

@Composable
private fun DetailContent(
    earthquake: Earthquake,
    relativeLocation: RelativeLocation?,
    mapsConfigured: Boolean,
    refreshFailed: Boolean,
    asOf: Instant,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val zoneId = ZoneId.systemDefault()
    val number = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 } }
    val coordinateNumber = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 3 } }
    val occurred =
        remember(earthquake.occurredAt, locale, zoneId) {
            DateFormat
                .getDateTimeInstance(DateFormat.FULL, DateFormat.SHORT, locale)
                .apply { timeZone = java.util.TimeZone.getTimeZone(zoneId) }
                .format(Date.from(earthquake.occurredAt))
        }
    val shareText = remember(earthquake, locale, asOf, zoneId) { buildShareText(earthquake, locale, asOf, zoneId) }
    val safeUrl = remember(earthquake.detailUrl) { safeUsgsUrl(earthquake.detailUrl) }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        if (refreshFailed) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.detail_refresh_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(12.dp),
                )
            }
        }
        EarthquakeHero(earthquake, EarthquakeFormatter.relativeTime(earthquake.occurredAt, asOf, locale, zoneId), locale)

        earthquake.coordinates?.let { coordinates ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.event_location), style = MaterialTheme.typography.titleLarge)
                EventLocationMap(
                    coordinates = coordinates,
                    magnitude = EarthquakeFormatter.magnitude(earthquake, locale),
                    mapsConfigured = mapsConfigured,
                )
                Text(
                    text = "${coordinateNumber.format(coordinates.latitude)}, ${coordinateNumber.format(coordinates.longitude)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.event_facts), style = MaterialTheme.typography.titleLarge)
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    DetailRow(stringResource(R.string.occurred), occurred)
                    EarthquakeFormatter.depth(earthquake, locale)?.let { DetailRow(stringResource(R.string.depth), it) }
                    relativeLocation?.let {
                        DetailRow(stringResource(R.string.distance_from_you), EarthquakeFormatter.relativeLocation(it, locale))
                    }
                    earthquake.magnitudeType?.let { DetailRow(stringResource(R.string.magnitude_type), it.uppercase(locale)) }
                    earthquake.feltReports?.let { DetailRow(stringResource(R.string.felt_reports), number.format(it)) }
                    earthquake.significance?.let { DetailRow(stringResource(R.string.usgs_significance), number.format(it)) }
                    earthquake.alert?.let { DetailRow(stringResource(R.string.alert_level), it.replaceFirstChar(Char::uppercase)) }
                    earthquake.tsunami?.let {
                        DetailRow(
                            stringResource(R.string.tsunami),
                            stringResource(if (it) R.string.flagged else R.string.not_flagged),
                        )
                    }
                    earthquake.reviewStatus?.let { DetailRow(stringResource(R.string.review_status), it.replaceFirstChar(Char::uppercase)) }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(intent, context.getString(R.string.share_earthquake)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Text(stringResource(R.string.share))
            }
            if (safeUrl != null) {
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, safeUrl.toUri()))
                        } catch (_: ActivityNotFoundException) {
                            // The official URL remains visible in share text; no unsafe fallback is attempted.
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null)
                    Text(stringResource(R.string.usgs_source))
                }
            }
        }
        Text(stringResource(R.string.usgs_attribution), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun EarthquakeHero(
    earthquake: Earthquake,
    relativeTime: String,
    locale: java.util.Locale,
) {
    val severe = (earthquake.magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5
    Card(
        colors =
        CardDefaults.cardColors(
            containerColor = if (severe) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (severe) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = stringResource(R.string.reported_earthquake).uppercase(locale),
                style = MaterialTheme.typography.labelLarge,
            )
            Text(
                text = "M ${EarthquakeFormatter.magnitude(earthquake, locale)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
            )
            Text(EarthquakeFormatter.place(earthquake, locale), style = MaterialTheme.typography.headlineSmall)
            Text(relativeTime, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun EventLocationMap(
    coordinates: com.besklar.grounded.model.Coordinates,
    magnitude: String,
    mapsConfigured: Boolean,
) {
    val point = remember(coordinates) { LatLng(coordinates.latitude, coordinates.longitude) }
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(point, 8f)
    }
    val markerState = remember(point) { MarkerState(point) }
    val shape = MaterialTheme.shapes.large
    Box(
        modifier =
        Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (mapsConfigured) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraState,
                properties = MapProperties(mapType = MapType.NORMAL),
                uiSettings =
                MapUiSettings(
                    compassEnabled = false,
                    indoorLevelPickerEnabled = false,
                    mapToolbarEnabled = false,
                    myLocationButtonEnabled = false,
                    rotationGesturesEnabled = false,
                    scrollGesturesEnabled = false,
                    scrollGesturesEnabledDuringRotateOrZoom = false,
                    tiltGesturesEnabled = false,
                    zoomControlsEnabled = false,
                    zoomGesturesEnabled = false,
                ),
            ) {
                Marker(
                    state = markerState,
                    title = stringResource(R.string.earthquake_details),
                    snippet = "M $magnitude",
                    contentDescription = stringResource(R.string.event_map_description, magnitude),
                )
            }
        } else {
            Text(
                text = stringResource(R.string.map_preview_unavailable),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(24.dp),
            )
        }
        Surface(
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            shape = RoundedCornerShape(50),
            shadowElevation = 3.dp,
            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
        ) {
            Text(
                text = stringResource(R.string.event_epicenter),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun MissingEvent(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.event_unavailable), style = MaterialTheme.typography.titleLarge)
        Text(
            stringResource(R.string.event_unavailable_explanation),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.back_to_earthquakes))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.weight(0.38f),
        )
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.62f))
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}
