package org.fdroid.download

import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.cio.CIO

actual fun getHttpClientEngineFactory(): HttpClientEngineFactory<*> {
    return CIO
}
