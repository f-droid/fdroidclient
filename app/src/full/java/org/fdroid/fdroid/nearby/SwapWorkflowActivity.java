package org.fdroid.fdroid.nearby;

import static org.fdroid.fdroid.views.main.MainActivity.ACTION_REQUEST_SWAP;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.LightingColorFilter;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.nearby.peers.BluetoothPeer;
import org.fdroid.fdroid.nearby.peers.Peer;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.qr.CameraCharacteristicsChecker;
import org.fdroid.fdroid.views.main.MainActivity;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import cc.mvdan.accesspoint.WifiApControl;
import io.reactivex.rxjava3.disposables.CompositeDisposable;

/**
 * This is the core of the UI for the whole nearby swap experience.  Each
 * screen is implemented as a {@link View} with the related logic in this
 * {@link android.app.Activity}. Long lived pieces work in {@link SwapService}.
 * All these pieces of the UX are tracked here:
 * <ul>
 * <li>which WiFi network to use</li>
 * <li>whether to advertise via Bluetooth or WiFi+Bonjour</li>
 * <li>connect to another device's swap</li>
 * <li>choose which apps to share</li>
 * <li>ask if the other device would like to swap with us</li>
 * <li>help connect via QR Code or NFC</li>
 * </ul>
 * <p>
 * There are lots of async events in this system, and the user can also change
 * the views while things are working.  The {@link ViewGroup}
 * {@link SwapWorkflowActivity#container} can have all its widgets removed and
 * replaced by a new view at any point.  Therefore, any widget config that is
 * based on fetching it from {@code container}  must check that the result is
 * not null before trying to config it.
 *
 * @see <a href="https://developer.squareup.com/blog/advocating-against-android-fragments/"></a>
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

    private MaterialToolbar toolbar;
    private SwapView currentView;
    private boolean hasPreparedLocalRepo;
    private boolean newIntent;
    private NewRepoConfig confirmSwapConfig;
    private LocalBroadcastManager localBroadcastManager;
    private WifiManager wifiManager;
    private WifiApControl wifiApControl;
    private BluetoothAdapter bluetoothAdapter;

    @LayoutRes
    private int currentSwapViewLayoutRes = R.layout.swap_start_swap;
    private final Stack<Integer> backstack = new Stack<>();

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) sendFDroidBluetooth();
            });

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
            service.getIndex().observe(SwapWorkflowActivity.this, index ->
                    onRepoUpdateSuccess());
            service.getIndexError().observe(SwapWorkflowActivity.this, e ->
                    onRepoUpdateError(e));
            showRelevantView();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            finish();
            service.getIndex().removeObservers(SwapWorkflowActivity.this);
            service.getIndexError().removeObservers(SwapWorkflowActivity.this);
            service = null;
        }
    };

    @Nullable
    private SwapService service;

    @NonNull
    public SwapService getSwapService() {
        return service;
    }

    /**
     * Handle the back logic for the system back button.
     *
     * @see #inflateSwapView(int, boolean)
     */
    @Override
    public void onBackPressed() {
        if (backstack.isEmpty()) {
            super.onBackPressed();
        } else {
            int resId = backstack.pop();
            inflateSwapView(resId, true);
        }
    }

    /**
     * Handle the back logic for the upper left back button in the toolbar.
     * This has a simpler, hard-coded back logic than the system back button.
     *
     * @see #onBackPressed()
     */
    public void onToolbarBackPressed() {
        int nextStep = R.layout.swap_start_swap;
        switch (currentView.getLayoutResId()) {
            case R.layout.swap_confirm_receive:
                nextStep = backstack.peek();
                break;
            case R.layout.swap_connecting:
                nextStep = R.layout.swap_select_apps;
                break;
            case R.layout.swap_join_wifi:
                nextStep = R.layout.swap_start_swap;
                break;
            case R.layout.swap_nfc:
                nextStep = R.layout.swap_join_wifi;
                break;
            case R.layout.swap_select_apps:
                if (!backstack.isEmpty() && backstack.peek() == R.layout.swap_start_swap) {
                    nextStep = R.layout.swap_start_swap;
                } else if (getSwapService() != null && getSwapService().isConnectingWithPeer()) {
                    nextStep = R.layout.swap_success;
                } else {
                    nextStep = R.layout.swap_join_wifi;
                }
                break;
            case R.layout.swap_send_fdroid:
                nextStep = R.layout.swap_start_swap;
                break;
            case R.layout.swap_start_swap:
                if (getSwapService() != null && getSwapService().isConnectingWithPeer()) {
                    nextStep = R.layout.swap_success;
                } else {
                    SwapService.stop(this);
                    finish();
                    return;
                }
                break;
            case R.layout.swap_success:
                nextStep = R.layout.swap_start_swap;
                break;
            case R.layout.swap_wifi_qr:
                if (!backstack.isEmpty() && backstack.peek() == R.layout.swap_start_swap) {
                    nextStep = R.layout.swap_start_swap;
                } else {
                    nextStep = R.layout.swap_join_wifi;
                }
                break;
        }
        currentSwapViewLayoutRes = nextStep;
        inflateSwapView(currentSwapViewLayoutRes);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);

        currentView = new SwapView(this); // dummy placeholder to avoid NullPointerExceptions;

        if (!bindService(new Intent(this, SwapService.class), serviceConnection,
                BIND_ABOVE_CLIENT | BIND_IMPORTANT)) {
            Toast.makeText(this, "ERROR: cannot bind to SwapService!", Toast.LENGTH_LONG).show();
            finish();
        }

        setContentView(R.layout.swap_activity);

        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        container = (ViewGroup) findViewById(R.id.container);

        backstack.clear();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        wifiManager = ContextCompat.getSystemService(getApplicationContext(), WifiManager.class);
        wifiApControl = WifiApControl.getInstance(this);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        new SwapDebug().logStatus();
    }

    @Override
    protected void onDestroy() {
        compositeDisposable.dispose();
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
                if (getSwapService().isConnectingWithPeer()) {
                    setUpNextButton(menu, R.string.next, R.drawable.ic_nearby);
                } else {
                    setUpNextButton(menu, R.string.next, null);
                }
                setUpSearchView(menu);
                return true;
            case R.layout.swap_success:
                menuInflater.inflate(R.menu.swap_search, menu);
                setUpSearchView(menu);
                return true;
            case R.layout.swap_join_wifi:
                menuInflater.inflate(R.menu.swap_next, menu);
                setUpNextButton(menu, R.string.next, R.drawable.ic_arrow_forward);
                return true;
            case R.layout.swap_nfc:
                menuInflater.inflate(R.menu.swap_next, menu);
                setUpNextButton(menu, R.string.skip, R.drawable.ic_arrow_forward);
                return true;
        }

        return super.onPrepareOptionsMenu(menu);
    }

    private void setUpNextButton(Menu menu, @StringRes int titleResId, Integer drawableResId) {
        MenuItem next = menu.findItem(R.id.action_next);
        CharSequence title = getString(titleResId);
        next.setTitle(title);
        next.setTitleCondensed(title);
        if (drawableResId == null) {
            next.setVisible(false);
        } else {
            next.setVisible(true);
            next.setIcon(drawableResId);
        }
        next.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
        next.setOnMenuItemClickListener(item -> {
            sendNext();
            return true;
        });
    }

    private void sendNext() {
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
        MenuItem appsMenuItem = menu.findItem(R.id.action_apps);
        if (appsMenuItem != null) {
            appsMenuItem.setOnMenuItemClickListener(item -> {
                inflateSwapView(R.layout.swap_select_apps);
                return true;
            });
        }

        SearchView searchView = new SearchView(this);
        MenuItem searchMenuItem = menu.findItem(R.id.action_search);
        searchMenuItem.setActionView(searchView);
        searchMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

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
        localBroadcastManager.registerReceiver(localRepoStatus, new IntentFilter(LocalRepoService.ACTION_STATUS));
        localBroadcastManager.registerReceiver(bonjourFound, new IntentFilter(BonjourManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bonjourRemoved, new IntentFilter(BonjourManager.ACTION_REMOVED));
        localBroadcastManager.registerReceiver(bonjourStatusReceiver, new IntentFilter(BonjourManager.ACTION_STATUS));
        localBroadcastManager.registerReceiver(bluetoothFound, new IntentFilter(BluetoothManager.ACTION_FOUND));
        localBroadcastManager.registerReceiver(bluetoothStatusReceiver, new IntentFilter(BluetoothManager.ACTION_STATUS));

        registerReceiver(bluetoothScanModeChanged,
                new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));

        checkIncomingIntent();

        if (newIntent) {
            showRelevantView();
            newIntent = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(bluetoothScanModeChanged);

        localBroadcastManager.unregisterReceiver(onWifiStateChanged);
        localBroadcastManager.unregisterReceiver(localRepoStatus);
        localBroadcastManager.unregisterReceiver(bonjourFound);
        localBroadcastManager.unregisterReceiver(bonjourRemoved);
        localBroadcastManager.unregisterReceiver(bonjourStatusReceiver);
        localBroadcastManager.unregisterReceiver(bluetoothFound);
        localBroadcastManager.unregisterReceiver(bluetoothStatusReceiver);
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
        if (uri != null && !isSwapUrl(uri) && !BluetoothDownloader.isBluetoothUri(uri)) {
            String msg = getString(R.string.swap_toast_invalid_url, uri);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
        }
        confirmSwapConfig = new NewRepoConfig(this, intent);
    }

    private static boolean isSwapUrl(Uri uri) {
        return isSwapUrl(uri.getHost(), uri.getPort());
    }

    private static boolean isSwapUrl(String host, int port) {
        return port > 1023 // only root can use <= 1023, so never a swap repo
                && host.matches("[0-9.]+") // host must be an IP address
                && FDroidApp.subnetInfo.isInRange(host); // on the same subnet as we are
    }

    private void promptToSelectWifiNetwork() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.swap_join_same_wifi)
                .setMessage(R.string.swap_join_same_wifi_desc)
                .setNeutralButton(R.string.cancel, (dialog, which) -> {
                    // Do nothing
                })
                .setPositiveButton(R.string.wifi, (dialog, which) -> {
                    SwapService.putWifiEnabledBeforeSwap(wifiManager.isWifiEnabled());
                    wifiManager.setWifiEnabled(true);
                    Intent intent = new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                })
                .setNegativeButton(R.string.wifi_ap, (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= 26) {
                        showTetheringSettings();
                    } else if (!Settings.System.canWrite(getBaseContext())) {
                        requestWriteSettingsPermission();
                    } else {
                        setupWifiAP();
                    }
                })
                .create().show();
    }

    private void setupWifiAP() {
        if (wifiApControl == null) {
            Log.e(TAG, "WiFi AP is null");
            Toast.makeText(this, R.string.swap_toast_could_not_enable_hotspot, Toast.LENGTH_LONG).show();
            return;
        }
        SwapService.putHotspotEnabledBeforeSwap(wifiApControl.isEnabled());
        wifiManager.setWifiEnabled(false);
        if (wifiApControl.enable()) {
            Toast.makeText(this, R.string.swap_toast_hotspot_enabled, Toast.LENGTH_SHORT).show();
            SwapService.putHotspotActivatedUserPreference(true);
        } else {
            Toast.makeText(this, R.string.swap_toast_could_not_enable_hotspot, Toast.LENGTH_LONG).show();
            SwapService.putHotspotActivatedUserPreference(false);
            Log.e(TAG, "Could not enable WiFi AP.");
        }
    }

    /**
     * Handle events that trigger different swap views to be shown.
     */
    private void showRelevantView() {

        if (confirmSwapConfig != null) {
            inflateSwapView(R.layout.swap_confirm_receive);
            setUpConfirmReceive();
            confirmSwapConfig = null;
            return;
        }

        switch (currentSwapViewLayoutRes) {
            case R.layout.swap_start_swap:
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
        inflateSwapView(viewRes, false);
    }

    /**
     * The {@link #backstack} for the global back button is managed mostly here.
     * The initial screen is never added to the {@code backstack} since the
     * empty state is used to detect that the system's backstack should be used.
     */
    public void inflateSwapView(@LayoutRes int viewRes, boolean backPressed) {
        getSwapService().initTimer();

        if (!backPressed) {
            switch (currentSwapViewLayoutRes) {
                case R.layout.swap_connecting:
                case R.layout.swap_confirm_receive:
                    // do not add to backstack
                    break;
                default:
                    if (backstack.isEmpty()) {
                        if (viewRes != R.layout.swap_start_swap) {
                            backstack.push(currentSwapViewLayoutRes);
                        }
                    } else {
                        if (backstack.peek() != currentSwapViewLayoutRes) {
                            backstack.push(currentSwapViewLayoutRes);
                        }
                    }
            }
        }

        container.removeAllViews();
        View view = ContextCompat.getSystemService(this, LayoutInflater.class)
                .inflate(viewRes, container, false);
        currentView = (SwapView) view;
        currentView.setLayoutResId(viewRes);
        currentSwapViewLayoutRes = viewRes;

        toolbar.setTitle(currentView.getToolbarTitle());
        toolbar.setNavigationOnClickListener(v -> onToolbarBackPressed());
        toolbar.setNavigationOnClickListener(v -> {
            switch (currentView.getLayoutResId()) {
                case R.layout.swap_start_swap:
                    SwapService.stop(this);
                    finish();
                    return;
                default:
                    currentSwapViewLayoutRes = R.layout.swap_start_swap;
            }
            inflateSwapView(currentSwapViewLayoutRes);
        });
        if (viewRes == R.layout.swap_start_swap) {
            toolbar.setNavigationIcon(R.drawable.ic_close);
        } else {
            toolbar.setNavigationIcon(R.drawable.abc_ic_ab_back_material);
        }
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
            case R.layout.swap_select_apps:
                LocalRepoService.create(this, getSwapService().getAppsToSwap());
                break;
            case R.layout.swap_connecting:
                setUpConnectingView();
                break;
            case R.layout.swap_start_swap:
                setUpStartVisibility();
                break;
        }
    }

    public void showIntro() {
        // If we were previously swapping with a specific client, forget that we were doing that,
        // as we are starting over now.
        getSwapService().swapWith(null);

        LocalRepoService.create(this);

        inflateSwapView(R.layout.swap_start_swap);
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

    public void requestWriteSettingsPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivityForResult(intent, REQUEST_WRITE_SETTINGS_PERMISSION);
    }

    public void sendFDroid() {
        if (bluetoothAdapter == null // TODO make Bluetooth work with content:// URIs
                || (!bluetoothAdapter.isEnabled() && LocalHTTPDManager.isAlive())) {
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
        if (bluetoothAdapter.isEnabled()) {
            sendFDroidApk();
        } else if (Build.VERSION.SDK_INT >= 31) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120);
                startActivityForResult(discoverBt, REQUEST_BLUETOOTH_ENABLE_FOR_SEND);
            } else {
                requestPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT);
            }
        }
    }

    private void sendFDroidApk() {
        ((FDroidApp) getApplication()).sendViaBluetooth(this, AppCompatActivity.RESULT_OK, BuildConfig.APPLICATION_ID);
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
        if (currentSwapViewLayoutRes == R.layout.swap_start_swap
                || currentSwapViewLayoutRes == R.layout.swap_confirm_receive) {
            // This will force the "Select apps to swap" workflow to begin.
            swapWith(peer);
        } else {
            getSwapService().swapWith(peer);
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
        super.onActivityResult(requestCode, resultCode, intent);
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
            if (Settings.System.canWrite(this)) {
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
                BluetoothManager.start(this);
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
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled()) {
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
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Utils.debugLog(TAG, "Not currently in discoverable mode, so prompting user to enable.");
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            intent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600); // 1 hour
            startActivityForResult(intent, REQUEST_BLUETOOTH_DISCOVERABLE);
        }
        BluetoothManager.start(this);
    }

    private final BroadcastReceiver bluetoothScanModeChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            SwitchMaterial bluetoothSwitch = container.findViewById(R.id.switch_bluetooth);
            TextView textBluetoothVisible = container.findViewById(R.id.bluetooth_visible);
            if (bluetoothSwitch == null || textBluetoothVisible == null
                    || !BluetoothManager.ACTION_STATUS.equals(intent.getAction())) {
                return;
            }
            switch (intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, -1)) {
                case BluetoothAdapter.SCAN_MODE_NONE:
                    textBluetoothVisible.setText(R.string.disabled);
                    bluetoothSwitch.setEnabled(true);
                    break;

                case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                    bluetoothSwitch.setEnabled(true);
                    break;

                case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                    bluetoothSwitch.setEnabled(true);
                    break;
            }
        }
    };

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
                String bluetooth;

                bluetooth = "N/A";
                if (bluetoothAdapter != null) {
                    Map<Integer, String> scanModes = new HashMap<>(3);
                    scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE, "CON");
                    scanModes.put(BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, "CON_DISC");
                    scanModes.put(BluetoothAdapter.SCAN_MODE_NONE, "NONE");
                    bluetooth = "\"" + bluetoothAdapter.getName() + "\" - "
                            + scanModes.get(bluetoothAdapter.getScanMode());
                }
            }

            Date now = new Date();
            Utils.debugLog("SWAP_STATUS",
                    now.getHours() + ":" + now.getMinutes() + ":" + now.getSeconds() + " " + message);

            new Timer().schedule(new TimerTask() {
                                     @Override
                                     public void run() {
                                         new SwapDebug().logStatus();
                                     }
                                 }, 1000
            );
        }
    }

    private final BroadcastReceiver onWifiStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpFromWifi();

            TextView textWifiVisible = container.findViewById(R.id.wifi_visible);
            if (textWifiVisible == null) {
                return;
            }
            switch (intent.getIntExtra(WifiStateChangeService.EXTRA_STATUS, -1)) {
                case WifiManager.WIFI_STATE_ENABLING:
                    textWifiVisible.setText(R.string.swap_setting_up_wifi);
                    break;
                case WifiManager.WIFI_STATE_ENABLED:
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    break;
                case WifiManager.WIFI_STATE_DISABLING:
                case WifiManager.WIFI_STATE_DISABLED:
                    textWifiVisible.setText(R.string.swap_stopping_wifi);
                    break;
                case WifiManager.WIFI_STATE_UNKNOWN:
                    break;
            }
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

            compositeDisposable.add(Utils.generateQrBitmap(this, qrUriString)
                    .subscribe(qrBitmap -> {
                        qrImage.setImageBitmap(qrBitmap);

                        // Replace all blacks with the background blue.
                        qrImage.setColorFilter(new LightingColorFilter(0xffffffff,
                                ContextCompat.getColor(this, R.color.swap_blue)));

                        final View qrWarningMessage = container.findViewById(R.id.warning_qr_scanner);
                        if (qrWarningMessage != null) {
                            if (CameraCharacteristicsChecker.getInstance(this).hasAutofocus()) {
                                qrWarningMessage.setVisibility(View.GONE);
                            } else {
                                qrWarningMessage.setVisibility(View.VISIBLE);
                            }
                        }
                    })
            );
        }
    }

    // TODO: Listen for "Connecting..." state and reflect that in the view too.
    private void setUpJoinWifi() {
        currentView.setOnClickListener(v -> startActivity(new Intent(WifiManager.ACTION_PICK_WIFI_NETWORK)));
        TextView descriptionView = container.findViewById(R.id.text_description);
        ImageView wifiIcon = container.findViewById(R.id.wifi_icon);
        TextView ssidView = container.findViewById(R.id.wifi_ssid);
        TextView tapView = container.findViewById(R.id.wifi_available_networks_prompt);
        if (descriptionView == null || wifiIcon == null || ssidView == null || tapView == null) {
            return;
        }
        if (TextUtils.isEmpty(FDroidApp.bssid) && !TextUtils.isEmpty(FDroidApp.ipAddressString)) {
            // empty bssid with an ipAddress means hotspot mode
            descriptionView.setText(R.string.swap_join_this_hotspot);
            wifiIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_wifi_tethering));
            ssidView.setText(R.string.swap_active_hotspot);
            tapView.setText(R.string.swap_switch_to_wifi);
        } else if (TextUtils.isEmpty(FDroidApp.ssid)) {
            // not connected to or setup with any wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_wifi));
            ssidView.setText(R.string.swap_no_wifi_network);
            tapView.setText(R.string.swap_view_available_networks);
        } else {
            // connected to a regular wifi network
            descriptionView.setText(R.string.swap_join_same_wifi);
            wifiIcon.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_wifi));
            ssidView.setText(FDroidApp.ssid);
            tapView.setText(R.string.swap_view_available_networks);
        }
    }

    private void setUpStartVisibility() {
        bluetoothStatusReceiver.onReceive(this, new Intent(BluetoothManager.ACTION_STATUS));
        bonjourStatusReceiver.onReceive(this, new Intent(BonjourManager.ACTION_STATUS));

        TextView viewWifiNetwork = findViewById(R.id.wifi_network);
        SwitchMaterial wifiSwitch = findViewById(R.id.switch_wifi);
        MaterialButton scanQrButton = findViewById(R.id.btn_scan_qr);
        MaterialButton appsButton = findViewById(R.id.btn_apps);
        if (viewWifiNetwork == null || wifiSwitch == null || scanQrButton == null || appsButton == null) {
            return;
        }
        viewWifiNetwork.setOnClickListener(v -> promptToSelectWifiNetwork());

        wifiSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Context context = getApplicationContext();
            if (isChecked) {
                if (wifiApControl != null && wifiApControl.isEnabled()) {
                    setupWifiAP();
                } else {
                    wifiManager.setWifiEnabled(true);
                }
                BonjourManager.start(context);
            }
            BonjourManager.setVisible(context, isChecked);
            SwapService.putWifiVisibleUserPreference(isChecked);
        });

        scanQrButton.setOnClickListener(v -> inflateSwapView(R.layout.swap_wifi_qr));

        appsButton.setOnClickListener(v -> inflateSwapView(R.layout.swap_select_apps));
        appsButton.setEllipsize(TextUtils.TruncateAt.END);

        if (SwapService.getWifiVisibleUserPreference()) {
            wifiSwitch.setChecked(true);
        } else {
            wifiSwitch.setChecked(false);
        }
    }

    private final BroadcastReceiver bonjourStatusReceiver = new BroadcastReceiver() {

        private volatile int bonjourStatus = BonjourManager.STATUS_STOPPED;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BonjourManager.ACTION_STATUS.equals(intent.getAction())) {
                return;
            }
            bonjourStatus = intent.getIntExtra(BonjourManager.EXTRA_STATUS, bonjourStatus);
            TextView textWifiVisible = container.findViewById(R.id.wifi_visible);
            TextView peopleNearbyText = container.findViewById(R.id.text_people_nearby);
            ProgressBar peopleNearbyProgress = container.findViewById(R.id.searching_people_nearby);
            if (textWifiVisible == null || peopleNearbyText == null || peopleNearbyProgress == null) {
                return;
            }
            switch (bonjourStatus) {
                case BonjourManager.STATUS_STARTING:
                    textWifiVisible.setText(R.string.swap_setting_up_wifi);
                    peopleNearbyText.setText(R.string.swap_starting);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BonjourManager.STATUS_STARTED:
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    peopleNearbyText.setText(R.string.swap_scanning_for_peers);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BonjourManager.STATUS_VPN_CONFLICT:
                    textWifiVisible.setText(R.string.swap_wifi_vpn_conflict);
                    break;
                case BonjourManager.STATUS_NOT_VISIBLE:
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    peopleNearbyText.setText(R.string.swap_scanning_for_peers);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BonjourManager.STATUS_VISIBLE:
                    if (wifiApControl != null && wifiApControl.isEnabled()) {
                        textWifiVisible.setText(R.string.swap_visible_hotspot);
                    } else {
                        textWifiVisible.setText(R.string.swap_visible_wifi);
                    }
                    peopleNearbyText.setText(R.string.swap_scanning_for_peers);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BonjourManager.STATUS_STOPPING:
                    textWifiVisible.setText(R.string.swap_stopping_wifi);
                    if (!BluetoothManager.isAlive()) {
                        peopleNearbyText.setText(R.string.swap_stopping);
                        peopleNearbyText.setVisibility(View.VISIBLE);
                        peopleNearbyProgress.setVisibility(View.VISIBLE);
                    }
                    break;
                case BonjourManager.STATUS_STOPPED:
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    if (!BluetoothManager.isAlive()) {
                        peopleNearbyText.setVisibility(View.GONE);
                        peopleNearbyProgress.setVisibility(View.GONE);
                    }
                    break;
                case BonjourManager.STATUS_ERROR:
                    textWifiVisible.setText(R.string.swap_not_visible_wifi);
                    peopleNearbyText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.GONE);
                    break;
                default:
                    String msg = "Bad intent: " + intent + " " + bonjourStatus;
                    Log.i(TAG, msg);
                    if (BuildConfig.DEBUG) {
                        throw new IllegalArgumentException(msg);
                    }
            }
        }
    };

    /**
     * Add any new Bonjour devices that were found, as long as they are not
     * already present.
     *
     * @see #bluetoothFound
     * @see ArrayAdapter#getPosition(Object)
     * @see java.util.List#indexOf(Object)
     */
    private final BroadcastReceiver bonjourFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ListView peopleNearbyList = container.findViewById(R.id.list_people_nearby);
            if (peopleNearbyList != null) {
                ArrayAdapter<Peer> peopleNearbyAdapter = (ArrayAdapter<Peer>) peopleNearbyList.getAdapter();
                Peer peer = intent.getParcelableExtra(BonjourManager.EXTRA_BONJOUR_PEER);
                if (peopleNearbyAdapter.getPosition(peer) == -1) {
                    peopleNearbyAdapter.add(peer);
                }
            }
        }
    };

    private final BroadcastReceiver bonjourRemoved = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ListView peopleNearbyList = container.findViewById(R.id.list_people_nearby);
            if (peopleNearbyList != null) {
                ArrayAdapter<Peer> peopleNearbyAdapter = (ArrayAdapter<Peer>) peopleNearbyList.getAdapter();
                peopleNearbyAdapter.remove((Peer) intent.getParcelableExtra(BonjourManager.EXTRA_BONJOUR_PEER));
            }
        }
    };

    private final BroadcastReceiver bluetoothStatusReceiver = new BroadcastReceiver() {

        private volatile int bluetoothStatus = BluetoothManager.STATUS_STOPPED;

        @Override
        public void onReceive(Context context, Intent intent) {
            if (!BluetoothManager.ACTION_STATUS.equals(intent.getAction())) {
                return;
            }
            bluetoothStatus = intent.getIntExtra(BluetoothManager.EXTRA_STATUS, bluetoothStatus);
            SwitchMaterial bluetoothSwitch = container.findViewById(R.id.switch_bluetooth);
            TextView textBluetoothVisible = container.findViewById(R.id.bluetooth_visible);
            TextView textDeviceIdBluetooth = container.findViewById(R.id.device_id_bluetooth);
            TextView peopleNearbyText = container.findViewById(R.id.text_people_nearby);
            ProgressBar peopleNearbyProgress = container.findViewById(R.id.searching_people_nearby);
            if (bluetoothSwitch == null || textBluetoothVisible == null || textDeviceIdBluetooth == null
                    || peopleNearbyText == null || peopleNearbyProgress == null) {
                return;
            }
            switch (bluetoothStatus) {
                case BluetoothManager.STATUS_STARTING:
                    bluetoothSwitch.setEnabled(false);
                    textBluetoothVisible.setText(R.string.swap_setting_up_bluetooth);
                    textDeviceIdBluetooth.setVisibility(View.VISIBLE);
                    peopleNearbyText.setText(R.string.swap_scanning_for_peers);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BluetoothManager.STATUS_STARTED:
                    bluetoothSwitch.setEnabled(true);
                    textBluetoothVisible.setText(R.string.swap_visible_bluetooth);
                    textDeviceIdBluetooth.setVisibility(View.VISIBLE);
                    peopleNearbyText.setText(R.string.swap_scanning_for_peers);
                    peopleNearbyText.setVisibility(View.VISIBLE);
                    peopleNearbyProgress.setVisibility(View.VISIBLE);
                    break;
                case BluetoothManager.STATUS_STOPPING:
                    bluetoothSwitch.setEnabled(false);
                    textBluetoothVisible.setText(R.string.swap_stopping);
                    textDeviceIdBluetooth.setVisibility(View.GONE);
                    if (!BonjourManager.isAlive()) {
                        peopleNearbyText.setText(R.string.swap_stopping);
                        peopleNearbyText.setVisibility(View.VISIBLE);
                        peopleNearbyProgress.setVisibility(View.VISIBLE);
                    }
                    break;
                case BluetoothManager.STATUS_STOPPED:
                    bluetoothSwitch.setEnabled(true);
                    textBluetoothVisible.setText(R.string.swap_not_visible_bluetooth);
                    textDeviceIdBluetooth.setVisibility(View.GONE);
                    if (!BonjourManager.isAlive()) {
                        peopleNearbyText.setVisibility(View.GONE);
                        peopleNearbyProgress.setVisibility(View.GONE);
                    }

                    ListView peopleNearbyView = container.findViewById(R.id.list_people_nearby);
                    if (peopleNearbyView == null) {
                        break;
                    }
                    ArrayAdapter peopleNearbyAdapter = (ArrayAdapter) peopleNearbyView.getAdapter();
                    for (int i = 0; i < peopleNearbyAdapter.getCount(); i++) {
                        Peer peer = (Peer) peopleNearbyAdapter.getItem(i);
                        if (peer.getClass().equals(BluetoothPeer.class)) {
                            Utils.debugLog(TAG, "Removing bluetooth peer: " + peer.getName());
                            peopleNearbyAdapter.remove(peer);
                        }
                    }
                    break;
                case BluetoothManager.STATUS_ERROR:
                    bluetoothSwitch.setEnabled(true);
                    textBluetoothVisible.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                    textDeviceIdBluetooth.setVisibility(View.VISIBLE);
                    break;
                default:
                    throw new IllegalArgumentException("Bad intent: " + intent);
            }
        }
    };

    /**
     * Add any new Bluetooth devices that were found, as long as they are not
     * already present.
     *
     * @see #bonjourFound
     * @see ArrayAdapter#getPosition(Object)
     * @see java.util.List#indexOf(Object)
     */
    private final BroadcastReceiver bluetoothFound = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ListView peopleNearbyList = container.findViewById(R.id.list_people_nearby);
            if (peopleNearbyList != null) {
                ArrayAdapter<Peer> peopleNearbyAdapter = (ArrayAdapter<Peer>) peopleNearbyList.getAdapter();
                Peer peer = intent.getParcelableExtra(BluetoothManager.EXTRA_PEER);
                if (peopleNearbyAdapter.getPosition(peer) == -1) {
                    peopleNearbyAdapter.add(peer);
                }
            }
        }
    };

    private void setUpUseBluetoothButton() {
        Button useBluetooth = findViewById(R.id.btn_use_bluetooth);
        if (useBluetooth != null) {
            if (bluetoothAdapter == null) {
                useBluetooth.setVisibility(View.GONE);
            } else {
                useBluetooth.setVisibility(View.VISIBLE);
            }
            useBluetooth.setOnClickListener(v -> {
                showIntro();
                sendFDroidBluetooth();
            });
        }
    }

    private void setUpQrScannerButton() {
        Button openQr = findViewById(R.id.btn_qr_scanner);
        if (openQr != null) {
            openQr.setOnClickListener(v -> initiateQrScan());
        }
    }

    private void setUpConfirmReceive() {
        TextView descriptionTextView = findViewById(R.id.text_description);
        if (descriptionTextView != null) {
            descriptionTextView.setText(getString(R.string.swap_confirm_connect, confirmSwapConfig.getHost()));
        }

        Button confirmReceiveYes = container.findViewById(R.id.confirm_receive_yes);
        if (confirmReceiveYes != null) {
            confirmReceiveYes.setOnClickListener(v -> denySwap());
        }

        Button confirmReceiveNo = container.findViewById(R.id.confirm_receive_no);
        if (confirmReceiveNo != null) {
            confirmReceiveNo.setOnClickListener(new View.OnClickListener() {

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
        if (dontShowAgain != null) {
            dontShowAgain.setOnCheckedChangeListener((buttonView, isChecked)
                    -> Preferences.get().setShowNfcDuringSwap(!isChecked));
        }
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
    private final BroadcastReceiver localRepoStatus = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            setUpConnectingProgressText(intent.getStringExtra(Intent.EXTRA_TEXT));

            ProgressBar progressBar = container.findViewById(R.id.progress_bar);
            Button tryAgainButton = container.findViewById(R.id.try_again);
            if (progressBar == null || tryAgainButton == null) {
                return;
            }

            switch (intent.getIntExtra(LocalRepoService.EXTRA_STATUS, -1)) {
                case LocalRepoService.STATUS_PROGRESS:
                    progressBar.setVisibility(View.VISIBLE);
                    tryAgainButton.setVisibility(View.GONE);
                    break;
                case LocalRepoService.STATUS_STARTED:
                    progressBar.setVisibility(View.VISIBLE);
                    tryAgainButton.setVisibility(View.GONE);
                    onLocalRepoPrepared();
                    break;
                case LocalRepoService.STATUS_ERROR:
                    progressBar.setVisibility(View.GONE);
                    tryAgainButton.setVisibility(View.VISIBLE);
                    break;
                default:
                    throw new IllegalArgumentException("Bogus intent: " + intent);
            }
        }
    };

    private void onRepoUpdateSuccess() {
        ProgressBar progressBar = container.findViewById(R.id.progress_bar);
        Button tryAgainButton = container.findViewById(R.id.try_again);
        if (progressBar != null && tryAgainButton != null) {
            progressBar.setVisibility(View.VISIBLE);
            tryAgainButton.setVisibility(View.GONE);
        }
        getSwapService().addCurrentPeerToActive();
        inflateSwapView(R.layout.swap_success);
    }

    private void onRepoUpdateError(Exception e) {
        ProgressBar progressBar = container.findViewById(R.id.progress_bar);
        Button tryAgainButton = container.findViewById(R.id.try_again);
        if (progressBar != null && tryAgainButton != null) {
            progressBar.setVisibility(View.GONE);
            tryAgainButton.setVisibility(View.VISIBLE);
        }
        String msg = e.getMessage() == null ? "Error updating repo " + e : e.getMessage();
        setUpConnectingProgressText(msg);
        getSwapService().removeCurrentPeerFromActive();
    }

    private void setUpConnectingView() {
        TextView heading = container.findViewById(R.id.progress_text);
        heading.setText(R.string.swap_connecting);
        Button tryAgainButton = container.findViewById(R.id.try_again);
        if (tryAgainButton != null) {
            tryAgainButton.setOnClickListener(v -> onAppsSelected());
        }
    }
}
