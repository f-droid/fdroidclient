package org.fdroid.fdroid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.graphics.drawable.Drawable;

public class Permission {

    private final PackageManager packageManager;
    private final PermissionInfo permissionInfo;

    public Permission(Context context, String permission)
            throws PackageManager.NameNotFoundException {
        this.packageManager = context.getPackageManager();
        this.permissionInfo = this.packageManager.getPermissionInfo(
                fdroidToAndroid(permission), PackageManager.GET_META_DATA);
    }

    /**
     * It appears that all of the permissions in android.Manifest.permissions
     * are prefixed with "android.permission." and then the constant name.
     * FDroid just includes the constant name in the apk list, so we prefix it
     * with "android.permission."
     */
    private static String fdroidToAndroid(String permission) {
        return "android.permission." + permission;
    }

    public CharSequence getName() {
        String label = this.permissionInfo.loadLabel(this.packageManager).toString();
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

    public CharSequence getDescription() {
        return this.permissionInfo.loadDescription(this.packageManager);
    }

    public Drawable getIcon() {
        return this.permissionInfo.loadIcon(this.packageManager);
    }

}
