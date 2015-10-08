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

package org.fdroid.fdroid.privileged;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

/**
 * This service provides an API via AIDL IPC for the main F-Droid app to install/delete packages.
 */
public class PrivilegedService extends Service {

    private static final String TAG = "PrivilegedService";

    private Method mInstallMethod;
    private Method mDeleteMethod;

    private boolean hasPrivilegedPermissionsImpl() {
        boolean hasInstallPermission =
                getPackageManager().checkPermission(Manifest.permission.INSTALL_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;
        boolean hasDeletePermission =
                getPackageManager().checkPermission(Manifest.permission.DELETE_PACKAGES, getPackageName())
                        == PackageManager.PERMISSION_GRANTED;

        return hasInstallPermission && hasDeletePermission;
    }

    private void installPackageImpl(Uri packageURI, int flags, String installerPackageName,
                                    final IPrivilegedCallback callback) {

        // Internal callback from the system
        IPackageInstallObserver.Stub installObserver = new IPackageInstallObserver.Stub() {
            @Override
            public void packageInstalled(String packageName, int returnCode) throws RemoteException {
                // forward this internal callback to our callback
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e(TAG, "RemoteException", e1);
                }
            }
        };

        // execute internal method
        try {
            mInstallMethod.invoke(getPackageManager(), packageURI, installObserver,
                    flags, installerPackageName);
        } catch (Exception e) {
            Log.e(TAG, "Android not compatible!", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e(TAG, "RemoteException", e1);
            }
        }
    }

    private void deletePackageImpl(String packageName, int flags, final IPrivilegedCallback callback) {

        // Internal callback from the system
        IPackageDeleteObserver.Stub deleteObserver = new IPackageDeleteObserver.Stub() {
            @Override
            public void packageDeleted(String packageName, int returnCode) throws RemoteException {
                // forward this internal callback to our callback
                try {
                    callback.handleResult(packageName, returnCode);
                } catch (RemoteException e1) {
                    Log.e(TAG, "RemoteException", e1);
                }
            }
        };

        // execute internal method
        try {
            mDeleteMethod.invoke(getPackageManager(), packageName, deleteObserver, flags);
        } catch (Exception e) {
            Log.e(TAG, "Android not compatible!", e);
            try {
                callback.handleResult(null, 0);
            } catch (RemoteException e1) {
                Log.e(TAG, "RemoteException", e1);
            }
        }

    }

    private final IPrivilegedService.Stub mBinder = new IPrivilegedService.Stub() {
        @Override
        public boolean hasPrivilegedPermissions() {
            return hasPrivilegedPermissionsImpl();
        }

        @Override
        public void installPackage(Uri packageURI, int flags, String installerPackageName,
                                   IPrivilegedCallback callback) {
            installPackageImpl(packageURI, flags, installerPackageName, callback);
        }

        @Override
        public void deletePackage(String packageName, int flags, IPrivilegedCallback callback) {
            deletePackageImpl(packageName, flags, callback);
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // get internal methods via reflection
        try {
            Class<?>[] installTypes = {
                Uri.class, IPackageInstallObserver.class, int.class,
                String.class,
            };
            Class<?>[] deleteTypes = {
                String.class, IPackageDeleteObserver.class,
                int.class,
            };

            PackageManager pm = getPackageManager();
            mInstallMethod = pm.getClass().getMethod("installPackage", installTypes);
            mDeleteMethod = pm.getClass().getMethod("deletePackage", deleteTypes);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Android not compatible!", e);
            stopSelf();
        }
    }

}
