package org.fdroid.ui.search

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.text.input.TextFieldState
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.DownloadRequest
import org.fdroid.download.PackageName
import org.fdroid.download.getImageModel
import org.fdroid.index.RepoManager
import org.fdroid.install.InstalledAppsCache
import org.fdroid.settings.SettingsManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.ui.utils.normalize
import org.fdroid.utils.IoDispatcher
import java.text.Collator
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTimedValue

@Singleton
class SearchManager @Inject constructor(
    private val db: FDroidDatabase,
    private val repoManager: RepoManager,
    private val settingsManager: SettingsManager,
    private val installedAppsCache: InstalledAppsCache,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) {

    private val log = KotlinLogging.logger { }
    private val localeList = LocaleListCompat.getDefault()
    private val collator = Collator.getInstance(Locale.getDefault())
    private val _searchResults = MutableStateFlow<SearchResults?>(null)
    val textFieldState = TextFieldState()
    val searchResults = _searchResults.asStateFlow()

    private val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        categories.map { category ->
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
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

    fun onSearchCleared() {
        _searchResults.value = null
    }
}
