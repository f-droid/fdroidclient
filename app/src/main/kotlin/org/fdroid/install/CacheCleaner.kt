package org.fdroid.install

import android.content.Context
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import mu.KotlinLogging

private const val DELETE_OLDER_THAN_MILLIS = 8_640_000 // 24h

@Singleton
class CacheCleaner @Inject constructor(@param:ApplicationContext private val context: Context) {
  private val log = KotlinLogging.logger {}
  private val shaRegex = "^[a-zA-Z0-9]{64}$".toRegex()

  @WorkerThread
  fun clean() {
    log.info { "Cleaning up old files..," }
    try {
      context.cacheDir.listFiles()?.forEach { file ->
        if (file.isFile && shaRegex.matches(file.name) && file.isTooOld()) {
          log.debug { "Deleting ${file.name}..." }
          file.delete()
        }
      } ?: throw NullPointerException("listFiles() returned null")
    } catch (e: Exception) {
      log.error(e) { "Error deleting old cached files: " }
    }
  }

  private fun File.isTooOld(): Boolean {
    val age = System.currentTimeMillis() - lastModified()
    return age >= DELETE_OLDER_THAN_MILLIS
  }
}
