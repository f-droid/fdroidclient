package org.fdroid.database

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import org.fdroid.CompatibilityChecker
import org.fdroid.index.v2.IndexStreamReceiver
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2

internal class DbStreamReceiver(
    private val db: FDroidDatabaseInt,
    private val compatibilityChecker: CompatibilityChecker,
) : IndexStreamReceiver {

    private val locales: LocaleListCompat = getLocales(Resources.getSystem().configuration)

    override fun receive(repoId: Long, repo: RepoV2, version: Int, certificate: String?) {
        db.getRepositoryDao().replace(repoId, repo, version, certificate)
    }

    override fun receive(repoId: Long, packageId: String, p: PackageV2) {
        db.getAppDao().insert(repoId, packageId, p.metadata, locales)
        db.getVersionDao().insert(repoId, packageId, p.versions) {
            compatibilityChecker.isCompatible(it.manifest)
        }
    }

    override fun onStreamEnded(repoId: Long) {
        db.afterUpdatingRepo(repoId)
    }

}
