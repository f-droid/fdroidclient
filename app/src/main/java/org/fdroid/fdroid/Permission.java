package org.fdroid.fdroid;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;

public class Permission {

    private final PackageManager packageManager;
    private final PermissionInfo permissionInfo;

    Permission(Context context, String permission)
            throws PackageManager.NameNotFoundException {
        this.packageManager = context.getPackageManager();
        this.permissionInfo = this.packageManager.getPermissionInfo(
                permission, PackageManager.GET_META_DATA);
    }

    public CharSequence getName() {
        String label = this.permissionInfo.loadLabel(this.packageManager).toString();
        return Character.toUpperCase(label.charAt(0)) + label.substring(1);
    }

}
