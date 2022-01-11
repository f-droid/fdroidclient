package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun getHttpClientEngine(): HttpClientEngine {
    return CIO.create {
        // we could add special OkHttp config options here
    }
}
