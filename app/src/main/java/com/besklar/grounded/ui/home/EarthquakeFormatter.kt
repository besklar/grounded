package com.besklar.grounded.ui.home

import com.besklar.grounded.location.RelativeLocation
import com.besklar.grounded.model.Earthquake
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object EarthquakeFormatter {
    fun magnitude(earthquake: Earthquake, locale: Locale): String = earthquake.magnitude?.let { magnitudeValue(it, locale) } ?: "—"

    fun magnitudeValue(magnitude: Double, locale: Locale): String = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(magnitude)

    fun place(earthquake: Earthquake, locale: Locale): String = earthquake.place
        ?: earthquake.coordinates?.let {
            val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 }
            "${number.format(it.latitude)}, ${number.format(it.longitude)}"
        }
        ?: "Location unavailable"

    fun depth(earthquake: Earthquake, locale: Locale): String? = earthquake.depthKilometers?.let {
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }
        "${number.format(it)} km deep"
    }

    fun relativeLocation(relative: RelativeLocation, locale: Locale): String {
        val useMiles = usesMiles(locale)
        val value = if (useMiles) relative.distanceKilometers * 0.621371 else relative.distanceKilometers
        val unit = if (useMiles) "mi" else "km"
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        return "${number.format(value)} $unit ${relative.direction.label}"
    }

    fun distance(distanceKilometers: Double, locale: Locale): String {
        val useMiles = usesMiles(locale)
        val value = if (useMiles) distanceKilometers * 0.621371 else distanceKilometers
        val unit = if (useMiles) "mi" else "km"
        val number = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 0 }
        return "${number.format(value)} $unit"
    }

    fun relativeTime(
        occurredAt: Instant,
        now: Instant,
        locale: Locale,
        zoneId: ZoneId,
    ): String {
        val age = Duration.between(occurredAt, now).coerceAtLeast(Duration.ZERO)
        return when {
            age < Duration.ofMinutes(1) -> "Just now"
            age < Duration.ofHours(1) -> "${age.toMinutes()} min ago"
            age < Duration.ofHours(24) -> "${age.toHours()} hr ago"
            else ->
                DateFormat
                    .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
                    .apply { timeZone = java.util.TimeZone.getTimeZone(zoneId) }
                    .format(Date.from(occurredAt))
        }
    }

    private fun usesMiles(locale: Locale): Boolean = locale.country.uppercase(Locale.ROOT) in setOf("US", "LR", "MM")
}
