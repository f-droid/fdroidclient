package org.fdroid.download

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun getHttpClientEngine(): HttpClientEngine {
    return OkHttp.create {
        // we could add special OkHttp config options here
    }
}
