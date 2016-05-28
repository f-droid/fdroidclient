/*
 * Copyright (C) 2008 The Android Open Source Project
 * Copyright (C) 2016 Hans-Christoph Steiner
 * Copyright (C) 2016 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fdroid.fdroid.installer;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.fdroid.fdroid.Utils;

/**
 * InstallerService based on DownloaderService
 */
public class InstallerService extends Service {
    private static final String TAG = "InstallerService";

    private static final String ACTION_INSTALL = "org.fdroid.fdroid.installer.InstallerService.action.INSTALL";
    private static final String ACTION_UNINSTALL = "org.fdroid.fdroid.installer.InstallerService.action.UNINSTALL";

    private volatile Looper serviceLooper;
    private static volatile ServiceHandler serviceHandler;
    private LocalBroadcastManager localBroadcastManager;
    private Installer installer;

    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            Utils.debugLog(TAG, "Handling message with ID of " + msg.what);
            handleIntent((Intent) msg.obj);
            stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Utils.debugLog(TAG, "Creating installer service.");

        HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Utils.debugLog(TAG, "Received Intent for installing/uninstalling: " + intent + " (with a startId of " + startId + ")");

        if (ACTION_INSTALL.equals(intent.getAction())) {
            Uri uri = intent.getData();

            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            msg.what = uri.hashCode();
            serviceHandler.sendMessage(msg);
            Utils.debugLog(TAG, "Start install of " + uri.toString());
        } else if (ACTION_UNINSTALL.equals(intent.getAction())) {
            String packageName = intent.getStringExtra(Installer.EXTRA_PACKAGE_NAME);

            Message msg = serviceHandler.obtainMessage();
            msg.arg1 = startId;
            msg.obj = intent;
            msg.what = packageName.hashCode();
            serviceHandler.sendMessage(msg);
            Utils.debugLog(TAG, "Start uninstall of " + packageName);
        } else {
            Log.e(TAG, "Received Intent with unknown action: " + intent);
        }

        return START_REDELIVER_INTENT; // if killed before completion, retry Intent
    }

    @Override
    public void onDestroy() {
        Utils.debugLog(TAG, "Destroying installer service. Will move to background and stop our Looper.");
        serviceLooper.quit(); //NOPMD - this is copied from IntentService, no super call needed
    }

    /**
     * This service does not use binding, so no need to implement this method
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    protected void handleIntent(Intent intent) {
        String packageName = intent.getStringExtra(Installer.EXTRA_PACKAGE_NAME);
        installer = InstallerFactory.create(this, packageName);

        switch (intent.getAction()) {
            case ACTION_INSTALL: {
                Uri uri = intent.getData();
                Uri originatingUri = intent.getParcelableExtra(Installer.EXTRA_ORIGINATING_URI);

                installer.installPackage(uri, originatingUri, packageName);
                break;
            }

            case ACTION_UNINSTALL: {
                installer.uninstallPackage(packageName);
                break;
            }
        }
    }

    public static void install(Context context, Uri uri, Uri originatingUri, String packageName) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(uri);
        intent.putExtra(Installer.EXTRA_ORIGINATING_URI, originatingUri);
        intent.putExtra(Installer.EXTRA_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

    public static void uninstall(Context context, String packageName) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_UNINSTALL);
        intent.putExtra(Installer.EXTRA_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

}
