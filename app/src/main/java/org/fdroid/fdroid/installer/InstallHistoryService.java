/*
 * Copyright (C) 2016 Blue Jay Wireless
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

package org.fdroid.fdroid.installer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Process;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves all activity of installs and uninstalls to a file for later use, like
 * displaying in some kind of history viewer or reporting to a "popularity contest"
 * app tracker.
 */
public class InstallHistoryService extends JobIntentService {
    public static final String TAG = "InstallHistoryService";
    private static final int JOB_ID = TAG.hashCode();

    public static final Uri LOG_URI = Uri.parse("content://" + Installer.AUTHORITY + "/install_history/all");

    private static BroadcastReceiver broadcastReceiver;

    public static void register(Context context) {
        if (broadcastReceiver != null) {
            return;  // already registered
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addDataScheme("http");
        intentFilter.addDataScheme("https");
        intentFilter.addDataScheme("package");
        intentFilter.addAction(Installer.ACTION_INSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_INSTALL_INTERRUPTED);
        intentFilter.addAction(Installer.ACTION_INSTALL_USER_INTERACTION);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_STARTED);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_COMPLETE);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_INTERRUPTED);
        intentFilter.addAction(Installer.ACTION_UNINSTALL_USER_INTERACTION);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                queue(context, intent);
            }
        };
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.registerReceiver(broadcastReceiver, intentFilter);
    }

    public static void unregister(Context context) {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
        localBroadcastManager.unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
    }

    private static void queue(Context context, Intent intent) {
        Utils.debugLog(TAG, "queue " + intent);
        intent.setClass(context, InstallHistoryService.class);
        JobIntentService.enqueueWork(context, InstallHistoryService.class, JOB_ID, intent);
    }

    public static File getInstallHistoryFile(Context context) {
        File installHistoryDir = new File(context.getCacheDir(), "install_history");
        installHistoryDir.mkdir();
        return new File(installHistoryDir, "all");
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Utils.debugLog(TAG, "onHandleIntent " + intent);

        Process.setThreadPriority(Process.THREAD_PRIORITY_LOWEST);
        long timestamp = System.currentTimeMillis();
        Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
        String packageName = apk.packageName;
        long versionCode = apk.versionCode;

        List<String> values = new ArrayList<>(4);
        values.add(String.valueOf(timestamp));
        values.add(packageName);
        values.add(String.valueOf(versionCode));
        values.add(intent.getAction());

        File logFile = getInstallHistoryFile(this);
        FileWriter fw = null;
        PrintWriter out = null;
        try {
            fw = new FileWriter(logFile, true);
            out = new PrintWriter(fw);
            out.println(TextUtils.join(",", values));
        } catch (IOException e) {
            Utils.debugLog(TAG, e.getMessage());
        } finally {
            Utils.closeQuietly(out);
            Utils.closeQuietly(fw);
        }
    }
}
