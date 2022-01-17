package org.fdroid.download

import io.ktor.client.features.ResponseException
import io.ktor.http.Url
import mu.KotlinLogging

class MirrorChooser {

    companion object {
        val log = KotlinLogging.logger {}
    }

    /**
     * Returns a list of mirrors with the best mirrors first.
     */
    private fun orderMirrors(mirrors: List<Mirror>): List<Mirror> {
        // simple random selection for now
        // TODO Filter-out onion mirrors for non-tor connections
        return mirrors.toMutableList().apply { shuffle() }
    }

    /**
     * Executes the given request on the best mirror and tries the next best ones if that fails.
     */
    internal suspend fun <T> mirrorRequest(
        downloadRequest: DownloadRequest,
        request: suspend (mirror: Mirror, url: Url) -> T,
    ): T {
        orderMirrors(downloadRequest.mirrors).forEachIndexed { index, mirror ->
            try {
                return request(mirror, mirror.getUrl(downloadRequest.path))
            } catch (e: ResponseException) {
                val wasLastMirror = index == downloadRequest.mirrors.size - 1
                log.warn(e) { if (wasLastMirror) "Last mirror, rethrowing..." else "Trying other mirror now..." }
                if (wasLastMirror) throw e
            }
        }
        error("Reached code that was thought to be unreachable.")
    }

}
