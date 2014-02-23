package org.fdroid.fdroid.net;

import java.io.*;
import java.net.*;
import android.content.*;
import org.fdroid.fdroid.*;

public abstract class Downloader {

    private OutputStream outputStream;
    private ProgressListener progressListener = null;
    private ProgressListener.Event progressEvent = null;
    private final File outputFile;

    public abstract InputStream inputStream() throws IOException;

    // The context is required for opening the file to write to.
    public Downloader(String destFile, Context ctx)
            throws FileNotFoundException, MalformedURLException {
        outputStream = ctx.openFileOutput(destFile, Context.MODE_PRIVATE);
        outputFile   = new File(ctx.getFilesDir() + File.separator + destFile);
    }

    /**
     * Downloads to a temporary file, which *you must delete yourself when
     * you are done*.
     * @see org.fdroid.fdroid.net.Downloader#getFile()
     */
    public Downloader(Context ctx) throws IOException {
        // http://developer.android.com/guide/topics/data/data-storage.html#InternalCache
        outputFile = File.createTempFile("dl-", "", ctx.getCacheDir());
        outputStream = new FileOutputStream(outputFile);
    }

    public Downloader(OutputStream output)
            throws MalformedURLException {
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

    private void setupProgressListener() {
        if (progressListener != null && progressEvent != null) {
            progressEvent.total = totalDownloadSize();
        }
    }

    protected abstract int totalDownloadSize();

    public void download() throws IOException {
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
