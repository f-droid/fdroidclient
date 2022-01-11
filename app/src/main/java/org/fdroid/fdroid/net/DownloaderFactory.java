package org.fdroid.fdroid.net;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;

import org.fdroid.download.Mirror;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class DownloaderFactory {

    private static final String TAG = "DownloaderFactory";

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done.  It is stored in {@link Context#getCacheDir()} and starts
     * with the prefix {@code dl-}.
     */
    public static Downloader create(Context context, String urlString)
            throws IOException {
        File destFile = File.createTempFile("dl-", "", context.getCacheDir());
        destFile.deleteOnExit(); // this probably does nothing, but maybe...
        Uri uri = Uri.parse(urlString);
        return create(context, uri, destFile);
    }

    public static Downloader create(Context context, Uri uri, File destFile)
            throws IOException {
        Downloader downloader;

        String scheme = uri.getScheme();
        if (BluetoothDownloader.SCHEME.equals(scheme)) {
            downloader = new BluetoothDownloader(uri, destFile);
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            downloader = new TreeUriDownloader(uri, destFile);
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            downloader = new LocalFileDownloader(uri, destFile);
        } else {
            final String[] projection = {Schema.RepoTable.Cols.USERNAME, Schema.RepoTable.Cols.PASSWORD};
            Repo repo = RepoProvider.Helper.findByUrl(context, uri, projection);
            if (repo == null) {
                Utils.debugLog(TAG, "Warning: no repo found for " + uri);
                Mirror mirror = new Mirror(uri.toString());
                downloader = new HttpDownloader("", destFile, Collections.singletonList(mirror));
            } else {
                String urlSuffix = uri.toString().replace(repo.address, "");
                List<Mirror> mirrors = Mirror.fromStrings(repo.getMirrorList());
                Utils.debugLog(TAG, "Using suffix " + urlSuffix + " with mirrors " + mirrors);
                downloader =
                        new HttpDownloader(urlSuffix, destFile, mirrors, repo.username, repo.password);
            }
        }
        return downloader;
    }
}
