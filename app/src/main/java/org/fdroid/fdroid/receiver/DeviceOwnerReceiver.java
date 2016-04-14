/*
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.receiver;

import android.annotation.TargetApi;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.os.PersistableBundle;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import static android.app.admin.DevicePolicyManager.EXTRA_PROVISIONING_ADMIN_EXTRAS_BUNDLE;
import static android.app.admin.DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED;

/**
 * Receiver required to enable F-Droid as a "device owner".
 * Based on https://github.com/googlesamples/android-testdpc/
 * <p/>
 * TODO: implement bug report receiver for Android N?
 * (see code in https://github.com/googlesamples/android-testdpc)
 */
@TargetApi(Build.VERSION_CODES.M)
public class DeviceOwnerReceiver extends DeviceAdminReceiver {

    private static final String TAG = "DeviceOwnerReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            default:
                super.onReceive(context, intent);
                break;
        }
    }

    public static boolean isDeviceOwner(Context context) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        return devicePolicyManager.isDeviceOwnerApp(context.getPackageName());
    }

    public static void disableDeviceOwner(Context context) {
        DevicePolicyManager devicePolicyManager =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        devicePolicyManager.clearDeviceOwnerApp(context.getPackageName());
    }

    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        // Drop out quickly if we're not device owner.
        if (!isDeviceOwner(context)) {
            Log.e(TAG, "DeviceAdminReceiver.onProvisioningComplete() invoked, but device owner "
                    + "not assigned");
            return;
        }

        // From M onwards, permissions are not auto-granted, so we need to manually grant
        // permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            autoGrantRequestedPermissionsToSelf(context);
        }

        // TODO: callback?
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void autoGrantRequestedPermissionsToSelf(Context context) {
        DevicePolicyManager devicePolicyManager = (DevicePolicyManager) context.getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        String packageName = context.getPackageName();
        ComponentName adminComponentName = getComponentName(context);

        List<String> permissions = getRuntimePermissions(context.getPackageManager(), packageName);
        for (String permission : permissions) {
            boolean success = devicePolicyManager.setPermissionGrantState(adminComponentName,
                    packageName, permission, PERMISSION_GRANT_STATE_GRANTED);
            if (!success) {
                Log.e(TAG, "Failed to auto grant permission to self: " + permission);
            }
        }
    }

    private List<String> getRuntimePermissions(PackageManager packageManager, String packageName) {
        List<String> permissions = new ArrayList<>();
        PackageInfo packageInfo;
        try {
            packageInfo =
                    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Could not retrieve info about the package: " + packageName, e);
            return permissions;
        }

        if (packageInfo != null && packageInfo.requestedPermissions != null) {
            for (String requestedPerm : packageInfo.requestedPermissions) {
                if (isRuntimePermission(packageManager, requestedPerm)) {
                    permissions.add(requestedPerm);
                }
            }
        }
        return permissions;
    }

    private boolean isRuntimePermission(PackageManager packageManager, String permission) {
        try {
            PermissionInfo pInfo = packageManager.getPermissionInfo(permission, 0);
            if (pInfo != null) {
                if ((pInfo.protectionLevel & PermissionInfo.PROTECTION_MASK_BASE)
                        == PermissionInfo.PROTECTION_DANGEROUS) {
                    return true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.i(TAG, "Could not retrieve info about the permission: " + permission);
        }
        return false;
    }

    /**
     * @param context The context of the application.
     * @return The component name of this component in the given context.
     */
    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getApplicationContext(), DeviceOwnerReceiver.class);
    }

}