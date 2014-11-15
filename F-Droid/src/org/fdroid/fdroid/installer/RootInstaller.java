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
import eu.chainfire.libsuperuser.Shell;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Installer using a root shell and "pm install", "pm uninstall" commands
 */
public class RootInstaller extends Installer {

    Shell.Interactive rootSession;

    public RootInstaller(Context context, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        super(context, pm, callback);
    }

    private Shell.Builder createShellBuilder() {
        Shell.Builder shellBuilder = new Shell.Builder()
                .useSU()
                .setWantSTDERR(true)
                .setWatchdogTimeout(30)
                .setMinimalLogging(false);

        return shellBuilder;
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
        rootSession.addCommand("pm install -r " + apkFile.getAbsolutePath(), 0,
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
        List<String> commands = new ArrayList<String>();
        String pm = "pm install -r ";
        for (File apkFile : apkFiles) {
            commands.add(pm + apkFile.getAbsolutePath());
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
        rootSession.addCommand("pm uninstall " + packageName, 0,
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
