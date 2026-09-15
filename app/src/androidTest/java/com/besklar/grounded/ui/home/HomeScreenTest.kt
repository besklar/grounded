package com.besklar.grounded.ui.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

    private fun snapshot(): EarthquakeSnapshot {
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
        return EarthquakeSnapshot(listOf(earthquake), "past_24_hours", now, now, null, "USGS")
    }
}
