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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;

public class InstallConfirmActivity extends Activity implements OnCancelListener, OnClickListener {

    public static final int RESULT_CANNOT_PARSE = RESULT_FIRST_USER + 1;

    private Intent intent;

    PackageManager mPm;

    AppDiff mAppDiff;

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

        final Drawable appIcon = mAppDiff.mPkgInfo.applicationInfo.loadIcon(mPm);
        final String appLabel = (String) mAppDiff.mPkgInfo.applicationInfo.loadLabel(mPm);

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
        AppSecurityPermissions perms = new AppSecurityPermissions(this, mAppDiff.mPkgInfo);
        if (mAppDiff.mInstalledAppInfo != null) {
            msg = (mAppDiff.mInstalledAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
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
                throw new RuntimeException("This should not happen. No new permissions were found but InstallConfirmActivity has been started!");
            }
            adapter.addTab(tabHost.newTabSpec(TAB_ID_NEW).setIndicator(
                    getText(R.string.newPerms)), mScrollView);
        } else  {
            findViewById(R.id.tabscontainer).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.VISIBLE);
        }
        final int NP = perms.getPermissionCount(AppSecurityPermissions.WHICH_PERSONAL);
        final int ND = perms.getPermissionCount(AppSecurityPermissions.WHICH_DEVICE);
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

        if (!permVisible) {
            if (mAppDiff.mInstalledAppInfo != null) {
                // This is an update to an application, but there are no
                // permissions at all.
                msg = (mAppDiff.mInstalledAppInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0
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

    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        ((FDroidApp) getApplication()).applyTheme(this);

        mPm = getPackageManager();

        intent = getIntent();
        Uri packageURI = intent.getData();

        mAppDiff = new AppDiff(mPm, packageURI);
        if (mAppDiff.mPkgInfo == null) {
            setResult(RESULT_CANNOT_PARSE, intent);
            finish();
        }

        setContentView(R.layout.install_start);
        mInstallConfirm = findViewById(R.id.install_confirm_panel);
        mInstallConfirm.setVisibility(View.INVISIBLE);

        startInstallConfirm();
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
