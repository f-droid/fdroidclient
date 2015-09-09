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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
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
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;

interface AppDetailsData {
    App getApp();
    AppDetails.ApkListAdapter getApks();
    Signature getInstalledSignature();
    String getInstalledSignatureId();
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

    public static final int REQUEST_ENABLE_BLUETOOTH = 2;

    public static final String EXTRA_APPID = "appid";
    public static final String EXTRA_FROM = "from";

    private FDroidApp fdroidApp;
    private ApkListAdapter adapter;

    private static class ViewHolder {
        TextView version;
        TextView status;
        TextView size;
        TextView api;
        TextView incompatibleReasons;
        TextView buildtype;
        TextView added;
        TextView nativecode;
    }

    // observer to update view when package has been installed/deleted
    AppObserver myAppObserver;

    class AppObserver extends ContentObserver {

        public AppObserver(Handler handler) {
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

        public ApkListAdapter(Context context, App app) {
            super(context, 0);
            final List<Apk> apks = ApkProvider.Helper.findByApp(context, app.id);
            for (final Apk apk : apks) {
                if (apk.compatible || Preferences.get().showIncompatibleVersions()) {
                    add(apk);
                }
            }
        }

        private String getInstalledStatus(final Apk apk) {
            // Definitely not installed.
            if (apk.vercode != app.installedVersionCode) {
                return getString(R.string.not_inst);
            }
            // Definitely installed this version.
            if (mInstalledSigID != null && apk.sig != null
                    && apk.sig.equals(mInstalledSigID)) {
                return getString(R.string.inst);
            }
            // Installed the same version, but from someplace else.
            final String installerPkgName;
            try {
                installerPkgName = mPm.getInstallerPackageName(app.id);
            } catch (IllegalArgumentException e) {
                Log.w(TAG, "Application " + app.id + " is not installed anymore");
                return getString(R.string.not_inst);
            }
            if (TextUtils.isEmpty(installerPkgName)) {
                return getString(R.string.inst_unknown_source);
            }
            final String installerLabel = InstalledAppProvider
                .getApplicationLabel(mctx, installerPkgName);
            return getString(R.string.inst_known_source, installerLabel);
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
                holder.nativecode.setText(apk.nativecode.toString().replaceAll(","," "));
                holder.nativecode.setVisibility(View.VISIBLE);
            } else {
                holder.nativecode.setVisibility(View.GONE);
            }

            if (apk.incompatible_reasons != null) {
                holder.incompatibleReasons.setText(
                    getResources().getString(
                        R.string.requires_features,
                        apk.incompatible_reasons.toPrettyString()));
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
                holder.nativecode
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

        public ConfigurationChangeHelper(ApkDownloader downloader, App app) {
            this.downloader = downloader;
            this.app = app;
        }
    }

    private boolean inProcessOfChangingConfiguration = false;

    /**
     * Attempt to extract the appId from the intent which launched this activity.
     * @return May return null, if we couldn't find the appId. This should
     * never happen as AppDetails is only to be called by the FDroid activity
     * and not externally.
     */
    private String getAppIdFromIntent() {
        Intent i = getIntent();
        if (!i.hasExtra(EXTRA_APPID)) {
            Log.e(TAG, "No application ID found in the intent!");
            return null;
        }

        return i.getStringExtra(EXTRA_APPID);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        fdroidApp = ((FDroidApp) getApplication());
        fdroidApp.applyTheme(this);

        super.onCreate(savedInstanceState);

        // Must be called *after* super.onCreate(), as that is where the action bar
        // compat implementation is assigned in the ActionBarActivity base class.
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        if (getIntent().hasExtra(EXTRA_FROM)) {
            setTitle(getIntent().getStringExtra(EXTRA_FROM));
        }

        mPm = getPackageManager();

        installer = Installer.getActivityInstaller(this, mPm, myInstallerCallback);

        // Get the preferences we're going to use in this Activity...
        ConfigurationChangeHelper previousData = (ConfigurationChangeHelper)getLastCustomNonConfigurationInstance();
        if (previousData != null) {
            Utils.DebugLog(TAG, "Recreating view after configuration change.");
            downloadHandler = previousData.downloader;
            if (downloadHandler != null) {
                Utils.DebugLog(TAG, "Download was in progress before the configuration change, so we will start to listen to its events again.");
            }
            app = previousData.app;
            setApp(app);
        } else {
            if (!reset(getAppIdFromIntent())) {
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
        if (AsyncDownloaderFromAndroid.isDownloading(this, app.id) >= 0) {
            // call install() to re-setup the listeners and downloaders
            // the AsyncDownloader will not restart the download since the download is running,
            // and thus the version we pass to install() is not important
            refreshHeader();
            refreshApkList();
            final Apk apkToInstall = ApkProvider.Helper.find(this, app.id, app.suggestedVercode);
            install(apkToInstall);
        }

    }

    // The signature of the installed version.
    private Signature mInstalledSignature;
    private String mInstalledSigID;

    @Override
    protected void onStart() {
        super.onStart();
        // register observer to know when install status changes
        myAppObserver = new AppObserver(new Handler());
        getContentResolver().registerContentObserver(
                AppProvider.getContentUri(app.id),
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

                if (downloadHandler.getTotalBytes() == 0)
                    mHeaderFragment.startProgress();
                else
                    mHeaderFragment.updateProgress(downloadHandler.getBytesRead(), downloadHandler.getTotalBytes());
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
            Utils.DebugLog(TAG, "Updating 'ignore updates', as it has changed since we started the activity...");
            setIgnoreUpdates(app.id, app.ignoreAllUpdates, app.ignoreThisUpdate);
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
            if (mHeaderFragment != null)
                mHeaderFragment.updateProgress(intent.getIntExtra(Downloader.EXTRA_BYTES_READ, -1),
                    intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, -1));
        }
    };

    private void onAppChanged() {
        if (!reset(app.id)) {
            AppDetails.this.finish();
            return;
        }

        refreshApkList();
        refreshHeader();
        supportInvalidateOptionsMenu();
    }

    public void setIgnoreUpdates(String appId, boolean ignoreAll, int ignoreVersionCode) {

        Uri uri = AppProvider.getContentUri(appId);

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
        if (downloadHandler != null) {
            if (!inProcessOfChangingConfiguration) {
                downloadHandler.cancel(false);
                cleanUpFinishedDownload();
            }
        }
        inProcessOfChangingConfiguration = false;
        super.onDestroy();
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset(String appId) {

        Utils.DebugLog(TAG, "Getting application details for " + appId);
        App newApp = null;

        if (!TextUtils.isEmpty(appId)) {
            newApp = AppProvider.Helper.findById(getContentResolver(), appId);
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

        // Get the signature of the installed package...
        mInstalledSignature = null;
        mInstalledSigID = null;

        if (app.isInstalled()) {
            PackageManager pm = getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(app.id, PackageManager.GET_SIGNATURES);
                mInstalledSignature = pi.signatures[0];
                Hasher hash = new Hasher("MD5", mInstalledSignature.toCharsString().getBytes());
                mInstalledSigID = hash.getHash();
            } catch (PackageManager.NameNotFoundException e) {
                Log.w(TAG, "Failed to get installed signature");
            } catch (NoSuchAlgorithmException e) {
                Log.w(TAG, "Failed to calculate signature MD5 sum");
                mInstalledSignature = null;
            }
        }
    }

    private void refreshApkList() {
        adapter.notifyDataSetChanged();
    }

    private void refreshHeader() {
        mHeaderFragment = (AppDetailsHeaderFragment)
                getSupportFragmentManager().findFragmentById(R.id.header);
        mHeaderFragment.updateViews();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        menu.clear();
        if (app == null)
            return true;

        MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, SHARE, 1, R.string.menu_share)
                        .setIcon(R.drawable.ic_share_white),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                        MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        if (app.isInstalled()) {
            MenuItemCompat.setShowAsAction(menu.add(
                            Menu.NONE, UNINSTALL, 1, R.string.menu_uninstall)
                            .setIcon(R.drawable.ic_delete_white),
                    MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        if (mPm.getLaunchIntentForPackage(app.id) != null && app.canAndWantToUpdate()) {
            MenuItemCompat.setShowAsAction(menu.add(
                            Menu.NONE, LAUNCH, 1, R.string.menu_launch)
                            .setIcon(R.drawable.ic_play_arrow_white),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                            MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

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
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private static class SafeLinkMovementMethod extends LinkMovementMethod {

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

    protected void navigateUp() {
        NavUtils.navigateUpFromSameTask(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case android.R.id.home:
            navigateUp();
            return true;

        case LAUNCH:
            launchApk(app.id);
            return true;

        case SHARE:
            shareApp(app);
            return true;

        case INSTALL:
            // Note that this handles updating as well as installing.
            if (app.suggestedVercode > 0) {
                final Apk apkToInstall = ApkProvider.Helper.find(this, app.id, app.suggestedVercode);
                install(apkToInstall);
            }
            return true;

        case UNINSTALL:
            removeApk(app.id);
            return true;

        case IGNOREALL:
            app.ignoreAllUpdates ^= true;
            item.setChecked(app.ignoreAllUpdates);
            return true;

        case IGNORETHIS:
            if (app.ignoreThisUpdate >= app.suggestedVercode)
                app.ignoreThisUpdate = 0;
            else
                app.ignoreThisUpdate = app.suggestedVercode;
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
        // Ignore call if another download is running.
        if (downloadHandler != null && !downloadHandler.isComplete())
            return;

        final String repoaddress = getRepoAddress(apk);
        if (repoaddress == null) return;

        if (!apk.compatible) {
            AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
            ask_alrt.setMessage(R.string.installIncompatible);
            ask_alrt.setPositiveButton(R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            startDownload(apk, repoaddress);
                        }
                    });
            ask_alrt.setNegativeButton(R.string.no,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    });
            AlertDialog alert = ask_alrt.create();
            alert.show();
            return;
        }
        if (mInstalledSigID != null && apk.sig != null
                && !apk.sig.equals(mInstalledSigID)) {
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
        final String[] projection = { RepoProvider.DataColumns.ADDRESS };
        Repo repo = RepoProvider.Helper.findById(this, apk.repo, projection);
        if (repo == null || repo.address == null) {
            return null;
        }
        return repo.address;
    }

    private void startDownload(Apk apk, String repoAddress) {
        downloadHandler = new ApkDownloader(getBaseContext(), app, apk, repoAddress);
        localBroadcastManager.registerReceiver(downloaderProgressReceiver,
                new IntentFilter(Downloader.LOCAL_ACTION_PROGRESS));
        downloadHandler.setProgressListener(this);
        if (downloadHandler.download()) {
            mHeaderFragment.startProgress();
        }
    }

    private void installApk(File file) {
        try {
            installer.installPackage(file);
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

    final Installer.InstallerCallback myInstallerCallback = new Installer.InstallerCallback() {

        @Override
        public void onSuccess(final int operation) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (operation == Installer.InstallerCallback.OPERATION_INSTALL) {
                        PackageManagerCompat.setInstaller(mPm, app.id);
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
                    body = R.string.install_error_unknown;
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

    private void launchApk(String id) {
        Intent intent = mPm.getLaunchIntentForPackage(id);
        startActivity(intent);
    }

    private void shareApp(App app) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, app.name);
        shareIntent.putExtra(Intent.EXTRA_TEXT, app.name + " (" + app.summary + ") - https://f-droid.org/app/" + app.id);

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
            Utils.DebugLog(TAG, "Discarding downloader event \"" + event.type + "\" as it is from an old (probably cancelled) downloader.");
            return;
        }

        boolean finished = false;
        switch (event.type) {
        case ApkDownloader.EVENT_ERROR:
            final int res;
            if (event.getData().getInt(ApkDownloader.EVENT_DATA_ERROR_TYPE) == ApkDownloader.ERROR_HASH_MISMATCH)
                res = R.string.corrupt_download;
            else
                res = R.string.details_notinstalled;
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
            if (mHeaderFragment != null)
                mHeaderFragment.removeProgress();
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
            fdroidApp.sendViaBluetooth(this, resultCode, app.id);
            break;
        }
    }

    @Override
    public App getApp() { return app; }

    @Override
    public ApkListAdapter getApks() { return adapter; }

    @Override
    public Signature getInstalledSignature() { return mInstalledSignature; }

    @Override
    public String getInstalledSignatureId() { return mInstalledSigID; }

    public static class AppDetailsSummaryFragment extends Fragment {

        protected final Preferences prefs;
        private AppDetailsData data;
        private static final int MAX_LINES = 5;
        private static boolean view_all_description;
        private static boolean view_all_information;
        private static boolean view_all_permissions;
        private static LinearLayout ll_view_more_description;
        private static LinearLayout ll_view_more_information;
        private static LinearLayout ll_view_more_permissions;

        public AppDetailsSummaryFragment() {
            prefs = Preferences.get();
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            data = (AppDetailsData)activity;
        }

        protected App getApp() { return data.getApp(); }

        protected ApkListAdapter getApks() { return data.getApks(); }

        protected Signature getInstalledSignature() { return data.getInstalledSignature(); }

        protected String getInstalledSignatureId() { return data.getInstalledSignatureId(); }

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

        private void setupView(final View view) {
            // Expandable description
            final TextView description = (TextView) view.findViewById(R.id.description);
            final Spanned desc = Html.fromHtml(getApp().description, null, new Utils.HtmlTagHandler());
            description.setMovementMethod(SafeLinkMovementMethod.getInstance(getActivity()));
            description.setText(desc.subSequence(0, desc.length() - 2));
            final ImageView view_more_description = (ImageView) view.findViewById(R.id.view_more_description);
            description.post(new Runnable() {
                @Override
                public void run() {
                    // If description has more than five lines
                    if (description.getLineCount() > MAX_LINES) {
                        description.setMaxLines(MAX_LINES);
                        description.setOnClickListener(expander_description);
                        view_all_description = true;

                        ll_view_more_description = (LinearLayout) view.findViewById(R.id.ll_description);
                        ll_view_more_description.setOnClickListener(expander_description);

                        view_more_description.setImageResource(R.drawable.ic_expand_more_grey600);
                        view_more_description.setOnClickListener(expander_description);
                    } else {
                        view_more_description.setVisibility(View.GONE);
                    }
                }
            });

            // App ID
            final TextView appIdView = (TextView) view.findViewById(R.id.appid);
            if (prefs.expertMode())
                appIdView.setText(getApp().id);
            else
                appIdView.setVisibility(View.GONE);

            // Expandable information
            ll_view_more_information = (LinearLayout) view.findViewById(R.id.ll_information);
            final TextView information = (TextView) view.findViewById(R.id.information);
            final LinearLayout ll_view_more_information_content = (LinearLayout) view.findViewById(R.id.ll_information_content);
            ll_view_more_information_content.setVisibility(View.GONE);
            view_all_information = true;
            information.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_more_grey600), null);

            ll_view_more_information.setOnClickListener(expander_information);
            information.setOnClickListener(expander_information);

            // Summary
            final TextView summaryView = (TextView) view.findViewById(R.id.summary);
            summaryView.setText(getApp().summary);

            // Website button
            TextView tv = (TextView) view.findViewById(R.id.website);
            if (!TextUtils.isEmpty(getApp().webURL))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Source button
            tv = (TextView) view.findViewById(R.id.source);
            if (!TextUtils.isEmpty(getApp().sourceURL))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Issues button
            tv = (TextView) view.findViewById(R.id.issues);
            if (!TextUtils.isEmpty(getApp().trackerURL))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Changelog button
            tv = (TextView) view.findViewById(R.id.changelog);
            if (!TextUtils.isEmpty(getApp().changelogURL))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Donate button
            tv = (TextView) view.findViewById(R.id.donate);
            if (!TextUtils.isEmpty(getApp().donateURL))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Bitcoin
            tv = (TextView) view.findViewById(R.id.bitcoin);
            if (!TextUtils.isEmpty(getApp().bitcoinAddr))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Litecoin
            tv = (TextView) view.findViewById(R.id.litecoin);
            if (!TextUtils.isEmpty(getApp().litecoinAddr))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Dogecoin
            tv = (TextView) view.findViewById(R.id.dogecoin);
            if (!TextUtils.isEmpty(getApp().dogecoinAddr))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Flattr
            tv = (TextView) view.findViewById(R.id.flattr);
            if (!TextUtils.isEmpty(getApp().flattrID))
                tv.setOnClickListener(mOnClickListener);
            else
                tv.setVisibility(View.GONE);

            // Categories TextView
            final TextView categories = (TextView) view.findViewById(R.id.categories);
            if (prefs.expertMode() && getApp().categories != null)
                categories.setText(getApp().categories.toString().replaceAll(",", ", "));
            else
                categories.setVisibility(View.GONE);

            Apk curApk = null;
            for (int i = 0; i < getApks().getCount(); i++) {
                final Apk apk = getApks().getItem(i);
                if (apk.vercode == getApp().suggestedVercode) {
                    curApk = apk;
                    break;
                }
            }

            // Expandable permissions
            ll_view_more_permissions = (LinearLayout) view.findViewById(R.id.ll_permissions);
            final TextView permissionHeader = (TextView) view.findViewById(R.id.permissions);
            final TextView permissionListView = (TextView) view.findViewById(R.id.permissions_list);
            permissionListView.setVisibility(View.GONE);
            view_all_permissions = true;

            final boolean curApkCompatible = curApk != null && curApk.compatible;
            if (!getApks().isEmpty() && (curApkCompatible || prefs.showIncompatibleVersions())) {
                permissionHeader.setText(getString(R.string.permissions_for_long, getApks().getItem(0).version));
                permissionHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_more_grey600), null);

                ll_view_more_permissions.setOnClickListener(expander_permissions);
                permissionHeader.setOnClickListener(expander_permissions);
            } else {
                permissionHeader.setVisibility(View.GONE);
                permissionHeader.setCompoundDrawables(null, null, null, null);
            }

            // Anti features
            final TextView antiFeaturesView = (TextView) view.findViewById(R.id.antifeatures);
            if (getApp().antiFeatures != null) {
                StringBuilder sb = new StringBuilder();
                for (final String af : getApp().antiFeatures) {
                    final String afdesc = descAntiFeature(af);
                    if (afdesc != null) {
                        sb.append("\t• ").append(afdesc).append('\n');
                    }
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

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                switch (v.getId()) {
                    case R.id.website:
                        ((AppDetails) getActivity()).tryOpenUri(getApp().webURL);
                        break;

                    case R.id.source:
                        ((AppDetails) getActivity()).tryOpenUri(getApp().sourceURL);
                        break;

                    case R.id.issues:
                        ((AppDetails) getActivity()).tryOpenUri(getApp().trackerURL);
                        break;

                    case R.id.changelog:
                        ((AppDetails) getActivity()).tryOpenUri(getApp().changelogURL);
                        break;

                    case R.id.donate:
                        ((AppDetails) getActivity()).tryOpenUri(getApp().donateURL);
                        break;

                    case R.id.bitcoin:
                        ((AppDetails) getActivity()).tryOpenUri("bitcoin:" + getApp().bitcoinAddr);
                        break;

                    case R.id.litecoin:
                        ((AppDetails) getActivity()).tryOpenUri("litecoin:" + getApp().litecoinAddr);
                        break;

                    case R.id.dogecoin:
                        ((AppDetails) getActivity()).tryOpenUri("dogecoin:" + getApp().dogecoinAddr);
                        break;

                    case R.id.flattr:
                        ((AppDetails) getActivity()).tryOpenUri("https://flattr.com/thing/" + getApp().flattrID);
                        break;
                }
            }
        };

        private final View.OnClickListener expander_description = new View.OnClickListener() {
            public void onClick(View v) {
                final TextView description = (TextView) ll_view_more_description.findViewById(R.id.description);
                final ImageView view_more_permissions = (ImageView) ll_view_more_description.findViewById(R.id.view_more_description);
                if (!view_all_description) {
                    view_all_description = true;
                    description.setMaxLines(MAX_LINES);
                    view_more_permissions.setImageResource(R.drawable.ic_expand_more_grey600);
                } else {
                    view_all_description = false;
                    description.setMaxLines(Integer.MAX_VALUE);
                    view_more_permissions.setImageResource(R.drawable.ic_expand_less_grey600);
                }
            }
        };

        private final View.OnClickListener expander_information = new View.OnClickListener() {
            public void onClick(View v) {
                final TextView informationHeader = (TextView) ll_view_more_information.findViewById(R.id.information);
                final LinearLayout information_content = (LinearLayout) ll_view_more_information.findViewById(R.id.ll_information_content);
                if (!view_all_information) {
                    view_all_information = true;
                    information_content.setVisibility(View.GONE);
                    informationHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_more_grey600), null);
                } else {
                    view_all_information = false;
                    information_content.setVisibility(View.VISIBLE);
                    informationHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_less_grey600), null);
                }
            }
        };

        private final View.OnClickListener expander_permissions = new View.OnClickListener() {
            public void onClick(View v) {
                final TextView permissionHeader = (TextView) ll_view_more_permissions.findViewById(R.id.permissions);
                final TextView permissionListView = (TextView) ll_view_more_permissions.findViewById(R.id.permissions_list);
                if (!view_all_permissions) {
                    view_all_permissions = true;
                    permissionListView.setVisibility(View.GONE);
                    permissionHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_more_grey600), null);
                } else {
                    view_all_permissions = false;
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
                        }
                        permissionListView.setText(sb.toString());
                    }
                    permissionListView.setVisibility(View.VISIBLE);
                    permissionHeader.setCompoundDrawablesWithIntrinsicBounds(null, null, getActivity().getResources().getDrawable(R.drawable.ic_expand_less_grey600), null);
                }
            }
        };

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
            }
            return null;
        }

        public void updateViews(View view) {

            if (view == null) {
                Log.e(TAG, "AppDetailsSummaryFragment.updateViews(): view == null. Oops.");
                return;
            }

            TextView signatureView = (TextView) view.findViewById(R.id.signature);
            if (prefs.expertMode() && getInstalledSignature() != null) {
                signatureView.setVisibility(View.VISIBLE);
                signatureView.setText("Signed: " + getInstalledSignatureId());
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
        protected final DisplayImageOptions displayImageOptions;
        public static boolean installed = false;
        public static boolean updateWanted = false;

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

        private App getApp() { return data.getApp(); }

        private ApkListAdapter getApks() { return data.getApks(); }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.app_details_header, container, false);
            setupView(view);
            return view;
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            data = (AppDetailsData)activity;
        }

        private void setupView(View view) {

            // Set the icon...
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            ImageLoader.getInstance().displayImage(getApp().iconUrlLarge, iv,
                    displayImageOptions);

            // Set the title
            TextView tv = (TextView) view.findViewById(R.id.title);
            tv.setText(getApp().name);

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
        public void updateProgress(long progress, long total) {
            long percent = progress * 100 / total;
            setProgressVisible(true);
            progressBar.setIndeterminate(false);
            progressBar.setProgress((int) percent);
            progressBar.setMax(100);
            progressSize.setText(readableFileSize(progress) + " / " + readableFileSize(total));
            progressPercent.setText(Long.toString(percent) + " %");
        }

        /**
         * Converts a number of bytes to a human readable file size (eg 3.5 GiB).
         *
         * Based on http://stackoverflow.com/a/5599842
         */
        public String readableFileSize(long bytes) {
            final String[] units = getResources().getStringArray(R.array.file_size_units);
            if (bytes <= 0) return "0 " + units[0];
            int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
            return new DecimalFormat("#,##0.#")
                    .format(bytes / Math.pow(1024, digitGroups)) + " " + units[digitGroups];
        }

        /**
         * Shows or hides progress bar and related views.
         */
        private void setProgressVisible(boolean visible) {
            int state = (visible) ? View.VISIBLE : View.GONE;
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
            if (activity == null || activity.downloadHandler == null)
                return;

            activity.downloadHandler.cancel(true);
            activity.cleanUpFinishedDownload();
            setProgressVisible(false);
            updateViews();
        }

        public void updateViews() {
            updateViews(getView());
        }

        public void updateViews(View view) {
            TextView statusView = (TextView) view.findViewById(R.id.status);
            btMain.setVisibility(View.VISIBLE);

            AppDetails activity = (AppDetails) getActivity();
            if (activity.downloadHandler != null) {
                btMain.setText(R.string.downloading);
                btMain.setEnabled(false);
            // Check count > 0 due to incompatible apps resulting in an empty list.
            // If App isn't installed
            } else if (!getApp().isInstalled() && getApp().suggestedVercode > 0 &&
                    ((AppDetails)getActivity()).adapter.getCount() > 0) {
                installed = false;
                statusView.setText(R.string.details_notinstalled);
                NfcHelper.disableAndroidBeam(getActivity());
                // Set Install button and hide second button
                btMain.setText(R.string.menu_install);
                btMain.setOnClickListener(mOnClickListener);
                btMain.setEnabled(true);
            // If App is installed
            } else if (getApp().isInstalled()) {
                installed = true;
                statusView.setText(getString(R.string.details_installed, getApp().installedVersionName));
                NfcHelper.setAndroidBeam(getActivity(), getApp().id);
                if (getApp().canAndWantToUpdate()) {
                    updateWanted = true;
                    btMain.setText(R.string.menu_upgrade);
                } else {
                    updateWanted = false;
                    if (((AppDetails)getActivity()).mPm.getLaunchIntentForPackage(getApp().id) != null){
                        btMain.setText(R.string.menu_launch);
                    } else {
                        btMain.setText(R.string.menu_uninstall);
                        if (!getApp().uninstallable) {
                            btMain.setVisibility(View.GONE);
                        }
                    }
                }
                btMain.setOnClickListener(mOnClickListener);
                btMain.setEnabled(true);
            }
            TextView currentVersion = (TextView) view.findViewById(R.id.current_version);
            if (!getApks().isEmpty()) {
                currentVersion.setText(getApks().getItem(0).version);
            } else {
                currentVersion.setVisibility(View.GONE);
                btMain.setVisibility(View.GONE);
            }

        }

        private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                if (updateWanted) {
                    if (getApp().suggestedVercode > 0) {
                        final Apk apkToInstall = ApkProvider.Helper.find(getActivity(), getApp().id, getApp().suggestedVercode);
                        ((AppDetails)getActivity()).install(apkToInstall);
                        return;
                    }
                }
                // If installed
                if (installed) {
                    // If "launchable", launch
                    if (((AppDetails)getActivity()).mPm.getLaunchIntentForPackage(getApp().id) != null) {
                        ((AppDetails)getActivity()).launchApk(getApp().id);
                    } else {
                        ((AppDetails)getActivity()).removeApk(getApp().id);
                    }
                // If not installed, install
                } else if (getApp().suggestedVercode > 0) {
                    btMain.setEnabled(false);
                    btMain.setText(R.string.system_install_installing);
                    final Apk apkToInstall = ApkProvider.Helper.find(getActivity(), getApp().id, getApp().suggestedVercode);
                    ((AppDetails)getActivity()).install(apkToInstall);
                }
            }
        };
    }

    public static class AppDetailsListFragment extends ListFragment {

        private final String SUMMARY_TAG = "summary";

        private AppDetailsData data;
        private AppInstallListener installListener;
        private AppDetailsSummaryFragment summaryFragment = null;

        private FrameLayout headerView;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            data = (AppDetailsData)activity;
            installListener = (AppInstallListener)activity;
        }

        protected void install(final Apk apk) {
            installListener.install(apk);
        }

        protected void remove() {
            installListener.removeApk(getApp().id);
        }

        protected App getApp() { return data.getApp(); }

        protected ApkListAdapter getApks() { return data.getApks(); }

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
            final Apk apk = getApks().getItem(position - l.getHeaderViewsCount());
            if (getApp().installedVersionCode == apk.vercode) {
                remove();
            } else if (getApp().installedVersionCode > apk.vercode) {
                AlertDialog.Builder ask_alrt = new AlertDialog.Builder(getActivity());
                ask_alrt.setMessage(R.string.installDowngrade);
                ask_alrt.setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                                install(apk);
                            }
                        });
                ask_alrt.setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog,
                                    int whichButton) {
                            }
                        });
                AlertDialog alert = ask_alrt.create();
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
                summaryFragment = (AppDetailsSummaryFragment)fragment;
            } else {
                summaryFragment = new AppDetailsSummaryFragment();
            }
            getChildFragmentManager().beginTransaction().replace(headerView.getId(), summaryFragment, SUMMARY_TAG).commit();
            headerView.setVisibility(View.VISIBLE);
        }
    }

}
