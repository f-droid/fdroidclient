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

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.PrivilegedInstaller;
import org.fdroid.fdroid.views.main.MainActivity;

import java.io.File;

import eu.chainfire.libsuperuser.Shell;

/**
 * Note: This activity has no view on its own, it displays consecutive dialogs.
 */
public class InstallExtensionDialogActivity extends FragmentActivity {

    private static final String TAG = "InstallIntoSystem";

    public static final String ACTION_INSTALL = "install";

    public static final String ACTION_UNINSTALL = "uninstall";
    public static final String ACTION_POST_INSTALL = "post_install";

    private String apkPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        if (getIntent().getAction() == null) {
            Log.e(TAG, "Please define an action!");
            finish();
            return;
        }

        Uri dataUri = getIntent().getData();
        if (dataUri != null) {
            File apkFile = new File(dataUri.getPath());
            apkPath = apkFile.getAbsolutePath();
        }

        switch (getIntent().getAction()) {
            case ACTION_UNINSTALL:
                uninstall();
                break;
            case ACTION_INSTALL:
                askBeforeInstall();
                break;
            case ACTION_POST_INSTALL:
                postInstall();
                break;
        }
    }

    private void askBeforeInstall() {
        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        // not support on Android >= 5.1
        if (android.os.Build.VERSION.SDK_INT >= 22) {
            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(theme);
            alertBuilder.setMessage(R.string.system_install_not_supported);
            alertBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    InstallExtensionDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                    InstallExtensionDialogActivity.this.finish();
                }
            });
            alertBuilder.create().show();
            return;
        }

        AlertDialog.Builder alertBuilder = new AlertDialog.Builder(theme);
        alertBuilder.setTitle(R.string.system_install_question);
        String message = InstallExtension.create(getApplicationContext()).getWarningString();
        alertBuilder.setMessage(Html.fromHtml(message));
        alertBuilder.setPositiveButton(R.string.system_install_button_install, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                checkRootTask.execute();
            }
        });
        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                InstallExtensionDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                InstallExtensionDialogActivity.this.finish();
            }
        });
        alertBuilder.create().show();
    }

    /**
     * 1. Check for root access
     */
    private final AsyncTask<Void, Void, Boolean> checkRootTask = new AsyncTask<Void, Void, Boolean>() {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            progressDialog = new ProgressDialog(theme);
            progressDialog.setMessage(getString(R.string.requesting_root_access_body));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return Shell.SU.available();
        }

        @Override
        protected void onPostExecute(Boolean rootGranted) {
            super.onPostExecute(rootGranted);

            progressDialog.dismiss();

            if (rootGranted) {
                // root access granted

                switch (getIntent().getAction()) {
                    case ACTION_UNINSTALL:
                        uninstallTask.execute();
                        break;
                    case ACTION_INSTALL:
                        installTask.execute();
                        break;
                }
            } else {
                // root access denied
                // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
                ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                        FDroidApp.getCurThemeResId());

                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(theme)
                        .setTitle(R.string.root_access_denied_title)
                        .setMessage(getString(R.string.root_access_denied_body))
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                InstallExtensionDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                                InstallExtensionDialogActivity.this.finish();
                            }
                        });
                alertBuilder.create().show();
            }
        }
    };

    /**
     * 2. Install into system
     */
    private final AsyncTask<Void, Void, Void> installTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            progressDialog = new ProgressDialog(theme);
            progressDialog.setMessage(InstallExtension.create(getApplicationContext()).getInstallingString());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallExtension.create(getApplicationContext()).runInstall(apkPath);
            return null;
        }
    };

    /**
     * 3. Verify that install worked
     */
    private void postInstall() {
        int isInstalledCorrectly =
                PrivilegedInstaller.isExtensionInstalledCorrectly(this);

        String title;
        String message;
        final int result;
        switch (isInstalledCorrectly) {
            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES:
                title = getString(R.string.system_install_post_success);
                message = getString(R.string.system_install_post_success_message);
                result = Activity.RESULT_OK;
                break;
            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_NO:
                title = getString(R.string.system_install_post_fail);
                message = getString(R.string.system_install_post_fail_message);
                result = Activity.RESULT_CANCELED;
                break;
            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM:
                title = getString(R.string.system_install_post_fail);
                message = getString(R.string.system_install_post_fail_message) +
                        "\n\n" + getString(R.string.system_install_denied_signature);
                result = Activity.RESULT_CANCELED;
                break;
            default:
                throw new RuntimeException("unhandled return");
        }

        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        AlertDialog.Builder builder = new AlertDialog.Builder(theme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        InstallExtensionDialogActivity.this.setResult(result);
                        InstallExtensionDialogActivity.this.finish();
                        startActivity(new Intent(InstallExtensionDialogActivity.this, MainActivity.class));
                    }
                })
                .setCancelable(false);
        builder.create().show();
    }

    private void uninstall() {
        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        final boolean isInstalled = PrivilegedInstaller.isExtensionInstalled(this);

        if (isInstalled) {
            String message = InstallExtension.create(getApplicationContext()).getWarningString();

            AlertDialog.Builder builder = new AlertDialog.Builder(theme)
                    .setTitle(R.string.system_uninstall)
                    .setMessage(Html.fromHtml(message))
                    .setPositiveButton(R.string.system_uninstall_button, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            checkRootTask.execute();
                        }
                    })
                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            InstallExtensionDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                            InstallExtensionDialogActivity.this.finish();
                        }
                    });
            builder.create().show();
        } else {
            throw new RuntimeException("Uninstall invoked, but extension is not installed!");
        }
    }

    private final AsyncTask<Void, Void, Void> uninstallTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            progressDialog = new ProgressDialog(theme);
            progressDialog.setMessage(getString(R.string.uninstalling));
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallExtension.create(getApplicationContext()).runUninstall();
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);

            progressDialog.dismiss();

            // app is uninstalled but still display, kill it!
            System.exit(0);
        }
    };

}

