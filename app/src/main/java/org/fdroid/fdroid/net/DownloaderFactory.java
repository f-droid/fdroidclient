package org.fdroid.fdroid.net;

import android.content.Context;
import android.content.Intent;
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
        return create(context, new URL(urlString), destFile);
    }

    public static Downloader create(Context context, URL url, File destFile)
            throws IOException {
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

        downloader.setListener(new Downloader.DownloaderProgressListener() {
            @Override
            public void sendProgress(URL sourceUrl, int bytesRead, int totalBytes) {
                Intent intent = new Intent(Downloader.LOCAL_ACTION_PROGRESS);
                intent.putExtra(Downloader.EXTRA_ADDRESS, sourceUrl.toString());
                intent.putExtra(Downloader.EXTRA_BYTES_READ, bytesRead);
                intent.putExtra(Downloader.EXTRA_TOTAL_BYTES, totalBytes);
                localBroadcastManager.sendBroadcast(intent);
            }
        });
        return downloader;
    }

    private static boolean isBluetoothAddress(URL url) {
        return "bluetooth".equalsIgnoreCase(url.getProtocol());
    }

    private static boolean isLocalFile(URL url) {
        return "file".equalsIgnoreCase(url.getProtocol());
    }

    public static AsyncDownloader createAsync(Context context, String urlString, File destFile, AsyncDownloader.Listener listener)
            throws IOException {
        URL url = new URL(urlString);
        return new AsyncDownloadWrapper(create(context, url, destFile), listener);
    }
}
