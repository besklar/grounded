package com.besklar.grounded.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso.pressBack
import com.besklar.grounded.location.LocationContext
import com.besklar.grounded.location.SearchScope
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.model.EarthquakeSnapshot
import com.besklar.grounded.ui.theme.GroundedTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class HomeScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun loadingStateIsStableAndVisible() {
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithText("Loading recent earthquakes").assertIsDisplayed()
    }

    @Test
    fun listSelectionEmitsStableEventId() {
        var selected: String? = null
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), mode = HomeMode.LIST, initialAttemptFinished = true),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = { selected = it },
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.onNodeWithText("Test place").performClick()
        composeRule.runOnIdle { assertEquals("test-event", selected) }
    }

    @Test
    fun noCacheFailureOffersRetry() {
        var retried = false
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(refreshStatus = RefreshStatus.Failed, initialAttemptFinished = true),
                    onModeSelected = {},
                    onRefresh = { retried = true },
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.onNodeWithText("Earthquake data is unavailable").assertIsDisplayed()
        composeRule.onNodeWithText("Retry").performClick()
        composeRule.runOnIdle { assertEquals(true, retried) }
    }

    @Test
    fun cachedFailureKeepsResultsAndExplainsTheyAreSaved() {
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state =
                    HomeUiState(
                        snapshot = snapshot(),
                        mode = HomeMode.LIST,
                        refreshStatus = RefreshStatus.Failed,
                        initialAttemptFinished = true,
                    ),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.onNodeWithText("Offline", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("Test place").assertIsDisplayed()
    }

    @Test
    fun validEmptySnapshotIsNotPresentedAsFailure() {
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(emptyList()), mode = HomeMode.LIST, initialAttemptFinished = true),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.onNodeWithText("No earthquakes were reported", substring = true).assertIsDisplayed()
    }

    @Test
    fun locationExplanationPrecedesPermissionCallback() {
        var requested = false
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state =
                    HomeUiState(
                        snapshot = snapshot(),
                        mode = HomeMode.LIST,
                        initialAttemptFinished = true,
                        locationContext = LocationContext.NotRequested,
                    ),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = { requested = true },
                )
            }
        }

        composeRule.onNodeWithText("See earthquakes relative to you").performClick()
        composeRule.onNodeWithText("Your location is not uploaded or stored", substring = true).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(false, requested) }
    }

    @Test
    fun missingMapConfigurationHasActionableFallback() {
        composeRule.setContent {
            GroundedTheme {
                EarthquakeMap(
                    earthquakes = snapshot().earthquakes,
                    userCoordinates = null,
                    selectedEventId = null,
                    onEventSelected = {},
                    onOpenDetails = {},
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithText("Map setup needed").assertIsDisplayed()
        composeRule.onNodeWithText("The earthquake list remains available", substring = true).assertIsDisplayed()
    }

    @Test
    fun filterSheetChangesTimeRangeAndExplainsUnavailableNearestOrdering() {
        var filters = HomeFilters.DEFAULT
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), mode = HomeMode.LIST, initialAttemptFinished = true, filters = filters),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                    onFiltersChanged = { filters = it },
                    onResetFilters = {},
                )
            }
        }

        composeRule.onNodeWithText("Filters").performClick()
        composeRule.onNodeWithText("past hour").performClick()
        composeRule.runOnIdle { assertEquals(TimeRange.PAST_HOUR, filters.timeRange) }
        composeRule.onNodeWithText("nearest").assertIsNotEnabled()
        composeRule.onNodeWithText("Add approximate location to order by distance.").assertExists()
    }

    @Test
    fun filteredEmptyStateExplainsThatFiltersExcludedEvents() {
        val oldEvent =
            snapshot().earthquakes.single().copy(occurredAt = Instant.now().minusSeconds(2 * 60 * 60))
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state =
                    HomeUiState(
                        snapshot = snapshot(listOf(oldEvent)),
                        mode = HomeMode.LIST,
                        initialAttemptFinished = true,
                        filters = HomeFilters(timeRange = TimeRange.PAST_HOUR),
                    ),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.onNodeWithText("No earthquakes match the selected filters", substring = true).assertIsDisplayed()
    }

    @Test
    fun placeSearchAcceptsInputAndSubmitsFromKeyboard() {
        var query by mutableStateOf("")
        var submitted = false
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), initialAttemptFinished = true, searchQuery = query),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                    onSearchQueryChanged = { query = it },
                    onSearch = { submitted = true },
                )
            }
        }

        composeRule.onNodeWithText("City or postal code").performTextInput("Denver")
        composeRule.onNodeWithText("City or postal code").performImeAction()

        composeRule.runOnIdle {
            assertEquals("Denver", query)
            assertEquals(true, submitted)
        }
    }

    @Test
    fun mapOverlayOffersLayersAndExplainsLocationBeforeRequesting() {
        var requested = false
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), mode = HomeMode.MAP, initialAttemptFinished = true),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = { requested = true },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Map layers").performClick()
        composeRule.onNodeWithText("Terrain map").assertIsDisplayed()
        composeRule.onNodeWithText("Satellite map").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Locate me").performClick()
        composeRule.onNodeWithText("Your location is not uploaded or stored", substring = true).assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(false, requested) }
    }

    @Test
    fun resultPeekExpandsAndMapButtonCollapsesIt() {
        var mode by mutableStateOf(HomeMode.MAP)
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), mode = mode, initialAttemptFinished = true),
                    onModeSelected = { mode = it },
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("1 result. Expand results.").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("Collapse results and return to map").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(HomeMode.MAP, mode) }
        composeRule.onNodeWithContentDescription("Locate me").assertIsDisplayed()
    }

    @Test
    fun systemBackCollapsesExpandedResultsBeforeLeavingHome() {
        var mode by mutableStateOf(HomeMode.LIST)
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state = HomeUiState(snapshot = snapshot(), mode = mode, initialAttemptFinished = true),
                    onModeSelected = { mode = it },
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                )
            }
        }

        composeRule.waitForIdle()
        pressBack()
        composeRule.waitForIdle()

        composeRule.runOnIdle { assertEquals(HomeMode.MAP, mode) }
        composeRule.onNodeWithContentDescription("Locate me").assertIsDisplayed()
    }

    @Test
    fun placeScopeFiltersTheSharedResultCountAndList() {
        val now = Instant.now()
        val near = earthquake("near", "Near event", Coordinates(0.1, 0.1), now)
        val far = earthquake("far", "Far event", Coordinates(20.0, 20.0), now)
        composeRule.setContent {
            GroundedTheme {
                HomeScreen(
                    state =
                    HomeUiState(
                        snapshot = snapshot(listOf(near, far)),
                        mode = HomeMode.LIST,
                        initialAttemptFinished = true,
                        searchQuery = "Origin",
                        searchScope = SearchScope("Origin", Coordinates(0.0, 0.0)),
                        searchStatus = SearchStatus.Resolved,
                    ),
                    onModeSelected = {},
                    onRefresh = {},
                    onEventSelected = {},
                    onMapEventSelected = {},
                    onOpenDetails = {},
                    onRequestLocation = {},
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithText("1 result").assertIsDisplayed()
        composeRule.onNodeWithText("Near event").assertIsDisplayed()
        composeRule.onNodeWithText("Far event").assertDoesNotExist()
    }

    private fun snapshot(earthquakes: List<Earthquake>? = null): EarthquakeSnapshot {
        val now = Instant.now()
        val earthquake = earthquake("test-event", "Test place", null, now)
        return EarthquakeSnapshot(earthquakes ?: listOf(earthquake), "past_24_hours", now, now, null, "USGS")
    }

    private fun earthquake(
        id: String,
        place: String,
        coordinates: Coordinates?,
        now: Instant,
    ) = Earthquake(
        id,
        3.2,
        "ml",
        place,
        now,
        now,
        coordinates,
        4.0,
        null,
        null,
        null,
        null,
        null,
        null,
    )
}
