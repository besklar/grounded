package com.besklar.grounded.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.MainDispatcherRule
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `first successful load does not label the entire feed as new`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository = FakeRepository(result = RefreshResult.Success(setOf("one", "two"), 0, 0))
        val viewModel = viewModel(repository)

        runCurrent()

        assertEquals(RefreshStatus.Success(0), viewModel.uiState.value.refreshStatus)
        assertTrue(viewModel.uiState.value.newEventIds.isEmpty())
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `refresh taps share one in-flight ViewModel operation`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val gate = CompletableDeferred<RefreshResult>()
        val repository = FakeRepository(gate = gate)
        val viewModel = viewModel(repository)
        runCurrent()

        viewModel.refresh()
        viewModel.refresh()
        runCurrent()

        assertEquals(1, repository.refreshCalls)
        gate.complete(RefreshResult.Success(emptySet(), 0, 0))
        runCurrent()
        assertEquals(RefreshStatus.Success(0), viewModel.uiState.value.refreshStatus)
        viewModel.viewModelScope.cancel()
    }

    private fun viewModel(repository: EarthquakeRepository) = HomeViewModel(
        repository = repository,
        locationRepository = FakeLocationRepository(),
        savedStateHandle = SavedStateHandle(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private class FakeRepository(
        private val result: RefreshResult? = null,
        private val gate: CompletableDeferred<RefreshResult>? = null,
    ) : EarthquakeRepository {
        private val snapshots = MutableStateFlow<EarthquakeSnapshot?>(null)
        var refreshCalls = 0
            private set

        override fun observeSnapshot(): Flow<EarthquakeSnapshot?> = snapshots

        override suspend fun refresh(): RefreshResult {
            refreshCalls++
            return result ?: checkNotNull(gate).await()
        }
    }

    private class FakeLocationRepository : LocationRepository {
        override val context = MutableStateFlow<LocationContext>(LocationContext.NotRequested)

        override suspend fun loadApproximateLocation() = Unit

        override fun recordDenial(permanently: Boolean) = Unit
    }
}
