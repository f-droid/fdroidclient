package org.fdroid.fdroid.panic;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import info.guardianproject.panic.Panic;
import info.guardianproject.panic.PanicResponder;

/**
 * This {@link AppCompatActivity} is purely to run events in response to a panic trigger.
 * It needs to be an {@code AppCompatActivity} rather than a {@link android.app.Service}
 * so that it can fetch some of the required information about what sent the
 * {@link Intent}.  This is therefore an {@code AppCompatActivity} without any UI, which
 * is a special case in Android.  All the code must be in
 * {@link #onCreate(Bundle)} and {@link #finish()} must be called at the end of
 * that method.
 *
 * @see PanicResponder#receivedTriggerFromConnectedApp(AppCompatActivity)
 */
public class PanicResponderActivity extends AppCompatActivity {

    private static final String TAG = PanicResponderActivity.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (!Panic.isTriggerIntent(intent)) {
            finish();
            return;
        }

        // received intent from panic app
        Log.i(TAG, "Received Panic Trigger...");

        final Preferences preferences = Preferences.get();

        boolean receivedTriggerFromConnectedApp = PanicResponder.receivedTriggerFromConnectedApp(this);
        final boolean runningAppUninstalls = PrivilegedInstaller.isDefault(this);

        ArrayList<String> wipeList = new ArrayList<>(preferences.getPanicWipeSet());
        preferences.setPanicWipeSet(Collections.<String>emptySet());
        preferences.setPanicTmpSelectedSet(Collections.<String>emptySet());

        if (receivedTriggerFromConnectedApp && runningAppUninstalls && wipeList.size() > 0) {

            // if this app (e.g. F-Droid) is to be deleted, do it last
            if (wipeList.contains(getPackageName())) {
                wipeList.remove(getPackageName());
                wipeList.add(getPackageName());
            }

            final Context context = this;
            final CountDownLatch latch = new CountDownLatch(1);
            final LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
            final String lastToUninstall = wipeList.get(wipeList.size() - 1);
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    switch ((intent.getAction())) {
                        case Installer.ACTION_UNINSTALL_INTERRUPTED:
                        case Installer.ACTION_UNINSTALL_COMPLETE:
                            latch.countDown();
                            break;
                    }
                }
            };
            lbm.registerReceiver(receiver, Installer.getUninstallIntentFilter(lastToUninstall));

            for (String packageName : wipeList) {
                App app = new App();
                Apk apk = new Apk();
                app.packageName = packageName;
                apk.packageName = packageName;
                InstallerService.uninstall(context, app, apk);
            }

            // wait for apps to uninstall before triggering final responses
            new Thread() {
                @Override
                public void run() {
                    try {
                        latch.await(10, TimeUnit.MINUTES);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                    lbm.unregisterReceiver(receiver);
                    if (preferences.panicResetRepos()) {
                        resetRepos(context);
                    }
                    if (preferences.panicHide()) {
                        HidingManager.hide(context);
                    }
                    if (preferences.panicExit()) {
                        exitAndClear();
                    }
                }
            }.start();
        } else if (receivedTriggerFromConnectedApp) {
            if (preferences.panicResetRepos()) {
                resetRepos(this);
            }
            // Performing destructive panic response
            if (preferences.panicHide()) {
                Log.i(TAG, "Hiding app...");
                HidingManager.hide(this);
            }
        }

        // exit and clear, if not deactivated
        if (!runningAppUninstalls && preferences.panicExit()) {
            exitAndClear();
        }
        finish();
    }

    static void resetRepos(Context context) {
        DBHelper.resetRepos(context);
    }

    private void exitAndClear() {
        ExitActivity.exitAndRemoveFromRecentApps(this);
        finishAndRemoveTask();
    }
}
