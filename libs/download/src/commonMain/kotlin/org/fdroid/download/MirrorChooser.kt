package org.fdroid.download

import io.ktor.client.plugins.ResponseException
import io.ktor.http.HttpStatusCode.Companion.Forbidden
import io.ktor.http.HttpStatusCode.Companion.NotFound
import io.ktor.http.Url
import kotlinx.io.IOException
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

  /** Executes the given request on the best mirror and tries the next best ones if that fails. */
  override suspend fun <T> mirrorRequest(
    downloadRequest: DownloadRequest,
    request: suspend (mirror: Mirror, url: Url) -> T,
  ): T {
    val mirrors =
      if (downloadRequest.proxy == null) {
        // keep ordered mirror list rather than reverting back to raw list from request
        val orderedMirrors = orderMirrors(downloadRequest)
        // if we don't use a proxy, filter out onion mirrors (won't work without Orbot)
        val filteredMirrors = orderedMirrors.filter { mirror -> !mirror.isOnion() }
        filteredMirrors.ifEmpty {
          // if we only have onion mirrors, take what we have and expect errors
          orderedMirrors
        }
      } else {
        orderMirrors(downloadRequest)
      }

    if (mirrors.isEmpty()) {
      error("No valid mirrors were found. Check settings.")
    }

    mirrors.forEachIndexed { index, mirror ->
      val ipfsCidV1 = downloadRequest.indexFile.ipfsCidV1
      val url =
        if (mirror.isIpfsGateway) {
          if (ipfsCidV1 == null) {
            val e = IOException("Got IPFS gateway without CID")
            handleException(e, mirror, index, mirrors.size)
            return@forEachIndexed
          } else mirror.getUrl(ipfsCidV1)
        } else {
          mirror.getUrl(downloadRequest.indexFile.name)
        }
      try {
        return executeRequest(mirror, url, request)
      } catch (e: ResponseException) {
        // don't try other mirrors if we got Forbidden response, but supplied credentials
        if (downloadRequest.hasCredentials && e.response.status == Forbidden) throw e
        // don't try other mirrors if we got NotFount response and downloaded a repo
        if (downloadRequest.tryFirstMirror != null && e.response.status == NotFound) throw e
        // also throw if this is the last mirror to try, otherwise try next
        handleException(e, mirror, index, mirrors.size)
      } catch (e: IOException) {
        handleException(e, mirror, index, mirrors.size)
      } catch (e: NoResumeException) {
        // continue to next mirror, if we need to resume, but this one doesn't support it
        handleException(e, mirror, index, mirrors.size)
      }
    }
    error("Reached code that was thought to be unreachable.")
  }

  open suspend fun <T> executeRequest(
    mirror: Mirror,
    url: Url,
    request: suspend (mirror: Mirror, url: Url) -> T,
  ): T {
    return request(mirror, url)
  }

  open fun handleException(e: Exception, mirror: Mirror, mirrorIndex: Int, mirrorCount: Int) {
    val wasLastMirror = mirrorIndex == mirrorCount - 1
    log.info {
      val info =
        if (e is ResponseException) e.response.status.toString() else e::class.simpleName ?: ""
      if (wasLastMirror) "Last mirror, rethrowing... ($info)"
      else "Trying other mirror now... ($info)"
    }
    if (wasLastMirror) throw e
  }
}

internal class MirrorChooserRandom : MirrorChooserImpl() {

  /** Returns a list of mirrors with the best mirrors first. */
  override fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror> {
    // simple random selection for now
    return downloadRequest.mirrors
      .toMutableList()
      .apply { shuffle() }
      .also { mirrors ->
        // respect the mirror to try first, if set
        if (downloadRequest.tryFirstMirror != null) {
          mirrors.sortBy { if (it == downloadRequest.tryFirstMirror) 0 else 1 }
        }
      }
  }
}

internal class MirrorChooserWithParameters(
  private val mirrorParameterManager: MirrorParameterManager? = null
) : MirrorChooserImpl() {

  override fun orderMirrors(downloadRequest: DownloadRequest): List<Mirror> {
    val errorComparator = Comparator { mirror1: Mirror, mirror2: Mirror ->
      // if no parameter manager is available, default to 0 (should return equal)
      val error1 = mirrorParameterManager?.getMirrorErrorCount(mirror1.baseUrl) ?: 0
      val error2 = mirrorParameterManager?.getMirrorErrorCount(mirror2.baseUrl) ?: 0

      // prefer mirrors with fewer errors
      error1.compareTo(error2)
    }

    val mirrorList: MutableList<Mirror> = mutableListOf<Mirror>()

    if (
      mirrorParameterManager != null && mirrorParameterManager.getCurrentLocation().isNotEmpty()
    ) {
      // if we have access to mirror parameters and the current location,
      // then use that information to sort the mirror list
      val mirrorFilteredList: List<Mirror> =
        sortMirrorsByLocation(
          mirrorParameterManager.preferForeignMirrors(),
          downloadRequest.mirrors,
          mirrorParameterManager.getCurrentLocation(),
          errorComparator,
        )
      mirrorList.addAll(mirrorFilteredList)
    } else {
      // shuffle initial list so all viable mirrors will be tried
      // then sort list to avoid mirrors that have caused errors
      val mirrorCompleteList: List<Mirror> =
        downloadRequest.mirrors.toMutableList().apply { shuffle() }.sortedWith(errorComparator)
      mirrorList.addAll(mirrorCompleteList)
    }

    // respect the mirror to try first, if set
    if (downloadRequest.tryFirstMirror != null) {
      mirrorList.sortBy { if (it == downloadRequest.tryFirstMirror) 0 else 1 }
    }

    return mirrorList
  }

  private fun sortMirrorsByLocation(
    foreignMirrorsPreferred: Boolean,
    availableMirrorList: List<Mirror>,
    currentLocation: String,
    mirrorComparator: Comparator<Mirror>,
  ): List<Mirror> {
    // shuffle initial list so all viable mirrors will be tried
    // then sort list to avoid mirrors that have caused errors
    val mirrorList: MutableList<Mirror> = mutableListOf<Mirror>()
    val sortedList: List<Mirror> =
      availableMirrorList.toMutableList().apply { shuffle() }.sortedWith(mirrorComparator)

    val domesticList: List<Mirror> =
      sortedList.filter { mirror ->
        !mirror.countryCode.isNullOrEmpty() && currentLocation == mirror.countryCode
      }
    val foreignList: List<Mirror> =
      sortedList.filter { mirror ->
        !mirror.countryCode.isNullOrEmpty() && currentLocation != mirror.countryCode
      }
    val unknownList: List<Mirror> =
      sortedList.filter { mirror -> mirror.countryCode.isNullOrEmpty() }

    if (foreignMirrorsPreferred) {
      mirrorList.addAll(foreignList)
      mirrorList.addAll(unknownList)
      mirrorList.addAll(domesticList)
    } else {
      mirrorList.addAll(domesticList)
      mirrorList.addAll(unknownList)
      mirrorList.addAll(foreignList)
    }
    return mirrorList
  }

  override suspend fun <T> executeRequest(
    mirror: Mirror,
    url: Url,
    request: suspend (mirror: Mirror, url: Url) -> T,
  ): T {
    return try {
      request(mirror, url)
    } catch (e: Exception) {
      // in case of an exception, potentially attempt a single retry
      if (mirrorParameterManager != null && mirrorParameterManager.shouldRetryRequest(url.host)) {
        request(mirror, url)
      } else {
        throw e
      }
    }
  }

  override fun handleException(e: Exception, mirror: Mirror, mirrorIndex: Int, mirrorCount: Int) {
    if (e is ResponseException || e is IOException) {
      mirrorParameterManager?.incrementMirrorErrorCount(mirror.baseUrl)
    }
    super.handleException(e, mirror, mirrorIndex, mirrorCount)
  }
}
