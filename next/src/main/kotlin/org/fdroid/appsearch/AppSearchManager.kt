package org.fdroid.appsearch

import android.annotation.SuppressLint
import android.content.Context
import androidx.appsearch.app.AppSearchSession
import androidx.appsearch.app.PutDocumentsRequest
import androidx.appsearch.app.SearchSpec
import androidx.appsearch.app.SetSchemaRequest
import androidx.appsearch.localstorage.LocalStorage
import androidx.concurrent.futures.await
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.asFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import mu.KotlinLogging
import org.fdroid.database.FDroidDatabase
import org.fdroid.utils.IoDispatcher
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AppSearchDoc

@Singleton
class AppSearchManager @Inject constructor(
    @ApplicationContext val context: Context,
    @IoDispatcher val scope: CoroutineScope,
    val db: FDroidDatabase,
) {

    private val log = KotlinLogging.logger { }
    private val isInitialized: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private lateinit var session: AppSearchSession

    init {
        scope.launch {
            val session = LocalStorage.createSearchSessionAsync(
                LocalStorage.SearchContext.Builder(context, "fdroid")
                    .build()
            ).await()
            try {
                val setSchemaRequest = SetSchemaRequest.Builder()
                    .setForceOverride(true)
                    .addDocumentClasses(AppDocument::class.java, CategoryDocument::class.java)
                    .build()
                val response = session.setSchemaAsync(setSchemaRequest).await()
                response.migrationFailures.forEach { failure ->
                    log.warn { "Migration failure: $failure" }
                }
                response.incompatibleTypes.forEach { incompatible ->
                    log.warn { "Incompatible types: $incompatible" }
                }
                response.deletedTypes.forEach { deletedTypes ->
                    log.warn { "Deleted types: $deletedTypes" }
                }
                response.migratedTypes.forEach { migratedTypes ->
                    log.warn { "Migrated types: $migratedTypes" }
                }
                this@AppSearchManager.session = session
                isInitialized.value = true
                log.info { "Initialized." }
                awaitCancellation()
            } finally {
                log.info { "Closing session..." }
                session.close()
            }
        }
    }

    @SuppressLint("RequiresFeature") // we only use local search which supports this
    suspend fun search(s: String = "F-Droid"): List<AppSearchDoc> {
        awaitInitialization()
        val weights = mapOf(
            "name" to 100.0,
            "summary" to 50.0,
            "description" to 10.0,
            "packageName" to 5.0,
            "authorName" to 1.0,
        )
        // https://developer.android.com/reference/androidx/appsearch/app/SearchSpec.Builder#setRankingStrategy(java.lang.String)
        val now = System.currentTimeMillis()
        val weeksOldSpec = "($now - this.creationTimestamp()) / ${1000 * 60 * 60 * 24 * 7}"
        val searchSpec = SearchSpec.Builder()
            .setResultCountPerPage(200)
            .setSnippetCount(0)
            .setSnippetCountPerProperty(1)
            .addFilterProperties(
                AppDocument::class.java,
                listOf("name", "summary", "description", "packageName", "authorName"),
            )
            .setRankingStrategy("this.relevanceScore()*100-$weeksOldSpec")
            .setPropertyWeightsForDocumentClass(AppDocument::class.java, weights)
            .build()
        return session.search(s, searchSpec).use { results ->
            // we just use a single page for simplicity
            val resultList = results.nextPageAsync.await()
            resultList.map { r ->
                if (r.genericDocument.namespace == "app") r.getDocument(AppDocument::class.java)
                else r.getDocument(CategoryDocument::class.java)
            }
        }
    }

    /**
     * This removes all documents from the index and re-adds them back based on current DB state.
     * This is done to ensure that the index is in sync with what the DB has.
     * Typically, it should be called when
     * * repos got updated
     * * repos got disabled (it gets updated when enabled, so that is covered above)
     * * the preferred repo for an app has changed
     * * the device locales have changed
     */
    // TODO also call this when
    //  * repos get disabled
    //  * the preferred repo for an app was changed
    //  * the device locale was changed
    fun updateIndex() = scope.launch {
        awaitInitialization()

        log.debug { "Removing all documents..." }
        val removeSpec = SearchSpec.Builder().build()
        session.removeAsync("", removeSpec).await()

        log.debug { "Getting all apps..." }
        val localeList = LocaleListCompat.getDefault()
        // TODO check if it makes more sense to do this per-repo
        val docs = db.getAppDao().getAppSearchItems().map { app ->
            AppDocument(
                id = app.packageName,
                lastUpdated = app.lastUpdated,
                repoId = app.repoId,
                name = app.name,
                summary = app.summary,
                description = app.getDescription(localeList),
                packageName = app.packageName,
                authorName = app.authorName,
                icon = app.getIcon(localeList),
            )
        }
        log.debug { "Got ${docs.size} apps. Adding to appsearch.." }
        val putRequest = PutDocumentsRequest.Builder()
            .addDocuments(docs)
            .build()
        val result = session.putAsync(putRequest).await()
        if (!result.isSuccess) log.error {
            "Error putting documents: $result"
        }
        log.info { "Added ${result.successes.size} apps. Flushing to disk..." }
        session.requestFlushAsync()

        val categoryDocs = db.getRepositoryDao().getLiveCategories().asFlow().first().map {
            CategoryDocument(
                id = it.id,
                repoId = it.repoId,
                name = it.getName(localeList),
                description = it.getDescription(localeList),
                icon = it.getIcon(localeList),
            )
        }
        log.debug { "Got ${categoryDocs.size} categories. Adding to appsearch.." }
        val categoryPutRequest = PutDocumentsRequest.Builder()
            .addDocuments(categoryDocs)
            .build()
        val categoryResult = session.putAsync(categoryPutRequest).await()
        if (!categoryResult.isSuccess) log.error {
            "Error putting documents: $result"
        }
        log.info { "Added ${categoryResult.successes.size} categories. Flushing to disk..." }
        session.requestFlushAsync()
    }

    private suspend fun awaitInitialization() {
        if (!isInitialized.value) {
            isInitialized.first { it }
        }
    }

}
