/*
 * Copyright (C) 2014  Peter Serwylo, peter@serwylo.com
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid.receiver;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.InstalledAppProvider;

public class PackageAddedReceiver extends PackageReceiver {

    private static final String TAG = "PackageAddedReceiver";

    @Override
    protected boolean toDiscard(Intent intent) {
        if (intent.hasExtra(Intent.EXTRA_REPLACING)) {
            Utils.debugLog(TAG, "Discarding since this PACKAGE_ADDED is just a PACKAGE_REPLACED");
            return true;
        }
        return false;
    }

    @Override
    protected void handle(Context context, String appId) {
        PackageInfo info = getPackageInfo(context, appId);
        if (info == null) {
            Utils.debugLog(TAG, "Could not get package info on '" + appId + "' - skipping.");
            return;
        }

        Utils.debugLog(TAG, "Inserting installed app info for '" + appId + "' (v" + info.versionCode + ")");

        Uri uri = InstalledAppProvider.getContentUri();
        ContentValues values = new ContentValues(4);
        values.put(InstalledAppProvider.DataColumns.APP_ID, appId);
        values.put(InstalledAppProvider.DataColumns.VERSION_CODE, info.versionCode);
        values.put(InstalledAppProvider.DataColumns.VERSION_NAME, info.versionName);
        values.put(InstalledAppProvider.DataColumns.APPLICATION_LABEL,
                InstalledAppProvider.getApplicationLabel(context, appId));
        context.getContentResolver().insert(uri, values);
    }

}
