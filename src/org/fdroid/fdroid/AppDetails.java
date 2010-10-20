/*
 * Copyright (C) 2010  Ciaran Gultnieks, ciaran@ciarang.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.fdroid.fdroid.R;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class AppDetails extends ListActivity {

    private String LOCAL_PATH = "/sdcard/.fdroid";

    private static final int REQUEST_INSTALL = 0;
    private static final int REQUEST_UNINSTALL = 1;

    private class ApkListAdapter extends BaseAdapter {

        private List<DB.Apk> items = new ArrayList<DB.Apk>();

        public ApkListAdapter(Context context) {
        }

        public void addItem(DB.Apk apk) {
            items.add(apk);
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
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(R.layout.apklistitem, null);
            }
            DB.Apk apk = items.get(position);
            TextView version = (TextView) v.findViewById(R.id.version);
            version.setText("Version " + apk.version);
            TextView status = (TextView) v.findViewById(R.id.status);
            if (apk.version.equals(app.installedVersion))
                status.setText("Installed");
            else
                status.setText("Not installed");
            return v;
        }
    }

    private static final int INSTALL = Menu.FIRST;
    private static final int UNINSTALL = Menu.FIRST + 1;
    private static final int WEBSITE = Menu.FIRST + 2;
    private static final int ISSUES = Menu.FIRST + 3;
    private static final int SOURCE = Menu.FIRST + 4;
    private static final int MARKET = Menu.FIRST + 5;

    private DB db;
    private DB.App app;
    private DB.Apk curapk;
    private String appid;
    private PackageManager mPm;
    private ProgressDialog pd;

    private Context mctx = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.appdetails);

        db = new DB(this);
        mPm = getPackageManager();

        Intent i = getIntent();
        appid = "";
        if (!i.hasExtra("appid")) {
            Log.d("FDroid", "No application ID in AppDetails!?");
        } else {
            appid = i.getStringExtra("appid");
        }

        reset(false);

    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled. In the latter case,
    // 'update' is true to make the installed status get refreshed.
    private void reset(boolean update) {

        Log.d("FDroid", "Getting application details for " + appid);
        app = db.getApps(appid, null, update).get(0);

        // Set the icon...
        ImageView iv = (ImageView) findViewById(R.id.icon);
        String icon_path = this.getString(R.string.icons_path) + app.icon;
        File test_icon = new File(icon_path);
        if (test_icon.exists()) {
            iv.setImageDrawable(new BitmapDrawable(icon_path));
        } else {
            iv.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        // Set the title and other header details...
        TextView tv = (TextView) findViewById(R.id.title);
        tv.setText(app.name);
        tv = (TextView) findViewById(R.id.license);
        tv.setText(app.license);
        tv = (TextView) findViewById(R.id.status);
        int vnum = app.apks.size();
        String v = vnum == 1 ? "version" : "versions";
        tv.setText("" + vnum + " " + v + ", "
                + (app.installedVersion == null ? "not" : "1") + " installed");
        tv = (TextView) findViewById(R.id.description);
        tv.setText(app.description);

        // Set up the list...
        ApkListAdapter la = new ApkListAdapter(this);
        for (DB.Apk apk : app.apks)
            la.addItem(apk);
        setListAdapter(la);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Create alert dialog...
        final AlertDialog p = new AlertDialog.Builder(this).create();

        curapk = app.apks.get(position);

        // Set the title and icon...
        String icon_path = this.getString(R.string.icons_path) + app.icon;
        File test_icon = new File(icon_path);
        if (test_icon.exists()) {
            p.setIcon(new BitmapDrawable(icon_path));
        } else {
            p.setIcon(android.R.drawable.sym_def_app_icon);
        }
        p.setTitle(app.name + " " + curapk.version);

        boolean caninstall = true;
        String installed = getString(R.string.no);
        if (app.installedVersion != null) {
            if (app.installedVersion.equals(curapk.version)) {
                installed = getString(R.string.yes);
                caninstall = false;
            } else {
                installed += " - " + app.installedVersion;
            }
        }
        p.setMessage(getString(R.string.isinst) + " " + installed);

        p.setButton(getString(R.string.ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                });

        if (caninstall) {
            p.setButton2(getString(R.string.install),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            p.dismiss();
                            install();
                        }
                    });
        }
        else {
            p.setButton2(getString(R.string.uninstall),
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            p.dismiss();
                            removeApk(app.id);
                        }
                    });            
        }
            

        p.show();
    }

    // Install the version of this app denoted by 'curapk'.
    private void install() {
        new Thread() {
            public void run() {
                String apk_file = downloadFile(app, curapk);
                if (apk_file == null) {
                    Message msg = new Message();
                    msg.arg1 = 1;
                    download_handler.sendMessage(msg);
                    download_error_handler.sendEmptyMessage(0);
                } else {
                    installApk(apk_file);
                }
            }
        }.start();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.clear();
        if (app.installedVersion == null && app.getCurrentVersion() != null) {
            menu.add(Menu.NONE, INSTALL, 1, R.string.menu_install).setIcon(
                    android.R.drawable.ic_menu_add);
        } else {
            menu.add(Menu.NONE, UNINSTALL, 1, R.string.menu_uninstall).setIcon(
                    android.R.drawable.ic_menu_delete);
        }
        if (app.webURL.length() > 0) {
            menu.add(Menu.NONE, WEBSITE, 2, R.string.menu_website).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.trackerURL.length() > 0) {
            menu.add(Menu.NONE, ISSUES, 3, R.string.menu_issues).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.sourceURL.length() > 0) {
            menu.add(Menu.NONE, SOURCE, 4, R.string.menu_source).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        menu.add(Menu.NONE, MARKET, 5, R.string.menu_market).setIcon(
                android.R.drawable.ic_menu_view);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case INSTALL:
            curapk = app.getCurrentVersion();
            install();
            return true;

        case UNINSTALL:
            removeApk(app.id);
            return true;

        case WEBSITE:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(app.webURL)));
            return true;

        case ISSUES:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse(app.trackerURL)));
            return true;

        case SOURCE:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse(app.sourceURL)));
            return true;

        case MARKET:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse("market://search?q=pname:" + app.id)));
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Download the requested apk file, given the DB.App and DB.Apk
    // that refer to it. Returns the path to the downloaded file, or
    // null if the download was not successful.
    private String downloadFile(DB.App app, DB.Apk apk) {
        try {

            String apkname = apk.apkName;
            String localfile = new String(LOCAL_PATH + "/" + apkname);
            String remotefile = apk.server + "/" + apkname.replace(" ", "%20");

            Log.d("FDroid", "Downloading apk from " + remotefile);

            Message msg = new Message();
            msg.arg1 = 0;
            msg.obj = new String(remotefile);
            download_handler.sendMessage(msg);

            BufferedInputStream getit = new BufferedInputStream(new URL(
                    remotefile).openStream(), 8192);

            FileOutputStream saveit = new FileOutputStream(localfile);
            BufferedOutputStream bout = new BufferedOutputStream(saveit, 1024);
            byte data[] = new byte[1024];

            int readed = getit.read(data, 0, 1024);
            while (readed != -1) {
                bout.write(data, 0, readed);
                readed = getit.read(data, 0, 1024);
            }
            bout.close();
            getit.close();
            saveit.close();
            File f = new File(localfile);
            Md5Handler hash = new Md5Handler();

            if (apk.hash.equalsIgnoreCase(hash.md5Calc(f))) {
                return localfile;
            } else {
                return null;
            }
        } catch (Exception e) {
            Log.d("FDroid", "Download failed - " + e.getMessage());
            return null;
        }
    }

    private Handler download_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.arg1 == 0) {
                pd = ProgressDialog.show(mctx, getString(R.string.download),
                        getString(R.string.download_server) + ":\n "
                                + msg.obj.toString(), true);
            } else {
                pd.dismiss();
            }
        }
    };

    private Handler download_error_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mctx, getString(R.string.connection_error_msg),
                    Toast.LENGTH_LONG).show();
        }
    };

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
    }

    private void installApk(String id) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + id),
                "application/vnd.android.package-archive");

        Message msg = new Message();
        msg.arg1 = 1;
        download_handler.sendMessage(msg);

        startActivityForResult(intent, REQUEST_INSTALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        reset(true);
    }

}
