
package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

    public static Downloader create(String url, Context context)
            throws IOException {
        return new HttpDownloader(url, context);
    }

    public static Downloader create(String url, File destFile)
            throws IOException {
        return new HttpDownloader(url, destFile);
    }
}
