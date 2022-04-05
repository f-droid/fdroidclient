package org.fdroid.download

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import org.fdroid.get
import org.fdroid.getRandomString
import org.fdroid.runSuspend
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@Suppress("BlockingMethodInNonBlockingContext")
internal class HttpPosterTest {

    private val userAgent = getRandomString()

    @Test
    fun testPostSucceeds() = runSuspend {
        val body = """{ "foo": "bar" }"""
        val mockEngine = MockEngine { respondOk() }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpPoster = HttpPoster(httpManager, "http://example.org")
        httpPoster.post(body)

        assertEquals(1, mockEngine.requestHistory.size)
        mockEngine.requestHistory.forEach { request ->
            assertEquals(body, request.body.toByteArray().decodeToString())
        }
    }

    @Test
    fun testPostThrowsIOExceptionOnError() = runSuspend {
        val body = """{ "foo": "bar" }"""
        val mockEngine = MockEngine { respondError(BadRequest) }
        val httpManager = HttpManager(userAgent, null, httpClientEngineFactory = get(mockEngine))
        val httpPoster = HttpPoster(httpManager, "http://example.org")

        assertFailsWith<IOException> {
            httpPoster.post(body)
        }
        assertEquals(1, mockEngine.requestHistory.size)
    }
}
