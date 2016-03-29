package org.fdroid.fdroid.net;

import org.fdroid.fdroid.ProgressListener;

public interface AsyncDownloader {

    interface Listener extends ProgressListener {
        void onErrorDownloading();

        void onDownloadComplete();
    }

    int getBytesRead();

    int getTotalBytes();

    void download();

    void attemptCancel(boolean userRequested);

}
