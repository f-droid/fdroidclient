package org.fdroid.fdroid.net;

import android.content.Context;
import android.os.Build;

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

    public static AsyncDownloader createAsync(Context context, String urlString, File destFile, String title, String id, AsyncDownloader.Listener listener) throws IOException {
        return createAsync(context, new URL(urlString), destFile, title, id, listener);
    }

    public static AsyncDownloader createAsync(Context context, URL url, File destFile, String title, String id, AsyncDownloader.Listener listener)
            throws IOException {
        if (canUseDownloadManager(url)) {
            return new AsyncDownloaderFromAndroid(context, listener, title, id, url.toString(), destFile);
        } else {
            return new AsyncDownloadWrapper(create(context, url, destFile), listener);
        }
    }

    static boolean isOnionAddress(URL url) {
        return url.getHost().endsWith(".onion");
    }

    /**
     * Tests to see if we can use Android's DownloadManager to download the APK, instead of
     * a downloader returned from DownloadFactory.
     */
    private static boolean canUseDownloadManager(URL url) {
        return Build.VERSION.SDK_INT > Build.VERSION_CODES.FROYO && !isOnionAddress(url);
    }

}
