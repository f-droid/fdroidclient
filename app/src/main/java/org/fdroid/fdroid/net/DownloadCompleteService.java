package org.fdroid.fdroid.net;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Process;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;

public class DownloadCompleteService extends IntentService {
    private static final String TAG = "DownloadCompleteService";

    private static final String ACTION_NOTIFY = "org.fdroid.fdroid.net.action.NOTIFY";
    private static final String EXTRA_PACKAGE_NAME = "org.fdroid.fdroid.net.extra.PACKAGE_NAME";

    public DownloadCompleteService() {
        super("DownloadCompleteService");
    }

    public static void notify(Context context, String packageName, String urlString) {
        Intent intent = new Intent(context, DownloadCompleteService.class);
        intent.setAction(ACTION_NOTIFY);
        intent.setData(Uri.parse(urlString));
        intent.putExtra(EXTRA_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        if (intent != null) {
            final String action = intent.getAction();
            if (!ACTION_NOTIFY.equals(action)) {
                Utils.debugLog(TAG, "Intent action is not ACTION_NOTIFY");
                return;
            }
            String packageName = intent.getStringExtra(EXTRA_PACKAGE_NAME);
            if (TextUtils.isEmpty(packageName)) {
                Utils.debugLog(TAG, "Intent is missing EXTRA_PACKAGE_NAME");
                return;
            }

            String title;
            try {
                PackageManager pm = getPackageManager();
                title = String.format(getString(R.string.tap_to_update_format),
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)));
            } catch (PackageManager.NameNotFoundException e) {
                App app = AppProvider.Helper.findByPackageName(getContentResolver(), packageName,
                        new String[]{
                                AppProvider.DataColumns.NAME,
                        });
                title = String.format(getString(R.string.tap_to_install_format), app.name);
            }

            int requestCode = Utils.getApkUrlNotificationId(intent.getDataString());
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(this)
                            .setAutoCancel(true)
                            .setContentTitle(title)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setContentIntent(DownloaderService.createAppDetailsIntent(this, requestCode, packageName))
                            .setContentText(getString(R.string.tap_to_install));
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(Utils.getApkUrlNotificationId(intent.getDataString()), builder.build());
        }
    }
}
