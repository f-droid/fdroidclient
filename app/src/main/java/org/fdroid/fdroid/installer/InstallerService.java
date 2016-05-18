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

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.PatternMatcher;
import android.os.Process;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import org.fdroid.fdroid.Utils;

import java.io.File;

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
            String packageName = intent.getStringExtra(InstallHelper.EXTRA_UNINSTALL_PACKAGE_NAME);

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
        switch (intent.getAction()) {
            case ACTION_INSTALL: {
                Uri uri = intent.getData();
                Uri originatingUri = intent.getParcelableExtra(InstallHelper.EXTRA_ORIGINATING_URI);
                sendBroadcastInstall(uri, originatingUri, InstallHelper.ACTION_INSTALL_STARTED);

                Utils.debugLog(TAG, "ACTION_INSTALL uri: " + uri + " file: " + new File(uri.getPath()));

                // TODO: rework for uri
                Uri sanitizedUri = null;
                try {
                    File file = InstallHelper.preparePackage(this, new File(uri.getPath()), null,
                            originatingUri.toString());
                    sanitizedUri = Uri.fromFile(file);
                } catch (Installer.InstallFailedException e) {
                    e.printStackTrace();
                }
                Utils.debugLog(TAG, "ACTION_INSTALL sanitizedUri: " + sanitizedUri);


                Intent installIntent = new Intent(this, AndroidInstallerActivity.class);
                installIntent.setAction(AndroidInstallerActivity.ACTION_INSTALL_PACKAGE);
                installIntent.putExtra(AndroidInstallerActivity.EXTRA_ORIGINATING_URI, originatingUri);
                installIntent.setData(sanitizedUri);
                PendingIntent installPendingIntent = PendingIntent.getActivity(this.getApplicationContext(),
                        uri.hashCode(),
                        installIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                sendBroadcastInstall(uri, originatingUri, InstallHelper.ACTION_INSTALL_USER_INTERACTION,
                        installPendingIntent);

                break;
            }

            case ACTION_UNINSTALL: {
                String packageName =
                        intent.getStringExtra(InstallHelper.EXTRA_UNINSTALL_PACKAGE_NAME);
                sendBroadcastUninstall(packageName, InstallHelper.ACTION_UNINSTALL_STARTED);


                Intent installIntent = new Intent(this, AndroidInstallerActivity.class);
                installIntent.setAction(AndroidInstallerActivity.ACTION_UNINSTALL_PACKAGE);
                installIntent.putExtra(
                        AndroidInstallerActivity.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
                PendingIntent uninstallPendingIntent = PendingIntent.getActivity(this.getApplicationContext(),
                        packageName.hashCode(),
                        installIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                sendBroadcastUninstall(packageName, InstallHelper.ACTION_UNINSTALL_USER_INTERACTION,
                        uninstallPendingIntent);


                break;
            }
        }
    }

    private void sendBroadcastInstall(Uri uri, Uri originatingUri, String action,
                                      PendingIntent pendingIntent) {
        sendBroadcastInstall(uri, originatingUri, action, pendingIntent, null);
    }

    private void sendBroadcastInstall(Uri uri, Uri originatingUri, String action) {
        sendBroadcastInstall(uri, originatingUri, action, null, null);
    }

    private void sendBroadcastInstall(Uri uri, Uri originatingUri, String action,
                                      PendingIntent pendingIntent, String errorMessage) {
        Intent intent = new Intent(action);
        intent.setData(uri);
        intent.putExtra(InstallHelper.EXTRA_ORIGINATING_URI, originatingUri);
        intent.putExtra(InstallHelper.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(InstallHelper.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    private void sendBroadcastUninstall(String packageName, String action) {
        sendBroadcastUninstall(packageName, action, null, null);
    }

    private void sendBroadcastUninstall(String packageName, String action,
                                        PendingIntent pendingIntent) {
        sendBroadcastUninstall(packageName, action, pendingIntent, null);
    }

    private void sendBroadcastUninstall(String packageName, String action,
                                        PendingIntent pendingIntent, String errorMessage) {
        Uri uri = Uri.fromParts("package", packageName, null);

        Intent intent = new Intent(action);
        intent.setData(uri); // for broadcast filtering
        intent.putExtra(InstallHelper.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
        intent.putExtra(InstallHelper.EXTRA_USER_INTERACTION_PI, pendingIntent);
        if (!TextUtils.isEmpty(errorMessage)) {
            intent.putExtra(InstallHelper.EXTRA_ERROR_MESSAGE, errorMessage);
        }
        localBroadcastManager.sendBroadcast(intent);
    }

    public static void install(Context context, Uri uri, Uri originatingUri) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_INSTALL);
        intent.setData(uri);
        intent.putExtra(InstallHelper.EXTRA_ORIGINATING_URI, originatingUri);
        context.startService(intent);
    }

    public static void uninstall(Context context, String packageName) {
        Intent intent = new Intent(context, InstallerService.class);
        intent.setAction(ACTION_UNINSTALL);
        intent.putExtra(InstallHelper.EXTRA_UNINSTALL_PACKAGE_NAME, packageName);
        context.startService(intent);
    }

    public static IntentFilter getInstallIntentFilter(Uri uri) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InstallHelper.ACTION_INSTALL_STARTED);
        intentFilter.addAction(InstallHelper.ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(InstallHelper.ACTION_INSTALL_INTERRUPTED);
        intentFilter.addAction(InstallHelper.ACTION_INSTALL_USER_INTERACTION);
        intentFilter.addDataScheme(uri.getScheme());
        intentFilter.addDataPath(uri.getPath(), PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }

    public static IntentFilter getUninstallIntentFilter(String packageName) {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(InstallHelper.ACTION_UNINSTALL_STARTED);
        intentFilter.addAction(InstallHelper.ACTION_UNINSTALL_COMPLETE);
        intentFilter.addAction(InstallHelper.ACTION_UNINSTALL_INTERRUPTED);
        intentFilter.addAction(InstallHelper.ACTION_UNINSTALL_USER_INTERACTION);
        intentFilter.addDataScheme("package");
        intentFilter.addDataPath(packageName, PatternMatcher.PATTERN_LITERAL);
        return intentFilter;
    }
}
