package org.fdroid.download

import android.net.Uri
import androidx.core.net.toUri
import org.fdroid.IndexFile
import org.fdroid.database.Repository

fun IndexFile.getImageModel(repository: Repository?): Any? {
    if (repository == null) return null
    val address = repository.address
    if (address.startsWith("content://") || address.startsWith("file://")) {
        return getUri(address, this)
    }
    return DownloadRequest(
        indexFile = this,
        mirrors = repository.getMirrors(),
        proxy = null, // TODO proxy support
        username = repository.username,
        password = repository.password,
    )
}

fun getUri(repoAddress: String, indexFile: IndexFile): Uri {
    val pathElements = indexFile.name.split("/")
    if (repoAddress.startsWith("content://")) {
        // This is a hack that won't work with most ContentProviders
        // as they don't expose the path in the Uri.
        // However, it works for local file storage.
        val result = StringBuilder(repoAddress)
        for (element in pathElements) {
            result.append("%2F")
            result.append(element)
        }
        return result.toString().toUri()
    } else { // Normal URL
        val result = repoAddress.toUri().buildUpon()
        for (element in pathElements) {
            result.appendPath(element)
        }
        return result.build()
    }
}
