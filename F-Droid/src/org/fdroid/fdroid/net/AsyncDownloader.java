package org.fdroid.fdroid.net;

import org.fdroid.fdroid.ProgressListener;

public interface AsyncDownloader {

    interface Listener extends ProgressListener {
        void onErrorDownloading(String localisedExceptionDetails);
        void onDownloadComplete();
        void onDownloadCancelled();
    }

    int getBytesRead();
    int getTotalBytes();
    void download();
    void attemptCancel(boolean userRequested);

}
