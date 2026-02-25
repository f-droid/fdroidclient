package org.fdroid.download

import javax.inject.Inject
import javax.inject.Singleton
import org.fdroid.ui.ipfs.IpfsManager

@Singleton
class DownloadRequestInterceptor @Inject constructor(private val ipfsManager: IpfsManager) {
  fun intercept(request: DownloadRequest): DownloadRequest {
    return if (request.indexFile.ipfsCidV1 != null && ipfsManager.enabled) {
      // add IPFS gateways to mirrors,
      // because have a CIDv1 and IPFS is enabled in preferences
      val newMirrors =
        request.mirrors.toMutableList().apply {
          val gatewayMirrors = ipfsManager.activeGateways.map { Mirror(it, null, true) }
          addAll(gatewayMirrors)
        }
      request.copy(mirrors = newMirrors)
    } else {
      request
    }
  }
}
