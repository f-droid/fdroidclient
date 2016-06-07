package org.fdroid.fdroid;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

class ProgressBufferedInputStream extends BufferedInputStream {

    private final ProgressListener progressListener;
    private final URL sourceUrl;
    private final int totalBytes;

    private int currentBytes;

    /**
     * Reports progress to the specified {@link ProgressListener}, with the
     * progress based on the {@code totalBytes}.
     */
    ProgressBufferedInputStream(InputStream in, ProgressListener progressListener, URL sourceUrl, int totalBytes) {
        super(in);
        this.progressListener = progressListener;
        this.sourceUrl = sourceUrl;
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
                progressListener.onProgress(sourceUrl, currentBytes, totalBytes);
            }
        }
        return super.read(buffer, byteOffset, byteCount);
    }
}
