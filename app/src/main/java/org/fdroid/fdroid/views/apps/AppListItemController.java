package org.fdroid.fdroid.views.apps;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.nostra13.universalimageloader.core.ImageLoader;
import org.fdroid.fdroid.AppDetails2;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.AppUpdateStatusManager.AppUpdateStatus;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;
import org.fdroid.fdroid.data.Apk;
import org.fdroid.fdroid.data.ApkProvider;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.installer.ApkCache;
import org.fdroid.fdroid.installer.InstallManagerService;
import org.fdroid.fdroid.installer.Installer;
import org.fdroid.fdroid.installer.InstallerFactory;
import org.fdroid.fdroid.views.updates.DismissResult;

import java.io.File;
import java.util.Iterator;

/**
 * Supports the following layouts:
 * <ul>
 * <li>app_list_item (see {@link StandardAppListItemController}</li>
 * <li>updateable_app_list_status_item (see
 * {@link org.fdroid.fdroid.views.updates.items.AppStatusListItemController}</li>
 * <li>updateable_app_list_item (see
 * {@link org.fdroid.fdroid.views.updates.items.UpdateableAppListItemController}</li>
 * <li>installed_app_list_item (see {@link StandardAppListItemController}</li>
 * </ul>
 * <p>
 * The state of the UI is defined in a dumb {@link AppListItemState} class, then applied to the UI
 * in the {@link #updateAppStatus(App, AppUpdateStatus)}  method.
 */
public abstract class AppListItemController extends RecyclerView.ViewHolder {

    private static final String TAG = "AppListItemController";

    protected final Activity activity;

    @NonNull
    private final ImageView icon;

    @NonNull
    private final TextView name;

    @Nullable
    private final ImageView installButton;

    @Nullable
    private final TextView status;

    @Nullable
    private final TextView secondaryStatus;

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

    @Nullable
    private final Button secondaryButton;

    @Nullable
    private App currentApp;

    @Nullable
    private AppUpdateStatus currentStatus;

    @TargetApi(21)
    public AppListItemController(final Activity activity, View itemView) {
        super(itemView);
        this.activity = activity;

        installButton = (ImageView) itemView.findViewById(R.id.install);
        if (installButton != null) {
            installButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onActionButtonPressed(currentApp);
                }
            });

            if (Build.VERSION.SDK_INT >= 21) {
                installButton.setOutlineProvider(new ViewOutlineProvider() {
                    @Override
                    public void getOutline(View view, Outline outline) {
                        float density = activity.getResources().getDisplayMetrics().density;

                        // This is a bit hacky/hardcoded/too-specific to the particular icons we're using.
                        // This is because the default "download & install" and "downloaded & ready to install"
                        // icons are smaller than the "downloading progress" button. Hence, we can't just use
                        // the width/height of the view to calculate the outline size.
                        int xPadding = (int) (8 * density);
                        int yPadding = (int) (9 * density);
                        int right = installButton.getWidth() - xPadding;
                        int bottom = installButton.getHeight() - yPadding;
                        outline.setOval(xPadding, yPadding, right, bottom);
                    }
                });
            }
        }

        icon = (ImageView) itemView.findViewById(R.id.icon);
        name = (TextView) itemView.findViewById(R.id.app_name);
        status = (TextView) itemView.findViewById(R.id.status);
        secondaryStatus = (TextView) itemView.findViewById(R.id.secondary_status);
        progressBar = (ProgressBar) itemView.findViewById(R.id.progress_bar);
        cancelButton = (ImageButton) itemView.findViewById(R.id.cancel_button);
        actionButton = (Button) itemView.findViewById(R.id.action_button);
        secondaryButton = (Button) itemView.findViewById(R.id.secondary_button);

        if (actionButton != null) {
            actionButton.setEnabled(true);
            actionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionButton.setEnabled(false);
                    onActionButtonPressed(currentApp);
                }
            });
        }

        if (secondaryButton != null) {
            secondaryButton.setOnClickListener(onSecondaryButtonClicked);
        }

        if (cancelButton != null) {
            cancelButton.setOnClickListener(onCancelDownload);
        }

        itemView.setOnClickListener(onAppClicked);
    }

    @Nullable
    protected final AppUpdateStatus getCurrentStatus() {
        return currentStatus;
    }

    public void bindModel(@NonNull App app) {
        currentApp = app;

        ImageLoader.getInstance().displayImage(app.iconUrl, icon, Utils.getRepoAppDisplayImageOptions());

        // Figures out the current install/update/download/etc status for the app we are viewing.
        // Then, asks the view to update itself to reflect this status.
        Iterator<AppUpdateStatus> statuses =
                AppUpdateStatusManager.getInstance(activity).getByPackageName(app.packageName).iterator();
        if (statuses.hasNext()) {
            AppUpdateStatus status = statuses.next();
            updateAppStatus(app, status);
        } else {
            updateAppStatus(app, null);
        }

        final LocalBroadcastManager broadcastManager =
                LocalBroadcastManager.getInstance(activity.getApplicationContext());
        broadcastManager.unregisterReceiver(onStatusChanged);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        intentFilter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_CHANGED);
        broadcastManager.registerReceiver(onStatusChanged, intentFilter);
    }

    /**
     * To be overridden if required
     */
    public boolean canDismiss() {
        return false;
    }

    /**
     * If able, forwards the request onto {@link #onDismissApp(App)}.
     * This mainly exists to keep the API consistent, in that the {@link App} is threaded through to the relevant
     * method with a guarantee that it is not null, rather than every method having to check if it is null or not.
     */
    @NonNull
    public final DismissResult onDismiss() {
        if (currentApp != null && canDismiss()) {
            return onDismissApp(currentApp);
        }

        return new DismissResult();
    }

    /**
     * Override to respond to the user swiping an app to dismiss it from the list.
     *
     * @return Optionally return a description of what you did if it is not obvious to the user. It will be shown as
     * a {@link android.widget.Toast} for a {@link android.widget.Toast#LENGTH_SHORT} time.
     * @see #canDismiss() This must also be overriden and should return true.
     */
    @NonNull
    protected DismissResult onDismissApp(@NonNull App app) {
        return new DismissResult();
    }

    /**
     * Updates both the progress bar and the circular install button (which
     * shows progress around the outside of the circle). Also updates the app
     * label to indicate that the app is being downloaded.
     * <p>
     * Queries the current state via {@link #getCurrentViewState(App, AppUpdateStatus)}
     * and then updates the relevant widgets depending on that state.
     * <p>
     * Should contain little to no business logic, this all belongs to
     * {@link #getCurrentViewState(App, AppUpdateStatus)}.
     *
     * @see AppListItemState
     * @see #getCurrentViewState(App, AppUpdateStatus)
     */
    private void updateAppStatus(@NonNull App app, @Nullable AppUpdateStatus appStatus) {
        currentStatus = appStatus;

        AppListItemState viewState = getCurrentViewState(app, appStatus);

        name.setText(viewState.getMainText());

        if (actionButton != null) {
            if (viewState.shouldShowActionButton()) {
                actionButton.setVisibility(View.VISIBLE);
                actionButton.setText(viewState.getActionButtonText());
            } else {
                actionButton.setVisibility(View.GONE);
            }
        }

        if (secondaryButton != null) {
            if (viewState.shouldShowSecondaryButton()) {
                secondaryButton.setVisibility(View.VISIBLE);
                secondaryButton.setText(viewState.getSecondaryButtonText());
            } else {
                secondaryButton.setVisibility(View.GONE);
            }
        }

        if (progressBar != null) {
            if (viewState.showProgress()) {
                progressBar.setVisibility(View.VISIBLE);
                if (viewState.isProgressIndeterminate()) {
                    progressBar.setIndeterminate(true);
                } else {
                    progressBar.setIndeterminate(false);
                    progressBar.setMax(viewState.getProgressMax());
                    progressBar.setProgress(viewState.getProgressCurrent());
                }
            } else {
                progressBar.setVisibility(View.GONE);
            }
        }

        if (cancelButton != null) {
            if (viewState.showProgress()) {
                cancelButton.setVisibility(View.VISIBLE);
            } else {
                cancelButton.setVisibility(View.GONE);
            }
        }

        if (installButton != null) {
            if (viewState.shouldShowActionButton()) {
                installButton.setVisibility(View.GONE);
            } else if (viewState.showProgress()) {
                installButton.setEnabled(false);
                installButton.setVisibility(View.VISIBLE);
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download_progress));
                int progressAsDegrees = viewState.getProgressMax() <= 0 ? 0 :
                        (int) (((float) viewState.getProgressCurrent() / viewState.getProgressMax()) * 360);
                installButton.setImageLevel(progressAsDegrees);
            } else if (viewState.shouldShowInstall()) {
                installButton.setEnabled(true);
                installButton.setVisibility(View.VISIBLE);
                installButton.setImageDrawable(ContextCompat.getDrawable(activity, R.drawable.ic_download));
            } else {
                installButton.setVisibility(View.GONE);
            }
        }

        if (status != null) {
            CharSequence statusText = viewState.getStatusText();
            if (statusText == null) {
                status.setVisibility(View.GONE);
            } else {
                status.setVisibility(View.VISIBLE);
                status.setText(statusText);
            }
        }

        if (secondaryStatus != null) {
            CharSequence statusText = viewState.getSecondaryStatusText();
            if (statusText == null) {
                secondaryStatus.setVisibility(View.GONE);
            } else {
                secondaryStatus.setVisibility(View.VISIBLE);
                secondaryStatus.setText(statusText);
            }
        }
    }

    @NonNull
    protected AppListItemState getCurrentViewState(@NonNull App app, @Nullable AppUpdateStatus appStatus) {
        if (appStatus == null) {
            return getViewStateDefault(app);
        } else {
            switch (appStatus.status) {
                case ReadyToInstall:
                    return getViewStateReadyToInstall(app);

                case PendingInstall:
                case Downloading:
                    return getViewStateDownloading(app, appStatus);

                case Installing:
                    return getViewStateInstalling(app);

                case Installed:
                    return getViewStateInstalled(app);

                default:
                    return getViewStateDefault(app);
            }
        }
    }

    protected AppListItemState getViewStateInstalling(@NonNull App app) {
        CharSequence mainText = activity.getString(
                R.string.app_list__name__downloading_in_progress, app.name);

        return new AppListItemState(app)
                .setMainText(mainText)
                .showActionButton(null)
                .setStatusText(activity.getString(R.string.notification_content_single_installing, app.name));
    }

    protected AppListItemState getViewStateInstalled(@NonNull App app) {
        CharSequence mainText = activity.getString(
                R.string.app_list__name__successfully_installed, app.name);

        AppListItemState state = new AppListItemState(app)
                .setMainText(mainText)
                .setStatusText(activity.getString(R.string.notification_content_single_installed));

        if (activity.getPackageManager().getLaunchIntentForPackage(app.packageName) != null) {
            state.showActionButton(activity.getString(R.string.menu_launch));
        }

        return state;
    }

    protected AppListItemState getViewStateDownloading(@NonNull App app, @NonNull AppUpdateStatus currentStatus) {
        CharSequence mainText = activity.getString(
                R.string.app_list__name__downloading_in_progress, app.name);

        return new AppListItemState(app)
                .setMainText(mainText)
                .setProgress(Utils.bytesToKb(currentStatus.progressCurrent),
                        Utils.bytesToKb(currentStatus.progressMax));
    }

    protected AppListItemState getViewStateReadyToInstall(@NonNull App app) {
        int actionButtonLabel = app.isInstalled(activity.getApplicationContext())
                ? R.string.app__install_downloaded_update
                : R.string.menu_install;

        return new AppListItemState(app)
                .setMainText(app.name)
                .showActionButton(activity.getString(actionButtonLabel))
                .setStatusText(activity.getString(R.string.app_list_download_ready));
    }

    protected AppListItemState getViewStateDefault(@NonNull App app) {
        return new AppListItemState(app);
    }

    /* =================================================================
     * Various listeners for each different click/broadcast that we need
     * to respond to.
     * =================================================================
     */

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
                String transitionAppIcon = activity.getString(R.string.transition_app_item_icon);
                Pair<View, String> iconTransitionPair = Pair.create((View) icon, transitionAppIcon);
                Bundle bundle = ActivityOptionsCompat
                        .makeSceneTransitionAnimation(activity, iconTransitionPair).toBundle();
                activity.startActivity(intent, bundle);
            } else {
                activity.startActivity(intent);
            }
        }
    };

    private final BroadcastReceiver onStatusChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            AppUpdateStatus newStatus = intent.getParcelableExtra(AppUpdateStatusManager.EXTRA_STATUS);

            if (currentApp == null
                    || !TextUtils.equals(newStatus.app.packageName, currentApp.packageName)
                    || (installButton == null && progressBar == null)) {
                return;
            }

            updateAppStatus(currentApp, newStatus);
        }
    };

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onSecondaryButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (currentApp == null) {
                return;
            }

            onSecondaryButtonPressed(currentApp);
        }
    };

    protected void onActionButtonPressed(App app) {
        if (app == null) {
            return;
        }

        // When the button says "Run", then launch the app.
        if (currentStatus != null && currentStatus.status == AppUpdateStatusManager.Status.Installed) {
            Intent intent = activity.getPackageManager().getLaunchIntentForPackage(app.packageName);
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
            Uri apkDownloadUri = Uri.parse(currentStatus.apk.getUrl());
            File apkFilePath = ApkCache.getApkDownloadPath(activity, apkDownloadUri);
            Utils.debugLog(TAG, "skip download, we have already downloaded " + currentStatus.apk.getUrl() +
                    " to " + apkFilePath);

            final LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(activity);
            final BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    broadcastManager.unregisterReceiver(this);

                    if (Installer.ACTION_INSTALL_USER_INTERACTION.equals(intent.getAction())) {
                        PendingIntent pendingIntent =
                                intent.getParcelableExtra(Installer.EXTRA_USER_INTERACTION_PI);
                        try {
                            pendingIntent.send();
                        } catch (PendingIntent.CanceledException ignored) {
                        }
                    }
                }
            };

            broadcastManager.registerReceiver(receiver, Installer.getInstallIntentFilter(apkDownloadUri));
            Installer installer = InstallerFactory.create(activity, currentStatus.apk);
            installer.installPackage(Uri.parse(apkFilePath.toURI().toString()), apkDownloadUri);
        } else {
            final Apk suggestedApk = ApkProvider.Helper.findSuggestedApk(activity, app);
            InstallManagerService.queue(activity, app, suggestedApk);
        }
    }

    /**
     * To be overridden by subclasses if desired
     */
    protected void onSecondaryButtonPressed(@NonNull App app) {
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final View.OnClickListener onCancelDownload = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            cancelDownload();
        }
    };

    protected final void cancelDownload() {
        if (currentStatus == null || currentStatus.status != AppUpdateStatusManager.Status.Downloading) {
            return;
        }

        InstallManagerService.cancel(activity, currentStatus.getUniqueKey());
    }
}
