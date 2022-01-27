package org.fdroid.fdroid.net;

import android.content.ContentResolver;
import android.net.Uri;

import androidx.annotation.Nullable;

import org.fdroid.download.DownloadRequest;
import org.fdroid.download.Downloader;
import org.fdroid.download.HttpDownloader;
import org.fdroid.download.HttpManager;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Repo;

import java.io.File;
import java.io.IOException;
import java.net.Proxy;
import java.util.List;

import info.guardianproject.netcipher.NetCipher;

public class DownloaderFactory {

    private static final String TAG = "DownloaderFactory";
    // TODO move to application object or inject where needed
    public static final HttpManager HTTP_MANAGER =
            new HttpManager(Utils.getUserAgent(), FDroidApp.queryString, NetCipher.getProxy());

    /**
     * Same as {@link #create(Repo, Uri, File)}, but trying canonical address first.
     * <p>
     * See https://gitlab.com/fdroid/fdroidclient/-/issues/1708 for why this is still needed.
     */
    public static Downloader createWithTryFirstMirror(Repo repo, Uri uri, File destFile)
            throws IOException {
        Mirror tryFirst = new Mirror(repo.address);
        List<Mirror> mirrors = Mirror.fromStrings(repo.getMirrorList());
        return create(repo, mirrors, uri, destFile, tryFirst);
    }

    public static Downloader create(Repo repo, Uri uri, File destFile) throws IOException {
        List<Mirror> mirrors = Mirror.fromStrings(repo.getMirrorList());
        return create(repo, mirrors, uri, destFile, null);
    }

    private static Downloader create(Repo repo, List<Mirror> mirrors, Uri uri, File destFile,
                                     @Nullable Mirror tryFirst) throws IOException {
        Downloader downloader;

        String scheme = uri.getScheme();
        if (BluetoothDownloader.SCHEME.equals(scheme)) {
            downloader = new BluetoothDownloader(uri, destFile);
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            downloader = new TreeUriDownloader(uri, destFile);
        } else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            downloader = new LocalFileDownloader(uri, destFile);
        } else {
            String path = uri.toString().replace(repo.address, "");
            Utils.debugLog(TAG, "Using suffix " + path + " with mirrors " + mirrors);
            Proxy proxy = NetCipher.getProxy();
            DownloadRequest request =
                    new DownloadRequest(path, mirrors, proxy, repo.username, repo.password, tryFirst);
            downloader = new HttpDownloader(HTTP_MANAGER, request, destFile);
        }
        return downloader;
    }
}
