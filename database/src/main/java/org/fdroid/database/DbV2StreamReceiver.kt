package org.fdroid.database

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import org.fdroid.CompatibilityChecker
import org.fdroid.index.v2.IndexV2StreamReceiver
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2

internal class DbV2StreamReceiver(
    private val db: FDroidDatabaseInt,
    private val compatibilityChecker: CompatibilityChecker,
    private val repoId: Long,
) : IndexV2StreamReceiver {

    private val locales: LocaleListCompat = getLocales(Resources.getSystem().configuration)
    private var clearedRepoData = false

    @Synchronized
    override fun receive(repo: RepoV2, version: Int, certificate: String?) {
        clearRepoDataIfNeeded()
        db.getRepositoryDao().update(repoId, repo, version, certificate)
    }

    @Synchronized
    override fun receive(packageId: String, p: PackageV2) {
        clearRepoDataIfNeeded()
        db.getAppDao().insert(repoId, packageId, p.metadata, locales)
        db.getVersionDao().insert(repoId, packageId, p.versions) {
            compatibilityChecker.isCompatible(it.manifest)
        }
    }

    @Synchronized
    override fun onStreamEnded() {
        db.afterUpdatingRepo(repoId)
    }

    /**
     * As it is a valid index to receive packages before the repo,
     * we can not clear all repo data when receiving the repo,
     * but need to do it once at the beginning.
     */
    private fun clearRepoDataIfNeeded() {
        if (!clearedRepoData) {
            db.getRepositoryDao().clear(repoId)
            clearedRepoData = true
        }
    }

}
