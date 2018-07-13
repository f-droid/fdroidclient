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

package org.fdroid.fdroid;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefsProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.views.AppDetailsRecyclerViewAdapter;
import org.fdroid.fdroid.views.OverscrollLinearLayoutManager;
import org.fdroid.fdroid.views.ShareChooserDialog;
import org.fdroid.fdroid.views.apps.FeatureImage;

import java.util.Iterator;

public class AppDetails2 extends AppCompatActivity
        implements ShareChooserDialog.ShareChooserDialogListener,
        AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks {

    public static final String EXTRA_APPID = "appid";
    private static final String TAG = "AppDetails2";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_PERMISSION_DIALOG = 3;
    private static final int REQUEST_UNINSTALL_DIALOG = 4;

    private FDroidApp fdroidApp;
    private App app;
    private CoordinatorLayout coordinatorLayout;
    private AppBarLayout appBarLayout;
    private RecyclerView recyclerView;
    private AppDetailsRecyclerViewAdapter adapter;
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
    protected void onCreate(Bundle savedInstanceState) {
        fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Nice and clean toolbar
        toolbar.setNavigationIcon(R.drawable.ic_back);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        supportPostponeEnterTransition();

        if (!reset(getPackageNameFromIntent(getIntent()))) {
            finish();
            return;
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.rootCoordinator);
        appBarLayout = (AppBarLayout) coordinatorLayout.findViewById(R.id.app_bar);
        recyclerView = (RecyclerView) findViewById(R.id.rvDetails);
        adapter = new AppDetailsRecyclerViewAdapter(this, app, this);
        OverscrollLinearLayoutManager lm = new OverscrollLinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        lm.setStackFromEnd(false);

        // Has to be invoked after AppDetailsRecyclerViewAdapter is created.
        refreshStatus();

        /* The recyclerView/AppBarLayout combo has a bug that prevents a "fling" from the bottom
         * to continue all the way to the top by expanding the AppBarLayout. It will instead stop
         * with the app bar in a collapsed state. See here: https://code.google.com/p/android/issues/detail?id=177729
         * Not sure this is the exact issue, but it is true that while in a fling the RecyclerView will
         * consume the scroll events quietly, without calling the nested scrolling mechanism.
         * We fix this behavior by using an OverscrollLinearLayoutManager that will give us information
         * of overscroll, i.e. when we have not consumed all of a scroll event, and use this information
         * to send the scroll to the app bar layout so that it will expand itself.
         */
        lm.setOnOverscrollListener(new OverscrollLinearLayoutManager.OnOverscrollListener() {
            @Override
            public int onOverscrollX(int overscroll) {
                return 0;
            }

            @Override
            public int onOverscrollY(int overscroll) {
                int consumed = 0;
                if (overscroll < 0) {
                    CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams)
                            appBarLayout.getLayoutParams();
                    CoordinatorLayout.Behavior behavior = lp.getBehavior();
                    if (behavior != null && behavior instanceof AppBarLayout.Behavior) {
                        ((AppBarLayout.Behavior) behavior).onNestedScroll(
                                coordinatorLayout, appBarLayout, recyclerView, 0, 0, 0, overscroll);
                        consumed = overscroll; // Consume all of it!
                    }
                }
                return consumed;
            }
        });
        recyclerView.setLayoutManager(lm);
        recyclerView.setAdapter(adapter);

        recyclerView.getViewTreeObserver().addOnPreDrawListener(
                new ViewTreeObserver.OnPreDrawListener() {
                    @Override
                    public boolean onPreDraw() {
                        supportStartPostponedEnterTransition();
                        return true;
                    }
                }
        );

        // Load the feature graphic, if present
        final FeatureImage featureImage = (FeatureImage) findViewById(R.id.feature_graphic);
        DisplayImageOptions displayImageOptions = Utils.getRepoAppDisplayImageOptions();
        String featureGraphicUrl = app.getFeatureGraphicUrl(this);
        featureImage.loadImageAndDisplay(ImageLoader.getInstance(), displayImageOptions,
                featureGraphicUrl, app.iconUrl);
    }

    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }
        return intent.getStringExtra(EXTRA_APPID);
    }

    /**
     * If passed null, this will show a message to the user ("Could not find app ..." or something
     * like that) and then finish the activity.
     */
    private void setApp(App newApp) {
        if (newApp == null) {
            Toast.makeText(this, R.string.no_such_app, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        app = newApp;

        // Remove all "installed" statuses for this app, since we are now viewing it.
        AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
        for (AppUpdateStatusManager.AppUpdateStatus status : ausm.getByPackageName(app.packageName)) {
            if (status.status == AppUpdateStatusManager.Status.Installed) {
                ausm.removeApk(status.getUniqueKey());
            }
        }
    }

    /**
     * Some notifications (like "downloading" and "installed") are not shown
     * for this app if it is open in app details.  When closing, we need to
     * refresh the notifications, so they are displayed again.
     */
    private void updateNotificationsForApp() {
        if (app != null) {
            AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
            for (AppUpdateStatusManager.AppUpdateStatus status : ausm.getByPackageName(app.packageName)) {
                if (status.status == AppUpdateStatusManager.Status.Installed) {
                    ausm.removeApk(status.getUniqueKey());
                } else {
                    ausm.refreshApk(status.getUniqueKey());
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (app != null) {
            visiblePackageName = app.packageName;
        }

        appObserver = new AppObserver(new Handler());
        getContentResolver().registerContentObserver(
                AppProvider.getHighestPriorityMetadataUri(app.packageName),
                true,
                appObserver);

        updateNotificationsForApp();
        refreshStatus();
        registerAppStatusReceiver();
    }

    /**
     * Figures out the current install/update/download/etc status for the app we are viewing.
     * Then, asks the view to update itself to reflect this status.
     */
    private void refreshStatus() {
        AppUpdateStatusManager ausm = AppUpdateStatusManager.getInstance(this);
        Iterator<AppUpdateStatusManager.AppUpdateStatus> statuses = ausm.getByPackageName(app.packageName).iterator();
        if (statuses.hasNext()) {
            AppUpdateStatusManager.AppUpdateStatus status = statuses.next();
            updateAppStatus(status, false);
        }

        currentStatus = null;
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterAppStatusReceiver();
    }

    protected void onStop() {
        super.onStop();
        visiblePackageName = null;

        getContentResolver().unregisterContentObserver(appObserver);

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
        if (app == null) {
            return true;
        }
        MenuItem itemIgnoreAll = menu.findItem(R.id.action_ignore_all);
        if (itemIgnoreAll != null) {
            itemIgnoreAll.setChecked(app.getPrefs(this).ignoreAllUpdates);
        }
        MenuItem itemIgnoreThis = menu.findItem(R.id.action_ignore_this);
        if (itemIgnoreThis != null) {
            itemIgnoreThis.setVisible(app.hasUpdates());
            itemIgnoreThis.setChecked(app.getPrefs(this).ignoreThisUpdate >= app.suggestedVersionCode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, app.name);
            shareIntent.putExtra(Intent.EXTRA_TEXT, app.name + " (" + app.summary
                    + ") - https://f-droid.org/app/" + app.packageName);

            boolean showNearbyItem = app.isInstalled(getApplicationContext()) && fdroidApp.bluetoothAdapter != null;
            CoordinatorLayout coordinatorLayout = (CoordinatorLayout) findViewById(R.id.rootCoordinator);
            ShareChooserDialog.createChooser(coordinatorLayout, this, this, shareIntent, showNearbyItem);
            return true;
        } else if (item.getItemId() == R.id.action_ignore_all) {
            app.getPrefs(this).ignoreAllUpdates ^= true;
            item.setChecked(app.getPrefs(this).ignoreAllUpdates);
            AppPrefsProvider.Helper.update(this, app, app.getPrefs(this));
            return true;
        } else if (item.getItemId() == R.id.action_ignore_this) {
            if (app.getPrefs(this).ignoreThisUpdate >= app.suggestedVersionCode) {
                app.getPrefs(this).ignoreThisUpdate = 0;
            } else {
                app.getPrefs(this).ignoreThisUpdate = app.suggestedVersionCode;
            }
            item.setChecked(app.getPrefs(this).ignoreThisUpdate > 0);
            AppPrefsProvider.Helper.update(this, app, app.getPrefs(this));
            return true;
        } else if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onNearby() {
        // If Bluetooth has not been enabled/turned on, then
        // enabling device discoverability will automatically enable Bluetooth
        Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 121);
        startActivityForResult(discoverBt, REQUEST_ENABLE_BLUETOOTH);
        // if this is successful, the Bluetooth transfer is started
    }

    @Override
    public void onResolvedShareIntent(Intent shareIntent) {
        startActivity(shareIntent);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                fdroidApp.sendViaBluetooth(this, resultCode, app.packageName);
                break;
            case REQUEST_PERMISSION_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
                    Uri uri = data.getData();
                    Apk apk = ApkProvider.Helper.findByUri(this, uri, Schema.ApkTable.Cols.ALL);
                    startInstall(apk);
                }
                break;
            case REQUEST_UNINSTALL_DIALOG:
                if (resultCode == Activity.RESULT_OK) {
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
            builder.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            initiateInstall(apk);
                        }
                    });
            builder.setNegativeButton(R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        if (app.installedSig != null && apk.sig != null
                && !apk.sig.equals(app.installedSig)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.SignatureMismatch).setPositiveButton(
                    R.string.ok,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
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
        if (isAppDownloading()) {
            Log.i(TAG, "Ignoring request to install " + apk.packageName + " version " + apk.versionName
                    + ", as we are already downloading (either that or a different version).");
            return;
        }

        Installer installer = InstallerFactory.create(this, apk);
        Intent intent = installer.getPermissionScreen();
        if (intent != null) {
            // permission screen required
            Utils.debugLog(TAG, "permission screen required");
            startActivityForResult(intent, REQUEST_PERMISSION_DIALOG);
            return;
        }

        startInstall(apk);
    }

    private void startInstall(Apk apk) {
        InstallManagerService.queue(this, app, apk);
    }

    private void startUninstall() {
        registerUninstallReceiver();
        InstallerService.uninstall(this, app.installedApk);
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
        this.currentStatus = newStatus;
        if (this.currentStatus == null) {
            return;
        }

        switch (newStatus.status) {
            case Downloading:
                if (newStatus.progressMax == 0) {
                    // The first progress notification we get telling us our status is "Downloading"
                    adapter.setIndeterminateProgress(R.string.download_pending);
                } else {
                    adapter.setProgress(newStatus.progressCurrent, newStatus.progressMax);
                }
                break;

            case ReadyToInstall:
                if (justReceived) {
                    adapter.setIndeterminateProgress(R.string.installing);
                    localBroadcastManager.registerReceiver(installReceiver,
                            Installer.getInstallIntentFilter(Uri.parse(newStatus.getUniqueKey())));
                }
                break;

            case DownloadInterrupted:
                if (justReceived) {
                    if (TextUtils.isEmpty(newStatus.errorText)) {
                        Toast.makeText(this, R.string.details_notinstalled, Toast.LENGTH_LONG).show();
                    } else {
                        String msg = newStatus.errorText + " " + newStatus.getUniqueKey();
                        Toast.makeText(this, R.string.download_error, Toast.LENGTH_SHORT).show();
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }

                    adapter.clearProgress();
                }
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
                    && !TextUtils.equals(status.getUniqueKey(), currentStatus.getUniqueKey())) {
                Utils.debugLog(TAG, "Ignoring app status change because it belongs to "
                        + status.getUniqueKey() + " not " + currentStatus.getUniqueKey());
            } else if (status != null && !TextUtils.equals(status.apk.packageName, app.packageName)) {
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
                    // return control back to us until after it has already braodcast to the
                    // InstalledAppProviderService. This means that we are not listening for any
                    // feedback from InstalledAppProviderService (we intentionally stop listening in
                    // onPause). Empirically, this happens when upgrading an app rather than a clean
                    // install. However given the nature of this race condition, it may be different
                    // on different operating systems. As such, we'll just update our view now. It may
                    // happen again in our appObserver, but that will only cause a little more load
                    // on the system, it shouldn't cause a different UX.
                    onAppChanged();
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    adapter.clearProgress();
                    onAppChanged();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage) && !isFinishing()) {
                        Log.e(TAG, "install aborted with errorMessage: " + errorMessage);

                        String title = String.format(
                                getString(R.string.install_error_notify_title),
                                app.name);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
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
                    onAppChanged();
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    adapter.clearProgress();
                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "uninstall aborted with errorMessage: " + errorMessage);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
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
     * also when something has been installed/uninstalled.
     * Return true if the app was found, false otherwise.
     */
    private boolean reset(String packageName) {

        Utils.debugLog(TAG, "Getting application details for " + packageName);
        App newApp = null;

        if (!TextUtils.isEmpty(packageName)) {
            newApp = AppProvider.Helper.findHighestPriorityMetadata(getContentResolver(), packageName);
        }

        setApp(newApp);
        return this.app != null;
    }

    private void onAppChanged() {
        recyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (!reset(app.packageName)) {
                    AppDetails2.this.finish();
                    return;
                }
                AppDetailsRecyclerViewAdapter adapter = (AppDetailsRecyclerViewAdapter) recyclerView.getAdapter();
                adapter.updateItems(app);
                refreshStatus();
                supportInvalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean isAppDownloading() {
        return currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.Downloading;
    }

    @Override
    public void enableAndroidBeam() {
        NfcHelper.setAndroidBeam(this, app.packageName);
    }

    @Override
    public void disableAndroidBeam() {
        NfcHelper.disableAndroidBeam(this);
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
        if (isAppDownloading()) {
            InstallManagerService.cancel(this, currentStatus.getUniqueKey());
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

    @Override
    public void installApk() {
        Apk apkToInstall = ApkProvider.Helper.findSuggestedApk(this, app);
        installApk(apkToInstall);
    }

    @Override
    public void upgradeApk() {
        Apk apkToInstall = ApkProvider.Helper.findSuggestedApk(this, app);
        installApk(apkToInstall);
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
                // When the app isn't a media file - the above workaround refers to this.
                apk = app.getInstalledApk(this);
                if (apk == null) {
                    Log.d(TAG, "Couldn't find installed apk for " + app.packageName);
                    Toast.makeText(this, R.string.uninstall_error_unknown, Toast.LENGTH_SHORT).show();
                    uninstallReceiver.onReceive(this, new Intent(Installer.ACTION_UNINSTALL_INTERRUPTED));
                    return;
                }
            }
            app.installedApk = apk;
        }
        Installer installer = InstallerFactory.create(this, apk);
        Intent intent = installer.getUninstallScreen();
        if (intent != null) {
            // uninstall screen required
            Utils.debugLog(TAG, "screen screen required");
            startActivityForResult(intent, REQUEST_UNINSTALL_DIALOG);
            return;
        }
        startUninstall();
    }

    // observer to update view when package has been installed/deleted
    private AppObserver appObserver;

    class AppObserver extends ContentObserver {

        AppObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            onAppChanged();
        }

    }

}
