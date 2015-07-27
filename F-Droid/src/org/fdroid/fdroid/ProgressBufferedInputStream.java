package org.fdroid.fdroid;

import android.os.Bundle;

import org.fdroid.fdroid.data.Repo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressBufferedInputStream extends BufferedInputStream {
    private static final String TAG = "ProgressBufferedInputSt";

    final Repo repo;
    final ProgressListener progressListener;
    final Bundle data;
    final int totalBytes;

    int currentBytes = 0;

    /**
     * Reports progress to the specified {@link ProgressListener}, with the
     * progress based on the {@code totalBytes}.
     */
    public ProgressBufferedInputStream(InputStream in, ProgressListener progressListener, Repo repo, int totalBytes)
            throws IOException {
        super(in);
        this.progressListener = progressListener;
        this.repo = repo;
        this.data = new Bundle(1);
        this.data.putString(RepoUpdater.PROGRESS_DATA_REPO_ADDRESS, repo.address);
        this.totalBytes = totalBytes;
    }

    @Override
    public int read(byte[] buffer, int byteOffset, int byteCount) throws IOException {
        if (progressListener != null) {
            currentBytes += byteCount;
            /* don't send every change to keep things efficient.  333333 bytes to keep all
             * the digits changing because it looks pretty, < 9000 since the reads won't
             * line up exactly */
            if (currentBytes % 333333 < 9000) {
                progressListener.onProgress(
                        new ProgressListener.Event(
                                RepoUpdater.PROGRESS_TYPE_PROCESS_XML,
                                currentBytes, totalBytes, data));
            }
        }
        return super.read(buffer, byteOffset, byteCount);
    }
}
