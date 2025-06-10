package org.fdroid

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders.Range
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.runBlocking
import kotlinx.io.Buffer
import kotlinx.io.Source
import java.net.SocketTimeoutException
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.fail

fun getRandomString(length: Int = Random.nextInt(4, 16)): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun runSuspend(block: suspend () -> Unit) = runBlocking {
    block()
}

fun HttpRequestData.getByteRangeFrom(): Int {
    val (fromStr, endStr) = (headers[Range] ?: fail("No Range header"))
        .replace("bytes=", "")
        .split('-')
    assertEquals("", endStr)
    return fromStr.toIntOrNull() ?: fail("No valid content range ${headers[Range]}")
}

fun get(mockEngine: MockEngine) = object : HttpClientEngineFactory<MockEngineConfig> {
    override fun create(block: MockEngineConfig.() -> Unit): HttpClientEngine {
        return mockEngine
    }
}

internal fun getIndexFile(
    name: String,
    sha256: String? = null,
    size: Long? = null,
    ipfsCidV1: String? = null,
): IndexFile {
    return object : IndexFile {
        override val name: String = name
        override val sha256: String? = sha256
        override val size: Long? = size
        override val ipfsCidV1: String? = ipfsCidV1
        override fun serialize(): String = error("Not yet implemented")
    }
}

/**
 * This class isn't reliable and produces flaky tests.
 * It doesn't seem to be possible to mock failed HTTP downloads where partial data gets transferred.
 */
@Suppress("OVERRIDE_DEPRECATION", "OverridingDeprecatedMember", "DEPRECATION")
internal class TestByteReadChannel(private val buffer: Buffer) : ByteReadChannel {
    @InternalAPI
    override val readBuffer: Source = buffer
    override val closedCause: Throwable? = null
    override val isClosedForRead: Boolean
        get() {
            if (buffer.exhausted()) {
                throw SocketTimeoutException("boom!")
            }
            return false
        }

    override suspend fun awaitContent(min: Int): Boolean = true
    override fun cancel(cause: Throwable?) = error("Not yet implemented")
}
