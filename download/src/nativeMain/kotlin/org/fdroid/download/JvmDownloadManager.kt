package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.curl.Curl

actual fun getHttpClientEngine(): HttpClientEngine {
    return Curl.create {
        // we could add special curl config options here
    }
}
