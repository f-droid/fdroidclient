package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

    public static Downloader create(String url, Context context)
            throws IOException {
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, context);
        } else {
            return new HttpDownloader(url, context);
        }
    }

    public static Downloader create(String url, File destFile)
            throws IOException {
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, destFile);
        } else {
            return new HttpDownloader(url, destFile);
        }
    }

    private static boolean isOnionAddress(String url) {
        return url.matches("^[a-zA-Z0-9]+://[^/]+\\.onion/.*");
    }
}
