package org.fdroid.download

import io.ktor.client.features.ResponseException
import io.ktor.http.Url
import mu.KotlinLogging

interface MirrorChooser {
    fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror>
    suspend fun <T> mirrorRequest(
        downloadRequest: DownloadRequest,
        request: suspend (mirror: Mirror, url: Url) -> T,
    ): T
}

internal abstract class MirrorChooserImpl : MirrorChooser {

    companion object {
        protected val log = KotlinLogging.logger {}
    }

    /**
     * Executes the given request on the best mirror and tries the next best ones if that fails.
     */
    override suspend fun <T> mirrorRequest(
        downloadRequest: DownloadRequest,
        request: suspend (mirror: Mirror, url: Url) -> T,
    ): T {
        orderMirrors(downloadRequest).forEachIndexed { index, mirror ->
            val url = mirror.getUrl(downloadRequest.path)
            try {
                return request(mirror, url)
            } catch (e: ResponseException) {
                val wasLastMirror = index == downloadRequest.mirrors.size - 1
                log.warn(e) { if (wasLastMirror) "Last mirror, rethrowing..." else "Trying other mirror now..." }
                if (wasLastMirror) throw e
            }
        }
        error("Reached code that was thought to be unreachable.")
    }
}

internal class MirrorChooserRandom : MirrorChooserImpl() {

    /**
     * Returns a list of mirrors with the best mirrors first.
     */
    override fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror> {
        val mirrors = if (downloadRequest.proxy == null) {
            downloadRequest.mirrors.filter { mirror -> !mirror.isOnion() }.toMutableList()
        } else {
            downloadRequest.mirrors.toMutableList()
        }
        // simple random selection for now
        return mirrors.apply { shuffle() }
    }

}
