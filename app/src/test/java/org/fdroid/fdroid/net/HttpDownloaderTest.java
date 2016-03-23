
package org.fdroid.fdroid.net;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertTrue;

public class HttpDownloaderTest {

    String[] urls = {
            "https://www.google.com",
            "https://en.wikipedia.org/wiki/Index.html",
            "https://mirrors.kernel.org/debian/dists/stable/Release",
            "https://f-droid.org/archive/de.we.acaldav_5.apk",
            // sites that use SNI for HTTPS
            "https://guardianproject.info/fdroid/repo/index.jar",
            "https://firstlook.org",
    };

    private boolean receivedProgress;

    @Test
    public void downloadUninterruptedTest() throws IOException, InterruptedException {
        for (String urlString : urls) {
            URL url = new URL(urlString);
            File destFile = File.createTempFile("dl-", "");
            destFile.deleteOnExit(); // this probably does nothing, but maybe...
            HttpDownloader httpDownloader = new HttpDownloader(url, destFile, null);
            httpDownloader.download();
            assertTrue(destFile.exists());
            assertTrue(destFile.canRead());
        }
    }

    @Test
    public void downloadUninterruptedTestWithProgress() throws IOException, InterruptedException {
        for (String urlString : urls) {
            receivedProgress = false;
            URL url = new URL(urlString);
            File destFile = File.createTempFile("dl-", "");
            destFile.deleteOnExit(); // this probably does nothing, but maybe...
            HttpDownloader httpDownloader = new HttpDownloader(url, destFile, null);
            httpDownloader.setListener(new Downloader.DownloaderProgressListener() {
                @Override
                public void sendProgress(URL sourceUrl, int bytesRead, int totalBytes) {
                    receivedProgress = true;
                }
            });
            httpDownloader.download();
            assertTrue(destFile.exists());
            assertTrue(destFile.canRead());
            assertTrue(receivedProgress);
        }
    }
}
