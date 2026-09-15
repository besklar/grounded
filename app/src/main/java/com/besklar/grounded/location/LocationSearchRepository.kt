package com.besklar.grounded.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.besklar.grounded.model.Coordinates
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class SearchScope(
    val label: String,
    val center: Coordinates,
    val radiusKilometers: Double = DEFAULT_RADIUS_KILOMETERS,
) {
    companion object {
        const val DEFAULT_RADIUS_KILOMETERS = 805.0
    }
}

sealed interface LocationSearchResult {
    data class Success(val scope: SearchScope) : LocationSearchResult

    data object NotFound : LocationSearchResult

    data object ProviderUnavailable : LocationSearchResult

    data object Failed : LocationSearchResult
}

interface LocationSearchRepository {
    suspend fun search(query: String): LocationSearchResult
}

@Singleton
internal class SystemLocationSearchRepository
@Inject
constructor(
    @param:ApplicationContext private val context: Context,
) : LocationSearchRepository {
    override suspend fun search(query: String): LocationSearchResult {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) return LocationSearchResult.NotFound
        if (!Geocoder.isPresent()) return LocationSearchResult.ProviderUnavailable

        return try {
            val scope = geocode(normalizedQuery).firstNotNullOfOrNull { it.toCandidate().toSearchScope(normalizedQuery) }
            scope?.let(LocationSearchResult::Success) ?: LocationSearchResult.NotFound
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: IOException) {
            LocationSearchResult.Failed
        } catch (_: IllegalArgumentException) {
            LocationSearchResult.NotFound
        } catch (_: Exception) {
            LocationSearchResult.Failed
        }
    }

    private suspend fun geocode(query: String): List<Address> {
        val geocoder = Geocoder(context, Locale.getDefault())
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocationName(
                    query,
                    MAX_RESULTS,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses)
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(emptyList())
                        }
                    },
                )
            }
        } else {
            legacyGeocode(geocoder, query)
        }
    }

    @Suppress("DEPRECATION")
    private suspend fun legacyGeocode(geocoder: Geocoder, query: String): List<Address> = withContext(Dispatchers.IO) { geocoder.getFromLocationName(query, MAX_RESULTS).orEmpty() }

    private companion object {
        const val MAX_RESULTS = 5
    }
}

internal data class LocationSearchCandidate(
    val hasCoordinates: Boolean,
    val latitude: Double,
    val longitude: Double,
    val locality: String?,
    val adminArea: String?,
    val postalCode: String?,
    val featureName: String?,
    val countryName: String?,
)

private fun Address.toCandidate() = LocationSearchCandidate(
    hasCoordinates = hasLatitude() && hasLongitude(),
    latitude = latitude,
    longitude = longitude,
    locality = locality,
    adminArea = adminArea,
    postalCode = postalCode,
    featureName = featureName,
    countryName = countryName,
)

internal fun LocationSearchCandidate.toSearchScope(fallback: String): SearchScope? {
    if (!hasCoordinates || !latitude.isFinite() || !longitude.isFinite() || latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return null
    val label =
        listOfNotNull(locality, adminArea)
            .map(String::trim)
            .filter(String::isNotEmpty)
            .distinct()
            .joinToString(", ")
            .ifEmpty { listOfNotNull(postalCode, featureName, countryName).firstOrNull { it.isNotBlank() }?.trim() ?: fallback }
    return SearchScope(label = label, center = Coordinates(latitude, longitude))
}
