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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.UriPermission;
import android.database.ContentObserver;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;
import android.util.Log;
import org.fdroid.fdroid.views.main.MainActivity;
import org.fdroid.fdroid.views.main.NearbyViewBinder;

import java.util.HashMap;

/**
 * This is just a shim to receive {@link UsbManager#ACTION_USB_ACCESSORY_ATTACHED}
 * events then open up the right screen in {@link MainActivity}.
 */
public class UsbDeviceAttachedActivity extends Activity {
    public static final String TAG = "UsbDeviceAttachedActivi";

    private static final HashMap<Uri, ContentObserver> contentObservers = new HashMap<>();

    @RequiresApi(api = 19)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT < 19) {
            finish();
            return;
        }

        Intent intent = getIntent();
        if (intent == null || TextUtils.isEmpty(intent.getAction())
                || !UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
            Log.i(TAG, "ignoring irrelevant intent: " + intent);
            finish();
            return;
        }
        Log.i(TAG, "handling intent: " + intent);

        final ContentResolver contentResolver = getContentResolver();
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!UsbManager.ACTION_USB_DEVICE_DETACHED.equals(intent.getAction())) {
                    return;
                }
                NearbyViewBinder.updateUsbOtg(UsbDeviceAttachedActivity.this);
                unregisterReceiver(this);
                for (ContentObserver contentObserver : contentObservers.values()) {
                    contentResolver.unregisterContentObserver(contentObserver);
                }
            }
        };
        registerReceiver(receiver, new IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED));

        for (final UriPermission uriPermission : contentResolver.getPersistedUriPermissions()) {
            Uri uri = uriPermission.getUri();
            final ContentObserver contentObserver = new ContentObserver(new Handler()) {

                @Override
                public void onChange(boolean selfChange, Uri uri) {
                    NearbyViewBinder.updateUsbOtg(UsbDeviceAttachedActivity.this);
                }
            };
            contentResolver.registerContentObserver(uri, true, contentObserver);
        }
        intent.setComponent(new ComponentName(this, MainActivity.class));
        intent.putExtra(MainActivity.EXTRA_VIEW_NEARBY, true);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void finish() {
        setResult(RESULT_OK);
        super.finish();
    }
}
