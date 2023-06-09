package org.fdroid.fdroid.shadows;

import android.annotation.SuppressLint;
import android.text.TextUtils;
import android.util.Log;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

/**
 * There are two lines of output which end up in our CI 1000s of times:
 * <p>
 * - I/CursorWindowStats: Created a new Cursor. # Open Cursors=1 (# cursors opened by this proc=1)
 * - D/SQLiteCursor: received count(*) from native_fill_window: 0
 * <p>
 * Neither of them seem like they are telling us anything useful, so this suppresses them by intercepting the call
 * to {@link Log#isLoggable(String, int)} and returning if it matches either of them.
 */
@Implements(Log.class)
@SuppressLint("Unused")
public class ShadowLog extends org.robolectric.shadows.ShadowLog {

    @Implementation
    public static synchronized boolean isLoggable(String tag, int level) {
        if (TextUtils.equals(tag, "CursorWindowStats") && level <= Log.INFO
                || TextUtils.equals(tag, "SQLiteCursor") && level <= Log.DEBUG) {
            return false;
        }

        return org.robolectric.shadows.ShadowLog.isLoggable(tag, level);
    }
}
