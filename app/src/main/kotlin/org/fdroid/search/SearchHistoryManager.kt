package org.fdroid.search

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.annotation.WorkerThread
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging

private const val HISTORY_FILE = "search_history.json"
private const val MAX_SEARCHES = 10

@Singleton
class SearchHistoryManager(
  private val context: Context,
  private val maxNumSearches: Int = MAX_SEARCHES,
) {
  private val log = KotlinLogging.logger {}

  @Inject constructor(@ApplicationContext context: Context) : this(context, MAX_SEARCHES)

  @Synchronized
  @WorkerThread
  @OptIn(ExperimentalSerializationApi::class)
  fun getSavedSearches(): List<SavedSearch> {
    return try {
      context.openFileInput(HISTORY_FILE).use { inputStream ->
        Json.decodeFromStream<List<SavedSearch>>(inputStream).sortedByDescending { it.time }
      }
    } catch (e: Exception) {
      if (e !is FileNotFoundException) {
        log.error(e) { "Error getting saved searches: " }
        clearAll()
      }
      emptyList()
    }
  }

  /** Saved the given [query] and returns the updated list of saved searches. */
  @Synchronized
  @WorkerThread
  fun saveSearchQuery(query: String): List<SavedSearch> {
    log.info { "Saving search query \"$query\"" }
    // get existing searches, remove any with the same query
    val savedSearches = getSavedSearches().toMutableList()
    savedSearches.removeAll { it.query == query }
    // keep only the most recent maxNumSearches - 1 searches, so we can add the new one
    val searchesToSave =
      if (savedSearches.size > maxNumSearches - 1) {
        savedSearches.subList(0, maxNumSearches - 1)
      } else {
        savedSearches
      }
    // add the new search with the current time
    val savedSearch = SavedSearch(System.currentTimeMillis(), query)
    searchesToSave.add(savedSearch)
    // save searches to disk
    try {
      context.openFileOutput(HISTORY_FILE, MODE_PRIVATE).use { outputStream ->
        outputStream.write(Json.encodeToString(searchesToSave).encodeToByteArray())
      }
    } catch (e: Exception) {
      log.error(e) { "Error saving $savedSearch: " }
    }
    return searchesToSave.sortedByDescending { it.time }
  }

  @Synchronized
  @WorkerThread
  fun clearAll(): Boolean {
    return try {
      context.deleteFile(HISTORY_FILE)
    } catch (e: Exception) {
      log.error(e) { "Error deleting file: " }
      false
    }
  }
}

@Serializable data class SavedSearch(val time: Long, val query: String)
