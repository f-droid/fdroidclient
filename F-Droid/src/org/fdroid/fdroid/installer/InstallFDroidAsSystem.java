package org.fdroid.fdroid.installer;

import android.content.Context;
import android.os.Build;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

abstract class InstallFDroidAsSystem {

    protected final Context context;

    public InstallFDroidAsSystem(final Context context) {
        this.context = context;
    }

    public static InstallFDroidAsSystem create(final Context context) {
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

    final void performUninstall() {
        final String[] commands = {
                "mount -o rw,remount /system",
                "pm uninstall " + context.getPackageName(),
                "rm -f " + installPath(),
                "sleep 5",
                "mount -o ro,remount /system"
        };
        Shell.SU.run(commands);
    }

    final void performInstall() {
        onPreInstall();
        Shell.SU.run(getCommands());
    }

    private String installPath() {
        return getSystemFolder() + "FDroid.apk";
    }

    private List<String> getCommands() {
        final List<String> commands = new ArrayList<>();
        commands.add(makePartitionWriteable());
        commands.add(copyApkToPartition());
        commands.add(uninstallFDroid());
        commands.addAll(reinstallFDroidAsSystem());
        commands.addAll(makePartitionReadOnly());
        return commands;
    }

    protected String makePartitionWriteable() {
        return "mount -o rw,remount /system";
    }

    protected String copyApkToPartition() {
        return "cat " + context.getPackageCodePath() + " > " + installPath() + ".tmp";
    }

    protected String uninstallFDroid() {
        return "pm uninstall -k " + context.getPackageName();
    }

    protected List<String> reinstallFDroidAsSystem() {
        final List<String> commands = new ArrayList<>(3);
        commands.add("mv " + installPath() + ".tmp " + installPath());
        commands.add("pm install -r " + installPath());
        commands.add("sleep 5");
        return commands;
    }

    protected List<String> makePartitionReadOnly() {
        final List<String> commands = new ArrayList<>(2);
        commands.add("mount -o ro,remount /system");
        commands.add("am start -n org.fdroid.fdroid/.installer.InstallIntoSystemDialogActivity --ez post_install true");
        return commands;
    }

    private static class PreKitKatImpl extends InstallFDroidAsSystem {

        public PreKitKatImpl(Context context) {
            super(context);
        }

        @Override
        protected String getSystemFolder() {
            return "/system/app";
        }

    }

    private static class KitKatToLollipopImpl extends InstallFDroidAsSystem {

        public KitKatToLollipopImpl(Context context) {
            super(context);
        }

        /**
         * New folder introduced in
         * https://github.com/android/platform_frameworks_base/commit/ccbf84f44c9e6a5ed3c08673614826bb237afc54
         */
        @Override
        protected String getSystemFolder() {
            return "/system/priv-app/";
        }

    }

    private static class LollipopImpl extends InstallFDroidAsSystem {

        public LollipopImpl(Context context) {
            super(context);
        }

        /**
         * New cluster based installation and app dirs
         */
        @Override
        protected String getSystemFolder() {
            return "/system/priv-app/FDroid/";
        }

        /**
         * TODO: Currently only works with reboot. Find a way how this could work without.
         * See http://stackoverflow.com/q/26487750
         */
        @Override
        protected List<String> makePartitionReadOnly() {
            List<String> commands = new ArrayList<>(3);
            commands.add("am broadcast -a android.intent.action.ACTION_SHUTDOWN");
            commands.add("sleep 1");
            commands.add("reboot");
            return commands;
        }

        protected void onPreInstall() {
            // Setup preference to execute postInstall after reboot
            Preferences.get().setPostSystemInstall(true);
        }

        public String getWarningInfo() {
            return context.getString(R.string.system_install_question_lollipop);
        }

    }

}
