package com.besklar.grounded.ui.home

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.besklar.grounded.MainDispatcherRule
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
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

    @Test
    fun `failed refresh keeps cached content visible`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val cached = snapshot()
        val repository = FakeRepository(result = RefreshResult.TransportFailure, initialSnapshot = cached)
        val viewModel = viewModel(repository)

        runCurrent()

        assertEquals(cached, viewModel.uiState.value.snapshot)
        assertEquals(RefreshStatus.Failed, viewModel.uiState.value.refreshStatus)
        assertEquals(false, viewModel.uiState.value.isFullScreenFailure)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `new event badges expire after thirty seconds`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository =
            FakeRepository(
                result = RefreshResult.Success(setOf("new-event"), 0, 0),
                initialSnapshot = snapshot(),
            )
        val viewModel = viewModel(repository)
        try {
            runCurrent()

            assertEquals(setOf("new-event"), viewModel.uiState.value.newEventIds)
            advanceTimeBy(30_001)
            assertTrue(viewModel.uiState.value.newEventIds.isEmpty())
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun `data older than thirty minutes is exposed as stale`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val repository =
            FakeRepository(
                result = RefreshResult.TransportFailure,
                initialSnapshot = snapshot(now.minusSeconds(31 * 60)),
            )
        val viewModel = viewModel(repository)
        runCurrent()

        assertTrue(viewModel.uiState.value.dataAge!! > java.time.Duration.ofMinutes(30))
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `filtering out the selected event clears selection`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val event = earthquake("selected", magnitude = 2.0)
        val repository = FakeRepository(result = RefreshResult.Success(emptySet(), 0, 0), initialSnapshot = snapshot(events = listOf(event)))
        val viewModel = viewModel(repository)
        runCurrent()

        viewModel.selectEvent(event.id)
        viewModel.updateFilters(HomeFilters(magnitude = MagnitudeFilter.FOUR_POINT_FIVE))

        assertEquals(null, viewModel.uiState.value.selectedEventId)
        viewModel.viewModelScope.cancel()
    }

    @Test
    fun `filters restore from saved state and reset to defaults`() = runTest(mainDispatcherRule.dispatcher.scheduler) {
        val savedState =
            SavedStateHandle(
                mapOf(
                    "time_range" to TimeRange.PAST_WEEK.name,
                    "magnitude_filter" to MagnitudeFilter.TWO_POINT_FIVE.name,
                    "earthquake_order" to EarthquakeOrder.STRONGEST.name,
                ),
            )
        val repository = FakeRepository(result = RefreshResult.Success(emptySet(), 0, 0))
        val viewModel =
            HomeViewModel(repository, FakeLocationRepository(), savedState, Clock.fixed(now, ZoneOffset.UTC))
        runCurrent()

        assertEquals(
            HomeFilters(TimeRange.PAST_WEEK, MagnitudeFilter.TWO_POINT_FIVE, EarthquakeOrder.STRONGEST),
            viewModel.uiState.value.filters,
        )
        viewModel.resetFilters()
        assertEquals(HomeFilters.DEFAULT, viewModel.uiState.value.filters)
        viewModel.viewModelScope.cancel()
    }

    private fun viewModel(repository: EarthquakeRepository) = HomeViewModel(
        repository = repository,
        locationRepository = FakeLocationRepository(),
        savedStateHandle = SavedStateHandle(),
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    private fun snapshot(retrievedAt: Instant = now, events: List<Earthquake> = emptyList()) = EarthquakeSnapshot(events, "past_7_days", retrievedAt, retrievedAt, null, "USGS")

    private fun earthquake(id: String, magnitude: Double) = Earthquake(id, magnitude, null, id, now, now, null, null, null, null, null, null, null, null)

    private class FakeRepository(
        private val result: RefreshResult? = null,
        private val gate: CompletableDeferred<RefreshResult>? = null,
        initialSnapshot: EarthquakeSnapshot? = null,
    ) : EarthquakeRepository {
        private val snapshots = MutableStateFlow(initialSnapshot)
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
