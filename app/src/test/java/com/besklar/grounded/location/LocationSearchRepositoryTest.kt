package com.besklar.grounded.location

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationSearchRepositoryTest {
    @Test
    fun `candidate normalizes locality and region into a stable scope`() {
        val result = candidate(locality = "Denver", adminArea = "Colorado").toSearchScope("80202")

        assertEquals("Denver, Colorado", result?.label)
        assertEquals(39.7, result?.center?.latitude ?: 0.0, 0.0)
        assertEquals(SearchScope.DEFAULT_RADIUS_KILOMETERS, result?.radiusKilometers ?: 0.0, 0.0)
    }

    @Test
    fun `postal code is used when locality is unavailable`() {
        val result = candidate(locality = null, adminArea = null, postalCode = "84045").toSearchScope("84045")

        assertEquals("84045", result?.label)
    }

    @Test
    fun `invalid coordinates are rejected`() {
        assertNull(candidate(latitude = 91.0).toSearchScope("Nowhere"))
        assertNull(candidate(hasCoordinates = false).toSearchScope("Nowhere"))
    }

    private fun candidate(
        hasCoordinates: Boolean = true,
        latitude: Double = 39.7,
        longitude: Double = -104.9,
        locality: String? = "Denver",
        adminArea: String? = "Colorado",
        postalCode: String? = null,
    ) = LocationSearchCandidate(
        hasCoordinates = hasCoordinates,
        latitude = latitude,
        longitude = longitude,
        locality = locality,
        adminArea = adminArea,
        postalCode = postalCode,
        featureName = null,
        countryName = "United States",
    )
}
