package org.fdroid.fdroid.views.apps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.util.Pair;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppPrefs;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;

import java.io.File;
import java.util.Iterator;

// TODO: Support cancelling of downloads by tapping the install button a second time.
public class AppListItemController extends RecyclerView.ViewHolder {

    private static final String TAG = "AppListItemController";

    private final Activity activity;

    @NonNull
    private final ImageView icon;

    @NonNull
    private final TextView name;

    @Nullable
    private final ImageView installButton;

    @Nullable
    private final TextView status;

    @Nullable
    private final TextView installedVersion;

    @Nullable
    private final TextView ignoredStatus;

    @Nullable
    private final ProgressBar progressBar;

    @Nullable
    private final ImageButton cancelButton;

    /**
     * Will operate as the "Download is complete, click to (install|update)" button, as well as the
     * "Installed successfully, click to run" button.
     */
    @Nullable
    private final Button actionButton;

    private final DisplayImageOptions displayImageOptions;

    @Nullable
    private App currentApp;

    @Nullable
    private AppUpdateStatusManager.AppUpdateStatus currentStatus;

    @TargetApi(21)
    public AppListItemController(final Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        installButton = (ImageView) itemView.findViewById(R.id.install);
        if (installButton != null) {
            installButton.setOnClickListener(onActionClicked);

            if (Build.VERSION.SDK_INT >= 21) {
                installButton.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        float density = activity.getResources().getDisplayMetrics().density;

                        // TODO: This is a bit hacky/hardcoded/too-specific to the particular icons we're using.
                        // This is because the default "download & install" and "downloaded & ready to install"
                        // icons are smaller than the "downloading progress" button. Hence, we can't just use
                        // the width/height of the view to calculate the outline size.
                        int xPadding = (int) (8 * density);
                        int yPadding = (int) (9 * density);
                        outline.setOval(xPadding, yPadding, installButton.getWidth() - xPadding, installButton.getHeight() - yPadding);
                    }
                });
            }
        }

        icon = (ImageView) itemView.findViewById(R.id.icon);
        name = (TextView) itemView.findViewById(R.id.app_name);
        status = (TextView) itemView.findViewById(R.id.status);
        installedVersion = (TextView) itemView.findViewById(R.id.installed_version);
        ignoredStatus = (TextView) itemView.findViewById(R.id.ignored_status);
        progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
        cancelButton = (ImageButton) itemView.findViewById(R.id.cancel_button);
        actionButton = (Button) itemView.findViewById(R.id.action_button);

        if (actionButton != null) {
            actionButton.setOnClickListener(onActionClicked);
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(onCancelDownload);
        }

        displayImageOptions = Utils.getImageLoadingOptions().build();

        itemView.setOnClickListener(onAppClicked);
    }

    /**
     * Figures out the current install/update/download/etc status for the app we are viewing.
     * Then, asks the view to update itself to reflect this status.
     */
    private void refreshStatus(@NonNull App app) {
        Iterator<AppUpdateStatusManager.AppUpdateStatus> statuses = AppUpdateStatusManager.getInstance(activity).getByPackageName(app.packageName).iterator();
        if (statuses.hasNext()) {
            AppUpdateStatusManager.AppUpdateStatus status = statuses.next();
            updateAppStatus(app, status);
        } else {
            currentStatus = null;
        }
    }

    public void bindModel(@NonNull App app) {
        currentApp = app;

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, displayImageOptions);

        refreshStatus(app);

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
        broadcastManager.unregisterReceiver(onInstallAction);
        broadcastManager.unregisterReceiver(onStatusChanged);

        // broadcastManager.registerReceiver(onInstallAction, Installer.getInstallIntentFilter(Uri.parse(currentAppDownloadUrl)));


        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        broadcastManager.registerReceiver(onStatusChanged, intentFilter);

        configureAppName(app);
        configureStatusText(app);
        configureInstalledVersion(app);
        configureIgnoredStatus(app);
        configureInstallButton(app);
        configureActionButton(app);
    }

    /**
     * Sets the text/visibility of the {@link R.id#status} {@link TextView} based on whether the app:
     *  * Is compatible with the users device
     *  * Is installed
     *  * Can be updated
     */
    private void configureStatusText(@NonNull App app) {
        if (status == null) {
            return;
        }

        if (!app.compatible) {
            status.setText(activity.getString(R.string.app_incompatible));
            status.setVisibility(View.VISIBLE);
        } else if (app.isInstalled()) {
            if (app.canAndWantToUpdate(activity)) {
                String upgradeFromTo = activity.getString(R.string.app_version_x_available, app.getSuggestedVersionName());
                status.setText(upgradeFromTo);
            } else {
                String installed = activity.getString(R.string.app_version_x_installed, app.installedVersionName);
                status.setText(installed);
            }

            status.setVisibility(View.VISIBLE);
        } else {
            status.setVisibility(View.INVISIBLE);
        }

    }

    /**
     * Shows the currently installed version name, and whether or not it is the recommended version.
     * Binds to the {@link R.id#installed_version} {@link TextView}.
     */
    private void configureInstalledVersion(@NonNull App app) {
        if (installedVersion == null) {
            return;
        }

        int res = (app.suggestedVersionCode == app.installedVersionCode)
                ? R.string.app_recommended_version_installed : R.string.app_version_x_installed;

        installedVersion.setText(activity.getString(res, app.installedVersionName));
    }

    /**
     * Shows whether the user has previously asked to ignore updates for this app entirely, or for a
     * specific version of this app. Binds to the {@link R.id#ignored_status} {@link TextView}.
     */
    private void configureIgnoredStatus(@NonNull App app) {
        if (ignoredStatus == null) {
            return;
        }

        AppPrefs prefs = app.getPrefs(activity);
        if (prefs.ignoreAllUpdates) {
            ignoredStatus.setText(activity.getString(R.string.installed_app__updates_ignored));
            ignoredStatus.setVisibility(View.VISIBLE);
        } else if (prefs.ignoreThisUpdate > 0 && prefs.ignoreThisUpdate == app.suggestedVersionCode) {
            ignoredStatus.setText(activity.getString(R.string.installed_app__updates_ignored_for_suggested_version, app.getSuggestedVersionName()));
            ignoredStatus.setVisibility(View.VISIBLE);
        } else {
            ignoredStatus.setVisibility(View.GONE);
        }
    }

    /**
     * Queries the {@link AppUpdateStatusManager} to find out if there are any apks corresponding to
     * `app` which are ready to install.
     */
    private boolean isReadyToInstall(@NonNull App app) {
        for (AppUpdateStatusManager.AppUpdateStatus appStatus : AppUpdateStatusManager.getInstance(activity).getByPackageName(app.packageName)) {
            if (appStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                return true;
            }
        }
        return false;
    }

    /**
     * The app name {@link TextView} is used for a few reasons:
     * <li> Display name + summary of the app (most common).
     * <li> If downloading, mention that it is downloading instead of showing the summary.
     * <li> If downloaded and ready to install, mention that it is ready to update/install.
     */
    private void configureAppName(@NonNull App app) {
        if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
            if (app.isInstalled()) {
                String appName = activity.getString(R.string.app_list__name__downloaded_and_ready_to_update, app.name);
                if (app.lastUpdated != null) {
                    long ageInMillis = System.currentTimeMillis() - app.lastUpdated.getTime();
                    int ageInDays = (int) (ageInMillis / 1000 / 60 / 60 / 24);
                    Resources resources = activity.getResources();
                    String age;
                    if (ageInDays == 0) {
                        age = resources.getString(R.string.app_list__age__released_today);
                    } else if (ageInDays == 1) {
                        age = resources.getString(R.string.app_list__age__released_yesterday);
                    } else {
                        age = resources.getQuantityString(R.plurals.app_list__age__released_x_days_ago,
                                ageInDays, ageInDays);
                    }
                    name.setText(appName + "\n" + age);
                } else {
                    name.setText(appName);
                }
            } else {
                name.setText(activity.getString(R.string.app_list__name__downloaded_and_ready_to_install, app.name));
            }
        } else if (currentStatus != null && currentStatus.isDownloading()) {
            name.setText(activity.getString(R.string.app_list__name__downloading_in_progress, app.name));
        } else if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.Installed) {
            name.setText(activity.getString(R.string.app_list__name__successfully_installed, app.name));
        } else {
            name.setText(Utils.formatAppNameAndSummary(app.name, app.summary));
        }
    }

    /**
     * The action button will either tell the user to "Update" or "Install" the app. Both actually do
     * the same thing (launch the package manager). It depends on whether the app has a previous
     * version installed or not as to the chosen terminology.
     */
    private void configureActionButton(@NonNull App app) {
        if (actionButton == null) {
            return;
        }

        actionButton.setVisibility(View.VISIBLE);

        if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.Installed) {
            if (activity.getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
                actionButton.setText(R.string.menu_launch);
            } else {
                actionButton.setVisibility(View.GONE);
            }
        } else if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
            if (app.isInstalled()) {
                actionButton.setText(R.string.app__install_downloaded_update);
            } else {
                actionButton.setText(R.string.menu_install);
            }
        } else {
            actionButton.setVisibility(View.GONE);
        }
    }

    /**
     * The install button is shown when an app:
     *  * Is compatible with the users device.
     *  * Has not been filtered due to anti-features/root/etc.
     *  * Is either not installed or installed but can be updated.
     */
    private void configureInstallButton(@NonNull App app) {
        if (installButton == null) {
            return;
        }

        if (isReadyToInstall(app)) {
            installButton.setVisibility(View.GONE);
        } else {
            boolean installable = app.canAndWantToUpdate(activity) || !app.isInstalled();
            boolean shouldAllow = app.compatible && !app.isFiltered();

            if (shouldAllow && installable) {
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download));
                installButton.setVisibility(View.VISIBLE);
            } else {
                installButton.setVisibility(View.GONE);
            }
        }
    }

    private void onDownloadStarted() {
        if (installButton != null) {
            installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_progress));
            installButton.setImageLevel(0);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setIndeterminate(true);
        }

        if (cancelButton != null) {
            cancelButton.setVisibility(View.VISIBLE);
        }
    }

    private void onDownloadProgressUpdated(int bytesRead, int totalBytes) {
        if (installButton != null) {
            installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_progress));
            int progressAsDegrees = totalBytes <= 0 ? 0 : (int) (((float) bytesRead / totalBytes) * 360);
            installButton.setImageLevel(progressAsDegrees);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.VISIBLE);
            if (totalBytes <= 0) {
                progressBar.setIndeterminate(true);
            } else {
                progressBar.setIndeterminate(false);
                progressBar.setMax(totalBytes);
                progressBar.setProgress(bytesRead);
            }
        }

        if (cancelButton != null) {
            cancelButton.setVisibility(View.VISIBLE);
        }
    }

    private void onDownloadComplete() {
        if (installButton != null) {
            installButton.setVisibility(View.GONE);
        }

        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }

        if (cancelButton != null) {
            cancelButton.setVisibility(View.GONE);
        }

        if (currentApp != null) {
            configureActionButton(currentApp);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onAppClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            Intent intent = new Intent(activity, AppDetails2.class);
            intent.putExtra(AppDetails2.EXTRA_APPID, currentApp.packageName);
            if (Build.VERSION.SDK_INT >= 21) {
                Pair<View, String> iconTransitionPair = Pair.create((View) icon, activity.getString(R.string.transition_app_item_icon));
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();
                activity.startActivity(intent, bundle);
            } else {
                activity.startActivity(intent);
            }
        }
    };

    /**
     * Updates both the progress bar and the circular install button (which shows progress around the outside of the circle).
     * Also updates the app label to indicate that the app is being downloaded.
     */
    private void updateAppStatus(@NonNull App app, @NonNull AppUpdateStatusManager.AppUpdateStatus status) {
        currentStatus = status;

        configureAppName(app);
        configureActionButton(app);

        switch (status.status) {
            case PendingDownload:
                onDownloadStarted();
                break;

            case Downloading:
                onDownloadProgressUpdated(status.progressCurrent, status.progressMax);
                break;

            case ReadyToInstall:
                onDownloadComplete();
                break;


            case Installed:
            case Installing:
            case InstallError:
            case UpdateAvailable:
            case DownloadInterrupted:
                break;
        }
    }

    private final BroadcastReceiver onStatusChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUpdateStatusManager.AppUpdateStatus newStatus = intent.getParcelableExtra(AppUpdateStatusManager.EXTRA_STATUS);

            if (currentApp == null || !TextUtils.equals(newStatus.app.packageName, currentApp.packageName) || (installButton == null && progressBar == null)) {
                return;
            }

            updateAppStatus(currentApp, newStatus);
        }
    };

    private final BroadcastReceiver onInstallAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
            if (currentApp == null || !TextUtils.equals(apk.packageName, currentApp.packageName)) {
                return;
            }

            configureAppName(currentApp);
            configureActionButton(currentApp);

            if (installButton == null) {
                return;
            }

            if (Installer.ACTION_INSTALL_STARTED.equals(intent.getAction())) {
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_progress));
                installButton.setImageLevel(0);
            } else if (Installer.ACTION_INSTALL_COMPLETE.equals(intent.getAction())) {
                installButton.setVisibility(View.GONE);
                // TODO: It could've been a different version other than the current suggested version.
                // In these cases, don't hide the button but rather set it back to the default install image.
            } else if (Installer.ACTION_INSTALL_INTERRUPTED.equals(intent.getAction())) {
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download));
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onActionClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            // When the button says "Run", then launch the app.
            if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.Installed) {
                Intent intent = activity.getPackageManager().getLaunchIntentForPackage(currentApp.packageName);
                if (intent != null) {
                    activity.startActivity(intent);

                    // Once it is explicitly launched by the user, then we can pretty much forget about
                    // any sort of notification that the app was successfully installed. It should be
                    // apparent to the user because they just launched it.
                    AppUpdateStatusManager.getInstance(activity).removeApk(currentStatus.getUniqueKey());
                }
                return;
            }

            if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                File apkFilePath = ApkCache.getApkDownloadPath(activity, Uri.parse(currentStatus.apk.getUrl()));
                Utils.debugLog(TAG, "skip download, we have already downloaded " + currentStatus.apk.getUrl() + " to " + apkFilePath);

                // TODO: This seems like a bit of a hack. Is there a better way to do this by changing
                // the Installer API so that we can ask it to install without having to get it to fire
                // off an intent which we then listen for and action?
                final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity);
                final BroadcastReceiver receiver = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context context, Intent intent) {
                        broadcastManager.unregisterReceiver(this);

                        if (Installer.ACTION_INSTALL_USER_INTERACTION.equals(intent.getAction())) {
                            PendingIntent pendingIntent = intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);
                            try {
                                pendingIntent.send();
                            } catch (PendingIntent.CanceledException ignored) { }
                        }
                    }
                };

                broadcastManager.registerReceiver(receiver, Installer.getInstallIntentFilter(Uri.parse(currentStatus.apk.getUrl())));
                Installer installer = InstallerFactory.create(activity, currentStatus.apk);
                installer.installPackage(Uri.parse(apkFilePath.toURI().toString()), Uri.parse(currentStatus.apk.getUrl()));
            } else {
                final Apk suggestedApk = ApkProvider.Helper.findApkFromAnyRepo(activity, currentApp.packageName, currentApp.suggestedVersionCode);
                InstallManagerService.queue(activity, currentApp, suggestedApk);
            }
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onCancelDownload = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentStatus == null || !currentStatus.isDownloading()) {
                return;
            }

            InstallManagerService.cancel(activity, currentStatus.getUniqueKey());
        }
    };
}
