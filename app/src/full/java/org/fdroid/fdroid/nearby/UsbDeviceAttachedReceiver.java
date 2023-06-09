/*
 * Copyright (C) 2018-2019 Hans-Christoph Steiner <hans@eds.org>
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

package org.fdroid.fdroid.nearby;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.UriPermission;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.views.main.NearbyViewBinder;


/**
 * This is just a shim to receive {@link UsbManager#ACTION_USB_ACCESSORY_ATTACHED}
 * events.
 */
public class UsbDeviceAttachedReceiver extends BroadcastReceiver {
    public static final String TAG = "UsbDeviceAttachedReceiv";


    @Override
    public void onReceive(final Context context, Intent intent) {

        if (intent == null || TextUtils.isEmpty(intent.getAction())
                || !UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            Log.i(TAG, "ignoring irrelevant intent: " + intent);
            return;
        }
        Log.i(TAG, "handling intent: " + intent);

        final ContentResolver contentResolver = context.getContentResolver();

        for (final UriPermission uriPermission : contentResolver.getPersistedUriPermissions()) {
            Uri uri = uriPermission.getUri();
            final ContentObserver contentObserver = new ContentObserver(new Handler()) {

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    NearbyViewBinder.updateUsbOtg(context);
                }
            };
            contentResolver.registerContentObserver(uri, true, contentObserver);
            UsbDeviceDetachedReceiver.contentObservers.put(uri, contentObserver);
        }
    }
}
