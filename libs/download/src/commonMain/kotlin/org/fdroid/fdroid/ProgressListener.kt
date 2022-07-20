package org.fdroid.fdroid

/**
 * This is meant only to send download progress for any URL (e.g. index
 * updates, APKs, etc). This also keeps this class pure Java so that classes
 * that use `ProgressListener` can be tested on the JVM, without requiring
 * an Android device or emulator.
 *
 *
 * The full URL of a download is used as the unique identifier throughout
 * F-Droid.  I can take a few forms:
 *
 *  * [URL] instances
 *  * [android.net.Uri] instances
 *  * `String` instances, i.e. [URL.toString]
 *  * `int`s, i.e. [String.hashCode]
 *
 */
public fun interface ProgressListener {
    public fun onProgress(bytesRead: Long, totalBytes: Long)
}
