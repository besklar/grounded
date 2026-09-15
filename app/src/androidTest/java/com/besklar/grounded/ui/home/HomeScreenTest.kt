package com.besklar.grounded.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.besklar.grounded.location.LocationContext
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
                    state = HomeUiState(snapshot = snapshot(), initialAttemptFinished = true),
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
                    state = HomeUiState(snapshot = snapshot(emptyList()), initialAttemptFinished = true),
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

    private fun snapshot(earthquakes: List<Earthquake>? = null): EarthquakeSnapshot {
        val now = Instant.now()
        val earthquake =
            Earthquake(
                "test-event",
                3.2,
                "ml",
                "Test place",
                now,
                now,
                null,
                4.0,
                null,
                null,
                null,
                null,
                null,
                null,
            )
        return EarthquakeSnapshot(earthquakes ?: listOf(earthquake), "past_24_hours", now, now, null, "USGS")
    }
}
