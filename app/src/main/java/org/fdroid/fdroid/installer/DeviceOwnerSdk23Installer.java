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

package org.fdroid.fdroid.installer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.privileged.views.InstallConfirmActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * For Android >= 6.0.
 * <p/>
 * https://github.com/android/platform_frameworks_base/blob/marshmallow-release/services/core/java/com/android/server/pm/PackageInstallerSession.java
 * https://github.com/googlesamples/android-testdpc/tree/master/app/src/main/java/com/afwsamples/testdpc/cosu
 * http://florent-dupont.blogspot.de/2015/02/10-things-to-know-about-device-owner.html
 */
@TargetApi(Build.VERSION_CODES.M)
public class DeviceOwnerSdk23Installer extends Installer {
    private final Activity mActivity;

    private static final String TAG = "Installer";

    public static final String ACTION_INSTALL_COMPLETE = "org.fdroid.fdroid.INSTALL_COMPLETE";
    public static final String ACTION_DELETE_COMPLETE = "org.fdroid.fdroid.DELETE_COMPLETE";

    private static final int REQUEST_CONFIRM_PERMS = 0;

    private static final String INSTALL_SESSION_NAME = "org.fdroid.fdroid";

    public DeviceOwnerSdk23Installer(Activity activity, PackageManager pm, InstallerCallback callback)
            throws InstallFailedException {
        super(activity, pm, callback);
        this.mActivity = activity;
    }

    PackageInstaller packageInstaller;

    @Override
    protected void installPackageInternal(File apkFile) throws InstallFailedException {

        Uri packageUri = Uri.fromFile(apkFile);
        int count = newPermissionCount(packageUri);
        if (count < 0) {
            mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                    InstallerCallback.ERROR_CODE_CANNOT_PARSE);
            return;
        }
        if (count > 0) {
            Intent intent = new Intent(mContext, InstallConfirmActivity.class);
            intent.setData(packageUri);
            mActivity.startActivityForResult(intent, REQUEST_CONFIRM_PERMS);
        } else {
            try {
                doInstallPackageInternal(apkFile);
            } catch (InstallFailedException e) {
                mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                        InstallerCallback.ERROR_CODE_OTHER);
            }
        }

    }

    private void doInstallPackageInternal(File apkFile) throws InstallFailedException {
        PackageManager pm = mContext.getPackageManager();
        packageInstaller = pm.getPackageInstaller();

        try {
            int id = createSession();
            writeSession(id, apkFile);
            commitSession(id);
        } catch (IOException e) {
            throw new InstallFailedException("trouble opening the file for writing, " +
                    "such as lack of disk space or unavailable media.");
        }
    }

    @Override
    protected void deletePackageInternal(String packageName) throws InstallFailedException {
        try {
            int id = createSession();
            delete(id, packageName);
            commitSession(id);
        } catch (IOException e) {
            throw new InstallFailedException("trouble opening the file for writing, " +
                    "such as lack of disk space or unavailable media.");
        }
    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        return true;
    }

    private int createSession() throws IOException {
        final PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO);

        int sessionId = packageInstaller.createSession(params);

        PackageInstaller.SessionCallback sessionCallback = new
                PackageInstaller.SessionCallback() {
                    @Override
                    public void onCreated(int i) {
                        Utils.debugLog(TAG, "SessionCallback#onCreate : " + i);
                    }

                    @Override
                    public void onBadgingChanged(int i) {
                        Utils.debugLog(TAG, "SessionCallback#onBadgingChanged : " + i);
                    }

                    @Override
                    public void onActiveChanged(int i, boolean b) {
                        Utils.debugLog(TAG, "SessionCallback#onActiveChanged : "
                                + i + ", boolean :" + b);
                    }

                    @Override
                    public void onProgressChanged(int i, float v) {
                        Utils.debugLog(TAG, "SessionCallback#onProgressChanged :" + i + ", " + v);
                    }

                    @Override
                    public void onFinished(int i, boolean b) {
                        Utils.debugLog(TAG, "SessionCallback#onFinished : " + i + ", boolean :" + b);
                    }
                };

        packageInstaller.registerSessionCallback(sessionCallback);

        return sessionId;
    }

    /**
     * @throws IOException if trouble opening the file for writing,
     *                     such as lack of disk space or unavailable media.
     */
    private void writeSession(final int sessionId, File apkFile) throws IOException {
        long sizeBytes = -1;

        if (apkFile.isFile()) {
            sizeBytes = apkFile.length();
        }
        Utils.debugLog(TAG, "apk size :" + sizeBytes);

        PackageInstaller.Session session = null;
        InputStream in = null;
        OutputStream out = null;
        try {
            session = packageInstaller.openSession(sessionId);
            in = new FileInputStream(apkFile);
            out = session.openWrite(INSTALL_SESSION_NAME, 0, sizeBytes);

            Utils.copy(in, out);
            session.fsync(out);
        } finally {
            Utils.closeQuietly(out);
            Utils.closeQuietly(in);
            Utils.closeQuietly(session);
        }
    }

    private void delete(final int sessionId, String packageName) throws IOException {
        PackageInstaller.Session session = null;

        try {
            session = packageInstaller.openSession(sessionId);
            packageInstaller.uninstall(packageName, createIntentSender(mContext, sessionId, ACTION_DELETE_COMPLETE));
        } finally {
            Utils.closeQuietly(session);
        }
    }

    private void commitSession(final int sessionId) throws IOException {
        PackageInstaller.Session session = null;
        try {
            session = packageInstaller.openSession(sessionId);

            session.commit(createIntentSender(mContext, sessionId, ACTION_INSTALL_COMPLETE));
        } finally {
            Utils.closeQuietly(session);
        }
    }

    private static IntentSender createIntentSender(Context context, int sessionId, String action) {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                new Intent(action),
                0);
        return pendingIntent.getIntentSender();
    }
}
