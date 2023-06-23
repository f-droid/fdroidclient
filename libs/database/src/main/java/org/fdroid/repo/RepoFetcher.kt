package org.fdroid.repo

import android.net.Uri
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.Repository
import org.fdroid.download.NotFoundException
import org.fdroid.index.SigningException
import java.io.IOException

internal fun interface RepoFetcher {
    @Throws(IOException::class, SigningException::class, NotFoundException::class)
    suspend fun fetchRepo(
        uri: Uri,
        repo: Repository,
        receiver: RepoPreviewReceiver,
        fingerprint: String?,
    )
}

internal interface RepoPreviewReceiver {
    fun onRepoReceived(repo: Repository)
    fun onAppReceived(app: AppOverviewItem)
}
