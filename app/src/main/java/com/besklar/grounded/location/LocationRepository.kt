package com.besklar.grounded.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.besklar.grounded.model.Coordinates
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

interface LocationRepository {
    val context: StateFlow<LocationContext>

    suspend fun loadApproximateLocation()

    fun recordDenial(permanently: Boolean)
}

@Singleton
internal class DefaultLocationRepository
@Inject
constructor(
    private val client: FusedLocationProviderClient,
    private val clock: Clock,
    @param:ApplicationContext private val applicationContext: Context,
) : LocationRepository {
    private val mutableContext = MutableStateFlow<LocationContext>(LocationContext.NotRequested)
    override val context: StateFlow<LocationContext> = mutableContext.asStateFlow()

    @SuppressLint("MissingPermission")
    override suspend fun loadApproximateLocation() {
        if (!applicationContext.getSystemService(LocationManager::class.java).isLocationEnabled) {
            mutableContext.value = LocationContext.ServicesDisabled
            return
        }
        mutableContext.value = LocationContext.Loading
        try {
            val current = client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
            val usable = current ?: client.lastLocation.await()?.takeIf(::isRecent)
            mutableContext.value = usable?.toContext() ?: LocationContext.Unavailable
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: SecurityException) {
            mutableContext.value = LocationContext.Denied(permanently = false)
        } catch (_: Exception) {
            mutableContext.value = LocationContext.Unavailable
        }
    }

    override fun recordDenial(permanently: Boolean) {
        mutableContext.value = LocationContext.Denied(permanently)
    }

    private fun isRecent(location: Location): Boolean = Duration.between(Instant.ofEpochMilli(location.time), clock.instant()).let {
        !it.isNegative && it <= Duration.ofMinutes(30)
    }

    private fun Location.toContext() = LocationContext.Available(
        coordinates = Coordinates(latitude = latitude, longitude = longitude),
        capturedAt = Instant.ofEpochMilli(time),
    )
}
