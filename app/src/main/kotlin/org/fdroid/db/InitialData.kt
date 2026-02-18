package org.fdroid.db

import org.fdroid.database.FDroidDatabase
import org.fdroid.database.FDroidFixture
import org.fdroid.repo.RepoPreLoader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InitialData @Inject constructor(
    private val repoPreLoader: RepoPreLoader,
) : FDroidFixture {
    override fun prePopulateDb(db: FDroidDatabase) {
        repoPreLoader.addPreloadedRepositories(db)
        // we are kicking off the initial update from the UI,
        // not here to account for metered connection
    }
}
