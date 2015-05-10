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
package org.fdroid.fdroid.installer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import org.fdroid.fdroid.R;

public class InstallConfirmActivity extends Activity implements OnCancelListener, OnClickListener {

    private Intent intent;

    PackageManager mPm;
    PackageInfo mPkgInfo;

    private ApplicationInfo mAppInfo = null;

    // View for install progress
    View mInstallConfirm;
    // Buttons to indicate user acceptance
    private Button mOk;
    private Button mCancel;
    CaffeinatedScrollView mScrollView = null;
    private boolean mOkCanInstall = false;

    private static final String TAB_ID_ALL = "all";
    private static final String TAB_ID_NEW = "new";

    private void startInstallConfirm() {

        final Drawable appIcon = mPkgInfo.applicationInfo.loadIcon(mPm);
        final String appLabel = (String) mPkgInfo.applicationInfo.loadLabel(mPm);

        View appSnippet = findViewById(R.id.app_snippet);
        ((ImageView) appSnippet.findViewById(R.id.app_icon)).setImageDrawable(appIcon);
        ((TextView) appSnippet.findViewById(R.id.app_name)).setText(appLabel);

        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);
        tabHost.setup();
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        TabsAdapter adapter = new TabsAdapter(this, tabHost, viewPager);
        adapter.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            @Override
            public void onTabChanged(String tabId) {
            }
        });

        boolean permVisible = false;
        mScrollView = null;
        mOkCanInstall = false;
        int msg = 0;
        if (mPkgInfo != null) {
            AppSecurityPermissions perms = new AppSecurityPermissions(this, mPkgInfo);
            final int NP = perms.getPermissionCount(AppSecurityPermissions.WHICH_PERSONAL);
            final int ND = perms.getPermissionCount(AppSecurityPermissions.WHICH_DEVICE);
            if (mAppInfo != null) {
                msg = (mAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        ? R.string.install_confirm_update_system
                        : R.string.install_confirm_update;
                mScrollView = new CaffeinatedScrollView(this);
                mScrollView.setFillViewport(true);
                final boolean newPermissionsFound =
                        (perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW) > 0);
                if (newPermissionsFound) {
                    permVisible = true;
                    mScrollView.addView(perms.getPermissionsView(
                            AppSecurityPermissions.WHICH_NEW));
                } else {
                    LayoutInflater inflater = (LayoutInflater) getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);
                    TextView label = (TextView) inflater.inflate(R.layout.label, null);
                    label.setText(R.string.no_new_perms);
                    mScrollView.addView(label);
                }
                adapter.addTab(tabHost.newTabSpec(TAB_ID_NEW).setIndicator(
                        getText(R.string.newPerms)), mScrollView);
            } else  {
                findViewById(R.id.tabscontainer).setVisibility(View.GONE);
                findViewById(R.id.divider).setVisibility(View.VISIBLE);
            }
            if (NP > 0 || ND > 0) {
                permVisible = true;
                LayoutInflater inflater = (LayoutInflater) getSystemService(
                        Context.LAYOUT_INFLATER_SERVICE);
                View root = inflater.inflate(R.layout.permissions_list, null);
                if (mScrollView == null) {
                    mScrollView = (CaffeinatedScrollView) root.findViewById(R.id.scrollview);
                }
                final ViewGroup privacyList = (ViewGroup) root.findViewById(R.id.privacylist);
                if (NP > 0) {
                    privacyList.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_PERSONAL));
                } else {
                    privacyList.setVisibility(View.GONE);
                }
                final ViewGroup deviceList = (ViewGroup) root.findViewById(R.id.devicelist);
                if (ND > 0) {
                    deviceList.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_DEVICE));
                } else {
                    root.findViewById(R.id.devicelist).setVisibility(View.GONE);
                }
                adapter.addTab(tabHost.newTabSpec(TAB_ID_ALL).setIndicator(
                        getText(R.string.allPerms)), root);
            }
        }
        if (!permVisible) {
            if (mAppInfo != null) {
                // This is an update to an application, but there are no
                // permissions at all.
                msg = (mAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                        ? R.string.install_confirm_update_system_no_perms
                        : R.string.install_confirm_update_no_perms;
            } else {
                // This is a new application with no permissions.
                msg = R.string.install_confirm_no_perms;
            }
            tabHost.setVisibility(View.GONE);
            findViewById(R.id.filler).setVisibility(View.VISIBLE);
            findViewById(R.id.divider).setVisibility(View.GONE);
            mScrollView = null;
        }
        if (msg != 0) {
            ((TextView) findViewById(R.id.install_confirm)).setText(msg);
        }
        mInstallConfirm.setVisibility(View.VISIBLE);
        mOk = (Button) findViewById(R.id.ok_button);
        mCancel = (Button) findViewById(R.id.cancel_button);
        mOk.setOnClickListener(this);
        mCancel.setOnClickListener(this);
        if (mScrollView == null) {
            // There is nothing to scroll view, so the ok button is immediately
            // set to install.
            mOk.setText(R.string.menu_install);
            mOkCanInstall = true;
        } else {
            mScrollView.setFullScrollAction(new Runnable() {
                @Override
                public void run() {
                    mOk.setText(R.string.menu_install);
                    mOkCanInstall = true;
                }
            });
        }
    }

    private void initiateInstall() {
        String pkgName = mPkgInfo.packageName;
        // Check if there is already a package on the device with this name
        // but it has been renamed to something else.
        final String[] oldName = mPm.canonicalToCurrentPackageNames(new String[] { pkgName });
        if (oldName != null && oldName.length > 0 && oldName[0] != null) {
            pkgName = oldName[0];
            mPkgInfo.packageName = pkgName;
            mPkgInfo.applicationInfo.packageName = pkgName;
        }
        // Check if package is already installed. display confirmation dialog if replacing pkg
        try {
            // This is a little convoluted because we want to get all uninstalled
            // apps, but this may include apps with just data, and if it is just
            // data we still want to count it as "installed".
            mAppInfo = mPm.getApplicationInfo(pkgName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
            if ((mAppInfo.flags&ApplicationInfo.FLAG_INSTALLED) == 0) {
                mAppInfo = null;
            }
        } catch (NameNotFoundException e) {
            mAppInfo = null;
        }

        startInstallConfirm();
    }

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        mPm = getPackageManager();

        intent = getIntent();
        Uri mPackageURI = intent.getData();
        final String pkgPath = mPackageURI.getPath();

        mPkgInfo = mPm.getPackageArchiveInfo(pkgPath, PackageManager.GET_PERMISSIONS);
        mPkgInfo.applicationInfo.sourceDir = pkgPath;
        mPkgInfo.applicationInfo.publicSourceDir = pkgPath;

        setContentView(R.layout.install_start);
        mInstallConfirm = findViewById(R.id.install_confirm_panel);
        mInstallConfirm.setVisibility(View.INVISIBLE);

        initiateInstall();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    // Generic handling when pressing back key
    public void onCancel(DialogInterface dialog) {
        finish();
    }

    public void onClick(View v) {
        if (v == mOk) {
            if (mOkCanInstall || mScrollView == null) {
                setResult(RESULT_OK, intent);
                finish();
            } else {
                mScrollView.pageScroll(View.FOCUS_DOWN);
            }
        } else if (v == mCancel) {
            setResult(RESULT_CANCELED, intent);
            finish();
        }
    }
}
