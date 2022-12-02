package org.fdroid.fdroid;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

public class NfcHelper {

    private static final String TAG = "NfcHelper";

    private static NfcAdapter getAdapter(Context context) {
        return NfcAdapter.getDefaultAdapter(context.getApplicationContext());
    }

    public static boolean setPushMessage(AppCompatActivity activity, Uri toShare) {
        NfcAdapter adapter = getAdapter(activity);
        if (adapter != null) {
            adapter.setNdefPushMessage(new NdefMessage(new NdefRecord[]{
                    NdefRecord.createUri(toShare),
            }), activity);
            return true;
        }
        return false;
    }

    public static void setAndroidBeam(AppCompatActivity activity, String packageName) {
        PackageManager pm = activity.getPackageManager();
        NfcAdapter nfcAdapter = getAdapter(activity);
        if (nfcAdapter != null) {
            ApplicationInfo appInfo;
            try {
                appInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
                Uri[] uris = {
                        Uri.parse("file://" + appInfo.publicSourceDir),
                };
                nfcAdapter.setBeamPushUris(uris, activity);
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not get application info", e);
            }
        }
    }

    public static void disableAndroidBeam(AppCompatActivity activity) {
        NfcAdapter nfcAdapter = getAdapter(activity);
        if (nfcAdapter != null) {
            nfcAdapter.setBeamPushUris(null, activity);
        }
    }

}
