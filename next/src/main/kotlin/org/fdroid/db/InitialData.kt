package org.fdroid.db

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidFixture
import org.fdroid.repo.RepoPreLoader
import org.fdroid.repo.RepoUpdateWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitialData @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repoPreLoader: RepoPreLoader,
) : FDroidFixture {
    override fun prePopulateDb(db: FDroidDatabase) {
        repoPreLoader.addPreloadedRepositories(db)
        RepoUpdateWorker.updateNow(context)
    }
}
