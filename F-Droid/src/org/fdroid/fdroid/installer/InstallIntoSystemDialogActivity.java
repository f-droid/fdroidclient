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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;

import java.util.ArrayList;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;

/**
 * Note: This activity has no view on its own, it displays consecutive dialogs.
 * <p/>
 * Partly based on https://github.com/omerjerk/RemoteDroid/blob/master/app/src/main/java/in/omerjerk/remotedroid/app/MainActivity.java
 * http://omerjerk.in/2014/08/how-to-install-an-app-to-system-partition/
 * <p/>
 * Info for lollipop:
 * http://stackoverflow.com/q/26487750
 * <p/>
 * Removed apk observers in
 * https://github.com/android/platform_frameworks_base/commit/84e71d1d61c53cd947becc7879e05947be681103
 * <p/>
 * History of PackageManagerService:
 * https://github.com/android/platform_frameworks_base/commits/lollipop-release/services/core/java/com/android/server/pm/PackageManagerService.java
 */
public class InstallIntoSystemDialogActivity extends FragmentActivity {

    private static final String TAG = "InstallIntoSystem";

    public static final String ACTION_INSTALL = "install";
    public static final String ACTION_UNINSTALL = "uninstall";
    public static final String ACTION_POST_INSTALL = "post_install";
    public static final String ACTION_FIRST_TIME = "first_time";

    String action;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        if (getIntent().getAction() == null) {
            Log.e(TAG, "Please define an action!");
            finish();
            return;
        }

        action = getIntent().getAction();
        if (ACTION_UNINSTALL.equals(action)) {
            uninstall();
        } else if (ACTION_INSTALL.equals(action)) {
            checkRootTask.execute();
        } else if (ACTION_FIRST_TIME.equals(action)) {
            Preferences.get().setFirstTime(false);
            checkRootTask.execute();
        } else if (ACTION_POST_INSTALL.equals(action)) {
            postInstall();
        }
    }

    /**
     * first time
     */
    private void firstTime() {
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        AlertDialog.Builder builder = new AlertDialog.Builder(theme);
        String message = getString(R.string.system_install_first_time_message) + "<br/><br/>" + InstallFDroidAsSystem.create(getApplicationContext()).getWarningInfo();
        builder.setMessage(Html.fromHtml(message));
        builder.setPositiveButton(R.string.system_permission_install_via_root, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                installTask.execute();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                InstallIntoSystemDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                InstallIntoSystemDialogActivity.this.finish();
            }
        });
        builder.create().show();
    }

    /**
     * 1. Check for root access
     */
    public AsyncTask<Void, Void, Boolean> checkRootTask = new AsyncTask<Void, Void, Boolean>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallIntoSystemDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            mProgressDialog = new ProgressDialog(theme);
            mProgressDialog.setMessage(getString(R.string.requesting_root_access_body));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return Shell.SU.available();
        }

        @Override
        protected void onPostExecute(Boolean rootGranted) {
            super.onPostExecute(rootGranted);

            mProgressDialog.dismiss();

            if (rootGranted) {
                // root access granted

                if (ACTION_UNINSTALL.equals(action)) {
                    uninstallTask.execute();
                } else if (ACTION_INSTALL.equals(action)) {
                    installTask.execute();
                } else if (ACTION_FIRST_TIME.equals(action)) {
                    firstTime();
                }
            } else {
                // root access denied

                if (!ACTION_FIRST_TIME.equals(action)) {
                    // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
                    ContextThemeWrapper theme = new ContextThemeWrapper(InstallIntoSystemDialogActivity.this,
                            FDroidApp.getCurThemeResId());

                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(theme);
                    alertBuilder.setTitle(R.string.root_access_denied_title);
                    alertBuilder.setMessage(getString(R.string.root_access_denied_body));
                    alertBuilder.setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            InstallIntoSystemDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                            InstallIntoSystemDialogActivity.this.finish();
                        }
                    });
                    alertBuilder.create().show();
                }
            }
        }
    };

    /**
     * 2. Install into system
     */
    AsyncTask<Void, Void, Void> installTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallIntoSystemDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            mProgressDialog = new ProgressDialog(theme);
            mProgressDialog.setMessage(getString(R.string.system_install_installing));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallFDroidAsSystem.create(getApplicationContext()).performInstall();
            return null;
        }
    };

    /**
     * 3. Verify that install worked
     */
    private void postInstall() {
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final boolean success = Installer.hasSystemPermissions(this, this.getPackageManager());

        // enable system installer on installation success
        Preferences.get().setSystemInstallerEnabled(success);

        AlertDialog.Builder builder = new AlertDialog.Builder(theme);
        builder.setTitle(success ? R.string.system_install_post_success : R.string.system_install_post_fail);
        builder.setMessage(success ? R.string.system_install_post_success_message : R.string.system_install_post_fail_message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                InstallIntoSystemDialogActivity.this.setResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
                InstallIntoSystemDialogActivity.this.finish();
                startActivity(new Intent(InstallIntoSystemDialogActivity.this, FDroid.class));
            }
        });
        builder.create().show();
    }

    private void uninstall() {
        // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final boolean systemApp = Installer.hasSystemPermissions(this, this.getPackageManager());

        if (systemApp) {
            AlertDialog.Builder builder = new AlertDialog.Builder(theme);
            builder.setTitle(R.string.system_uninstall);
            builder.setMessage(R.string.system_uninstall_message);
            builder.setPositiveButton(R.string.system_uninstall_button, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    checkRootTask.execute();
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InstallIntoSystemDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                    InstallIntoSystemDialogActivity.this.finish();
                }
            });
            builder.create().show();
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(theme);
            builder.setTitle(R.string.system_permission_denied_title);
            builder.setMessage(getString(R.string.system_permission_denied_body));
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InstallIntoSystemDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                    InstallIntoSystemDialogActivity.this.finish();
                }
            });
            builder.create().show();
        }
    }

    AsyncTask<Void, Void, Void> uninstallTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get holo design (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallIntoSystemDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            mProgressDialog = new ProgressDialog(theme);
            mProgressDialog.setMessage(getString(R.string.system_install_uninstalling));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallFDroidAsSystem.create(getApplicationContext()).performUninstall();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);

            mProgressDialog.dismiss();

            // app is uninstalled but still display, kill it!
            System.exit(0);
        }
    };

}

