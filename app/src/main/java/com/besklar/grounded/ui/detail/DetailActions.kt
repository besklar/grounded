package com.besklar.grounded.ui.detail

import com.besklar.grounded.model.Earthquake
import com.besklar.grounded.ui.home.EarthquakeFormatter
import java.time.ZoneId
import java.util.Locale

fun safeUsgsUrl(value: String?): String? {
    val uri = value?.let { runCatching { java.net.URI(it) }.getOrNull() } ?: return null
    val host = uri.host?.lowercase(Locale.ROOT) ?: return null
    return value.takeIf { uri.scheme == "https" && (host == "usgs.gov" || host.endsWith(".usgs.gov")) }
}

fun buildShareText(
    earthquake: Earthquake,
    locale: Locale,
    now: java.time.Instant,
    zoneId: ZoneId,
): String = buildList {
    add("Magnitude ${EarthquakeFormatter.magnitude(earthquake, locale)} earthquake")
    add(EarthquakeFormatter.place(earthquake, locale))
    add(EarthquakeFormatter.relativeTime(earthquake.occurredAt, now, locale, zoneId))
    safeUsgsUrl(earthquake.detailUrl)?.let(::add)
    add("Source: U.S. Geological Survey")
}.joinToString("\n")
