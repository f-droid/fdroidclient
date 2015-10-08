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
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.text.Html;
import android.util.Log;
import android.view.ContextThemeWrapper;

import org.fdroid.fdroid.AppDetails;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.installer.PrivilegedInstaller;

import eu.chainfire.libsuperuser.Shell;

/**
 * Note: This activity has no view on its own, it displays consecutive dialogs.
 */
public class InstallExtensionDialogActivity extends FragmentActivity {

    private static final String TAG = "InstallIntoSystem";

    public static final String ACTION_INSTALL = "install";
    public static final String EXTRA_INSTALL_APK = "apk_file";

    public static final String ACTION_UNINSTALL = "uninstall";
    public static final String ACTION_POST_INSTALL = "post_install";
    public static final String ACTION_FIRST_TIME = "first_time";

    String apkFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // this activity itself has no content view (see manifest)

        if (getIntent().getAction() == null) {
            Log.e(TAG, "Please define an action!");
            finish();
            return;
        }

        apkFile = getIntent().getStringExtra(EXTRA_INSTALL_APK);

        switch (getIntent().getAction()) {
            case ACTION_UNINSTALL:
                uninstall();
                break;
            case ACTION_INSTALL:
                askBeforeInstall();
                break;
            case ACTION_FIRST_TIME:
                checkRootTask.execute();
                break;
            case ACTION_POST_INSTALL:
                postInstall();
                break;
        }
    }

    public static void firstTime(final Context context) {
        if (Preferences.get().isFirstTime()) {
            Preferences.get().setFirstTime(false);

            int isInstalledCorrectly = PrivilegedInstaller.isExtensionInstalledCorrectly(context);
            switch (isInstalledCorrectly) {
                case PrivilegedInstaller.IS_EXTENSION_INSTALLED_YES:
                    Preferences.get().setPrivilegedInstallerEnabled(true);
                    break;

                case PrivilegedInstaller.IS_EXTENSION_INSTALLED_NO:
                    runFirstTime(context);
                    break;

                case PrivilegedInstaller.IS_EXTENSION_INSTALLED_PERMISSIONS_PROBLEM:
                case PrivilegedInstaller.IS_EXTENSION_INSTALLED_SIGNATURE_PROBLEM:
                default:
                    // do nothing
            }
        }
    }

    public static void runFirstTime(final Context context) {
        // don't do a "real" root access, just look up if "su" is present and has a version!
        // a real check would annoy the user
        AsyncTask<Void, Void, Boolean> checkRoot = new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return Shell.SU.version(true) != null;
            }

            @Override
            protected void onPostExecute(Boolean probablyRoot) {
                super.onPostExecute(probablyRoot);

                // TODO: remove false condition once the install into system
                // process is stable - #294, #346, #347, #348
                if (false && probablyRoot) {
                    // looks like we have root, at least su has a version number and is present

                    Intent installIntent = new Intent(context, InstallExtensionDialogActivity.class);
                    installIntent.setAction(InstallExtensionDialogActivity.ACTION_FIRST_TIME);
                    installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    PendingIntent resultPendingIntent =
                            PendingIntent.getActivity(
                                    context,
                                    0,
                                    installIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT
                            );

                    NotificationCompat.Builder builder =
                            new NotificationCompat.Builder(context)
                                    .setContentIntent(resultPendingIntent)
                                    .setSmallIcon(R.drawable.ic_stat_notify)
                                    .setContentTitle(context.getString(R.string.system_install_first_time_notification))
                                    .setContentText(context.getString(R.string.system_install_first_time_notification_message_short))
                                    .setDefaults(Notification.DEFAULT_LIGHTS)
                                    .setAutoCancel(true)
                                    /*
                                     * Sets the big view "big text" style and supplies the
                                     * text (the user's reminder message) that will be displayed
                                     * in the detail area of the expanded notification.
                                     * These calls are ignored by the support library for
                                     * pre-4.1 devices.
                                     */
                                    .setStyle(new NotificationCompat.BigTextStyle()
                                            .bigText(context.getString(R.string.system_install_first_time_notification_message)));

                    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                    nm.notify(42, builder.build());
                }
            }
        };
        checkRoot.execute();
    }

    /**
     * first time
     */
    private void firstTime() {
        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

        String message = getString(R.string.system_install_first_time_message) + "<br/><br/>"
                + getString(R.string.system_install_question);

        AlertDialog.Builder builder = new AlertDialog.Builder(theme)
                .setMessage(Html.fromHtml(message))
                .setPositiveButton(R.string.system_install_button_open, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Open details of F-Droid Privileged
                        Intent intent = new Intent(InstallExtensionDialogActivity.this, AppDetails.class);
                        intent.putExtra(AppDetails.EXTRA_APPID,
                                PrivilegedInstaller.PRIVILEGED_EXTENSION_PACKAGE_NAME);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        InstallExtensionDialogActivity.this.setResult(Activity.RESULT_CANCELED);
                        InstallExtensionDialogActivity.this.finish();
                    }
                });
        builder.create().show();
    }

    public void askBeforeInstall() {
        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

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
    public final AsyncTask<Void, Void, Boolean> checkRootTask = new AsyncTask<Void, Void, Boolean>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
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

                switch (getIntent().getAction()) {
                    case ACTION_UNINSTALL:
                        uninstallTask.execute();
                        break;
                    case ACTION_INSTALL:
                        installTask.execute();
                        break;
                    case ACTION_FIRST_TIME:
                        firstTime();
                        break;
                }
            } else {
                // root access denied

                if (!ACTION_FIRST_TIME.equals(getIntent().getAction())) {
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
        }
    };

    /**
     * 2. Install into system
     */
    final AsyncTask<Void, Void, Void> installTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            mProgressDialog = new ProgressDialog(theme);
            mProgressDialog.setMessage(InstallExtension.create(getApplicationContext()).getInstallingString());
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallExtension.create(getApplicationContext()).runInstall(apkFile);
            return null;
        }
    };

    /**
     * 3. Verify that install worked
     */
    private void postInstall() {
        // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
        ContextThemeWrapper theme = new ContextThemeWrapper(this, FDroidApp.getCurThemeResId());

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

                // enable system installer on installation success
                Preferences.get().setPrivilegedInstallerEnabled(true);
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
            case PrivilegedInstaller.IS_EXTENSION_INSTALLED_PERMISSIONS_PROBLEM:
                title = getString(R.string.system_install_post_fail);
                message = getString(R.string.system_install_post_fail_message) +
                        "\n\n" + getString(R.string.system_install_denied_permissions);
                result = Activity.RESULT_CANCELED;
                break;
            default:
                throw new RuntimeException("unhandled return");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(theme)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        InstallExtensionDialogActivity.this.setResult(result);
                        InstallExtensionDialogActivity.this.finish();
                        startActivity(new Intent(InstallExtensionDialogActivity.this, FDroid.class));
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

    final AsyncTask<Void, Void, Void> uninstallTask = new AsyncTask<Void, Void, Void>() {
        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            // hack to get theme applied (which is not automatically applied due to activity's Theme.NoDisplay
            ContextThemeWrapper theme = new ContextThemeWrapper(InstallExtensionDialogActivity.this,
                    FDroidApp.getCurThemeResId());

            mProgressDialog = new ProgressDialog(theme);
            mProgressDialog.setMessage(getString(R.string.system_install_uninstalling));
            mProgressDialog.setIndeterminate(true);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            InstallExtension.create(getApplicationContext()).runUninstall();
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

