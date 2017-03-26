package org.fdroid.fdroid;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.installer.InstallManagerService;

import java.util.ArrayList;
import java.util.List;

/**
 * Scans the list of downloaded .apk files in the cache for each app which can be updated.
 * If a valid .apk file is found then it will tell the {@link AppUpdateStatusManager} that it is
 * {@link AppUpdateStatusManager.Status#ReadyToInstall}. This is an {@link IntentService} so as to
 * run on a background thread, as it hits the disk a bit to figure out the hash of each downloaded
 * file.
 *
 * TODO: Deal with more than just the suggested version. It should also work for people downloading earlier versions (but still newer than their current)
 * TODO: Identify new apps which have not been installed before, but which have been downloading. Currently only works for updates.
 */
public class AppUpdateStatusService extends IntentService {

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
        List<App> apps = AppProvider.Helper.findCanUpdate(this, Schema.AppMetadataTable.Cols.ALL);
        List<Apk> apksReadyToInstall = new ArrayList<>();
        for (App app : apps) {
            Apk apk = ApkProvider.Helper.findApkFromAnyRepo(this, app.packageName, app.suggestedVersionCode);
            Uri downloadUri = Uri.parse(apk.getUrl());
            if (ApkCache.apkIsCached(ApkCache.getApkDownloadPath(this, downloadUri), apk)) {
                apksReadyToInstall.add(apk);
            }
        }

        AppUpdateStatusManager.getInstance(this).addApks(apksReadyToInstall, AppUpdateStatusManager.Status.ReadyToInstall);
        InstallManagerService.managePreviouslyDownloadedApks(this);
    }
}