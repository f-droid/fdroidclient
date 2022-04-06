package org.fdroid.database

import org.fdroid.CompatibilityChecker
import org.fdroid.index.v1.IndexV1StreamReceiver
import org.fdroid.index.v2.AntiFeatureV2
import org.fdroid.index.v2.CategoryV2
import org.fdroid.index.v2.MetadataV2
import org.fdroid.index.v2.PackageVersionV2
import org.fdroid.index.v2.ReleaseChannelV2
import org.fdroid.index.v2.RepoV2

internal class DbV1StreamReceiver(
    private val db: FDroidDatabaseInt,
    private val compatibilityChecker: CompatibilityChecker,
) : IndexV1StreamReceiver {

    override fun receive(repoId: Long, repo: RepoV2, certificate: String?) {
        db.getRepositoryDao().replace(repoId, repo, certificate)
    }

    override fun receive(repoId: Long, packageId: String, m: MetadataV2) {
        db.getAppDao().insert(repoId, packageId, m)
    }

    override fun receive(repoId: Long, packageId: String, v: Map<String, PackageVersionV2>) {
        db.getVersionDao().insert(repoId, packageId, v) {
            compatibilityChecker.isCompatible(it.manifest)
        }
    }

    override fun updateRepo(
        repoId: Long,
        antiFeatures: Map<String, AntiFeatureV2>,
        categories: Map<String, CategoryV2>,
        releaseChannels: Map<String, ReleaseChannelV2>,
    ) {
        val repoDao = db.getRepositoryDao()
        repoDao.insertAntiFeatures(antiFeatures.toRepoAntiFeatures(repoId))
        repoDao.insertCategories(categories.toRepoCategories(repoId))
        repoDao.insertReleaseChannels(releaseChannels.toRepoReleaseChannel(repoId))

        db.getAppDao().updateCompatibility(repoId)
    }

    override fun updateAppMetadata(repoId: Long, packageId: String, preferredSigner: String?) {
        db.getAppDao().updatePreferredSigner(repoId, packageId, preferredSigner)
    }

}
