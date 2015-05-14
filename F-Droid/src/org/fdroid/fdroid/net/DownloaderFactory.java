package org.fdroid.fdroid.net;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

    public static Downloader create(String url, Context context) throws IOException {
        Uri uri = Uri.parse(url);
        if (isBluetoothAddress(uri)) {
            return new BluetoothDownloader(null, uri.getPath(), context);
        } else if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, context);
        } else {
            return new HttpDownloader(url, context);
        }
    }

    public static Downloader create(String url, File destFile) throws IOException {
        Uri uri = Uri.parse(url);
        if (isBluetoothAddress(uri)) {
            return new BluetoothDownloader(null, uri.getPath(), destFile);
        } else if (isOnionAddress(url)) {
            return new TorHttpDownloader(url, destFile);
        } else {
            return new HttpDownloader(url, destFile);
        }
    }

    private static boolean isBluetoothAddress(Uri uri) {
        return "bluetooth".equalsIgnoreCase(uri.getScheme());
    }

    private static boolean isOnionAddress(String url) {
        return url.matches("^[a-zA-Z0-9]+://[^/]+\\.onion/.*");
    }
}
