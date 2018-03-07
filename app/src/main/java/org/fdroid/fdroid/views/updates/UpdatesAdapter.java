package org.fdroid.fdroid.views.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import com.hannesdorfmann.adapterdelegates3.AdapterDelegatesManager;
import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.updates.items.AppStatus;
import org.fdroid.fdroid.views.updates.items.AppUpdateData;
import org.fdroid.fdroid.views.updates.items.KnownVulnApp;
import org.fdroid.fdroid.views.updates.items.UpdateableApp;
import org.fdroid.fdroid.views.updates.items.UpdateableAppsHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Manages the following types of information:
 * <ul>
 * <li>Apps marked for downloading (while the user is offline)</li>
 * <li>Currently downloading apps</li>
 * <li>Apps which have been downloaded (and need further action to install)</li>
 * </ul>
 * This includes new installs and updates.
 * <ul>
 * <li>Reminders to users that they can donate to apps (only shown infrequently after several updates)</li>
 * <li>A list of apps which are eligible to be updated (for when the "Automatic Updates" option is disabled),
 * including:
 * + A summary of all apps to update including an "Update all" button and a "Show apps" button.
 * + Once "Show apps" is expanded then each app is shown along with its own download button.</li>
 * </ul>
 * It does this by maintaining several different lists of interesting apps. Each list contains wrappers
 * around the piece of data it wants to render ({@link AppStatus}, {@link UpdateableApp}).
 * Instead of juggling the various viewTypes
 * to find out which position in the adapter corresponds to which view type, this is handled by
 * the {@link UpdatesAdapter#delegatesManager}.
 * <p>
 * There are a series of type-safe lists which hold the specific data this adapter is interested in.
 * This data is then collated into a single list (see {@link UpdatesAdapter#populateItems()}) which
 * is the actual thing the adapter binds too. At any point it is safe to clear the single list and
 * repopulate it from the original source lists of data. When this is done, the adapter will notify
 * the recycler view that its data has changed. Sometimes it will also ask the recycler view to
 * scroll to the newly added item (if attached to the recycler view).
 * <p>
 * TODO: If a user downloads an old version of an app (resulting in a new update being available
 * instantly), then we need to refresh the list of apps to update.
 */
public class UpdatesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int LOADER_CAN_UPDATE = 289753982;
    private static final int LOADER_KNOWN_VULN = 520389740;

    private final AdapterDelegatesManager<List<AppUpdateData>> delegatesManager = new AdapterDelegatesManager<>();
    private final List<AppUpdateData> items = new ArrayList<>();

    private final AppCompatActivity activity;

    private final List<AppStatus> appsToShowStatus = new ArrayList<>();
    private final List<UpdateableApp> updateableApps = new ArrayList<>();
    private final List<KnownVulnApp> knownVulnApps = new ArrayList<>();

    private boolean showAllUpdateableApps = false;

    public UpdatesAdapter(AppCompatActivity activity) {
        this.activity = activity;

        delegatesManager.addDelegate(new AppStatus.Delegate(activity))
                .addDelegate(new UpdateableApp.Delegate(activity))
                .addDelegate(new UpdateableAppsHeader.Delegate(activity))
                .addDelegate(new KnownVulnApp.Delegate(activity));

        initLoaders();
    }

    /**
     * There are some statuses managed by {@link AppUpdateStatusManager} which we don't care about
     * for the "Updates" view. For example Also, although this
     * adapter does know about apps with updates availble, it does so by querying the database not
     * by querying the app update status manager. As such, apps with the status
     * {@link org.fdroid.fdroid.AppUpdateStatusManager.Status#UpdateAvailable} are not interesting here.
     */
    private boolean shouldShowStatus(AppUpdateStatusManager.AppUpdateStatus status) {
        return status.status == AppUpdateStatusManager.Status.Downloading ||
                status.status == AppUpdateStatusManager.Status.Installed ||
                status.status == AppUpdateStatusManager.Status.ReadyToInstall;
    }

    /**
     * Adds items from the {@link AppUpdateStatusManager} to {@link UpdatesAdapter#appsToShowStatus}.
     * Note that this will then subsequently rebuild the underlying adapter data structure by
     * invoking {@link UpdatesAdapter#populateItems}. However as per the populateItems method, it
     * does not know how best to notify the recycler view of any changes. That is up to the caller
     * of this method.
     */
    private void populateAppStatuses() {
        for (AppUpdateStatusManager.AppUpdateStatus status : AppUpdateStatusManager.getInstance(activity).getAll()) {
            if (shouldShowStatus(status)) {
                appsToShowStatus.add(new AppStatus(activity, status));
            }
        }

        Collections.sort(appsToShowStatus, new Comparator<AppStatus>() {
            @Override
            public int compare(AppStatus o1, AppStatus o2) {
                return o1.status.app.name.compareTo(o2.status.app.name);
            }
        });

        populateItems();
    }

    public boolean canViewAllUpdateableApps() {
        return showAllUpdateableApps;
    }

    public void toggleAllUpdateableApps() {
        showAllUpdateableApps = !showAllUpdateableApps;
        populateItems();
    }

    /**
     * Completely rebuilds the underlying data structure used by this adapter. Note however, that
     * this does not notify the recycler view of any changes. Thus, it is up to other methods which
     * initiate a call to this method to make sure they appropriately notify the recyler view.
     */
    private void populateItems() {
        items.clear();

        Set<String> toShowStatusPackageNames = new HashSet<>(appsToShowStatus.size());
        for (AppStatus app : appsToShowStatus) {
            toShowStatusPackageNames.add(app.status.app.packageName);
            items.add(app);
        }

        if (updateableApps != null) {
            // Only count/show apps which are not shown above in the "Apps to show status" list.
            List<UpdateableApp> updateableAppsToShow = new ArrayList<>(updateableApps.size());
            for (UpdateableApp app : updateableApps) {
                if (!toShowStatusPackageNames.contains(app.app.packageName)) {
                    updateableAppsToShow.add(app);
                }
            }

            if (updateableAppsToShow.size() > 0) {
                items.add(new UpdateableAppsHeader(activity, this, updateableAppsToShow));

                if (showAllUpdateableApps) {
                    items.addAll(updateableAppsToShow);
                }
            }
        }

        items.addAll(knownVulnApps);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public int getItemViewType(int position) {
        return delegatesManager.getItemViewType(items, position);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return delegatesManager.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        delegatesManager.onBindViewHolder(items, position, holder);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Uri uri;
        switch (id) {
            case LOADER_CAN_UPDATE:
                uri = AppProvider.getCanUpdateUri();
                break;

            case LOADER_KNOWN_VULN:
                uri = AppProvider.getInstalledWithKnownVulnsUri();
                break;

            default:
                throw new IllegalStateException("Unknown loader requested: " + id);
        }

        return new CursorLoader(
                activity, uri, Schema.AppMetadataTable.Cols.ALL, null, null, Schema.AppMetadataTable.Cols.NAME);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case LOADER_CAN_UPDATE:
                onCanUpdateLoadFinished(cursor);
                break;

            case LOADER_KNOWN_VULN:
                onKnownVulnLoadFinished(cursor);
                break;
        }

        populateItems();
        notifyDataSetChanged();
    }

    private void onCanUpdateLoadFinished(Cursor cursor) {
        updateableApps.clear();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            updateableApps.add(new UpdateableApp(activity, new App(cursor)));
            cursor.moveToNext();
        }
    }

    private void onKnownVulnLoadFinished(Cursor cursor) {
        knownVulnApps.clear();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            knownVulnApps.add(new KnownVulnApp(activity, new App(cursor)));
            cursor.moveToNext();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    /**
     * If this adapter is "active" then it is part of the current UI that the user is looking to.
     * Under those circumstances, we want to make sure it is up to date, and also listen to the
     * correct set of broadcasts.
     * Doesn't listen for {@link AppUpdateStatusManager#BROADCAST_APPSTATUS_CHANGED} because the
     * individual items in the recycler view will listen for the appropriate changes in state and
     * update themselves accordingly (if they are displayed).
     */
    public void setIsActive() {
        appsToShowStatus.clear();
        populateAppStatuses();
        notifyDataSetChanged();

        IntentFilter filter = new IntentFilter();
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED);
        filter.addAction(AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED);

        LocalBroadcastManager.getInstance(activity).registerReceiver(receiverAppStatusChanges, filter);
    }

    public void stopListeningForStatusUpdates() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(receiverAppStatusChanges);
    }

    private void onManyAppStatusesChanged(String reasonForChange) {
        switch (reasonForChange) {
            case AppUpdateStatusManager.REASON_UPDATES_AVAILABLE:
                onUpdateableAppsChanged();
                break;

            case AppUpdateStatusManager.REASON_READY_TO_INSTALL:
                onFoundAppsReadyToInstall();
                break;
        }
    }

    /**
     * Apps have been made available for update which were not available for update before.
     * We need to rerun our database query to get a list of apps to update.
     */
    private void onUpdateableAppsChanged() {
        initLoaders();
    }

    private void initLoaders() {
        activity.getSupportLoaderManager().initLoader(LOADER_CAN_UPDATE, null, this);
        activity.getSupportLoaderManager().initLoader(LOADER_KNOWN_VULN, null, this);
    }

    /**
     * We have completed a scan of .apk files in the cache, and identified there are
     * some which are ready to install.
     */
    private void onFoundAppsReadyToInstall() {
        populateAppStatuses();
        notifyDataSetChanged();
    }

    private void onAppStatusAdded() {
        appsToShowStatus.clear();
        populateAppStatuses();
        notifyDataSetChanged();
    }

    private void onAppStatusRemoved() {
        appsToShowStatus.clear();
        populateAppStatuses();
        notifyDataSetChanged();
    }

    private final BroadcastReceiver receiverAppStatusChanges = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }
            switch (intent.getAction()) {
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED:
                    onManyAppStatusesChanged(intent.getStringExtra(AppUpdateStatusManager.EXTRA_REASON_FOR_CHANGE));
                    break;

                case AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED:
                    onAppStatusAdded();
                    break;

                case AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED:
                    onAppStatusRemoved();
                    break;
            }
        }
    };

    /**
     * If an item representing an {@link org.fdroid.fdroid.AppUpdateStatusManager.AppUpdateStatus} is dismissed,
     * then we should rebuild the list of app statuses and update the adapter.
     */
    public void refreshStatuses() {
        onAppStatusRemoved();
    }
}
