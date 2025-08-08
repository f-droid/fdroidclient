package org.fdroid.ui.repositories

import androidx.core.os.LocaleListCompat
import org.fdroid.database.Repository
import org.fdroid.download.DownloadRequest
import org.fdroid.download.getDownloadRequest

data class RepositoryItem(
    val repoId: Long,
    val address: String,
    val name: String,
    val icon: DownloadRequest? = null,
    val timestamp: Long,
    val lastUpdated: Long?,
    val weight: Int,
    val enabled: Boolean,
) {
    constructor(repo: Repository, localeList: LocaleListCompat) : this(
        repoId = repo.repoId,
        address = repo.address,
        name = repo.getName(localeList) ?: "Unknown Repo",
        icon = repo.getIcon(localeList)?.getDownloadRequest(repo),
        timestamp = repo.timestamp,
        lastUpdated = repo.lastUpdated,
        weight = repo.weight,
        enabled = repo.enabled,
    )
}
