package org.fdroid.fdroid.views.apps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
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
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.fdroid.fdroid.AppDetails;
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
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

import java.io.File;

// TODO: Support cancelling of downloads by tapping the install button a second time.
// TODO: Support installing of an app once downloaded by tapping the install button a second time.
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

    private final DisplayImageOptions displayImageOptions;

    private App currentApp;
    private String currentAppDownloadUrl;

    @TargetApi(21)
    public AppListItemController(final Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        installButton = (ImageView) itemView.findViewById(R.id.install);
        if (installButton != null) {
            installButton.setOnClickListener(onInstallClicked);

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

        displayImageOptions = Utils.getImageLoadingOptions().build();

        itemView.setOnClickListener(onAppClicked);
    }

    public void bindModel(@NonNull App app) {
        currentApp = app;
        name.setText(Utils.formatAppNameAndSummary(app.name, app.summary));

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, displayImageOptions);

        Apk apkToInstall = ApkProvider.Helper.findApkFromAnyRepo(activity, app.packageName, app.suggestedVersionCode);
        currentAppDownloadUrl = apkToInstall.getUrl();

        final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity.getApplicationContext());
        broadcastManager.unregisterReceiver(onDownloadProgress);
        broadcastManager.unregisterReceiver(onInstallAction);

        broadcastManager.registerReceiver(onDownloadProgress, DownloaderService.getIntentFilter(currentAppDownloadUrl));
        broadcastManager.registerReceiver(onInstallAction, Installer.getInstallIntentFilter(Uri.parse(currentAppDownloadUrl)));

        configureStatusText(app);
        configureInstalledVersion(app);
        configureIgnoredStatus(app);
        configureInstallButton(app);
    }

    /**
     * Sets the text/visibility of the {@link R.id#status} {@link TextView} based on whether the app:
     *  * Is compatible with the users device
     *  * Is installed
     *  * Can be updated
     *
     * TODO: This button also needs to be repurposed to support the "Downloaded but not installed" state.
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

    private boolean isReadyToInstall(@NonNull App app) {
        for (AppUpdateStatusManager.AppUpdateStatus appStatus : AppUpdateStatusManager.getInstance(activity).getByPackageName(app.packageName)) {
            if (appStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                return true;
            }
        }
        return false;
    }

    /**
     * The install button is shown when an app:
     *  * Is compatible with the users device.
     *  * Has not been filtered due to anti-features/root/etc.
     *  * Is either not installed or installed but can be updated.
     *
     * TODO: This button also needs to be repurposed to support the "Downloaded but not installed" state.
     */
    private void configureInstallButton(@NonNull App app) {
        if (installButton == null) {
            return;
        }

        if (isReadyToInstall(app)) {
            installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_complete));
            installButton.setVisibility(View.VISIBLE);
            // TODO: If in the downloading phase, then need to reflect that instead of this "download complete" icon.
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

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onAppClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            Intent intent = new Intent(activity, AppDetails2.class);
            intent.putExtra(AppDetails.EXTRA_APPID, currentApp.packageName);
            if (Build.VERSION.SDK_INT >= 21) {
                Pair<View, String> iconTransitionPair = Pair.create((View) icon, activity.getString(R.string.transition_app_item_icon));
                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();
                activity.startActivity(intent, bundle);
            } else {
                activity.startActivity(intent);
            }
        }
    };

    private final BroadcastReceiver onDownloadProgress = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (installButton == null || currentApp == null || !TextUtils.equals(currentAppDownloadUrl, intent.getDataString())) {
                return;
            }

            if (Downloader.ACTION_PROGRESS.equals(intent.getAction())) {
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_progress));
                int bytesRead = intent.getIntExtra(Downloader.EXTRA_BYTES_READ, 0);
                int totalBytes = intent.getIntExtra(Downloader.EXTRA_TOTAL_BYTES, 100);

                int progressAsDegrees = (int) (((float) bytesRead / totalBytes) * 360);
                installButton.setImageLevel(progressAsDegrees);
            } else if (Downloader.ACTION_COMPLETE.equals(intent.getAction())) {
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_complete));
            }
        }
    };

    private final BroadcastReceiver onInstallAction = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (currentApp == null || installButton == null) {
                return;
            }

            Apk apk = intent.getParcelableExtra(Installer.EXTRA_APK);
            if (!TextUtils.equals(apk.packageName, currentApp.packageName)) {
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
    private final View.OnClickListener onInstallClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            final Apk suggestedApk = ApkProvider.Helper.findApkFromAnyRepo(activity, currentApp.packageName, currentApp.suggestedVersionCode);

            if (isReadyToInstall(currentApp)) {
                File apkFilePath = ApkCache.getApkDownloadPath(activity, Uri.parse(suggestedApk.getUrl()));
                Utils.debugLog(TAG, "skip download, we have already downloaded " + suggestedApk.getUrl() + " to " + apkFilePath);

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

                broadcastManager.registerReceiver(receiver, Installer.getInstallIntentFilter(Uri.parse(suggestedApk.getUrl())));
                Installer installer = InstallerFactory.create(activity, suggestedApk);
                installer.installPackage(Uri.parse(apkFilePath.toURI().toString()), Uri.parse(suggestedApk.getUrl()));
            } else {
                InstallManagerService.queue(activity, currentApp, suggestedApk);
            }
        }
    };
}
