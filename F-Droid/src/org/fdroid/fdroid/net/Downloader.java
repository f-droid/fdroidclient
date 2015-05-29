package org.fdroid.fdroid.net;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import java.net.URL;

public abstract class Downloader {

    private static final String TAG = "Downloader";
    private OutputStream outputStream;

    private ProgressListener progressListener = null;
    private Bundle eventData = null;
    private final File outputFile;

    protected URL sourceUrl;
    protected String cacheTag = null;

    public static final String EVENT_PROGRESS = "downloadProgress";

    public abstract InputStream getInputStream() throws IOException;

    // The context is required for opening the file to write to.
    Downloader(String destFile, @NonNull Context ctx)
            throws FileNotFoundException, MalformedURLException {
        this(new File(ctx.getFilesDir() + File.separator + destFile));
    }

    // The context is required for opening the file to write to.
    Downloader(@NonNull Context ctx) throws IOException {
        this(File.createTempFile("dl-", "", ctx.getCacheDir()));
    }

    Downloader(File destFile)
            throws FileNotFoundException, MalformedURLException {
        outputFile = destFile;
        outputStream = new FileOutputStream(outputFile);
    }

    Downloader(OutputStream output)
            throws MalformedURLException {
        outputStream = output;
        outputFile   = null;
    }

    public void setProgressListener(ProgressListener listener) {
        setProgressListener(listener, null);
    }

    public void setProgressListener(ProgressListener listener, Bundle eventData) {
        this.progressListener = listener;
        this.eventData = eventData;
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

    protected boolean wantToCheckCache() {
        return cacheTag != null;
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

    /**
     * Helper function for synchronous downloads (i.e. those *not* using AsyncDownloadWrapper),
     * which don't really want to bother dealing with an InterruptedException.
     * The InterruptedException thrown from download() is there to enable cancelling asynchronous
     * downloads, but regular synchronous downloads cannot be cancelled because download() will
     * block until completed.
     * @throws IOException
     */
    public void downloadUninterrupted() throws IOException {
        try {
            download();
        } catch (InterruptedException ignored) {}
    }

    public abstract void download() throws IOException, InterruptedException;

    public abstract boolean isCached();

    protected void downloadFromStream() throws IOException, InterruptedException {
        Log.d(TAG, "Downloading from stream");
        InputStream input = null;
        try {
            input = getInputStream();

            // Getting the input stream is slow(ish) for HTTP downloads, so we'll check if
            // we were interrupted before proceeding to the download.
            throwExceptionIfInterrupted();

            copyInputToOutputStream(getInputStream());
        } finally {
            Utils.closeQuietly(outputStream);
            Utils.closeQuietly(input);
        }

        // Even if we have completely downloaded the file, we should probably respect
        // the wishes of the user who wanted to cancel us.
        throwExceptionIfInterrupted();
    }

    /**
     * In a synchronous download (the usual usage of the Downloader interface),
     * you will not be able to interrupt this because the thread will block
     * after you have called download(). However if you use the AsyncDownloadWrapper,
     * then it will use this mechanism to cancel the download.
     *
     * After every network operation that could take a while, we will check if an
     * interrupt occured during that blocking operation. The goal is to ensure we
     * don't move onto another slow, network operation if we have cancelled the
     * download.
     * @throws InterruptedException
     */
    private void throwExceptionIfInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
            Log.d(TAG, "Received interrupt, cancelling download");
            throw new InterruptedException();
        }
    }

    protected void copyInputToOutputStream(InputStream input) throws IOException, InterruptedException {

        byte[] buffer = new byte[Utils.BUFFER_SIZE];
        int bytesRead = 0;
        int totalBytes = totalDownloadSize();

        // Getting the total download size could potentially take time, depending on how
        // it is implemented, so we may as well check this before we proceed.
        throwExceptionIfInterrupted();

        sendProgress(bytesRead, totalBytes);
        while (true) {

            int count = input.read(buffer);
            throwExceptionIfInterrupted();

            if (count == -1) {
                Log.d(TAG, "Finished downloading from stream");
                break;
            }
            bytesRead += count;
            sendProgress(bytesRead, totalBytes);
            outputStream.write(buffer, 0, count);
        }
        outputStream.flush();
    }

    protected void sendProgress(int bytesRead, int totalBytes) {
        sendProgress(new ProgressListener.Event(EVENT_PROGRESS, bytesRead, totalBytes, eventData));
    }

    protected void sendProgress(ProgressListener.Event event) {
        if (progressListener != null) {
            progressListener.onProgress(event);
        }
    }

}
