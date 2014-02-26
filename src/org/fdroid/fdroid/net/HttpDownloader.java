package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.SSLHandshakeException;

public class HttpDownloader extends Downloader {
    private static final String TAG = "HttpDownloader";

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_FIELD_ETAG = "ETag";

    private URL sourceUrl;
    private HttpURLConnection connection;
    private int statusCode = -1;

    // The context is required for opening the file to write to.
    public HttpDownloader(String source, String destFile, Context ctx)
            throws FileNotFoundException, MalformedURLException {
        super(destFile, ctx);
        sourceUrl = new URL(source);
    }

    // The context is required for opening the file to write to.
    public HttpDownloader(String source, File destFile)
            throws FileNotFoundException, MalformedURLException {
        super(destFile);
        sourceUrl = new URL(source);
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done*.
     * @see org.fdroid.fdroid.net.HttpDownloader#getFile()
     */
    public HttpDownloader(String source, Context ctx) throws IOException {
        super(ctx);
        sourceUrl = new URL(source);
    }

    public HttpDownloader(String source, OutputStream output)
            throws MalformedURLException {
        super(output);
        sourceUrl = new URL(source);
    }

    public InputStream inputStream() throws IOException {
        return connection.getInputStream();
    }

    // Get a remote file. Returns the HTTP response code.
    // If 'etag' is not null, it's passed to the server as an If-None-Match
    // header, in which case expect a 304 response if nothing changed.
    // In the event of a 200 response ONLY, 'retag' (which should be passed
    // empty) may contain an etag value for the response, or it may be left
    // empty if none was available.
    @Override
    public void download() throws IOException {
        try {
            connection = (HttpURLConnection)sourceUrl.openConnection();
            setupCacheCheck();
            statusCode = connection.getResponseCode();
            Log.i(TAG, "download " + statusCode);
            if (statusCode == 304) {
                // The index is unchanged since we last read it. We just mark
                // everything that came from this repo as being updated.
                Log.d("FDroid", "Repo index for " + sourceUrl
                        + " is up to date (by etag)");
            } else if (statusCode == 200) {
                download();
                updateCacheCheck();
            } else {
                // Is there any code other than 200 which still returns
                // content? Just in case, lets try to clean up.
                if (getFile() != null) {
                    getFile().delete();
                }
                throw new IOException(
                        "Failed to update repo " + sourceUrl +
                        " - HTTP response " + statusCode);
            }
        } catch (SSLHandshakeException e) {
            // TODO this should be handled better
            throw new IOException(
                    "A problem occurred while establishing an SSL " +
                            "connection. If this problem persists, AND you have a " +
                            "very old device, you could try using http instead of " +
                            "https for the repo URL." + Log.getStackTraceString(e) );
        }
    }

    private void setupCacheCheck() {
        if (eTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, eTag);
        }
    }

    private void updateCacheCheck() {
        eTag = connection.getHeaderField(HEADER_FIELD_ETAG);
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
        return this.statusCode == 200;
    }

}
