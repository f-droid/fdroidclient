package org.fdroid.fdroid.net;

import android.content.Context;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class HttpDownloader extends Downloader {

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_FIELD_ETAG = "ETag";

    private URL sourceUrl;
    private String eTag = null;
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
    public int downloadHttpFile() throws IOException {
        connection = (HttpURLConnection)sourceUrl.openConnection();
        setupCacheCheck();
        statusCode = connection.getResponseCode();
        if (statusCode == 200) {
            download();
            updateCacheCheck();
        }
        return statusCode;
    }

    /**
     * Only available after downloading a file.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * If you ask for the eTag before calling download(), you will get the
     * same one you passed in (if any). If you call it after download(), you
     * will get the new eTag from the server, or null if there was none.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * If this eTag matches that returned by the server, then no download will
     * take place, and a status code of 304 will be returned by download().
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }


    protected void setupCacheCheck() {
        if (eTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, eTag);
        }
    }

    protected void updateCacheCheck() {
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
    protected int totalDownloadSize() {
        return connection.getContentLength();
    }

    public boolean hasChanged() {
        return this.statusCode == 200;
    }

}
