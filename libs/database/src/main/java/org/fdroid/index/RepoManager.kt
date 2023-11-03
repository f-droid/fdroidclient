package org.fdroid.index

import android.content.Context
import android.net.Uri
import androidx.annotation.AnyThread
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.fdroid.database.AppPrefs
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryDaoInt
import org.fdroid.download.DownloaderFactory
import org.fdroid.download.HttpManager
import org.fdroid.repo.AddRepoState
import org.fdroid.repo.RepoAdder
import org.fdroid.repo.RepoUriGetter
import java.io.File
import java.net.Proxy
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
public class RepoManager @JvmOverloads constructor(
    context: Context,
    db: FDroidDatabase,
    downloaderFactory: DownloaderFactory,
    httpManager: HttpManager,
    private val repoUriBuilder: RepoUriBuilder = defaultRepoUriBuilder,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) {

    private val repositoryDao = db.getRepositoryDao() as RepositoryDaoInt
    private val tempFileProvider = TempFileProvider {
        File.createTempFile("dl-", "", context.cacheDir)
    }
    private val repoAdder = RepoAdder(
        context = context,
        db = db,
        tempFileProvider = tempFileProvider,
        downloaderFactory = downloaderFactory,
        httpManager = httpManager,
        repoUriBuilder = repoUriBuilder,
        coroutineContext = coroutineContext,
    )

    private val _repositoriesState: MutableStateFlow<List<Repository>> =
        MutableStateFlow(emptyList())
    public val repositoriesState: StateFlow<List<Repository>> = _repositoriesState.asStateFlow()
    public val liveRepositories: LiveData<List<Repository>> = _repositoriesState.asLiveData()

    public val addRepoState: StateFlow<AddRepoState> = repoAdder.addRepoState.asStateFlow()
    public val liveAddRepoState: LiveData<AddRepoState> = repoAdder.addRepoState.asLiveData()

    /**
     * Used internally as a mechanism to wait until repositories are loaded from the DB.
     * This happens quite fast and the load is triggered at construction time.
     * However, in some cases like when the app got killed and restarted by the OS,
     * code could try to access the [repositoriesState] before they've loaded.
     */
    private val repoCountDownLatch = CountDownLatch(1)

    init {
        // we need to load the repositories first off the UiThread, so it doesn't deadlock
        GlobalScope.launch(coroutineContext) {
            _repositoriesState.value = repositoryDao.getRepositories()
            repoCountDownLatch.countDown()
            withContext(Dispatchers.Main) {
                // keep observing the repos from the DB
                // and update internal cache when changes happen
                db.getRepositoryDao().getLiveRepositories().observeForever { repositories ->
                    _repositoriesState.value = repositories
                }
            }
        }
    }

    /**
     * This method will block the current thread in the rare case
     * that the repositories have not been loaded from the DB.
     */
    public fun getRepository(repoId: Long): Repository? {
        repoCountDownLatch.await()
        return repositoriesState.value.firstOrNull { repo -> repo.repoId == repoId }
    }

    /**
     * This method will block the current thread in the rare case
     * that the repositories have not been loaded from the DB.
     */
    public fun getRepositories(): List<Repository> {
        repoCountDownLatch.await()
        return repositoriesState.value
    }

    /**
     * Enables or disables the repository with the given [repoId].
     * Data from disabled repositories is ignored in many queries.
     */
    @WorkerThread
    public fun setRepositoryEnabled(repoId: Long, enabled: Boolean): Unit =
        repositoryDao.setRepositoryEnabled(repoId, enabled)

    /**
     * Removes a Repository with the given repoId with all associated data from the database.
     */
    @WorkerThread
    public fun deleteRepository(repoId: Long) {
        repositoryDao.deleteRepository(repoId)
        // while this gets updated automatically, getting the update may be slow,
        // so to speed up the UI, we emit the state change right away
        _repositoriesState.value = _repositoriesState.value.filter { repository ->
            repository.repoId == repoId
        }
    }

    /**
     * Fetches a preview of the repository at the given [url]
     * with the intention of possibly adding it to the database.
     * Progress can be observed via [addRepoState] or [liveAddRepoState].
     */
    @AnyThread
    @JvmOverloads
    public fun fetchRepositoryPreview(url: String, proxy: Proxy? = null) {
        repoAdder.fetchRepository(url, proxy)
    }

    /**
     * When [addRepoState] is in [org.fdroid.repo.Fetching.done],
     * you can call this to actually add the repo to the DB.
     * @throws IllegalStateException if [addRepoState] is currently in any other state.
     */
    @AnyThread
    public fun addFetchedRepository() {
        GlobalScope.launch(coroutineContext) {
            val addedRepo = repoAdder.addFetchedRepository()
            // if repo was added, update state right away, so it becomes available asap
            if (addedRepo != null) withContext(Dispatchers.Main) {
                _repositoriesState.value = _repositoriesState.value.toMutableList().apply {
                    add(addedRepo)
                }
            }
        }
    }

    /**
     * Aborts the process of fetching a [Repository] preview,
     * e.g. when the user leaves the UI flow or wants to cancel the preview process.
     * Note that this won't work after [addFetchedRepository] has already been called.
     */
    @UiThread
    public fun abortAddingRepository() {
        repoAdder.abortAddingRepo()
    }

    /**
     * Changes repository priorities that determine the order
     * they are returned from [getRepositories] and the preferred repositories.
     * The lower a repository is in the list, the lower is its priority.
     * If an app is in more than one repository, by default,
     * the repo higher in the list will provide metadata and updates.
     * Only setting [AppPrefs.preferredRepoId] overrides this.
     *
     * @param repoToReorder this repository will change its position in the list.
     * @param repoTarget the repository in which place the [repoToReorder] shall be moved.
     * If our list is [ A B C D ] and we call reorderRepositories(B, D),
     * then the new list will be [ A C D B ].
     * @throws IllegalArgumentException if one of the repos is an archive repo.
     * Those are expected to be tied to their main repo one down the list
     * and are moved automatically when their main repo moves.
     */
    @AnyThread
    public fun reorderRepositories(repoToReorder: Repository, repoTarget: Repository) {
        GlobalScope.launch(coroutineContext) {
            repositoryDao.reorderRepositories(repoToReorder, repoTarget)
        }
    }

    /**
     * Returns true if the given [uri] belongs to a swap repo.
     */
    @UiThread
    public fun isSwapUri(uri: Uri?): Boolean {
        return uri != null && RepoUriGetter.isSwapUri(uri)
    }

}
