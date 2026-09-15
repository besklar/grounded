package com.besklar.grounded.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface EarthquakeDao {
    @Query("SELECT * FROM earthquakes ORDER BY occurredAtMillis DESC")
    suspend fun getEarthquakes(): List<EarthquakeEntity>

    @Query("SELECT * FROM snapshot_metadata WHERE id = 1")
    suspend fun getMetadata(): SnapshotMetadataEntity?

    @Query("DELETE FROM earthquakes")
    suspend fun deleteEarthquakes()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEarthquakes(events: List<EarthquakeEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMetadata(metadata: SnapshotMetadataEntity)
}
