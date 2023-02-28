package org.fdroid.fdroid;

import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;

import androidx.appcompat.app.AppCompatActivity;

// aka Android 4.0 aka Ice Cream Sandwich
public class NfcNotEnabledActivity extends AppCompatActivity {

    /*
     * ACTION_NFC_SETTINGS was added in 4.1 aka Jelly Bean MR1 as a
     * separate thing from ACTION_NFCSHARING_SETTINGS. It is now
     * possible to have NFC enabled, but not "Android Beam", which is
     * needed for NDEF. Therefore, we detect the current state of NFC,
     * and steer the user accordingly.
     */
    private void doOnJellybean(Intent intent) {
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            return;
        }
        if (nfcAdapter.isEnabled()) {
            intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_NFC_SETTINGS);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        final Intent intent = new Intent();
        doOnJellybean(intent);
        startActivity(intent);
        finish();
    }
}
