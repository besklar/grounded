package com.besklar.grounded.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class UsgsFeatureCollectionDto(
    val metadata: UsgsMetadataDto? = null,
    val features: List<UsgsFeatureDto> = emptyList(),
)

@Serializable
internal data class UsgsMetadataDto(
    val generated: Long? = null,
    val url: String? = null,
    val title: String? = null,
    val count: Int? = null,
)

@Serializable
internal data class UsgsFeatureDto(
    val id: String? = null,
    val properties: UsgsPropertiesDto? = null,
    val geometry: UsgsGeometryDto? = null,
)

@Serializable
internal data class UsgsPropertiesDto(
    val mag: Double? = null,
    val place: String? = null,
    val time: Long? = null,
    val updated: Long? = null,
    val url: String? = null,
    val felt: Int? = null,
    val sig: Int? = null,
    val alert: String? = null,
    val tsunami: Int? = null,
    val status: String? = null,
    @SerialName("magType") val magnitudeType: String? = null,
)

@Serializable
internal data class UsgsGeometryDto(
    val coordinates: List<Double?>? = null,
)
