package org.fdroid.fdroid.net;

import android.content.Context;
import android.net.Uri;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;

import java.io.File;
import java.io.IOException;

public class DownloaderFactory {

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
        if ("bluetooth".equals(scheme)) {
            downloader = new BluetoothDownloader(uri, destFile);
        } else {
            final String[] projection = {Schema.RepoTable.Cols.USERNAME, Schema.RepoTable.Cols.PASSWORD};
            Repo repo = RepoProvider.Helper.findByUrl(context, uri, projection);
            if (repo == null) {
                downloader = new HttpDownloader(uri, destFile);
            } else {
                downloader = new HttpDownloader(uri, destFile, repo.username, repo.password);
            }
        }
        return downloader;
    }
}
