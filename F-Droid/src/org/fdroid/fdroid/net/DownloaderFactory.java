package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done
     */
    public static Downloader create(Context context, String url)
            throws IOException {
        File destFile = File.createTempFile("dl-", "", context.getCacheDir());
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(context, url, destFile);
        }
        return new HttpDownloader(context, url, destFile);
    }

    public static Downloader create(Context context, String url, File destFile)
            throws IOException {
        if (isOnionAddress(url)) {
            return new TorHttpDownloader(context, url, destFile);
        }
        return new HttpDownloader(context, url, destFile);
    }

    private static boolean isOnionAddress(String url) {
        return url.matches("^[a-zA-Z0-9]+://[^/]+\\.onion/.*");
    }
}
