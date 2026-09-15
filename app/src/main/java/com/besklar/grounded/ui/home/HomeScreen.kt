package com.besklar.grounded.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.besklar.grounded.R
import com.besklar.grounded.location.LocationContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onModeSelected: (HomeMode) -> Unit,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onMapEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onRefresh, enabled = state.refreshStatus !is RefreshStatus.Refreshing) {
                        Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            SituationSummaryCard(state, onRequestLocation)
            Spacer(Modifier.height(16.dp))
            ModeSelector(state.mode, onModeSelected)
            Spacer(Modifier.height(12.dp))
            val saveableStateHolder = rememberSaveableStateHolder()
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    state.isInitialLoading -> LoadingState()
                    state.isFullScreenFailure -> FailureState(onRefresh)
                    state.snapshot?.earthquakes?.isEmpty() == true -> EmptyState()
                    else -> saveableStateHolder.SaveableStateProvider(state.mode.name) {
                        if (state.mode == HomeMode.MAP) {
                            EarthquakeMap(
                                earthquakes = state.snapshot?.earthquakes.orEmpty(),
                                userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates,
                                selectedEventId = state.selectedEventId,
                                onEventSelected = onMapEventSelected,
                                onOpenDetails = onOpenDetails,
                            )
                        } else {
                            EarthquakeList(
                                earthquakes = state.snapshot?.earthquakes.orEmpty(),
                                refreshing = state.refreshStatus is RefreshStatus.Refreshing,
                                refreshFailed = state.refreshStatus is RefreshStatus.Failed,
                                lastUpdatedAt = state.snapshot?.lastSuccessfulRetrieval,
                                newEventIds = state.newEventIds,
                                onRefresh = onRefresh,
                                onEventSelected = onEventSelected,
                                userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SituationSummaryCard(state: HomeUiState, onRequestLocation: () -> Unit) {
    val calculator = SituationSummaryCalculator()
    val summary = calculator.calculate(state.snapshot, state.refreshStatus is RefreshStatus.Failed, state.locationContext)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var showLocationExplanation by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text =
            if (state.isInitialLoading) {
                stringResource(R.string.checking_recent_activity)
            } else {
                when (summary) {
                    is SituationSummary.GlobalActivity -> stringResource(R.string.recent_activity)
                    is SituationSummary.SavedActivity -> stringResource(R.string.showing_saved_activity)
                    is SituationSummary.NearbySignificant ->
                        stringResource(
                            R.string.significant_nearby,
                            EarthquakeFormatter.magnitude(summary.earthquake, locale),
                        )
                    is SituationSummary.NoSignificantNearby -> stringResource(R.string.no_significant_nearby)
                    SituationSummary.NoActivity -> stringResource(R.string.no_recent_activity)
                }
            },
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text =
            if (state.isInitialLoading) {
                stringResource(R.string.checking_recent_activity_explanation)
            } else {
                when (summary) {
                    is SituationSummary.GlobalActivity ->
                        pluralStringResource(R.plurals.events_past_day, summary.eventCount, summary.eventCount)
                    is SituationSummary.SavedActivity ->
                        pluralStringResource(R.plurals.saved_events_count, summary.eventCount, summary.eventCount)
                    is SituationSummary.NearbySignificant -> EarthquakeFormatter.relativeLocation(summary.relative, locale)
                    is SituationSummary.NoSignificantNearby ->
                        pluralStringResource(R.plurals.nearby_events, summary.nearbyCount, summary.nearbyCount)
                    SituationSummary.NoActivity -> stringResource(R.string.no_results_explanation)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.refreshStatus is RefreshStatus.Refreshing && state.snapshot != null) {
            Text(stringResource(R.string.refreshing), style = MaterialTheme.typography.labelMedium)
        }
        state.snapshot?.let { snapshot ->
            val freshness =
                EarthquakeFormatter.relativeTime(
                    occurredAt = snapshot.lastSuccessfulRetrieval,
                    now = java.time.Instant.now(),
                    locale = locale,
                    zoneId = java.time.ZoneId.systemDefault(),
                )
            Text(
                text =
                if ((state.dataAge ?: java.time.Duration.ZERO) > java.time.Duration.ofMinutes(30)) {
                    stringResource(R.string.stale_updated, freshness)
                } else {
                    stringResource(R.string.last_updated, freshness)
                },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        when (val status = state.refreshStatus) {
            is RefreshStatus.Success ->
                Text(
                    text =
                    if (status.newCount == 0) {
                        stringResource(R.string.up_to_date)
                    } else {
                        pluralStringResource(R.plurals.new_earthquakes, status.newCount, status.newCount)
                    },
                    style = MaterialTheme.typography.labelLarge,
                )
            else -> Unit
        }
        if (state.locationContext is LocationContext.NotRequested || state.locationContext is LocationContext.Denied) {
            TextButton(onClick = { showLocationExplanation = true }) {
                Text(stringResource(R.string.add_location_context))
            }
        }
    }
    if (showLocationExplanation) {
        AlertDialog(
            onDismissRequest = { showLocationExplanation = false },
            title = { Text(stringResource(R.string.location_explanation_title)) },
            text = { Text(stringResource(R.string.location_explanation_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLocationExplanation = false
                        onRequestLocation()
                    },
                ) {
                    Text(
                        stringResource(
                            if ((state.locationContext as? LocationContext.Denied)?.permanently == true) {
                                R.string.open_settings
                            } else {
                                R.string.use_my_location
                            },
                        ),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showLocationExplanation = false }) { Text(stringResource(R.string.not_now)) }
            },
        )
    }
    when (state.locationContext) {
        LocationContext.Loading -> Text(stringResource(R.string.finding_location))
        LocationContext.ServicesDisabled -> Text(stringResource(R.string.location_services_disabled))
        LocationContext.Unavailable -> Text(stringResource(R.string.location_unavailable))
        else -> Unit
    }
}

@Composable
private fun ModeSelector(selected: HomeMode, onSelected: (HomeMode) -> Unit) {
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        HomeMode.entries.forEachIndexed { index, mode ->
            SegmentedButton(
                selected = mode == selected,
                onClick = { onSelected(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, HomeMode.entries.size),
                label = { Text(stringResource(if (mode == HomeMode.MAP) R.string.map else R.string.list)) },
            )
        }
    }
}

@Composable
private fun LoadingState() {
    val description = stringResource(R.string.loading_earthquakes)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        CircularProgressIndicator()
        Text(description)
    }
}

@Composable
private fun FailureState(onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.could_not_load), style = MaterialTheme.typography.titleLarge)
        androidx.compose.material3.Button(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun EmptyState() {
    Text(stringResource(R.string.no_results_explanation), style = MaterialTheme.typography.bodyLarge)
}
