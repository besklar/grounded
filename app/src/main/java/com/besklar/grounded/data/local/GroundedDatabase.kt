package com.besklar.grounded.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EarthquakeEntity::class, SnapshotMetadataEntity::class],
    version = 1,
    exportSchema = true,
)
internal abstract class GroundedDatabase : RoomDatabase() {
    abstract fun earthquakeDao(): EarthquakeDao
}
