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

package org.fdroid.fdroid.privileged.install;

import android.content.Context;
import android.os.Build;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Partly based on
 * http://omerjerk.in/2014/08/how-to-install-an-app-to-system-partition/
 * https://github.com/omerjerk/RemoteDroid/blob/master/app/src/main/java/in/omerjerk/remotedroid/app/MainActivity.java
 */
abstract class InstallExtension {

    protected final Context context;

    private static final String APK_FILE_NAME = "FDroidPrivileged.apk";

    public InstallExtension(final Context context) {
        this.context = context;
    }

    public static InstallExtension create(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new LollipopImpl(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new KitKatToLollipopImpl(context);
        } else {
            return new PreKitKatImpl(context);
        }
    }

    final void runInstall(String apkPath) {
        onPreInstall();
        Shell.SU.run(getInstallCommands(apkPath));
    }

    final void runUninstall() {
        Shell.SU.run(getUninstallCommands());
    }

    protected abstract String getSystemFolder();

    protected void onPreInstall() {
        // To be overridden by relevant base class[es]
    }

    public String getWarningString() {
        return context.getString(R.string.system_install_warning);
    }

    public String getInstallingString() {
        return context.getString(R.string.system_install_installing);
    }

    protected String getInstallPath() {
        return getSystemFolder() + APK_FILE_NAME;
    }

    private List<String> getInstallCommands(String apkPath) {
        final List<String> commands = new ArrayList<>();
        commands.add("mount -o rw,remount /system"); // remount as read-write
        commands.addAll(getCopyToSystemCommands(apkPath));
        commands.add("mv " + getInstallPath() + ".tmp " + getInstallPath());
        commands.add("sleep 5"); // wait until the app is really installed
        commands.add("mount -o ro,remount /system"); // remount as read-only
        commands.add("am force-stop " + PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        commands.addAll(getPostInstallCommands());
        return commands;
    }

    protected List<String> getCopyToSystemCommands(String apkPath) {
        final List<String> commands = new ArrayList<>(2);
        commands.add("cat " + apkPath + " > " + getInstallPath() + ".tmp");
        commands.add("chmod 644 " + getInstallPath() + ".tmp");
        return commands;
    }

    protected List<String> getPostInstallCommands() {
        final List<String> commands = new ArrayList<>(1);
        commands.add("am start -n org.fdroid.fdroid/.privileged.install.InstallExtensionDialogActivity --ez "
                + InstallExtensionDialogActivity.ACTION_POST_INSTALL + " true");
        return commands;
    }

    private List<String> getUninstallCommands() {
        final List<String> commands = new ArrayList<>();
        commands.add("am force-stop " + PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        commands.add("pm clear " + PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
        commands.add("mount -o rw,remount /system");
        commands.addAll(getCleanUninstallCommands());
        commands.add("sleep 5");
        commands.add("mount -o ro,remount /system");
        commands.addAll(getPostUninstallCommands());
        return commands;
    }

    protected List<String> getCleanUninstallCommands() {
        final List<String> commands = new ArrayList<>(1);
        commands.add("rm -f " + getInstallPath());
        return commands;
    }

    protected List<String> getPostUninstallCommands() {
        return new ArrayList<>(0);
    }

    private static class PreKitKatImpl extends InstallExtension {

        public PreKitKatImpl(Context context) {
            super(context);
        }

        @Override
        protected String getSystemFolder() {
            return "/system/app/";
        }

    }

    private static class KitKatToLollipopImpl extends InstallExtension {

        public KitKatToLollipopImpl(Context context) {
            super(context);
        }

        /**
         * On KitKat, "Some system apps are more system than others"
         * https://github.com/android/platform_frameworks_base/commit/ccbf84f44c9e6a5ed3c08673614826bb237afc54
         */
        @Override
        protected String getSystemFolder() {
            return "/system/priv-app/";
        }

    }

    /**
     * History of PackageManagerService in Lollipop:
     * https://github.com/android/platform_frameworks_base/commits/lollipop-release/services/core/java/com/android/server/pm/PackageManagerService.java
     */
    private static class LollipopImpl extends InstallExtension {

        public LollipopImpl(Context context) {
            super(context);
        }

        @Override
        protected void onPreInstall() {
            // Setup preference to execute postInstall after reboot
            Preferences.get().setPostPrivilegedInstall(true);
        }

        public String getWarningString() {
            return context.getString(R.string.system_install_warning_lollipop);
        }

        public String getInstallingString() {
            return context.getString(R.string.system_install_installing_rebooting);
        }

        /**
         * Cluster-style layout where each app is placed in a unique directory
         */
        @Override
        protected String getSystemFolder() {
            return "/system/priv-app/FDroidPrivileged/";
        }

        /**
         * Create app directory
         */
        @Override
        protected List<String> getCopyToSystemCommands(String apkPath) {
            List<String> commands = new ArrayList<>(4);
            commands.add("mkdir -p " + getSystemFolder()); // create app directory if not existing
            commands.add("chmod 755 " + getSystemFolder());
            commands.add("cat " + apkPath + " > " + getInstallPath() + ".tmp");
            commands.add("chmod 644 " + getInstallPath() + ".tmp");
            return commands;
        }

        /**
         * NOTE: Only works with reboot
         * <p/>
         * File observers on /system/priv-app/ have been removed because they don't work with the new
         * cluser-style layout. See
         * https://github.com/android/platform_frameworks_base/commit/84e71d1d61c53cd947becc7879e05947be681103
         * <p/>
         * Related stack overflow post: http://stackoverflow.com/q/26487750
         */
        @Override
        protected List<String> getPostInstallCommands() {
            List<String> commands = new ArrayList<>(3);
            commands.add("am broadcast -a android.intent.action.ACTION_SHUTDOWN");
            commands.add("sleep 1");
            commands.add("reboot");
            return commands;
        }

        @Override
        protected List<String> getCleanUninstallCommands() {
            final List<String> commands = new ArrayList<>(1);
            commands.add("rm -rf " + getSystemFolder());
            return commands;
        }

        @Override
        protected List<String> getPostUninstallCommands() {
            List<String> commands = new ArrayList<>(3);
            commands.add("am broadcast -a android.intent.action.ACTION_SHUTDOWN");
            commands.add("sleep 1");
            commands.add("reboot");
            return commands;
        }

    }

}
