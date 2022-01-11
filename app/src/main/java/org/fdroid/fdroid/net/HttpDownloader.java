/*
 * Copyright (C) 2014-2017  Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2014-2018  Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2015-2016  Daniel Martí <mvdan@mvdan.cc>
 * Copyright (c) 2018  Senecto Limited
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.net;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.fdroid.download.DownloadRequest;
import org.fdroid.download.HeadInfo;
import org.fdroid.download.JvmDownloadManager;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * Download files over HTTP, with support for proxies, {@code .onion} addresses,
 * HTTP Basic Auth, etc.  This is not a full HTTP client!  This is only using
 * the bits of HTTP that F-Droid needs to operate.  It does not support things
 * like redirects or other HTTP tricks.  This keeps the security model and code
 * a lot simpler.
 */
public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";

    private final JvmDownloadManager downloadManager =
            new JvmDownloadManager(Utils.getUserAgent(), FDroidApp.queryString);
    private final String path;
    private final String username;
    private final String password;
    private final List<Mirror> mirrors;

    private boolean newFileAvailableOnServer;
    private long fileFullSize = -1L;

    HttpDownloader(String path, File destFile, List<Mirror> mirrors) {
        this(path, destFile, mirrors, null, null);
    }

    /**
     * Create a downloader that can authenticate via HTTP Basic Auth using the supplied
     * {@code username} and {@code password}.
     *
     * @param path     The path to the file to download
     * @param destFile Where the download is saved
     * @param mirrors  The repo base URLs where the file can be found
     * @param username Username for HTTP Basic Auth, use {@code null} to ignore
     * @param password Password for HTTP Basic Auth, use {@code null} to ignore
     */
    HttpDownloader(String path, File destFile, List<Mirror> mirrors, @Nullable String username,
                   @Nullable String password) {
        super(Uri.EMPTY, destFile);
        this.path = path;
        this.mirrors = mirrors;
        this.username = username;
        this.password = password;
    }

    @Override
    protected InputStream getDownloadersInputStream() throws IOException {
        DownloadRequest request = new DownloadRequest(path, mirrors, username, password);
        // TODO why do we need to wrap this in a BufferedInputStream here?
        return new BufferedInputStream(downloadManager.getBlocking(request));
    }

    /**
     * Get a remote file, checking the HTTP response code, if it has changed since
     * the last time a download was tried.
     * <p>
     * If the {@code ETag} does not match, it could be caused by the previous
     * download of the same file coming from a mirror running on a different
     * webserver, e.g. Apache vs Nginx.  {@code Content-Length} and
     * {@code Last-Modified} are used to check whether the file has changed since
     * those are more standardized than {@code ETag}.  Plus, Nginx and Apache 2.4
     * defaults use only those two values to generate the {@code ETag} anyway.
     * Unfortunately, other webservers and CDNs have totally different methods
     * for generating the {@code ETag}.  And mirrors that are syncing using a
     * method other than {@code rsync} could easily have different {@code Last-Modified}
     * times on the exact same file.  On top of that, some services like GitHub's
     * raw file support {@code raw.githubusercontent.com} and GitLab's raw file
     * support do not set the {@code Last-Modified} header at all.  So ultimately,
     * then {@code ETag} needs to be used first and foremost, then this calculated
     * {@code ETag} can serve as a common fallback.
     * <p>
     * In order to prevent the {@code ETag} from being used as a form of tracking
     * cookie, this code never sends the {@code ETag} to the server.  Instead, it
     * uses a {@code HEAD} request to get the {@code ETag} from the server, then
     * only issues a {@code GET} if the {@code ETag} has changed.
     * <p>
     * This uses a integer value for {@code Last-Modified} to avoid enabling the
     * use of that value as some kind of "cookieless cookie".  One second time
     * resolution should be plenty since these files change more on the time
     * space of minutes or hours.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/1708">update index from any available mirror</a>
     * @see <a href="http://lucb1e.com/rp/cookielesscookies">Cookieless cookies</a>
     */
    @Override
    public void download() throws IOException, InterruptedException {
        // boolean isSwap = isSwapUrl(sourceUrl);
        DownloadRequest request = new DownloadRequest(path, mirrors, username, password);
        HeadInfo headInfo = downloadManager.headBlocking(request, cacheTag);
        fileFullSize = headInfo.getContentLength() == null ? -1 : headInfo.getContentLength();
        if (!headInfo.getETagChanged()) {
            // ETag has not changed, don't download again
            Utils.debugLog(TAG, path + " cached, not downloading.");
            newFileAvailableOnServer = false;
            return;
        }
        newFileAvailableOnServer = true;

        boolean resumable = false;
        long fileLength = outputFile.length();
        if (fileLength > fileFullSize) {
            FileUtils.deleteQuietly(outputFile);
        } else if (fileLength == fileFullSize && outputFile.isFile()) {
            Utils.debugLog(TAG, "Already have outputFile, not download. " + outputFile.getAbsolutePath());
            return; // already have it!
        } else if (fileLength > 0) {
            resumable = true;
        }
        Utils.debugLog(TAG, "downloading " + path + " (is resumable: " + resumable + ")");
        downloadFromStream(resumable);
    }

    public static boolean isSwapUrl(Uri uri) {
        return isSwapUrl(uri.getHost(), uri.getPort());
    }

    static boolean isSwapUrl(URL url) {
        return isSwapUrl(url.getHost(), url.getPort());
    }

    static boolean isSwapUrl(String host, int port) {
        return port > 1023 // only root can use <= 1023, so never a swap repo
                && host.matches("[0-9.]+") // host must be an IP address
                && FDroidApp.subnetInfo.isInRange(host); // on the same subnet as we are
    }

    // Testing in the emulator for me, showed that figuring out the
    // filesize took about 1 to 1.5 seconds.
    // To put this in context, downloading a repo of:
    //  - 400k takes ~6 seconds
    //  - 5k   takes ~3 seconds
    // on my connection. I think the 1/1.5 seconds is worth it,
    // because as the repo grows, the tradeoff will
    // become more worth it.
    @Override
    @TargetApi(24)
    public long totalDownloadSize() {
        if (Build.VERSION.SDK_INT < 24) {
            return (int) fileFullSize;
        } else {
            return fileFullSize;
        }
    }

    @Override
    public boolean hasChanged() {
        return newFileAvailableOnServer;
    }

    @Override
    public void close() {
        // TODO abort ongoing download somehow
    }
}
