package com.besklar.grounded.data.remote

import com.besklar.grounded.model.Coordinates
import com.besklar.grounded.model.Earthquake
import java.time.Instant
import javax.inject.Inject

internal data class NormalizedFeed(
    val earthquakes: List<Earthquake>,
    val generatedAt: Instant?,
    val sourceUrl: String?,
    val attribution: String,
    val discardedCount: Int,
)

internal class InvalidFeedException(message: String) : IllegalArgumentException(message)

internal class UsgsFeedMapper
@Inject
constructor() {
    fun map(feed: UsgsFeatureCollectionDto): NormalizedFeed {
        val mapped = feed.features.mapNotNull(::mapFeature)
        if (feed.features.isNotEmpty() && mapped.isEmpty()) {
            throw InvalidFeedException("The USGS response contained no usable events")
        }

        val deduplicated = linkedMapOf<String, Earthquake>()
        mapped.forEach { candidate ->
            val existing = deduplicated[candidate.id]
            if (existing == null || candidate.updatedAt.isAfter(existing.updatedAt)) {
                deduplicated[candidate.id] = candidate
            }
        }

        return NormalizedFeed(
            earthquakes = deduplicated.values.sortedByDescending(Earthquake::occurredAt),
            generatedAt = feed.metadata?.generated?.let(Instant::ofEpochMilli),
            sourceUrl = feed.metadata?.url.clean(),
            attribution = feed.metadata?.title.clean() ?: "U.S. Geological Survey",
            discardedCount = feed.features.size - deduplicated.size,
        )
    }

    private fun mapFeature(feature: UsgsFeatureDto): Earthquake? {
        val id = feature.id.clean() ?: return null
        val properties = feature.properties ?: return null
        val occurredAt = properties.time?.let(Instant::ofEpochMilli) ?: return null
        val updatedAt = properties.updated?.let(Instant::ofEpochMilli) ?: occurredAt
        val rawCoordinates = feature.geometry?.coordinates
        val longitude = rawCoordinates?.getOrNull(0)
        val latitude = rawCoordinates?.getOrNull(1)
        val coordinates =
            if (
                latitude != null &&
                longitude != null &&
                latitude.isFinite() &&
                longitude.isFinite() &&
                latitude in -90.0..90.0 &&
                longitude in -180.0..180.0
            ) {
                Coordinates(latitude = latitude, longitude = longitude)
            } else {
                null
            }

        return Earthquake(
            id = id,
            magnitude = properties.mag?.takeIf(Double::isFinite),
            magnitudeType = properties.magnitudeType.clean(),
            place = properties.place.clean(),
            occurredAt = occurredAt,
            updatedAt = updatedAt,
            coordinates = coordinates,
            depthKilometers = rawCoordinates?.getOrNull(2)?.takeIf(Double::isFinite),
            detailUrl = properties.url.clean(),
            feltReports = properties.felt,
            significance = properties.sig,
            alert = properties.alert.clean(),
            tsunami = properties.tsunami?.let { it == 1 },
            reviewStatus = properties.status.clean(),
        )
    }
}

private fun String?.clean(): String? = this?.trim()?.takeIf(String::isNotEmpty)
