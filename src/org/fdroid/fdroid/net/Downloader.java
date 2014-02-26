package org.fdroid.fdroid.net;

import android.content.Context;
import android.util.Log;

import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.Utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;

public abstract class Downloader {
    private static final String TAG = "Downloader";

    private OutputStream outputStream;
    private ProgressListener progressListener = null;
    private ProgressListener.Event progressEvent = null;
    private File outputFile;

    protected String eTag = null;

    public static final int EVENT_PROGRESS = 1;

    public abstract InputStream inputStream() throws IOException;

    // The context is required for opening the file to write to.
    public Downloader(String destFile, Context ctx)
            throws FileNotFoundException, MalformedURLException {
        this(new File(ctx.getFilesDir() + File.separator + destFile));
    }

    // The context is required for opening the file to write to.
    public Downloader(Context ctx) throws IOException {
        this(File.createTempFile("dl-", "", ctx.getCacheDir()));
    }

    public Downloader(File destFile)
            throws FileNotFoundException, MalformedURLException {
        // http://developer.android.com/guide/topics/data/data-storage.html#InternalCache
        outputFile = destFile;
        outputStream = new FileOutputStream(outputFile);
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done*.
     * @see org.fdroid.fdroid.net.Downloader#getFile()
     */
    public Downloader(File destFile, Context ctx) throws IOException {
    }

    public Downloader(OutputStream output)
            throws MalformedURLException {
        outputStream = output;
        outputFile   = null;
    }

    public void setProgressListener(ProgressListener progressListener,
                                    ProgressListener.Event progressEvent) {
        Log.i(TAG, "setProgressListener(ProgressListener listener, ProgressListener.Event progressEvent)");
        this.progressListener = progressListener;
        this.progressEvent = progressEvent;
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

    /**
     * Only available if you passed a context object into the constructor
     * (rather than an outputStream, which may or  may not be associated with
     * a file).
     */
    public File getFile() {
        return outputFile;
    }

    public abstract boolean hasChanged();

    public abstract int totalDownloadSize();

    private void setupProgressListener() {
        Log.i(TAG, "setupProgressListener");
        if (progressListener != null && progressEvent != null) {
            progressEvent.total = totalDownloadSize();
        }
    }

    public void download() throws IOException {
        Log.i(TAG, "download");
        setupProgressListener();
        InputStream input = null;
        try {
            input = inputStream();
            Utils.copy(input, outputStream,
                    progressListener, progressEvent);
        } finally {
            Utils.closeQuietly(outputStream);
            Utils.closeQuietly(input);
        }
    }

}
