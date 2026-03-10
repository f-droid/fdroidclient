package org.fdroid.install

import android.content.Context
import android.graphics.Bitmap
import androidx.core.graphics.drawable.toBitmap
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.ImageRequest
import coil3.size.Size
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.getCacheKey

@Singleton
class InstallIconResolver
@Inject
constructor(@param:ApplicationContext private val context: Context) {

  /**
   * Gets icon for preapproval from memory cache. In the unlikely event that the icon is not in
   * cache, downloads it for [DownloadRequest] models.
   */
  suspend fun resolve(iconModel: Any?): Bitmap? {
    return when (iconModel) {
      is DownloadRequest -> {
        val memoryCache = SingletonImageLoader.get(context).memoryCache
        val key = iconModel.getCacheKey()
        memoryCache?.get(MemoryCache.Key(key))?.image?.toBitmap()
          ?: run {
            val request = ImageRequest.Builder(context).data(iconModel).size(Size.ORIGINAL).build()
            SingletonImageLoader.get(context).execute(request).image?.toBitmap()
          }
      }
      is PackageName -> {
        context.packageManager.getApplicationIcon(iconModel.packageName).toBitmap()
      }
      else -> null
    }
  }
}

