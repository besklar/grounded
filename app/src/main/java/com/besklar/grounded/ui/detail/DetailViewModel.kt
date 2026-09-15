package com.besklar.grounded.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.location.RelativeLocationCalculator
import com.besklar.grounded.model.Earthquake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val earthquake: Earthquake? = null,
    val relativeLocation: RelativeLocation? = null,
    val isResolved: Boolean = false,
    val isRefreshing: Boolean = false,
    val refreshFailed: Boolean = false,
)

@HiltViewModel
class DetailViewModel
@Inject
constructor(
    private val repository: EarthquakeRepository,
    locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(savedStateHandle["eventId"])
    private val refreshState = MutableStateFlow(DetailRefreshState.IDLE)
    private var refreshJob: Job? = null

    val uiState: StateFlow<DetailUiState> =
        combine(repository.observeSnapshot(), locationRepository.context, refreshState) { snapshot, location, refresh ->
            val earthquake = snapshot?.earthquakes?.firstOrNull { it.id == eventId }
            val relative =
                (location as? LocationContext.Available)?.coordinates?.let { user ->
                    earthquake?.coordinates?.let { event -> RelativeLocationCalculator.calculate(user, event) }
                }
            DetailUiState(
                earthquake = earthquake,
                relativeLocation = relative,
                isResolved = snapshot != null,
                isRefreshing = refresh == DetailRefreshState.REFRESHING,
                refreshFailed = refresh == DetailRefreshState.FAILED,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun refresh() {
        if (refreshJob?.isActive == true) return
        refreshJob =
            viewModelScope.launch {
                refreshState.value = DetailRefreshState.REFRESHING
                refreshState.value =
                    when (repository.refresh()) {
                        is RefreshResult.Success -> DetailRefreshState.IDLE
                        is RefreshResult.HttpFailure,
                        RefreshResult.TransportFailure,
                        RefreshResult.DecodingFailure,
                        -> DetailRefreshState.FAILED
                    }
            }
    }
}

private enum class DetailRefreshState {
    IDLE,
    REFRESHING,
    FAILED,
}
