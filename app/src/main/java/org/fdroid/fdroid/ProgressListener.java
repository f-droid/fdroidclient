package org.fdroid.fdroid;

import java.net.URL;

/**
 * This is meant only to send download progress for any URL (e.g. index
 * updates, APKs, etc). This also keeps this class pure Java so that classes
 * that use {@code ProgressListener} can be tested on the JVM, without requiring
 * an Android device or emulator.
 * <p/>
 * The full URL of a download is used as the unique identifier throughout
 * F-Droid.  I can take a few forms:
 * <ul>
 * <li>{@link URL} instances
 * <li>{@link android.net.Uri} instances
 * <li>{@code String} instances, i.e. {@link URL#toString()}
 * <li>{@code int}s, i.e. {@link String#hashCode()}
 * </ul>
 */
public interface ProgressListener {

    void onProgress(String urlString, long bytesRead, long totalBytes);

}
