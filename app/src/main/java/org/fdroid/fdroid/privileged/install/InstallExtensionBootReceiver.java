/*
 * Copyright (C) 2015 Dominik Schürmann <dominik@dominikschuermann.de>
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

package org.fdroid.fdroid.privileged.install;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import org.fdroid.fdroid.Preferences;

public class InstallExtensionBootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED) && Preferences.get().isPostPrivilegedInstall()) {
            Preferences.get().setPostPrivilegedInstall(false);

            Intent postInstall = new Intent(context.getApplicationContext(), InstallExtensionDialogActivity.class);
            postInstall.setAction(InstallExtensionDialogActivity.ACTION_POST_INSTALL);
            postInstall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(postInstall);
        }
    }
}
