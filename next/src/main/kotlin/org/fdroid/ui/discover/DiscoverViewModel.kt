package org.fdroid.ui.discover

import android.annotation.SuppressLint
import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asFlow
import androidx.lifecycle.viewModelScope
import app.cash.molecule.AndroidUiDispatcher
import app.cash.molecule.RecompositionMode.ContextClock
import app.cash.molecule.launchMolecule
import dagger.hilt.android.lifecycle.HiltViewModel
import io.ktor.client.engine.ProxyConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.NetworkMonitor
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.repo.RepoUpdateManager
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.utils.normalize
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import kotlin.time.measureTimedValue

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val app: Application,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    updatesManager: UpdatesManager,
    networkMonitor: NetworkMonitor,
    private val settingsManager: SettingsManager,
    private val repoManager: RepoManager,
    private val repoUpdateManager: RepoUpdateManager,
    private val installedAppsCache: InstalledAppsCache,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    private val moleculeScope =
        CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val collator = Collator.getInstance(Locale.getDefault())

    val numUpdates = updatesManager.numUpdates
    val hasAppIssues = updatesManager.appsWithIssues.map { !it.isNullOrEmpty() }
    private val newApps = db.getAppDao().getNewAppsFlow().map { list ->
        val proxyConfig = settingsManager.proxyConfig
        list.mapNotNull {
            val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
            it.toAppDiscoverItem(repository, proxyConfig)
        }
    }
    private val recentlyUpdatedApps = db.getAppDao().getRecentlyUpdatedAppsFlow().map { list ->
        val proxyConfig = settingsManager.proxyConfig
        list.mapNotNull {
            val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
            it.toAppDiscoverItem(repository, proxyConfig)
        }
    }
    private val mostDownloadedApps = MutableStateFlow<List<AppDiscoverItem>?>(null)
    private val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        categories.map { category ->
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
    private val searchResults = MutableStateFlow<SearchResults?>(null)
    private val hasRepoIssues = repoManager.repositoriesState.map { repos ->
        repos.any { it.errorCount >= 5 }
    }

    val localeList = LocaleListCompat.getDefault()
    val discoverModel: StateFlow<DiscoverModel> by lazy(LazyThreadSafetyMode.NONE) {
        @SuppressLint("StateFlowValueCalledInComposition") // see comment below
        moleculeScope.launchMolecule(mode = ContextClock) {
            DiscoverPresenter(
                newAppsFlow = newApps,
                recentlyUpdatedAppsFlow = recentlyUpdatedApps,
                mostDownloadedAppsFlow = mostDownloadedApps,
                categoriesFlow = categories,
                repositoriesFlow = repoManager.repositoriesState,
                searchResultsFlow = searchResults,
                isFirstStart = settingsManager.isFirstStart,
                // not observing the flow, but just taking the current value,
                // because we kick off repo updates from the UI depending on this state
                networkState = networkMonitor.networkState.value,
                repoUpdateStateFlow = repoUpdateManager.repoUpdateState,
                hasRepoIssuesFlow = hasRepoIssues,
            )
        }
    }

    init {
        loadMostDownloadedApps()
    }

    private fun loadMostDownloadedApps() {
        viewModelScope.launch(ioScope.coroutineContext) {
            val packageNames = try {
                app.assets.open("most_downloaded_apps.json").use { inputStream ->
                    @OptIn(ExperimentalSerializationApi::class)
                    Json.decodeFromStream<List<String>>(inputStream)
                }
            } catch (e: Exception) {
                log.error(e) { "Error loading most downloaded apps: " }
                return@launch
            }
            db.getAppDao().getAppsFlow(packageNames).collect { apps ->
                val proxyConfig = settingsManager.proxyConfig
                mostDownloadedApps.value = apps.mapNotNull {
                    val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
                    it.toAppDiscoverItem(repository, proxyConfig)
                }
            }
        }
    }

    suspend fun search(term: String) = withContext(ioScope.coroutineContext) {
        // we need a way to make the app crash for testing, e.g. the crash reporter
        if (term == "CrashMe") error("BOOOOOOOOM!!!")

        val sanitized = term.replace(Regex.fromLiteral("\""), "")
        val splits = sanitized.split(' ').filter { it.isNotBlank() }
        val query = splits.joinToString(" ") { word ->
            var isCjk = false
            // go through word and separate CJK chars (if needed)
            val newString = word.toList().joinToString("") {
                if (Character.isIdeographic(it.code)) {
                    isCjk = true
                    "$it* "
                } else "$it"
            }
            // add * to enable prefix matches
            if (isCjk) newString else "$newString*"
        }.let { firstPassQuery ->
            // if we had more than one word, make a more complex query
            if (splits.size > 1) {
                "$firstPassQuery " + // search* term* (implicit AND and prefix search)
                    "OR ${splits.joinToString("")}* " + // camel case prefix
                    "OR \"${splits.joinToString("* ")}*\"" // phrase query
            } else firstPassQuery
        }
        log.info { "Searching for: $query" }
        val timedApps = measureTimedValue {
            try {
                val proxyConfig = settingsManager.proxyConfig
                db.getAppDao().getAppSearchItems(query).sortedDescending().mapNotNull {
                    val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
                    val iconModel = it.getIcon(localeList)?.getImageModel(repository, proxyConfig)
                        as? DownloadRequest
                    val isInstalled = installedAppsCache.isInstalled(it.packageName)
                    AppListItem(
                        repoId = it.repoId,
                        packageName = it.packageName,
                        name = it.name.getBestLocale(localeList) ?: "Unknown",
                        summary = it.summary.getBestLocale(localeList) ?: "",
                        lastUpdated = it.lastUpdated,
                        isInstalled = isInstalled,
                        isCompatible = true, // doesn't matter here, as we don't filter
                        iconModel = if (isInstalled) {
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
            this@DiscoverViewModel.categories.first().filter {
                // normalization removed diacritics, so searches without them work
                it.name.normalize().contains(sanitized.normalize(), ignoreCase = true)
            }
        }
        searchResults.value = SearchResults(timedApps.value, timedCategories.value)
        log.debug {
            val numResults = searchResults.value?.apps?.size ?: 0
            "Search for $query had $numResults results " +
                "and took ${timedApps.duration} and ${timedCategories.duration}"
        }
    }

    fun onSearchCleared() {
        searchResults.value = null
    }

    private fun AppOverviewItem.toAppDiscoverItem(
        repository: Repository,
        proxyConfig: ProxyConfig?,
    ): AppDiscoverItem {
        val isInstalled = installedAppsCache.isInstalled(packageName)
        val imageModel =
            getIcon(localeList)?.getImageModel(repository, proxyConfig) as? DownloadRequest
        return AppDiscoverItem(
            packageName = packageName,
            name = getName(localeList) ?: "Unknown App",
            lastUpdated = lastUpdated,
            isInstalled = isInstalled,
            imageModel = if (isInstalled) {
                PackageName(packageName, imageModel)
            } else {
                imageModel
            },
        )
    }
}
