package org.fdroid.repo

import android.net.Uri
import kotlinx.serialization.SerializationException
import org.fdroid.database.AppOverviewItem
import org.fdroid.database.Repository
import org.fdroid.download.NotFoundException
import org.fdroid.index.SigningException
import java.io.File
import java.io.IOException

internal fun interface RepoFetcher {
    /**
     * Fetches the repo from the given [uri] and posts updates to [receiver].
     * @return the temporary file the repo was written to.
     * Note that the OS may delete this at any time.
     */
    @Throws(
        IOException::class,
        SigningException::class,
        NotFoundException::class,
        SerializationException::class,
    )
    suspend fun fetchRepo(
        uri: Uri,
        repo: Repository,
        receiver: RepoPreviewReceiver,
        fingerprint: String?,
    ): File
}

internal interface RepoPreviewReceiver {
    fun onRepoReceived(repo: Repository)
    fun onAppReceived(app: AppOverviewItem)
}
