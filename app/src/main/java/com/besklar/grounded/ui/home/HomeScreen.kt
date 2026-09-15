package com.besklar.grounded.ui.home

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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.besklar.grounded.R
import com.besklar.grounded.location.LocationContext
import com.google.maps.android.compose.MapType
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
    onSearchQueryChanged: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onClearSearch: () -> Unit = {},
) {
    val userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates
    val visibleEarthquakes =
        EarthquakeFilter.apply(
            earthquakes = state.snapshot?.earthquakes.orEmpty(),
            filters = state.filters,
            now = Instant.now(),
            userCoordinates = userCoordinates,
            searchScope = state.searchScope,
        )
    val filteredState = state.copy(snapshot = state.snapshot?.copy(earthquakes = visibleEarthquakes))
    val filtersExcludedAll = state.snapshot?.earthquakes?.isNotEmpty() == true && visibleEarthquakes.isEmpty()
    val windowWidth = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    val useSidePane = windowWidth >= 600.dp
    var mapTypeName by rememberSaveable { mutableStateOf(MapType.NORMAL.name) }
    val mapType = runCatching { MapType.valueOf(mapTypeName) }.getOrDefault(MapType.NORMAL)
    var locateRequest by rememberSaveable { mutableIntStateOf(0) }
    var cameraIntent by rememberSaveable { mutableStateOf("search") }
    val cameraFocus = if (cameraIntent == "locate" && userCoordinates != null) userCoordinates else state.searchScope?.center
    val cameraFocusKey =
        when {
            cameraIntent == "locate" && userCoordinates != null -> "locate:$locateRequest:${userCoordinates.latitude}:${userCoordinates.longitude}"
            state.searchScope != null -> "search:${state.searchScope.label}:${state.searchScope.center.latitude}:${state.searchScope.center.longitude}"
            else -> null
        }

    Scaffold(modifier = modifier) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PlaceSearchBar(
                query = state.searchQuery,
                status = state.searchStatus,
                scopeLabel = state.searchScope?.label,
                onQueryChanged = onSearchQueryChanged,
                onSearch = {
                    cameraIntent = "search"
                    onSearch()
                },
                onClear = onClearSearch,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (useSidePane) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapSurface(
                        state = filteredState,
                        mapType = mapType,
                        cameraFocus = cameraFocus,
                        cameraFocusKey = cameraFocusKey,
                        onRefresh = onRefresh,
                        onMapEventSelected = onMapEventSelected,
                        onOpenDetails = onOpenDetails,
                        filters = state.filters,
                        onFiltersChanged = onFiltersChanged,
                        onResetFilters = onResetFilters,
                        onMapTypeChanged = { mapTypeName = it.name },
                        onLocate = {
                            cameraIntent = "locate"
                            locateRequest++
                            onRequestLocation()
                        },
                        modifier = Modifier.weight(0.62f).fillMaxHeight(),
                    )
                    Column(modifier = Modifier.weight(0.38f).fillMaxHeight().padding(end = 12.dp)) {
                        SituationSummaryCard(filteredState, onRequestLocation)
                        FilterControls(
                            filters = state.filters,
                            locationAvailable = userCoordinates != null,
                            onFiltersChanged = onFiltersChanged,
                            onReset = onResetFilters,
                        )
                        Spacer(Modifier.height(8.dp))
                        HomeModeContent(
                            state = filteredState.copy(mode = HomeMode.LIST),
                            filtersExcludedAll = filtersExcludedAll,
                            onRefresh = onRefresh,
                            onEventSelected = onEventSelected,
                            onMapEventSelected = onMapEventSelected,
                            onOpenDetails = onOpenDetails,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else if (state.mode == HomeMode.MAP) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MapSurface(
                        state = filteredState,
                        mapType = mapType,
                        cameraFocus = cameraFocus,
                        cameraFocusKey = cameraFocusKey,
                        onRefresh = onRefresh,
                        onMapEventSelected = onMapEventSelected,
                        onOpenDetails = onOpenDetails,
                        filters = state.filters,
                        onFiltersChanged = onFiltersChanged,
                        onResetFilters = onResetFilters,
                        onMapTypeChanged = { mapTypeName = it.name },
                        onLocate = {
                            cameraIntent = "locate"
                            locateRequest++
                            onRequestLocation()
                        },
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { onModeSelected(HomeMode.LIST) }, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Icon(Icons.AutoMirrored.Rounded.List, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(pluralStringResource(R.plurals.show_results, visibleEarthquakes.size, visibleEarthquakes.size))
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    OutlinedButton(onClick = { onModeSelected(HomeMode.MAP) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Rounded.Map, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.map))
                    }
                    SituationSummaryCard(filteredState, onRequestLocation)
                    FilterControls(
                        filters = state.filters,
                        locationAvailable = userCoordinates != null,
                        onFiltersChanged = onFiltersChanged,
                        onReset = onResetFilters,
                    )
                    Spacer(Modifier.height(8.dp))
                    HomeModeContent(
                        state = filteredState.copy(mode = HomeMode.LIST),
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
}

@Composable
private fun PlaceSearchBar(
    query: String,
    status: SearchStatus,
    scopeLabel: String?,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text(stringResource(R.string.search_place_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                when {
                    status is SearchStatus.Searching -> CircularProgressIndicator(modifier = Modifier.width(24.dp))
                    query.isNotEmpty() ->
                        IconButton(onClick = onClear) {
                            Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.clear_search))
                        }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions =
            KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    onSearch()
                },
            ),
        )
        when (status) {
            SearchStatus.Searching -> Text(stringResource(R.string.searching_place), style = MaterialTheme.typography.labelMedium)
            SearchStatus.Resolved ->
                scopeLabel?.let {
                    Text(
                        stringResource(R.string.within_search_area, it),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            is SearchStatus.Error ->
                Text(
                    text =
                    stringResource(
                        when (status.failure) {
                            SearchFailure.NOT_FOUND -> R.string.search_place_not_found
                            SearchFailure.PROVIDER_UNAVAILABLE -> R.string.search_provider_unavailable
                            SearchFailure.FAILED -> R.string.search_place_failed
                        },
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            SearchStatus.Idle -> Unit
        }
    }
}

@Composable
private fun MapSurface(
    state: HomeUiState,
    mapType: MapType,
    cameraFocus: com.besklar.grounded.model.Coordinates?,
    cameraFocusKey: Any?,
    onRefresh: () -> Unit,
    onMapEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    filters: HomeFilters,
    onFiltersChanged: (HomeFilters) -> Unit,
    onResetFilters: () -> Unit,
    onMapTypeChanged: (MapType) -> Unit,
    onLocate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showLocationExplanation by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        EarthquakeMap(
            earthquakes = state.snapshot?.earthquakes.orEmpty(),
            userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates,
            selectedEventId = state.selectedEventId,
            onEventSelected = onMapEventSelected,
            onOpenDetails = onOpenDetails,
            mapType = mapType,
            cameraFocus = cameraFocus,
            cameraFocusKey = cameraFocusKey,
            showRecenterButton = false,
        )
        if (state.isInitialLoading) {
            LoadingState()
        } else if (state.isFullScreenFailure) {
            FailureState(onRefresh)
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapTypeControl(mapType, onMapTypeChanged)
            FilterControls(
                filters = filters,
                locationAvailable = state.locationContext is LocationContext.Available,
                onFiltersChanged = onFiltersChanged,
                onReset = onResetFilters,
                compact = true,
            )
            FilledTonalIconButton(
                onClick = {
                    if (state.locationContext is LocationContext.Available) onLocate() else showLocationExplanation = true
                },
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = stringResource(R.string.locate_me))
            }
        }
        FilledTonalIconButton(
            onClick = onRefresh,
            enabled = state.refreshStatus !is RefreshStatus.Refreshing,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = stringResource(R.string.refresh))
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
                        onLocate()
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
            dismissButton = { TextButton(onClick = { showLocationExplanation = false }) { Text(stringResource(R.string.not_now)) } },
        )
    }
}

@Composable
private fun MapTypeControl(selected: MapType, onSelected: (MapType) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box {
        FilledTonalIconButton(onClick = { expanded = true }) {
            Icon(Icons.Rounded.Layers, contentDescription = stringResource(R.string.map_layers))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(
                MapType.NORMAL to R.string.normal_map,
                MapType.TERRAIN to R.string.terrain_map,
                MapType.SATELLITE to R.string.satellite_map,
            ).forEach { (type, label) ->
                DropdownMenuItem(
                    text = { Text(stringResource(label)) },
                    onClick = {
                        onSelected(type)
                        expanded = false
                    },
                    leadingIcon = { RadioButton(selected = type == selected, onClick = null) },
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
    compact: Boolean = false,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    if (compact) {
        FilledTonalIconButton(onClick = { showSheet = true }) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription =
                if (filters.activeCount == 0) {
                    stringResource(R.string.filters)
                } else {
                    pluralStringResource(R.plurals.filters_changed, filters.activeCount, filters.activeCount)
                },
            )
        }
    } else {
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
