package com.besklar.grounded.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.location.LocationSearchRepository
import com.besklar.grounded.location.LocationSearchResult
import com.besklar.grounded.location.SearchScope
import com.besklar.grounded.model.Coordinates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val repository: EarthquakeRepository,
    private val locationRepository: LocationRepository,
    private val locationSearchRepository: LocationSearchRepository,
    private val savedStateHandle: SavedStateHandle,
    private val clock: Clock,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            HomeUiState(
                mode = savedStateHandle.get<String>(MODE_KEY)?.let(HomeMode::valueOf) ?: HomeMode.LIST,
                filters =
                HomeFilters(
                    timeRange = savedEnum(TIME_RANGE_KEY, TimeRange.PAST_DAY),
                    magnitude = savedEnum(MAGNITUDE_KEY, MagnitudeFilter.ALL),
                    order = savedEnum(ORDER_KEY, EarthquakeOrder.RECENT),
                ),
                searchQuery = savedStateHandle[SEARCH_QUERY_KEY] ?: "",
                searchScope = restoredSearchScope(),
                searchStatus = if (savedStateHandle.get<String>(SEARCH_LABEL_KEY) != null) SearchStatus.Resolved else SearchStatus.Idle,
            ),
        )
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()
    private val mutableEffects = MutableSharedFlow<HomeEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<HomeEffect> = mutableEffects

    private var refreshJob: Job? = null
    private var searchJob: Job? = null
    private var clearNewEventsJob: Job? = null
    private var clearRefreshStatusJob: Job? = null
    private val initialSnapshotObserved = CompletableDeferred<Unit>()

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collectLatest { snapshot ->
                val current = mutableUiState.value
                mutableUiState.value =
                    current.copy(
                        snapshot = snapshot,
                        dataAge = snapshot?.let { Duration.between(it.lastSuccessfulRetrieval, clock.instant()).coerceAtLeast(Duration.ZERO) },
                        selectedEventId = current.selectedEventId?.takeIf { it in visibleIds(snapshot, current.filters, current.locationContext, current.searchScope) },
                    )
                initialSnapshotObserved.complete(Unit)
            }
        }
        viewModelScope.launch {
            locationRepository.context.collectLatest { context ->
                mutableUiState.value = mutableUiState.value.copy(locationContext = context)
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(FRESHNESS_TICK_MILLIS)
                mutableUiState.value.snapshot?.let { snapshot ->
                    mutableUiState.value =
                        mutableUiState.value.copy(
                            dataAge = Duration.between(snapshot.lastSuccessfulRetrieval, clock.instant()).coerceAtLeast(Duration.ZERO),
                        )
                }
            }
        }
        refresh()
    }

    fun selectMode(mode: HomeMode) {
        savedStateHandle[MODE_KEY] = mode.name
        mutableUiState.value = mutableUiState.value.copy(mode = mode)
    }

    fun selectEvent(id: String?) {
        mutableUiState.value = mutableUiState.value.copy(selectedEventId = id)
    }

    fun updateFilters(filters: HomeFilters) {
        savedStateHandle[TIME_RANGE_KEY] = filters.timeRange.name
        savedStateHandle[MAGNITUDE_KEY] = filters.magnitude.name
        savedStateHandle[ORDER_KEY] = filters.order.name
        val current = mutableUiState.value
        mutableUiState.value =
            current.copy(
                filters = filters,
                selectedEventId = current.selectedEventId?.takeIf { it in visibleIds(current.snapshot, filters, current.locationContext, current.searchScope) },
            )
    }

    fun updateSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
        mutableUiState.value =
            mutableUiState.value.copy(
                searchQuery = query,
                searchStatus = if (mutableUiState.value.searchStatus is SearchStatus.Error) SearchStatus.Idle else mutableUiState.value.searchStatus,
            )
    }

    fun submitSearch() {
        val query = mutableUiState.value.searchQuery.trim()
        if (query.isEmpty()) {
            clearSearch()
            return
        }
        searchJob?.cancel()
        searchJob =
            viewModelScope.launch {
                mutableUiState.value = mutableUiState.value.copy(searchStatus = SearchStatus.Searching)
                when (val result = locationSearchRepository.search(query)) {
                    is LocationSearchResult.Success -> applySearchScope(query, result.scope)
                    LocationSearchResult.NotFound -> setSearchFailure(SearchFailure.NOT_FOUND)
                    LocationSearchResult.ProviderUnavailable -> setSearchFailure(SearchFailure.PROVIDER_UNAVAILABLE)
                    LocationSearchResult.Failed -> setSearchFailure(SearchFailure.FAILED)
                }
            }
    }

    fun clearSearch() {
        searchJob?.cancel()
        savedStateHandle[SEARCH_QUERY_KEY] = ""
        savedStateHandle.remove<String>(SEARCH_LABEL_KEY)
        savedStateHandle.remove<Double>(SEARCH_LATITUDE_KEY)
        savedStateHandle.remove<Double>(SEARCH_LONGITUDE_KEY)
        val current = mutableUiState.value
        mutableUiState.value = current.copy(searchQuery = "", searchScope = null, searchStatus = SearchStatus.Idle, selectedEventId = null)
    }

    fun resetFilters() {
        updateFilters(HomeFilters.DEFAULT)
    }

    fun loadLocation() {
        viewModelScope.launch { locationRepository.loadApproximateLocation() }
    }

    fun recordLocationDenial(permanently: Boolean) {
        locationRepository.recordDenial(permanently)
    }

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                initialSnapshotObserved.await()
                val hadSnapshotBeforeRefresh = mutableUiState.value.snapshot != null
                mutableUiState.value = mutableUiState.value.copy(refreshStatus = RefreshStatus.Refreshing)
                var newEventIds = emptySet<String>()
                val status =
                    when (val result = repository.refresh()) {
                        is RefreshResult.Success -> {
                            newEventIds = result.newEventIds.takeIf { hadSnapshotBeforeRefresh }.orEmpty()
                            RefreshStatus.Success(newEventIds.size)
                        }
                        is RefreshResult.HttpFailure,
                        RefreshResult.DecodingFailure,
                        RefreshResult.TransportFailure,
                        -> RefreshStatus.Failed
                    }
                mutableUiState.value =
                    mutableUiState.value.copy(
                        refreshStatus = status,
                        initialAttemptFinished = true,
                        newEventIds = newEventIds,
                    )
                if (newEventIds.isNotEmpty()) {
                    mutableEffects.tryEmit(HomeEffect.NewEarthquakes)
                    clearNewEventsJob?.cancel()
                    clearNewEventsJob =
                        viewModelScope.launch {
                            delay(NEW_EVENT_DURATION_MILLIS)
                            mutableUiState.value = mutableUiState.value.copy(newEventIds = emptySet())
                        }
                }
                clearRefreshStatusJob?.cancel()
                clearRefreshStatusJob =
                    viewModelScope.launch {
                        delay(REFRESH_CONFIRMATION_MILLIS)
                        if (mutableUiState.value.refreshStatus is RefreshStatus.Success) {
                            mutableUiState.value = mutableUiState.value.copy(refreshStatus = RefreshStatus.Idle)
                        }
                    }
            }
    }

    private companion object {
        const val MODE_KEY = "home_mode"
        const val TIME_RANGE_KEY = "time_range"
        const val MAGNITUDE_KEY = "magnitude_filter"
        const val ORDER_KEY = "earthquake_order"
        const val SEARCH_QUERY_KEY = "search_query"
        const val SEARCH_LABEL_KEY = "search_label"
        const val SEARCH_LATITUDE_KEY = "search_latitude"
        const val SEARCH_LONGITUDE_KEY = "search_longitude"
        const val NEW_EVENT_DURATION_MILLIS = 30_000L
        const val REFRESH_CONFIRMATION_MILLIS = 3_000L
        const val FRESHNESS_TICK_MILLIS = 60_000L
    }

    private inline fun <reified T : Enum<T>> savedEnum(key: String, default: T): T = savedStateHandle.get<String>(key)?.let { value -> runCatching { enumValueOf<T>(value) }.getOrNull() } ?: default

    private fun visibleIds(
        snapshot: com.besklar.grounded.model.EarthquakeSnapshot?,
        filters: HomeFilters,
        locationContext: LocationContext,
        searchScope: SearchScope?,
    ): Set<String> = EarthquakeFilter
        .apply(
            earthquakes = snapshot?.earthquakes.orEmpty(),
            filters = filters,
            now = clock.instant(),
            userCoordinates = (locationContext as? LocationContext.Available)?.coordinates,
            searchScope = searchScope,
        ).mapTo(mutableSetOf()) { it.id }

    private fun applySearchScope(query: String, scope: SearchScope) {
        savedStateHandle[SEARCH_QUERY_KEY] = query
        savedStateHandle[SEARCH_LABEL_KEY] = scope.label
        savedStateHandle[SEARCH_LATITUDE_KEY] = scope.center.latitude
        savedStateHandle[SEARCH_LONGITUDE_KEY] = scope.center.longitude
        val current = mutableUiState.value
        mutableUiState.value = current.copy(searchQuery = query, searchScope = scope, searchStatus = SearchStatus.Resolved, selectedEventId = null)
    }

    private fun setSearchFailure(failure: SearchFailure) {
        mutableUiState.value = mutableUiState.value.copy(searchStatus = SearchStatus.Error(failure))
    }

    private fun restoredSearchScope(): SearchScope? {
        val label = savedStateHandle.get<String>(SEARCH_LABEL_KEY) ?: return null
        val latitude = savedStateHandle.get<Double>(SEARCH_LATITUDE_KEY) ?: return null
        val longitude = savedStateHandle.get<Double>(SEARCH_LONGITUDE_KEY) ?: return null
        if (!latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
        return SearchScope(label, Coordinates(latitude, longitude))
    }
}
