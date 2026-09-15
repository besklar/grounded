package com.besklar.grounded.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.besklar.grounded.location.SearchScope
import com.besklar.grounded.model.Earthquake
import com.google.maps.android.compose.MapType
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onModeSelected: (HomeMode) -> Unit,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
    onFiltersChanged: (HomeFilters) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onSearchQueryChanged: (String) -> Unit = {},
    onSearch: () -> Unit = {},
    onClearSearch: () -> Unit = {},
    mapsConfigured: Boolean = com.besklar.grounded.BuildConfig.MAPS_CONFIGURED,
) {
    val userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates
    val mapEarthquakes =
        EarthquakeFilter.apply(
            earthquakes = state.snapshot?.earthquakes.orEmpty(),
            filters = state.filters,
            now = Instant.now(),
            userCoordinates = userCoordinates,
            searchScope = null,
        )
    var mapViewport by remember { mutableStateOf<MapViewport?>(null) }
    val visibleEarthquakes =
        mapViewport?.let { viewport ->
            mapEarthquakes.filter { earthquake -> earthquake.coordinates?.let(viewport::contains) == true }
        } ?: EarthquakeFilter.apply(
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
            state.searchScope != null ->
                "search:${state.searchScope.label}:${state.searchScope.center.latitude}:${state.searchScope.center.longitude}:${state.filters.distance.name}"
            else -> null
        }

    fun locateMap() {
        mapViewport = null
        cameraIntent = "locate"
        locateRequest++
        onClearSearch()
        onRequestLocation()
    }

    fun updateFilters(filters: HomeFilters) {
        if (filters.distance != state.filters.distance && state.searchScope != null) {
            mapViewport = null
            cameraIntent = "search"
        }
        onFiltersChanged(filters)
    }

    fun resetFilters() {
        if (state.searchScope != null) {
            mapViewport = null
            cameraIntent = "search"
        }
        onResetFilters()
    }

    Scaffold(modifier = modifier, containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier = Modifier.padding(start = 12.dp).height(80.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    FilterControls(
                        filters = state.filters,
                        locationAvailable = userCoordinates != null,
                        searchActive = state.searchScope != null,
                        onFiltersChanged = ::updateFilters,
                        onReset = ::resetFilters,
                        compact = true,
                    )
                }
                PlaceSearchBar(
                    query = state.searchQuery,
                    status = state.searchStatus,
                    scope = state.searchScope,
                    radiusKilometers = state.filters.distance.radiusKilometers,
                    onQueryChanged = onSearchQueryChanged,
                    onSearch = {
                        mapViewport = null
                        cameraIntent = "search"
                        onSearch()
                    },
                    onClear = onClearSearch,
                    modifier = Modifier.weight(1f).padding(start = 8.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                )
            }
            if (useSidePane) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MapSurface(
                        state = filteredState,
                        mapEarthquakes = mapEarthquakes,
                        mapType = mapType,
                        cameraFocus = cameraFocus,
                        cameraFocusKey = cameraFocusKey,
                        onRefresh = onRefresh,
                        onOpenDetails = onOpenDetails,
                        onMapTypeChanged = { mapTypeName = it.name },
                        onLocate = ::locateMap,
                        onViewportChanged = { mapViewport = it },
                        mapsConfigured = mapsConfigured,
                        modifier = Modifier.weight(0.62f).fillMaxHeight(),
                    )
                    Column(modifier = Modifier.weight(0.38f).fillMaxHeight().padding(end = 12.dp)) {
                        SituationSummaryCard(filteredState, onRequestLocation)
                        FilterControls(
                            filters = state.filters,
                            locationAvailable = userCoordinates != null,
                            searchActive = state.searchScope != null,
                            onFiltersChanged = ::updateFilters,
                            onReset = ::resetFilters,
                            showButton = false,
                        )
                        Spacer(Modifier.height(8.dp))
                        HomeModeContent(
                            state = filteredState.copy(mode = HomeMode.LIST),
                            filtersExcludedAll = filtersExcludedAll,
                            onRefresh = onRefresh,
                            onEventSelected = onEventSelected,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            } else {
                PortraitMapAndResults(
                    state = filteredState,
                    mapEarthquakes = mapEarthquakes,
                    resultCount = visibleEarthquakes.size,
                    viewportActive = mapViewport != null,
                    filtersExcludedAll = filtersExcludedAll,
                    mapType = mapType,
                    cameraFocus = cameraFocus,
                    cameraFocusKey = cameraFocusKey,
                    onModeSelected = onModeSelected,
                    onRefresh = onRefresh,
                    onEventSelected = onEventSelected,
                    onOpenDetails = onOpenDetails,
                    onFiltersChanged = ::updateFilters,
                    onResetFilters = ::resetFilters,
                    onMapTypeChanged = { mapTypeName = it.name },
                    onLocate = ::locateMap,
                    onViewportChanged = { mapViewport = it },
                    mapsConfigured = mapsConfigured,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitMapAndResults(
    state: HomeUiState,
    mapEarthquakes: List<Earthquake>,
    resultCount: Int,
    viewportActive: Boolean,
    filtersExcludedAll: Boolean,
    mapType: MapType,
    cameraFocus: com.besklar.grounded.model.Coordinates?,
    cameraFocusKey: Any?,
    onModeSelected: (HomeMode) -> Unit,
    onRefresh: () -> Unit,
    onEventSelected: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onFiltersChanged: (HomeFilters) -> Unit,
    onResetFilters: () -> Unit,
    onMapTypeChanged: (MapType) -> Unit,
    onLocate: () -> Unit,
    onViewportChanged: (MapViewport) -> Unit,
    mapsConfigured: Boolean,
) {
    val sheetState =
        rememberStandardBottomSheetState(
            initialValue = if (state.mode == HomeMode.LIST) SheetValue.Expanded else SheetValue.PartiallyExpanded,
            skipHiddenState = true,
        )
    val scaffoldState = rememberBottomSheetScaffoldState(sheetState)
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val expanded = sheetState.currentValue == SheetValue.Expanded || sheetState.targetValue == SheetValue.Expanded
    val resultLabel = pluralStringResource(R.plurals.results_count, resultCount, resultCount)
    val expandResultLabel = stringResource(R.string.expand_results_description, resultLabel)
    val showMapDescription = stringResource(R.string.show_map_description)

    LaunchedEffect(state.mode) {
        if (state.mode == HomeMode.LIST) sheetState.expand() else sheetState.partialExpand()
    }
    LaunchedEffect(sheetState.currentValue) {
        when (sheetState.currentValue) {
            SheetValue.Expanded -> onModeSelected(HomeMode.LIST)
            SheetValue.PartiallyExpanded -> onModeSelected(HomeMode.MAP)
            SheetValue.Hidden -> Unit
        }
    }
    BackHandler(enabled = expanded) {
        onModeSelected(HomeMode.MAP)
        coroutineScope.launch { sheetState.partialExpand() }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 72.dp,
        sheetContainerColor = MaterialTheme.colorScheme.background,
        sheetContent = {
            Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
                Column(modifier = Modifier.fillMaxSize()) {
                    if (!expanded) {
                        Row(
                            modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable(role = Role.Button) {
                                    onModeSelected(HomeMode.LIST)
                                    coroutineScope.launch { sheetState.expand() }
                                }
                                .semantics { contentDescription = expandResultLabel }
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                resultLabel,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                    }
                    if (expanded) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            CompactResultsHeader(
                                state = state,
                                resultCount = resultCount,
                                viewportActive = viewportActive,
                            )
                            FilterControls(
                                filters = state.filters,
                                locationAvailable = state.locationContext is LocationContext.Available,
                                searchActive = state.searchScope != null,
                                onFiltersChanged = onFiltersChanged,
                                onReset = onResetFilters,
                                showButton = false,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        HomeModeContent(
                            state = state.copy(mode = HomeMode.LIST),
                            filtersExcludedAll = filtersExcludedAll,
                            onRefresh = onRefresh,
                            onEventSelected = onEventSelected,
                            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                            listState = listState,
                            listContentPadding = PaddingValues(bottom = 88.dp),
                        )
                    }
                }
                if (expanded) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            onModeSelected(HomeMode.MAP)
                            coroutineScope.launch { sheetState.partialExpand() }
                        },
                        modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp)
                            .semantics { contentDescription = showMapDescription },
                        icon = { Icon(Icons.Rounded.Map, contentDescription = null) },
                        text = { Text(stringResource(R.string.map)) },
                    )
                }
            }
        },
    ) { _ ->
        MapSurface(
            state = state,
            mapEarthquakes = mapEarthquakes,
            mapType = mapType,
            cameraFocus = cameraFocus,
            cameraFocusKey = cameraFocusKey,
            onRefresh = onRefresh,
            onOpenDetails = onOpenDetails,
            onMapTypeChanged = onMapTypeChanged,
            onLocate = onLocate,
            onViewportChanged = onViewportChanged,
            mapsConfigured = mapsConfigured,
            modifier = Modifier.fillMaxSize(),
            showStatusOverlay = !expanded,
        )
    }
}

@Composable
private fun PlaceSearchBar(
    query: String,
    status: SearchStatus,
    scope: SearchScope?,
    radiusKilometers: Double,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val locale = LocalConfiguration.current.locales[0]
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
                scope?.let {
                    Text(
                        stringResource(R.string.within_search_area, EarthquakeFormatter.distance(radiusKilometers, locale), it.label),
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
    mapEarthquakes: List<Earthquake>,
    mapType: MapType,
    cameraFocus: com.besklar.grounded.model.Coordinates?,
    cameraFocusKey: Any?,
    onRefresh: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onMapTypeChanged: (MapType) -> Unit,
    onLocate: () -> Unit,
    onViewportChanged: (MapViewport) -> Unit,
    mapsConfigured: Boolean,
    modifier: Modifier = Modifier,
    showStatusOverlay: Boolean = true,
) {
    var showLocationExplanation by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        EarthquakeMap(
            earthquakes = mapEarthquakes,
            userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates,
            selectedEventId = state.selectedEventId?.takeIf { selected -> state.snapshot?.earthquakes?.any { it.id == selected } == true },
            onOpenDetails = onOpenDetails,
            mapsConfigured = mapsConfigured,
            mapType = mapType,
            cameraFocus = cameraFocus,
            cameraFocusKey = cameraFocusKey,
            cameraFocusRadiusKilometers = state.filters.distance.radiusKilometers,
            onViewportChanged = onViewportChanged,
            showRecenterButton = false,
        )
        if (showStatusOverlay && state.isInitialLoading) {
            LoadingState()
        } else if (showStatusOverlay && state.isFullScreenFailure) {
            FailureState(onRefresh)
        }
        Column(
            modifier = Modifier.align(Alignment.CenterStart).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MapTypeControl(mapType, onMapTypeChanged)
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
    modifier: Modifier = Modifier,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    listContentPadding: PaddingValues = PaddingValues(),
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            state.isInitialLoading -> LoadingState()
            state.isFullScreenFailure -> FailureState(onRefresh)
            state.snapshot?.earthquakes?.isEmpty() == true -> EmptyState(filtersExcludedAll)
            else ->
                EarthquakeList(
                    earthquakes = state.snapshot?.earthquakes.orEmpty(),
                    refreshing = state.refreshStatus is RefreshStatus.Refreshing,
                    refreshFailed = state.refreshStatus is RefreshStatus.Failed,
                    lastUpdatedAt = state.snapshot?.lastSuccessfulRetrieval,
                    newEventIds = state.newEventIds,
                    onRefresh = onRefresh,
                    onEventSelected = onEventSelected,
                    userCoordinates = (state.locationContext as? LocationContext.Available)?.coordinates,
                    listState = listState,
                    contentPadding = listContentPadding,
                )
        }
    }
}

@Composable
private fun CompactResultsHeader(
    state: HomeUiState,
    resultCount: Int,
    viewportActive: Boolean,
) {
    val locale = LocalConfiguration.current.locales[0]
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text =
            pluralStringResource(
                if (viewportActive) R.plurals.earthquakes_on_map else R.plurals.earthquakes_count,
                resultCount,
                resultCount,
            ),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        when {
            state.refreshStatus is RefreshStatus.Refreshing ->
                Text(
                    text = stringResource(R.string.refreshing),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            state.snapshot != null -> {
                val freshness =
                    EarthquakeFormatter.relativeTime(
                        occurredAt = state.snapshot.lastSuccessfulRetrieval,
                        now = Instant.now(),
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
        }
    }
}

@Composable
private fun SituationSummaryCard(
    state: HomeUiState,
    onRequestLocation: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    showLocationAction: Boolean = true,
) {
    val calculator = SituationSummaryCalculator()
    val summary = calculator.calculate(state.snapshot, state.refreshStatus is RefreshStatus.Failed, state.locationContext)
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    var showLocationExplanation by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (showTitle) {
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
        }
        Text(
            text =
            if (state.isInitialLoading) {
                stringResource(R.string.checking_recent_activity_explanation)
            } else {
                when (summary) {
                    is SituationSummary.GlobalActivity ->
                        state.searchScope?.let { scope ->
                            pluralStringResource(
                                R.plurals.events_near_place,
                                summary.eventCount,
                                summary.eventCount,
                                EarthquakeFormatter.distance(state.filters.distance.radiusKilometers, locale),
                                scope.label,
                                timeRangeLabel(state.filters.timeRange),
                            )
                        } ?: pluralStringResource(
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
        if (showLocationAction && (state.locationContext is LocationContext.NotRequested || state.locationContext is LocationContext.Denied)) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterControls(
    filters: HomeFilters,
    locationAvailable: Boolean,
    searchActive: Boolean,
    onFiltersChanged: (HomeFilters) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    showButton: Boolean = true,
) {
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val activeFilterCount = filters.activeCount(searchActive)
    if (compact) {
        FilledTonalIconButton(onClick = { showSheet = true }, modifier = modifier) {
            Icon(
                Icons.Rounded.Tune,
                contentDescription =
                if (activeFilterCount == 0) {
                    stringResource(R.string.filters)
                } else {
                    pluralStringResource(R.plurals.filters_changed, activeFilterCount, activeFilterCount)
                },
            )
        }
    } else {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (showButton) {
                OutlinedButton(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Tune, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (activeFilterCount == 0) {
                            stringResource(R.string.filters)
                        } else {
                            pluralStringResource(R.plurals.filters_changed, activeFilterCount, activeFilterCount)
                        },
                    )
                }
            }
            Text(
                text =
                if (searchActive) {
                    stringResource(
                        R.string.filter_summary_with_distance,
                        timeRangeLabel(filters.timeRange),
                        magnitudeLabel(filters.magnitude),
                        if (filters.order == EarthquakeOrder.NEAREST && !locationAvailable) {
                            stringResource(R.string.nearest_unavailable_summary)
                        } else {
                            orderLabel(filters.order)
                        },
                        distanceLabel(filters.distance),
                    )
                } else {
                    stringResource(
                        R.string.filter_summary,
                        timeRangeLabel(filters.timeRange),
                        magnitudeLabel(filters.magnitude),
                        if (filters.order == EarthquakeOrder.NEAREST && !locationAvailable) {
                            stringResource(R.string.nearest_unavailable_summary)
                        } else {
                            orderLabel(filters.order)
                        },
                    )
                },
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
                FilterSection(title = stringResource(R.string.search_radius)) {
                    DistanceFilter.entries.forEach { option ->
                        FilterOption(
                            label = distanceLabel(option),
                            supportingText = if (searchActive) null else stringResource(R.string.search_radius_requires_place),
                            selected = option == filters.distance,
                            enabled = searchActive,
                            onClick = { onFiltersChanged(filters.copy(distance = option)) },
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
private fun distanceLabel(value: DistanceFilter): String {
    val locale = LocalConfiguration.current.locales[0]
    return EarthquakeFormatter.distance(value.radiusKilometers, locale)
}

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
            if (filtersExcludedAll) R.string.filters_excluded_all else R.string.no_results_explanation,
        ),
        style = MaterialTheme.typography.bodyLarge,
    )
}
