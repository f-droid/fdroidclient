package org.fdroid.download

import io.ktor.client.features.ResponseException
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Url

class MirrorChooser {

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
    internal suspend fun mirrorRequest(
        downloadRequest: DownloadRequest,
        request: suspend (url: Url) -> HttpResponse,
    ): HttpResponse {
        orderMirrors(downloadRequest.mirrors).forEachIndexed { index, mirror ->
            try {
                return request(mirror.getUrl(downloadRequest.path))
            } catch (e: ResponseException) {
                println(e)
                if (index == downloadRequest.mirrors.size - 1) throw e
            }
        }
        error("Reached code that was thought to be unreachable.")
    }

}
