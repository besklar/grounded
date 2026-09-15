package com.besklar.grounded.model

import java.time.Instant

data class EarthquakeSnapshot(
    val earthquakes: List<Earthquake>,
    val queryWindow: String,
    val lastSuccessfulRetrieval: Instant,
    val responseGeneratedAt: Instant?,
    val sourceUrl: String?,
    val attribution: String,
)
