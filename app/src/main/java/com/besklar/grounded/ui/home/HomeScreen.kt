package com.besklar.grounded.ui.home

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.besklar.grounded.R
import com.besklar.grounded.location.LocationContext
import java.time.Instant

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
    onFiltersChanged: (HomeFilters) -> Unit = {},
    onResetFilters: () -> Unit = {},
) {
    val userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates
    val visibleEarthquakes =
        EarthquakeFilter.apply(
            earthquakes = state.snapshot?.earthquakes.orEmpty(),
            filters = state.filters,
            now = Instant.now(),
            userCoordinates = userCoordinates,
        )
    val filteredState = state.copy(snapshot = state.snapshot?.copy(earthquakes = visibleEarthquakes))
    val filtersExcludedAll = state.snapshot?.earthquakes?.isNotEmpty() == true && visibleEarthquakes.isEmpty()
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
        val contentModifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(modifier = contentModifier, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                SituationSummaryCard(
                    state = filteredState,
                    onRequestLocation = onRequestLocation,
                    modifier = Modifier.weight(0.42f).fillMaxHeight().verticalScroll(rememberScrollState()),
                )
                Column(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    ModeSelector(state.mode, onModeSelected)
                    FilterControls(
                        filters = state.filters,
                        locationAvailable = userCoordinates != null,
                        onFiltersChanged = onFiltersChanged,
                        onReset = onResetFilters,
                    )
                    Spacer(Modifier.height(12.dp))
                    HomeModeContent(
                        state = filteredState,
                        filtersExcludedAll = filtersExcludedAll,
                        onRefresh = onRefresh,
                        onEventSelected = onEventSelected,
                        onMapEventSelected = onMapEventSelected,
                        onOpenDetails = onOpenDetails,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Column(modifier = contentModifier) {
                SituationSummaryCard(filteredState, onRequestLocation)
                Spacer(Modifier.height(16.dp))
                ModeSelector(state.mode, onModeSelected)
                FilterControls(
                    filters = state.filters,
                    locationAvailable = userCoordinates != null,
                    onFiltersChanged = onFiltersChanged,
                    onReset = onResetFilters,
                )
                Spacer(Modifier.height(12.dp))
                HomeModeContent(
                    state = filteredState,
                    filtersExcludedAll = filtersExcludedAll,
                    onRefresh = onRefresh,
                    onEventSelected = onEventSelected,
                    onMapEventSelected = onMapEventSelected,
                    onOpenDetails = onOpenDetails,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeModeContent(
    state: HomeUiState,
    filtersExcludedAll: Boolean,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onMapEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isInitialLoading -> LoadingState()
            state.isFullScreenFailure -> FailureState(onRefresh)
            state.snapshot?.earthquakes?.isEmpty() == true -> EmptyState(filtersExcludedAll)
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

@Composable
private fun SituationSummaryCard(
    state: HomeUiState,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val calculator = SituationSummaryCalculator()
    val summary = calculator.calculate(state.snapshot, state.refreshStatus is RefreshStatus.Failed, state.locationContext)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var showLocationExplanation by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text =
            if (state.isInitialLoading) {
                stringResource(R.string.checking_recent_activity_explanation)
            } else {
                when (summary) {
                    is SituationSummary.GlobalActivity ->
                        pluralStringResource(
                            R.plurals.events_in_range,
                            summary.eventCount,
                            summary.eventCount,
                            timeRangeLabel(state.filters.timeRange),
                        )
                    is SituationSummary.SavedActivity ->
                        pluralStringResource(R.plurals.saved_events_count, summary.eventCount, summary.eventCount)
                    is SituationSummary.NearbySignificant -> EarthquakeFormatter.relativeLocation(summary.relative, locale)
                    is SituationSummary.NoSignificantNearby ->
                        pluralStringResource(
                            R.plurals.nearby_events,
                            summary.nearbyCount,
                            summary.nearbyCount,
                            timeRangeLabel(state.filters.timeRange),
                        )
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
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            else -> Unit
        }
        if (state.locationContext is LocationContext.NotRequested || state.locationContext is LocationContext.Denied) {
            TextButton(onClick = { showLocationExplanation = true }) {
                Text(stringResource(R.string.add_location_context))
            }
        }
        when (state.locationContext) {
            LocationContext.Loading -> Text(stringResource(R.string.finding_location))
            LocationContext.ServicesDisabled -> Text(stringResource(R.string.location_services_disabled))
            LocationContext.Unavailable -> Text(stringResource(R.string.location_unavailable))
            else -> Unit
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterControls(
    filters: HomeFilters,
    locationAvailable: Boolean,
    onFiltersChanged: (HomeFilters) -> Unit,
    onReset: () -> Unit,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedButton(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Rounded.Tune, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(
                if (filters.activeCount == 0) {
                    stringResource(R.string.filters)
                } else {
                    pluralStringResource(R.plurals.filters_changed, filters.activeCount, filters.activeCount)
                },
            )
        }
        Text(
            text =
            stringResource(
                R.string.filter_summary,
                timeRangeLabel(filters.timeRange),
                magnitudeLabel(filters.magnitude),
                if (filters.order == EarthquakeOrder.NEAREST && !locationAvailable) {
                    stringResource(R.string.nearest_unavailable_summary)
                } else {
                    orderLabel(filters.order)
                },
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.filter_earthquakes),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp).semantics { heading() },
                )
                FilterSection(title = stringResource(R.string.time_range)) {
                    TimeRange.entries.forEach { option ->
                        FilterOption(
                            label = timeRangeLabel(option),
                            selected = option == filters.timeRange,
                            onClick = { onFiltersChanged(filters.copy(timeRange = option)) },
                        )
                    }
                }
                HorizontalDivider()
                FilterSection(title = stringResource(R.string.magnitude)) {
                    MagnitudeFilter.entries.forEach { option ->
                        FilterOption(
                            label = magnitudeLabel(option),
                            selected = option == filters.magnitude,
                            onClick = { onFiltersChanged(filters.copy(magnitude = option)) },
                        )
                    }
                }
                HorizontalDivider()
                FilterSection(title = stringResource(R.string.ordering)) {
                    EarthquakeOrder.entries.forEach { option ->
                        val enabled = option != EarthquakeOrder.NEAREST || locationAvailable
                        FilterOption(
                            label = orderLabel(option),
                            supportingText =
                            if (option == EarthquakeOrder.NEAREST && !locationAvailable) {
                                stringResource(R.string.nearest_requires_location)
                            } else {
                                null
                            },
                            selected = option == filters.order,
                            enabled = enabled,
                            onClick = { onFiltersChanged(filters.copy(order = option)) },
                        )
                    }
                }
                TextButton(
                    onClick = onReset,
                    enabled = filters != HomeFilters.DEFAULT,
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(R.string.reset_filters))
                }
            }
        }
    }
}

@Composable
private fun FilterSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        content()
    }
}

@Composable
private fun FilterOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    Row(
        modifier =
        Modifier
            .fillMaxWidth()
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, enabled = enabled, onClick = null)
        Column(Modifier.padding(start = 8.dp)) {
            Text(label, color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun timeRangeLabel(value: TimeRange): String = stringResource(
    when (value) {
        TimeRange.PAST_HOUR -> R.string.past_hour
        TimeRange.PAST_DAY -> R.string.past_24_hours
        TimeRange.PAST_WEEK -> R.string.past_7_days
    },
)

@Composable
private fun magnitudeLabel(value: MagnitudeFilter): String = stringResource(
    when (value) {
        MagnitudeFilter.ALL -> R.string.all_magnitudes
        MagnitudeFilter.TWO_POINT_FIVE -> R.string.magnitude_2_5_plus
        MagnitudeFilter.FOUR_POINT_FIVE -> R.string.magnitude_4_5_plus
    },
)

@Composable
private fun orderLabel(value: EarthquakeOrder): String = stringResource(
    when (value) {
        EarthquakeOrder.RECENT -> R.string.most_recent
        EarthquakeOrder.NEAREST -> R.string.nearest
        EarthquakeOrder.STRONGEST -> R.string.strongest
    },
)

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
private fun EmptyState(filtersExcludedAll: Boolean) {
    Text(
        stringResource(
            if (filtersExcludedAll) R.string.filters_excluded_all else R.string.empty_list_explanation,
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
}
