package org.fdroid.search

import android.database.sqlite.SQLiteException
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.SearchQueryRewriter
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.search.SearchHelper.normalize
import org.fdroid.ui.search.SearchResults
import org.fdroid.utils.IoDispatcher

/** The minimum amount of characters we start auto-searching for. */
const val SEARCH_THRESHOLD = 2

@Singleton
class SearchManager
@Inject
constructor(
  private val db: FDroidDatabase,
  private val repoManager: RepoManager,
  private val settingsManager: SettingsManager,
  private val installedAppsCache: InstalledAppsCache,
  private val searchHistoryManager: SearchHistoryManager,
  @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

  private val log = KotlinLogging.logger {}
  private val localeList = LocaleListCompat.getDefault()
  private val collator = Collator.getInstance(Locale.getDefault())
  private val _searchResults = MutableStateFlow<SearchResults?>(null)
  private val _savedSearches = MutableStateFlow<List<SavedSearch>?>(null)
  private val categories =
    db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
      categories
        .map { category ->
          CategoryItem(id = category.id, name = category.getName(localeList) ?: "Unknown Category")
        }
        .sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
  private var searchJob: SearchJob? = null

  val searchResults = _searchResults.asStateFlow()
  val savedSearches = _savedSearches.asStateFlow()

  init {
    // load saved searches on initialization
    CoroutineScope(ioDispatcher).launch {
      _savedSearches.value = searchHistoryManager.getSavedSearches()
    }
  }

  suspend fun search(term: String) {
    withContext(ioDispatcher) {
      // we need a way to make the app crash for testing, e.g. the crash reporter
      if (term == "CrashMe") error("BOOOOOOOOM!!!")

      val sanitized = term.replace(Regex.fromLiteral("\""), "")
      val query = SearchQueryRewriter.rewriteQuery(sanitized)
      log.info { "Searching for: $query" }
      val timedApps = measureTimedValue {
        try {
          val proxyConfig = settingsManager.proxyConfig
          db.getAppDao().getAppSearchItems(query).sortedDescending().mapNotNull {
            val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
            val iconModel =
              it.getIcon(localeList)?.getImageModel(repository, proxyConfig) as? DownloadRequest
            val isInstalled = installedAppsCache.isInstalled(it.packageName)
            AppListItem(
              repoId = it.repoId,
              packageName = it.packageName,
              name = it.name.getBestLocale(localeList) ?: "Unknown",
              summary = it.summary.getBestLocale(localeList) ?: "",
              lastUpdated = it.lastUpdated,
              isInstalled = isInstalled,
              isCompatible = true, // doesn't matter here, as we don't filter
              iconModel =
                if (isInstalled) {
                  PackageName(it.packageName, iconModel)
                } else {
                  iconModel
                },
              categoryIds = it.categories?.toSet(),
            )
          }
        } catch (e: SQLiteException) {
          log.error(e) { "Error searching for $query: " }
          emptyList()
        }
      }
      val timedCategories = measureTimedValue {
        categories.first().filter {
          // normalization removed diacritics, so searches without them work
          it.name.normalize().contains(sanitized.normalize(), ignoreCase = true)
        }
      }
      _searchResults.value = SearchResults(timedApps.value, timedCategories.value)
      log.debug {
        val numResults = _searchResults.value?.apps?.size ?: 0
        "Search for $query had $numResults results " +
          "and took ${timedApps.duration} and ${timedCategories.duration}"
      }
    }
    coroutineScope {
      // cancel previous search job if it's still running, so we don't save search as you type
      // incomplete queries
      searchJob?.job?.cancel()
      val job =
        launch(ioDispatcher) {
          // debounce, so we don't save every single search if the user is typing quickly
          delay(1500)
          _savedSearches.value = searchHistoryManager.saveSearchQuery(term)
        }
      searchJob = SearchJob(job, term)
    }
  }

  fun onSearchCleared() {
    _searchResults.value = null
  }

  suspend fun onClearSearchHistory() {
    withContext(ioDispatcher) {
      if (searchHistoryManager.clearAll()) _savedSearches.value = emptyList()
    }
  }
}

class SearchJob(val job: Job, val query: String)
