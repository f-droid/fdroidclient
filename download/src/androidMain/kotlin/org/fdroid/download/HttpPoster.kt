package org.fdroid.download

import io.ktor.client.features.ResponseException
import kotlinx.coroutines.runBlocking
import java.io.IOException

/**
 * HTTP POST a JSON string to the URL configured in the constructor.
 */
class HttpPoster(
    private val httpManager: HttpManager,
    private val url: String,
) {

    @Throws(IOException::class)
    fun post(json: String) {
        runBlocking {
            try {
                httpManager.post(url, json)
            } catch (e: ResponseException) {
                throw IOException(e)
            }
        }
    }

}
