package org.fdroid.fdroid;

import android.app.Activity;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

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
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.fdroid.views.AppDetailsRecyclerViewAdapter;
import org.fdroid.fdroid.views.ShareChooserDialog;

public class AppDetails2 extends AppCompatActivity implements ShareChooserDialog.ShareChooserDialogListener, AppDetailsRecyclerViewAdapter.AppDetailsRecyclerViewAdapterCallbacks {
    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    private static final String TAG = "AppDetails2";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_PERMISSION_DIALOG = 3;
    private static final int REQUEST_UNINSTALL_DIALOG = 4;

    private FDroidApp mFDroidApp;
    private App mApp;
    private RecyclerView mRecyclerView;
    private AppDetailsRecyclerViewAdapter mAdapter;
    private LocalBroadcastManager mLocalBroadcastManager;
    private String mActiveDownloadUrlString;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mFDroidApp = (FDroidApp) getApplication();
        //mFDroidApp.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_details2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(""); // Nice and clean toolbar
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (!reset(getPackageNameFromIntent(getIntent()))) {
            finish();
            return;
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        mRecyclerView = (RecyclerView) findViewById(R.id.rvDetails);
        mAdapter = new AppDetailsRecyclerViewAdapter(this, mApp, this);
        LinearLayoutManager lm = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        lm.setStackFromEnd(false);
        mRecyclerView.setLayoutManager(lm);
        mRecyclerView.setAdapter(mAdapter);

        // Load the feature graphic, if present
        if (!TextUtils.isEmpty(mApp.iconUrlLarge)) {
            ImageView ivFeatureGraphic = (ImageView) findViewById(R.id.feature_graphic);
            DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
                    .cacheInMemory(false)
                    .cacheOnDisk(true)
                    .imageScaleType(ImageScaleType.NONE)
                    .bitmapConfig(Bitmap.Config.RGB_565)
                    .build();
            ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, ivFeatureGraphic, displayImageOptions);
        }
    }

    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(AppDetails.EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }
        return intent.getStringExtra(AppDetails.EXTRA_APPID);
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
        mApp = newApp;
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
        if (mApp == null) {
            return true;
        }
        MenuItem itemIgnoreAll = menu.findItem(R.id.action_ignore_all);
        if (itemIgnoreAll != null) {
            itemIgnoreAll.setChecked(mApp.getPrefs(this).ignoreAllUpdates);
        }
        MenuItem itemIgnoreThis = menu.findItem(R.id.action_ignore_this);
        if (itemIgnoreThis != null) {
            itemIgnoreThis.setVisible(mApp.hasUpdates());
            itemIgnoreThis.setChecked(mApp.getPrefs(this).ignoreThisUpdate >= mApp.suggestedVersionCode);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, mApp.name);
            shareIntent.putExtra(Intent.EXTRA_TEXT, mApp.name + " (" + mApp.summary + ") - https://f-droid.org/app/" + mApp.packageName);

            boolean showNearbyItem = mApp.isInstalled() && mFDroidApp.bluetoothAdapter != null;
            ShareChooserDialog.createChooser((CoordinatorLayout) findViewById(R.id.rootCoordinator), this, this, shareIntent, showNearbyItem);
            return true;
        } else if (item.getItemId() == R.id.action_ignore_all) {
            mApp.getPrefs(this).ignoreAllUpdates ^= true;
            item.setChecked(mApp.getPrefs(this).ignoreAllUpdates);
            AppPrefsProvider.Helper.update(this, mApp, mApp.getPrefs(this));
            return true;
        } else if (item.getItemId() == R.id.action_ignore_this) {
            if (mApp.getPrefs(this).ignoreThisUpdate >= mApp.suggestedVersionCode) {
                mApp.getPrefs(this).ignoreThisUpdate = 0;
            } else {
                mApp.getPrefs(this).ignoreThisUpdate = mApp.suggestedVersionCode;
            }
            item.setChecked(mApp.getPrefs(this).ignoreThisUpdate > 0);
            AppPrefsProvider.Helper.update(this, mApp, mApp.getPrefs(this));
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
                mFDroidApp.sendViaBluetooth(this, resultCode, mApp.packageName);
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
        if (mApp.installedSig != null && apk.sig != null
                && !apk.sig.equals(mApp.installedSig)) {
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
    }

    private void initiateInstall(Apk apk) {
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
        mActiveDownloadUrlString = apk.getUrl();
        registerDownloaderReceiver();
        InstallManagerService.queue(this, mApp, apk);
    }

    private void startUninstall() {
        registerUninstallReceiver();
        InstallerService.uninstall(this, mApp.installedApk);
    }

    private void registerUninstallReceiver() {
        mLocalBroadcastManager.registerReceiver(uninstallReceiver,
                Installer.getUninstallIntentFilter(mApp.packageName));
    }

    private void unregisterUninstallReceiver() {
        mLocalBroadcastManager.unregisterReceiver(uninstallReceiver);
    }

    private void registerDownloaderReceiver() {
        if (mActiveDownloadUrlString != null) { // if a download is active
            String url = mActiveDownloadUrlString;
            mLocalBroadcastManager.registerReceiver(downloadReceiver,
                    DownloaderService.getIntentFilter(url));
        }
    }

    private void unregisterDownloaderReceiver() {
        mLocalBroadcastManager.unregisterReceiver(downloadReceiver);
    }

    private void unregisterInstallReceiver() {
        mLocalBroadcastManager.unregisterReceiver(installReceiver);
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Downloader.ACTION_STARTED:
                    mAdapter.setProgress(-1, -1, R.string.download_pending);
                    break;
                case Downloader.ACTION_PROGRESS:
                    mAdapter.setProgress(intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1),
                            intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1), 0);
                    break;
                case Downloader.ACTION_COMPLETE:
                    // Starts the install process once the download is complete.
                    cleanUpFinishedDownload();
                    mLocalBroadcastManager.registerReceiver(installReceiver,
                            Installer.getInstallIntentFilter(intent.getData()));
                    break;
                case Downloader.ACTION_INTERRUPTED:
                    if (intent.hasExtra(Downloader.EXTRA_ERROR_MESSAGE)) {
                        String msg = intent.getStringExtra(Downloader.EXTRA_ERROR_MESSAGE)
                                + " " + intent.getDataString();
                        Toast.makeText(context, R.string.download_error, Toast.LENGTH_SHORT).show();
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
                    } else { // user canceled
                        Toast.makeText(context, R.string.details_notinstalled, Toast.LENGTH_LONG).show();
                    }
                    cleanUpFinishedDownload();
                    break;
                default:
                    throw new RuntimeException("intent action not handled!");
            }
        }
    };

    private final BroadcastReceiver installReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_INSTALL_STARTED:
                    mAdapter.setProgress(-1, -1, R.string.installing);
                    break;
                case Installer.ACTION_INSTALL_COMPLETE:
                    mAdapter.clearProgress();
                    unregisterInstallReceiver();
                    onAppChanged();
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    mAdapter.clearProgress();
                    onAppChanged();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "install aborted with errorMessage: " + errorMessage);

                        String title = String.format(
                                getString(R.string.install_error_notify_title),
                                mApp.name);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
                        alertBuilder.setTitle(title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }
                    unregisterInstallReceiver();
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
    };

    private final BroadcastReceiver uninstallReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Installer.ACTION_UNINSTALL_STARTED:
                    mAdapter.setProgress(-1, -1, R.string.uninstalling);
                    break;
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    mAdapter.clearProgress();
                    onAppChanged();
                    unregisterUninstallReceiver();
                    break;
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    mAdapter.clearProgress();
                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "uninstall aborted with errorMessage: " + errorMessage);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails2.this);
                        alertBuilder.setTitle(R.string.uninstall_error_notify_title);
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

        calcActiveDownloadUrlString(packageName);

        if (!TextUtils.isEmpty(packageName)) {
            newApp = AppProvider.Helper.findHighestPriorityMetadata(getContentResolver(), packageName);
        }

        setApp(newApp);
        return this.mApp != null;
    }

    private void calcActiveDownloadUrlString(String packageName) {
        String urlString = getPreferences(MODE_PRIVATE).getString(packageName, null);
        if (DownloaderService.isQueuedOrActive(urlString)) {
            mActiveDownloadUrlString = urlString;
        } else {
            // this URL is no longer active, remove it
            getPreferences(MODE_PRIVATE).edit().remove(packageName).apply();
        }
    }

    /**
     * Remove progress listener, suppress progress bar, set downloadHandler to null.
     */
    private void cleanUpFinishedDownload() {
        mActiveDownloadUrlString = null;
        mAdapter.clearProgress();
        unregisterDownloaderReceiver();
    }

    private void onAppChanged() {
        mRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                if (!reset(mApp.packageName)) {
                    AppDetails2.this.finish();
                    return;
                }
                AppDetailsRecyclerViewAdapter adapter = (AppDetailsRecyclerViewAdapter)mRecyclerView.getAdapter();
                adapter.updateItems(mApp);
                adapter.notifyDataSetChanged();
                supportInvalidateOptionsMenu();
            }
        });
    }

    @Override
    public boolean isAppDownloading() {
        return !TextUtils.isEmpty(mActiveDownloadUrlString);
    }

    @Override
    public boolean isAppInstalled() {
        return mApp.isInstalled();
    }

    @Override
    public void enableAndroidBeam() {
        NfcHelper.setAndroidBeam(this, mApp.packageName);
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
        if (!TextUtils.isEmpty(mActiveDownloadUrlString)) {
            InstallManagerService.cancel(this, mActiveDownloadUrlString);
        }
    }

    @Override
    public void launchApk() {
        Intent intent = getPackageManager().getLaunchIntentForPackage(mApp.packageName);
        startActivity(intent);
    }

    @Override
    public void installApk() {
        Apk apkToInstall = ApkProvider.Helper.findApkFromAnyRepo(AppDetails2.this, mApp.packageName, mApp.suggestedVersionCode);
        installApk(apkToInstall);
    }

    @Override
    public void upgradeApk() {
        Apk apkToInstall = ApkProvider.Helper.findApkFromAnyRepo(AppDetails2.this, mApp.packageName, mApp.suggestedVersionCode);
        installApk(apkToInstall);
    }

    @Override
    public void uninstallApk() {
        Apk apk = mApp.installedApk;
        if (apk == null) {
            // TODO ideally, app would be refreshed immediately after install, then this
            // workaround would be unnecessary
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(mApp.packageName, 0);
                apk = ApkProvider.Helper.findApkFromAnyRepo(this, pi.packageName, pi.versionCode);
                mApp.installedApk = apk;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return; // not installed
            }
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
}
