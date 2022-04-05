package org.fdroid.download

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.curl.Curl

internal actual fun getHttpClientEngineFactory(): HttpClientEngineFactory<*> {
    return Curl
}
