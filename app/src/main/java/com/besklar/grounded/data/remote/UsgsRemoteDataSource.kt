package com.besklar.grounded.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import java.io.IOException
import javax.inject.Inject

internal sealed interface RemoteFeedResult {
    data class Success(val feed: NormalizedFeed) : RemoteFeedResult

    data class HttpFailure(val code: Int) : RemoteFeedResult

    data class TransportFailure(val cause: IOException) : RemoteFeedResult

    data class DecodingFailure(val cause: Throwable) : RemoteFeedResult
}

internal interface EarthquakeRemoteSource {
    suspend fun fetch(): RemoteFeedResult
}

internal class UsgsRemoteDataSource
@Inject
constructor(
    private val api: UsgsApi,
    private val mapper: UsgsFeedMapper,
) : EarthquakeRemoteSource {
    override suspend fun fetch(): RemoteFeedResult = try {
        val response = api.getPastWeek()
        val body = response.body()
        when {
            !response.isSuccessful -> RemoteFeedResult.HttpFailure(response.code())
            body == null -> RemoteFeedResult.DecodingFailure(InvalidFeedException("Empty response body"))
            else -> RemoteFeedResult.Success(mapper.map(body))
        }
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (transport: IOException) {
        RemoteFeedResult.TransportFailure(transport)
    } catch (decoding: SerializationException) {
        RemoteFeedResult.DecodingFailure(decoding)
    } catch (invalid: InvalidFeedException) {
        RemoteFeedResult.DecodingFailure(invalid)
    }
}
