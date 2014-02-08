/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2013 Stefan Völkel, bd@bc-bd.org
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

import java.io.File;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;

import android.content.*;
import android.widget.*;
import org.fdroid.fdroid.data.*;
import org.xml.sax.XMLReader;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.Spanned;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Bitmap;

import android.support.v4.app.NavUtils;
import android.support.v4.view.MenuItemCompat;

import org.fdroid.fdroid.compat.PackageManagerCompat;
import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.compat.MenuManager;
import org.fdroid.fdroid.Utils.CommaSeparatedList;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

public class AppDetails extends ListActivity {

    private static final int REQUEST_INSTALL = 0;
    private static final int REQUEST_UNINSTALL = 1;
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

    private class ApkListAdapter extends ArrayAdapter<Apk> {

        private LayoutInflater mInflater = (LayoutInflater) mctx.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);

        public ApkListAdapter(Context context, App app) {
            super(context, 0);
            List<Apk> apks = ApkProvider.Helper.findByApp(context.getContentResolver(), app.id);
            for (Apk apk : apks ) {
                if (apk.compatible || pref_incompatibleVersions) {
                    add(apk);
                }
            }

        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            java.text.DateFormat df = DateFormat.getDateFormat(mctx);
            Apk apk = getItem(position);
            ViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.apklistitem, null);

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
                    + (apk.vercode == app.curVercode ? "  ☆" : ""));

            if (apk.vercode == app.getInstalledVerCode(getContext())
                    && mInstalledSigID != null && apk.sig != null
                    && apk.sig.equals(mInstalledSigID)) {
                holder.status.setText(getString(R.string.inst));
            } else {
                holder.status.setText(getString(R.string.not_inst));
            }

            if (apk.size > 0) {
                holder.size.setText(Utils.getFriendlySize(apk.size));
                holder.size.setVisibility(View.VISIBLE);
            } else {
                holder.size.setVisibility(View.GONE);
            }

            if (pref_expert && apk.minSdkVersion > 0) {
                holder.api.setText(getString(R.string.minsdk_or_later,
                            Utils.getAndroidVersionName(apk.minSdkVersion)));
                holder.api.setVisibility(View.VISIBLE);
            } else {
                holder.api.setVisibility(View.GONE);
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

            if (pref_expert && apk.nativecode != null) {
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
            View[] views = {
                convertView,
                holder.version,
                holder.status,
                holder.size,
                holder.api,
                holder.incompatibleReasons,
                holder.buildtype,
                holder.added,
                holder.nativecode
            };

            for (View v : views) {
                v.setEnabled(apk.compatible);
            }

            return convertView;
        }
    }

    private static final int INSTALL = Menu.FIRST;
    private static final int UNINSTALL = Menu.FIRST + 1;
    private static final int IGNOREALL = Menu.FIRST + 2;
    private static final int IGNORETHIS = Menu.FIRST + 3;
    private static final int WEBSITE = Menu.FIRST + 4;
    private static final int ISSUES = Menu.FIRST + 5;
    private static final int SOURCE = Menu.FIRST + 6;
    private static final int LAUNCH = Menu.FIRST + 7;
    private static final int SHARE = Menu.FIRST + 8;
    private static final int DONATE = Menu.FIRST + 9;
    private static final int BITCOIN = Menu.FIRST + 10;
    private static final int LITECOIN = Menu.FIRST + 11;
    private static final int DOGECOIN = Menu.FIRST + 12;
    private static final int FLATTR = Menu.FIRST + 13;
    private static final int DONATE_URL = Menu.FIRST + 14;

    private App app;
    private String appid;
    private PackageManager mPm;
    private DownloadHandler downloadHandler;
    private boolean stateRetained;

    private boolean startingIgnoreAll;
    private int startingIgnoreThis;

    LinearLayout headerView;
    View infoView;

    private final Context mctx = this;
    private DisplayImageOptions displayImageOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ((FDroidApp) getApplication()).applyTheme(this);

        super.onCreate(savedInstanceState);

        displayImageOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .cacheOnDisc(true)
            .imageScaleType(ImageScaleType.NONE)
            .showImageOnLoading(R.drawable.ic_repo_app_default)
            .showImageForEmptyUri(R.drawable.ic_repo_app_default)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();

        ActionBarCompat abCompat = ActionBarCompat.create(this);
        abCompat.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.appdetails);

        Intent i = getIntent();
        Uri data = i.getData();
        if (data != null) {
            if (data.isHierarchical()) {
                if (data.getHost() != null && data.getHost().equals("details")) {
                    // market://details?id=app.id
                    appid = data.getQueryParameter("id");
                } else {
                    // https://f-droid.org/app/app.id
                    appid = data.getLastPathSegment();
                    if (appid != null && appid.equals("app")) appid = null;
                }
            } else {
                // fdroid.app:app.id
                appid = data.getEncodedSchemeSpecificPart();
            }
            Log.d("FDroid", "AppDetails launched from link, for '" + appid
                    + "'");
        } else if (!i.hasExtra("appid")) {
            Log.d("FDroid", "No application ID in AppDetails!?");
        } else {
            appid = i.getStringExtra("appid");
        }

        if (i.hasExtra("from")) {
            setTitle(i.getStringExtra("from"));
        }

        mPm = getPackageManager();
        // Get the preferences we're going to use in this Activity...
        AppDetails old = (AppDetails) getLastNonConfigurationInstance();
        if (old != null) {
            copyState(old);
        } else {
            if (!reset()) {
                finish();
                return;
            }
            resetRequired = false;
        }

        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        pref_expert = prefs.getBoolean(Preferences.PREF_EXPERT, false);
        pref_permissions = prefs.getBoolean(Preferences.PREF_PERMISSIONS, false);
        pref_incompatibleVersions = prefs.getBoolean(
                Preferences.PREF_INCOMP_VER, false);

        // Set up the list...
        headerView = new LinearLayout(this);
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.addHeaderView(headerView);
        adapter = new ApkListAdapter(this, app);
        setListAdapter(adapter);

        startViews();

    }

    private boolean pref_expert;
    private boolean pref_permissions;
    private boolean pref_incompatibleVersions;
    private boolean resetRequired;

    // The signature of the installed version.
    private Signature mInstalledSignature;
    private String mInstalledSigID;

    @Override
    protected void onResume() {
        super.onResume();
        if (resetRequired) {
            if (!reset()) {
                finish();
                return;
            }
            resetRequired = false;
        }
        updateViews();

        MenuManager.create(this).invalidateOptionsMenu();

        if (downloadHandler != null) {
            downloadHandler.startUpdates();
        }
    }

    @Override
    protected void onPause() {
        if (downloadHandler != null) {
            downloadHandler.stopUpdates();
        }
        if (app != null && (app.ignoreAllUpdates != startingIgnoreAll
                || app.ignoreThisUpdate != startingIgnoreThis)) {
            setIgnoreUpdates(app.id, app.ignoreAllUpdates, app.ignoreThisUpdate);
        }
        super.onPause();
    }

    public void setIgnoreUpdates(String appId, boolean ignoreAll, int ignoreVersionCode) {

        Uri uri = AppProvider.getContentUri(appId);

        ContentValues values = new ContentValues(2);
        values.put(AppProvider.DataColumns.IGNORE_ALLUPDATES, ignoreAll ? 1 : 0);
        values.put(AppProvider.DataColumns.IGNORE_THISUPDATE, ignoreVersionCode);

        getContentResolver().update(uri, values, null, null);

    }


    @Override
    public Object onRetainNonConfigurationInstance() {
        stateRetained = true;
        return this;
    }

    @Override
    protected void onDestroy() {
        if (downloadHandler != null) {
            if (!stateRetained)
                downloadHandler.cancel();
            downloadHandler.destroy();
        }
        super.onDestroy();
    }

    // Copy all relevant state from an old instance. This is used in
    // place of reset(), so it must initialize all fields normally set
    // there.
    private void copyState(AppDetails old) {
        if (old.downloadHandler != null)
            downloadHandler = new DownloadHandler(old.downloadHandler);
        app = old.app;
        mInstalledSignature = old.mInstalledSignature;
        mInstalledSigID = old.mInstalledSigID;
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset() {

        Log.d("FDroid", "Getting application details for " + appid);
        app = null;

        if (appid != null && appid.length() > 0) {
            app = AppProvider.Helper.findById(getContentResolver(), appid);
        }

        if (app == null) {
            Toast toast = Toast.makeText(this,
                    getString(R.string.no_such_app), Toast.LENGTH_LONG);
            toast.show();
            finish();
            return false;
        }

        startingIgnoreAll = app.ignoreAllUpdates;
        startingIgnoreThis = app.ignoreThisUpdate;

        // Get the signature of the installed package...
        mInstalledSignature = null;
        mInstalledSigID = null;
        if (app.getInstalledVersion(this) != null) {
            PackageManager pm = getBaseContext().getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(appid,
                        PackageManager.GET_SIGNATURES);
                mInstalledSignature = pi.signatures[0];
                Hasher hash = new Hasher("MD5", mInstalledSignature
                        .toCharsString().getBytes());
                mInstalledSigID = hash.getHash();
            } catch (NameNotFoundException e) {
                Log.d("FDroid", "Failed to get installed signature");
            } catch (NoSuchAlgorithmException e) {
                Log.d("FDroid", "Failed to calculate signature MD5 sum");
                mInstalledSignature = null;
            }
        }
        return true;
    }

    private void startViews() {

        // Insert the 'infoView' (which contains the summary, various odds and
        // ends, and the description) into the appropriate place, if we're in
        // landscape mode. In portrait mode, we put it in the listview's
        // header..
        infoView = View.inflate(this, R.layout.appinfo, null);
        LinearLayout landparent = (LinearLayout) findViewById(R.id.landleft);
        headerView.removeAllViews();
        if (landparent != null) {
            landparent.addView(infoView);
            Log.d("FDroid", "Setting landparent infoview");
        } else {
            headerView.addView(infoView);
            Log.d("FDroid", "Setting header infoview");
        }

        // Set the icon...
        ImageView iv = (ImageView) findViewById(R.id.icon);
        ImageLoader.getInstance().displayImage(app.iconUrl, iv,
            displayImageOptions);

        // Set the title and other header details...
        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(app.name);
        tv = (TextView) findViewById(R.id.license);
        tv.setText(app.license);

        if (app.categories != null) {
            tv = (TextView) findViewById(R.id.categories);
            tv.setText(app.categories.toString().replaceAll(",",", "));
        }

        tv = (TextView) infoView.findViewById(R.id.description);

        tv.setMovementMethod(LinkMovementMethod.getInstance());

        // Need this to add the unimplemented support for ordered and unordered
        // lists to Html.fromHtml().
        class HtmlTagHandler implements TagHandler {
            int listNum;

            @Override
            public void handleTag(boolean opening, String tag, Editable output,
                    XMLReader reader) {
                if (tag.equals("ul")) {
                    if (opening)
                        listNum = -1;
                    else
                        output.append('\n');
                } else if (opening && tag.equals("ol")) {
                    if (opening)
                        listNum = 1;
                    else
                        output.append('\n');
                } else if (tag.equals("li")) {
                    if (opening) {
                        if (listNum == -1) {
                            output.append("\t• ");
                        } else {
                            output.append("\t").append(Integer.toString(listNum)).append(". ");
                            listNum++;
                        }
                    } else {
                        output.append('\n');
                    }
                }
            }
        }
        Spanned desc = Html.fromHtml(
                app.description, null, new HtmlTagHandler());
        tv.setText(desc.subSequence(0, desc.length() - 2));

        tv = (TextView) infoView.findViewById(R.id.appid);
        if (pref_expert)
            tv.setText(app.id);
        else
            tv.setVisibility(View.GONE);

        tv = (TextView) infoView.findViewById(R.id.summary);
        tv.setText(app.summary);

        Apk curApk = null;
        for (int i = 0; i < adapter.getCount(); i ++) {
            Apk apk = adapter.getItem(i);
            if (apk.vercode == app.curVercode) {
                curApk = apk;
                break;
            }
        }

        if (pref_permissions && !adapter.isEmpty() &&
                ((curApk != null && curApk.compatible) || pref_incompatibleVersions)) {
            tv = (TextView) infoView.findViewById(R.id.permissions_list);

            CommaSeparatedList permsList = adapter.getItem(0).permissions;
            if (permsList == null) {
                tv.setText(getString(R.string.no_permissions));
            } else {
                Iterator<String> permissions = permsList.iterator();
                StringBuilder sb = new StringBuilder();
                while (permissions.hasNext()) {
                    String permissionName = permissions.next();
                    try {
                        Permission permission = new Permission(this, permissionName);
                        sb.append("\t• ").append(permission.getName()).append('\n');
                    } catch (NameNotFoundException e) {
                        if (permissionName.equals("ACCESS_SUPERUSER")) {
                            sb.append("\t• Full permissions to all device features and storage\n");
                        } else {
                            Log.d("FDroid", "Permission not yet available: "
                                    +permissionName);
                        }
                    }
                }
                if (sb.length() > 0) sb.setLength(sb.length() - 1);
                tv.setText(sb.toString());
            }
            tv = (TextView) infoView.findViewById(R.id.permissions);
            tv.setText(getString(
                    R.string.permissions_for_long, adapter.getItem(0).version));
        } else {
            infoView.findViewById(R.id.permissions).setVisibility(View.GONE);
            infoView.findViewById(R.id.permissions_list).setVisibility(View.GONE);
        }

        tv = (TextView) infoView.findViewById(R.id.antifeatures);
        if (app.antiFeatures != null) {
            StringBuilder sb = new StringBuilder();
            for (String af : app.antiFeatures) {
                String afdesc = descAntiFeature(af);
                if (afdesc != null) {
                    sb.append("\t• ").append(afdesc).append("\n");
                }
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1);
                tv.setText(sb.toString());
            } else {
                tv.setVisibility(View.GONE);
            }
        } else {
            tv.setVisibility(View.GONE);
        }
    }

    private String descAntiFeature(String af) {
        if (af.equals("Ads"))
            return getString(R.string.antiadslist);
        if (af.equals("Tracking"))
            return getString(R.string.antitracklist);
        if (af.equals("NonFreeNet"))
            return getString(R.string.antinonfreenetlist);
        if (af.equals("NonFreeAdd"))
            return getString(R.string.antinonfreeadlist);
        if (af.equals("NonFreeDep"))
            return getString(R.string.antinonfreedeplist);
        if (af.equals("UpstreamNonFree"))
            return getString(R.string.antiupstreamnonfreelist);
        return null;
    }

    private void updateViews() {

        // Refresh the list...
        adapter.notifyDataSetChanged();

        TextView tv = (TextView) findViewById(R.id.status);
        if (app.getInstalledVersion(this) == null)
            tv.setText(getString(R.string.details_notinstalled));
        else
            tv.setText(getString(R.string.details_installed,
                    app.getInstalledVersion(this)));

        tv = (TextView) infoView.findViewById(R.id.signature);
        if (pref_expert && mInstalledSignature != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("Signed: " + mInstalledSigID);
        } else {
            tv.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final Apk apk = adapter.getItem(position - l.getHeaderViewsCount());
        if (app.getInstalledVerCode(this) == apk.vercode)
            removeApk(app.id);
        else if (app.getInstalledVerCode(this) > apk.vercode) {
            AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
            ask_alrt.setMessage(getString(R.string.installDowngrade));
            ask_alrt.setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            install(apk);
                        }
                    });
            ask_alrt.setNegativeButton(getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                        }
                    });
            AlertDialog alert = ask_alrt.create();
            alert.show();
        } else
            install(apk);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (app == null)
            return true;
        if (app.canAndWantToUpdate(this)) {
            MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, INSTALL, 0, R.string.menu_upgrade)
                        .setIcon(R.drawable.ic_menu_refresh),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                    MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }

        // Check count > 0 due to incompatible apps resulting in an empty list.
        if (app.getInstalledVersion(this) == null && app.curVercode > 0 &&
                adapter.getCount() > 0) {
            MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, INSTALL, 1, R.string.menu_install)
                        .setIcon(android.R.drawable.ic_menu_add),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                    MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        } else if (app.getInstalledVersion(this) != null) {
            MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, UNINSTALL, 1, R.string.menu_uninstall)
                        .setIcon(android.R.drawable.ic_menu_delete),
                    MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                    MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

            if (mPm.getLaunchIntentForPackage(app.id) != null) {
                MenuItemCompat.setShowAsAction(menu.add(
                            Menu.NONE, LAUNCH, 1, R.string.menu_launch)
                            .setIcon(android.R.drawable.ic_media_play),
                        MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                        MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
            }
        }

        MenuItemCompat.setShowAsAction(menu.add(
                    Menu.NONE, SHARE, 1, R.string.menu_share)
                    .setIcon(android.R.drawable.ic_menu_share),
                MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);

        menu.add(Menu.NONE, IGNOREALL, 2, R.string.menu_ignore_all)
                    .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                    .setCheckable(true)
                    .setChecked(app.ignoreAllUpdates);

        if (app.hasUpdates(this)) {
            menu.add(Menu.NONE, IGNORETHIS, 2, R.string.menu_ignore_this)
                        .setIcon(android.R.drawable.ic_menu_close_clear_cancel)
                        .setCheckable(true)
                        .setChecked(app.ignoreThisUpdate >= app.curVercode);
        }
        if (app.webURL.length() > 0) {
            menu.add(Menu.NONE, WEBSITE, 3, R.string.menu_website).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.trackerURL.length() > 0) {
            menu.add(Menu.NONE, ISSUES, 4, R.string.menu_issues).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.sourceURL.length() > 0) {
            menu.add(Menu.NONE, SOURCE, 5, R.string.menu_source).setIcon(
                    android.R.drawable.ic_menu_view);
        }

        if (app.bitcoinAddr != null || app.litecoinAddr != null ||
                app.dogecoinAddr != null ||
                app.flattrID != null || app.donateURL != null) {
            SubMenu donate = menu.addSubMenu(Menu.NONE, DONATE, 7,
                    R.string.menu_donate).setIcon(
                    android.R.drawable.ic_menu_send);
            if (app.bitcoinAddr != null)
                donate.add(Menu.NONE, BITCOIN, 8, R.string.menu_bitcoin);
            if (app.litecoinAddr != null)
                donate.add(Menu.NONE, LITECOIN, 8, R.string.menu_litecoin);
            if (app.dogecoinAddr != null)
                donate.add(Menu.NONE, DOGECOIN, 8, R.string.menu_dogecoin);
            if (app.flattrID != null)
                donate.add(Menu.NONE, FLATTR, 9, R.string.menu_flattr);
            if (app.donateURL != null)
                donate.add(Menu.NONE, DONATE_URL, 10, R.string.menu_website);
        }

        return true;
    }


    public void tryOpenUri(String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        if (intent.resolveActivity(getPackageManager()) == null) {
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case android.R.id.home:
            NavUtils.navigateUpFromSameTask(this);
            return true;

        case LAUNCH:
            launchApk(app.id);
            return true;

        case SHARE:
            shareApp(app);
            return true;

        case INSTALL:
            // Note that this handles updating as well as installing.
            if (app.curVercode > 0) {
                final Apk apkToInstall = ApkProvider.Helper.find(this, app.id, app.curVercode);
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
            if (app.ignoreThisUpdate >= app.curVercode)
                app.ignoreThisUpdate = 0;
            else
                app.ignoreThisUpdate = app.curVercode;
            item.setChecked(app.ignoreThisUpdate > 0);
            return true;

        case WEBSITE:
            tryOpenUri(app.webURL);
            return true;

        case ISSUES:
            tryOpenUri(app.trackerURL);
            return true;

        case SOURCE:
            tryOpenUri(app.sourceURL);
            return true;

        case BITCOIN:
            tryOpenUri("bitcoin:" + app.bitcoinAddr);
            return true;

        case LITECOIN:
            tryOpenUri("litecoin:" + app.litecoinAddr);
            return true;

        case DOGECOIN:
            tryOpenUri("dogecoin:" + app.dogecoinAddr);
            return true;

        case FLATTR:
            tryOpenUri("https://flattr.com/thing/" + app.flattrID);
            return true;

        case DONATE_URL:
            tryOpenUri(app.donateURL);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Install the version of this app denoted by 'app.curApk'.
    private void install(final Apk apk) {
        String [] projection = { RepoProvider.DataColumns.ADDRESS };
        Repo repo = RepoProvider.Helper.findById(
                getContentResolver(), apk.repo, projection);
        if (repo == null || repo.address == null) {
            return;
        }
        final String repoaddress = repo.address;

        if (!apk.compatible) {
            AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
            ask_alrt.setMessage(getString(R.string.installIncompatible));
            ask_alrt.setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            downloadHandler = new DownloadHandler(apk,
                                    repoaddress, Utils
                                    .getApkCacheDir(getBaseContext()));
                        }
                    });
            ask_alrt.setNegativeButton(getString(R.string.no),
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
                    getString(R.string.ok),
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
        downloadHandler = new DownloadHandler(apk, repoaddress,
                Utils.getApkCacheDir(getBaseContext()));
    }

    private void removeApk(String id) {
        PackageInfo pkginfo;
        try {
            pkginfo = mPm.getPackageInfo(id, 0);
        } catch (NameNotFoundException e) {
            Log.d("FDroid", "Couldn't find package " + id + " to uninstall.");
            return;
        }
        Uri uri = Uri.fromParts("package", pkginfo.packageName, null);
        Intent intent = new Intent(Intent.ACTION_DELETE, uri);
        startActivityForResult(intent, REQUEST_UNINSTALL);
        ((FDroidApp) getApplication()).invalidateApp(id);

    }

    private void installApk(File file, String id) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + file.getPath()),
                "application/vnd.android.package-archive");
        startActivityForResult(intent, REQUEST_INSTALL);
        ((FDroidApp) getApplication()).invalidateApp(id);
    }

    private void launchApk(String id) {
        Intent intent = mPm.getLaunchIntentForPackage(id);
        startActivity(intent);
    }

    private void shareApp(App app) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Android App: "+app.name);
        shareIntent.putExtra(Intent.EXTRA_TEXT, app.name+" ("+app.summary+") - https://f-droid.org/app/"+app.id);

        startActivity(Intent.createChooser(shareIntent, getString(R.string.menu_share)));
    }

    private ProgressDialog createProgressDialog(String file, int p, int max) {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage(getString(R.string.download_server) + ":\n " + file);
        pd.setMax(max);
        pd.setProgress(p);
        pd.setCancelable(true);
        pd.setCanceledOnTouchOutside(false);
        pd.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                downloadHandler.cancel();
            }
        });
        pd.setButton(DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        pd.cancel();
                    }
                });
        pd.show();
        return pd;
    }

    // Handler used to update the progress dialog while downloading.
    private class DownloadHandler extends Handler {
        private Downloader download;
        private ProgressDialog pd;
        private boolean updating;
        private String id;

        public DownloadHandler(Apk apk, String repoaddress, File destdir) {
            id = apk.id;
            download = new Downloader(apk, repoaddress, destdir);
            download.start();
            startUpdates();
        }

        public DownloadHandler(DownloadHandler oldHandler) {
            if (oldHandler != null) {
                download = oldHandler.download;
            }
            startUpdates();
        }

        public boolean updateProgress() {
            boolean finished = false;
            switch (download.getStatus()) {
            case RUNNING:
                if (pd == null) {
                    pd = createProgressDialog(download.remoteFile(),
                            download.getProgress(), download.getMax());
                } else {
                    pd.setProgress(download.getProgress());
                }
                break;
            case ERROR:
                if (pd != null)
                    pd.dismiss();
                String text;
                if (download.getErrorType() == Downloader.Error.CORRUPT)
                    text = getString(R.string.corrupt_download);
                else
                    text = download.getErrorMessage();
                Toast.makeText(AppDetails.this, text, Toast.LENGTH_LONG).show();
                finished = true;
                break;
            case DONE:
                if (pd != null)
                    pd.dismiss();
                installApk(download.localFile(), id);
                finished = true;
                break;
            case CANCELLED:
                Toast.makeText(AppDetails.this,
                        getString(R.string.download_cancelled),
                        Toast.LENGTH_SHORT).show();
                finished = true;
                break;
            default:
                break;
            }
            return finished;
        }

        public void startUpdates() {
            if (!updating) {
                updating = true;
                sendEmptyMessage(0);
            }
        }

        public void stopUpdates() {
            updating = false;
            removeMessages(0);
        }

        public void cancel() {
            if (download != null)
                download.interrupt();
        }

        public void destroy() {
            // The dialog can't be dismissed when it's not displayed,
            // so do it when the activity is being destroyed.
            if (pd != null) {
                pd.dismiss();
                pd = null;
            }
            // Cancel any scheduled updates so that we don't
            // accidentally recreate the progress dialog.
            stopUpdates();
        }

        // Repeatedly run updateProgress() until it's finished.
        @Override
        public void handleMessage(Message msg) {
            if (download == null)
                return;
            boolean finished = updateProgress();
            if (finished)
                download = null;
            else
                sendMessageDelayed(obtainMessage(), 50);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
        case REQUEST_INSTALL:
            if (downloadHandler != null) {
                downloadHandler = null;
            }

            PackageManagerCompat.setInstaller(mPm, app.id);
            resetRequired = true;
            break;
        case REQUEST_UNINSTALL:
            resetRequired = true;
            break;
        }
    }

}
