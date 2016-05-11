package org.fdroid.fdroid;

import java.net.URL;

/**
 * This is meant only to send download progress for any URL (e.g. index
 * updates, APKs, etc). This also keeps this class pure Java so that classes
 * that use {@code ProgressListener} can be tested on the JVM, without requiring
 * an Android device or emulator.
 */
public interface ProgressListener {

    void onProgress(URL sourceUrl, int bytesRead, int totalBytes);

}
