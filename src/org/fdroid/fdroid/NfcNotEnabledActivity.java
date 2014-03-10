
package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

// aka Android 4.0 aka Ice Cream Sandwich
public class NfcNotEnabledActivity extends Activity {

    /*
     * ACTION_NFC_SETTINGS was added in 4.1 aka Jelly Bean MR1 as a
     * separate thing from ACTION_NFCSHARING_SETTINGS. It is now
     * possible to have NFC enabled, but not "Android Beam", which is
     * needed for NDEF. Therefore, we detect the current state of NFC,
     * and steer the user accordingly.
     */
    @TargetApi(14)
    private void doOnJellybean(Intent intent) {
        if (NfcAdapter.getDefaultAdapter(this).isEnabled())
            intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
        else
            intent.setAction(Settings.ACTION_NFC_SETTINGS);
    }

    // this API was added in 4.0 aka Ice Cream Sandwich
    @TargetApi(16)
    private void doOnIceCreamSandwich(Intent intent) {
        intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 16) {
            doOnJellybean(intent);
        } else if (Build.VERSION.SDK_INT >= 14) {
            doOnIceCreamSandwich(intent);
        } else {
            // no NFC support, so nothing to do here
            finish();
            return;
        }
        startActivity(intent);
        finish();
    }
}
