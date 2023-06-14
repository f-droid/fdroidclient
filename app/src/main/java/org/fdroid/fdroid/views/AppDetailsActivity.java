/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2013-15 Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2013 Stefan Völkel, bd@bc-bd.org
 * Copyright (C) 2015 Nico Alt, nicoalt@posteo.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.views;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.util.ObjectsCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.appbar.MaterialToolbar;

import org.fdroid.database.AppPrefs;
import org.fdroid.database.AppVersion;
import org.fdroid.database.FDroidDatabase;
import org.fdroid.database.Repository;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.CompatibilityChecker;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.NfcHelper;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.nearby.PublicSourceDirProvider;
import org.fdroid.fdroid.views.apps.FeatureImage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public class AppDetailsActivity extends AppCompatActivity
        implements AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks {

    public static final String EXTRA_APPID = "appid";
    private static final String TAG = "AppDetailsActivity";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_PERMISSION_DIALOG = 3;
    private static final int REQUEST_UNINSTALL_DIALOG = 4;

    @SuppressWarnings("unused")
    protected BluetoothAdapter bluetoothAdapter;

    private FDroidApp fdroidApp;
    private FDroidDatabase db;
    private volatile App app;
    @Nullable
    private volatile List<Apk> versions;
    @Nullable
    private volatile AppPrefs appPrefs;
    private String packageName;
    private RecyclerView recyclerView;
    private AppDetailsRecyclerViewAdapter adapter;
    private CompatibilityChecker checker;
    private LocalBroadcastManager localBroadcastManager;
    private AppUpdateStatusManager.AppUpdateStatus currentStatus;

    /**
     * Check if {@code packageName} is currently visible to the user.
     */
    public static boolean isAppVisible(String packageName) {
        return packageName != null && packageName.equals(visiblePackageName);
    }

    private static String visiblePackageName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details2);
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false); // clear title
        supportPostponeEnterTransition();

        bluetoothAdapter = getBluetoothAdapter();

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        recyclerView = findViewById(R.id.rvDetails);
        adapter = new AppDetailsRecyclerViewAdapter(this, app, this);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        lm.setStackFromEnd(false);

        packageName = getPackageNameFromIntent(getIntent());
        if (packageName == null || TextUtils.isEmpty(packageName)) {
            finish();
            return;
        }

        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        recyclerView.getViewTreeObserver().addOnPreDrawListener(() -> {
            supportStartPostponedEnterTransition();
            return true;
        });
        checker = new CompatibilityChecker(this);
        db = DBHelper.getDb(getApplicationContext());
        db.getAppDao().getApp(packageName).observe(this, this::onAppChanged);
        db.getVersionDao().getAppVersions(packageName).observe(this, this::onVersionsChanged);
        db.getAppPrefsDao().getAppPrefs(packageName).observe(this, this::onAppPrefsChanged);
    }

    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }
        return intent.getStringExtra(EXTRA_APPID);
    }

    /**
     * Some notifications (like "downloading" and "installed") are not shown
     * for this app if it is open in app details.  When closing, we need to
     * refresh the notifications, so they are displayed again.
     */
    private void updateNotificationsForApp() {
        AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
        for (AppUpdateStatusManager.AppUpdateStatus status : ausm.getByPackageName(packageName)) {
            if (status.status == AppUpdateStatusManager.Status.Installed) {
                ausm.removeApk(status.getCanonicalUrl());
            } else {
                ausm.refreshApk(status.getCanonicalUrl());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        visiblePackageName = packageName;

        updateNotificationsForApp();
        // don't call this in onResume() because while install confirmation dialog gets shown we pause/resume,
        // so it would get called twice for the same state
        refreshStatus();
        registerAppStatusReceiver();

        Glide.with(this).applyDefaultRequestOptions(new RequestOptions()
                .onlyRetrieveFromCache(!Preferences.get().isBackgroundDownloadAllowed()));
    }

    /**
     * Figures out the current install/update/download/etc status for the app we are viewing.
     * Then, asks the view to update itself to reflect this status.
     */
    private void refreshStatus() {
        AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
        Iterator<AppUpdateStatusManager.AppUpdateStatus> statuses = ausm.getByPackageName(packageName).iterator();
        if (statuses.hasNext()) {
            AppUpdateStatusManager.AppUpdateStatus status = statuses.next();
            updateAppStatus(status, false);
        } else {
            // no status found, so we should update to reflect that as well
            updateAppStatus(null, false);
        }

        currentStatus = null;
    }

    protected void onStop() {
        super.onStop();
        visiblePackageName = null;
        unregisterAppStatusReceiver();

        // When leaving the app details, make sure to refresh app status for this app, since
        // we might want to show notifications for it now.
        updateNotificationsForApp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean ret = super.onCreateOptionsMenu(menu);
        if (ret) {
            getMenuInflater().inflate(R.menu.details2, menu);
        }
        return ret;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        final AppPrefs prefs = appPrefs;
        // don't show menu before appPrefs haven't been loaded
        if (prefs == null || app == null) return false;

        MenuItem itemIgnoreAll = menu.findItem(R.id.action_ignore_all);
        itemIgnoreAll.setChecked(prefs.getIgnoreAllUpdates());
        MenuItem itemIgnoreThis = menu.findItem(R.id.action_ignore_this);
        MenuItem itemBeta = menu.findItem(R.id.action_release_channel_beta);
        if (itemIgnoreAll.isChecked()) {
            itemIgnoreThis.setEnabled(false);
            itemBeta.setEnabled(false);
        } else if (app != null && versions != null) {
            itemIgnoreThis.setVisible(app.hasUpdates(versions, appPrefs));
            itemIgnoreThis.setChecked(prefs.shouldIgnoreUpdate(app.autoInstallVersionCode));
        }
        itemBeta.setChecked(prefs.getReleaseChannels().contains(Apk.RELEASE_CHANNEL_BETA));
        return true;
    }

    /**
     * An app can create an {@link Intent#ACTION_SEND} to share a file
     * and/or text to another app.  This {@link Intent} can provide an
     * {@link java.io.InputStream} to get the actual file via
     * {@link Intent#EXTRA_STREAM}.  This {@link Intent} can also include
     * {@link Intent#EXTRA_TEXT} to describe what the shared file is.  Apps
     * like K-9Mail, Gmail, Signal, etc. correctly handle this case and
     * include both the file itself and the related text in the draft message.
     * <p>
     * This is used in F-Droid to share apps.  The text is the
     * name/description of the app and the URL that points to the app's page
     * on f-droid.org.  The {@link Intent#EXTRA_STREAM} is the actual APK if available.
     * Having all together means that the user can choose to share a message
     * or the actual APK, depending on the receiving app.
     * <p>
     * Unfortunately, not all apps handle this well.  WhatsApp and Element
     * only attach the file and ignore the text.
     *
     * @see <a href="https://github.com/vector-im/element-android/issues/3637"></a>
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            String extraText = String.format("%s (%s)\nhttps://f-droid.org/packages/%s/",
                    app.name, app.summary, app.packageName);

            Intent uriIntent = new Intent(Intent.ACTION_SEND);
            Uri shareUri = app.getShareUri(this);
            if (shareUri != null) uriIntent.setData(shareUri);
            uriIntent.putExtra(Intent.EXTRA_TITLE, app.name);

            Intent textIntent = new Intent(Intent.ACTION_SEND);
            textIntent.setType("text/plain");
            textIntent.putExtra(Intent.EXTRA_SUBJECT, app.name);
            textIntent.putExtra(Intent.EXTRA_TITLE, app.name);
            textIntent.putExtra(Intent.EXTRA_TEXT, extraText);

            if (app.isInstalled(getApplicationContext())) {
                // allow user to share APK if app is installed
                Intent streamIntent = PublicSourceDirProvider.getApkShareIntent(this, app.packageName);
                streamIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared from F-Droid: " + app.name + ".apk");
                streamIntent.putExtra(Intent.EXTRA_TITLE, app.name + ".apk");
                streamIntent.putExtra(Intent.EXTRA_TEXT, extraText);

                Intent chooserIntent = Intent.createChooser(streamIntent, getString(R.string.menu_share));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{
                        textIntent,
                        uriIntent,
                });
                startActivity(chooserIntent);
            } else {
                Intent chooserIntent = Intent.createChooser(textIntent, getString(R.string.menu_share));
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{
                        uriIntent,
                });
                startActivity(chooserIntent);
            }
            return true;
        } else if (item.getItemId() == R.id.action_ignore_all) {
            final AppPrefs prefs = Objects.requireNonNull(appPrefs);
            Utils.runOffUiThread(() -> db.getAppPrefsDao().update(prefs.toggleIgnoreAllUpdates()));
            AppUpdateStatusManager.getInstance(this).checkForUpdates();
            return true;
        } else if (item.getItemId() == R.id.action_ignore_this) {
            final AppPrefs prefs = Objects.requireNonNull(appPrefs);
            Utils.runOffUiThread(() ->
                    db.getAppPrefsDao().update(prefs.toggleIgnoreVersionCodeUpdate(app.autoInstallVersionCode)));
            AppUpdateStatusManager.getInstance(this).checkForUpdates();
            return true;
        } else if (item.getItemId() == R.id.action_release_channel_beta) {
            final AppPrefs prefs = Objects.requireNonNull(appPrefs);
            Utils.runOffUiThread(() -> {
                db.getAppPrefsDao().update(prefs.toggleReleaseChannel(Apk.RELEASE_CHANNEL_BETA));
                return true; // we don't really care about the result here
            }, result -> AppUpdateStatusManager.getInstance(this).checkForUpdates());
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*
    private void shareApkBluetooth() {
        // If Bluetooth has not been enabled/turned on, then
        // enabling device discoverability will automatically enable Bluetooth
        Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 121);
        startActivityForResult(discoverBt, REQUEST_ENABLE_BLUETOOTH);
        // if this is successful, the Bluetooth transfer is started
    }
     */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                fdroidApp.sendViaBluetooth(this, resultCode, app.packageName);
                break;
            case REQUEST_PERMISSION_DIALOG:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    App app = data.getParcelableExtra(Installer.EXTRA_APP);
                    Apk apk = data.getParcelableExtra(Installer.EXTRA_APK);
                    InstallManagerService.queue(this, app, apk);
                }
                break;
            case REQUEST_UNINSTALL_DIALOG:
                if (resultCode == AppCompatActivity.RESULT_OK) {
                    startUninstall();
                }
                break;
        }
    }

    // Install the version of this app denoted by 'app.curApk'.
    @Override
    public void installApk(final Apk apk) {
        if (isFinishing()) {
            return;
        }

        if (!apk.compatible) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.installIncompatible);
            builder.setPositiveButton(R.string.yes, (dialog, whichButton) -> initiateInstall(apk));
            builder.setNegativeButton(R.string.no, (dialog, whichButton) -> {
            });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        if (app.installedSigner != null && apk.signer != null
                && !apk.signer.equals(app.installedSigner)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.SignatureMismatch).setPositiveButton(
                    R.string.ok, (dialog, id) -> dialog.cancel());
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        initiateInstall(apk);

        // Scroll back to the header, so that the user can see the progress beginning. This can be
        // removed once https://gitlab.com/fdroid/fdroidclient/issues/903 is implemented. However
        // for now it adds valuable feedback to the user about the download they just initiated.
        ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0);
    }

    private void initiateInstall(Apk apk) {
        Installer installer = InstallerFactory.create(this, app, apk);
        Intent intent = installer.getPermissionScreen();
        if (intent != null) {
            // permission screen required
            Utils.debugLog(TAG, "permission screen required");
            startActivityForResult(intent, REQUEST_PERMISSION_DIALOG);
            return;
        }

        InstallManagerService.queue(this, app, apk);
    }

    private void startUninstall() {
        registerUninstallReceiver();
        InstallerService.uninstall(this, app, app.installedApk);
    }

    private void registerUninstallReceiver() {
        localBroadcastManager.registerReceiver(uninstallReceiver,
                Installer.getUninstallIntentFilter(app.packageName));
    }

    private void unregisterUninstallReceiver() {
        localBroadcastManager.unregisterReceiver(uninstallReceiver);
    }

    private void registerAppStatusReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        localBroadcastManager.registerReceiver(appStatusReceiver, filter);
    }

    private void unregisterAppStatusReceiver() {
        localBroadcastManager.unregisterReceiver(appStatusReceiver);
    }

    private void unregisterInstallReceiver() {
        localBroadcastManager.unregisterReceiver(installReceiver);
    }

    private void updateAppStatus(@Nullable AppUpdateStatusManager.AppUpdateStatus newStatus, boolean justReceived) {
        if (!justReceived && newStatus == null && currentStatus != null) {
            // clear progress if the state got removed in the meantime (e.g. download canceled)
            adapter.clearProgress();
        }
        this.currentStatus = newStatus;
        if (this.currentStatus == null) {
            return;
        }

        switch (newStatus.status) {
            case PendingInstall:
            case Downloading:
                if (newStatus.progressMax == 0) {
                    // The first progress notification we get telling us our status is "Downloading"
                    adapter.notifyAboutDownloadedApk(newStatus.apk);
                    adapter.setIndeterminateProgress(R.string.download_pending);
                } else {
                    adapter.setProgress(newStatus.progressCurrent, newStatus.progressMax);
                }
                break;

            case ReadyToInstall:
                if (justReceived) {
                    adapter.setIndeterminateProgress(R.string.installing);
                    localBroadcastManager.registerReceiver(installReceiver,
                            Installer.getInstallIntentFilter(newStatus.getCanonicalUrl()));
                } else {
                    try {
                        if (newStatus.intent != null) {
                            localBroadcastManager.registerReceiver(installReceiver,
                                    Installer.getInstallIntentFilter(newStatus.getCanonicalUrl()));
                            newStatus.intent.send();
                        }
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }
                    adapter.clearProgress();
                }
                break;

            case DownloadInterrupted:
                if (justReceived) {
                    if (TextUtils.isEmpty(newStatus.errorText)) {
                        Toast.makeText(this, R.string.details_notinstalled, Toast.LENGTH_LONG).show();
                    } else {
                        String msg = newStatus.errorText;
                        if (!newStatus.getCanonicalUrl().equals(msg)) {
                            msg += " " + newStatus.getCanonicalUrl();
                        }
                        Toast.makeText(this, R.string.download_error, Toast.LENGTH_SHORT).show();
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                }
                adapter.clearProgress();
                break;

            case Installing:
                adapter.setIndeterminateProgress(R.string.installing);
                break;

            case Installed:
            case UpdateAvailable:
            case InstallError:
                // Ignore.
                break;
        }

    }

    private final BroadcastReceiver appStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUpdateStatusManager.AppUpdateStatus status = intent.getParcelableExtra(
                    AppUpdateStatusManager.EXTRA_STATUS);

            boolean isRemoving = TextUtils.equals(intent.getAction(),
                    AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
            if (currentStatus != null
                    && isRemoving
                    && !TextUtils.equals(status.getCanonicalUrl(), currentStatus.getCanonicalUrl())) {
                Utils.debugLog(TAG, "Ignoring app status change because it belongs to "
                        + status.getCanonicalUrl() + " not " + currentStatus.getCanonicalUrl());
            } else if (status != null && app != null && !TextUtils.equals(status.apk.packageName, app.packageName)) {
                Utils.debugLog(TAG, "Ignoring app status change because it belongs to "
                        + status.apk.packageName + " not " + app.packageName);
            } else {
                updateAppStatus(status, true);
            }
        }
    };

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_INSTALL_STARTED:
                    adapter.setIndeterminateProgress(R.string.installing);
                    break;
                case Installer.ACTION_INSTALL_COMPLETE:
                    adapter.clearProgress();
                    unregisterInstallReceiver();
                    // Ideally, we wouldn't try to update the view here, because the InstalledAppProviderService
                    // hasn't had time to do its thing and mark the app as installed. Instead, we
                    // wait for that service to notify us, and then we will respond in appObserver.

                    // Having said that, there are some cases where the PackageManager doesn't
                    // return control back to us until after it has already broadcast to the
                    // InstalledAppProviderService. This means that we are not listening for any
                    // feedback from InstalledAppProviderService (we intentionally stop listening in
                    // onPause). Empirically, this happens when upgrading an app rather than a clean
                    // install. However given the nature of this race condition, it may be different
                    // on different operating systems. As such, we'll just update our view now. It may
                    // happen again in our appObserver, but that will only cause a little more load
                    // on the system, it shouldn't cause a different UX.
                    if (app != null) {
                        PackageInfo packageInfo = getPackageInfo(app.packageName);
                        app.setInstalled(packageInfo);
                        onAppChanged(app);
                    }
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    adapter.clearProgress();
                    if (app != null) onAppChanged(app);

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage) && !isFinishing()) {
                        Log.e(TAG, "install aborted with errorMessage: " + errorMessage);

                        String title = getString(R.string.install_error_notify_title,
                                app == null ? "" : app.name);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetailsActivity.this);
                        alertBuilder.setTitle(title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                    unregisterInstallReceiver();
                    break;
                case Installer.ACTION_INSTALL_USER_INTERACTION:
                    Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
                    if (!isAppVisible(apk.packageName)) {
                        Utils.debugLog(TAG, "Ignore request for user interaction from installer, because "
                                + apk.packageName + " is no longer showing.");
                        break;
                    }

                    Utils.debugLog(TAG, "Automatically showing package manager for " + apk.packageName
                            + " as it is being viewed by the user.");
                    PendingIntent pendingIntent = intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        pendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }

                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    private final BroadcastReceiver uninstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_UNINSTALL_STARTED:
                    adapter.setIndeterminateProgress(R.string.uninstalling);
                    break;
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    adapter.clearProgress();
                    if (app != null) {
                        app.installedSigner = null;
                        app.installedVersionCode = 0;
                        app.installedVersionName = null;
                        onAppChanged(app);
                    }
                    AppUpdateStatusManager.getInstance(context).checkForUpdates();
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    adapter.clearProgress();
                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage) && !isFinishing()) {
                        Log.e(TAG, "uninstall aborted with errorMessage: " + errorMessage);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetailsActivity.this);
                        Uri uri = intent.getData();
                        if (uri == null) {
                            alertBuilder.setTitle(getString(R.string.uninstall_error_notify_title, ""));
                        } else {
                            alertBuilder.setTitle(getString(R.string.uninstall_error_notify_title,
                                    uri.getSchemeSpecificPart()));
                        }
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_USER_INTERACTION:
                    PendingIntent uninstallPendingIntent =
                            intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);

                    try {
                        uninstallPendingIntent.send();
                    } catch (PendingIntent.CanceledException e) {
                        Log.e(TAG, "PI canceled", e);
                    }

                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    /**
     * Reset the display and list contents. Used when entering the activity, and
     * also when something has been installed/uninstalled.  An index update or
     * other external factors might have changed since {@code app} was set
     * before.  This also removes all pending installs with
     * {@link AppUpdateStatusManager.Status#Installed Installed}
     * status for this {@code packageName}, to prevent any lingering open ones from
     * messing up any action that the user might take.  They sometimes might not get
     * removed while F-Droid was in the background.
     */
    private void onAppChanged(@Nullable org.fdroid.database.App dbApp) {
        if (dbApp == null) {
            Toast.makeText(this, R.string.no_such_app, Toast.LENGTH_LONG).show();
            finish();
        } else {
            PackageInfo packageInfo = getPackageInfo(dbApp.getPackageName());
            app = new App(dbApp, packageInfo);
            onAppChanged(app);
        }
    }

    private void onAppChanged(App app) {
        // as receivers don't get unregistered properly,
        // it can happen that we call this while destroyed
        if (isDestroyed()) return;
        // update app info from versions (in case they loaded before the app)
        if (appPrefs != null) {
            updateAppInfo(app, versions, appPrefs);
        }
        // Load the feature graphic, if present
        final FeatureImage featureImage = findViewById(R.id.feature_graphic);
        featureImage.loadImageAndDisplay(app);
        //
        AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
        for (AppUpdateStatusManager.AppUpdateStatus status : ausm.getByPackageName(app.packageName)) {
            if (status.status == AppUpdateStatusManager.Status.Installed) {
                ausm.removeApk(status.getCanonicalUrl());
            }
        }
    }

    private void onVersionsChanged(List<AppVersion> appVersions) {
        List<Apk> apks = new ArrayList<>(appVersions.size());
        for (AppVersion appVersion : appVersions) {
            Repository repo = FDroidApp.getRepoManager(this).getRepository(appVersion.getRepoId());
            Apk apk = new Apk(appVersion, ObjectsCompat.requireNonNull(repo)); // repo shouldn't go missing here
            apk.setCompatibility(checker);
            apks.add(apk);
        }
        versions = apks;
        if (app != null && appPrefs != null) updateAppInfo(app, apks, appPrefs);
    }

    private void onAppPrefsChanged(AppPrefs appPrefs) {
        this.appPrefs = appPrefs;
        if (app != null) updateAppInfo(app, versions, appPrefs);
    }

    private void updateAppInfo(App app, @Nullable List<Apk> apks, AppPrefs appPrefs) {
        // This gets called two times: before versions are loaded and after versions are loaded
        // This is to show something as soon as possible as loading many versions can take time.
        // If versions are not available, we use an empty list temporarily.
        List<Apk> apkList = apks == null ? new ArrayList<>() : apks;
        app.update(this, apkList, appPrefs);
        adapter.updateItems(app, apkList, appPrefs);
        refreshStatus();
        supportInvalidateOptionsMenu();
    }

    @Nullable
    @SuppressLint("PackageManagerGetSignatures")
    private PackageInfo getPackageInfo(String packageName) {
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return packageInfo;
    }

    @Override
    public boolean isAppDownloading() {
        return currentStatus != null &&
                (currentStatus.status == AppUpdateStatusManager.Status.PendingInstall
                        || currentStatus.status == AppUpdateStatusManager.Status.Downloading);
    }

    @Override
    public void enableAndroidBeam() {
        NfcHelper.setAndroidBeam(this, app.packageName);
    }

    @Override
    public void disableAndroidBeam() {
        NfcHelper.disableAndroidBeam(this);
    }

    private BluetoothAdapter getBluetoothAdapter() {
        // to use the new, recommended way of getting the adapter
        // http://developer.android.com/reference/android/bluetooth/BluetoothAdapter.html
        return ContextCompat.getSystemService(this, BluetoothManager.class).getAdapter();
    }

    @Override
    public void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    @Override
    public void installCancel() {
        if (currentStatus != null) {
            InstallManagerService.cancel(this, currentStatus.getCanonicalUrl());
        }
    }

    @Override
    public void launchApk() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(app.packageName);
        if (intent != null) {
            startActivity(intent);
        } else {
            // This can happen when the app was just uninstalled.
            Toast.makeText(this, R.string.app_not_installed, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Uninstall the app from the current screen.  Since there are many ways
     * to uninstall an app, including from Google Play, {@code adb uninstall},
     * or Settings -> Apps, this method cannot ever be sure that the app isn't
     * already being uninstalled.  So it needs to check that we can actually
     * get info on the installed app, otherwise, just call it interrupted and
     * quit.
     *
     * @see <a href="https://gitlab.com/fdroid/fdroidclient/issues/1435">issue #1435</a>
     */
    @Override
    public void uninstallApk() {
        Apk apk = app.installedApk;
        if (apk == null) {
            apk = app.getMediaApkifInstalled(getApplicationContext());
            if (apk == null) {
                List<Apk> versions = this.versions;
                if (versions != null) {
                    // When the app isn't a media file - the above workaround refers to this.
                    apk = app.getInstalledApk(this, versions);
                }
                if (apk == null) {
                    Log.d(TAG, "Couldn't find installed apk for " + app.packageName);
                    Toast.makeText(this, R.string.uninstall_error_unknown, Toast.LENGTH_SHORT).show();
                    uninstallReceiver.onReceive(this, new Intent(Installer.ACTION_UNINSTALL_INTERRUPTED));
                    return;
                }
            }
            app.installedApk = apk;
        }
        Installer installer = InstallerFactory.create(this, app, apk);
        Intent intent = installer.getUninstallScreen();
        if (intent != null) {
            // uninstall screen required
            Utils.debugLog(TAG, "screen screen required");
            startActivityForResult(intent, REQUEST_UNINSTALL_DIALOG);
            return;
        }
        startUninstall();
    }

}
