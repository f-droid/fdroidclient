package org.fdroid.fdroid.net;

import java.io.*;
import java.net.*;
import android.content.*;
import org.fdroid.fdroid.*;

public class Downloader {

    private static final String HEADER_IF_NONE_MATCH = "If-None-Match";
    private static final String HEADER_FIELD_ETAG = "ETag";

    private URL sourceUrl;
    private OutputStream outputStream;
    private ProgressListener progressListener = null;
    private ProgressListener.Event progressEvent = null;
    private String eTag = null;
    private final File outputFile;
    private HttpURLConnection connection;
    private int statusCode = -1;

    // The context is required for opening the file to write to.
    public Downloader(String source, String destFile, Context ctx)
            throws FileNotFoundException, MalformedURLException {
        sourceUrl    = new URL(source);
        outputStream = ctx.openFileOutput(destFile, Context.MODE_PRIVATE);
        outputFile   = new File(ctx.getFilesDir() + File.separator + destFile);
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done*.
     * @see org.fdroid.fdroid.net.Downloader#getFile()
     */
    public Downloader(String source, Context ctx) throws IOException {
        // http://developer.android.com/guide/topics/data/data-storage.html#InternalCache
        outputFile = File.createTempFile("dl-", "", ctx.getCacheDir());
        outputStream = new FileOutputStream(outputFile);
        sourceUrl = new URL(source);
    }

    public Downloader(String source, OutputStream output)
            throws MalformedURLException {
        sourceUrl    = new URL(source);
        outputStream = output;
        outputFile   = null;
    }

    public void setProgressListener(ProgressListener progressListener,
                                    ProgressListener.Event progressEvent) {
        this.progressListener = progressListener;
        this.progressEvent = progressEvent;
    }

    /**
     * Only available if you passed a context object into the constructor
     * (rather than an outputStream, which may or  may not be associated with
     * a file).
     */
    public File getFile() {
        return outputFile;
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

    // Get a remote file. Returns the HTTP response code.
    // If 'etag' is not null, it's passed to the server as an If-None-Match
    // header, in which case expect a 304 response if nothing changed.
    // In the event of a 200 response ONLY, 'retag' (which should be passed
    // empty) may contain an etag value for the response, or it may be left
    // empty if none was available.
    public int download() throws IOException {
        connection = (HttpURLConnection)sourceUrl.openConnection();
        setupCacheCheck();
        statusCode = connection.getResponseCode();
        if (statusCode == 200) {
            setupProgressListener();
            InputStream input = null;
            try {
                input = connection.getInputStream();
                Utils.copy(input, outputStream,
                        progressListener, progressEvent);
            } finally {
                Utils.closeQuietly(outputStream);
                Utils.closeQuietly(input);
            }
            updateCacheCheck();
        }
        return statusCode;
    }

    protected void setupCacheCheck() {
        if (eTag != null) {
            connection.setRequestProperty(HEADER_IF_NONE_MATCH, eTag);
        }
    }

    protected void updateCacheCheck() {
        eTag = connection.getHeaderField(HEADER_FIELD_ETAG);
    }

    protected void setupProgressListener() {
        if (progressListener != null && progressEvent != null) {
            // Testing in the emulator for me, showed that figuring out the
            // filesize took about 1 to 1.5 seconds.
            // To put this in context, downloading a repo of:
            //  - 400k takes ~6 seconds
            //  - 5k   takes ~3 seconds
            // on my connection. I think the 1/1.5 seconds is worth it,
            // because as the repo grows, the tradeoff will
            // become more worth it.
            progressEvent.total = connection.getContentLength();
        }
    }

    public boolean hasChanged() {
        return this.statusCode == 200;
    }

}
