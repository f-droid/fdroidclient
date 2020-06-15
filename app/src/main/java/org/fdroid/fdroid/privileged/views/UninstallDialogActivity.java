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

package org.fdroid.fdroid.privileged.views;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.appcompat.app.AlertDialog;
import android.view.ContextThemeWrapper;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.installer.Installer;

/**
 * This class provides the confirmation prompt for when the user chooses to
 * uninstall an app.  This has to be implemented here for the privileged
 * extension, it is only shown for {@link Installer} instances that can do
 * installs and uninstalls without user prompts, which is detected via
 * {@link Installer#isUnattended()}.
 */
public class UninstallDialogActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        final Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);

        PackageManager pm = getPackageManager();

        ApplicationInfo appInfo;
        try {
            //noinspection WrongConstant (lint is actually wrong here!)
            appInfo = pm.getApplicationInfo(apk.packageName,
                    PackageManager.GET_UNINSTALLED_PACKAGES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Failed to get ApplicationInfo for uninstalling");
        }

        final boolean isSystem = (appInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
        final boolean isUpdate = (appInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;

        if (isSystem && !isUpdate) {
            // Cannot remove system apps unless we're uninstalling updates
            throw new RuntimeException("Cannot remove system apps unless we're uninstalling updates");
        }

        int messageId;
        if (isUpdate) {
            messageId = R.string.uninstall_update_confirm;
        } else {
            messageId = R.string.uninstall_confirm;
        }

        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final AlertDialog.Builder builder = new AlertDialog.Builder(theme);
        builder.setTitle(appInfo.loadLabel(pm));
        builder.setIcon(appInfo.loadIcon(pm));
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent data = new Intent();
                        data.putExtra(Installer.EXTRA_APK, apk);
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });
        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
        builder.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setResult(Activity.RESULT_CANCELED);
                        finish();
                    }
                });
        builder.setMessage(messageId);
        builder.create().show();
    }
}
