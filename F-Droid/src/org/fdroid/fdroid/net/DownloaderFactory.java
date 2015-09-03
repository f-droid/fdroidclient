package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class DownloaderFactory {

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done.  It is stored in {@link Context#getCacheDir()} and starts
     * with the prefix {@code dl-}.
     */
    public static Downloader create(Context context, String urlString)
            throws IOException {
        return create(context, new URL(urlString));
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done.  It is stored in {@link Context#getCacheDir()} and starts
     * with the prefix {@code dl-}.
     */
    public static Downloader create(Context context, URL url)
            throws IOException {
        File destFile = File.createTempFile("dl-", "", context.getCacheDir());
        destFile.deleteOnExit(); // this probably does nothing, but maybe...
        return create(context, url, destFile);
    }

    public static Downloader create(Context context, String urlString, File destFile)
            throws IOException {
        return create(context, new URL(urlString), destFile);
    }

    public static Downloader create(Context context, URL url, File destFile)
            throws IOException {
        if (isBluetoothAddress(url)) {
            String macAddress = url.getHost().replace("-", ":");
            return new BluetoothDownloader(context, macAddress, url, destFile);
        } else if (isOnionAddress(url)) {
            return new TorHttpDownloader(context, url, destFile);
        } else {
            return new HttpDownloader(context, url, destFile);
        }
    }

    private static boolean isBluetoothAddress(URL url) {
        return "bluetooth".equalsIgnoreCase(url.getProtocol());
    }

    static boolean isOnionAddress(URL url) {
        return url.getHost().endsWith(".onion");
    }
}
