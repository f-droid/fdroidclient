package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.InstalledAppProviderService;
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
@SuppressWarnings("LineLength")
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
        File cacheDir = ApkCache.getApkCacheDir(this);
        if (cacheDir == null) {
            return;
        }
        String[] cacheDirList = cacheDir.list();
        if (cacheDirList == null) {
            return;
        }
        List<Apk> apksReadyToInstall = new ArrayList<>();
        for (String repoDirName : cacheDirList) {
            File repoDir = new File(cacheDir, repoDirName);
            String[] apks = repoDir.list();
            if (apks == null) {
                continue;
            }
            for (String apkFileName : apks) {
                Apk apk = processDownloadedApk(new File(repoDir, apkFileName));
                if (apk != null) {
                    Log.i(TAG, "Found downloaded apk " + apk.packageName + ". Notifying user that it should be installed.");
                    apksReadyToInstall.add(apk);
                }
            }
        }

        if (apksReadyToInstall.size() > 0) {
            AppUpdateStatusManager.getInstance(this).addApks(apksReadyToInstall, AppUpdateStatusManager.Status.ReadyToInstall);
            InstallManagerService.managePreviouslyDownloadedApks(this);
        }
    }

    /**
     * Verifies that {@param apkPath} is a valid apk which the user intends to install.
     * If it is corrupted to the point where {@link PackageManager} can't read it, doesn't match the hash of any apk
     * we know about in our database, is not pending install, or is already installed, then it will return null.
     */
    @Nullable
    private Apk processDownloadedApk(File apkPath) {
        Utils.debugLog(TAG, "Checking " + apkPath);

        // Overly defensive checking for existence. One would think that the file exists at this point,
        // because we got it from the result of File#list() earlier. However, this has proven to not be
        // sufficient, and by the time we get here we are often hitting a non-existent file.
        // This may be due to the fact that the loop checking each file in the cache takes a long time to execute.
        // If the number of apps in the cache is large, it can take 10s of seconds to complete. In such
        // cases, it is possible that Android has cleared up some files in the cache to make space in
        // the meantime.
        //
        // This is all just a hypothesis about what may have caused
        // https://gitlab.com/fdroid/fdroidclient/issues/1172
        if (!apkPath.exists()) {
            Log.i(TAG, "Was going to check " + apkPath + ", but it has since been removed from the cache.");
            return null;
        }

        PackageInfo downloadedInfo = getPackageManager().getPackageArchiveInfo(apkPath.getAbsolutePath(), PackageManager.GET_GIDS);
        if (downloadedInfo == null) {
            Log.i(TAG, "Skipping " + apkPath + " because PackageManager was unable to read it.");
            return null;
        }

        Utils.debugLog(TAG, "Found package for " + downloadedInfo.packageName + ", checking its hash to see if it downloaded correctly.");
        Apk downloadedApk = findApkMatchingHash(apkPath);
        if (downloadedApk == null) {
            Log.i(TAG, "Either the apk wasn't downloaded fully, or the repo it came from has been disabled. Either way, not notifying the user about it.");
            return null;
        }

        if (!AppUpdateStatusManager.getInstance(this).isPendingInstall(downloadedApk.hash)) {
            Log.i(TAG, downloadedApk.packageName + " is NOT pending install, probably just left over from a previous install.");
            return null;
        }

        try {
            PackageInfo info = getPackageManager().getPackageInfo(downloadedApk.packageName, 0);
            File pathToInstalled = InstalledAppProviderService.getPathToInstalledApk(info);
            if (pathToInstalled != null && pathToInstalled.canRead() &&
                    pathToInstalled.length() == downloadedApk.size && // Check size before hash for performance.
                    TextUtils.equals(Utils.getBinaryHash(pathToInstalled, "sha256"), downloadedApk.hash)) {
                Log.i(TAG, downloadedApk.packageName + " is pending install, but we already have the correct version installed.");
                AppUpdateStatusManager.getInstance(this).markAsNoLongerPendingInstall(downloadedApk.getUrl());
                return null;
            }
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        Utils.debugLog(TAG, downloadedApk.packageName + " is pending install, so we need to notify the user about installing it.");
        return downloadedApk;
    }

    /**
     * There could be multiple apks with the same hash, provided by different repositories.
     * This method looks for all matching records in the database. It then asks each of these
     * {@link Apk} instances where they expect to be downloaded. If they expect to be downloaded
     * to {@param apkPath} then that instance is returned.
     * <p>
     * If no files have a matching hash, or only those which don't belong to the correct repo, then
     * this will return null.  This method needs to do its own check whether the file exists,
     * since files can be deleted from the cache at any time without warning.
     */
    @Nullable
    private Apk findApkMatchingHash(File apkPath) {
        if (!apkPath.canRead()) {
            return null;
        }

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