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
