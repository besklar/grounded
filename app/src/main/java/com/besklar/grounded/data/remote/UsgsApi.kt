package com.besklar.grounded.data.remote

import retrofit2.Response
import retrofit2.http.GET

internal interface UsgsApi {
    @GET("earthquakes/feed/v1.0/summary/all_week.geojson")
    suspend fun getPastWeek(): Response<UsgsFeatureCollectionDto>
}
