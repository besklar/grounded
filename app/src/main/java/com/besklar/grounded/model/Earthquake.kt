package com.besklar.grounded.model

import java.time.Instant

data class Coordinates(
    val latitude: Double,
    val longitude: Double,
)

data class Earthquake(
    val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String?,
    val occurredAt: Instant,
    val updatedAt: Instant,
    val coordinates: Coordinates?,
    val depthKilometers: Double?,
    val detailUrl: String?,
    val feltReports: Int?,
    val significance: Int?,
    val alert: String?,
    val tsunami: Boolean?,
    val reviewStatus: String?,
)
