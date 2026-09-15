package com.besklar.grounded.ui.detail

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.unit.Density
import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.ui.theme.GroundedTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Instant

class DetailScreenTest {
    @get:Rule val composeRule = createComposeRule()

    @Test
    fun resolvedMissingEventOffersAWorkingWayBack() {
        var wentBack = false
        composeRule.setContent {
            GroundedTheme {
                DetailScreen(
                    earthquake = null,
                    relativeLocation = null,
                    isLoading = false,
                    onBack = { wentBack = true },
                )
            }
        }

        composeRule.onNodeWithText("Earthquake no longer available").assertIsDisplayed()
        composeRule.onNodeWithText("Back to earthquakes").performClick()
        composeRule.runOnIdle { assertTrue(wentBack) }
    }

    @Test
    fun largeTextKeepsPrimaryActionsReachable() {
        composeRule.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 2f)) {
                GroundedTheme {
                    DetailScreen(
                        earthquake = earthquake(),
                        relativeLocation = null,
                        isLoading = false,
                        onBack = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Share").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("USGS").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun coordinatesAddAnAccessibleLocationSectionWithoutRequiringMapsConfiguration() {
        composeRule.setContent {
            GroundedTheme {
                DetailScreen(
                    earthquake = earthquake().copy(coordinates = Coordinates(39.7, -104.9)),
                    relativeLocation = null,
                    isLoading = false,
                    onBack = {},
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithText("Event location").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("Map preview unavailable", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("39.7", substring = true).assertIsDisplayed()
    }

    @Test
    fun pullGestureRefreshesDetailsWithoutHidingTheCurrentEvent() {
        var refreshRequested = false
        composeRule.setContent {
            GroundedTheme {
                DetailScreen(
                    earthquake = earthquake(),
                    relativeLocation = null,
                    isLoading = false,
                    onBack = {},
                    onRefresh = { refreshRequested = true },
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithTag("detail-pull-refresh").performTouchInput { swipeDown() }

        composeRule.runOnIdle { assertTrue(refreshRequested) }
        composeRule.onNodeWithText("M 4.2").assertIsDisplayed()
    }

    @Test
    fun failedRefreshKeepsDetailsAndShowsSavedDataMessage() {
        composeRule.setContent {
            GroundedTheme {
                DetailScreen(
                    earthquake = earthquake(),
                    relativeLocation = null,
                    isLoading = false,
                    onBack = {},
                    refreshFailed = true,
                    mapsConfigured = false,
                )
            }
        }

        composeRule.onNodeWithText("Couldn’t update this earthquake", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("M 4.2").assertIsDisplayed()
    }

    private fun earthquake(): Earthquake {
        val now = Instant.parse("2026-09-13T20:00:00Z")
        return Earthquake(
            id = "test-event",
            magnitude = 4.2,
            magnitudeType = "mw",
            place = "A deliberately long earthquake location used to verify large text behavior",
            occurredAt = now,
            updatedAt = now,
            coordinates = null,
            depthKilometers = 12.3,
            detailUrl = "https://earthquake.usgs.gov/earthquakes/eventpage/test-event",
            feltReports = 42,
            significance = 300,
            alert = "green",
            tsunami = false,
            reviewStatus = "reviewed",
        )
    }
}
