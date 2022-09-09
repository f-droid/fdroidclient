package org.fdroid.database

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import kotlinx.serialization.SerializationException
import org.fdroid.CompatibilityChecker
import org.fdroid.index.IndexFormatVersion.TWO
import org.fdroid.index.v2.FileV2
import org.fdroid.index.v2.IndexV2StreamReceiver
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2

/**
 * Receives a stream of IndexV2 data and stores it in the DB.
 *
 * Note: This should only be used once.
 * If you want to process a second stream, create a new instance.
 */
internal class DbV2StreamReceiver(
    private val db: FDroidDatabaseInt,
    private val repoId: Long,
    private val compatibilityChecker: CompatibilityChecker,
) : IndexV2StreamReceiver {

    private val locales: LocaleListCompat = getLocales(Resources.getSystem().configuration)
    private var clearedRepoData = false
    private val nonNullFileV2: (FileV2?) -> Unit = { fileV2 ->
        if (fileV2 != null) {
            if (fileV2.sha256 == null) throw SerializationException("${fileV2.name} has no sha256")
            if (fileV2.size == null) throw SerializationException("${fileV2.name} has no size")
            if (!fileV2.name.startsWith('/')) {
                throw SerializationException("${fileV2.name} does not start with /")
            }
        }
    }

    @Synchronized
    override fun receive(repo: RepoV2, version: Long, certificate: String) {
        repo.walkFiles(nonNullFileV2)
        clearRepoDataIfNeeded()
        db.getRepositoryDao().update(repoId, repo, version, TWO, certificate)
    }

    @Synchronized
    override fun receive(packageName: String, p: PackageV2) {
        p.walkFiles(nonNullFileV2)
        clearRepoDataIfNeeded()
        db.getAppDao().insert(repoId, packageName, p.metadata, locales)
        db.getVersionDao().insert(repoId, packageName, p.versions) {
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
