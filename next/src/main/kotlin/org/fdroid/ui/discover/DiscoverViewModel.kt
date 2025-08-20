package org.fdroid.ui.discover

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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.FDroidDatabase
import org.fdroid.download.getDownloadRequest
import org.fdroid.index.RepoManager
import org.fdroid.ui.categories.CategoryItem
import org.fdroid.ui.lists.AppListItem
import org.fdroid.updates.UpdatesManager
import org.fdroid.utils.IoDispatcher
import java.text.Collator
import java.text.Normalizer
import java.text.Normalizer.Form.NFKD
import java.util.Locale
import javax.inject.Inject
import kotlin.time.measureTimedValue

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    app: Application,
    savedStateHandle: SavedStateHandle,
    private val db: FDroidDatabase,
    updatesManager: UpdatesManager,
    private val repoManager: RepoManager,
    @param:IoDispatcher private val ioScope: CoroutineScope,
) : AndroidViewModel(app) {

    private val log = KotlinLogging.logger { }
    private val scope = CoroutineScope(viewModelScope.coroutineContext + AndroidUiDispatcher.Main)
    private val collator = Collator.getInstance(Locale.getDefault())
    private val normalizerRegex = "\\p{M}".toRegex()

    val numUpdates = updatesManager.numUpdates
    val apps = db.getAppDao().getAppOverviewItems().asFlow().map { list ->
        list.mapNotNull {
            val repository = repoManager.getRepository(it.repoId)
                ?: return@mapNotNull null
            AppDiscoverItem(
                packageName = it.packageName,
                name = it.name ?: "Unknown",
                isNew = it.lastUpdated == it.added,
                lastUpdated = it.lastUpdated,
                iconDownloadRequest = it.getIcon(localeList)
                    ?.getDownloadRequest(repository),
            )
        }
    }
    val categories = db.getRepositoryDao().getLiveCategories().asFlow().map { categories ->
        categories.map { category ->
            CategoryItem(
                id = category.id,
                name = category.getName(localeList) ?: "Unknown Category",
            )
        }.sortedWith { c1, c2 -> collator.compare(c1.name, c2.name) }
    }
    private val searchResults = MutableStateFlow<SearchResults?>(null)

    val localeList = LocaleListCompat.getDefault()
    val discoverModel: StateFlow<DiscoverModel> = scope.launchMolecule(mode = ContextClock) {
        DiscoverPresenter(
            appsFlow = apps,
            categoriesFlow = categories,
            repositoriesFlow = repoManager.repositoriesState,
            searchResultsFlow = searchResults,
        )
    }

    suspend fun search(term: String) = withContext(ioScope.coroutineContext) {
        val sanitized = term.replace(Regex.fromLiteral("\""), "")
        val query = sanitized.split(' ').joinToString(" ") { word ->
            if (word.isBlank()) "" else {
                var isCjk = false
                // go through word and separate CJK chars (if needed)
                val newString = word.toList().joinToString("") {
                    if (Character.isIdeographic(it.code)) {
                        isCjk = true
                        "$it* "
                    } else "$it"
                }
                if (isCjk) newString else "$newString*"
            }
        }
        log.info { "Searching for: $query" }
        val timedApps = measureTimedValue {
            try {
                db.getAppDao().getAppSearchItems(query).sortedDescending().mapNotNull {
                    val repository = repoManager.getRepository(it.repoId) ?: return@mapNotNull null
                    AppListItem(
                        repoId = it.repoId,
                        packageName = it.packageName,
                        name = it.name.getBestLocale(localeList) ?: "Unknown",
                        summary = it.summary.getBestLocale(localeList) ?: "",
                        lastUpdated = it.lastUpdated,
                        iconDownloadRequest = it.getIcon(localeList)
                            ?.getDownloadRequest(repository),
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

    /**
     * Normalizes the string by removing any diacritics that may appear.
     */
    private fun String.normalize(): String {
        if (Normalizer.isNormalized(this, NFKD)) return this
        return Normalizer.normalize(this, NFKD).replace(normalizerRegex, "")
    }
}
