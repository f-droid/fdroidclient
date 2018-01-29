package org.fdroid.fdroid.views.panic;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.views.hiding.HidingManager;

import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicResponder;

public class PanicResponderActivity extends AppCompatActivity {

    private static final String TAG = PanicResponderActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null || !Panic.isTriggerIntent(intent)) {
            finish();
            return;
        }

        // received intent from panic app
        Log.i(TAG, "Received Panic Trigger...");

        Preferences preferences = Preferences.get();

        if (PanicResponder.receivedTriggerFromConnectedApp(this)) {
            Log.i(TAG, "Panic Trigger came from connected app");

            // Performing destructive panic responses
            if (preferences.panicHide()) {
                Log.i(TAG, "Hiding app...");
                HidingManager.hide(this);
            }
        }

        // exit and clear, if not deactivated
        if (preferences.panicExit()) {
            ExitActivity.exitAndRemoveFromRecentApps(this);
            if (Build.VERSION.SDK_INT >= 21) {
                finishAndRemoveTask();
            }
        }
        finish();
    }

}
