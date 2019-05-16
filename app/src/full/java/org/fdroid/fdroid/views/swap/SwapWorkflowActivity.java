package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import cc.mvdan.accesspoint.WifiApControl;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.QrGenAsyncTask;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.localrepo.LocalRepoService;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.SwapView;
import org.fdroid.fdroid.localrepo.peers.Peer;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.HttpDownloader;
import org.fdroid.fdroid.net.WifiStateChangeService;
import org.fdroid.fdroid.views.main.MainActivity;
import org.fdroid.fdroid.views.swap.device.camera.CameraCharacteristicsChecker;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.fdroid.fdroid.views.main.MainActivity.ACTION_REQUEST_SWAP;

/**
 * This activity will do its best to show the most relevant screen about swapping to the user.
 * The problem comes when there are two competing goals - 1) Show the user a list of apps from another
 * device to download and install, and 2) Prepare your own list of apps to share.
 */
@SuppressWarnings("LineLength")
public class SwapWorkflowActivity extends AppCompatActivity {
    private static final String TAG = "SwapWorkflowActivity";

    /**
     * When connecting to a swap, we then go and initiate a connection with that
     * device and ask if it would like to swap with us. Upon receiving that request
     * and agreeing, we don't then want to be asked whether we want to swap back.
     * This flag protects against two devices continually going back and forth
     * among each other offering swaps.
     */
    public static final String EXTRA_PREVENT_FURTHER_SWAP_REQUESTS = "preventFurtherSwap";

    private ViewGroup container;

    private static final int REQUEST_BLUETOOTH_ENABLE_FOR_SWAP = 2;
    private static final int REQUEST_BLUETOOTH_DISCOVERABLE = 3;
    private static final int REQUEST_BLUETOOTH_ENABLE_FOR_SEND = 4;
    private static final int REQUEST_WRITE_SETTINGS_PERMISSION = 5;
    private static final int STEP_INTRO = 1;  // TODO remove this special case, only use layoutResIds

    private Toolbar toolbar;
    private SwapView currentView;
    private boolean hasPreparedLocalRepo;
    private boolean newIntent;
    private NewRepoConfig confirmSwapConfig;
    private LocalBroadcastManager localBroadcastManager;
    private WifiManager wifiManager;

    @LayoutRes
    private int currentSwapViewLayoutRes = STEP_INTRO;

    public static void requestSwap(Context context, String repo) {
        requestSwap(context, Uri.parse(repo));
    }

    public static void requestSwap(Context context, Uri uri) {
        Intent intent = new Intent(MainActivity.ACTION_REQUEST_SWAP, uri, context, SwapWorkflowActivity.class);
        intent.putExtra(EXTRA_PREVENT_FURTHER_SWAP_REQUESTS, true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @NonNull
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ((SwapService.Binder) binder).getService();
            showRelevantView();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            finish();
            service = null;
        }
    };

    @Nullable
    private SwapService service;

    @NonNull
    public SwapService getSwapService() {
        return service;
    }

    @Override
    public void onBackPressed() {
        if (currentView.getLayoutResId() == STEP_INTRO) {
            SwapService.stop(this);
            finish();
        } else {
            // TODO: Currently StartSwapView is handleed by the SwapWorkflowActivity as a special case, where
            // if getLayoutResId is STEP_INTRO, don't even bother asking for getPreviousStep. But that is a
            // bit messy. It would be nicer if this was handled using the same mechanism as everything
            // else.
            int nextStep = -1;
            switch (currentView.getLayoutResId()) {
                case R.layout.swap_confirm_receive:
                    nextStep = STEP_INTRO;
                    break;
                case R.layout.swap_connecting:
                    nextStep = R.layout.swap_select_apps;
                    break;
                case R.layout.swap_join_wifi:
                    nextStep = STEP_INTRO;
                    break;
                case R.layout.swap_nfc:
                    nextStep = R.layout.swap_join_wifi;
                    break;
                case R.layout.swap_select_apps:
                    nextStep = getSwapService().isConnectingWithPeer() ? STEP_INTRO : R.layout.swap_join_wifi;
                    break;
                case R.layout.swap_send_fdroid:
                    nextStep = STEP_INTRO;
                    break;
                case R.layout.swap_start_swap:
                    nextStep = STEP_INTRO;
                    break;
                case R.layout.swap_success:
                    nextStep = STEP_INTRO;
                    break;
                case R.layout.swap_wifi_qr:
                    nextStep = R.layout.swap_join_wifi;
                    break;
            }
            currentSwapViewLayoutRes = nextStep;
            showRelevantView();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ((FDroidApp) getApplication()).setSecureWindow(this);
        super.onCreate(savedInstanceState);

        currentView = new SwapView(this); // dummy placeholder to avoid NullPointerExceptions;

        if (!bindService(new Intent(this, SwapService.class), serviceConnection,
                BIND_ABOVE_CLIENT | BIND_IMPORTANT)) {
            Toast.makeText(this, "ERROR: cannot bind to SwapService!", Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.swap_activity);

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitleTextAppearance(getApplicationContext(), R.style.SwapTheme_Wizard_Text_Toolbar);
        setSupportActionBar(toolbar);

        container = (ViewGroup) findViewById(R.id.container);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.registerReceiver(downloaderInterruptedReceiver,
                new IntentFilter(Downloader.ACTION_INTERRUPTED));

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        new SwapDebug().logStatus();
    }

    @Override
    protected void onDestroy() {
        localBroadcastManager.unregisterReceiver(downloaderInterruptedReceiver);
        unbindService(serviceConnection);
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();

        MenuInflater menuInflater = getMenuInflater();
        switch (currentView.getLayoutResId()) {
            case R.layout.swap_select_apps:
                menuInflater.inflate(R.menu.swap_next_search, menu);
                setUpNextButton(menu, R.string.next);
                setUpSearchView(menu);
                return true;
            case R.layout.swap_success:
                menuInflater.inflate(R.menu.swap_search, menu);
                setUpSearchView(menu);
                return true;
            case R.layout.swap_join_wifi:
                menuInflater.inflate(R.menu.swap_next, menu);
                setUpNextButton(menu, R.string.next);
                return true;
            case R.layout.swap_nfc:
                menuInflater.inflate(R.menu.swap_next, menu);
                setUpNextButton(menu, R.string.skip);
                return true;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setUpNextButton(Menu menu, @StringRes int titleResId) {
        MenuItem next = menu.findItem(R.id.action_next);
        CharSequence title = getString(titleResId);
        next.setTitle(title);
        next.setTitleCondensed(title);
        MenuItemCompat.setShowAsAction(next,
                MenuItemCompat.SHOW_AS_ACTION_ALWAYS | MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        next.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                sendNext();
                return true;
            }
        });
    }

    void sendNext() {
        int currentLayoutResId = currentView.getLayoutResId();
        switch (currentLayoutResId) {
            case R.layout.swap_select_apps:
                onAppsSelected();
                break;
            case R.layout.swap_join_wifi:
                inflateSwapView(R.layout.swap_select_apps);
                break;
            case R.layout.swap_nfc:
                inflateSwapView(R.layout.swap_wifi_qr);
                break;
        }
    }

    private void setUpSearchView(Menu menu) {
        SearchView searchView = new SearchView(this);

        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        MenuItemCompat.setActionView(searchMenuItem, searchView);
        MenuItemCompat.setShowAsAction(searchMenuItem, MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String newText) {
                String currentFilterString = currentView.getCurrentFilterString();
                String newFilter = !TextUtils.isEmpty(newText) ? newText : null;
                if (currentFilterString == null && newFilter == null) {
                    return true;
                }
                if (currentFilterString != null && currentFilterString.equals(newFilter)) {
                    return true;
                }
                currentView.setCurrentFilterString(newFilter);
                if (currentView instanceof SelectAppsView) {
                    getSupportLoaderManager().restartLoader(currentView.getLayoutResId(), null,
                            (SelectAppsView) currentView);
                } else if (currentView instanceof SwapSuccessView) {
                    getSupportLoaderManager().restartLoader(currentView.getLayoutResId(), null,
                            (SwapSuccessView) currentView);
                } else {
                    throw new IllegalStateException(currentView.getClass() + " does not have Loader!");
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        localBroadcastManager.registerReceiver(onWifiStateChanged,
                new IntentFilter(WifiStateChangeService.BROADCAST));
        localBroadcastManager.registerReceiver(prepareSwapReceiver,
                new IntentFilter(SwapWorkflowActivity.PrepareSwapRepo.ACTION));
        localBroadcastManager.registerReceiver(repoUpdateReceiver,
                new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));

        checkIncomingIntent();

        if (newIntent) {
            showRelevantView();
            newIntent = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        localBroadcastManager.unregisterReceiver(onWifiStateChanged);
        localBroadcastManager.unregisterReceiver(prepareSwapReceiver);
        localBroadcastManager.unregisterReceiver(repoUpdateReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        newIntent = true;
    }

    /**
     * Check whether incoming {@link Intent} is a swap repo, and ensure that
     * it is a valid swap URL.  The hostname can only be either an IP or
     * Bluetooth address.
     */
    private void checkIncomingIntent() {
        Intent intent = getIntent();
        if (!ACTION_REQUEST_SWAP.equals(intent.getAction())) {
            return;
        }
        Uri uri = intent.getData();
        if (uri != null && !HttpDownloader.isSwapUrl(uri) && !BluetoothDownloader.isBluetoothUri(uri)) {
            String msg = getString(R.string.swap_toast_invalid_url, uri);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }
        confirmSwapConfig = new NewRepoConfig(this, intent);
    }

    public void promptToSelectWifiNetwork() {
        //
        // On Android >= 5.0, the neutral button is the one by itself off to the left of a dialog
        // (not the negative button). Thus, the layout of this dialogs buttons should be:
        //
        // |                                 |
        // +---------------------------------+
        // | Cancel           Hotspot   WiFi |
        // +---------------------------------+
        //
        // TODO: Investigate if this should be set dynamically for earlier APIs.
        //
        new AlertDialog.Builder(this)
                .setTitle(R.string.swap_join_same_wifi)
                .setMessage(R.string.swap_join_same_wifi_desc)
                .setNeutralButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing
                    }
                })
                .setPositiveButton(R.string.wifi, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SwapService.putWifiEnabledBeforeSwap(wifiManager.isWifiEnabled());
                        wifiManager.setWifiEnabled(true);
                        Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.wifi_ap, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (Build.VERSION.SDK_INT >= 26) {
                            showTetheringSettings();
                        } else if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(getBaseContext())) {
                            requestWriteSettingsPermission();
                        } else {
                            setupWifiAP();
                        }
                    }
                })
                .create().show();
    }

    private void setupWifiAP() {
        WifiApControl ap = WifiApControl.getInstance(this);
        wifiManager.setWifiEnabled(false);
        if (ap.enable()) {
            Toast.makeText(this, R.string.swap_toast_hotspot_enabled, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.swap_toast_could_not_enable_hotspot, Toast.LENGTH_LONG).show();
            Log.e(TAG, "Could not enable WiFi AP.");
        }
    }

    private void showRelevantView() {

        if (confirmSwapConfig != null) {
            inflateSwapView(R.layout.swap_confirm_receive);
            setUpConfirmReceive();
            confirmSwapConfig = null;
            return;
        }

        switch (currentSwapViewLayoutRes) {
            case STEP_INTRO:
                showIntro();
                return;
            case R.layout.swap_nfc:
                if (!attemptToShowNfc()) {
                    inflateSwapView(R.layout.swap_wifi_qr);
                    return;
                }
                break;
            case R.layout.swap_connecting:
                // TODO: Properly decide what to do here (i.e. returning to the activity after it was connecting)...
                inflateSwapView(R.layout.swap_start_swap);
                return;
        }
        inflateSwapView(currentSwapViewLayoutRes);
    }

    public void inflateSwapView(@LayoutRes int viewRes) {
        container.removeAllViews();
        View view = ((LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE)).inflate(viewRes, container, false);
        currentView = (SwapView) view;
        currentView.setLayoutResId(viewRes);
        currentSwapViewLayoutRes = viewRes;

        toolbar.setBackgroundColor(currentView.getToolbarColour());
        toolbar.setTitle(currentView.getToolbarTitle());
        toolbar.setNavigationIcon(R.drawable.ic_close_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onToolbarCancel();
            }
        });
        container.addView(view);
        supportInvalidateOptionsMenu();

        switch (currentView.getLayoutResId()) {
            case R.layout.swap_send_fdroid:
                setUpFromWifi();
                setUpUseBluetoothButton();
                break;
            case R.layout.swap_wifi_qr:
                setUpFromWifi();
                setUpQrScannerButton();
                break;
            case R.layout.swap_nfc:
                setUpNfcView();
                break;
            case R.layout.swap_connecting:
                setUpConnectingView();
                break;
        }
    }

    private void onToolbarCancel() {
        SwapService.stop(this);
        finish();
    }

    public void showIntro() {
        // If we were previously swapping with a specific client, forget that we were doing that,
        // as we are starting over now.
        getSwapService().swapWith(null);

        LocalRepoService.create(this);

        inflateSwapView(R.layout.swap_start_swap);
    }

    public void startQrWorkflow() {
        if (!getSwapService().isEnabled()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.not_visible_nearby)
                    .setMessage(R.string.not_visible_nearby_description)
                    .setCancelable(true)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Do nothing. The dialog will get dismissed anyway, which is all we ever wanted...
                        }
                    })
                    .create().show();
        } else {
            inflateSwapView(R.layout.swap_wifi_qr);
        }
    }

    /**
     * On {@code android-26}, only apps with privileges can access
     * {@code WRITE_SETTINGS}.  So this just shows the tethering settings
     * for the user to do it themselves.
     */
    public void showTetheringSettings() {
        final Intent intent = new Intent(Intent.ACTION_MAIN, null);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        final ComponentName cn = new ComponentName("com.android.settings",
                "com.android.settings.TetherSettings");
        intent.setComponent(cn);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @TargetApi(23)
    public void requestWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
    }

    public void sendFDroid() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null
                || Build.VERSION.SDK_INT >= 23 // TODO make Bluetooth work with content:// URIs
                || (!adapter.isEnabled() && getSwapService().getWifiSwap().isConnected())) {
            inflateSwapView(R.layout.swap_send_fdroid);
        } else {
            sendFDroidBluetooth();
        }
    }

    /**
     * Send the F-Droid APK via Bluetooth.  If Bluetooth has not been
     * enabled/turned on, then enabling device discoverability will
     * automatically enable Bluetooth.
     */
    public void sendFDroidBluetooth() {
        if (BluetoothAdapter.getDefaultAdapter().isEnabled()) {
            sendFDroidApk();
        } else {
            Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
            startActivityForResult(discoverBt, REQUEST_BLUETOOTH_ENABLE_FOR_SEND);
        }
    }

    private void sendFDroidApk() {
        ((FDroidApp) getApplication()).sendViaBluetooth(this, Activity.RESULT_OK, BuildConfig.APPLICATION_ID);
    }

    /**
     * TODO: Figure out whether they have changed since last time LocalRepoService
     * was run.  If the local repo is running, then we can ask it what apps it is
     * swapping and compare with that. Otherwise, probably will need to scan the
     * file system.
     */
    public void onAppsSelected() {
        if (hasPreparedLocalRepo) {
            onLocalRepoPrepared();
        } else {
            LocalRepoService.create(this, getSwapService().getAppsToSwap());
            currentSwapViewLayoutRes = R.layout.swap_connecting;
            inflateSwapView(R.layout.swap_connecting);
        }
    }

    /**
     * Once the LocalRepoService has finished preparing our repository index, we can
     * show the next screen to the user. This will be one of two things:
     * <ol>
     * <li>If we directly selected a peer to swap with initially, we will skip straight to getting
     * the list of apps from that device.</li>
     * <li>Alternatively, if we didn't have a person to connect to, and instead clicked "Scan QR Code",
     * then we want to show a QR code or NFC dialog.</li>
     * </ol>
     */
    public void onLocalRepoPrepared() {
        // TODO ditch this, use a message from LocalRepoService.  Maybe?
        hasPreparedLocalRepo = true;
        if (getSwapService().isConnectingWithPeer()) {
            startSwappingWithPeer();
        } else if (!attemptToShowNfc()) {
            inflateSwapView(R.layout.swap_wifi_qr);
        }
    }

    private void startSwappingWithPeer() {
        getSwapService().connectToPeer();
        inflateSwapView(R.layout.swap_connecting);
    }

    private boolean attemptToShowNfc() {
        // TODO: What if NFC is disabled? Hook up with NfcNotEnabledActivity? Or maybe only if they
        // click a relevant button?

        // Even if they opted to skip the message which says "Touch devices to swap",
        // we still want to actually enable the feature, so that they could touch
        // during the wifi qr code being shown too.
        boolean nfcMessageReady = NfcHelper.setPushMessage(this, Utils.getSharingUri(FDroidApp.repo));

        // TODO move all swap-specific preferences to a SharedPreferences instance for SwapWorkflowActivity
        if (Preferences.get().showNfcDuringSwap() && nfcMessageReady) {
            inflateSwapView(R.layout.swap_nfc);
            return true;
        }
        return false;
    }

    public void swapWith(Peer peer) {
        getSwapService().swapWith(peer);
        inflateSwapView(R.layout.swap_select_apps);
    }

    /**
     * This is for when we initiate a swap by viewing the "Are you sure you want to swap with" view
     * This can arise either:
     * * As a result of scanning a QR code (in which case we likely already have a repo setup) or
     * * As a result of the other device selecting our device in the "start swap" screen, in which
     * case we are likely just sitting on the start swap screen also, and haven't configured
     * anything yet.
     */
    public void swapWith(NewRepoConfig repoConfig) {
        Peer peer = repoConfig.toPeer();
        if (currentSwapViewLayoutRes == STEP_INTRO || currentSwapViewLayoutRes == R.layout.swap_confirm_receive) {
            // This will force the "Select apps to swap" workflow to begin.
            // TODO: Find a better way to decide whether we need to select the apps. Not sure if we
            //       can or cannot be in STEP_INTRO with a full blown repo ready to swap.
            swapWith(peer);
        } else {
            getSwapService().swapWith(repoConfig.toPeer());
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
                    Toast.makeText(this, R.string.swap_qr_isnt_for_swap, Toast.LENGTH_SHORT).show();
                }
            }
        } else if (requestCode == REQUEST_WRITE_SETTINGS_PERMISSION) {
            if (Build.VERSION.SDK_INT >= 23 && Settings.System.canWrite(this)) {
                setupWifiAP();
            }
        } else if (requestCode == REQUEST_BLUETOOTH_ENABLE_FOR_SWAP) {

            if (resultCode == RESULT_OK) {
                Utils.debugLog(TAG, "User enabled Bluetooth, will make sure we are discoverable.");
                ensureBluetoothDiscoverableThenStart();
            } else {
                Utils.debugLog(TAG, "User chose not to enable Bluetooth, so doing nothing");
                SwapService.putBluetoothVisibleUserPreference(false);
            }

        } else if (requestCode == REQUEST_BLUETOOTH_DISCOVERABLE) {

            if (resultCode != RESULT_CANCELED) {
                Utils.debugLog(TAG, "User made Bluetooth discoverable, will proceed to start bluetooth server.");
                getSwapService().getBluetoothSwap().startInBackground(); // TODO replace with Intent to SwapService
            } else {
                Utils.debugLog(TAG, "User chose not to make Bluetooth discoverable, so doing nothing");
                SwapService.putBluetoothVisibleUserPreference(false);
            }

        } else if (requestCode == REQUEST_BLUETOOTH_ENABLE_FOR_SEND) {
            sendFDroidApk();
        }
    }

    /**
     * The process for setting up bluetooth is as follows:
     * <ul>
     * <li>Assume we have bluetooth available (otherwise the button which allowed us to start
     * the bluetooth process should not have been available)</li>
     * <li>Ask user to enable (if not enabled yet)</li>
     * <li>Start bluetooth server socket</li>
     * <li>Enable bluetooth discoverability, so that people can connect to our server socket.</li>
     * </ul>
     * Note that this is a little different than the usual process for bluetooth _clients_, which
     * involves pairing and connecting with other devices.
     */
    public void startBluetoothSwap() {

        Utils.debugLog(TAG, "Initiating Bluetooth swap, will ensure the Bluetooth devices is enabled and discoverable before starting server.");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null) {
            if (adapter.isEnabled()) {
                Utils.debugLog(TAG, "Bluetooth enabled, will check if device is discoverable with device.");
                ensureBluetoothDiscoverableThenStart();
            } else {
                Utils.debugLog(TAG, "Bluetooth disabled, asking user to enable it.");
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_BLUETOOTH_ENABLE_FOR_SWAP);
            }
        }
    }

    private void ensureBluetoothDiscoverableThenStart() {
        Utils.debugLog(TAG, "Ensuring Bluetooth is in discoverable mode.");
        if (BluetoothAdapter.getDefaultAdapter().getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {

            // TODO: Listen for BluetoothAdapter.ACTION_SCAN_MODE_CHANGED and respond if discovery
            // is cancelled prematurely.

            // 3600 is new maximum! TODO: What about when this expires? What if user manually disables discovery?
            final int discoverableTimeout = 3600;

            Utils.debugLog(TAG, "Not currently in discoverable mode, so prompting user to enable.");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, discoverableTimeout);
            startActivityForResult(intent, REQUEST_BLUETOOTH_DISCOVERABLE);
        }
        service.getBluetoothSwap().startInBackground();  // TODO replace with Intent to SwapService
    }

    public class PrepareSwapRepo {
        public static final String ACTION = "PrepareSwapRepo.Action";
        public static final String EXTRA_MESSAGE = "PrepareSwapRepo.Status.Message";
        public static final String EXTRA_TYPE = "PrepareSwapRepo.Action.Type";
        public static final int TYPE_STATUS = 0;
        public static final int TYPE_COMPLETE = 1;
        public static final int TYPE_ERROR = 2;
    }

    /**
     * Helper class to try and make sense of what the swap workflow is currently doing.
     * The more technologies are involved in the process (e.g. Bluetooth/Wifi/NFC/etc)
     * the harder it becomes to reason about and debug the whole thing. Thus,this class
     * will periodically dump the state to logcat so that it is easier to see when certain
     * protocols are enabled/disabled.
     * <p>
     * To view only this output from logcat:
     * <p>
     * adb logcat | grep 'Swap Status'
     * <p>
     * To exclude this output from logcat (it is very noisy):
     * <p>
     * adb logcat | grep -v 'Swap Status'
     */
    class SwapDebug {

        public void logStatus() {

            if (true) return; // NOPMD

            String message = "";
            if (service == null) {
                message = "No swap service";
            } else {
                String bluetooth = service.getBluetoothSwap().isConnected() ? "Y" : " N";
                String wifi = service.getWifiSwap().isConnected() ? "Y" : " N";
                String mdns = service.getWifiSwap().getBonjour().isConnected() ? "Y" : " N";
                message += "Swap { BT: " + bluetooth + ", WiFi: " + wifi + ", mDNS: " + mdns + "}, ";

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                bluetooth = "N/A";
                if (adapter != null) {
                    Map<Integer, String> scanModes = new HashMap<>(3);
                    scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE, "CON");
                    scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, "CON_DISC");
                    scanModes.put(BluetoothAdapter.SCAN_MODE_NONE, "NONE");
                    bluetooth = "\"" + adapter.getName() + "\" - " + scanModes.get(adapter.getScanMode());
                }

                message += "Find { BT: " + bluetooth + ", WiFi: " + wifi + "}";
            }

            Date now = new Date();
            Utils.debugLog("Swap Status", now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + " " + message);

            new Timer().schedule(new TimerTask() {
                                     @Override
                                     public void run() {
                                         new SwapDebug().logStatus();
                                     }
                                 }, 1000
            );
        }
    }

    public void install(@NonNull final App app, @NonNull final Apk apk) {
        localBroadcastManager.registerReceiver(new InstallReceiver(),
                Installer.getInstallIntentFilter(apk.getCanonicalUrl()));
        InstallManagerService.queue(this, app, apk);
    }

    private class InstallReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_INSTALL_STARTED:
                    break;
                case Installer.ACTION_INSTALL_COMPLETE:
                    localBroadcastManager.unregisterReceiver(this);

                    showRelevantView();
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    localBroadcastManager.unregisterReceiver(this);
                    String errorMessage = intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);
                    if (errorMessage != null) {
                        Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
                    }
                    break;
                case Installer.ACTION_INSTALL_USER_INTERACTION:
                    PendingIntent installPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        installPendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }

                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    }

    private final BroadcastReceiver onWifiStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpFromWifi();
        }
    };

    private void setUpFromWifi() {
        String scheme = Preferences.get().isLocalRepoHttpsEnabled() ? "https://" : "http://";

        // the fingerprint is not useful on the button label
        String buttonLabel = scheme + FDroidApp.ipAddressString + ":" + FDroidApp.port;
        TextView ipAddressView = container.findViewById(R.id.device_ip_address);
        if (ipAddressView != null) {
            ipAddressView.setText(buttonLabel);
        }

        String qrUriString = null;
        switch (currentView.getLayoutResId()) {
            case R.layout.swap_join_wifi:
                setUpJoinWifi();
                return;
            case R.layout.swap_send_fdroid:
                qrUriString = buttonLabel;
                break;
            case R.layout.swap_wifi_qr:
                Uri sharingUri = Utils.getSharingUri(FDroidApp.repo);
                StringBuilder qrUrlBuilder = new StringBuilder(scheme);
                qrUrlBuilder.append(sharingUri.getHost());
                if (sharingUri.getPort() != 80) {
                    qrUrlBuilder.append(':');
                    qrUrlBuilder.append(sharingUri.getPort());
                }
                qrUrlBuilder.append(sharingUri.getPath());
                boolean first = true;

                Set<String> names = sharingUri.getQueryParameterNames();
                for (String name : names) {
                    if (!"ssid".equals(name)) {
                        if (first) {
                            qrUrlBuilder.append('?');
                            first = false;
                        } else {
                            qrUrlBuilder.append('&');
                        }
                        qrUrlBuilder.append(name.toUpperCase(Locale.ENGLISH));
                        qrUrlBuilder.append('=');
                        qrUrlBuilder.append(sharingUri.getQueryParameter(name).toUpperCase(Locale.ENGLISH));
                    }
                }
                qrUriString = qrUrlBuilder.toString();
                break;
        }

        ImageView qrImage = container.findViewById(R.id.wifi_qr_code);
        if (qrUriString != null && qrImage != null) {
            Utils.debugLog(TAG, "Encoded swap URI in QR Code: " + qrUriString);
            new QrGenAsyncTask(SwapWorkflowActivity.this, R.id.wifi_qr_code).execute(qrUriString);

            // Replace all blacks with the background blue.
            qrImage.setColorFilter(new LightingColorFilter(0xffffffff, getResources().getColor(R.color.swap_blue)));

            final View qrWarningMessage = container.findViewById(R.id.warning_qr_scanner);
            if (CameraCharacteristicsChecker.getInstance(this).hasAutofocus()) {
                qrWarningMessage.setVisibility(View.GONE);
            } else {
                qrWarningMessage.setVisibility(View.VISIBLE);
            }
        }
    }

    // TODO: Listen for "Connecting..." state and reflect that in the view too.
    private void setUpJoinWifi() {
        currentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK));
            }
        });
        TextView descriptionView = container.findViewById(R.id.text_description);
        ImageView wifiIcon = container.findViewById(R.id.wifi_icon);
        TextView ssidView = container.findViewById(R.id.wifi_ssid);
        TextView tapView = container.findViewById(R.id.wifi_available_networks_prompt);
        if (TextUtils.isEmpty(FDroidApp.bssid) && !TextUtils.isEmpty(FDroidApp.ipAddressString)) {
            // empty bssid with an ipAddress means hotspot mode
            descriptionView.setText(R.string.swap_join_this_hotspot);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.hotspot));
            ssidView.setText(R.string.swap_active_hotspot);
            tapView.setText(R.string.swap_switch_to_wifi);
        } else if (TextUtils.isEmpty(FDroidApp.ssid)) {
            // not connected to or setup with any wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.wifi));
            ssidView.setText(R.string.swap_no_wifi_network);
            tapView.setText(R.string.swap_view_available_networks);
        } else {
            // connected to a regular wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(getResources().getDrawable(R.drawable.wifi));
            ssidView.setText(FDroidApp.ssid);
            tapView.setText(R.string.swap_view_available_networks);
        }
    }

    private void setUpUseBluetoothButton() {
        Button useBluetooth = findViewById(R.id.btn_use_bluetooth);
        if (useBluetooth != null) {
            useBluetooth.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showIntro();
                    sendFDroidBluetooth();
                }
            });
        }
    }

    private void setUpQrScannerButton() {
        Button openQr = findViewById(R.id.btn_qr_scanner);
        if (openQr != null) {
            openQr.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    initiateQrScan();
                }
            });
        }
    }

    private void setUpConfirmReceive() {
        TextView descriptionTextView = findViewById(R.id.text_description);
        if (descriptionTextView != null) {
            descriptionTextView.setText(getString(R.string.swap_confirm_connect, confirmSwapConfig.getHost()));
        }

        Button confirmReceiveYes = container.findViewById(R.id.confirm_receive_yes);
        if (confirmReceiveYes != null) {
            findViewById(R.id.confirm_receive_yes).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    denySwap();
                }
            });
        }

        Button confirmReceiveNo = container.findViewById(R.id.confirm_receive_no);
        if (confirmReceiveNo != null) {
            findViewById(R.id.confirm_receive_no).setOnClickListener(new View.OnClickListener() {

                private final NewRepoConfig config = confirmSwapConfig;

                @Override
                public void onClick(View v) {
                    swapWith(config);
                }
            });
        }
    }

    private void setUpNfcView() {
        CheckBox dontShowAgain = container.findViewById(R.id.checkbox_dont_show);
        dontShowAgain.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Preferences.get().setShowNfcDuringSwap(!isChecked);
            }
        });

    }

    private void setUpConnectingProgressText(String message) {
        TextView progressText = container.findViewById(R.id.progress_text);
        if (progressText != null && message != null) {
            progressText.setVisibility(View.VISIBLE);
            progressText.setText(message);
        }
    }

    /**
     * Listens for feedback about a local repository being prepared, like APK
     * files copied to the LocalHTTPD webroot, the {@code index.html} generated,
     * etc.  Icons will be copied to the webroot in the background and so are
     * not part of this process.
     */
    private final BroadcastReceiver prepareSwapReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpConnectingProgressText(intent.getStringExtra(SwapWorkflowActivity.PrepareSwapRepo.EXTRA_MESSAGE));

            ProgressBar progressBar = container.findViewById(R.id.progress_bar);
            Button tryAgainButton = container.findViewById(R.id.try_again);

            if (progressBar == null || tryAgainButton == null) {
                Utils.debugLog(TAG, "prepareSwapReceiver received intent without view: " + intent);
                return;
            }

            int type = intent.getIntExtra(SwapWorkflowActivity.PrepareSwapRepo.EXTRA_TYPE, -1);
            if (type == SwapWorkflowActivity.PrepareSwapRepo.TYPE_ERROR) {
                progressBar.setVisibility(View.GONE);
                tryAgainButton.setVisibility(View.VISIBLE);
                return;
            } else {
                progressBar.setVisibility(View.VISIBLE);
                tryAgainButton.setVisibility(View.GONE);
            }

            if (type == SwapWorkflowActivity.PrepareSwapRepo.TYPE_COMPLETE) {
                onLocalRepoPrepared();
            }
        }
    };

    /**
     * Listens for feedback about a repo update process taking place.
     * Tracks an index.jar download and show the progress messages
     */
    private final BroadcastReceiver repoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(UpdateService.EXTRA_MESSAGE);
            if (message == null) {
                CharSequence[] repoErrors = intent.getCharSequenceArrayExtra(UpdateService.EXTRA_REPO_ERRORS);
                if (repoErrors != null) {
                    StringBuilder msgBuilder = new StringBuilder();
                    for (CharSequence error : repoErrors) {
                        if (msgBuilder.length() > 0) {
                            msgBuilder.append(" + ");
                        }
                        msgBuilder.append(error);
                    }
                    message = msgBuilder.toString();
                }
            }
            setUpConnectingProgressText(message);

            ProgressBar progressBar = container.findViewById(R.id.progress_bar);
            Button tryAgainButton = container.findViewById(R.id.try_again);

            if (progressBar == null || tryAgainButton == null) {
                Utils.debugLog(TAG, "repoUpdateReceiver received intent without view: " + intent);
                return;
            }

            int status = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);
            if (status == UpdateService.STATUS_ERROR_GLOBAL ||
                    status == UpdateService.STATUS_ERROR_LOCAL ||
                    status == UpdateService.STATUS_ERROR_LOCAL_SMALL) {
                progressBar.setVisibility(View.GONE);
                tryAgainButton.setVisibility(View.VISIBLE);
                return;
            } else {
                progressBar.setVisibility(View.VISIBLE);
                tryAgainButton.setVisibility(View.GONE);
            }

            if (status == UpdateService.STATUS_COMPLETE_AND_SAME
                    || status == UpdateService.STATUS_COMPLETE_WITH_CHANGES) {
                inflateSwapView(R.layout.swap_success);
            }
        }
    };

    private final BroadcastReceiver downloaderInterruptedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Repo repo = RepoProvider.Helper.findByUrl(context, intent.getData(), null);
            if (repo != null && repo.isSwap) {
                setUpConnectingProgressText(intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE));
            }
        }
    };

    private void setUpConnectingView() {
        TextView heading = container.findViewById(R.id.progress_text);
        heading.setText(R.string.swap_connecting);
        container.findViewById(R.id.try_again).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onAppsSelected();
            }
        });
    }
}
