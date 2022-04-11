package org.fdroid.index.v1

import android.net.Uri
import org.fdroid.database.Repository

public enum class IndexUpdateResult {
    UNCHANGED,
    PROCESSED,
    NOT_FOUND,
}

public interface IndexUpdateListener {
    public fun onDownloadProgress(bytesRead: Long, totalBytes: Long)
    public fun onStartProcessing()
}

public class IndexUpdater

public fun Repository.getCanonicalUri(): Uri = Uri.parse(address).buildUpon()
    .appendPath(SIGNED_FILE_NAME)
    .build()
