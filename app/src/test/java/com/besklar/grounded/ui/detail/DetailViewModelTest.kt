package com.besklar.grounded.ui.detail

import androidx.lifecycle.SavedStateHandle
import com.besklar.grounded.MainDispatcherRule
import com.besklar.grounded.data.repository.EarthquakeRepository
import com.besklar.grounded.data.repository.RefreshResult
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.LocationRepository
import com.besklar.grounded.model.EarthquakeSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `refresh exposes progress and serializes repeated requests`() = runTest {
        val result = CompletableDeferred<RefreshResult>()
        val repository = FakeRepository(result)
        val viewModel = detailViewModel(repository)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.refresh()
        viewModel.refresh()
        runCurrent()

        assertTrue(viewModel.uiState.value.isRefreshing)
        assertEquals(1, repository.refreshCalls)

        result.complete(RefreshResult.Success(emptySet(), 0, 0))
        runCurrent()

        assertFalse(viewModel.uiState.value.isRefreshing)
        assertFalse(viewModel.uiState.value.refreshFailed)
        collection.cancel()
    }

    @Test
    fun `failed refresh preserves a resolved screen and exposes failure`() = runTest {
        val result = CompletableDeferred<RefreshResult>()
        val repository = FakeRepository(result)
        val viewModel = detailViewModel(repository)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        viewModel.refresh()
        runCurrent()
        result.complete(RefreshResult.TransportFailure)
        runCurrent()

        assertTrue(viewModel.uiState.value.isResolved)
        assertTrue(viewModel.uiState.value.refreshFailed)
        collection.cancel()
    }

    @Test
    fun `screen time advances while detail state is observed`() = runTest {
        val clock = MutableClock(Instant.parse("2026-09-13T20:00:00Z"))
        val repository = FakeRepository(CompletableDeferred())
        val viewModel = detailViewModel(repository, clock)
        val collection = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }

        runCurrent()
        assertEquals(clock.instant(), viewModel.uiState.value.asOf)

        clock.now = clock.now.plusSeconds(60)
        advanceTimeBy(60_000)
        runCurrent()

        assertEquals(clock.instant(), viewModel.uiState.value.asOf)
        collection.cancel()
    }

    private fun detailViewModel(
        repository: EarthquakeRepository,
        clock: Clock = Clock.fixed(Instant.parse("2026-09-13T20:00:00Z"), ZoneOffset.UTC),
    ) = DetailViewModel(
        repository = repository,
        locationRepository = FakeLocationRepository(),
        savedStateHandle = SavedStateHandle(mapOf("eventId" to "event")),
        clock = clock,
    )

    private class FakeRepository(private val result: CompletableDeferred<RefreshResult>) : EarthquakeRepository {
        private val snapshots = MutableStateFlow(EarthquakeSnapshot(emptyList(), "window", java.time.Instant.EPOCH, null, null, "USGS"))
        var refreshCalls = 0

        override fun observeSnapshot(): Flow<EarthquakeSnapshot?> = snapshots

        override suspend fun refresh(): RefreshResult {
            refreshCalls++
            return result.await()
        }
    }

    private class FakeLocationRepository : LocationRepository {
        override val context = MutableStateFlow<LocationContext>(LocationContext.NotRequested)

        override suspend fun loadApproximateLocation() = Unit

        override fun recordDenial(permanently: Boolean) = Unit
    }

    private class MutableClock(var now: Instant) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneId = ZoneOffset.UTC

        override fun withZone(zone: ZoneId): Clock = Clock.fixed(now, zone)
    }
}
