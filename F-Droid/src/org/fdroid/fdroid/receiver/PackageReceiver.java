/*
 * Copyright (C) 2014  Ciaran Gultnieks, ciaran@ciarang.com,
 * Peter Serwylo, peter@serwylo.com
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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.util.Log;

import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.AppProvider;

abstract class PackageReceiver extends BroadcastReceiver {

    private static final String TAG = "PackageReceiver";

    abstract protected boolean toDiscard(Intent intent);
    abstract protected void handle(Context context, String appId);

    protected PackageInfo getPackageInfo(Context context, String appId) {
        for (PackageInfo info : context.getPackageManager().getInstalledPackages(0)) {
            if (info.packageName.equals(appId)) {
                return info;
            }
        }
        return null;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "PackageReceiver received [action = '" + intent.getAction() + "', data = '" + intent.getData() + "']");
        if (toDiscard(intent)) {
            return;
        }
        String appId = intent.getData().getSchemeSpecificPart();
        handle(context, appId);
        context.getContentResolver().notifyChange(AppProvider.getContentUri(appId), null);
        context.getContentResolver().notifyChange(ApkProvider.getAppUri(appId), null);
    }

}
