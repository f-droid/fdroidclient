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
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.Layout;
import android.text.Selection;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefs;
import org.fdroid.fdroid.data.AppPrefsProvider;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;
import org.fdroid.fdroid.installer.InstallerService;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;
import org.fdroid.fdroid.privileged.views.AppDiff;
import org.fdroid.fdroid.privileged.views.AppSecurityPermissions;

import java.util.List;

public class AppDetails extends AppCompatActivity {

    private static final String TAG = "AppDetails";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;
    private static final int REQUEST_PERMISSION_DIALOG = 3;
    private static final int REQUEST_UNINSTALL_DIALOG = 4;

    public static final String EXTRA_APPID = "appid";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_HINT_SEARCHING = "searching";

    private FDroidApp fdroidApp;
    private ApkListAdapter adapter;

    /**
     * Check if {@code packageName} is currently visible to the user.
     */
    public static boolean isAppVisible(String packageName) {
        return packageName != null && packageName.equals(visiblePackageName);
    }

    private static String visiblePackageName;

    private static class ViewHolder {
        TextView version;
        TextView status;
        TextView repository;
        TextView size;
        TextView api;
        TextView incompatibleReasons;
        TextView buildtype;
        TextView added;
        TextView nativecode;
    }

    // observer to update view when package has been installed/deleted
    private AppObserver myAppObserver;

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

    class ApkListAdapter extends ArrayAdapter<Apk> {

        private final LayoutInflater mInflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        ApkListAdapter(Context context, App app) {
            super(context, 0);
            final List<Apk> apks = ApkProvider.Helper.findByPackageName(context, app.packageName);
            for (final Apk apk : apks) {
                if (apk.compatible || Preferences.get().showIncompatibleVersions()) {
                    add(apk);
                }
            }
        }

        private String getInstalledStatus(final Apk apk) {
            // Definitely not installed.
            if (apk.versionCode != app.installedVersionCode) {
                return getString(R.string.app_not_installed);
            }
            // Definitely installed this version.
            if (apk.sig != null && apk.sig.equals(app.installedSig)) {
                return getString(R.string.app_installed);
            }
            // Installed the same version, but from someplace else.
            final String installerPkgName;
            try {
                installerPkgName = packageManager.getInstallerPackageName(app.packageName);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Application " + app.packageName + " is not installed anymore");
                return getString(R.string.app_not_installed);
            }
            if (TextUtils.isEmpty(installerPkgName)) {
                return getString(R.string.app_inst_unknown_source);
            }
            final String installerLabel = InstalledAppProvider
                    .getApplicationLabel(context, installerPkgName);
            return getString(R.string.app_inst_known_source, installerLabel);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            java.text.DateFormat df = DateFormat.getDateFormat(context);
            final Apk apk = getItem(position);
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.apklistitem, parent, false);

                holder = new ViewHolder();
                holder.version = (TextView) convertView.findViewById(R.id.version);
                holder.status = (TextView) convertView.findViewById(R.id.status);
                holder.repository = (TextView) convertView.findViewById(R.id.repository);
                holder.size = (TextView) convertView.findViewById(R.id.size);
                holder.api = (TextView) convertView.findViewById(R.id.api);
                holder.incompatibleReasons = (TextView) convertView.findViewById(R.id.incompatible_reasons);
                holder.buildtype = (TextView) convertView.findViewById(R.id.buildtype);
                holder.added = (TextView) convertView.findViewById(R.id.added);
                holder.nativecode = (TextView) convertView.findViewById(R.id.nativecode);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.version.setText(getString(R.string.version)
                    + " " + apk.versionName
                    + (apk.versionCode == app.suggestedVersionCode ? "  ☆" : ""));

            holder.status.setText(getInstalledStatus(apk));

            holder.repository.setText(getString(R.string.repo_provider,
                    RepoProvider.Helper.findById(getContext(), apk.repo).getName()));

            if (apk.size > 0) {
                holder.size.setText(Utils.getFriendlySize(apk.size));
                holder.size.setVisibility(View.VISIBLE);
            } else {
                holder.size.setVisibility(View.GONE);
            }

            if (!Preferences.get().expertMode()) {
                holder.api.setVisibility(View.GONE);
            } else if (apk.minSdkVersion > 0 && apk.maxSdkVersion < Apk.SDK_VERSION_MAX_VALUE) {
                holder.api.setText(getString(R.string.minsdk_up_to_maxsdk,
                            Utils.getAndroidVersionName(apk.minSdkVersion),
                            Utils.getAndroidVersionName(apk.maxSdkVersion)));
                holder.api.setVisibility(View.VISIBLE);
            } else if (apk.minSdkVersion > 0) {
                holder.api.setText(getString(R.string.minsdk_or_later,
                            Utils.getAndroidVersionName(apk.minSdkVersion)));
                holder.api.setVisibility(View.VISIBLE);
            } else if (apk.maxSdkVersion > 0) {
                holder.api.setText(getString(R.string.up_to_maxsdk,
                            Utils.getAndroidVersionName(apk.maxSdkVersion)));
                holder.api.setVisibility(View.VISIBLE);
            }

            if (apk.srcname != null) {
                holder.buildtype.setText("source");
            } else {
                holder.buildtype.setText("bin");
            }

            if (apk.added != null) {
                holder.added.setText(getString(R.string.added_on,
                            df.format(apk.added)));
                holder.added.setVisibility(View.VISIBLE);
            } else {
                holder.added.setVisibility(View.GONE);
            }

            if (Preferences.get().expertMode() && apk.nativecode != null) {
                holder.nativecode.setText(TextUtils.join(" ", apk.nativecode));
                holder.nativecode.setVisibility(View.VISIBLE);
            } else {
                holder.nativecode.setVisibility(View.GONE);
            }

            if (apk.incompatibleReasons != null) {
                holder.incompatibleReasons.setText(
                        getResources().getString(
                            R.string.requires_features,
                            TextUtils.join(", ", apk.incompatibleReasons)));
                holder.incompatibleReasons.setVisibility(View.VISIBLE);
            } else {
                holder.incompatibleReasons.setVisibility(View.GONE);
            }

            // Disable it all if it isn't compatible...
            final View[] views = {
                convertView,
                holder.version,
                holder.status,
                holder.repository,
                holder.size,
                holder.api,
                holder.buildtype,
                holder.added,
                holder.nativecode,
            };

            for (final View v : views) {
                v.setEnabled(apk.compatible);
            }

            return convertView;
        }
    }

    private static final int INSTALL            = Menu.FIRST;
    private static final int UNINSTALL          = Menu.FIRST + 1;
    private static final int IGNOREALL          = Menu.FIRST + 2;
    private static final int IGNORETHIS         = Menu.FIRST + 3;
    private static final int LAUNCH             = Menu.FIRST + 4;
    private static final int SHARE              = Menu.FIRST + 5;
    private static final int SEND_VIA_BLUETOOTH = Menu.FIRST + 6;

    private App app;
    private PackageManager packageManager;
    private String activeDownloadUrlString;
    private LocalBroadcastManager localBroadcastManager;

    private AppPrefs startingPrefs;

    private final Context context = this;

    private AppDetailsHeaderFragment headerFragment;

    /**
     * Stores relevant data that we want to keep track of when destroying the activity
     * with the expectation of it being recreated straight away (e.g. after an
     * orientation change). One of the major things is that we want the download thread
     * to stay active, but for it not to trigger any UI stuff (e.g. progress bar)
     * between the activity being destroyed and recreated.
     */
    private static class ConfigurationChangeHelper {

        public final String urlString;
        public final App app;

        ConfigurationChangeHelper(String urlString, App app) {
            this.urlString = urlString;
            this.app = app;
        }
    }

    /**
     * Attempt to extract the packageName from the intent which launched this activity.
     * @return May return null, if we couldn't find the packageName. This should
     * never happen as AppDetails is only to be called by the FDroid activity
     * and not externally.
     */
    private String getPackageNameFromIntent(Intent intent) {
        if (!intent.hasExtra(EXTRA_APPID)) {
            Log.e(TAG, "No package name found in the intent!");
            return null;
        }

        return intent.getStringExtra(EXTRA_APPID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fdroidApp = (FDroidApp) getApplication();
        fdroidApp.applyTheme(this);

        super.onCreate(savedInstanceState);

        // Must be called *after* super.onCreate(), as that is where the action bar
        // compat implementation is assigned in the ActionBarActivity base class.
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        Intent intent = getIntent();
        if (intent.hasExtra(EXTRA_FROM)) {
            setTitle(intent.getStringExtra(EXTRA_FROM));
        }

        packageManager = getPackageManager();

        // Get the preferences we're going to use in this Activity...
        ConfigurationChangeHelper previousData = (ConfigurationChangeHelper) getLastCustomNonConfigurationInstance();
        if (previousData != null) {
            Utils.debugLog(TAG, "Recreating view after configuration change.");
            activeDownloadUrlString = previousData.urlString;
            if (activeDownloadUrlString != null) {
                Utils.debugLog(TAG, "Download was in progress before the configuration change, so we will start to listen to its events again.");
            }
            app = previousData.app;
            setApp(app);
        } else {
            if (!reset(getPackageNameFromIntent(intent))) {
                finish();
                return;
            }
        }

        // Set up the list...
        adapter = new ApkListAdapter(this, app);

        // Wait until all other intialization before doing this, because it will create the
        // fragments, which rely on data from the activity that is set earlier in this method.
        setContentView(R.layout.app_details);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Check for the presence of a view which only exists in the landscape view.
        // This seems to be the preferred way to interrogate the view, rather than
        // to check the orientation. I guess this is because views can be dynamically
        // chosen based on more than just orientation (e.g. large screen sizes).
        View onlyInLandscape = findViewById(R.id.app_summary_container);

        AppDetailsListFragment listFragment =
                (AppDetailsListFragment) getSupportFragmentManager().findFragmentById(R.id.fragment_app_list);
        if (onlyInLandscape == null) {
            listFragment.setupSummaryHeader();
        } else {
            listFragment.removeSummaryHeader();
        }

        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // register observer to know when install status changes
        myAppObserver = new AppObserver(new Handler());
        getContentResolver().registerContentObserver(
                AppProvider.getContentUri(app.packageName),
                true,
                myAppObserver);
    }

    @Override
    protected void onResumeFragments() {
        // Must be called before super.onResumeFragments(), as the fragments depend on the active
        // url being correctly set in order to know whether or not to show the download progress bar.
        calcActiveDownloadUrlString(app.packageName);

        super.onResumeFragments();

        headerFragment = (AppDetailsHeaderFragment) getSupportFragmentManager().findFragmentById(R.id.header);
        refreshApkList();
        supportInvalidateOptionsMenu();
        if (DownloaderService.isQueuedOrActive(activeDownloadUrlString)) {
            registerDownloaderReceiver();
        }
        visiblePackageName = app.packageName;
    }

    /**
     * Remove progress listener, suppress progress bar, set downloadHandler to null.
     */
    private void cleanUpFinishedDownload() {
        activeDownloadUrlString = null;
        if (headerFragment != null) {
            headerFragment.removeProgress();
        }
        unregisterDownloaderReceiver();
    }

    protected void onStop() {
        super.onStop();
        getContentResolver().unregisterContentObserver(myAppObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        visiblePackageName = null;
        // save the active URL for this app in case we come back
        getPreferences(MODE_PRIVATE)
            .edit()
            .putString(getPackageNameFromIntent(getIntent()), activeDownloadUrlString)
            .apply();
        if (app != null && !app.getPrefs(this).equals(startingPrefs)) {
            Utils.debugLog(TAG, "Updating 'ignore updates', as it has changed since we started the activity...");
            AppPrefsProvider.Helper.update(this, app, app.getPrefs(this));
        }
        unregisterDownloaderReceiver();
    }

    private void unregisterDownloaderReceiver() {
        if (localBroadcastManager == null) {
            return;
        }
        localBroadcastManager.unregisterReceiver(downloadReceiver);
    }

    private void registerDownloaderReceiver() {
        if (activeDownloadUrlString != null) { // if a download is active
            String url = activeDownloadUrlString;
            localBroadcastManager.registerReceiver(downloadReceiver,
                    DownloaderService.getIntentFilter(url));
        }
    }

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case Downloader.ACTION_STARTED:
                    if (headerFragment != null) {
                        headerFragment.startProgress();
                    }
                    break;
                case Downloader.ACTION_PROGRESS:
                    if (headerFragment != null) {
                        headerFragment.updateProgress(intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1),
                                intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1));
                    }
                    break;
                case Downloader.ACTION_COMPLETE:
                    // Starts the install process one the download is complete.
                    cleanUpFinishedDownload();
                    localBroadcastManager.registerReceiver(installReceiver,
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
                    headerFragment.startProgress(false);
                    headerFragment.showIndeterminateProgress(getString(R.string.installing));
                    break;
                case Installer.ACTION_INSTALL_COMPLETE:
                    headerFragment.removeProgress();

                    localBroadcastManager.unregisterReceiver(this);
                    break;
                case Installer.ACTION_INSTALL_INTERRUPTED:
                    headerFragment.removeProgress();
                    onAppChanged();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "install aborted with errorMessage: " + errorMessage);

                        String title = String.format(
                                getString(R.string.install_error_notify_title),
                                app.name);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails.this);
                        alertBuilder.setTitle(title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }

                    localBroadcastManager.unregisterReceiver(this);
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
                    headerFragment.startProgress(false);
                    headerFragment.showIndeterminateProgress(getString(R.string.uninstalling));
                    break;
                case Installer.ACTION_UNINSTALL_COMPLETE:
                    headerFragment.removeProgress();
                    onAppChanged();

                    localBroadcastManager.unregisterReceiver(this);
                    break;
                case Installer.ACTION_UNINSTALL_INTERRUPTED:
                    headerFragment.removeProgress();

                    String errorMessage =
                            intent.getStringExtra(Installer.EXTRA_ERROR_MESSAGE);

                    if (!TextUtils.isEmpty(errorMessage)) {
                        Log.e(TAG, "uninstall aborted with errorMessage: " + errorMessage);

                        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails.this);
                        alertBuilder.setTitle(R.string.uninstall_error_notify_title);
                        alertBuilder.setMessage(errorMessage);
                        alertBuilder.setNeutralButton(android.R.string.ok, null);
                        alertBuilder.create().show();
                    }

                    localBroadcastManager.unregisterReceiver(this);
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

    private void onAppChanged() {
        if (!reset(app.packageName)) {
            this.finish();
            return;
        }

        refreshApkList();
        refreshHeader();
        supportInvalidateOptionsMenu();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return new ConfigurationChangeHelper(activeDownloadUrlString, app);
    }

    @Override
    protected void onDestroy() {
        unregisterDownloaderReceiver();
        super.onDestroy();
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset(String packageName) {

        Utils.debugLog(TAG, "Getting application details for " + packageName);
        App newApp = null;

        calcActiveDownloadUrlString(packageName);

        if (!TextUtils.isEmpty(packageName)) {
            newApp = AppProvider.Helper.findByPackageName(getContentResolver(), packageName);
        }

        setApp(newApp);

        return this.app != null;
    }

    private void calcActiveDownloadUrlString(String packageName) {
        String urlString = getPreferences(MODE_PRIVATE).getString(packageName, null);
        if (DownloaderService.isQueuedOrActive(urlString)) {
            activeDownloadUrlString = urlString;
        } else {
            // this URL is no longer active, remove it
            getPreferences(MODE_PRIVATE).edit().remove(packageName).apply();
        }
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

        startingPrefs = app.getPrefs(this).createClone();
    }

    private void refreshApkList() {
        adapter.notifyDataSetChanged();
    }

    private void refreshHeader() {
        if (headerFragment != null) {
            headerFragment.updateViews();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (app == null) {
            return true;
        }

        if (packageManager.getLaunchIntentForPackage(app.packageName) != null && app.canAndWantToUpdate(this)) {
            MenuItemCompat.setShowAsAction(menu.add(
                            Menu.NONE, LAUNCH, 1, R.string.menu_launch)
                            .setIcon(R.drawable.ic_play_arrow_white),
                    MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (app.isInstalled()) {
            MenuItemCompat.setShowAsAction(menu.add(
                            Menu.NONE, UNINSTALL, 1, R.string.menu_uninstall)
                            .setIcon(R.drawable.ic_delete_white),
                    MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, SHARE, 1, R.string.menu_share)
                        .setIcon(R.drawable.ic_share_white),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                        MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(Menu.NONE, IGNOREALL, 2, R.string.menu_ignore_all)
                    .setIcon(R.drawable.ic_do_not_disturb_white)
                    .setCheckable(true)
                    .setChecked(app.getPrefs(context).ignoreAllUpdates);

        if (app.hasUpdates()) {
            menu.add(Menu.NONE, IGNORETHIS, 2, R.string.menu_ignore_this)
                    .setIcon(R.drawable.ic_do_not_disturb_white)
                    .setCheckable(true)
                    .setChecked(app.getPrefs(context).ignoreThisUpdate >= app.suggestedVersionCode);
        }

        // Ignore on devices without Bluetooth
        if (app.isInstalled() && fdroidApp.bluetoothAdapter != null) {
            menu.add(Menu.NONE, SEND_VIA_BLUETOOTH, 3, R.string.send_via_bluetooth)
                    .setIcon(R.drawable.ic_bluetooth_white);
        }
        return true;
    }

    private void tryOpenUri(String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        if (intent.resolveActivity(packageManager) == null) {
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private static final class SafeLinkMovementMethod extends LinkMovementMethod {

        private static SafeLinkMovementMethod instance;

        private final Context ctx;

        private SafeLinkMovementMethod(Context ctx) {
            this.ctx = ctx;
        }

        public static SafeLinkMovementMethod getInstance(Context ctx) {
            if (instance == null) {
                instance = new SafeLinkMovementMethod(ctx);
            }
            return instance;
        }

        private static CharSequence getLink(TextView widget, Spannable buffer,
                MotionEvent event) {
            int x = (int) event.getX();
            int y = (int) event.getY();
            x -= widget.getTotalPaddingLeft();
            y -= widget.getTotalPaddingTop();
            x += widget.getScrollX();
            y += widget.getScrollY();

            Layout layout = widget.getLayout();
            final int line = layout.getLineForVertical(y);
            final int off = layout.getOffsetForHorizontal(line, x);
            final ClickableSpan[] links = buffer.getSpans(off, off, ClickableSpan.class);

            if (links.length > 0) {
                final ClickableSpan link = links[0];
                final Spanned s = (Spanned) widget.getText();
                return s.subSequence(s.getSpanStart(link), s.getSpanEnd(link));
            }
            return "null";
        }

        @Override
        public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer,
                @NonNull MotionEvent event) {
            try {
                return super.onTouchEvent(widget, buffer, event);
            } catch (ActivityNotFoundException ex) {
                Selection.removeSelection(buffer);
                final CharSequence link = getLink(widget, buffer, event);
                Toast.makeText(ctx,
                        ctx.getString(R.string.no_handler_app, link),
                        Toast.LENGTH_LONG).show();
                return true;
            }
        }

    }

    private void navigateUp() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case android.R.id.home:
                if (getIntent().hasExtra(EXTRA_HINT_SEARCHING)) {
                    finish();
                } else {
                    navigateUp();
                }
                return true;

            case LAUNCH:
                launchApk(app.packageName);
                return true;

            case SHARE:
                shareApp(app);
                return true;

            case INSTALL:
                // Note that this handles updating as well as installing.
                if (app.suggestedVersionCode > 0) {
                    final Apk apkToInstall = ApkProvider.Helper.find(this, app.packageName, app.suggestedVersionCode);
                    install(apkToInstall);
                }
                return true;

            case UNINSTALL:
                uninstallApk(app.packageName);
                return true;

            case IGNOREALL:
                app.getPrefs(this).ignoreAllUpdates ^= true;
                item.setChecked(app.getPrefs(this).ignoreAllUpdates);
                return true;

            case IGNORETHIS:
                if (app.getPrefs(this).ignoreThisUpdate >= app.suggestedVersionCode) {
                    app.getPrefs(this).ignoreThisUpdate = 0;
                } else {
                    app.getPrefs(this).ignoreThisUpdate = app.suggestedVersionCode;
                }
                item.setChecked(app.getPrefs(this).ignoreThisUpdate > 0);
                return true;

            case SEND_VIA_BLUETOOTH:
                /*
                 * If Bluetooth has not been enabled/turned on, then
                 * enabling device discoverability will automatically enable Bluetooth
                 */
                Intent discoverBt = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverBt.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 121);
                startActivityForResult(discoverBt, REQUEST_ENABLE_BLUETOOTH);
                // if this is successful, the Bluetooth transfer is started
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Install the version of this app denoted by 'app.curApk'.
    private void install(final Apk apk) {
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
    }

    private void initiateInstall(Apk apk) {
        Installer installer = InstallerFactory.create(this, apk);
        Intent intent = installer.getPermissionScreen(apk);
        if (intent != null) {
            // permission screen required
            Utils.debugLog(TAG, "permission screen required");
            startActivityForResult(intent, REQUEST_PERMISSION_DIALOG);
            return;
        }

        startInstall(apk);
    }

    private void startInstall(Apk apk) {
        activeDownloadUrlString = apk.getUrl();
        registerDownloaderReceiver();
        InstallManagerService.queue(this, app, apk);
    }

    private void uninstallApk(String packageName) {
        Installer installer = InstallerFactory.create(this, null);
        Intent intent = installer.getUninstallScreen(packageName);
        if (intent != null) {
            // uninstall screen required
            Utils.debugLog(TAG, "screen screen required");
            startActivityForResult(intent, REQUEST_UNINSTALL_DIALOG);
            return;
        }

        startUninstall();
    }

    private void startUninstall() {
        localBroadcastManager.registerReceiver(uninstallReceiver,
                Installer.getUninstallIntentFilter(app.packageName));
        InstallerService.uninstall(context, app.packageName);
    }

    private void launchApk(String packageName) {
        Intent intent = packageManager.getLaunchIntentForPackage(packageName);
        startActivity(intent);
    }

    private void shareApp(App app) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, app.name);
        shareIntent.putExtra(Intent.EXTRA_TEXT, app.name + " (" + app.summary + ") - https://f-droid.org/app/" + app.packageName);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
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
                    Apk apk = ApkProvider.Helper.find(this, uri, Schema.ApkTable.Cols.ALL);
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

    private App getApp() {
        return app;
    }

    private ApkListAdapter getApks() {
        return adapter;
    }

    public static class AppDetailsSummaryFragment extends Fragment {

        final Preferences prefs;
        private AppDetails appDetails;
        private static final int MAX_LINES = 5;
        private static boolean viewAllDescription;
        private static LinearLayout llViewMoreDescription;
        private static LinearLayout llViewMorePermissions;
        private final View.OnClickListener expanderPermissions = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View permissionListView = llViewMorePermissions.findViewById(R.id.permission_list);
                final TextView permissionHeader = (TextView) llViewMorePermissions.findViewById(R.id.permissions);

                if (permissionListView.getVisibility() == View.GONE) {
                    permissionListView.setVisibility(View.VISIBLE);
                    permissionHeader.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_lock_24dp_grey600), null, ContextCompat.getDrawable(getActivity(), R.drawable.ic_expand_less_grey600), null);
                } else {
                    permissionListView.setVisibility(View.GONE);
                    permissionHeader.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_lock_24dp_grey600), null, ContextCompat.getDrawable(getActivity(), R.drawable.ic_expand_more_grey600), null);
                }
            }
        };
        private ViewGroup layoutLinks;

        public AppDetailsSummaryFragment() {
            prefs = Preferences.get();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            appDetails = (AppDetails) activity;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            super.onCreateView(inflater, container, savedInstanceState);
            View summaryView = inflater.inflate(R.layout.app_details_summary, container, false);
            setupView(summaryView);
            return summaryView;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateViews(getView());
        }

        // The HTML formatter adds "\n\n" at the end of every paragraph. This
        // is desired between paragraphs, but not at the end of the whole
        // string as it adds unwanted spacing at the end of the TextView.
        // Remove all trailing newlines.
        // Use this function instead of a trim() as that would require
        // converting to String and thus losing formatting (e.g. bold).
        private static CharSequence trimNewlines(CharSequence s) {
            if (s == null || s.length() < 1) {
                return s;
            }
            int i;
            for (i = s.length() - 1; i >= 0; i--) {
                if (s.charAt(i) != '\n') {
                    break;
                }
            }
            if (i == s.length() - 1) {
                return s;
            }
            return s.subSequence(0, i + 1);
        }

        private ViewGroup layoutLinksContent;
        private final View.OnClickListener expanderLinks = new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                TextView linksHeader = (TextView) layoutLinks.findViewById(R.id.information);

                if (layoutLinksContent.getVisibility() == View.GONE) {
                    layoutLinksContent.setVisibility(View.VISIBLE);
                    linksHeader.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_website), null, ContextCompat.getDrawable(getActivity(), R.drawable.ic_expand_less_grey600), null);
                } else {
                    layoutLinksContent.setVisibility(View.GONE);
                    linksHeader.setCompoundDrawablesWithIntrinsicBounds(ContextCompat.getDrawable(getActivity(), R.drawable.ic_website), null, ContextCompat.getDrawable(getActivity(), R.drawable.ic_expand_more_grey600), null);
                }
            }
        };

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                String url = null;
                App app = appDetails.getApp();
                switch (v.getId()) {
                    case R.id.website:
                        url = app.webURL;
                        break;
                    case R.id.email:
                        final String subject = Uri.encode(getString(R.string.app_details_subject, app.name));
                        url = "mailto:" + app.email + "?subject=" + subject;
                        break;
                    case R.id.source:
                        url = app.sourceURL;
                        break;
                    case R.id.issues:
                        url = app.trackerURL;
                        break;
                    case R.id.changelog:
                        url = app.changelogURL;
                        break;
                    case R.id.donate:
                        url = app.donateURL;
                        break;
                    case R.id.bitcoin:
                        url = "bitcoin:" + app.bitcoinAddr;
                        break;
                    case R.id.litecoin:
                        url = "litecoin:" + app.litecoinAddr;
                        break;
                    case R.id.flattr:
                        url = "https://flattr.com/thing/" + app.flattrID;
                        break;
                }
                if (url != null) {
                    ((AppDetails) getActivity()).tryOpenUri(url);
                }
            }
        };

        private final View.OnClickListener expanderDescription = new View.OnClickListener() {
            public void onClick(View v) {
                final TextView description = (TextView) llViewMoreDescription.findViewById(R.id.description);
                final TextView viewMorePermissions = (TextView) llViewMoreDescription.findViewById(R.id.view_more_description);
                if (viewAllDescription) {
                    description.setMaxLines(Integer.MAX_VALUE);
                    viewMorePermissions.setText(getString(R.string.less));
                } else {
                    description.setMaxLines(MAX_LINES);
                    if (Build.VERSION.SDK_INT > 10) {
                        // ellipsizing doesn't work properly here on 2.X
                        description.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    }
                    viewMorePermissions.setText(R.string.more);
                }
                viewAllDescription ^= true;
            }
        };

        private void setupView(final View view) {
            App app = appDetails.getApp();
            // Expandable description
            final TextView description = (TextView) view.findViewById(R.id.description);
            final Spanned desc = Html.fromHtml(app.description, null, new Utils.HtmlTagHandler());
            description.setMovementMethod(SafeLinkMovementMethod.getInstance(getActivity()));
            description.setText(trimNewlines(desc));
            final View viewMoreDescription = view.findViewById(R.id.view_more_description);
            description.post(new Runnable() {
                @Override
                public void run() {
                    // If description has more than five lines
                    if (description.getLineCount() > MAX_LINES) {
                        description.setMaxLines(MAX_LINES);
                        if (Build.VERSION.SDK_INT > 10) {
                            // ellipsizing doesn't work properly here on 2.X
                            description.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        }
                        description.setOnClickListener(expanderDescription);
                        viewAllDescription = true;

                        llViewMoreDescription = (LinearLayout) view.findViewById(R.id.ll_description);
                        llViewMoreDescription.setOnClickListener(expanderDescription);

                        viewMoreDescription.setOnClickListener(expanderDescription);
                    } else {
                        viewMoreDescription.setVisibility(View.GONE);
                    }
                }
            });

            // App ID
            final TextView packageNameView = (TextView) view.findViewById(R.id.package_name);
            if (prefs.expertMode()) {
                packageNameView.setText(app.packageName);
            } else {
                packageNameView.setVisibility(View.GONE);
            }

            // Summary
            final TextView summaryView = (TextView) view.findViewById(R.id.summary);
            summaryView.setText(app.summary);

            layoutLinks = (ViewGroup) view.findViewById(R.id.ll_information);
            layoutLinksContent = (ViewGroup) layoutLinks.findViewById(R.id.ll_information_content);

            final TextView linksHeader = (TextView) view.findViewById(R.id.information);
            linksHeader.setOnClickListener(expanderLinks);

            // Website button
            View tv = view.findViewById(R.id.website);
            if (!TextUtils.isEmpty(app.webURL)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Email button
            tv = view.findViewById(R.id.email);
            if (!TextUtils.isEmpty(app.email)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Source button
            tv = view.findViewById(R.id.source);
            if (!TextUtils.isEmpty(app.sourceURL)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Issues button
            tv = view.findViewById(R.id.issues);
            if (!TextUtils.isEmpty(app.trackerURL)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Changelog button
            tv = view.findViewById(R.id.changelog);
            if (!TextUtils.isEmpty(app.changelogURL)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Donate button
            tv = view.findViewById(R.id.donate);
            if (!TextUtils.isEmpty(app.donateURL)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Bitcoin
            tv = view.findViewById(R.id.bitcoin);
            if (!TextUtils.isEmpty(app.bitcoinAddr)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Litecoin
            tv = view.findViewById(R.id.litecoin);
            if (!TextUtils.isEmpty(app.litecoinAddr)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Flattr
            tv = view.findViewById(R.id.flattr);
            if (!TextUtils.isEmpty(app.flattrID)) {
                tv.setOnClickListener(mOnClickListener);
            } else {
                tv.setVisibility(View.GONE);
            }

            // Categories TextView
            final TextView categories = (TextView) view.findViewById(R.id.categories);
            if (prefs.expertMode() && app.categories != null) {
                categories.setText(TextUtils.join(", ", app.categories));
            } else {
                categories.setVisibility(View.GONE);
            }

            Apk curApk = null;
            for (int i = 0; i < appDetails.getApks().getCount(); i++) {
                final Apk apk = appDetails.getApks().getItem(i);
                if (apk.versionCode == app.suggestedVersionCode) {
                    curApk = apk;
                    break;
                }
            }

            // Expandable permissions
            llViewMorePermissions = (LinearLayout) view.findViewById(R.id.ll_permissions);
            final TextView permissionHeader = (TextView) view.findViewById(R.id.permissions);

            final boolean curApkCompatible = curApk != null && curApk.compatible;
            if (!appDetails.getApks().isEmpty() && (curApkCompatible || prefs.showIncompatibleVersions())) {
                // build and set the string once
                buildPermissionInfo();
                permissionHeader.setOnClickListener(expanderPermissions);

            } else {
                permissionHeader.setVisibility(View.GONE);
            }

            // Anti features
            final TextView antiFeaturesView = (TextView) view.findViewById(R.id.antifeatures);
            if (app.antiFeatures != null) {
                StringBuilder sb = new StringBuilder();
                for (String af : app.antiFeatures) {
                    String afdesc = descAntiFeature(af);
                    sb.append("\t• ").append(afdesc).append('\n');
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                    antiFeaturesView.setText(sb.toString());
                } else {
                    antiFeaturesView.setVisibility(View.GONE);
                }
            } else {
                antiFeaturesView.setVisibility(View.GONE);
            }

            updateViews(view);
        }

        private void buildPermissionInfo() {
            AppDiff appDiff = new AppDiff(appDetails.getPackageManager(), appDetails.getApks().getItem(0));
            AppSecurityPermissions perms = new AppSecurityPermissions(appDetails, appDiff.pkgInfo);

            final ViewGroup permList = (ViewGroup) llViewMorePermissions.findViewById(R.id.permission_list);
            permList.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
        }

        private String descAntiFeature(String af) {
            switch (af) {
                case "Ads":
                    return getString(R.string.antiadslist);
                case "Tracking":
                    return getString(R.string.antitracklist);
                case "NonFreeNet":
                    return getString(R.string.antinonfreenetlist);
                case "NonFreeAdd":
                    return getString(R.string.antinonfreeadlist);
                case "NonFreeDep":
                    return getString(R.string.antinonfreedeplist);
                case "UpstreamNonFree":
                    return getString(R.string.antiupstreamnonfreelist);
                case "NonFreeAssets":
                    return getString(R.string.antinonfreeassetslist);
                default:
                    return af;
            }
        }

        public void updateViews(View view) {
            if (view == null) {
                Log.e(TAG, "AppDetailsSummaryFragment.updateViews(): view == null. Oops.");
                return;
            }

            App app = appDetails.getApp();
            TextView signatureView = (TextView) view.findViewById(R.id.signature);
            if (prefs.expertMode() && !TextUtils.isEmpty(app.installedSig)) {
                signatureView.setVisibility(View.VISIBLE);
                signatureView.setText("Signed: " + app.installedSig);
            } else {
                signatureView.setVisibility(View.GONE);
            }
        }
    }

    public static class AppDetailsHeaderFragment extends Fragment implements View.OnClickListener {

        private AppDetails appDetails;
        private Button btMain;
        private ProgressBar progressBar;
        private TextView progressSize;
        private TextView progressPercent;
        private ImageButton cancelButton;
        final DisplayImageOptions displayImageOptions;
        public static boolean installed;
        public static boolean updateWanted;

        public AppDetailsHeaderFragment() {
            displayImageOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .imageScaleType(ImageScaleType.NONE)
                .showImageOnLoading(R.drawable.ic_repo_app_default)
                .showImageForEmptyUri(R.drawable.ic_repo_app_default)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.app_details_header, container, false);
            setupView(view);
            return view;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            appDetails = (AppDetails) activity;
        }

        private void setupView(View view) {
            App app = appDetails.getApp();

            // Set the icon...
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            ImageLoader.getInstance().displayImage(app.iconUrlLarge, iv,
                    displayImageOptions);

            // Set the title
            TextView tv = (TextView) view.findViewById(R.id.title);
            tv.setText(app.name);

            btMain   = (Button) view.findViewById(R.id.btn_main);
            progressBar     = (ProgressBar) view.findViewById(R.id.progress_bar);
            progressSize    = (TextView) view.findViewById(R.id.progress_size);
            progressPercent = (TextView) view.findViewById(R.id.progress_percentage);
            cancelButton    = (ImageButton) view.findViewById(R.id.cancel);
            progressBar.setIndeterminate(false);
            cancelButton.setOnClickListener(this);

            updateViews(view);
        }

        @Override
        public void onResume() {
            super.onResume();
            updateViews();
            restoreProgressBarOnResume();
        }

        /**
         * After resuming the fragment, decide whether or not we need to show the progress bar.
         * Also, put an appropriate message depending on whether or not the download is active or
         * just queued.
         *
         * NOTE: this can't be done in the `updateViews` method as it currently stands. The reason
         * is because that method gets called all the time, for all sorts of reasons. The progress
         * bar is updated with actual progress values in response to async broadcasts. If we always
         * tried to force the progress bar in `updateViews`, it would override the values that were
         * set by the async progress broadcasts.
         */
        private void restoreProgressBarOnResume() {
            if (appDetails.activeDownloadUrlString != null) {
                // We don't actually know what the current progress is, so this will show an indeterminate
                // progress bar until the first progress/complete event we receive.
                if (DownloaderService.isQueuedOrActive(appDetails.activeDownloadUrlString)) {
                    showIndeterminateProgress(getString(R.string.download_pending));
                } else {
                    showIndeterminateProgress("");
                }
            }
        }

        /**
         * Displays empty, indeterminate progress bar and related views.
         */
        public void startProgress() {
            startProgress(true);
        }

        public void startProgress(boolean allowCancel) {
            cancelButton.setVisibility(allowCancel ? View.VISIBLE : View.GONE);
            showIndeterminateProgress(getString(R.string.download_pending));
            updateViews();
        }

        private void showIndeterminateProgress(String message) {
            setProgressVisible(true);
            progressBar.setIndeterminate(true);
            progressSize.setText(message);
            progressPercent.setText("");
        }

        /**
         * Updates progress bar and captions to new values (in bytes).
         */
        public void updateProgress(long bytesDownloaded, long totalBytes) {
            if (bytesDownloaded < 0 || totalBytes == 0) {
                // Avoid division by zero and other weird values
                return;
            }

            if (totalBytes == -1) {
                setProgressVisible(true);
                progressBar.setIndeterminate(true);
                progressSize.setText(Utils.getFriendlySize(bytesDownloaded));
                progressPercent.setText("");
            } else {
                long percent = bytesDownloaded * 100 / totalBytes;
                setProgressVisible(true);
                progressBar.setIndeterminate(false);
                progressBar.setProgress((int) percent);
                progressBar.setMax(100);
                progressSize.setText(Utils.getFriendlySize(bytesDownloaded) + " / " + Utils.getFriendlySize(totalBytes));
                progressPercent.setText(Long.toString(percent) + " %");
            }
        }

        /**
         * Shows or hides progress bar and related views.
         */
        private void setProgressVisible(boolean visible) {
            int state = visible ? View.VISIBLE : View.GONE;
            progressBar.setVisibility(state);
            progressSize.setVisibility(state);
            progressPercent.setVisibility(state);
        }

        /**
         * Removes progress bar and related views, invokes {@link #updateViews()}.
         */
        public void removeProgress() {
            setProgressVisible(false);
            cancelButton.setVisibility(View.GONE);
            updateViews();
        }

        /**
         * Cancels download and hides progress bar.
         */
        @Override
        public void onClick(View view) {
            AppDetails appDetails = (AppDetails) getActivity();
            if (appDetails == null || appDetails.activeDownloadUrlString == null) {
                return;
            }

            DownloaderService.cancel(getContext(), appDetails.activeDownloadUrlString);
        }

        public void updateViews() {
            updateViews(getView());
        }

        public void updateViews(View view) {
            if (view == null) {
                Log.e(TAG, "AppDetailsHeaderFragment.updateViews(): view == null. Oops.");
                return;
            }
            App app = appDetails.getApp();
            TextView statusView = (TextView) view.findViewById(R.id.status);
            btMain.setVisibility(View.VISIBLE);

            if (appDetails.activeDownloadUrlString != null) {
                btMain.setText(R.string.downloading);
                btMain.setEnabled(false);
            } else if (!app.isInstalled() && app.suggestedVersionCode > 0 &&
                    appDetails.adapter.getCount() > 0) {
                // Check count > 0 due to incompatible apps resulting in an empty list.
                // If App isn't installed
                installed = false;
                statusView.setText(R.string.details_notinstalled);
                NfcHelper.disableAndroidBeam(appDetails);
                // Set Install button and hide second button
                btMain.setText(R.string.menu_install);
                btMain.setOnClickListener(mOnClickListener);
                btMain.setEnabled(true);
            } else if (app.isInstalled()) {
                // If App is installed
                installed = true;
                statusView.setText(getString(R.string.details_installed, app.installedVersionName));
                NfcHelper.setAndroidBeam(appDetails, app.packageName);
                if (app.canAndWantToUpdate(appDetails)) {
                    updateWanted = true;
                    btMain.setText(R.string.menu_upgrade);
                } else {
                    updateWanted = false;
                    if (appDetails.packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                        btMain.setText(R.string.menu_launch);
                    } else {
                        btMain.setText(R.string.menu_uninstall);
                    }
                }
                btMain.setOnClickListener(mOnClickListener);
                btMain.setEnabled(true);
            }
            TextView author = (TextView) view.findViewById(R.id.author);
            if (!TextUtils.isEmpty(app.author)) {
                author.setText(getString(R.string.by_author) + " " + app.author);
                author.setVisibility(View.VISIBLE);
            }
            TextView currentVersion = (TextView) view.findViewById(R.id.current_version);
            if (!appDetails.getApks().isEmpty()) {
                currentVersion.setText(appDetails.getApks().getItem(0).versionName + " (" + app.license + ")");
            } else {
                currentVersion.setVisibility(View.GONE);
                btMain.setVisibility(View.GONE);
            }

        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                App app = appDetails.getApp();
                AppDetails activity = (AppDetails) getActivity();
                if (updateWanted && app.suggestedVersionCode > 0) {
                    Apk apkToInstall = ApkProvider.Helper.find(activity, app.packageName, app.suggestedVersionCode);
                    activity.install(apkToInstall);
                    return;
                }
                if (installed) {
                    // If installed
                    if (activity.packageManager.getLaunchIntentForPackage(app.packageName) != null) {
                        // If "launchable", launch
                        activity.launchApk(app.packageName);
                    } else {
                        activity.uninstallApk(app.packageName);
                    }
                } else if (app.suggestedVersionCode > 0) {
                    // If not installed, install
                    btMain.setEnabled(false);
                    btMain.setText(R.string.system_install_installing);
                    final Apk apkToInstall = ApkProvider.Helper.find(activity, app.packageName, app.suggestedVersionCode);
                    activity.install(apkToInstall);
                }
            }
        };
    }

    public static class AppDetailsListFragment extends ListFragment {

        private static final String SUMMARY_TAG = "summary";

        private AppDetails appDetails;
        private AppDetailsSummaryFragment summaryFragment;

        private FrameLayout headerView;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            appDetails = (AppDetails) activity;
        }

        void remove() {
            appDetails.uninstallApk(appDetails.getApp().packageName);
        }

        @Override
        public void onViewCreated(View view, Bundle savedInstanceState) {
            // A bit of a hack, but we can't add the header view in setupSummaryHeader(),
            // due to the fact it needs to happen before setListAdapter(). Also, seeing
            // as we may never add a summary header (i.e. in landscape), this is probably
            // the last opportunity to set the list adapter. As such, we use the headerView
            // as a mechanism to optionally allow adding a header in the future.
            if (headerView == null) {
                headerView = new FrameLayout(getActivity());
                headerView.setId(R.id.appDetailsSummaryHeader);
            } else {
                Fragment summaryFragment = getChildFragmentManager().findFragmentByTag(SUMMARY_TAG);
                if (summaryFragment != null) {
                    getChildFragmentManager().beginTransaction().remove(summaryFragment).commit();
                }
            }

            setListAdapter(null);
            getListView().addHeaderView(headerView);
            setListAdapter(appDetails.getApks());
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            App app = appDetails.getApp();
            final Apk apk = appDetails.getApks().getItem(position - l.getHeaderViewsCount());
            if (app.installedVersionCode == apk.versionCode) {
                remove();
            } else if (app.installedVersionCode > apk.versionCode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.installDowngrade);
                builder.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                appDetails.install(apk);
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
            } else {
                appDetails.install(apk);
            }
        }

        public void removeSummaryHeader() {
            Fragment summary = getChildFragmentManager().findFragmentByTag(SUMMARY_TAG);
            if (summary != null) {
                getChildFragmentManager().beginTransaction().remove(summary).commit();
                headerView.removeAllViews();
                headerView.setVisibility(View.GONE);
                summaryFragment = null;
            }
        }

        public void setupSummaryHeader() {
            Fragment fragment = getChildFragmentManager().findFragmentByTag(SUMMARY_TAG);
            if (fragment != null) {
                summaryFragment = (AppDetailsSummaryFragment) fragment;
            } else {
                summaryFragment = new AppDetailsSummaryFragment();
            }
            getChildFragmentManager().beginTransaction().replace(headerView.getId(), summaryFragment, SUMMARY_TAG).commit();
            headerView.setVisibility(View.VISIBLE);
        }
    }

}
