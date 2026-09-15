package com.besklar.grounded.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class UsgsRemoteDataSourceIntegrationTest {
    private val server = MockWebServer()

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `retrofit decodes and normalizes a real HTTP response`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(validFeed))

        val result = source().fetch() as RemoteFeedResult.Success

        assertEquals("event-1", result.feed.earthquakes.single().id)
        assertEquals(3.4, result.feed.earthquakes.single().magnitude!!, 0.0)
        assertEquals("/earthquakes/feed/v1.0/summary/all_week.geojson", server.takeRequest().path)
    }

    @Test
    fun `non-success response becomes a typed HTTP failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(503))

        val result = source().fetch()

        assertEquals(RemoteFeedResult.HttpFailure(503), result)
    }

    @Test
    fun `malformed JSON becomes a decoding failure`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{not-json"))

        assertTrue(source().fetch() is RemoteFeedResult.DecodingFailure)
    }

    private fun source(): UsgsRemoteDataSource {
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        return UsgsRemoteDataSource(retrofit.create(UsgsApi::class.java), UsgsFeedMapper())
    }

    private val validFeed =
        """
        {
          "metadata": {"generated": 1789339200000, "title": "USGS test feed"},
          "features": [
            {
              "id": "event-1",
              "properties": {
                "mag": 3.4,
                "place": "Test region",
                "time": 1789339100000,
                "updated": 1789339150000,
                "url": "https://earthquake.usgs.gov/earthquakes/eventpage/event-1",
                "magType": "ml"
              },
              "geometry": {"coordinates": [-122.0, 38.0, 7.2]}
            }
          ]
        }
        """.trimIndent()
}
