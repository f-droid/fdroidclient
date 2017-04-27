package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.installer.InstallManagerService;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Scans the list of downloaded .apk files in the cache for each app which can be updated.
 * If a valid .apk file is found then it will tell the {@link AppUpdateStatusManager} that it is
 * {@link AppUpdateStatusManager.Status#ReadyToInstall}. This is an {@link IntentService} so as to
 * run on a background thread, as it hits the disk a bit to figure out the hash of each downloaded
 * file.
 */
public class AppUpdateStatusService extends IntentService {

    private static final String TAG = "AppUpdateStatusService";

    /**
     * Queue up a background scan of all downloaded apk files to see if we should notify the user
     * that they are ready to install.
     */
    public static void scanDownloadedApks(Context context) {
        context.startService(new Intent(context, AppUpdateStatusService.class));
    }

    public AppUpdateStatusService() {
        super("AppUpdateStatusService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Utils.debugLog(TAG, "Scanning apk cache to see if we need to prompt the user to install any apks.");
        List<Apk> apksReadyToInstall = new ArrayList<>();
        File cacheDir = ApkCache.getApkCacheDir(this);
        for (String repoDirName : cacheDir.list()) {
            File repoDir = new File(cacheDir, repoDirName);
            for (String apkFileName : repoDir.list()) {
                Apk apk = processDownloadedApk(new File(repoDir, apkFileName));
                if (apk != null) {
                    Log.i(TAG, "Found downloaded apk " + apk.packageName + ". Notifying user that it should be installed.");
                    apksReadyToInstall.add(apk);
                }
            }
        }

        AppUpdateStatusManager.getInstance(this).addApks(apksReadyToInstall, AppUpdateStatusManager.Status.ReadyToInstall);
        InstallManagerService.managePreviouslyDownloadedApks(this);
    }

    @Nullable
    private Apk processDownloadedApk(File apkPath) {
        Utils.debugLog(TAG, "Checking " + apkPath);
        PackageInfo downloadedInfo = getPackageManager().getPackageArchiveInfo(apkPath.getAbsolutePath(), PackageManager.GET_GIDS);
        if (downloadedInfo == null) {
            Utils.debugLog(TAG, "Skipping " + apkPath + " because PackageManager was unable to read it.");
            return null;
        }

        Utils.debugLog(TAG, "Found package for " + downloadedInfo.packageName + ", checking its hash to see if it downloaded correctly.");
        Apk downloadedApk = findApkMatchingHash(apkPath);
        if (downloadedApk ==  null) {
            Utils.debugLog(TAG, "Either the apk wasn't downloaded fully, or the repo it came from has been disabled. Either way, not notifying the user about it.");
            return null;
        }

        if (AppUpdateStatusManager.getInstance(this).isPendingInstall(downloadedApk.hash)) {
            Utils.debugLog(TAG, downloadedApk.packageName + " is pending install, so we need to notify the user about installing it.");
            return downloadedApk;
        } else {
            Utils.debugLog(TAG, downloadedApk.packageName + " is NOT pending install, probably just left over from a previous install.");
            return null;
        }
    }

    /**
     * There could be multiple apks with the same hash, provided by different repositories.
     * This method looks for all matching records in the database. It then asks each of these
     * {@link Apk} instances where they expect to be downloaded. If they expect to be downloaded
     * to {@param apkPath} then that instance is returned.
     *
     * If no files have a matching hash, or only those which don't belong to the correct repo, then
     * this will return null.
     */
    @Nullable
    private Apk findApkMatchingHash(File apkPath) {

        // NOTE: This presumes SHA256 is the only supported hash. It seems like that is an assumption
        // in more than one place in the F-Droid client. If this becomes a problem in the future, we
        // can query the Apk table for `SELECT DISTINCT hashType FROM fdroid_apk` and then we can just
        // try each of the hash types that have been specified in the metadata. Seems a bit overkill
        // at the time of writing though.
        String hash = Utils.getBinaryHash(apkPath, "sha256");

        List<Apk> apksMatchingHash = ApkProvider.Helper.findApksByHash(this, hash);
        Utils.debugLog(TAG, "Found " + apksMatchingHash.size() + " apk(s) matching the hash " + hash);

        for (Apk apk : apksMatchingHash) {
            if (apkPath.equals(ApkCache.getApkDownloadPath(this, Uri.parse(apk.getUrl())))) {
                return apk;
            }
        }

        return null;
    }
}