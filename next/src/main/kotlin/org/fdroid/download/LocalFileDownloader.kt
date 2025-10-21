package org.fdroid.download

import android.net.Uri
import org.fdroid.IndexFile
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * "Downloads" files from `file:///` [Uri]s.  Even though it is
 * obviously unnecessary to download a file that is locally available, this
 * class is here so that the whole security-sensitive installation process is
 * the same, no matter where the files are downloaded from.  Also, for things
 * like icons and graphics, it makes sense to have them copied to the cache so
 * that they are available even after removable storage is no longer present.
 */
class LocalFileDownloader(
    uri: Uri,
    indexFile: IndexFile,
    destFile: File,
) : Downloader(indexFile, destFile) {
    private val sourceFile: File = File(uri.path ?: error("Uri had no path"))

    override fun getInputStream(resumable: Boolean): InputStream = sourceFile.inputStream()

    override fun close() {}

    @Deprecated("Only for v1 repos")
    override fun hasChanged(): Boolean = true

    override fun totalDownloadSize(): Long = sourceFile.length()

    override fun download() {
        if (!sourceFile.exists()) {
            throw FileNotFoundException("$sourceFile does not exist")
        }
        var resumable = false
        val contentLength = sourceFile.length()
        val fileLength = outputFile.length()
        if (fileLength > contentLength) {
            outputFile.delete()
        } else if (fileLength == contentLength && outputFile.isFile()) {
            return // already have it!
        } else if (fileLength > 0) {
            resumable = true
        }
        downloadFromStream(resumable)
    }
}
