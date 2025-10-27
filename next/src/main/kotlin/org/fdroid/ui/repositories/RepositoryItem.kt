package org.fdroid.ui.repositories

import androidx.core.os.LocaleListCompat
import io.ktor.client.engine.ProxyConfig
import org.fdroid.database.Repository
import org.fdroid.download.getImageModel

data class RepositoryItem(
    val repoId: Long,
    val address: String,
    val name: String,
    val icon: Any? = null,
    val timestamp: Long,
    val lastUpdated: Long?,
    val weight: Int,
    val enabled: Boolean,
) {
    constructor(repo: Repository, localeList: LocaleListCompat, proxy: ProxyConfig?) : this(
        repoId = repo.repoId,
        address = repo.address,
        name = repo.getName(localeList) ?: "Unknown Repo",
        icon = repo.getIcon(localeList)?.getImageModel(repo, proxy),
        timestamp = repo.timestamp,
        lastUpdated = repo.lastUpdated,
        weight = repo.weight,
        enabled = repo.enabled,
    )
}
