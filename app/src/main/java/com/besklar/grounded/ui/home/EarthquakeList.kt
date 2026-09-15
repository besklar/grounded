package com.besklar.grounded.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.besklar.grounded.R
import com.besklar.grounded.location.RelativeLocationCalculator
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EarthquakeList(
    earthquakes: List<Earthquake>,
    refreshing: Boolean,
    refreshFailed: Boolean,
    lastUpdatedAt: Instant?,
    now: Instant,
    newEventIds: Set<String>,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    userCoordinates: Coordinates?,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            if (refreshFailed) {
                item(key = "offline") {
                    val locale = LocalConfiguration.current.locales[0]
                    val age =
                        lastUpdatedAt?.let {
                            EarthquakeFormatter.relativeTime(it, now, locale, ZoneId.systemDefault())
                        } ?: stringResource(R.string.unknown_time)
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = stringResource(R.string.offline_saved_results, age),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }
            items(earthquakes, key = Earthquake::id) { earthquake ->
                EarthquakeRow(
                    earthquake = earthquake,
                    isNew = earthquake.id in newEventIds,
                    onClick = { onEventSelected(earthquake.id) },
                    userCoordinates = userCoordinates,
                    now = now,
                )
            }
        }
    }
}

@Composable
private fun EarthquakeRow(
    earthquake: Earthquake,
    isNew: Boolean,
    onClick: () -> Unit,
    userCoordinates: Coordinates?,
    now: Instant,
) {
    val locale = LocalConfiguration.current.locales[0]
    val magnitude = EarthquakeFormatter.magnitude(earthquake, locale)
    val place = EarthquakeFormatter.place(earthquake, locale)
    val relativeTime = EarthquakeFormatter.relativeTime(earthquake.occurredAt, now, locale, ZoneId.systemDefault())
    val depth = EarthquakeFormatter.depth(earthquake, locale)
    val relative =
        userCoordinates?.let { user ->
            earthquake.coordinates?.let { event ->
                EarthquakeFormatter.relativeLocation(RelativeLocationCalculator.calculate(user, event), locale)
            }
        }
    val accessibilityLabel = stringResource(R.string.earthquake_row_description, magnitude, place, relativeTime)
    val magnitudeColor =
        if ((earthquake.magnitude ?: Double.NEGATIVE_INFINITY) >= 4.5) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.primary
        }

    Surface(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = accessibilityLabel },
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(horizontalAlignment = Alignment.Start) {
                Text(
                    text = "M",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = magnitude,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = magnitudeColor,
                )
            }
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(text = place, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = listOfNotNull(relativeTime, depth, relative).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isNew) StatusChip(stringResource(R.string.new_event))
                    earthquake.alert?.let { StatusChip(it.uppercase(locale)) }
                    if (earthquake.tsunami == true) StatusChip(stringResource(R.string.tsunami_flag))
                }
            }
        }
    }
}

@Composable
private fun StatusChip(label: String) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium)
    }
}
