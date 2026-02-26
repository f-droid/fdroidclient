package org.fdroid.download

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DownloadRequestInterceptor @Inject constructor() {
    fun intercept(request: DownloadRequest): DownloadRequest = request
}
