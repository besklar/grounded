package com.besklar.grounded.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.location.RelativeLocationCalculator
import com.besklar.grounded.model.Earthquake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DetailUiState(
    val earthquake: Earthquake? = null,
    val relativeLocation: RelativeLocation? = null,
)

@HiltViewModel
class DetailViewModel
@Inject
constructor(
    repository: EarthquakeRepository,
    locationRepository: LocationRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    val uiState: StateFlow<DetailUiState> =
        combine(repository.observeSnapshot(), locationRepository.context) { snapshot, location ->
            val earthquake = snapshot?.earthquakes?.firstOrNull { it.id == eventId }
            val relative =
                (location as? LocationContext.Available)?.coordinates?.let { user ->
                    earthquake?.coordinates?.let { event -> RelativeLocationCalculator.calculate(user, event) }
                }
            DetailUiState(earthquake, relative)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())
}
