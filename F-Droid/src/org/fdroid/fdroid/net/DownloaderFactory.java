package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done
     */
    public static Downloader create(String url, Context context)
            throws IOException {
        File destFile = File.createTempFile("dl-", "", context.getCacheDir());
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, destFile);
        }
        return new HttpDownloader(url, destFile);
    }

    public static Downloader create(String url, File destFile)
            throws IOException {
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, destFile);
        }
        return new HttpDownloader(url, destFile);
    }

    private static boolean isOnionAddress(String url) {
        return url.matches("^[a-zA-Z0-9]+://[^/]+\\.onion/.*");
    }
}
