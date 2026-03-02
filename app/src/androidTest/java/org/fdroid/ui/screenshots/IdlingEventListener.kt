package org.fdroid.ui.screenshots

import androidx.compose.ui.test.IdlingResource
import coil3.EventListener
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult

class IdlingEventListener : IdlingResource, EventListener() {
  private val requests: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()
  override val isIdleNow: Boolean
    get() = requests.isEmpty()

  override fun onStart(request: ImageRequest) {
    requests.add(request.data.toString())
  }

  override fun onSuccess(request: ImageRequest, result: SuccessResult) {
    requests.remove(request.data.toString())
  }

  override fun onError(request: ImageRequest, result: ErrorResult) {
    requests.remove(request.data.toString())
  }

  override fun onCancel(request: ImageRequest) {
    requests.remove(request.data.toString())
  }
}
