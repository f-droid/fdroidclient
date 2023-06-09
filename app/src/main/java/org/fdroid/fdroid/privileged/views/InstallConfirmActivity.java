/*
 **
 ** Copyright 2007, The Android Open Source Project
 ** Copyright 2015 Daniel Martí <mvdan@mvdan.cc>
 **
 ** Licensed under the Apache License, Version 2.0 (the "License");
 ** you may not use this file except in compliance with the License.
 ** You may obtain a copy of the License at
 **
 **     http://www.apache.org/licenses/LICENSE-2.0
 **
 ** Unless required by applicable law or agreed to in writing, software
 ** distributed under the License is distributed on an "AS IS" BASIS,
 ** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 ** See the License for the specific language governing permissions and
 ** limitations under the License.
 */

package org.fdroid.fdroid.privileged.views;

import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager.widget.ViewPager;

import org.fdroid.database.AppDao;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.DBHelper;
import org.fdroid.fdroid.installer.Installer;

/**
 * NOTES:
 * Parts are based on AOSP src/com/android/packageinstaller/PackageInstallerActivity.java
 * latest included commit: c23d802958158d522e7350321ad9ac6d43013883
 */
public class InstallConfirmActivity extends AppCompatActivity implements OnCancelListener, OnClickListener {

    private Intent intent;

    private AppDiff appDiff;

    // View for install progress
    private View installConfirm;
    // Buttons to indicate user acceptance
    private Button okButton;
    private Button cancelButton;
    private CaffeinatedScrollView scrollView;
    private boolean okCanInstall;

    private static final String TAB_ID_ALL = "all";
    private static final String TAB_ID_NEW = "new";

    private void startInstallConfirm() {
        TabHost tabHost = findViewById(android.R.id.tabhost);
        tabHost.setup();
        ViewPager viewPager = findViewById(R.id.pager);
        TabsAdapter adapter = new TabsAdapter(this, tabHost, viewPager);
        adapter.setOnTabChangedListener(tabId -> {
        });

        boolean permVisible = false;
        scrollView = null;
        okCanInstall = false;
        int msg = 0;
        AppSecurityPermissions perms = new AppSecurityPermissions(this, appDiff.apkPackageInfo);
        if (appDiff.installedApplicationInfo != null) {
            msg = (appDiff.installedApplicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                    ? R.string.install_confirm_update_system
                    : R.string.install_confirm_update;
            scrollView = new CaffeinatedScrollView(this);
            scrollView.setFillViewport(true);
            final boolean newPermissionsFound =
                    perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW) > 0;
            if (newPermissionsFound) {
                permVisible = true;
                scrollView.addView(perms.getPermissionsView(
                        AppSecurityPermissions.WHICH_NEW));
            } else {
                throw new RuntimeException("This should not happen. No new permissions were found"
                        + " but InstallConfirmActivity has been started!");
            }
            adapter.addTab(tabHost.newTabSpec(TAB_ID_NEW).setIndicator(
                    getText(R.string.newPerms)), scrollView);
        } else {
            findViewById(R.id.tabscontainer).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.VISIBLE);
        }
        final int n = perms.getPermissionCount(AppSecurityPermissions.WHICH_ALL);
        if (n > 0) {
            permVisible = true;
            LayoutInflater inflater = ContextCompat.getSystemService(this, LayoutInflater.class);
            View root = inflater.inflate(R.layout.permissions_list, null);
            if (scrollView == null) {
                scrollView = root.findViewById(R.id.scrollview);
            }
            final ViewGroup permList = root.findViewById(R.id.permission_list);
            permList.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
            adapter.addTab(tabHost.newTabSpec(TAB_ID_ALL).setIndicator(
                    getText(R.string.allPerms)), root);
        }

        if (!permVisible) {
            if (appDiff.installedApplicationInfo != null) {
                // This is an update to an application, but there are no
                // permissions at all.
                msg = (appDiff.installedApplicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        ? R.string.install_confirm_update_system_no_perms
                        : R.string.install_confirm_update_no_perms;
            } else {
                // This is a new application with no permissions.
                throw new RuntimeException("no permissions requested. This screen should not appear!");
            }
            tabHost.setVisibility(View.GONE);
            findViewById(R.id.filler).setVisibility(View.VISIBLE);
            findViewById(R.id.divider).setVisibility(View.GONE);
            scrollView = null;
        }
        if (msg != 0) {
            ((TextView) findViewById(R.id.install_confirm)).setText(msg);
        }
        installConfirm.setVisibility(View.VISIBLE);
        okButton = findViewById(R.id.ok_button);
        cancelButton = findViewById(R.id.cancel_button);
        okButton.setOnClickListener(this);
        cancelButton.setOnClickListener(this);
        if (scrollView == null) {
            // There is nothing to scroll view, so the ok button is immediately
            // set to install.
            okButton.setText(R.string.menu_install);
            okCanInstall = true;
        } else {
            scrollView.setFullScrollAction(() -> {
                okButton.setText(R.string.menu_install);
                okCanInstall = true;
            });
        }
    }

    @Override
    protected void onCreate(Bundle icicle) {
        FDroidApp fdroidApp = (FDroidApp) getApplication();
        fdroidApp.setSecureWindow(this);

        fdroidApp.applyPureBlackBackgroundInDarkTheme(this);

        super.onCreate(icicle);

        intent = getIntent();
        Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        AppDao appDao = DBHelper.getDb(this).getAppDao();
        Utils.runOffUiThread(() -> appDao.getApp(apk.repoId, apk.packageName), this::onAppLoaded);

        appDiff = new AppDiff(this, apk);

        setContentView(R.layout.install_start);

        // increase dialog to full width
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        installConfirm = findViewById(R.id.install_confirm_panel);
        installConfirm.setVisibility(View.INVISIBLE);

        startInstallConfirm();
    }

    private void onAppLoaded(org.fdroid.database.App dbApp) {
        App app = new App(dbApp, null);
        View appSnippet = findViewById(R.id.app_snippet);
        TextView appName = appSnippet.findViewById(R.id.app_name);
        appName.setText(app.name);
        ImageView appIcon = appSnippet.findViewById(R.id.app_icon);
        app.loadWithGlide(this, app.iconFile)
                .apply(Utils.getAlwaysShowIconRequestOptions())
                .into(appIcon);
    }

    // Generic handling when pressing back key
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onClick(View v) {
        if (v == okButton) {
            if (okCanInstall || scrollView == null) {
                setResult(RESULT_OK, intent);
                finish();
            } else {
                scrollView.pageScroll(View.FOCUS_DOWN);
            }
        } else if (v == cancelButton) {
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }
}
