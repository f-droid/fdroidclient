package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;

import org.fdroid.fdroid.Preferences;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketAddress;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";

    protected static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    protected static final String HEADER_FIELD_ETAG = "ETag";

    protected HttpURLConnection connection;
    private int statusCode = -1;

    // The context is required for opening the file to write to.
    HttpDownloader(String source, File destFile)
            throws FileNotFoundException, MalformedURLException {
        super(destFile);
        sourceUrl = new URL(source);
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done*.
     * @see org.fdroid.fdroid.net.Downloader#getFile()
     */
    HttpDownloader(String source, Context ctx) throws IOException {
        super(ctx);
        sourceUrl = new URL(source);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        setupConnection();
        // TODO check out BaseImageDownloader.getStreamFromNetwork() for optims
        return connection.getInputStream();
    }

    // Get a remote file. Returns the HTTP response code.
    // If 'etag' is not null, it's passed to the server as an If-None-Match
    // header, in which case expect a 304 response if nothing changed.
    // In the event of a 200 response ONLY, 'retag' (which should be passed
    // empty) may contain an etag value for the response, or it may be left
    // empty if none was available.
    @Override
    public void download() throws IOException, InterruptedException {
        try {
            setupConnection();
            doDownload();
        } catch (SSLHandshakeException e) {
            // TODO this should be handled better, it is not internationalised here
            throw new IOException(
                    "A problem occurred while establishing an SSL " +
                            "connection. If this problem persists, AND you have a " +
                            "very old device, you could try using http instead of " +
                            "https for the repo URL." + Log.getStackTraceString(e));
        }
    }

    protected void setupConnection() throws IOException {
        if (connection != null)
            return;
        Preferences prefs = Preferences.get();
        if (prefs.isProxyEnabled()) {
            SocketAddress sa = new InetSocketAddress(prefs.getProxyHost(), prefs.getProxyPort());
            Proxy proxy = new Proxy(Proxy.Type.HTTP, sa);
            connection = (HttpURLConnection) sourceUrl.openConnection(proxy);
        } else {
            connection = (HttpURLConnection) sourceUrl.openConnection();
        }
    }

    protected void doDownload() throws IOException, InterruptedException {
        if (wantToCheckCache()) {
            setupCacheCheck();
            Log.i(TAG, "Checking cached status of " + sourceUrl);
            statusCode = connection.getResponseCode();
        }

        if (isCached()) {
            Log.i(TAG, sourceUrl + " is cached, so not downloading (HTTP " + statusCode + ")");
        } else {
            Log.i(TAG, "Downloading from " + sourceUrl);
            downloadFromStream();
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

}
