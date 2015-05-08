package org.fdroid.fdroid.views.swap;

import android.app.ProgressDialog;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;
import android.widget.Toast;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.localrepo.LocalRepoManager;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class SwapActivity extends ActionBarActivity implements SwapProcessManager {

    private static final String STATE_START_SWAP = "startSwap";
    private static final String STATE_SELECT_APPS = "selectApps";
    private static final String STATE_JOIN_WIFI = "joinWifi";
    private static final String STATE_NFC = "nfc";
    private static final String STATE_WIFI_QR = "wifiQr";

    private Timer shutdownLocalRepoTimer;
    private UpdateAsyncTask updateSwappableAppsTask = null;
    private boolean hasPreparedLocalRepo = false;

    @Override
    public void onBackPressed() {
        switch (currentState()) {
        case STATE_START_SWAP:
            finish();
            break;
        default:
            super.onBackPressed();
            break;
        }
    }

    private String currentState() {
        FragmentManager.BackStackEntry lastFragment = getSupportFragmentManager().getBackStackEntryAt(getSupportFragmentManager().getBackStackEntryCount() - 1);
        return lastFragment.getName();
    }

    public void nextStep() {
        switch (currentState()) {
        case STATE_START_SWAP:
            showSelectApps();
            break;
        case STATE_SELECT_APPS:
            prepareLocalRepo();
            break;
        case STATE_JOIN_WIFI:
            ensureLocalRepoRunning();
            if (!attemptToShowNfc()) {
                showWifiQr();
            }
            break;
        case STATE_NFC:
            showWifiQr();
            break;
        case STATE_WIFI_QR:
            break;
        }
        supportInvalidateOptionsMenu();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_next) {
            nextStep();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {

            setContentView(R.layout.swap_activity);

            // Necessary to run on an Android 2.3.[something] device.
            new Handler().post(new Runnable() {
                @Override
                public void run() {

                    showFragment(new StartSwapFragment(), STATE_START_SWAP);

                    if (FDroidApp.isLocalRepoServiceRunning()) {
                        showSelectApps();
                        showJoinWifi();
                        attemptToShowNfc();
                        showWifiQr();
                    }

                }
            });
        }

    }

    private void showSelectApps() {

        showFragment(new SelectAppsFragment(), STATE_SELECT_APPS);

    }

    private void showJoinWifi() {

        showFragment(new JoinWifiFragment(), STATE_JOIN_WIFI);

    }

    private boolean attemptToShowNfc() {
        // TODO: What if NFC is disabled? Hook up with NfcNotEnabledActivity? Or maybe only if they
        // click a relevant button?

        // Even if they opted to skip the message which says "Touch devices to swap",
        // we still want to actually enable the feature, so that they could touch
        // during the wifi qr code being shown too.
        boolean nfcMessageReady = NfcHelper.setPushMessage(this, Utils.getSharingUri(FDroidApp.repo));

        if (Preferences.get().showNfcDuringSwap() && nfcMessageReady) {
            showFragment(new NfcSwapFragment(), STATE_NFC);
            return true;
        }
        return false;
    }

    private void showBluetooth() {

    }

    private void showWifiQr() {
        showFragment(new WifiQrFragment(), STATE_WIFI_QR);
    }

    private void showFragment(Fragment fragment, String name) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment, name)
                .addToBackStack(name)
                .commit();
    }

    private void prepareLocalRepo() {
        SelectAppsFragment fragment = (SelectAppsFragment)getSupportFragmentManager().findFragmentByTag(STATE_SELECT_APPS);
        boolean needsUpdating = !hasPreparedLocalRepo || fragment.hasSelectionChanged();
        if (updateSwappableAppsTask == null && needsUpdating) {
            updateSwappableAppsTask = new UpdateAsyncTask(this, fragment.getSelectedApps());
            updateSwappableAppsTask.execute();
        } else {
            showJoinWifi();
        }
    }

    /**
     * Once the UpdateAsyncTask has finished preparing our repository index, we can
     * show the next screen to the user.
     */
    private void onLocalRepoPrepared() {

        updateSwappableAppsTask = null;
        hasPreparedLocalRepo = true;
        showJoinWifi();

    }

    private void ensureLocalRepoRunning() {
        if (!FDroidApp.isLocalRepoServiceRunning()) {
            FDroidApp.startLocalRepoService(this);
            initLocalRepoTimer(900000); // 15 mins
        }
    }

    private void initLocalRepoTimer(long timeoutMilliseconds) {

        // reset the timer if viewing this Activity again
        if (shutdownLocalRepoTimer != null)
            shutdownLocalRepoTimer.cancel();

        // automatically turn off after 15 minutes
        shutdownLocalRepoTimer = new Timer();
        shutdownLocalRepoTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                FDroidApp.stopLocalRepoService(SwapActivity.this);
            }
        }, timeoutMilliseconds);

    }

    @Override
    public void stopSwapping() {
        if (FDroidApp.isLocalRepoServiceRunning()) {
            if (shutdownLocalRepoTimer != null) {
                shutdownLocalRepoTimer.cancel();
            }
            FDroidApp.stopLocalRepoService(SwapActivity.this);
        }
        finish();
    }

    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {

        @SuppressWarnings("UnusedDeclaration")
        private static final String TAG = "SwapActivity.UpdateAsyncTask";

        @NonNull
        private final ProgressDialog progressDialog;

        @NonNull
        private final Set<String> selectedApps;

        @NonNull
        private final Uri sharingUri;

        public UpdateAsyncTask(Context c, @NonNull Set<String> apps) {
            selectedApps = apps;
            progressDialog = new ProgressDialog(c);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
            sharingUri = Utils.getSharingUri(FDroidApp.repo);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... params) {
            try {
                final LocalRepoManager lrm = LocalRepoManager.get(SwapActivity.this);
                publishProgress(getString(R.string.deleting_repo));
                lrm.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    lrm.addApp(SwapActivity.this, app);
                }
                lrm.writeIndexPage(sharingUri.toString());
                publishProgress(getString(R.string.writing_index_jar));
                lrm.writeIndexJar();
                publishProgress(getString(R.string.linking_apks));
                lrm.copyApksToRepo();
                publishProgress(getString(R.string.copying_icons));
                // run the icon copy without progress, its not a blocker
                new AsyncTask<Void, Void, Void>() {

                    @Override
                    protected Void doInBackground(Void... params) {
                        lrm.copyIconsToRepo();
                        return null;
                    }
                }.execute();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(String... progress) {
            super.onProgressUpdate(progress);
            progressDialog.setMessage(progress[0]);
        }

        @Override
        protected void onPostExecute(Void result) {
            progressDialog.dismiss();
            Toast.makeText(SwapActivity.this, R.string.updated_local_repo, Toast.LENGTH_SHORT).show();
            onLocalRepoPrepared();
        }
    }

}
