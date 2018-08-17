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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AlertDialog;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class contains the SecurityPermissions view implementation.
 * Initially the package's advanced or dangerous security permissions
 * are displayed under categorized
 * groups. Clicking on the additional permissions presents
 * extended information consisting of all groups and permissions.
 * To use this view define a LinearLayout or any ViewGroup and add this
 * view by instantiating AppSecurityPermissions and invoking getPermissionsView.
 * <p>
 * NOTES:
 * Based on AOSP {@code core/java/android/widget/AppSecurityPermissions.java},
 * latest included commit: a3f68ef2f6811cf72f1282214c0883db5a30901d,
 * Reviewed against {@code frameworks/base/core/java/android/widget/AppSecurityPermissions.java},
 * from commit {@code android-8.1.0_r2}
 * <p>
 * To update this file, Start from latest included commit and include changes
 * until the newest commit with care:
 * <a href="http://github.com/android/platform_frameworks_base/blob/master/core/java/android/widget/AppSecurityPermissions">android/widget/AppSecurityPermissions.java</a>
 * <p>
 * This file has a different code style than the rest of fdroidclient because
 * it is kept in sync with the file from AOSP.  Please maintain the original
 * AOSP code style so it is easy to track changes.
 */
public class AppSecurityPermissions {

    private static final String TAG = "AppSecurityPermissions";

    public static final int WHICH_NEW = 1 << 2;
    public static final int WHICH_ALL = 0xffff;

    private final Context context;
    private final LayoutInflater inflater;
    private final PackageManager pm;
    private final Map<String, MyPermissionGroupInfo> permGroups = new HashMap<>();
    private final List<MyPermissionGroupInfo> permGroupsList = new ArrayList<>();
    private final PermissionGroupInfoComparator permGroupComparator = new PermissionGroupInfoComparator();
    private final PermissionInfoComparator permComparator = new PermissionInfoComparator();
    private final CharSequence newPermPrefix;

    // PermissionGroupInfo implements Parcelable but its Parcel constructor is private and thus cannot be extended.
    @SuppressLint("ParcelCreator")
    @SuppressWarnings("LineLength")
    static class MyPermissionGroupInfo extends PermissionGroupInfo {
        CharSequence label;

        final List<MyPermissionInfo> newPermissions = new ArrayList<>();
        final List<MyPermissionInfo> allPermissions = new ArrayList<>();

        MyPermissionGroupInfo(PermissionInfo perm) {
            name = perm.packageName;
            packageName = perm.packageName;
        }

        MyPermissionGroupInfo(PermissionGroupInfo info) {
            super(info);
        }

        @TargetApi(22)
        public Drawable loadGroupIcon(Context context, PackageManager pm) {
            Drawable iconDrawable;
            if (icon != 0) {
                iconDrawable = (Build.VERSION.SDK_INT < 22) ? loadIcon(pm) : loadUnbadgedIcon(pm);
            } else {
                iconDrawable = ContextCompat.getDrawable(context, R.drawable.ic_perm_device_info);
            }

            Preferences.Theme theme = Preferences.get().getTheme();
            Drawable wrappedIconDrawable = DrawableCompat.wrap(iconDrawable).mutate();
            DrawableCompat.setTint(wrappedIconDrawable, theme == Preferences.Theme.light ? Color.BLACK : Color.WHITE);
            return wrappedIconDrawable;
        }

    }

    // PermissionInfo implements Parcelable but its Parcel constructor is private and thus cannot be extended.
    @SuppressLint("ParcelCreator")
    private static class MyPermissionInfo extends PermissionInfo {
        CharSequence label;

        /**
         * PackageInfo.requestedPermissionsFlags for the currently installed
         * package, if it is installed.
         */
        int existingReqFlags;

        /**
         * True if this should be considered a new permission.
         */
        boolean newPerm;

        MyPermissionInfo(PermissionInfo info) {
            super(info);
        }
    }

    public static class PermissionItemView extends LinearLayout implements View.OnClickListener {
        MyPermissionGroupInfo group;
        MyPermissionInfo perm;
        AlertDialog dialog;

        public PermissionItemView(Context context, AttributeSet attrs) {
            super(context, attrs);
            setClickable(true);
        }

        public void setPermission(MyPermissionGroupInfo grp, MyPermissionInfo perm,
                                  boolean first, CharSequence newPermPrefix) {
            group = grp;
            this.perm = perm;

            ImageView permGrpIcon = (ImageView) findViewById(R.id.perm_icon);
            TextView permNameView = (TextView) findViewById(R.id.perm_name);

            PackageManager pm = getContext().getPackageManager();
            Drawable icon = null;
            if (first) {
                icon = grp.loadGroupIcon(getContext(), pm);
            }
            CharSequence label = perm.label;
            if (perm.newPerm && newPermPrefix != null) {
                // If this is a new permission, format it appropriately.
                SpannableStringBuilder builder = new SpannableStringBuilder();
                Parcel parcel = Parcel.obtain();
                TextUtils.writeToParcel(newPermPrefix, parcel, 0);
                parcel.setDataPosition(0);
                CharSequence newStr = TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(parcel);
                parcel.recycle();
                builder.append(newStr);
                builder.append(label);
                label = builder;
            }

            permGrpIcon.setImageDrawable(icon);
            permGrpIcon.setColorFilter(0xff757575);
            permNameView.setText(label);
            setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (group != null && perm != null) {
                if (dialog != null) {
                    dialog.dismiss();
                }
                PackageManager pm = getContext().getPackageManager();
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(group.label);
                if (perm.descriptionRes != 0) {
                    builder.setMessage(perm.loadDescription(pm));
                } else {
                    CharSequence appName;
                    try {
                        ApplicationInfo app = pm.getApplicationInfo(perm.packageName, 0);
                        appName = app.loadLabel(pm);
                    } catch (NameNotFoundException e) {
                        appName = perm.packageName;
                    }
                    builder.setMessage(getContext().getString(
                            R.string.perms_description_app, appName) + "\n\n" + perm.name);
                }
                builder.setCancelable(true);
                builder.setIcon(group.loadGroupIcon(getContext(), pm));
                dialog = builder.show();
                dialog.setCanceledOnTouchOutside(true);
            }
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            if (dialog != null) {
                dialog.dismiss();
            }
        }

    }

    private AppSecurityPermissions(Context context) {
        this.context = context;
        inflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        pm = this.context.getPackageManager();
        // Pick up from framework resources instead.
        newPermPrefix = this.context.getText(R.string.perms_new_perm_prefix);
    }

    public AppSecurityPermissions(Context context, PackageInfo info) {
        this(context);
        if (info == null) {
            return;
        }

        final Set<MyPermissionInfo> permSet = new HashSet<>();
        PackageInfo installedPkgInfo = null;
        if (info.requestedPermissions != null) {
            try {
                installedPkgInfo = pm.getPackageInfo(info.packageName,
                        PackageManager.GET_PERMISSIONS);
            } catch (NameNotFoundException ignored) {
            }
            extractPerms(info, permSet, installedPkgInfo);
        }
        setPermissions(new ArrayList<>(permSet));
    }

    private int[] getRequestedPermissionFlags(PackageInfo info) {
        if (Build.VERSION.SDK_INT < 16) {
            return new int[info.requestedPermissions.length];
        }
        return info.requestedPermissionsFlags;
    }

    private void extractPerms(PackageInfo info, Set<MyPermissionInfo> permSet,
                              PackageInfo installedPkgInfo) {

        final String[] strList = info.requestedPermissions;
        if (strList == null || strList.length == 0) {
            return;
        }

        for (String permName : strList) {
            try {
                PermissionInfo tmpPermInfo = pm.getPermissionInfo(permName, 0);
                if (tmpPermInfo == null) {
                    continue;
                }
                int existingIndex = -1;
                if (installedPkgInfo != null && installedPkgInfo.requestedPermissions != null) {
                    for (int j = 0; j < installedPkgInfo.requestedPermissions.length; j++) {
                        if (permName.equals(installedPkgInfo.requestedPermissions[j])) {
                            existingIndex = j;
                            break;
                        }
                    }
                }
                int existingFlags = 0;
                if (existingIndex >= 0) {
                    final int[] instFlagsList = getRequestedPermissionFlags(installedPkgInfo);
                    existingFlags = instFlagsList[existingIndex];
                }
                if (!isDisplayablePermission(tmpPermInfo, existingFlags)) {
                    // This is not a permission that is interesting for the user
                    // to see, so skip it.
                    continue;
                }
                final String origGroupName = tmpPermInfo.group;
                String groupName = origGroupName;
                if (groupName == null) {
                    groupName = tmpPermInfo.packageName;
                    tmpPermInfo.group = groupName;
                }
                MyPermissionGroupInfo group = permGroups.get(groupName);
                if (group == null) {
                    PermissionGroupInfo grp = null;
                    if (origGroupName != null) {
                        grp = pm.getPermissionGroupInfo(origGroupName, 0);
                    }
                    if (grp != null) {
                        group = new MyPermissionGroupInfo(grp);
                    } else {
                        // We could be here either because the permission
                        // didn't originally specify a group or the group it
                        // gave couldn't be found.  In either case, we consider
                        // its group to be the permission's package name.
                        tmpPermInfo.group = tmpPermInfo.packageName;
                        group = permGroups.get(tmpPermInfo.group);
                        if (group == null) {
                            group = new MyPermissionGroupInfo(tmpPermInfo);
                        }
                    }
                    permGroups.put(tmpPermInfo.group, group);
                }
                MyPermissionInfo myPerm = new MyPermissionInfo(tmpPermInfo);
                myPerm.existingReqFlags = existingFlags;
                myPerm.newPerm = isNewPermission(installedPkgInfo, existingFlags);
                permSet.add(myPerm);
            } catch (NameNotFoundException e) {
                Log.i(TAG, "Ignoring unknown permission:" + permName);
            }
        }
    }

    /**
     * A permission is a "new permission" if the app is already installed and
     * doesn't currently hold this permission. On older devices that don't support
     * this concept, permissions are never "new permissions".
     */
    @TargetApi(16)
    private static boolean isNewPermission(PackageInfo installedPkgInfo, int existingFlags) {
        if (installedPkgInfo == null || Build.VERSION.SDK_INT < 16) {
            return false;
        }

        return (existingFlags & PackageInfo.REQUESTED_PERMISSION_GRANTED) == 0;
    }

    private List<MyPermissionInfo> getPermissionList(MyPermissionGroupInfo grp, int which) {
        switch (which) {
            case WHICH_NEW:
                return grp.newPermissions;
            default:
                return grp.allPermissions;
        }
    }

    public int getPermissionCount(int which) {
        int n = 0;
        for (MyPermissionGroupInfo grp : permGroupsList) {
            n += getPermissionList(grp, which).size();
        }
        return n;
    }

    public View getPermissionsView(int which) {
        LinearLayout permsView = (LinearLayout) inflater.inflate(R.layout.app_perms_summary, null);
        LinearLayout displayList = (LinearLayout) permsView.findViewById(R.id.perms_list);
        View noPermsView = permsView.findViewById(R.id.no_permissions);

        displayPermissions(permGroupsList, displayList, which);
        if (displayList.getChildCount() <= 0) {
            noPermsView.setVisibility(View.VISIBLE);
        }

        return permsView;
    }

    /**
     * Utility method that displays permissions from a map containing group name and
     * list of permission descriptions.
     */
    private void displayPermissions(List<MyPermissionGroupInfo> groups,
                                    LinearLayout permListView, int which) {
        permListView.removeAllViews();

        int spacing = (int) (8 * context.getResources().getDisplayMetrics().density);

        for (MyPermissionGroupInfo grp : groups) {
            final List<MyPermissionInfo> perms = getPermissionList(grp, which);
            for (int j = 0; j < perms.size(); j++) {
                MyPermissionInfo perm = perms.get(j);
                View view = getPermissionItemView(grp, perm, j == 0,
                        which != WHICH_NEW ? newPermPrefix : null);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                if (j == 0) {
                    lp.topMargin = spacing;
                }
                if (j == grp.allPermissions.size() - 1) {
                    lp.bottomMargin = spacing;
                }
                if (permListView.getChildCount() == 0) {
                    lp.topMargin *= 2;
                }
                permListView.addView(view, lp);
            }
        }
    }

    private PermissionItemView getPermissionItemView(MyPermissionGroupInfo grp, MyPermissionInfo perm,
                                                     boolean first, CharSequence newPermPrefix) {
        PermissionItemView permView = (PermissionItemView) inflater.inflate(
                Build.VERSION.SDK_INT >= 17 &&
                        (perm.flags & PermissionInfo.FLAG_COSTS_MONEY) != 0
                        ? R.layout.app_permission_item_money : R.layout.app_permission_item,
                null);
        permView.setPermission(grp, perm, first, newPermPrefix);
        return permView;
    }

    @TargetApi(23)
    private boolean isDisplayablePermission(PermissionInfo pInfo, int existingReqFlags) {
        final int base = pInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE;
        final boolean isNormal = base == PermissionInfo.PROTECTION_NORMAL;
        final boolean isDangerous = base == PermissionInfo.PROTECTION_DANGEROUS
                || ((pInfo.protectionLevel & PermissionInfo.PROTECTION_FLAG_PRE23) != 0);

        // Dangerous and normal permissions are always shown to the user
        // this is matches the permission list in AppDetails2
        if (isNormal || isDangerous) {
            return true;
        }

        final boolean isDevelopment = (pInfo.protectionLevel & PermissionInfo.PROTECTION_FLAG_DEVELOPMENT) != 0;
        final boolean wasGranted = (existingReqFlags & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;

        // Development permissions are only shown to the user if they are already
        // granted to the app -- if we are installing an app and they are not
        // already granted, they will not be granted as part of the install.
        return isDevelopment && wasGranted;
    }

    private static class PermissionGroupInfoComparator implements Comparator<MyPermissionGroupInfo> {

        private final Collator collator = Collator.getInstance();

        public final int compare(MyPermissionGroupInfo a, MyPermissionGroupInfo b) {
            return collator.compare(a.label, b.label);
        }
    }

    private static class PermissionInfoComparator implements Comparator<MyPermissionInfo> {

        private final Collator collator = Collator.getInstance();

        PermissionInfoComparator() {
        }

        public final int compare(MyPermissionInfo a, MyPermissionInfo b) {
            return collator.compare(a.label, b.label);
        }
    }

    private void addPermToList(List<MyPermissionInfo> permList,
                               MyPermissionInfo pInfo) {
        if (pInfo.label == null) {
            pInfo.label = pInfo.loadLabel(pm);
        }
        int idx = Collections.binarySearch(permList, pInfo, permComparator);
        if (idx < 0) {
            idx = -idx - 1;
            permList.add(idx, pInfo);
        }
    }

    private void setPermissions(List<MyPermissionInfo> permList) {
        if (permList != null) {
            // First pass to group permissions
            for (MyPermissionInfo pInfo : permList) {
                if (!isDisplayablePermission(pInfo, pInfo.existingReqFlags)) {
                    continue;
                }
                MyPermissionGroupInfo group = permGroups.get(pInfo.group);
                if (group != null) {
                    pInfo.label = pInfo.loadLabel(pm);
                    addPermToList(group.allPermissions, pInfo);
                    if (pInfo.newPerm) {
                        addPermToList(group.newPermissions, pInfo);
                    }
                }
            }
        }

        for (MyPermissionGroupInfo pgrp : permGroups.values()) {
            if (pgrp.labelRes != 0 || pgrp.nonLocalizedLabel != null) {
                pgrp.label = pgrp.loadLabel(pm);
            } else {
                try {
                    ApplicationInfo app = pm.getApplicationInfo(pgrp.packageName, 0);
                    pgrp.label = app.loadLabel(pm);
                } catch (NameNotFoundException e) {
                    pgrp.label = pgrp.loadLabel(pm);
                }
            }
            permGroupsList.add(pgrp);
        }
        Collections.sort(permGroupsList, permGroupComparator);
    }
}
