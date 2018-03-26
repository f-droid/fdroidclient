package org.fdroid.fdroid.net;

import android.content.Context;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;

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
        Downloader downloader;
        if (localBroadcastManager == null) {
            localBroadcastManager = LocalBroadcastManager.getInstance(context);
        }

        if ("bluetooth".equalsIgnoreCase(url.getProtocol())) {
            String macAddress = url.getHost().replace("-", ":");
            downloader = new BluetoothDownloader(macAddress, url, destFile);
        } else {
            final String[] projection = {Schema.RepoTable.Cols.USERNAME, Schema.RepoTable.Cols.PASSWORD};
            Repo repo = RepoProvider.Helper.findByUrl(context, Uri.parse(url.toString()), projection);
            if (repo == null) {
                downloader = new HttpDownloader(url, destFile);
            } else {
                downloader = new HttpDownloader(url, destFile, repo.username, repo.password);
            }
        }
        return downloader;
    }
}
