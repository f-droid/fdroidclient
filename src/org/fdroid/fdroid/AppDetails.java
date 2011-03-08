/*
 * Copyright (C) 2010-11  Ciaran Gultnieks, ciaran@ciarang.com
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
import java.math.BigInteger;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import android.preference.PreferenceManager;
import android.widget.ImageView;
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
            boolean iscurrent = apk.vercode == app_currentvercode;
            version.setText(getString(R.string.version) + " " + apk.version
                    + (iscurrent ? "*" : ""));
            TextView status = (TextView) v.findViewById(R.id.status);
            if (apk.version.equals(app.installedVersion))
                status.setText(getString(R.string.inst));
            else
                status.setText(getString(R.string.not_inst));
            TextView size = (TextView) v.findViewById(R.id.size);
            if (apk.size == 0) {
                size.setText("");
            } else {
                size.setText(getFriendlySize(apk.size));
            }
            TextView buildtype = (TextView) v.findViewById(R.id.buildtype);
            if(apk.srcname!=null) {
                buildtype.setText("source");
            } else { 
                buildtype.setText("bin");
            }
            if (!compatChecker.isCompatible(apk)) {
                View[] views = { v, version, status, size, buildtype };
                for (View view : views) {
                    view.setEnabled(false);
                }
            }
            return v;
        }
    }

    private static String getFriendlySize(int size) {
        double s = size;
        String[] format = { "%f B", "%.0f KiB", "%.1f MiB", "%.2f GiB" };
        int i = 0;
        while (i < format.length - 1 && s >= 1024) {
            s = (100 * s / 1024) / 100.0;
            i++;
        }
        return String.format(format[i], s);
    }

    private static final int INSTALL = Menu.FIRST;
    private static final int UNINSTALL = Menu.FIRST + 1;
    private static final int WEBSITE = Menu.FIRST + 2;
    private static final int ISSUES = Menu.FIRST + 3;
    private static final int SOURCE = Menu.FIRST + 4;
    private static final int MARKET = Menu.FIRST + 5;
    private static final int DONATE = Menu.FIRST + 6;

    private DB db;
    private DB.App app;
    private int app_currentvercode;
    private DB.Apk curapk;
    private String appid;
    private PackageManager mPm;
    private ProgressDialog pd;
    private DB.Apk.CompatibilityChecker compatChecker;
    private volatile boolean cancelDownload;

    private Context mctx = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.appdetails);

        Intent i = getIntent();
        appid = "";
        if (!i.hasExtra("appid")) {
            Log.d("FDroid", "No application ID in AppDetails!?");
        } else {
            appid = i.getStringExtra("appid");
        }

    }

    private boolean pref_cacheDownloaded;
    private boolean pref_expert;
    private boolean viewResetRequired;

    // The signature of the installed version.
    private Signature mInstalledSignature;
    private String mInstalledSigID;

    @Override
    protected void onStart() {
        super.onStart();
        db = new DB(this);
        mPm = getPackageManager();
        ((FDroidApp) getApplication()).inActivity++;
        // Get the preferences we're going to use in this Activity...
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(getBaseContext());
        pref_cacheDownloaded = prefs.getBoolean("cacheDownloaded", false);
        pref_expert = prefs.getBoolean("expert", false);
        viewResetRequired = true;
        compatChecker = DB.Apk.CompatibilityChecker.getChecker(this);

    }

    @Override
    protected void onResume() {
        if (viewResetRequired) {
            reset();
            viewResetRequired = false;
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        db.close();
        db = null;
        ((FDroidApp) getApplication()).inActivity--;
        super.onStop();
    }

    // Reset the display and list contents. Used when entering the activity, and
    // also when something has been installed/uninstalled.
    private void reset() {

        Log.d("FDroid", "Getting application details for " + appid);
        app = db.getApps(appid, null, true).get(0);
        DB.Apk curver = app.getCurrentVersion(compatChecker);
        app_currentvercode = curver == null ? 0 : curver.vercode;

        // Get the signature of the installed package...
        mInstalledSignature = null;
        if (curver != null) {
            PackageManager pm = getBaseContext().getPackageManager();
            try {
                PackageInfo pi = pm.getPackageInfo(appid,
                        PackageManager.GET_SIGNATURES);
                mInstalledSignature = pi.signatures[0];
                MessageDigest md;
                md = MessageDigest.getInstance("MD5");
                byte[] md5sum = new byte[32];
                md.update(mInstalledSignature.toCharsString().getBytes());
                md5sum = md.digest();
                BigInteger bigInt = new BigInteger(1, md5sum);
                String md5hash = bigInt.toString(16);
                while (md5hash.length() < 32)
                    md5hash = "0" + md5hash;
                mInstalledSigID = md5hash;
            } catch (NameNotFoundException e) {
                Log.d("FDroid", "Failed to get installed signature");
            } catch (NoSuchAlgorithmException e) {
                Log.d("FDroid", "Failed to calculate signature MD5 sum");
                mInstalledSignature = null;
            }
        }

        // Set the icon...
        ImageView iv = (ImageView) findViewById(R.id.icon);
        String icon_path = DB.getIconsPath() + app.icon;
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
        if (app.installedVersion == null)
            tv.setText(String.format(getString(R.string.details_notinstalled),
                    vnum));
        else
            tv.setText(String.format(getString(R.string.details_installed),
                    app.installedVersion));
        tv = (TextView) findViewById(R.id.description);
        tv.setText(app.description);
        if (pref_expert && mInstalledSignature != null) {
            tv = (TextView) findViewById(R.id.signature);
            tv.setText("Signed: " + mInstalledSigID);
        }

        // Set up the list...
        ApkListAdapter la = new ApkListAdapter(this);
        for (DB.Apk apk : app.apks)
            la.addItem(apk);
        setListAdapter(la);

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        curapk = app.apks.get(position);
        if (app.installedVersion != null
              && app.installedVersion.equals(curapk.version)) {
            removeApk(app.id);
        } else if (compatChecker.isCompatible(curapk)) {
            install();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        super.onCreateOptionsMenu(menu);
        menu.clear();
        DB.Apk curver = app.getCurrentVersion(compatChecker);
        if (app.installedVersion != null && curver != null
                && !app.installedVersion.equals(curver.version)) {
            menu.add(Menu.NONE, INSTALL, 0, R.string.menu_update).setIcon(
                    android.R.drawable.ic_menu_add);
        }
        if (app.installedVersion == null && curver != null) {
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
        if (app.marketVersion != null) {
            menu.add(Menu.NONE, MARKET, 5, R.string.menu_market).setIcon(
                    android.R.drawable.ic_menu_view);
        }
        if (app.donateURL != null) {
            menu.add(Menu.NONE, DONATE, 6, R.string.menu_donate).setIcon(
                    android.R.drawable.ic_menu_view);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

        case INSTALL:
            // Note that this handles updating as well as installing.
            curapk = app.getCurrentVersion(compatChecker);
            if (curapk != null)
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
                    .parse("http://market.android.com/details?id=" + app.id)));
            return true;

        case DONATE:
            startActivity(new Intent(Intent.ACTION_VIEW, Uri
                    .parse(app.donateURL)));
            return true;

        }
        return super.onOptionsItemSelected(item);
    }

    // Install the version of this app denoted by 'curapk'.
    private void install() {

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

        cancelDownload = false;

        pd = new ProgressDialog(this);
        pd.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pd.setMessage(getString(R.string.download_server));
        pd.setCancelable(true);
        pd.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        cancelDownload = true;
                    }
                });
        pd.setButton(getString(R.string.cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        pd.show();

        new Thread() {
            public void run() {

                // Download the apk file from the repository...
                File f;
                String apk_file = null;
                String apkname = curapk.apkName;
                String localfile = new String(LOCAL_PATH + "/" + apkname);
                try {

                    // See if we already have this apk cached...
                    f = new File(localfile);
                    if (f.exists()) {
                        // We do - if its hash matches, we'll use it...
                        Md5Handler hash = new Md5Handler();
                        String calcedhash = hash.md5Calc(f);
                        if (curapk.hash.equalsIgnoreCase(calcedhash)) {
                            apk_file = localfile;
                            Log.d("FDroid", "Using cached apk at " + localfile);
                            Message msg = new Message();
                            msg.arg1 = 0;
                            msg.arg2 = 1;
                            msg.obj = new String(localfile);
                            download_handler.sendMessage(msg);
                            msg = new Message();
                            msg.arg1 = 1;
                            download_handler.sendMessage(msg);
                        } else {
                            Log.d("FDroid", "Not using cached apk at "
                                    + localfile);
                            f.delete();
                        }
                    }

                    // If we haven't got the apk locally, we'll have to download
                    // it...
                    if (apk_file == null) {

                        String remotefile;
                        if (curapk.apkSource == null) {
                            remotefile = curapk.server + "/"
                                    + apkname.replace(" ", "%20");
                        } else {
                            remotefile = curapk.apkSource;
                        }
                        Log.d("FDroid", "Downloading apk from " + remotefile);

                        Message msg = new Message();
                        msg.arg1 = 0;
                        msg.arg2 = curapk.size;
                        msg.obj = new String(remotefile);
                        download_handler.sendMessage(msg);

                        BufferedInputStream getit = new BufferedInputStream(
                                new URL(remotefile).openStream(), 8192);

                        FileOutputStream saveit = new FileOutputStream(
                                localfile);
                        BufferedOutputStream bout = new BufferedOutputStream(
                                saveit, 1024);
                        byte data[] = new byte[1024];

                        int totalRead = 0;
                        int bytesRead = getit.read(data, 0, 1024);
                        while (bytesRead != -1) {
                            if (cancelDownload) {
                                Log.d("FDroid", "Download cancelled!");
                                break;
                            }
                            bout.write(data, 0, bytesRead);
                            totalRead += bytesRead;
                            msg = new Message();
                            msg.arg1 = totalRead;
                            download_handler.sendMessage(msg);
                            bytesRead = getit.read(data, 0, 1024);
                        }
                        bout.close();
                        getit.close();
                        saveit.close();
                        f = new File(localfile);
                        if (cancelDownload) {
                            f.delete();
                            msg = download_cancelled_handler.obtainMessage();
                            msg.sendToTarget();
                            return;
                        }
                        Md5Handler hash = new Md5Handler();
                        String calcedhash = hash.md5Calc(f);
                        if (curapk.hash.equalsIgnoreCase(calcedhash)) {
                            apk_file = localfile;
                        } else {
                            msg = new Message();
                            msg.obj = getString(R.string.corrupt_download);
                            download_error_handler.sendMessage(msg);
                            Log.d("FDroid", "Downloaded file hash of "
                                    + calcedhash + " did not match repo's "
                                    + curapk.hash);
                            // No point keeping a bad file, whether we're
                            // caching or
                            // not.
                            f = new File(localfile);
                            f.delete();
                        }
                    }
                } catch (Exception e) {
                    Log.e("FDroid", "Download failed:\n"
                            + Log.getStackTraceString(e));
                    Message msg = new Message();
                    msg.obj = e.getMessage();
                    download_error_handler.sendMessage(msg);
                    // Get rid of any partial download...
                    f = new File(localfile);
                    f.delete();
                }

                if (apk_file != null) {
                    Message msg = new Message();
                    msg.obj = apk_file;
                    download_complete_handler.sendMessage(msg);
                }
            }
        }.start();

    }

    // Handler used to update the progress dialog while downloading. The
    // message contains the progress (bytes read) in arg1. If this is 0,
    // the message also contains the total bytes to read in arg2, and the
    // message in obj.
    private Handler download_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            pd.setProgress(msg.arg1);
            if (msg.arg1 == 0) {
                pd.setMessage(getString(R.string.download_server) + ":\n "
                        + msg.obj.toString());
                pd.setMax(msg.arg2);
            }
        }
    };

    private Handler download_error_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            pd.dismiss();
            Toast.makeText(mctx, (String) msg.obj, Toast.LENGTH_LONG).show();
        }
    };

    private Handler download_complete_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            String apk_file = (String) msg.obj;
            installApk(apk_file);

            pd.dismiss();
        }
    };

    private Handler download_cancelled_handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(mctx, getString(R.string.download_cancelled),
                           Toast.LENGTH_SHORT).show();
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

    private void installApk(String file) {
        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse("file://" + file),
                "application/vnd.android.package-archive");

        startActivityForResult(intent, REQUEST_INSTALL);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_INSTALL) {
            // If we're not meant to be caching, delete the apk file we just
            // installed (or maybe the user cancelled the install - doesn't
            // matter)
            // from the SD card...
            if (!pref_cacheDownloaded) {
                String apkname = curapk.apkName;
                String apk_file = new String(LOCAL_PATH + "/" + apkname);
                File file = new File(apk_file);
                file.delete();
            }

            viewResetRequired = true;
        }

    }

}
