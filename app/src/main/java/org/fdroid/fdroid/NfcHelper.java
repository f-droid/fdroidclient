package org.fdroid.fdroid;

import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;

@Deprecated
public class NfcHelper {

    public static boolean setPushMessage(AppCompatActivity activity, Uri toShare) {
        // removed in Android 14: https://www.xda-developers.com/android-beam-permanent-removal-android-14/
        // adapter.setNdefPushMessage();
        return false;
    }

    public static void setAndroidBeam(AppCompatActivity activity, String packageName) {
        // removed in SDK 34: https://www.xda-developers.com/android-beam-permanent-removal-android-14/
        // nfcAdapter.setBeamPushUris(uris, activity);
    }

    public static void disableAndroidBeam(AppCompatActivity activity) {
        // removed in Android 14: https://www.xda-developers.com/android-beam-permanent-removal-android-14/
        // nfcAdapter.setBeamPushUris(null, activity);
    }
}
