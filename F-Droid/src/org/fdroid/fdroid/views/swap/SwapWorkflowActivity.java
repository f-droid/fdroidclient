package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.localrepo.SwapManager;
import org.fdroid.fdroid.net.bluetooth.BluetoothServer;

import java.util.Set;

public class SwapWorkflowActivity extends ActionBarActivity {

    private ViewGroup container;

    /**
     * A UI component (subclass of {@link View}) which forms part of the swap workflow.
     * There is a one to one mapping between an {@link org.fdroid.fdroid.views.swap.SwapWorkflowActivity.InnerView}
     * and a {@link org.fdroid.fdroid.localrepo.SwapManager.SwapStep}, and these views know what
     * the previous view before them should be.
     */
    public interface InnerView {
        /** @return True if the menu should be shown. */
        boolean buildMenu(Menu menu, @NonNull MenuInflater inflater);

        /** @return The step that this view represents. */
        @SwapManager.SwapStep int getStep();

        @SwapManager.SwapStep int getPreviousStep();

        @ColorRes int getToolbarColour();

        String getToolbarTitle();
    }

    private static final String TAG = "SwapWorkflowActivity";

    private static final int CONNECT_TO_SWAP = 1;
    private static final int REQUEST_BLUETOOTH_ENABLE = 2;
    private static final int REQUEST_BLUETOOTH_DISCOVERABLE = 3;

    private Toolbar toolbar;
    private SwapManager state;
    private InnerView currentView;
    private boolean hasPreparedLocalRepo = false;
    private UpdateAsyncTask updateSwappableAppsTask = null;

    @Override
    public void onBackPressed() {
        if (currentView.getStep() == SwapManager.STEP_INTRO) {
            finish();
        } else {
            int nextStep = currentView.getPreviousStep();
            state.setStep(nextStep);
            showRelevantView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        state = SwapManager.load(this);
        setContentView(R.layout.swap_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(getApplicationContext(), R.style.SwapTheme_Wizard_Text_Toolbar);
        setSupportActionBar(toolbar);

        container = (ViewGroup) findViewById(R.id.fragment_container);
        showRelevantView();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        boolean parent = super.onPrepareOptionsMenu(menu);
        boolean inner  = currentView.buildMenu(menu, getMenuInflater());
        return parent || inner;
    }

    @Override
    protected void onResume() {
        super.onResume();
        showRelevantView();
    }

    private void showRelevantView() {
        if (currentView != null && currentView.getStep() == state.getStep()) {
            // Already showing the currect step, so don't bother changing anything.
            return;
        }

        switch(state.getStep()) {
            case SwapManager.STEP_INTRO:
                showIntro();
                break;
            case SwapManager.STEP_SELECT_APPS:
                showSelectApps();
                break;
            case SwapManager.STEP_SHOW_NFC:
                showNfc();
                break;
            case SwapManager.STEP_JOIN_WIFI:
                showJoinWifi();
                break;
            case SwapManager.STEP_WIFI_QR:
                showWifiQr();
                break;
        }
    }

    public SwapManager getState() {
        return state;
    }

    private void showNfc() {
        if (!attemptToShowNfc()) {
            showWifiQr();
        }
    }

    private void inflateInnerView(@LayoutRes int viewRes) {
        container.removeAllViews();
        View view = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(viewRes, container, false);
        currentView = (InnerView)view;
        state.setStep(currentView.getStep());
        toolbar.setBackgroundColor(currentView.getToolbarColour());
        toolbar.setTitle(currentView.getToolbarTitle());
        toolbar.setNavigationIcon(R.drawable.ic_close_white);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToolbarCancel();
            }
        });
        container.addView(view);
        supportInvalidateOptionsMenu();
    }

    private void onToolbarCancel() {
        SwapManager.load(this).disableSwapping();
        finish();
    }

    private void showIntro() {
        inflateInnerView(R.layout.swap_blank);
    }

    public void showSelectApps() {
        inflateInnerView(R.layout.swap_select_apps);
    }

    // TODO: Figure out whether they have changed since last time UpdateAsyncTask was run.
    // If the local repo is running, then we can ask it what apps it is swapping and compare with that.
    // Otherwise, probably will need to scan the file system.
    public void onAppsSelected() {
        if (updateSwappableAppsTask == null && !hasPreparedLocalRepo) {
            updateSwappableAppsTask = new UpdateAsyncTask(state.getAppsToSwap());
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

    private void showJoinWifi() {
        inflateInnerView(R.layout.swap_join_wifi);
    }

    private void showBluetoothDeviceList() {
        inflateInnerView(R.layout.swap_bluetooth_devices);
    }

    public void onJoinWifiComplete() {
        ensureLocalRepoRunning();
        if (!attemptToShowNfc()) {
            showWifiQr();
        }
    }

    public void showWifiQr() {
        inflateInnerView(R.layout.swap_wifi_qr);
    }

    private boolean attemptToShowNfc() {
        // TODO: What if NFC is disabled? Hook up with NfcNotEnabledActivity? Or maybe only if they
        // click a relevant button?

        // Even if they opted to skip the message which says "Touch devices to swap",
        // we still want to actually enable the feature, so that they could touch
        // during the wifi qr code being shown too.
        boolean nfcMessageReady = NfcHelper.setPushMessage(this, Utils.getSharingUri(FDroidApp.repo));

        if (Preferences.get().showNfcDuringSwap() && nfcMessageReady) {
            inflateInnerView(R.layout.swap_nfc);
            return true;
        }
        return false;
    }

    private void ensureLocalRepoRunning() {
        getState().enableSwapping();
    }

    public void stopSwapping() {
        getState().disableSwapping();
        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                NewRepoConfig repoConfig = new NewRepoConfig(this, scanResult.getContents());
                if (repoConfig.isValidRepo()) {
                    startActivityForResult(new Intent(FDroid.ACTION_ADD_REPO, Uri.parse(scanResult.getContents()), this, ConnectSwapActivity.class), CONNECT_TO_SWAP);
                } else {
                    Toast.makeText(this, "The QR code you scanned doesn't look like a swap code.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CONNECT_TO_SWAP && resultCode == Activity.RESULT_OK) {
            finish();
        } else if (requestCode == REQUEST_BLUETOOTH_ENABLE) {

            if (resultCode == RESULT_OK) {
                Log.d(TAG, "User enabled Bluetooth, will make sure we are discoverable.");
                ensureBluetoothDiscoverable();
            } else {
                // Didn't enable bluetooth
                Log.d(TAG, "User chose not to enable Bluetooth, so doing nothing (i.e. sticking with wifi).");
            }

        } else if (requestCode == REQUEST_BLUETOOTH_DISCOVERABLE) {

            if (resultCode != RESULT_CANCELED) {
                Log.d(TAG, "User made Bluetooth discoverable, will proceed to start bluetooth server.");
                startBluetoothServer();
            } else {
                Log.d(TAG, "User chose not to make Bluetooth discoverable, so doing nothing (i.e. sticking with wifi).");
            }

        }
    }

    /**
     * The process for setting up bluetooth is as follows:
     *  * Assume we have bluetooth available (otherwise the button which allowed us to start
     *    the bluetooth process should not have been available). TODO: Remove button if bluetooth unavailable.
     *  * Ask user to enable (if not enabled yet).
     *  * Start bluetooth server socket.
     *  * Enable bluetooth discoverability, so that people can connect to our server socket.
     *
     * Note that this is a little different than the usual process for bluetooth _clients_, which
     * involves pairing and connecting with other devices.
     */
    public void connectWithBluetooth() {

        Log.d(TAG, "Initiating Bluetooth swap instead of wifi.");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null)
            if (adapter.isEnabled()) {
                Log.d(TAG, "Bluetooth enabled, will pair with device.");
                ensureBluetoothDiscoverable();
            } else {
                Log.d(TAG, "Bluetooth disabled, asking user to enable it.");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE);
            }
    }

    private void ensureBluetoothDiscoverable() {
        Log.d(TAG, "Ensuring Bluetooth is in discoverable mode.");
        if (BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

            // TODO: Listen for BluetoothAdapter.ACTION_SCAN_MODE_CHANGED and respond if discovery
            // is cancelled prematurely.

            Log.d(TAG, "Not currently in discoverable mode, so prompting user to enable.");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivityForResult(intent, REQUEST_BLUETOOTH_DISCOVERABLE);
        }

        Log.d(TAG, "Staring the Bluetooth Server whether we are discoverable or not, since paired devices can still connect.");
        startBluetoothServer();

    }

    private void startBluetoothServer() {
        Log.d(TAG, "Starting bluetooth server.");
        if (!state.isEnabled()) {
            state.enableSwapping();
        }
        new BluetoothServer(this,getFilesDir()).start();
        showBluetoothDeviceList();
    }


    class UpdateAsyncTask extends AsyncTask<Void, String, Void> {

        @SuppressWarnings("UnusedDeclaration")
        private static final String TAG = "UpdateAsyncTask";

        @NonNull
        private final ProgressDialog progressDialog;

        @NonNull
        private final Set<String> selectedApps;

        @NonNull
        private final Uri sharingUri;

        @NonNull
        private final Context context;

        public UpdateAsyncTask(@NonNull Set<String> apps) {
            context = SwapWorkflowActivity.this;
            selectedApps = apps;
            progressDialog = new ProgressDialog(context);
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
                final LocalRepoManager lrm = LocalRepoManager.get(context);
                publishProgress(getString(R.string.deleting_repo));
                lrm.deleteRepo();
                for (String app : selectedApps) {
                    publishProgress(String.format(getString(R.string.adding_apks_format), app));
                    lrm.addApp(context, app);
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
            Toast.makeText(context, R.string.updated_local_repo, Toast.LENGTH_SHORT).show();
            onLocalRepoPrepared();
        }
    }

}
