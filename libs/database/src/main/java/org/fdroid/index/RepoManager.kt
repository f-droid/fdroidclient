package org.fdroid.index

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
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.Repository
import java.util.concurrent.CountDownLatch
import kotlin.coroutines.CoroutineContext

@OptIn(DelicateCoroutinesApi::class)
public class RepoManager @JvmOverloads constructor(
    db: FDroidDatabase,
    private val coroutineContext: CoroutineContext = Dispatchers.IO,
) {

    private val repositoryDao = db.getRepositoryDao()

    private val _repositoriesState: MutableStateFlow<List<Repository>> =
        MutableStateFlow(emptyList())
    public val repositoriesState: StateFlow<List<Repository>> = _repositoriesState.asStateFlow()

    public val liveRepositories: LiveData<List<Repository>> = _repositoriesState.asLiveData()

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

}
