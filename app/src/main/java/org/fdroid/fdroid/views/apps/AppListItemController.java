package org.fdroid.fdroid.views.apps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Outline;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.net.Downloader;
import org.fdroid.fdroid.net.DownloaderService;

// TODO: Support cancelling of downloads by tapping the install button a second time.
// TODO: Support installing of an app once downloaded by tapping the install button a second time.
public class AppListItemController extends RecyclerView.ViewHolder {

    private final Activity activity;

    private final ImageView installButton;
    private final ImageView icon;
    private final TextView name;
    private final TextView status;
    private final DisplayImageOptions displayImageOptions;

    private App currentApp;
    private String currentAppDownloadUrl;

    @TargetApi(21)
    public AppListItemController(final Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        installButton = (ImageView) itemView.findViewById(R.id.install);
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

        icon = (ImageView) itemView.findViewById(R.id.icon);
        name = (TextView) itemView.findViewById(R.id.app_name);
        status = (TextView) itemView.findViewById(R.id.status);

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

        boolean readyToInstall = false;
        for (AppUpdateStatusManager.AppUpdateStatus appStatus : AppUpdateStatusManager.getInstance(activity).getByPackageName(app.packageName)) {
            if (appStatus.status == AppUpdateStatusManager.Status.ReadyToInstall) {
                readyToInstall = true;
                break;
            }
        }

        if (readyToInstall) {
            installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_complete));
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
            if (currentApp == null || !TextUtils.equals(currentAppDownloadUrl, intent.getDataString())) {
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
            if (currentApp == null) {
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

    private final View.OnClickListener onInstallClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            InstallManagerService.queue(activity, currentApp, ApkProvider.Helper.findApkFromAnyRepo(activity, currentApp.packageName, currentApp.suggestedVersionCode));
        }
    };
}
