package org.fdroid.fdroid;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class NfcNotEnabledActivity extends AppCompatActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        final Intent intent = new Intent();
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (nfcAdapter == null) {
            return;
        }
        if (nfcAdapter.isEnabled()) {
            intent.setAction(Settings.ACTION_NFCSHARING_SETTINGS);
        } else {
            intent.setAction(Settings.ACTION_NFC_SETTINGS);
        }
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e("NfcNotEnabledActivity", "Error starting activity: ", e);
            Toast.makeText(this, R.string.app_error_open, Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
