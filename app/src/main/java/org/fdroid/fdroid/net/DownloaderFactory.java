package org.fdroid.fdroid.net;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

import org.apache.commons.io.FilenameUtils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class DownloaderFactory {

    private static LocalBroadcastManager localBroadcastManager;

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done.  It is stored in {@link Context#getCacheDir()} and starts
     * with the prefix {@code dl-}.
     */
    public static Downloader create(Context context, String urlString)
            throws IOException {
        File destFile = File.createTempFile("dl-", "", context.getCacheDir());
        destFile.deleteOnExit(); // this probably does nothing, but maybe...
        return create(context, urlString, destFile);
    }

    public static Downloader create(Context context, Uri uri, File destFile)
            throws IOException {
        return create(context, uri.toString(), destFile);
    }

    public static Downloader create(Context context, String urlString, File destFile)
            throws IOException {
        URL url = new URL(urlString);
        Downloader downloader = null;
        if (localBroadcastManager == null) {
            localBroadcastManager = LocalBroadcastManager.getInstance(context);
        }

        if (isBluetoothAddress(url)) {
            String macAddress = url.getHost().replace("-", ":");
            downloader = new BluetoothDownloader(macAddress, url, destFile);
        } else if (isLocalFile(url)) {
            downloader = new LocalFileDownloader(url, destFile);
        } else {
            final String[] projection = {RepoProvider.DataColumns.USERNAME, RepoProvider.DataColumns.PASSWORD};
            String repoUrlString = FilenameUtils.getBaseName(url.toString());
            Repo repo = RepoProvider.Helper.findByAddress(context, repoUrlString, projection);
            if (repo == null) {
                downloader = new HttpDownloader(url, destFile);
            } else {
                downloader = new HttpDownloader(url, destFile, repo.username, repo.password);
            }
        }
        return downloader;
    }

    private static boolean isBluetoothAddress(URL url) {
        return "bluetooth".equalsIgnoreCase(url.getProtocol());
    }

    private static boolean isLocalFile(URL url) {
        return "file".equalsIgnoreCase(url.getProtocol());
    }
}
