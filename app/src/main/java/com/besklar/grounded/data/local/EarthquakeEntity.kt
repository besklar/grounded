package com.besklar.grounded.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "earthquakes")
internal data class EarthquakeEntity(
    @PrimaryKey val id: String,
    val magnitude: Double?,
    val magnitudeType: String?,
    val place: String?,
    val occurredAtMillis: Long,
    val updatedAtMillis: Long,
    val latitude: Double?,
    val longitude: Double?,
    val depthKilometers: Double?,
    val detailUrl: String?,
    val feltReports: Int?,
    val significance: Int?,
    val alert: String?,
    val tsunami: Boolean?,
    val reviewStatus: String?,
)

@Entity(tableName = "snapshot_metadata")
internal data class SnapshotMetadataEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val queryWindow: String,
    val lastSuccessfulRetrievalMillis: Long,
    val responseGeneratedAtMillis: Long?,
    val sourceUrl: String?,
    val attribution: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
