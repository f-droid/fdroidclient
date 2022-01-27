package org.fdroid.download

import io.ktor.client.features.ResponseException
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.Url
import mu.KotlinLogging

public interface MirrorChooser {
    public fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror>
    public suspend fun <T> mirrorRequest(
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
        val mirrors = if (downloadRequest.proxy == null) {
            // if we don't use a proxy, filter out onion mirrors (won't work without Orbot)
            val orderedMirrors =
                orderMirrors(downloadRequest).filter { mirror -> !mirror.isOnion() }
            // if we only have onion mirrors, take what we have and expect errors
            orderedMirrors.ifEmpty { downloadRequest.mirrors }
        } else {
            orderMirrors(downloadRequest)
        }
        mirrors.forEachIndexed { index, mirror ->
            val url = mirror.getUrl(downloadRequest.path)
            try {
                return request(mirror, url)
            } catch (e: ResponseException) {
                // don't try other mirrors if we got Forbidden response, but supplied credentials
                if (downloadRequest.hasCredentials && e.response.status == Forbidden) throw e
                // also throw if this is the last mirror to try, otherwise try next
                val wasLastMirror = index == downloadRequest.mirrors.size - 1
                log.warn(e) {
                    if (wasLastMirror) "Last mirror, rethrowing..."
                    else "Trying other mirror now..."
                }
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
        // simple random selection for now
        return downloadRequest.mirrors.toMutableList().apply { shuffle() }.also { mirrors ->
            // respect the mirror to try first, if set
            if (downloadRequest.tryFirstMirror != null) {
                mirrors.sortBy { if (it == downloadRequest.tryFirstMirror) 0 else 1 }
            }
        }
    }

}
