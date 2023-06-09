package org.fdroid.fdroid.net;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.os.Build;
import android.util.Log;

import androidx.core.util.Pair;

import org.fdroid.download.DownloadRequest;
import org.fdroid.download.HttpDownloader;
import org.fdroid.download.HttpManager;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.index.v1.IndexV1UpdaterKt;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class HttpDownloaderTest {
    private static final String TAG = "HttpDownloaderTest";

    private final HttpManager httpManager = new HttpManager(Utils.getUserAgent(), FDroidApp.queryString, null, true);
    private static final Collection<Pair<String, String>> URLS;

    // https://developer.android.com/reference/javax/net/ssl/SSLContext
    static {
        ArrayList<Pair<String, String>> tempUrls = new ArrayList<>(Arrays.asList(
                new Pair<>("https://f-droid.org/repo", IndexV1UpdaterKt.SIGNED_FILE_NAME),
                // sites that use SNI for HTTPS
                new Pair<>("https://mirrors.edge.kernel.org/", "debian/dists/stable/Release"),
                new Pair<>("https://fdroid.tetaneutral.net/fdroid/repo/", IndexV1UpdaterKt.SIGNED_FILE_NAME),
                new Pair<>("https://ftp.fau.de/fdroid/repo/", IndexV1UpdaterKt.SIGNED_FILE_NAME),
                new Pair<>("https://ftp.fau.de/fdroid/repo", "dev.lonami.klooni/en-US/phoneScreenshots/1-game.jpg"),
                //new Pair<>("https://microg.org/fdroid/repo/index-v1.jar"),
                //new Pair<>("https://grobox.de/fdroid/repo/index.jar"),
                new Pair<>("https://guardianproject.info/fdroid/repo", IndexV1UpdaterKt.SIGNED_FILE_NAME)
        ));
        if (Build.VERSION.SDK_INT >= 22) {
            tempUrls.addAll(Arrays.asList(
                    new Pair<>("https://en.wikipedia.org", "/wiki/Index.html"), // no SNI but weird ipv6 lookup issues
                    new Pair<>("https://mirror.cyberbits.eu/fdroid/repo/", IndexV1UpdaterKt.SIGNED_FILE_NAME)  // TLSv1.2 only and SNI
            ));
        }
        URLS = tempUrls;
    }

    private boolean receivedProgress;

    @Test
    public void downloadUninterruptedTest() throws IOException, InterruptedException {
        for (Pair<String, String> pair : URLS) {
            Log.i(TAG, "URL: " + pair.first + pair.second);
            File destFile = File.createTempFile("dl-", "");
            List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList(pair.first));
            DownloadRequest request = new DownloadRequest(pair.second, mirrors, null, null, null);
            HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
            httpDownloader.download();
            assertTrue(destFile.exists());
            assertTrue(destFile.canRead());
            destFile.deleteOnExit();
        }
    }

    @Test
    public void downloadUninterruptedTestWithProgress() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        String path = "index.jar";
        List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList("https://ftp.fau.de/fdroid/repo/"));
        receivedProgress = false;
        File destFile = File.createTempFile("dl-", "");
        final DownloadRequest request = new DownloadRequest(path, mirrors, null, null, null);
        final HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
        httpDownloader.setListener((bytesRead, totalBytes) -> receivedProgress = true);
        new Thread() {
            @Override
            public void run() {
                try {
                    httpDownloader.download();
                    latch.countDown();
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                    fail();
                }
            }
        }.start();
        latch.await(100, TimeUnit.SECONDS); // either 2 progress reports or 100 seconds
        assertTrue(destFile.exists());
        assertTrue(destFile.canRead());
        assertTrue(receivedProgress);
        destFile.deleteOnExit();
    }

    @Test
    public void downloadHttpBasicAuth() throws IOException, InterruptedException {
        String path = "myusername/supersecretpassword";
        List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList("https://httpbin.org/basic-auth/"));
        File destFile = File.createTempFile("dl-", "");
        final DownloadRequest request = new DownloadRequest(path, mirrors, null, "myusername", "supersecretpassword");
        HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
        httpDownloader.download();
        assertTrue(destFile.exists());
        assertTrue(destFile.canRead());
        destFile.deleteOnExit();
    }

    @Test(expected = IOException.class)
    public void downloadHttpBasicAuthWrongPassword() throws IOException, InterruptedException {
        String path = "myusername/supersecretpassword";
        List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList("https://httpbin.org/basic-auth/"));
        File destFile = File.createTempFile("dl-", "");
        final DownloadRequest request =
                new DownloadRequest(path, mirrors, null, "myusername", "wrongpassword");
        HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
        httpDownloader.download();
        assertFalse(destFile.exists());
        destFile.deleteOnExit();
    }

    @Test(expected = IOException.class)
    public void downloadHttpBasicAuthWrongUsername() throws IOException, InterruptedException {
        String path = "myusername/supersecretpassword";
        List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList("https://httpbin.org/basic-auth/"));
        File destFile = File.createTempFile("dl-", "");
        final DownloadRequest request =
                new DownloadRequest(path, mirrors, null, "wrongusername", "supersecretpassword");
        HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
        httpDownloader.download();
        assertFalse(destFile.exists());
        destFile.deleteOnExit();
    }

    @Test
    public void downloadThenCancel() throws IOException, InterruptedException {
        final CountDownLatch latch = new CountDownLatch(2);
        String path = "index.jar";
        List<Mirror> mirrors = Mirror.fromStrings(Collections.singletonList("https://f-droid.org/repo/"));
        File destFile = File.createTempFile("dl-", "");
        final DownloadRequest request = new DownloadRequest(path, mirrors, null, null, null);
        final HttpDownloader httpDownloader = new HttpDownloader(httpManager, request, destFile);
        httpDownloader.setListener((bytesRead, totalBytes) -> {
            receivedProgress = true;
            latch.countDown();
        });
        new Thread() {
            @Override
            public void run() {
                try {
                    httpDownloader.download();
                    fail();
                } catch (IOException e) {
                    e.printStackTrace();
                    fail();
                } catch (InterruptedException e) {
                    // success!
                }
            }
        }.start();
        latch.await(100, TimeUnit.SECONDS); // either 2 progress reports or 100 seconds
        httpDownloader.cancelDownload();
        assertTrue(receivedProgress);
        destFile.deleteOnExit();
    }
}
