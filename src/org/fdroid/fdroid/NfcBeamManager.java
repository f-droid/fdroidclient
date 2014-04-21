
package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;

@TargetApi(16)
public class NfcBeamManager {

    static void setAndroidBeam(Activity activity, String packageName) {
        if (Build.VERSION.SDK_INT < 16)
            return;
        PackageManager pm = activity.getPackageManager();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null) {
            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                Uri uris[] = {
                        Uri.parse("file://" + appInfo.publicSourceDir),
                };
                nfcAdapter.setBeamPushUris(uris, activity);
            } catch (NameNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    static void disableAndroidBeam(Activity activity) {
        if (Build.VERSION.SDK_INT < 16)
            return;
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(activity);
        if (nfcAdapter != null)
            nfcAdapter.setBeamPushUris(null, activity);
    }

}
