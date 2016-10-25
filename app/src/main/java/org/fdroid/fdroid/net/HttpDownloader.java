package org.belmarket.shop.net;

import com.nostra13.universalimageloader.core.download.BaseImageDownloader;

import org.apache.commons.io.FileUtils;
import org.belmarket.shop.FDroidApp;
import org.belmarket.shop.Utils;
import org.spongycastle.util.encoders.Base64;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import info.guardianproject.netcipher.NetCipher;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_FIELD_ETAG = "ETag";

    private final String username;
    private final String password;
    private HttpURLConnection connection;
    private int statusCode = -1;

    HttpDownloader(URL url, File destFile)
            throws FileNotFoundException, MalformedURLException {
        this(url, destFile, null, null);
    }

    /**
     * Create a downloader that can authenticate via HTTP Basic Auth using the supplied
     * {@code username} and {@code password}.
     *
     * @param url      The file to download
     * @param destFile Where the download is saved
     * @param username Username for HTTP Basic Auth, use {@code null} to ignore
     * @param password Password for HTTP Basic Auth, use {@code null} to ignore
     * @throws FileNotFoundException
     * @throws MalformedURLException
     */
    HttpDownloader(URL url, File destFile, String username, String password)
            throws FileNotFoundException, MalformedURLException {
        super(url, destFile);

        this.username = username;
        this.password = password;
    }

    /**
     * Note: Doesn't follow redirects (as far as I'm aware).
     * {@link BaseImageDownloader#getStreamFromNetwork(String, Object)} has an implementation worth
     * checking out that follows redirects up to a certain point. I guess though the correct way
     * is probably to check for a loop (keep a list of all URLs redirected to and if you hit the
     * same one twice, bail with an exception).
     *
     * @throws IOException
     */
    @Override
    protected InputStream getDownloadersInputStream() throws IOException {
        setupConnection(false);
        return new BufferedInputStream(connection.getInputStream());
    }

    /**
     * Get a remote file, checking the HTTP response code.  If 'etag' is not
     * {@code null}, it's passed to the server as an If-None-Match header, in
     * which case expect a 304 response if nothing changed. In the event of a
     * 200 response ONLY, 'retag' (which should be passed empty) may contain
     * an etag value for the response, or it may be left empty if none was
     * available.
     */
    @Override
    public void download() throws IOException, InterruptedException {
        boolean resumable = false;
        long fileLength = outputFile.length();

        // get the file size from the server
        HttpURLConnection tmpConn = getConnection();
        int contentLength = -1;
        if (tmpConn.getResponseCode() == 200) {
            contentLength = tmpConn.getContentLength();
        }
        tmpConn.disconnect();
        if (fileLength > contentLength) {
            FileUtils.deleteQuietly(outputFile);
        } else if (fileLength == contentLength && outputFile.isFile()) {
            return; // already have it!
        } else if (fileLength > 0) {
            resumable = true;
        }
        setupConnection(resumable);
        doDownload(resumable);
    }

    private boolean isSwapUrl() {
        String host = sourceUrl.getHost();
        return sourceUrl.getPort() > 1023 // only root can use <= 1023, so never a swap repo
                && host.matches("[0-9.]+") // host must be an IP address
                && FDroidApp.subnetInfo.isInRange(host); // on the same subnet as we are
    }

    private HttpURLConnection getConnection() throws IOException {
        HttpURLConnection connection;
        if (isSwapUrl()) {
            // swap never works with a proxy, its unrouted IP on the same subnet
            connection = (HttpURLConnection) sourceUrl.openConnection();
        } else {
            connection = NetCipher.getHttpURLConnection(sourceUrl);
        }

        if (username != null && password != null) {
            // add authorization header from username / password if set
            String authString = username + ":" + password;
            connection.setRequestProperty("Authorization", "Basic " + Base64.toBase64String(authString.getBytes()));
        }
        return connection;
    }

    /**
     * @return Whether the connection is resumable or not
     */
    private void setupConnection(boolean resumable) throws IOException {
        if (connection != null) {
            return;
        }
        connection = getConnection();

        if (resumable) {
            // partial file exists, resume the download
            connection.setRequestProperty("Range", "bytes=" + outputFile.length() + "-");
        }
    }

    private void doDownload(boolean resumable) throws IOException, InterruptedException {
        if (wantToCheckCache()) {
            setupCacheCheck();
            Utils.debugLog(TAG, "Checking cached status of " + sourceUrl);
            statusCode = connection.getResponseCode();
        }

        if (isCached()) {
            Utils.debugLog(TAG, sourceUrl + " is cached, so not downloading (HTTP " + statusCode + ")");
        } else {
            Utils.debugLog(TAG, "Need to download " + sourceUrl + " (is resumable: " + resumable + ")");
            downloadFromStream(8192, resumable);
            updateCacheCheck();
        }
    }

    @Override
    public boolean isCached() {
        return wantToCheckCache() && statusCode == 304;
    }

    private void setupCacheCheck() {
        if (cacheTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, cacheTag);
        }
    }

    private void updateCacheCheck() {
        cacheTag = connection.getHeaderField(HEADER_FIELD_ETAG);
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
    public int totalDownloadSize() {
        return connection.getContentLength();
    }

    @Override
    public boolean hasChanged() {
        return this.statusCode != 304;
    }

    @Override
    public void close() {
        if (connection != null) {
            connection.disconnect();
        }
    }
}
