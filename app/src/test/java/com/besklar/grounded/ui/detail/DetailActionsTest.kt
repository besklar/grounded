package com.besklar.grounded.ui.detail

import com.besklar.grounded.model.Earthquake
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.util.Locale

class DetailActionsTest {
    private val now = Instant.parse("2026-09-13T20:00:00Z")

    @Test
    fun `only accepts https USGS links`() {
        assertNotNull(safeUsgsUrl("https://earthquake.usgs.gov/earthquakes/eventpage/test"))
        assertNull(safeUsgsUrl("http://earthquake.usgs.gov/test"))
        assertNull(safeUsgsUrl("https://example.com/test"))
    }

    @Test
    fun `share text is accurate without a url`() {
        val text = buildShareText(earthquake(null), Locale.US, now, ZoneOffset.UTC)
        assertTrue(text.contains("Magnitude 3.4"))
        assertTrue(text.contains("Test place"))
        assertFalse(text.contains("null"))
        assertTrue(text.contains("U.S. Geological Survey"))
    }

    private fun earthquake(url: String?) = Earthquake("id", 3.4, "ml", "Test place", now, now, null, 2.0, url, null, null, null, null, null)
}
