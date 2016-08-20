package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.system.ErrnoException;
import android.system.Os;
import android.system.StructStat;

import java.io.File;

/**
 * Helper class to prevent {@link VerifyError}s from occurring in {@link CleanCacheService#clearOldFiles(File, long)}
 * due to the fact that {@link Os} was only introduced in API 21.
 */
@TargetApi(21)
class CleanCacheService21 {
    static void deleteIfOld(File file, long olderThan) {
        try {
            StructStat stat = Os.lstat(file.getAbsolutePath());
            if ((stat.st_atime * 1000L) < olderThan) {
                file.delete();
            }
        } catch (ErrnoException e) {
            e.printStackTrace();
        }
    }
}
