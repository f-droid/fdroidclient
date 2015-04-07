package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import org.fdroid.fdroid.*;
import org.fdroid.fdroid.net.WifiStateChangeService;

public class QrWizardDownloadActivity extends ActionBarActivity {

    private static final String TAG = "fdroid.QrWizardDownloadActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qr_wizard_activity);
        TextView instructions = (TextView) findViewById(R.id.qrWizardInstructions);
        instructions.setText(R.string.qr_wizard_download_instructions);
        Button next = (Button) findViewById(R.id.qrNextButton);
        next.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        resetNetworkInfo();
        LocalBroadcastManager.getInstance(this).registerReceiver(onWifiChange,
                new IntentFilter(WifiStateChangeService.BROADCAST));
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(onWifiChange);
    }

    private final BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            Log.i(TAG, "onWifiChange.onReceive()");
            resetNetworkInfo();
        }
    };

    private void resetNetworkInfo() {
        String qrString = "";
        if (Preferences.get().isLocalRepoHttpsEnabled())
            qrString += "https";
        else
            qrString += "http";
        qrString += "://" + FDroidApp.ipAddressString;
        qrString += ":" + FDroidApp.port;

        if (Build.VERSION.SDK_INT >= 8) // zxing requires >= 8
            new QrGenAsyncTask(this, R.id.qrWizardImage).execute(qrString);
        Log.i(TAG, "qr: " + qrString);

        TextView wifiNetworkName = (TextView) findViewById(R.id.qrWifiNetworkName);
        wifiNetworkName.setText(qrString.replaceFirst("http://", ""));
    }
}
