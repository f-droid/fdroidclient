package org.fdroid.fdroid.net;

import android.net.Uri;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.Timer;
import java.util.TimerTask;

public abstract class Downloader {

    private static final String TAG = "Downloader";

    public static final String ACTION_STARTED = "org.fdroid.fdroid.net.Downloader.action.STARTED";
    public static final String ACTION_PROGRESS = "org.fdroid.fdroid.net.Downloader.action.PROGRESS";
    public static final String ACTION_INTERRUPTED = "org.fdroid.fdroid.net.Downloader.action.INTERRUPTED";
    public static final String ACTION_CONNECTION_FAILED = "org.fdroid.fdroid.net.Downloader.action.CONNECTION_FAILED";
    public static final String ACTION_COMPLETE = "org.fdroid.fdroid.net.Downloader.action.COMPLETE";

    public static final String EXTRA_DOWNLOAD_PATH = "org.fdroid.fdroid.net.Downloader.extra.DOWNLOAD_PATH";
    public static final String EXTRA_BYTES_READ = "org.fdroid.fdroid.net.Downloader.extra.BYTES_READ";
    public static final String EXTRA_TOTAL_BYTES = "org.fdroid.fdroid.net.Downloader.extra.TOTAL_BYTES";
    public static final String EXTRA_ERROR_MESSAGE = "org.fdroid.fdroid.net.Downloader.extra.ERROR_MESSAGE";
    public static final String EXTRA_REPO_ID = "org.fdroid.fdroid.net.Downloader.extra.ERROR_REPO_ID";
    public static final String EXTRA_CANONICAL_URL = "org.fdroid.fdroid.net.Downloader.extra.ERROR_CANONICAL_URL";
    public static final String EXTRA_MIRROR_URL = "org.fdroid.fdroid.net.Downloader.extra.ERROR_MIRROR_URL";

    private volatile boolean cancelled = false;
    private volatile long bytesRead;
    private volatile long totalBytes;

    public final File outputFile;

    final String urlString;
    String cacheTag;
    boolean notFound;

    private volatile int timeout = 10000;

    /**
     * For sending download progress, should only be called in {@link #progressTask}
     */
    private volatile ProgressListener downloaderProgressListener;

    protected abstract InputStream getDownloadersInputStream() throws IOException;

    protected abstract void close();

    Downloader(Uri uri, File destFile) {
        this.urlString = uri.toString();
        outputFile = destFile;
    }

    public final InputStream getInputStream() throws IOException {
        return new WrappedInputStream(getDownloadersInputStream());
    }

    public void setListener(ProgressListener listener) {
        this.downloaderProgressListener = listener;
    }

    public void setTimeout(int ms) {
        timeout = ms;
    }

    public int getTimeout() {
        return timeout;
    }

    /**
     * If you ask for the cacheTag before calling download(), you will get the
     * same one you passed in (if any). If you call it after download(), you
     * will get the new cacheTag from the server, or null if there was none.
     */
    public String getCacheTag() {
        return cacheTag;
    }

    /**
     * If this cacheTag matches that returned by the server, then no download will
     * take place, and a status code of 304 will be returned by download().
     */
    public void setCacheTag(String cacheTag) {
        this.cacheTag = cacheTag;
    }

    public abstract boolean hasChanged();

    protected abstract long totalDownloadSize();

    public abstract void download() throws ConnectException, IOException, InterruptedException;

    /**
     * @return whether the requested file was not found in the repo (e.g. HTTP 404 Not Found)
     */
    public boolean isNotFound() {
        return notFound;
    }

    void downloadFromStream(int bufferSize, boolean resumable) throws IOException, InterruptedException {
        Utils.debugLog(TAG, "Downloading from stream");
        InputStream input = null;
        OutputStream outputStream = new FileOutputStream(outputFile, resumable);
        try {
            input = getInputStream();

            // Getting the input stream is slow(ish) for HTTP downloads, so we'll check if
            // we were interrupted before proceeding to the download.
            throwExceptionIfInterrupted();

            copyInputToOutputStream(input, bufferSize, outputStream);
        } finally {
            Utils.closeQuietly(outputStream);
            Utils.closeQuietly(input);
        }

        // Even if we have completely downloaded the file, we should probably respect
        // the wishes of the user who wanted to cancel us.
        throwExceptionIfInterrupted();
    }

    /**
     * After every network operation that could take a while, we will check if an
     * interrupt occured during that blocking operation. The goal is to ensure we
     * don't move onto another slow, network operation if we have cancelled the
     * download.
     *
     * @throws InterruptedException
     */
    private void throwExceptionIfInterrupted() throws InterruptedException {
        if (cancelled) {
            Utils.debugLog(TAG, "Received interrupt, cancelling download");
            throw new InterruptedException();
        }
    }

    /**
     * Cancel a running download, triggering an {@link InterruptedException}
     */
    public void cancelDownload() {
        cancelled = true;
    }

    /**
     * This copies the downloaded data from the InputStream to the OutputStream,
     * keeping track of the number of bytes that have flowed through for the
     * progress counter.
     */
    private void copyInputToOutputStream(InputStream input, int bufferSize, OutputStream output)
            throws IOException, InterruptedException {
        Timer timer = new Timer();
        try {
            bytesRead = 0;
            totalBytes = totalDownloadSize();
            byte[] buffer = new byte[bufferSize];

            timer.scheduleAtFixedRate(progressTask, 0, 100);

            // Getting the total download size could potentially take time, depending on how
            // it is implemented, so we may as well check this before we proceed.
            throwExceptionIfInterrupted();

            while (true) {

                int count;
                if (input.available() > 0) {
                    int readLength = Math.min(input.available(), buffer.length);
                    count = input.read(buffer, 0, readLength);
                } else {
                    count = input.read(buffer);
                }

                throwExceptionIfInterrupted();

                if (count == -1) {
                    Utils.debugLog(TAG, "Finished downloading from stream");
                    break;
                }
                bytesRead += count;
                output.write(buffer, 0, count);
            }
        } finally {
            downloaderProgressListener = null;
            timer.cancel();
            timer.purge();
            output.flush();
            output.close();
        }
    }

    /**
     * Send progress updates on a timer to avoid flooding receivers with pointless events.
     */
    private final TimerTask progressTask = new TimerTask() {
        @Override
        public void run() {
            if (downloaderProgressListener != null) {
                downloaderProgressListener.onProgress(urlString, bytesRead, totalBytes);
            }
        }
    };

    /**
     * Overrides every method in {@link InputStream} and delegates to the wrapped stream.
     * The only difference is that when we call the {@link WrappedInputStream#close()} method,
     * after delegating to the wrapped stream we invoke the {@link Downloader#close()} method
     * on the {@link Downloader} which created this.
     */
    private class WrappedInputStream extends InputStream {

        private final InputStream toWrap;

        WrappedInputStream(InputStream toWrap) {
            super();
            this.toWrap = toWrap;
        }

        @Override
        public void close() throws IOException {
            toWrap.close();
            Downloader.this.close();
        }

        @Override
        public int available() throws IOException {
            return toWrap.available();
        }

        @Override
        public void mark(int readlimit) {
            toWrap.mark(readlimit);
        }

        @Override
        public boolean markSupported() {
            return toWrap.markSupported();
        }

        @Override
        public int read(byte[] buffer) throws IOException {
            return toWrap.read(buffer);
        }

        @Override
        public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
            return toWrap.read(buffer, byteOffset, byteCount);
        }

        @Override
        public synchronized void reset() throws IOException {
            toWrap.reset();
        }

        @Override
        public long skip(long byteCount) throws IOException {
            return toWrap.skip(byteCount);
        }

        @Override
        public int read() throws IOException {
            return toWrap.read();
        }
    }
}
