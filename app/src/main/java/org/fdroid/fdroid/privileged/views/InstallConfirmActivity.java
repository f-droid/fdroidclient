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

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;

/**
 * NOTES:
 * Parts are based on AOSP src/com/android/packageinstaller/PackageInstallerActivity.java
 * latest included commit: c23d802958158d522e7350321ad9ac6d43013883
 */
public class InstallConfirmActivity extends FragmentActivity implements OnCancelListener, OnClickListener {

    public static final int RESULT_CANNOT_PARSE = RESULT_FIRST_USER + 1;

    private Intent intent;

    private PackageManager mPm;

    private AppDiff mAppDiff;

    // View for install progress
    private View mInstallConfirm;
    // Buttons to indicate user acceptance
    private Button mOk;
    private Button mCancel;
    private CaffeinatedScrollView mScrollView;
    private boolean mOkCanInstall;

    private static final String TAB_ID_ALL = "all";
    private static final String TAB_ID_NEW = "new";

    private App mApp;

    private final DisplayImageOptions displayImageOptions = new DisplayImageOptions.Builder()
            .cacheInMemory(true)
            .cacheOnDisk(true)
            .imageScaleType(ImageScaleType.NONE)
            .showImageOnLoading(R.drawable.ic_repo_app_default)
            .showImageForEmptyUri(R.drawable.ic_repo_app_default)
            .bitmapConfig(Bitmap.Config.RGB_565)
            .build();

    private void startInstallConfirm() {
        View appSnippet = findViewById(R.id.app_snippet);
        TextView appName = (TextView) appSnippet.findViewById(R.id.app_name);
        ImageView appIcon = (ImageView) appSnippet.findViewById(R.id.app_icon);
        TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

        appName.setText(mApp.name);
        ImageLoader.getInstance().displayImage(mApp.iconUrlLarge, appIcon,
                displayImageOptions);

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
                    perms.getPermissionCount(AppSecurityPermissions.WHICH_NEW) > 0;
            if (newPermissionsFound) {
                permVisible = true;
                mScrollView.addView(perms.getPermissionsView(
                        AppSecurityPermissions.WHICH_NEW));
            } else {
                throw new RuntimeException("This should not happen. No new permissions were found but InstallConfirmActivity has been started!");
            }
            adapter.addTab(tabHost.newTabSpec(TAB_ID_NEW).setIndicator(
                    getText(R.string.newPerms)), mScrollView);
        } else {
            findViewById(R.id.tabscontainer).setVisibility(View.GONE);
            findViewById(R.id.divider).setVisibility(View.VISIBLE);
        }
        final int n = perms.getPermissionCount(AppSecurityPermissions.WHICH_ALL);
        if (n > 0) {
            permVisible = true;
            LayoutInflater inflater = (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
            View root = inflater.inflate(R.layout.permissions_list, null);
            if (mScrollView == null) {
                mScrollView = (CaffeinatedScrollView) root.findViewById(R.id.scrollview);
            }
            final ViewGroup permList = (ViewGroup) root.findViewById(R.id.permission_list);
            permList.addView(perms.getPermissionsView(AppSecurityPermissions.WHICH_ALL));
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
                throw new RuntimeException("no permissions requested. This screen should not appear!");
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

        ((FDroidApp) getApplication()).applyDialogTheme(this);

        mPm = getPackageManager();

        intent = getIntent();
        Uri uri = intent.getData();
        Apk apk = ApkProvider.Helper.find(this, uri, ApkProvider.DataColumns.ALL);
        mApp = AppProvider.Helper.findByPackageName(getContentResolver(), apk.packageName);

        mAppDiff = new AppDiff(mPm, apk);
        if (mAppDiff.mPkgInfo == null) {
            setResult(RESULT_CANNOT_PARSE, intent);
            finish();
        }

        setContentView(R.layout.install_start);

        // increase dialog to full width for now
        // TODO: create a better design and minimum width for tablets
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mInstallConfirm = findViewById(R.id.install_confirm_panel);
        mInstallConfirm.setVisibility(View.INVISIBLE);

        startInstallConfirm();
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
