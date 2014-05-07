
package org.fdroid.fdroid.views;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.net.WifiStateChangeService;

public class QrWizardDownloadActivity extends Activity {
    private static final String TAG = "QrWizardDownloadActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

    private BroadcastReceiver onWifiChange = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent i) {
            Log.i(TAG, "onWifiChange.onReceive()");
            resetNetworkInfo();
        }
    };

    private void resetNetworkInfo() {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String qrString = "";
        if (prefs.getBoolean("use_https", false))
            qrString += "https";
        else
            qrString += "http";
        qrString += "://" + FDroidApp.ipAddressString;
        qrString += ":" + FDroidApp.port;

        new QrGenAsyncTask(this, R.id.qrWizardImage).execute(qrString);
        Log.i(TAG, "qr: " + qrString);

        TextView wifiNetworkName = (TextView) findViewById(R.id.qrWifiNetworkName);
        wifiNetworkName.setText(qrString.replaceFirst("http://", ""));
    }
}
