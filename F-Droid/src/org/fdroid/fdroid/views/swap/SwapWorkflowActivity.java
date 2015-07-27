package org.fdroid.fdroid.views.swap;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.ColorRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
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

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.localrepo.LocalRepoManager;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.peers.Peer;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * This activity will do its best to show the most relevant screen about swapping to the user.
 * The problem comes when there are two competing goals - 1) Show the user a list of apps from another
 * device to download and install, and 2) Prepare your own list of apps to share.
 */
public class SwapWorkflowActivity extends AppCompatActivity {

    /**
     * When connecting to a swap, we then go and initiate a connection with that
     * device and ask if it would like to swap with us. Upon receiving that request
     * and agreeing, we don't then want to be asked whether we want to swap back.
     * This flag protects against two devices continually going back and forth
     * among each other offering swaps.
     */
    public static final String EXTRA_PREVENT_FURTHER_SWAP_REQUESTS = "preventFurtherSwap";
    public static final String EXTRA_CONFIRM = "EXTRA_CONFIRM";
    public static final String EXTRA_REPO_ID = "repoId";

    private ViewGroup container;

    /**
     * A UI component (subclass of {@link View}) which forms part of the swap workflow.
     * There is a one to one mapping between an {@link org.fdroid.fdroid.views.swap.SwapWorkflowActivity.InnerView}
     * and a {@link SwapService.SwapStep}, and these views know what
     * the previous view before them should be.
     */
    public interface InnerView {
        /** @return True if the menu should be shown. */
        boolean buildMenu(Menu menu, @NonNull MenuInflater inflater);

        /** @return The step that this view represents. */
        @SwapService.SwapStep int getStep();

        @SwapService.SwapStep int getPreviousStep();

        @ColorRes int getToolbarColour();

        String getToolbarTitle();
    }

    private static final String TAG = "SwapWorkflowActivity";

    private static final int CONNECT_TO_SWAP = 1;
    private static final int REQUEST_BLUETOOTH_ENABLE = 2;
    private static final int REQUEST_BLUETOOTH_DISCOVERABLE = 3;

    private Toolbar toolbar;
    private InnerView currentView;
    private boolean hasPreparedLocalRepo = false;
    private PrepareSwapRepo updateSwappableAppsTask = null;
    private NewRepoConfig confirmSwapConfig = null;

    @NonNull
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.d(TAG, "Swap service connected. Will hold onto it so we can talk to it regularly.");
            service = ((SwapService.Binder)binder).getService();
            showRelevantView();
        }

        // TODO: What causes this? Do we need to stop swapping explicitly when this is invoked?
        @Override
        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG, "Swap service disconnected");
            service = null;
            // TODO: What to do about the UI in this instance?
        }
    };

    @Nullable
    private SwapService service = null;

    @NonNull
    public SwapService getService() {
        if (service == null) {
            // *Slightly* more informative than a null-pointer error that would otherwise happen.
            throw new IllegalStateException("Trying to access swap service before it was initialized.");
        }
        return service;
    }

    @Override
    public void onBackPressed() {
        if (currentView.getStep() == SwapService.STEP_INTRO) {
            if (service != null) {
                service.disableAllSwapping();
            }
            finish();
        } else {
            int nextStep = currentView.getPreviousStep();
            getService().setStep(nextStep);
            showRelevantView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // The server should not be doing anything or occupying any (noticeable) resources
        // until we actually ask it to enable swapping. Therefore, we will start it nice and
        // early so we don't have to wait until it is connected later.
        Intent service = new Intent(this, SwapService.class);
        if (bindService(service, serviceConnection, Context.BIND_AUTO_CREATE)) {
            startService(service);
        }

        setContentView(R.layout.swap_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(getApplicationContext(), R.style.SwapTheme_Wizard_Text_Toolbar);
        setSupportActionBar(toolbar);

        container = (ViewGroup) findViewById(R.id.fragment_container);

        new SwapDebug().logStatus();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        boolean parent = super.onPrepareOptionsMenu(menu);
        boolean inner  = currentView != null && currentView.buildMenu(menu, getMenuInflater());
        return parent || inner;
    }

    @Override
    protected void onResume() {
        super.onResume();

        checkIncomingIntent();
        showRelevantView();
    }

    private void checkIncomingIntent() {
        Intent intent = getIntent();
        if (intent.getBooleanExtra(EXTRA_CONFIRM, false)) {
            // Storing config in this variable will ensure that when showRelevantView() is next
            // run, it will show the connect swap view (if the service is available).
            confirmSwapConfig = new NewRepoConfig(this, intent);
        }
    }

    private void showRelevantView() {

        if (service == null) {
            showInitialLoading();
            return;
        }

        // This is separate from the switch statement below, because it is usually populated
        // during onResume, when there is a high probability of not having a swap service
        // available. Thus, we were unable to set the state of the swap service appropriately.
        if (confirmSwapConfig != null) {
            showConfirmSwap(confirmSwapConfig);
            confirmSwapConfig = null;
            return;
        }

        if (container.getVisibility() == View.GONE || currentView != null && currentView.getStep() == service.getStep()) {
            // Already showing the correct step, so don't bother changing anything.
            return;
        }

        switch(service.getStep()) {
            case SwapService.STEP_INTRO:
                showIntro();
                break;
            case SwapService.STEP_SELECT_APPS:
                showSelectApps();
                break;
            case SwapService.STEP_SHOW_NFC:
                showNfc();
                break;
            case SwapService.STEP_JOIN_WIFI:
                showJoinWifi();
                break;
            case SwapService.STEP_WIFI_QR:
                showWifiQr();
                break;
            case SwapService.STEP_SUCCESS:
                showSwapConnected();
                break;
            case SwapService.STEP_CONNECTING:
                // TODO: Properly decide what to do here...
                inflateInnerView(R.layout.swap_blank);
                break;
        }
    }

    public SwapService getState() {
        return service;
    }

    private void showNfc() {
        if (!attemptToShowNfc()) {
            showWifiQr();
        }
    }

    private InnerView inflateInnerView(@LayoutRes int viewRes) {
        container.removeAllViews();
        View view = ((LayoutInflater)getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(viewRes, container, false);
        currentView = (InnerView)view;

        // Don't actually set the step to STEP_INITIAL_LOADING, as we are going to use this view
        // purely as a placeholder for _whatever view is meant to be shown_.
        if (currentView.getStep() != SwapService.STEP_INITIAL_LOADING) {
            if (service == null) {
                throw new IllegalStateException("We are not in the STEP_INITIAL_LOADING state, but the service is not ready.");
            }
            service.setStep(currentView.getStep());
        }

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

        return currentView;
    }

    private void onToolbarCancel() {
        getService().disableAllSwapping();
        finish();
    }

    private void showInitialLoading() {
        inflateInnerView(R.layout.swap_initial_loading);
    }

    private void showIntro() {
        // If we were previously swapping with a specific client, forget that we were doing that,
        // as we are starting over now.
        getService().swapWith(null);

        if (!getService().isEnabled()) {
            prepareInitialRepo();
        }
        getService().scanForPeers();
        inflateInnerView(R.layout.swap_blank);
    }

    private void showConfirmSwap(@NonNull NewRepoConfig config) {
        ((ConfirmReceive)inflateInnerView(R.layout.swap_confirm_receive)).setup(config);
    }

    public void showSelectApps() {
        inflateInnerView(R.layout.swap_select_apps);
    }

    public void sendFDroid() {
        // TODO: What is available here? Currently we support Bluetooth (see main menu in F-Droid)
        // and Android Beam (try touching two devices together when in the app details view).
    }

    // TODO: Figure out whether they have changed since last time UpdateAsyncTask was run.
    // If the local repo is running, then we can ask it what apps it is swapping and compare with that.
    // Otherwise, probably will need to scan the file system.
    public void onAppsSelected() {
        if (updateSwappableAppsTask == null && !hasPreparedLocalRepo) {
            updateSwappableAppsTask = new PrepareFullSwapRepo(getService().getAppsToSwap());
            updateSwappableAppsTask.execute();
        } else {
            onLocalRepoPrepared();
        }
    }

    private void prepareInitialRepo() {
        // TODO: Make it so that this and updateSwappableAppsTask (the _real_ swap repo task)
        // don't stomp on eachothers toes. The other one should wait for this to finish, or cancel
        // this, but this should never take precedence over the other.
        // TODO: Also don't allow this to run multiple times (e.g. if a user keeps navigating back
        // to the main screen.
        Log.d(TAG, "Preparing initial repo with only F-Droid, until we have allowed the user to configure their own repo.");
        new PrepareInitialSwapRepo().execute();
    }

    /**
     * Once the UpdateAsyncTask has finished preparing our repository index, we can
     * show the next screen to the user. This will be one of two things:
     *  * If we directly selected a peer to swap with initially, we will skip straight to getting
     *    the list of apps from that device.
     *  * Alternatively, if we didn't have a person to connect to, and instead clicked "Scan QR Code",
     *    then we want to show a QR code or NFC dialog.
     */
    private void onLocalRepoPrepared() {
        updateSwappableAppsTask = null;
        hasPreparedLocalRepo = true;
        if (getService().isConnectingWithPeer()) {
            startSwappingWithPeer();
        } else if (!attemptToShowNfc()) {
            showWifiQr();
        }
    }

    private void startSwappingWithPeer() {
        inflateInnerView(R.layout.swap_connecting);
    }

    private void showJoinWifi() {
        inflateInnerView(R.layout.swap_join_wifi);
    }

    public void showWifiQr() {
        inflateInnerView(R.layout.swap_wifi_qr);
    }

    public void showSwapConnected() {
        inflateInnerView(R.layout.swap_success);
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

    public void swapWith(Peer peer) {
        getService().stopScanningForPeers();
        getService().swapWith(peer);
        showSelectApps();
    }

    /**
     * This is for when we initiate a swap by viewing the "Are you sure you want to swap with" view
     * This can arise either:
     *   * As a result of scanning a QR code (in which case we likely already have a repo setup) or
     *   * As a result of the other device selecting our device in the "start swap" screen, in which
     *     case we are likely just sitting on the start swap screen also, and haven't configured
     *     anything yet.
     */
    public void swapWith(NewRepoConfig repoConfig) {
        Peer peer = repoConfig.toPeer();
        if (getService().getStep() == SwapService.STEP_INTRO || getService().getStep() == SwapService.STEP_CONFIRM_SWAP) {
            // This will force the "Select apps to swap" workflow to begin.
            // TODO: Find a better way to decide whether we need to select the apps. Not sure if we
            //       can or cannot be in STEP_INTRO with a full blown repo ready to swap.
            swapWith(peer);
        } else {
            getService().swapWith(repoConfig.toPeer());
            startSwappingWithPeer();
        }
    }

    public void denySwap() {
        showIntro();
    }

    /**
     * Attempts to open a QR code scanner, in the hope a user will then scan the QR code of another
     * device configured to swapp apps with us. Delegates to the zxing library to do so.
     */
    public void initiateQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.initiateScan();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
        if (scanResult != null) {
            if (scanResult.getContents() != null) {
                NewRepoConfig repoConfig = new NewRepoConfig(this, scanResult.getContents());
                if (repoConfig.isValidRepo()) {
                    confirmSwapConfig = repoConfig;
                    showRelevantView();
                } else {
                    Toast.makeText(this, "The QR code you scanned doesn't look like a swap code.", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == CONNECT_TO_SWAP && resultCode == Activity.RESULT_OK) {
            finish();
        } else if (requestCode == REQUEST_BLUETOOTH_ENABLE) {

            if (resultCode == RESULT_OK) {
                Log.d(TAG, "User enabled Bluetooth, will make sure we are discoverable.");
                ensureBluetoothDiscoverableThenStart();
            } else {
                // Didn't enable bluetooth
                Log.d(TAG, "User chose not to enable Bluetooth, so doing nothing (i.e. sticking with wifi).");
            }

        } else if (requestCode == REQUEST_BLUETOOTH_DISCOVERABLE) {

            if (resultCode != RESULT_CANCELED) {
                Log.d(TAG, "User made Bluetooth discoverable, will proceed to start bluetooth server.");
                getState().getBluetoothSwap().startInBackground();
            } else {
                Log.d(TAG, "User chose not to make Bluetooth discoverable, so doing nothing (i.e. sticking with wifi).");
            }

        }
    }

    /**
     * The process for setting up bluetooth is as follows:
     *  * Assume we have bluetooth available (otherwise the button which allowed us to start
     *    the bluetooth process should not have been available).
     *  * Ask user to enable (if not enabled yet).
     *  * Start bluetooth server socket.
     *  * Enable bluetooth discoverability, so that people can connect to our server socket.
     *
     * Note that this is a little different than the usual process for bluetooth _clients_, which
     * involves pairing and connecting with other devices.
     */
    public void startBluetoothSwap() {

        Log.d(TAG, "Initiating Bluetooth swap, will ensure the Bluetooth devices is enabled and discoverable before starting server.");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null)
            if (adapter.isEnabled()) {
                Log.d(TAG, "Bluetooth enabled, will check if device is discoverable with device.");
                ensureBluetoothDiscoverableThenStart();
            } else {
                Log.d(TAG, "Bluetooth disabled, asking user to enable it.");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE);
            }
    }

    private void ensureBluetoothDiscoverableThenStart() {
        Log.d(TAG, "Ensuring Bluetooth is in discoverable mode.");
        if (BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

            // TODO: Listen for BluetoothAdapter.ACTION_SCAN_MODE_CHANGED and respond if discovery
            // is cancelled prematurely.

            Log.d(TAG, "Not currently in discoverable mode, so prompting user to enable.");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300); // TODO: What about when this expires? What if user manually disables discovery?
            startActivityForResult(intent, REQUEST_BLUETOOTH_DISCOVERABLE);
        }

        if (service == null) {
            throw new IllegalStateException("Can't start Bluetooth swap because service is null for some strange reason.");
        }

        service.getBluetoothSwap().startInBackground();
    }

    class PrepareInitialSwapRepo extends PrepareSwapRepo {
        public PrepareInitialSwapRepo() {
            super(new HashSet<>(Arrays.asList(new String[] { "org.fdroid.fdroid" })));
        }
    }

    class PrepareFullSwapRepo extends PrepareSwapRepo {

        @NonNull
        private final ProgressDialog progressDialog;

        public PrepareFullSwapRepo(@NonNull Set<String> apps) {
            super(apps);
            progressDialog = new ProgressDialog(context);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setTitle(R.string.updating);
        }

        @Override
        protected void onPreExecute() {
            progressDialog.show();
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

    abstract class PrepareSwapRepo extends AsyncTask<Void, String, Void> {

        @SuppressWarnings("UnusedDeclaration")
        private static final String TAG = "UpdateAsyncTask";

        @NonNull
        protected final Set<String> selectedApps;

        @NonNull
        protected final Uri sharingUri;

        @NonNull
        protected final Context context;

        public PrepareSwapRepo(@NonNull Set<String> apps) {
            context = SwapWorkflowActivity.this;
            selectedApps = apps;
            sharingUri = Utils.getSharingUri(FDroidApp.repo);
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
            Log.d(TAG, progress[0]);
        }
    }

    /**
     * Only difference from base class is that it navigates up to a different task.
     * It will go to the {@link org.fdroid.fdroid.views.swap.SwapWorkflowActivity}
     * whereas the base-class will go back to the main list of apps. Need to juggle
     * the repoId in order to be able to return to an appropriately configured swap
     * list.
     */
    public static class SwapAppDetails extends AppDetails {

        private long repoId;

        @Override
        protected void onResume() {
            super.onResume();
            repoId = getIntent().getLongExtra(EXTRA_REPO_ID, -1);
        }

        @Override
        protected void navigateUp() {
            Intent parentIntent = NavUtils.getParentActivityIntent(this);
            parentIntent.putExtra(EXTRA_REPO_ID, repoId);
            NavUtils.navigateUpTo(this, parentIntent);
        }

    }

    /**
     * Helper class to try and make sense of what the swap workflow is currently doing.
     * The more technologies are involved in the process (e.g. Bluetooth/Wifi/NFC/etc)
     * the harder it becomes to reason about and debug the whole thing. Thus,this class
     * will periodically dump the state to logcat so that it is easier to see when certain
     * protocols are enabled/disabled.
     *
     * To view only this output from logcat:
     *
     *  adb logcat | grep 'Swap Status'
     *
     * To exclude this output from logcat (it is very noisy):
     *
     *  adb logcat | grep -v 'Swap Status'
     *
     */
    class SwapDebug {

        public void logStatus() {
            String message = "";
            if (service == null) {
                message = "No swap service";
            } else {
                {
                    String bluetooth = service.getBluetoothSwap().isConnected() ? "Y" : " N";
                    String wifi = service.getWifiSwap().isConnected() ? "Y" : " N";
                    String mdns = service.getWifiSwap().getBonjour().isConnected() ? "Y" : " N";
                     message += "Swap { BT: " + bluetooth + ", WiFi: " + wifi + ", mDNS: " + mdns + "}, ";
                }

                {
                    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                    String bluetooth = "N/A";
                    if (adapter != null) {
                        Map<Integer, String> scanModes = new HashMap<>(3);
                        scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE, "CON");
                        scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, "CON_DISC");
                        scanModes.put(BluetoothAdapter.SCAN_MODE_NONE, "NONE");
                        bluetooth = "\"" + adapter.getName() + "\" - " + scanModes.get(adapter.getScanMode());
                    }

                    String wifi = service.getBonjourFinder().isScanning() ? "Y" : " N";
                    message += "Find { BT: " + bluetooth + ", WiFi: " + wifi + "}";
                }
            }

            Date now = new Date();
            Log.d("Swap Status", now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + " " + message);

            new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        new SwapDebug().logStatus();
                    }
                },
                1000
            );
        }
    }

}
