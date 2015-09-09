package org.fdroid.fdroid.net;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.fdroid.fdroid.ProgressListener;

import java.io.IOException;

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
