package org.fdroid.fdroid.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.data.InstalledAppProviderService;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

/**
 * Receive {@link Intent#ACTION_PACKAGE_ADDED} and {@link Intent#ACTION_PACKAGE_REMOVED}
 * events from {@link android.content.pm.PackageManager} to keep
 * {@link org.fdroid.fdroid.data.InstalledAppProvider} updated. This ignores
 * {@link Intent#EXTRA_REPLACING} and instead handles updates by just deleting then
 * inserting the app being updated in direct response to the {@code Intent}s from
 * the system.  This is also necessary because there are no other checks to prevent
 * multiple copies of the same app being inserted into {@link InstalledAppProviderService}.
 */
public class PackageManagerReceiver extends BroadcastReceiver {
    private static final String TAG = "PackageManagerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null) {
            String action = intent.getAction();
            if (Intent.ACTION_PACKAGE_ADDED.equals(action)) {
                InstalledAppProviderService.insert(context, intent.getData());
            } else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
                if (TextUtils.equals(context.getPackageName(), intent.getData().getSchemeSpecificPart())) {
                    Log.i(TAG, "Ignoring request to remove ourselves from cache.");
                } else {
                    InstalledAppProviderService.delete(context, intent.getData());
                }
            } else if (Intent.ACTION_PACKAGE_CHANGED.equals(action) && Build.VERSION.SDK_INT >= 29 &&
                    PrivilegedInstaller.isExtensionInstalledCorrectly(context) ==
                            PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES) {
                String[] allowList = new String[]{"org.chromium.chrome"};
                for (String allowed : allowList) {
                    if (allowed.equals(intent.getData().getSchemeSpecificPart())) {
                        InstalledAppProviderService.compareToPackageManager(context);
                    }
                }
            } else {
                Log.i(TAG, "unsupported action: " + action + " " + intent);
            }
        }
    }
}
