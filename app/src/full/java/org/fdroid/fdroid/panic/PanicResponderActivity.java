package org.fdroid.fdroid.panic;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.data.InstalledApp;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
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
                InstalledApp installedApp = InstalledAppProvider.Helper.findByPackageName(context, packageName);
                InstallerService.uninstall(context, new Apk(installedApp));
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
        HashSet<String> enabledAddresses = new HashSet<>();
        HashSet<String> disabledAddresses = new HashSet<>();
        String[] defaultReposItems = DBHelper.loadInitialRepos(context).toArray(new String[0]);
        for (int i = 1; i < defaultReposItems.length; i += DBHelper.REPO_XML_ITEM_COUNT) {
            if ("1".equals(defaultReposItems[i + 3])) {
                enabledAddresses.add(defaultReposItems[i]);
            } else {
                disabledAddresses.add(defaultReposItems[i]);
            }
        }

        List<Repo> repos = RepoProvider.Helper.all(context);
        for (Repo repo : repos) {
            ContentValues values = new ContentValues(1);
            if (enabledAddresses.contains(repo.address)) {
                values.put(Schema.RepoTable.Cols.IN_USE, true);
                RepoProvider.Helper.update(context, repo, values);
            } else if (disabledAddresses.contains(repo.address)) {
                values.put(Schema.RepoTable.Cols.IN_USE, false);
                RepoProvider.Helper.update(context, repo, values);
            } else {
                RepoProvider.Helper.remove(context, repo.getId());
            }
        }
    }

    private void exitAndClear() {
        ExitActivity.exitAndRemoveFromRecentApps(this);
        if (Build.VERSION.SDK_INT >= 21) {
            finishAndRemoveTask();
        }
    }
}
