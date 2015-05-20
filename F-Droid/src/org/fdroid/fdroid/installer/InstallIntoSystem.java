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

package org.fdroid.fdroid.installer;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Partly based on
 * http://omerjerk.in/2014/08/how-to-install-an-app-to-system-partition/
 * https://github.com/omerjerk/RemoteDroid/blob/master/app/src/main/java/in/omerjerk/remotedroid/app/MainActivity.java
 */
@TargetApi(Build.VERSION_CODES.FROYO)
abstract class InstallIntoSystem {

    protected final Context context;

    public InstallIntoSystem(final Context context) {
        this.context = context;
    }

    public static InstallIntoSystem create(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            return new LollipopImpl(context);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            return new KitKatToLollipopImpl(context);
        } else {
            return new PreKitKatImpl(context);
        }
    }

    protected abstract String getSystemFolder();

    protected void onPreInstall() {
        // To be overridden by relevant base class[es]
    }

    public String getWarningInfo() {
        return context.getString(R.string.system_install_question);
    }

    final void runUninstall() {
        final String[] commands = {
                "am force-stop org.fdroid.fdroid",
                "pm clear org.fdroid.fdroid",
                "mount -o rw,remount /system",
                "pm uninstall " + context.getPackageName(),
                "rm -f " + getInstallPath(),
                "sleep 5",
                "mount -o ro,remount /system"
        };
        Shell.SU.run(commands);
    }

    final void runInstall() {
        onPreInstall();
        Shell.SU.run(getInstallCommands());
    }

    protected String getInstallPath() {
        return getSystemFolder() + "FDroid.apk";
    }

    private List<String> getInstallCommands() {
        final List<String> commands = new ArrayList<>();
        commands.add("mount -o rw,remount /system");
        commands.addAll(getCopyToSystemCommands());
        commands.add("pm uninstall -k " + context.getPackageName()); // -k to retain data
        commands.add("mv " + getInstallPath() + ".tmp " + getInstallPath());
        commands.add("pm install -r " + getInstallPath());
        commands.add("sleep 5"); // wait until the app is really installed
        commands.add("mount -o ro,remount /system");
        commands.add("am force-stop org.fdroid.fdroid");
        commands.addAll(getPostInstallCommands());
        return commands;
    }

    protected List<String> getCopyToSystemCommands() {
        final List<String> commands = new ArrayList<>(2);
        commands.add("cat " + context.getPackageCodePath() + " > " + getInstallPath() + ".tmp");
        commands.add("chmod 655 " + getInstallPath() + ".tmp");
        return commands;
    }

    protected List<String> getPostInstallCommands() {
        final List<String> commands = new ArrayList<>(1);
        commands.add("am start -n org.fdroid.fdroid/.installer.InstallIntoSystemDialogActivity --ez post_install true");
        return commands;
    }

    private static class PreKitKatImpl extends InstallIntoSystem {

        public PreKitKatImpl(Context context) {
            super(context);
        }

        @Override
        protected String getSystemFolder() {
            return "/system/app";
        }

    }

    private static class KitKatToLollipopImpl extends InstallIntoSystem {

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
    private static class LollipopImpl extends InstallIntoSystem {

        public LollipopImpl(Context context) {
            super(context);
        }

        @Override
        protected void onPreInstall() {
            // Setup preference to execute postInstall after reboot
            Preferences.get().setPostSystemInstall(true);
        }

        public String getWarningInfo() {
            return context.getString(R.string.system_install_question_lollipop);
        }

        /**
         * Cluster-style layout where each app is placed in a unique directory
         */
        @Override
        protected String getSystemFolder() {
            return "/system/priv-app/FDroid/";
        }

        /**
         * Create app directory
         */
        @Override
        protected List<String> getCopyToSystemCommands() {
            List<String> commands = new ArrayList<>(3);
            commands.add("mkdir " + getSystemFolder()); // create app directory if not existing
            commands.add("cat " + context.getPackageCodePath() + " > " + getInstallPath() + ".tmp");
            commands.add("chmod 655 " + getInstallPath() + ".tmp");
            return commands;
        }

        /**
         * TODO: Currently only works with reboot
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

    }

}
