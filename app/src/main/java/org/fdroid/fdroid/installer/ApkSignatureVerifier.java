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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import org.acra.ACRA;
import org.fdroid.fdroid.Utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * NOTE: Silly Android API naming: APK signatures are actually certificates!
 * Thus, we are comparing certificates (including the public key) used to sign an APK,
 * not the actual APK signature.
 */
class ApkSignatureVerifier {

    private static final String TAG = "ApkSignatureVerifier";

    private final Context context;
    private final PackageManager pm;

    ApkSignatureVerifier(Context context) {
        this.context = context;
        pm = context.getPackageManager();
    }

    public boolean hasFDroidSignature(File apkFile) {
        if (!apkFile.exists()) {
            ACRA.getErrorReporter().handleException(
                    new Exception("Failed to install Privileged Extension, because " + apkFile.getAbsolutePath()
                            + " does not exist."),
                    false
            );

            return false;
        }

        byte[] apkSig = getApkSignature(apkFile);
        byte[] fdroidSig = getFDroidSignature();

        if (Arrays.equals(apkSig, fdroidSig)) {
            return true;
        }

        Utils.debugLog(TAG, "Signature mismatch!");
        Utils.debugLog(TAG, "APK sig: " + Utils.toHexString(getApkSignature(apkFile)));
        Utils.debugLog(TAG, "F-Droid sig: " + Utils.toHexString(getFDroidSignature()));
        return false;
    }

    private byte[] getApkSignature(File apkFile) {
        final String pkgPath = apkFile.getAbsolutePath();
        if (!apkFile.exists()) {
            throw new IllegalArgumentException("Could not find APK at \"" + pkgPath
                    + "\" when checking for signature.");
        }

        PackageInfo pkgInfo = pm.getPackageArchiveInfo(pkgPath, PackageManager.GET_SIGNATURES);
        if (pkgInfo == null) {
            throw new IllegalArgumentException("Could not find PackageInfo for package at \"" + pkgPath + "\".");
        }

        return signatureToBytes(pkgInfo.signatures);
    }

    private byte[] getFDroidSignature() {
        try {
            // we do check the byte array of *all* signatures
            @SuppressLint("PackageManagerGetSignatures")
            PackageInfo pkgInfo = pm.getPackageInfo(context.getPackageName(),
                    PackageManager.GET_SIGNATURES);
            return signatureToBytes(pkgInfo.signatures);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Should not happen! F-Droid package not found!");
        }
    }

    private byte[] signatureToBytes(Signature[] signatures) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        for (Signature sig : signatures) {
            try {
                outputStream.write(sig.toByteArray());
            } catch (IOException e) {
                throw new RuntimeException("Should not happen! Concatenating signatures failed");
            }
        }

        return outputStream.toByteArray();
    }

}
