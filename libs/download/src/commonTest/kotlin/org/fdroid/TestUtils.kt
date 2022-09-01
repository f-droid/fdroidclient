package org.fdroid

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import kotlinx.coroutines.runBlocking
import kotlin.random.Random

fun getRandomString(length: Int = Random.nextInt(4, 16)): String {
    val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

fun runSuspend(block: suspend () -> Unit) = runBlocking {
    block()
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
