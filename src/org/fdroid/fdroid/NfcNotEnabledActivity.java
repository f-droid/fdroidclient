
package org.fdroid.fdroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

@TargetApi(14)
// aka Android 4.0 aka Ice Cream Sandwich
public class NfcNotEnabledActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = new Intent();
        if (Build.VERSION.SDK_INT >= 16) {
            /*
             * ACTION_NFC_SETTINGS was added in 4.1 aka Jelly Bean MR1 as a
             * separate thing from ACTION_NFCSHARING_SETTINGS. It is now
             * possible to have NFC enabled, but not "Android Beam", which is
             * needed for NDEF. Therefore, we detect the current state of NFC,
             * and steer the user accordingly.
             */
            if (NfcAdapter.getDefaultAdapter(this).isEnabled())
                intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
            else
                intent.setAction(Settings.ACTION_NFC_SETTINGS);
        } else if (Build.VERSION.SDK_INT >= 14) {
            // this API was added in 4.0 aka Ice Cream Sandwich
            intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
        } else {
            // no NFC support, so nothing to do here
            finish();
            return;
        }
        startActivity(intent);
        finish();
    }
}
