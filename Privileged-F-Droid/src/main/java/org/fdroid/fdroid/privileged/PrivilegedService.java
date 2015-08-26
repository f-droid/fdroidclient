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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * This service provides an API via AIDL IPC for the main F-Droid app to install/delete packages.
 * <p/>
 * Security:
 * Binding only works when,...
 * - packageName is "org.fdroid.fdroid"
 * - signature is equal or BuildConfig.DEBUG
 */
public class PrivilegedService extends Service {

    public static final String TAG = "PrivilegedFDroid";

    private static final String F_DROID_PACKAGE = "org.fdroid.fdroid";

    private Method mInstallMethod;
    private Method mDeleteMethod;

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
        public void installPackage(Uri packageURI, int flags, String installerPackageName,
                                   IPrivilegedCallback callback) {
            if (isAllowed()) {
                installPackageImpl(packageURI, flags, installerPackageName, callback);
            }
        }

        @Override
        public void deletePackage(String packageName, int flags, IPrivilegedCallback callback) {
            if (isAllowed()) {
                deletePackageImpl(packageName, flags, callback);
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private boolean isAllowed() {
        // Check that binding app is allowed to use this API
        try {

            barrierPackageName();
            barrierPackageCertificate();

        } catch (WrongPackageCertificateException e) {
            Log.e(TAG, "package certificate is not allowed!", e);
            return false;
        } catch (WrongPackageNameException e) {
            Log.e(TAG, "package name is not allowed!", e);
            return false;
        }

        return true;
    }

    /**
     * Checks if process that binds to this service (i.e. the package name corresponding to the
     * process) is allowed. Only returns when package name is allowed!
     *
     * @throws WrongPackageNameException
     */
    private void barrierPackageName() throws WrongPackageNameException {
        int uid = Binder.getCallingUid();
        String[] callingPackages = getPackageManager().getPackagesForUid(uid);

        // is calling package allowed to use this service?
        for (String currentPkg : callingPackages) {
            if (F_DROID_PACKAGE.equals(currentPkg)) {
                return;
            }
        }

        throw new WrongPackageNameException("package name is not allowed");
    }

    private void barrierPackageCertificate() throws WrongPackageCertificateException {
        String packageName = getCurrentCallingPackage();

        byte[] packageCertificate;
        try {
            packageCertificate = getPackageCertificate(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new WrongPackageCertificateException(e.getMessage());
        }

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new WrongPackageCertificateException("SHA-512 not available!");
        }
        byte[] hash = md.digest(packageCertificate);

        Log.d(TAG, "hash:" + getHex(hash));
        Log.d(TAG, "F_DROID_CERT_SHA512:" + BuildConfig.F_DROID_CERT_SHA512);

        if (getHex(hash).equals(BuildConfig.F_DROID_CERT_SHA512)
                || BuildConfig.DEBUG) {
            return;
        }

        throw new WrongPackageCertificateException("certificate not allowed!");
    }

    private byte[] getPackageCertificate(String packageName) throws PackageManager.NameNotFoundException {
        // we do check the byte array of *all* signatures
        @SuppressLint("PackageManagerGetSignatures")
        PackageInfo pkgInfo = getPackageManager().getPackageInfo(packageName, PackageManager.GET_SIGNATURES);

        // NOTE: Silly Android API naming: Signatures are actually certificates
        Signature[] certificates = pkgInfo.signatures;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Signature cert : certificates) {
            try {
                outputStream.write(cert.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Should not happen! Writing ByteArrayOutputStream to concat certificates failed");
            }
        }

        // Even if an apk has several certificates, these certificates should never change
        // Google Play does not allow the introduction of new certificates into an existing apk
        // Also see this attack: http://stackoverflow.com/a/10567852
        return outputStream.toByteArray();
    }

    /**
     * Returns package name associated with the UID, which is assigned to the process that sent you the
     * current transaction that is being processed :)
     *
     * @return package name
     */
    protected String getCurrentCallingPackage() {
        String[] callingPackages = getPackageManager().getPackagesForUid(Binder.getCallingUid());

        // NOTE: No support for sharedUserIds
        // callingPackages contains more than one entry when sharedUserId has been used
        // No plans to support sharedUserIds due to many bugs connected to them:
        // http://java-hamster.blogspot.de/2010/05/androids-shareduserid.html
        return callingPackages[0];
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // get internal methods via reflection
        try {
            Class<?>[] installTypes = {
                    Uri.class, IPackageInstallObserver.class, int.class,
                    String.class
            };
            Class<?>[] deleteTypes = {
                    String.class, IPackageDeleteObserver.class,
                    int.class
            };

            PackageManager pm = getPackageManager();
            mInstallMethod = pm.getClass().getMethod("installPackage", installTypes);
            mDeleteMethod = pm.getClass().getMethod("deletePackage", deleteTypes);
        } catch (NoSuchMethodException e) {
            Log.e(TAG, "Android not compatible!", e);
            stopSelf();
        }
    }

    private String getHex(byte[] byteData) {
        StringBuilder hexString = new StringBuilder();
        for (byte aByteData : byteData) {
            String hex = Integer.toHexString(0xff & aByteData);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public static class WrongPackageCertificateException extends Exception {
        private static final long serialVersionUID = -1294642703122196028L;

        public WrongPackageCertificateException(String message) {
            super(message);
        }
    }

    public static class WrongPackageNameException extends Exception {
        private static final long serialVersionUID = -2294642703111196028L;

        public WrongPackageNameException(String message) {
            super(message);
        }
    }

    public static class AndroidNotCompatibleException extends Exception {
        private static final long serialVersionUID = -3294642703111196028L;

        public AndroidNotCompatibleException(String message) {
            super(message);
        }
    }
}
