package com.besklar.grounded.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    newEventIds: Set<String>,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    userCoordinates: Coordinates?,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = onRefresh,
        modifier = modifier.fillMaxSize(),
    ) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            if (refreshFailed) {
                item(key = "offline") {
                    val locale = LocalConfiguration.current.locales[0]
                    val age =
                        lastUpdatedAt?.let {
                            EarthquakeFormatter.relativeTime(it, Instant.now(), locale, ZoneId.systemDefault())
                        } ?: stringResource(R.string.unknown_time)
                    Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
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
                )
                HorizontalDivider()
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
) {
    val locale = LocalConfiguration.current.locales[0]
    val now = remember { Instant.now() }
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

    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = accessibilityLabel }
            .padding(vertical = 14.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = magnitude, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

@Composable
private fun StatusChip(label: String) {
    AssistChip(onClick = {}, label = { Text(label) })
}
