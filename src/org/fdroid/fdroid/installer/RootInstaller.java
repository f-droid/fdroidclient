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

import java.io.File;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

/**
 * Installer using a root shell and "pm install", "pm uninstall" commands
 */
public class RootInstaller extends Installer {

    Shell.Interactive rootSession;

    public RootInstaller(Context context, PackageManager pm, InstallerCallback callback)
            throws AndroidNotCompatibleException {
        super(context, pm, callback);
    }

    @Override
    public void installPackage(final File apkFile) throws AndroidNotCompatibleException {
        super.installPackage(apkFile);

        Shell.Builder shellBuilder = new Shell.Builder()
                .useSU()
                .setWantSTDERR(true)
                .setWatchdogTimeout(5)
                .setMinimalLogging(true);

        rootSession = shellBuilder.open(new Shell.OnCommandResultListener() {

            // Callback to report whether the shell was successfully
            // started up
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                    // TODO
                    // wrong uid
                    // Shell.OnCommandResultListener.SHELL_WRONG_UID
                    // exec failed
                    // Shell.OnCommandResultListener.SHELL_EXEC_FAILED

                    // reportError("Error opening root shell: exitCode " +
                    // exitCode);
                } else {
                    // Shell is up: send our first request
                    sendInstallCommand(apkFile);
                }
            }
        });
    }

    @Override
    public void deletePackage(final String packageName) throws AndroidNotCompatibleException {
        super.deletePackage(packageName);

        Shell.Builder shellBuilder = new Shell.Builder()
                .useSU()
                .setWantSTDERR(true)
                .setWatchdogTimeout(5)
                .setMinimalLogging(true);

        rootSession = shellBuilder.open(new Shell.OnCommandResultListener() {

            // Callback to report whether the shell was successfully
            // started up
            @Override
            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode != Shell.OnCommandResultListener.SHELL_RUNNING) {
                    // TODO
                    // wrong uid
                    // Shell.OnCommandResultListener.SHELL_WRONG_UID
                    // exec failed
                    // Shell.OnCommandResultListener.SHELL_EXEC_FAILED

                    // reportError("Error opening root shell: exitCode " +
                    // exitCode);
                } else {
                    // Shell is up: send our first request
                    sendDeleteCommand(packageName);
                }
            }
        });

    }

    @Override
    public boolean handleOnActivityResult(int requestCode, int resultCode, Intent data) {
        // no need to handle onActivityResult
        return false;
    }

    private void sendInstallCommand(File apkFile) {
        rootSession.addCommand("pm install -r " + apkFile.getAbsolutePath(), 0,
                new Shell.OnCommandResultListener() {
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        // close su shell
                        rootSession.close();

                        if (exitCode < 0) {
                            // reportError("Error executing commands: exitCode "
                            // + exitCode);
                            mCallback.onPackageInstalled(InstallerCallback.RETURN_CANCEL, true);
                        } else {
                            // wait until Android's internal PackageManger has
                            // received the new package state
                            Thread wait = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                    }

                                    mCallback.onPackageInstalled(InstallerCallback.RETURN_SUCCESS,
                                            true);
                                }
                            });
                            wait.start();
                        }
                    }
                });
    }

    private void sendDeleteCommand(String packageName) {
        rootSession.addCommand("pm uninstall " + packageName, 0,
                new Shell.OnCommandResultListener() {
                    public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                        // close su shell
                        rootSession.close();

                        if (exitCode < 0) {
                            // reportError("Error executing commands: exitCode "
                            // + exitCode);
                            mCallback.onPackageDeleted(InstallerCallback.RETURN_CANCEL, true);
                        } else {
                            // wait until Android's internal PackageManger has
                            // received the new package state
                            Thread wait = new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(2000);
                                    } catch (InterruptedException e) {
                                    }

                                    mCallback.onPackageDeleted(InstallerCallback.RETURN_SUCCESS,
                                            true);
                                }
                            });
                            wait.start();
                        }
                    }
                });
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
     * -r: reinstall an exisiting app, keeping its data.<br/>
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
