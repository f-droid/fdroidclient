package org.fdroid.fdroid.localrepo;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import org.apache.commons.io.FileUtils;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;

import java.io.File;
import java.io.IOException;
import java.security.cert.CertificateEncodingException;

/**
 * An {@link IntentService} subclass for generating cached info about the installed APKs
 * which are available for swapping.  It does not cache system apps, since those are
 * rarely swapped.  This is meant to start running when {@link SwapService} starts.
 * <p>
 * This could probably be replaced by {@link org.fdroid.fdroid.data.InstalledAppProvider}
 * if that contained all of the info to generate complete {@link App} and
 * {@link org.fdroid.fdroid.data.Apk} instances.
 */
public class CacheSwapAppsService extends IntentService {
    private static final String TAG = "CacheSwapAppsService";

    private static final String ACTION_PARSE_APP = "org.fdroid.fdroid.localrepo.action.PARSE_APP";

    public CacheSwapAppsService() {
        super("CacheSwapAppsService");
    }

    /**
     * Parse the locally installed APK for {@code packageName} and save its XML
     * to the APK XML cache.
     */
    private static void parseApp(Context context, String packageName) {
        Intent intent = new Intent();
        intent.setData(Utils.getPackageUri(packageName));
        intent.setClass(context, CacheSwapAppsService.class);
        intent.setAction(ACTION_PARSE_APP);
        context.startService(intent);
    }

    /**
     * Parse all of the locally installed APKs into a memory cache, starting
     * with the currently selected apps.  APKs that are already parsed in the
     * {@code index.jar} file will be read from that file.
     */
    public static void startCaching(Context context) {
        File indexJarFile = LocalRepoManager.get(context).getIndexJar();
        PackageManager pm = context.getPackageManager();
        for (ApplicationInfo applicationInfo : pm.getInstalledApplications(0)) {
            if (applicationInfo.publicSourceDir.startsWith(FDroidApp.SYSTEM_DIR_NAME)) {
                continue;
            }
            if (!indexJarFile.exists()
                    || FileUtils.isFileNewer(new File(applicationInfo.sourceDir), indexJarFile)) {
                parseApp(context, applicationInfo.packageName);
            }
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_LOWEST);
        if (intent == null || !ACTION_PARSE_APP.equals(intent.getAction())) {
            Utils.debugLog(TAG, "received bad Intent: " + intent);
            return;
        }

        try {
            PackageManager pm = getPackageManager();
            String packageName = intent.getData().getSchemeSpecificPart();
            App app = App.getInstance(this, pm, packageName);
            if (app != null) {
                SwapService.putAppInCache(packageName, app);
            }
        } catch (CertificateEncodingException | IOException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }
}
