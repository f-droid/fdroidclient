package org.belmarket.shop.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.belmarket.shop.Utils;
import org.belmarket.shop.data.InstalledAppProviderService;

/**
 * Receive {@link Intent#ACTION_PACKAGE_ADDED} and {@link Intent#ACTION_PACKAGE_REMOVED}
 * events from {@link android.content.pm.PackageManager} to keep
 * {@link org.belmarket.shop.data.InstalledAppProvider} updated. This ignores
 * {@link Intent#EXTRA_REPLACING} and instead handles updates by just deleting then
 * inserting the app being updated in direct response to the {@code Intent}s from
 * the system.  This is also necessary because there are no other checks to prevent
 * multiple copies of the same app being inserted into {@Link InstalledAppProvider}.
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
                InstalledAppProviderService.delete(context, intent.getData());
            } else {
                Utils.debugLog(TAG, "unsupported action: " + action + " " + intent);
            }
        }
    }
}
