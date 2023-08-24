package org.fdroid.repo

import android.content.res.Resources
import androidx.core.os.ConfigurationCompat.getLocales
import androidx.core.os.LocaleListCompat
import org.fdroid.LocaleChooser.getBestLocale
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.LocalizedIcon
import org.fdroid.database.Repository
import org.fdroid.database.RepositoryPreferences
import org.fdroid.database.toCoreRepository
import org.fdroid.database.toRepoAntiFeatures
import org.fdroid.database.toRepoCategories
import org.fdroid.database.toRepoReleaseChannel
import org.fdroid.index.IndexFormatVersion
import org.fdroid.index.v2.IndexV2StreamReceiver
import org.fdroid.index.v2.PackageV2
import org.fdroid.index.v2.RepoV2

internal open class RepoV2StreamReceiver(
    private val receiver: RepoPreviewReceiver,
    private val username: String?,
    private val password: String?,
) : IndexV2StreamReceiver {

    companion object {
        fun getRepository(
            repo: RepoV2,
            version: Long,
            formatVersion: IndexFormatVersion,
            certificate: String?,
            username: String?,
            password: String?,
        ) = Repository(
            repository = repo.toCoreRepository(
                version = version,
                formatVersion = formatVersion,
                certificate = certificate
            ),
            mirrors = emptyList(),
            antiFeatures = repo.antiFeatures.toRepoAntiFeatures(REPO_ID),
            categories = repo.categories.toRepoCategories(REPO_ID),
            releaseChannels = repo.releaseChannels.toRepoReleaseChannel(REPO_ID),
            preferences = RepositoryPreferences(
                repoId = REPO_ID,
                weight = 0,
                enabled = true,
                username = username,
                password = password,
            ),
        )

        fun getAppOverViewItem(
            packageName: String,
            p: PackageV2,
            locales: LocaleListCompat,
        ) = AppOverviewItem(
            repoId = REPO_ID,
            packageName = packageName,
            added = p.metadata.added,
            lastUpdated = p.metadata.lastUpdated,
            name = p.metadata.name.getBestLocale(locales),
            summary = p.metadata.summary.getBestLocale(locales),
            antiFeatures = p.versions.values.lastOrNull()?.antiFeatures,
            localizedIcon = p.metadata.icon?.map { (locale, file) ->
                LocalizedIcon(
                    repoId = 0L,
                    packageName = packageName,
                    type = "icon",
                    locale = locale,
                    name = file.name,
                    sha256 = file.sha256,
                    size = file.size,
                    ipfsCidV1 = file.ipfsCidV1,
                )
            },
        )
    }

    private val locales: LocaleListCompat = getLocales(Resources.getSystem().configuration)

    override fun receive(repo: RepoV2, version: Long, certificate: String) {
        receiver.onRepoReceived(
            getRepository(
                repo = repo,
                version = version,
                formatVersion = IndexFormatVersion.TWO,
                certificate = certificate,
                username = username,
                password = password,
            )
        )
    }

    override fun receive(packageName: String, p: PackageV2) {
        receiver.onAppReceived(getAppOverViewItem(packageName, p, locales))
    }

    override fun onStreamEnded() {
    }

}
