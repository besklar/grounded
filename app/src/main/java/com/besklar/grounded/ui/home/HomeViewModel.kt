package com.besklar.grounded.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel
@Inject
constructor(
    private val repository: EarthquakeRepository,
    private val locationRepository: LocationRepository,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val mutableUiState =
        MutableStateFlow(
            HomeUiState(
                mode = savedStateHandle.get<String>(MODE_KEY)?.let(HomeMode::valueOf) ?: HomeMode.LIST,
            ),
        )
    val uiState: StateFlow<HomeUiState> = mutableUiState.asStateFlow()

    private var refreshJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeSnapshot().collectLatest { snapshot ->
                mutableUiState.value = mutableUiState.value.copy(snapshot = snapshot)
            }
        }
        viewModelScope.launch {
            locationRepository.context.collectLatest { context ->
                mutableUiState.value = mutableUiState.value.copy(locationContext = context)
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
                mutableUiState.value = mutableUiState.value.copy(refreshStatus = RefreshStatus.Refreshing)
                var newEventIds = emptySet<String>()
                val status =
                    when (val result = repository.refresh()) {
                        is RefreshResult.Success -> {
                            newEventIds = result.newEventIds
                            RefreshStatus.Success(result.newEventIds.size)
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
            }
    }

    private companion object {
        const val MODE_KEY = "home_mode"
    }
}
