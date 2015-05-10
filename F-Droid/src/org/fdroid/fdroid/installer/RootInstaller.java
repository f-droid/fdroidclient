/*
 * Copyright (C) 2014 Dominik Schürmann <dominik@dominikschuermann.de>
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

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import eu.chainfire.libsuperuser.Shell;

/**
 * Installer using a root shell and "pm install", "pm uninstall" commands
 */
public class RootInstaller extends Installer {

    private static final String TAG = "RootInstaller";

    Shell.Interactive rootSession;

    public RootInstaller(Context context, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        super(context, pm, callback);
    }

    private Shell.Builder createShellBuilder() {
        return new Shell.Builder()
                .useSU()
                .setWantSTDERR(true)
                .setWatchdogTimeout(30)
                .setMinimalLogging(false);
    }

    @Override
    protected void installPackageInternal(final File apkFile) throws AndroidNotCompatibleException {
        rootSession = createShellBuilder().open(new Shell.OnCommandResultListener() {

            // Callback to report whether the shell was successfully
            // started up
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                    // NOTE: Additional exit codes:
                    // Shell.OnCommandResultListener.SHELL_WRONG_UID
                    // Shell.OnCommandResultListener.SHELL_EXEC_FAILED

                    Log.e(TAG, "Error opening root shell with exitCode " + exitCode);
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                            InstallerCallback.ERROR_CODE_OTHER);
                } else {
                    addInstallCommand(apkFile);
                }
            }
        });
    }

    @Override
    protected void installPackageInternal(final List<File> apkFiles)
            throws AndroidNotCompatibleException {
        rootSession = createShellBuilder().open(new Shell.OnCommandResultListener() {

            // Callback to report whether the shell was successfully
            // started up
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                    // NOTE: Additional exit codes:
                    // Shell.OnCommandResultListener.SHELL_WRONG_UID
                    // Shell.OnCommandResultListener.SHELL_EXEC_FAILED

                    Log.e(TAG, "Error opening root shell with exitCode " + exitCode);
                    mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                            InstallerCallback.ERROR_CODE_OTHER);
                } else {
                    addInstallCommand(apkFiles);
                }
            }
        });
    }

    @Override
    protected void deletePackageInternal(final String packageName)
            throws AndroidNotCompatibleException {
        rootSession = createShellBuilder().open(new Shell.OnCommandResultListener() {

            // Callback to report whether the shell was successfully
            // started up
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                    // NOTE: Additional exit codes:
                    // Shell.OnCommandResultListener.SHELL_WRONG_UID
                    // Shell.OnCommandResultListener.SHELL_EXEC_FAILED

                    Log.e(TAG, "Error opening root shell with exitCode " + exitCode);
                    mCallback.onError(InstallerCallback.OPERATION_DELETE,
                            InstallerCallback.ERROR_CODE_OTHER);
                } else {
                    addDeleteCommand(packageName);
                }
            }
        });

    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        // no need to handle onActivityResult
        return false;
    }

    private void addInstallCommand(File apkFile) {
        // Like package names, apk files should also only contain letters, numbers, dots, or underscore,
        // e.g., org.fdroid.fdroid_9.apk
        if (!isValidPackageName(apkFile.getName())) {
            Log.e(TAG, "File name is not valid (contains characters other than letters, numbers, dots, or underscore): "
                    + apkFile.getName());
            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                    InstallerCallback.ERROR_CODE_OTHER);
            return;
        }

        rootSession.addCommand("pm install -dr \"" + apkFile.getAbsolutePath() + "\"", 0,
                new Shell.OnCommandResultListener() {
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        // close su shell
                        rootSession.close();

                        if (exitCode < 0) {
                            Log.e(TAG, "Install failed with exit code " + exitCode);
                            mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        } else {
                            mCallback.onSuccess(InstallerCallback.OPERATION_INSTALL);
                        }
                    }
                });
    }

    private void addInstallCommand(List<File> apkFiles) {
        List<String> commands = new ArrayList<>();
        String pm = "pm install -dr ";
        for (File apkFile : apkFiles) {
            // see addInstallCommand()
            if (!isValidPackageName(apkFile.getName())) {
                Log.e(TAG, "File name is not valid (contains characters other than letters, numbers, dots, or underscore): "
                        + apkFile.getName());
                mCallback.onError(InstallerCallback.OPERATION_DELETE,
                        InstallerCallback.ERROR_CODE_OTHER);
                return;
            }
            commands.add(pm + "\"" + apkFile.getAbsolutePath() + "\"");
        }

        rootSession.addCommand(commands, 0,
                new Shell.OnCommandResultListener() {
                    public void onCommandResult(int commandCode, int exitCode,
                            List<String> output) {
                        // close su shell
                        rootSession.close();

                        if (exitCode < 0) {
                            Log.e(TAG, "Install failed with exit code " + exitCode);
                            mCallback.onError(InstallerCallback.OPERATION_INSTALL,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        } else {
                            mCallback.onSuccess(InstallerCallback.OPERATION_INSTALL);
                        }
                    }
                });

    }

    private void addDeleteCommand(String packageName) {
        if (!isValidPackageName(packageName)) {
            Log.e(TAG, "Package name is not valid (contains characters other than letters, numbers, dots, or underscore): "
                    + packageName);
            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                    InstallerCallback.ERROR_CODE_OTHER);
            return;
        }

        rootSession.addCommand("pm uninstall \"" + packageName + "\"", 0,
                new Shell.OnCommandResultListener() {
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        // close su shell
                        rootSession.close();

                        if (exitCode < 0) {
                            Log.e(TAG, "Delete failed with exit code " + exitCode);
                            mCallback.onError(InstallerCallback.OPERATION_DELETE,
                                    InstallerCallback.ERROR_CODE_OTHER);
                        } else {
                            mCallback.onSuccess(InstallerCallback.OPERATION_DELETE);
                        }
                    }
                });
    }

    @Override
    public boolean supportsUnattendedOperations() {
        return true;
    }

    private static final Pattern PACKAGE_NAME_BLACKLIST = Pattern.compile("[^a-zA-Z0-9\\.\\_]");

    /**
     * Package names should only contain letters, numbers, dots, and underscores!
     * Prevent injection attacks with app names like ";touch $'\057data\057injected'"
     *
     * @param packageName
     * @return
     */
    private boolean isValidPackageName(String packageName) {
        Matcher matcher = PACKAGE_NAME_BLACKLIST.matcher(packageName);
        return !matcher.find();
    }

    /**
     * pm install [-l] [-r] [-t] [-i INSTALLER_PACKAGE_NAME] [-s] [-f] [--algo
     * <algorithm name> --key <key-in-hex> --iv <IV-in-hex>] [--originating-uri
     * <URI>] [--referrer <URI>] PATH
     * <p/>
     * pm install: installs a package to the system.
     * <p/>
     * Options:<br/>
     * -l: install the package with FORWARD_LOCK.<br/>
     * -r: reinstall an existing app, keeping its data.<br/>
     * -t: allow test .apks to be installed.<br/>
     * -i: specify the installer package name.<br/>
     * -s: install package on sdcard.<br/>
     * -f: install package on internal flash.<br/>
     * -d: allow version code downgrade.<br/>
     * <p/>
     * pm uninstall [-k] PACKAGE
     * <p/>
     * pm uninstall: removes a package from the system.
     * <p/>
     * Options:<br/>
     * -k: keep the data and cache directories around after package removal.
     */

}
