package com.besklar.grounded.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.model.Earthquake
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class DetailViewModel
@Inject
constructor(
    repository: EarthquakeRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val eventId: String = checkNotNull(savedStateHandle["eventId"])

    val earthquake: StateFlow<Earthquake?> =
        repository
            .observeSnapshot()
            .map { snapshot -> snapshot?.earthquakes?.firstOrNull { it.id == eventId } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
