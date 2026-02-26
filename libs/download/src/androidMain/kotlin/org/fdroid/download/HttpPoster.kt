package org.fdroid.download

import io.ktor.client.plugins.ResponseException
import java.io.IOException
import kotlinx.coroutines.runBlocking

/** HTTP POST a JSON string to the URL configured in the constructor. */
public class HttpPoster(private val httpManager: HttpManager, private val url: String) {

  @Throws(IOException::class)
  public fun post(json: String) {
    runBlocking {
      try {
        httpManager.post(url, json)
      } catch (e: ResponseException) {
        throw IOException(e)
      }
    }
  }
}
