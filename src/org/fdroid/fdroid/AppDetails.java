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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.support.v4.view.MenuItemCompat;
import org.fdroid.fdroid.compat.ActionBarCompat;
import org.fdroid.fdroid.compat.MenuManager;
import org.fdroid.fdroid.DB.CommaSeparatedList;
import org.xml.sax.XMLReader;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.Signature;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Editable;
import android.text.Html;
import android.text.Html.TagHandler;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AppDetails extends ListActivity {

    private static final int REQUEST_INSTALL = 0;
    private static final int REQUEST_UNINSTALL = 1;

    private class ApkListAdapter extends BaseAdapter {

        private List<DB.Apk> items;

        public ApkListAdapter(Context context, List<DB.Apk> items) {
            this.items = (items != null ? items : new ArrayList<DB.Apk>());
        }

        public void addItem(DB.Apk apk) {
            items.add(apk);
        }

        public List<DB.Apk> getItems() {
            return items;
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public Object getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            java.text.DateFormat df = DateFormat.getDateFormat(mctx);

            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.apklistitem, null);
            }
            DB.Apk apk = items.get(position);
            TextView version = (TextView) v.findViewById(R.id.version);
            boolean iscurrent = apk.vercode == app_currentvercode;
            version.setText(getString(R.string.version) + " " + apk.version
                    + (iscurrent ? " *" : ""));

            TextView status = (TextView) v.findViewById(R.id.status);
            if (apk.vercode == app.installedVerCode
                    && apk.sig.equals(mInstalledSigID))
                status.setText(getString(R.string.inst));
            else
                status.setText(getString(R.string.not_inst));

            TextView size = (TextView) v.findViewById(R.id.size);
            if (apk.detail_size == 0) {
                size.setText("");
            } else {
                size.setText(Utils.getFriendlySize(apk.detail_size));
            }
            TextView buildtype = (TextView) v.findViewById(R.id.buildtype);
            if (apk.srcname != null) {
                buildtype.setText("source");
            } else {
                buildtype.setText("bin");
            }
            TextView added = (TextView) v.findViewById(R.id.added);
            if (apk.added != null) {
                added.setVisibility(View.VISIBLE);
                added.setText(getString(R.string.added_on, df.format(apk.added)));
            } else {
                added.setVisibility(View.GONE);
            }

            // Disable it all if it isn't compatible...
            View[] views = { v, version, status, size, buildtype, added };
            for (View view : views) {
                view.setEnabled(apk.compatible);
            }

            return v;
        }
    }

    private static final int INSTALL = Menu.FIRST;
    private static final int UNINSTALL = Menu.FIRST + 1;
    private static final int WEBSITE = Menu.FIRST + 2;
    private static final int ISSUES = Menu.FIRST + 3;
    private static final int SOURCE = Menu.FIRST + 4;
    private static final int MARKET = Menu.FIRST + 5;
    private static final int BITCOIN = Menu.FIRST + 6;
    private static final int FLATTR = Menu.FIRST + 7;
    private static final int DONATE = Menu.FIRST + 8;
    private static final int LAUNCH = Menu.FIRST + 9;
    private static final int SHARE = Menu.FIRST + 10;

    private DB.App app;
    private int app_currentvercode;
    private DB.Apk curapk;
    private String appid;
    private PackageManager mPm;
    private DownloadHandler downloadHandler;
    private boolean stateRetained;

    LinearLayout headerView;
    View infoView;

    private Context mctx = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ActionBarCompat abCompat = ActionBarCompat.create(this);
        abCompat.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.appdetails);

        Intent i = getIntent();
        appid = "";
        Uri data = getIntent().getData();
        if (data != null) {
            if (data.isHierarchical()) {
                if (data.getHost().equals("details")) {
                    // market://details?id=app.id
                    appid = data.getQueryParameter("id");
                } else {
                    // https://f-droid.org/app/app.id
                    appid = data.getLastPathSegment();
                }
            } else {
                // fdroid.app:app.id (old scheme)
                appid = data.getEncodedSchemeSpecificPart();
            }
            Log.d("FDroid", "AppDetails launched from link, for '" + appid
                    + "'");
        } else if (!i.hasExtra("appid")) {
            Log.d("FDroid", "No application ID in AppDetails!?");
        } else {
            appid = i.getStringExtra("appid");
        }

        // Set up the list...
        headerView = new LinearLayout(this);
        ListView lv = (ListView) findViewById(android.R.id.list);
        lv.addHeaderView(headerView);
        ApkListAdapter la = new ApkListAdapter(this, null);
        setListAdapter(la);

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
        startViews();

    }

    private boolean pref_expert;
    private boolean pref_permissions;
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
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        pref_expert = prefs.getBoolean("expert", false);
        pref_permissions = prefs.getBoolean("showPermissions", false);
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
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
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
        app_currentvercode = old.app_currentvercode;
        mInstalledSignature = old.mInstalledSignature;
        mInstalledSigID = old.mInstalledSigID;
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    // Return true if the app was found, false otherwise.
    private boolean reset() {

        Log.d("FDroid", "Getting application details for " + appid);
        app = null;
        List<DB.App> apps = ((FDroidApp) getApplication()).getApps();
        for (DB.App tapp : apps) {
            if (tapp.id.equals(appid)) {
                app = tapp;
                break;
            }
        }
        if (app == null) {
            Toast toast = Toast.makeText(this,
                    getString(R.string.no_such_app), Toast.LENGTH_LONG);
            toast.show();
            finish();
            return false;
        }

        // Make sure the app is populated.
        try {
            DB db = DB.getDB();
            db.populateDetails(app, 0);
        } catch (Exception ex) {
            Log.d("FDroid", "Failed to populate app - " + ex.getMessage());
        } finally {
            DB.releaseDB();
        }

        DB.Apk curver = app.getCurrentVersion();
        app_currentvercode = curver == null ? 0 : curver.vercode;

        // Get the signature of the installed package...
        mInstalledSignature = null;
        mInstalledSigID = null;
        if (app.installedVersion != null) {
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

        // Populate the list...
        ApkListAdapter la = (ApkListAdapter) getListAdapter();
        for (DB.Apk apk : app.apks)
            la.addItem(apk);
        la.notifyDataSetChanged();

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
        File icon = new File(DB.getIconsPath(this), app.icon);
        if (icon.exists()) {
            iv.setImageDrawable(new BitmapDrawable(icon.getPath()));
        } else {
            iv.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Set the title and other header details...
        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(app.name);
        tv = (TextView) findViewById(R.id.license);
        tv.setText(app.license);
        tv = (TextView) findViewById(R.id.status);

        tv = (TextView) infoView.findViewById(R.id.description);

        /*
        The following is a quick solution to enable both text selection and
        links. Causes glitches and crashes:
        java.lang.IndexOutOfBoundsException: setSpan (-1 ... -1) starts before 0
        
        class CustomMovementMethod extends LinkMovementMethod {
            @Override
            public boolean canSelectArbitrarily () {
                return true;
            }
        }
        
        if (Utils.hasApi(11)) {
            tv.setTextIsSelectable(true);
            tv.setMovementMethod(new CustomMovementMethod());
        } else {
            tv.setMovementMethod(LinkMovementMethod.getInstance());
        }
        */

        tv.setMovementMethod(LinkMovementMethod.getInstance());

        // Need this to add the unimplemented support for ordered and unordered
        // lists to Html.fromHtml().
        class HtmlTagHandler implements TagHandler {
            int listNum;

            @Override
            public void handleTag(boolean opening, String tag, Editable output,
                    XMLReader reader) {
                if (opening && tag.equals("ul")) {
                    listNum = -1;
                } else if (opening && tag.equals("ol")) {
                    listNum = 1;
                } else if (tag.equals("li")) {
                    if (opening) {
                        if (listNum == -1) {
                            output.append("\t•");
                        } else {
                            output.append("\t" + Integer.toString(listNum)
                                    + ". ");
                            listNum++;
                        }
                    } else {
                        output.append('\n');
                    }
                }
            }
        }
        tv.setText(Html.fromHtml(app.detail_description, null,
                new HtmlTagHandler()));

        tv = (TextView) infoView.findViewById(R.id.appid);
        tv.setText(app.id);

        tv = (TextView) infoView.findViewById(R.id.summary);
        tv.setText(app.summary);

        if (!app.apks.isEmpty()) {
            tv = (TextView) infoView.findViewById(R.id.permissions_list);

            CommaSeparatedList permsList = app.apks.get(0).detail_permissions;
            if (permsList == null) {
                tv.setText(getString(R.string.no_permissions) + '\n');
            } else {
                Iterator<String> permissions = permsList.iterator();
                StringBuilder sb = new StringBuilder();
                while (permissions.hasNext()) {
                    String permissionName = permissions.next();
                    try {
                        Permission permission = new Permission(this, permissionName);
                        sb.append("\t• " + permission.getName() + '\n');
                    } catch (NameNotFoundException e) {
                        Log.d( "FDroid",
                                "Can't find permission '" + permissionName + "'");
                    }
                }
                tv.setText(sb.toString());
            }
            tv = (TextView) infoView.findViewById(R.id.permissions);
            tv.setText(getString(
                    R.string.permissions_for_long, app.apks.get(0).version));
        }
    }

    private void updateViews() {

        // Refresh the list...
        ApkListAdapter la = (ApkListAdapter) getListAdapter();
        la.notifyDataSetChanged();

        TextView tv = (TextView) findViewById(R.id.status);
        if (app.installedVersion == null)
            tv.setText(getString(R.string.details_notinstalled));
        else
            tv.setText(getString(R.string.details_installed,
                    app.installedVersion));

        tv = (TextView) infoView.findViewById(R.id.appid);
        if (pref_expert) {
            tv.setVisibility(View.VISIBLE);
        } else {
            tv.setVisibility(View.GONE);
        }

        tv = (TextView) infoView.findViewById(R.id.signature);
        if (pref_expert && mInstalledSignature != null) {
            tv.setVisibility(View.VISIBLE);
            tv.setText("Signed: " + mInstalledSigID);
        } else {
            tv.setVisibility(View.GONE);
        }

        if (pref_permissions) {
            tv = (TextView) infoView.findViewById(R.id.permissions);
            tv.setVisibility(View.VISIBLE);
            tv = (TextView) infoView.findViewById(R.id.permissions_list);
            tv.setVisibility(View.VISIBLE);
        } else {
            tv = (TextView) infoView.findViewById(R.id.permissions);
            tv.setVisibility(View.GONE);
            tv = (TextView) infoView.findViewById(R.id.permissions_list);
            tv.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        curapk = app.apks.get(position - l.getHeaderViewsCount());
        if (app.installedVerCode == curapk.vercode)
            removeApk(app.id);
        else if (app.installedVerCode > curapk.vercode) {
            AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
            ask_alrt.setMessage(getString(R.string.installDowngrade));
            ask_alrt.setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            install();
                        }
                    });
            ask_alrt.setNegativeButton(getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            return;
                        }
                    });
            AlertDialog alert = ask_alrt.create();
            alert.show();
        } else
            install();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (app == null)
            return true;
        DB.Apk curver = app.getCurrentVersion();
        if (app.hasUpdates == true) {
            MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, INSTALL, 0, R.string.menu_update)
                        .setIcon(R.drawable.ic_menu_refresh),
                    MenuItemCompat.SHOW_AS_ACTION_IF_ROOM |
                    MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        }
        if (app.installedVersion == null && curver != null) {
            MenuItemCompat.setShowAsAction(menu.add(
                        Menu.NONE, INSTALL, 1, R.string.menu_install)
                        .setIcon(android.R.drawable.ic_menu_add),
                    MenuItemCompat.SHOW_AS_ACTION_ALWAYS |
                    MenuItemCompat.SHOW_AS_ACTION_WITH_TEXT);
        } else if (app.installedVersion != null) {
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

        if (app.detail_webURL.length() > 0) {
            menu.add(Menu.NONE, WEBSITE, 2, R.string.menu_website).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.detail_trackerURL.length() > 0) {
            menu.add(Menu.NONE, ISSUES, 3, R.string.menu_issues).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.detail_sourceURL.length() > 0) {
            menu.add(Menu.NONE, SOURCE, 4, R.string.menu_source).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        menu.add(Menu.NONE, MARKET, 5, R.string.menu_market).setIcon(
                android.R.drawable.ic_menu_view);
        if (app.detail_bitcoinAddr != null) {
            menu.add(Menu.NONE, BITCOIN, 6, R.string.menu_bitcoin).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.detail_flattrID != null) {
            menu.add(Menu.NONE, FLATTR, 7, R.string.menu_flattr).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.detail_donateURL != null) {
            menu.add(Menu.NONE, DONATE, 8, R.string.menu_donate).setIcon(
                    android.R.drawable.ic_menu_view);
        }

        return true;
    }


    public void tryOpenUri(String s) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(s));
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivity(intent);
        else
            Toast.makeText(this,
                    getString(R.string.no_handler_app, intent.getDataString()),
                    Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case LAUNCH:
            launchApk(app.id);
            return true;

        case SHARE:
            shareApp(app);
            return true;

        case INSTALL:
            // Note that this handles updating as well as installing.
            curapk = app.getCurrentVersion();
            if (curapk != null)
                install();
            return true;

        case UNINSTALL:
            removeApk(app.id);
            return true;

        case WEBSITE:
            tryOpenUri(app.detail_webURL);
            return true;

        case ISSUES:
            tryOpenUri(app.detail_trackerURL);
            return true;

        case SOURCE:
            tryOpenUri(app.detail_sourceURL);
            return true;

        case MARKET:
            tryOpenUri("https://play.google.com/store/apps/details?id=" + app.id);
            return true;

        case BITCOIN:
            tryOpenUri("bitcoin:" + app.detail_bitcoinAddr);
            return true;

        case FLATTR:
            tryOpenUri("https://flattr.com/thing/" + app.detail_flattrID);
            return true;

        case DONATE:
            tryOpenUri(app.detail_donateURL);
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Install the version of this app denoted by 'curapk'.
    private void install() {

        String ra = null;
        try {
            DB db = DB.getDB();
            DB.Repo repo = db.getRepo(curapk.repo);
            if (repo != null)
                ra = repo.address;
        } catch (Exception ex) {
            Log.d("FDroid", "Failed to get repo address - " + ex.getMessage());
        } finally {
            DB.releaseDB();
        }
        if (ra == null)
            return;
        final String repoaddress = ra;

        if (!curapk.compatible) {
            AlertDialog.Builder ask_alrt = new AlertDialog.Builder(this);
            ask_alrt.setMessage(getString(R.string.installIncompatible));
            ask_alrt.setPositiveButton(getString(R.string.yes),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            downloadHandler = new DownloadHandler(curapk,
                                    repoaddress, DB
                                            .getDataPath(getBaseContext()));
                        }
                    });
            ask_alrt.setNegativeButton(getString(R.string.no),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                int whichButton) {
                            return;
                        }
                    });
            AlertDialog alert = ask_alrt.create();
            alert.show();
            return;
        }
        if (mInstalledSigID != null && curapk.sig != null
                && !curapk.sig.equals(mInstalledSigID)) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage(R.string.SignatureMismatch).setPositiveButton(
                    getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
            return;
        }
        downloadHandler = new DownloadHandler(curapk, repoaddress,
                DB.getDataPath(this));
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
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + file.getPath()),
                "application/vnd.android.package-archive");
        startActivityForResult(intent, REQUEST_INSTALL);
        ((FDroidApp) getApplication()).invalidateApp(id);
    }

    private void launchApk(String id) {
        Intent intent = mPm.getLaunchIntentForPackage(id);
        startActivity(intent);
    }

    private void shareApp(DB.App app) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");

        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Android App: "+app.name);
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, app.name+" ("+app.summary+") - https://f-droid.org/app/"+app.id);

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
            public void onCancel(DialogInterface dialog) {
                downloadHandler.cancel();
            }
        });
        pd.setButton(DialogInterface.BUTTON_NEUTRAL,
                getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
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

        public DownloadHandler(DB.Apk apk, String repoaddress, File destdir) {
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
            resetRequired = true;
            break;
        case REQUEST_UNINSTALL:
            resetRequired = true;
            break;
        }
    }

}
