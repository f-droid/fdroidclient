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
import android.bluetooth.BluetoothAdapter;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import org.fdroid.fdroid.Utils.CommaSeparatedList;
import org.fdroid.fdroid.compat.PackageManagerCompat;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Credentials;
import org.fdroid.fdroid.data.InstalledAppProvider;
import org.fdroid.fdroid.data.Repo;
import org.fdroid.fdroid.data.RepoProvider;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.Installer.AndroidNotCompatibleException;
import org.fdroid.fdroid.installer.Installer.InstallerCallback;
import org.fdroid.fdroid.net.ApkDownloader;
import org.fdroid.fdroid.net.AsyncDownloaderFromAndroid;
import org.fdroid.fdroid.net.Downloader;

import java.io.File;
import java.util.Iterator;
import java.util.List;

interface AppDetailsData {
    App getApp();

    AppDetails.ApkListAdapter getApks();
}

/**
 * Interface which allows the apk list fragment to communicate with the activity when
 * a user requests to install/remove an apk by clicking on an item in the list.
 *
 * NOTE: This is <em>not</em> to do with with the sudo/packagemanager/other installer
 * stuff which allows multiple ways to install apps. It is only here to make fragment-
 * activity communication possible.
 */
interface AppInstallListener {
    void install(final Apk apk);

    void removeApk(String packageName);
}

public class AppDetails extends AppCompatActivity implements ProgressListener, AppDetailsData, AppInstallListener {

    private static final String TAG = "AppDetails";

    private static final int REQUEST_ENABLE_BLUETOOTH = 2;

    public static final String EXTRA_APPID = "appid";
    public static final String EXTRA_FROM = "from";
    public static final String EXTRA_HINT_SEARCHING = "searching";

    private FDroidApp fdroidApp;
    private ApkListAdapter adapter;

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

        private final LayoutInflater mInflater = (LayoutInflater) mctx.getSystemService(
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
            if (apk.vercode != app.installedVersionCode) {
                return getString(R.string.app_not_installed);
            }
            // Definitely installed this version.
            if (apk.sig != null && apk.sig.equals(app.installedSig)) {
                return getString(R.string.app_installed);
            }
            // Installed the same version, but from someplace else.
            final String installerPkgName;
            try {
                installerPkgName = mPm.getInstallerPackageName(app.packageName);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Application " + app.packageName + " is not installed anymore");
                return getString(R.string.app_not_installed);
            }
            if (TextUtils.isEmpty(installerPkgName)) {
                return getString(R.string.app_inst_unknown_source);
            }
            final String installerLabel = InstalledAppProvider
                    .getApplicationLabel(mctx, installerPkgName);
            return getString(R.string.app_inst_known_source, installerLabel);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            java.text.DateFormat df = DateFormat.getDateFormat(mctx);
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
                    + " " + apk.version
                    + (apk.vercode == app.suggestedVercode ? "  ☆" : ""));

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
            } else if (apk.minSdkVersion > 0 && apk.maxSdkVersion > 0) {
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
                holder.nativecode.setText(apk.nativecode.toString().replaceAll(",", " "));
                holder.nativecode.setVisibility(View.VISIBLE);
            } else {
                holder.nativecode.setVisibility(View.GONE);
            }

            if (apk.incompatibleReasons != null) {
                holder.incompatibleReasons.setText(
                        getResources().getString(
                            R.string.requires_features,
                            apk.incompatibleReasons.toPrettyString()));
                holder.incompatibleReasons.setVisibility(View.VISIBLE);
            } else {
                holder.incompatibleReasons.setVisibility(View.GONE);
            }

            // Disable it all if it isn't compatible...
            final View[] views = {
                convertView,
                holder.version,
                holder.status,
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
    private PackageManager mPm;
    private ApkDownloader downloadHandler;
    private LocalBroadcastManager localBroadcastManager;

    private boolean startingIgnoreAll;
    private int startingIgnoreThis;

    private final Context mctx = this;
    private Installer installer;

    private AppDetailsHeaderFragment mHeaderFragment;

    /**
     * Stores relevant data that we want to keep track of when destroying the activity
     * with the expectation of it being recreated straight away (e.g. after an
     * orientation change). One of the major things is that we want the download thread
     * to stay active, but for it not to trigger any UI stuff (e.g. progress bar)
     * between the activity being destroyed and recreated.
     */
    private static class ConfigurationChangeHelper {

        public final ApkDownloader downloader;
        public final App app;

        ConfigurationChangeHelper(ApkDownloader downloader, App app) {
            this.downloader = downloader;
            this.app = app;
        }
    }

    private boolean inProcessOfChangingConfiguration;

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

        mPm = getPackageManager();

        installer = Installer.getActivityInstaller(this, mPm, myInstallerCallback);

        // Get the preferences we're going to use in this Activity...
        ConfigurationChangeHelper previousData = (ConfigurationChangeHelper) getLastCustomNonConfigurationInstance();
        if (previousData != null) {
            Utils.debugLog(TAG, "Recreating view after configuration change.");
            downloadHandler = previousData.downloader;
            if (downloadHandler != null) {
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

        // Check if a download is running for this app
        if (AsyncDownloaderFromAndroid.isDownloading(this, app.packageName) >= 0) {
            // call install() to re-setup the listeners and downloaders
            // the AsyncDownloader will not restart the download since the download is running,
            // and thus the version we pass to install() is not important
            refreshHeader();
            refreshApkList();
            final Apk apkToInstall = ApkProvider.Helper.find(this, app.packageName, app.suggestedVercode);
            install(apkToInstall);
        }

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
        super.onResumeFragments();
        refreshApkList();
        refreshHeader();
        supportInvalidateOptionsMenu();

        if (downloadHandler != null) {
            if (downloadHandler.isComplete()) {
                downloadCompleteInstallApk();
            } else {
                localBroadcastManager.registerReceiver(downloaderProgressReceiver,
                        new IntentFilter(Downloader.LOCAL_ACTION_PROGRESS));
                downloadHandler.setProgressListener(this);

                if (downloadHandler.getTotalBytes() == 0) {
                    mHeaderFragment.startProgress();
                } else {
                    mHeaderFragment.updateProgress(downloadHandler.getBytesRead(), downloadHandler.getTotalBytes());
                }
            }
        }
    }

    /**
     * Remove progress listener, suppress progress bar, set downloadHandler to null.
     */
    private void cleanUpFinishedDownload() {
        if (downloadHandler != null) {
            downloadHandler.removeProgressListener();
            mHeaderFragment.removeProgress();
            downloadHandler = null;
        }
    }

    /**
     * Once the download completes successfully, call this method to start the install process
     * with the file that was downloaded.
     */
    private void downloadCompleteInstallApk() {
        if (downloadHandler != null) {
            installApk(downloadHandler.localFile());
            cleanUpFinishedDownload();
        }
    }

    protected void onStop() {
        super.onStop();
        getContentResolver().unregisterContentObserver(myAppObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (app != null && (app.ignoreAllUpdates != startingIgnoreAll
                || app.ignoreThisUpdate != startingIgnoreThis)) {
            Utils.debugLog(TAG, "Updating 'ignore updates', as it has changed since we started the activity...");
            setIgnoreUpdates(app.packageName, app.ignoreAllUpdates, app.ignoreThisUpdate);
        }

        localBroadcastManager.unregisterReceiver(downloaderProgressReceiver);
        if (downloadHandler != null) {
            downloadHandler.removeProgressListener();
        }

        mHeaderFragment.removeProgress();
    }

    private final BroadcastReceiver downloaderProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mHeaderFragment != null) {
                mHeaderFragment.updateProgress(intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1),
                        intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1));
            }
        }
    };

    private void onAppChanged() {
        if (!reset(app.packageName)) {
            AppDetails.this.finish();
            return;
        }

        refreshApkList();
        refreshHeader();
        supportInvalidateOptionsMenu();
    }

    private void setIgnoreUpdates(String packageName, boolean ignoreAll, int ignoreVersionCode) {

        Uri uri = AppProvider.getContentUri(packageName);

        ContentValues values = new ContentValues(2);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, ignoreAll ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, ignoreVersionCode);

        getContentResolver().update(uri, values, null, null);

    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        inProcessOfChangingConfiguration = true;
        return new ConfigurationChangeHelper(downloadHandler, app);
    }

    @Override
    protected void onDestroy() {
        if (downloadHandler != null && !inProcessOfChangingConfiguration) {
            downloadHandler.cancel(false);
            cleanUpFinishedDownload();
        }
        inProcessOfChangingConfiguration = false;
        super.onDestroy();
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset(String packageName) {

        Utils.debugLog(TAG, "Getting application details for " + packageName);
        App newApp = null;

        if (!TextUtils.isEmpty(packageName)) {
            newApp = AppProvider.Helper.findByPackageName(getContentResolver(), packageName);
        }

        setApp(newApp);

        return this.app != null;
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

        startingIgnoreAll = app.ignoreAllUpdates;
        startingIgnoreThis = app.ignoreThisUpdate;
    }

    private void refreshApkList() {
        adapter.notifyDataSetChanged();
    }

    private void refreshHeader() {
        mHeaderFragment = (AppDetailsHeaderFragment)
                getSupportFragmentManager().findFragmentById(R.id.header);
        if (mHeaderFragment != null) {
            mHeaderFragment.updateViews();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (app == null) {
            return true;
        }

        if (mPm.getLaunchIntentForPackage(app.packageName) != null && app.canAndWantToUpdate()) {
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
                    .setChecked(app.ignoreAllUpdates);

        if (app.hasUpdates()) {
            menu.add(Menu.NONE, IGNORETHIS, 2, R.string.menu_ignore_this)
                    .setIcon(R.drawable.ic_do_not_disturb_white)
                    .setCheckable(true)
                    .setChecked(app.ignoreThisUpdate >= app.suggestedVercode);
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
        if (intent.resolveActivity(mPm) == null) {
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
                if (app.suggestedVercode > 0) {
                    final Apk apkToInstall = ApkProvider.Helper.find(this, app.packageName, app.suggestedVercode);
                    install(apkToInstall);
                }
                return true;

            case UNINSTALL:
                removeApk(app.packageName);
                return true;

            case IGNOREALL:
                app.ignoreAllUpdates ^= true;
                item.setChecked(app.ignoreAllUpdates);
                return true;

            case IGNORETHIS:
                if (app.ignoreThisUpdate >= app.suggestedVercode) {
                    app.ignoreThisUpdate = 0;
                } else {
                    app.ignoreThisUpdate = app.suggestedVercode;
                }
                item.setChecked(app.ignoreThisUpdate > 0);
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
    @Override
    public void install(final Apk apk) {
        if (isFinishing()) {
            return;
        }

        // Ignore call if another download is running.
        if (downloadHandler != null && !downloadHandler.isComplete()) {
            return;
        }

        final String repoaddress = getRepoAddress(apk);
        if (repoaddress == null) return;

        if (!apk.compatible) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.installIncompatible);
            builder.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            startDownload(apk, repoaddress);
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
        startDownload(apk, repoaddress);
    }

    @Nullable
    private String getRepoAddress(Apk apk) {
        final String[] projection = {RepoProvider.DataColumns.ADDRESS};
        Repo repo = RepoProvider.Helper.findById(this, apk.repo, projection);
        if (repo == null || repo.address == null) {
            return null;
        }
        return repo.address;
    }

    @Nullable
    private Credentials getRepoCredentials(Apk apk) {
        final String[] projection = {RepoProvider.DataColumns.USERNAME, RepoProvider.DataColumns.PASSWORD};
        Repo repo = RepoProvider.Helper.findById(this, apk.repo, projection);
        if (repo == null || repo.username == null || repo.password == null) {
            return null;
        }
        return repo.getCredentials();
    }

    private void startDownload(Apk apk, String repoAddress) {
        downloadHandler = new ApkDownloader(getBaseContext(), app, apk, repoAddress);
        downloadHandler.setCredentials(getRepoCredentials(apk));

        localBroadcastManager.registerReceiver(downloaderProgressReceiver,
                new IntentFilter(Downloader.LOCAL_ACTION_PROGRESS));
        downloadHandler.setProgressListener(this);
        if (downloadHandler.download()) {
            mHeaderFragment.startProgress();
        }
    }

    private void installApk(File file) {
        try {
            installer.installPackage(file, app.packageName);
        } catch (AndroidNotCompatibleException e) {
            Log.e(TAG, "Android not compatible with this Installer!", e);
        }
    }

    @Override
    public void removeApk(String packageName) {
        try {
            installer.deletePackage(packageName);
        } catch (AndroidNotCompatibleException e) {
            Log.e(TAG, "Android not compatible with this Installer!", e);
        }
    }

    private final Installer.InstallerCallback myInstallerCallback = new Installer.InstallerCallback() {

        @Override
        public void onSuccess(final int operation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (operation == Installer.InstallerCallback.OPERATION_INSTALL) {
                        PackageManagerCompat.setInstaller(mPm, app.packageName);
                    }

                    onAppChanged();
                }
            });
        }

        @Override
        public void onError(int operation, final int errorCode) {
            if (errorCode == InstallerCallback.ERROR_CODE_CANCELED) {
                return;
            }
            final int title, body;
            if (operation == InstallerCallback.OPERATION_INSTALL) {
                title = R.string.install_error_title;
                switch (errorCode) {
                    case ERROR_CODE_CANNOT_PARSE:
                        body = R.string.install_error_cannot_parse;
                        break;
                    default: // ERROR_CODE_OTHER
                        body = R.string.install_error_unknown;
                        break;
                }
            } else { // InstallerCallback.OPERATION_DELETE
                title = R.string.uninstall_error_title;
                switch (errorCode) {
                    default: // ERROR_CODE_OTHER
                        body = R.string.uninstall_error_unknown;
                        break;
                }
            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    onAppChanged();

                    Log.e(TAG, "Installer aborted with errorCode: " + errorCode);

                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(AppDetails.this);
                    alertBuilder.setTitle(title);
                    alertBuilder.setMessage(body);
                    alertBuilder.setNeutralButton(android.R.string.ok, null);
                    alertBuilder.create().show();
                }
            });
        }
    };

    private void launchApk(String packageName) {
        Intent intent = mPm.getLaunchIntentForPackage(packageName);
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
    public void onProgress(Event event) {
        if (downloadHandler == null || !downloadHandler.isEventFromThis(event)) {
            // Choose not to respond to events from previous downloaders.
            // We don't even care if we receive "cancelled" events or the like, because
            // we dealt with cancellations in the onCancel listener of the dialog,
            // rather than waiting to receive the event here. We try and be careful in
            // the download thread to make sure that we check for cancellations before
            // sending events, but it is not possible to be perfect, because the interruption
            // which triggers the download can happen after the check to see if
            Utils.debugLog(TAG, "Discarding downloader event \"" + event.type + "\" as it is from an old (probably cancelled) downloader.");
            return;
        }

        boolean finished = false;
        switch (event.type) {
            case ApkDownloader.EVENT_ERROR:
                final int res;
                if (event.getData().getInt(ApkDownloader.EVENT_DATA_ERROR_TYPE) == ApkDownloader.ERROR_HASH_MISMATCH) {
                    res = R.string.corrupt_download;
                } else {
                    res = R.string.details_notinstalled;
                }
                // this must be on the main UI thread
                Toast.makeText(this, res, Toast.LENGTH_LONG).show();
                cleanUpFinishedDownload();
                finished = true;
                break;
            case ApkDownloader.EVENT_APK_DOWNLOAD_COMPLETE:
                downloadCompleteInstallApk();
                finished = true;
                break;
        }

        if (finished) {
            if (mHeaderFragment != null) {
                mHeaderFragment.removeProgress();
            }
            downloadHandler = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // handle cases for install manager first
        if (installer.handleOnActivityResult(requestCode, resultCode, data)) {
            return;
        }

        switch (requestCode) {
            case REQUEST_ENABLE_BLUETOOTH:
                fdroidApp.sendViaBluetooth(this, resultCode, app.packageName);
                break;
        }
    }

    @Override
    public App getApp() {
        return app;
    }

    @Override
    public ApkListAdapter getApks() {
        return adapter;
    }

    public static class AppDetailsSummaryFragment extends Fragment {

        final Preferences prefs;
        private AppDetailsData data;
        private static final int MAX_LINES = 5;
        private static boolean viewAllDescription;
        private static LinearLayout llViewMoreDescription;
        private static LinearLayout llViewMorePermissions;
        private final View.OnClickListener expanderPermissions = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final TextView permissionListView = (TextView) llViewMorePermissions.findViewById(R.id.permissions_list);
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
            data = (AppDetailsData) activity;
        }

        App getApp() {
            return data.getApp();
        }

        ApkListAdapter getApks() {
            return data.getApks();
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
                App app = getApp();
                switch (v.getId()) {
                    case R.id.website:
                        url = app.webURL;
                        break;
                    case R.id.email:
                        url = "mailto:" + app.email;
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
                    description.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    viewMorePermissions.setText(R.string.more);
                }
                viewAllDescription ^= true;
            }
        };

        private void setupView(final View view) {
            App app = getApp();
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
                        description.setEllipsize(TextUtils.TruncateAt.MARQUEE);
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
                categories.setText(app.categories.toString().replaceAll(",", ", "));
            } else {
                categories.setVisibility(View.GONE);
            }

            Apk curApk = null;
            for (int i = 0; i < getApks().getCount(); i++) {
                final Apk apk = getApks().getItem(i);
                if (apk.vercode == app.suggestedVercode) {
                    curApk = apk;
                    break;
                }
            }

            // Expandable permissions
            llViewMorePermissions = (LinearLayout) view.findViewById(R.id.ll_permissions);
            final TextView permissionHeader = (TextView) view.findViewById(R.id.permissions);

            final boolean curApkCompatible = curApk != null && curApk.compatible;
            if (!getApks().isEmpty() && (curApkCompatible || prefs.showIncompatibleVersions())) {
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
            final TextView permissionListView = (TextView) llViewMorePermissions.findViewById(R.id.permissions_list);

            CommaSeparatedList permsList = getApks().getItem(0).permissions;
            if (permsList == null) {
                permissionListView.setText(R.string.no_permissions);
            } else {
                Iterator<String> permissions = permsList.iterator();
                StringBuilder sb = new StringBuilder();
                while (permissions.hasNext()) {
                    final String permissionName = permissions.next();
                    try {
                        final Permission permission = new Permission(getActivity(), permissionName);
                        // TODO: Make this list RTL friendly
                        sb.append("\t• ").append(permission.getName()).append('\n');
                    } catch (PackageManager.NameNotFoundException e) {
                        Log.e(TAG, "Permission not yet available: " + permissionName);
                    }
                }
                if (sb.length() > 0) {
                    sb.setLength(sb.length() - 1);
                    permissionListView.setText(sb.toString());
                } else {
                    permissionListView.setText(R.string.no_permissions);
                }
            }
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

            App app = getApp();
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

        private AppDetailsData data;
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

        private App getApp() {
            return data.getApp();
        }

        private ApkListAdapter getApks() {
            return data.getApks();
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
            data = (AppDetailsData) activity;
        }

        private void setupView(View view) {
            App app = getApp();

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
        }

        /**
         * Displays empty, indeterminate progress bar and related views.
         */
        public void startProgress() {
            setProgressVisible(true);
            progressBar.setIndeterminate(true);
            progressSize.setText("");
            progressPercent.setText("");
            updateViews();
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
            cancelButton.setVisibility(state);
        }

        /**
         * Removes progress bar and related views, invokes {@link #updateViews()}.
         */
        public void removeProgress() {
            setProgressVisible(false);
            updateViews();
        }

        /**
         * Cancels download and hides progress bar.
         */
        @Override
        public void onClick(View view) {
            AppDetails activity = (AppDetails) getActivity();
            if (activity == null || activity.downloadHandler == null) {
                return;
            }

            activity.downloadHandler.cancel(true);
            activity.cleanUpFinishedDownload();
            setProgressVisible(false);
            updateViews();
        }

        public void updateViews() {
            updateViews(getView());
        }

        public void updateViews(View view) {
            App app = getApp();
            TextView statusView = (TextView) view.findViewById(R.id.status);
            btMain.setVisibility(View.VISIBLE);

            AppDetails activity = (AppDetails) getActivity();
            if (activity.downloadHandler != null) {
                btMain.setText(R.string.downloading);
                btMain.setEnabled(false);
            } else if (!app.isInstalled() && app.suggestedVercode > 0 &&
                    activity.adapter.getCount() > 0) {
                // Check count > 0 due to incompatible apps resulting in an empty list.
                // If App isn't installed
                installed = false;
                statusView.setText(R.string.details_notinstalled);
                NfcHelper.disableAndroidBeam(activity);
                // Set Install button and hide second button
                btMain.setText(R.string.menu_install);
                btMain.setOnClickListener(mOnClickListener);
                btMain.setEnabled(true);
            } else if (app.isInstalled()) {
                // If App is installed
                installed = true;
                statusView.setText(getString(R.string.details_installed, app.installedVersionName));
                NfcHelper.setAndroidBeam(activity, app.packageName);
                if (app.canAndWantToUpdate()) {
                    updateWanted = true;
                    btMain.setText(R.string.menu_upgrade);
                } else {
                    updateWanted = false;
                    if (activity.mPm.getLaunchIntentForPackage(app.packageName) != null) {
                        btMain.setText(R.string.menu_launch);
                    } else {
                        btMain.setText(R.string.menu_uninstall);
                        if (!app.uninstallable) {
                            btMain.setVisibility(View.GONE);
                        }
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
            if (!getApks().isEmpty()) {
                currentVersion.setText(getApks().getItem(0).version + " (" + app.license + ")");
            } else {
                currentVersion.setVisibility(View.GONE);
                btMain.setVisibility(View.GONE);
            }

        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                App app = getApp();
                AppDetails activity = (AppDetails) getActivity();
                if (updateWanted && app.suggestedVercode > 0) {
                    Apk apkToInstall = ApkProvider.Helper.find(activity, app.packageName, app.suggestedVercode);
                    activity.install(apkToInstall);
                    return;
                }
                if (installed) {
                    // If installed
                    if (activity.mPm.getLaunchIntentForPackage(app.packageName) != null) {
                        // If "launchable", launch
                        activity.launchApk(app.packageName);
                    } else {
                        activity.removeApk(app.packageName);
                    }
                } else if (app.suggestedVercode > 0) {
                    // If not installed, install
                    btMain.setEnabled(false);
                    btMain.setText(R.string.system_install_installing);
                    final Apk apkToInstall = ApkProvider.Helper.find(activity, app.packageName, app.suggestedVercode);
                    activity.install(apkToInstall);
                }
            }
        };
    }

    public static class AppDetailsListFragment extends ListFragment {

        private static final String SUMMARY_TAG = "summary";

        private AppDetailsData data;
        private AppInstallListener installListener;
        private AppDetailsSummaryFragment summaryFragment;

        private FrameLayout headerView;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            data = (AppDetailsData) activity;
            installListener = (AppInstallListener) activity;
        }

        void install(final Apk apk) {
            installListener.install(apk);
        }

        void remove() {
            installListener.removeApk(getApp().packageName);
        }

        App getApp() {
            return data.getApp();
        }

        ApkListAdapter getApks() {
            return data.getApks();
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
            setListAdapter(getApks());
        }

        @Override
        public void onListItemClick(ListView l, View v, int position, long id) {
            App app = getApp();
            final Apk apk = getApks().getItem(position - l.getHeaderViewsCount());
            if (app.installedVersionCode == apk.vercode) {
                remove();
            } else if (app.installedVersionCode > apk.vercode) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setMessage(R.string.installDowngrade);
                builder.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                install(apk);
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
                install(apk);
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
