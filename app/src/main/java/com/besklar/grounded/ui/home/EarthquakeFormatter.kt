package com.besklar.grounded.ui.home

import com.besklar.grounded.model.Earthquake
import java.text.DateFormat
import java.text.NumberFormat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Date
import java.util.Locale

object EarthquakeFormatter {
    fun magnitude(earthquake: Earthquake, locale: Locale): String = earthquake.magnitude?.let { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(it) } ?: "—"

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
}
