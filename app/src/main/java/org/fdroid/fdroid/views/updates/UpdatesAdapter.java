package org.fdroid.fdroid.views.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.ViewGroup;

import com.hannesdorfmann.adapterdelegates3.AdapterDelegatesManager;

import org.fdroid.fdroid.AppUpdateStatusManager;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.data.Schema;
import org.fdroid.fdroid.views.updates.items.AppNotification;
import org.fdroid.fdroid.views.updates.items.AppStatus;
import org.fdroid.fdroid.views.updates.items.AppUpdateData;
import org.fdroid.fdroid.views.updates.items.DonationPrompt;
import org.fdroid.fdroid.views.updates.items.UpdateableApp;
import org.fdroid.fdroid.views.updates.items.UpdateableAppsHeader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Manages the following types of information:
 *  * Apps marked for downloading (while the user is offline)
 *  * Currently downloading apps
 *  * Apps which have been downloaded (and need further action to install). This includes new installs and updates.
 *  * Reminders to users that they can donate to apps (only shown infrequently after several updates)
 *  * A list of apps which are eligible to be updated (for when the "Automatic Updates" option is disabled), including:
 *    + A summary of all apps to update including an "Update all" button and a "Show apps" button.
 *    + Once "Show apps" is expanded then each app is shown along with its own download button.
 *
 * It does this by maintaining several different lists of interesting apps. Each list contains wrappers
 * around the piece of data it wants to render ({@link AppStatus}, {@link DonationPrompt},
 * {@link AppNotification}, {@link UpdateableApp}). Instead of juggling the various viewTypes
 * to find out which position in the adapter corresponds to which view type, this is handled by
 * the {@link UpdatesAdapter#delegatesManager}.
 *
 * There are a series of type-safe lists which hold the specific data this adapter is interested in.
 * This data is then collated into a single list (see {@link UpdatesAdapter#populateItems()}) which
 * is the actual thing the adapter binds too. At any point it is safe to clear the single list and
 * repopulate it from the original source lists of data. When this is done, the adapter will notify
 * the recycler view that its data has changed. Sometimes it will also ask the recycler view to
 * scroll to the newly added item (if attached to the recycler view).
 *
 * TODO: If a user downloads an old version of an app (resulting in a new update being available
 * instantly), then we need to refresh the list of apps to update.
 */
public class UpdatesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements LoaderManager.LoaderCallbacks<Cursor> {

    private final AdapterDelegatesManager<List<AppUpdateData>> delegatesManager = new AdapterDelegatesManager<>();
    private final List<AppUpdateData> items = new ArrayList<>();

    private final AppCompatActivity activity;

    @Nullable
    private RecyclerView recyclerView;

    private final List<AppStatus> appsToShowStatus = new ArrayList<>();
    private final List<DonationPrompt> appsToPromptForDonation = new ArrayList<>();
    private final List<AppNotification> appsToNotifyAbout = new ArrayList<>();
    private final List<UpdateableApp> updateableApps = new ArrayList<>();

    private boolean showAllUpdateableApps = false;

    public UpdatesAdapter(AppCompatActivity activity) {
        this.activity = activity;

        delegatesManager.addDelegate(new AppStatus.Delegate(activity))
                .addDelegate(new AppNotification.Delegate())
                .addDelegate(new DonationPrompt.Delegate())
                .addDelegate(new UpdateableApp.Delegate(activity))
                .addDelegate(new UpdateableAppsHeader.Delegate(activity));

        activity.getSupportLoaderManager().initLoader(0, null, this);
    }

    /**
     * There are some statuses managed by {@link AppUpdateStatusManager} which we don't care about
     * for the "Updates" view. For example Also, although this
     * adapter does know about apps with updates availble, it does so by querying the database not
     * by querying the app update status manager. As such, apps with the status
     * {@link org.fdroid.fdroid.AppUpdateStatusManager.Status#UpdateAvailable} are not interesting here.
     */
    private boolean shouldShowStatus(AppUpdateStatusManager.AppUpdateStatus status) {
        return status.status == AppUpdateStatusManager.Status.PendingDownload ||
                status.status == AppUpdateStatusManager.Status.Downloading ||
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

        if (showAllUpdateableApps) {
            notifyItemRangeInserted(appsToShowStatus.size() + 1, updateableApps.size());
            if (recyclerView != null) {
                // Scroll so that the "Update X apps" header is at the top of the page, and the
                // list of apps takes up the rest of the screen.
                ((LinearLayoutManager) recyclerView.getLayoutManager()).scrollToPositionWithOffset(appsToShowStatus.size(), 0);
            }
        } else {
            notifyItemRangeRemoved(appsToShowStatus.size() + 1, updateableApps.size());
        }
    }

    /**
     * Completely rebuilds the underlying data structure used by this adapter. Note however, that
     * this does not notify the recycler view of any changes. Thus, it is up to other methods which
     * initiate a call to this method to make sure they appropriately notify the recyler view.
     */
    private void populateItems() {
        items.clear();

        items.addAll(appsToShowStatus);

        if (updateableApps != null && updateableApps.size() > 0) {
            items.add(new UpdateableAppsHeader(activity, this, updateableApps));
            if (showAllUpdateableApps) {
                items.addAll(updateableApps);
            }
        }

        items.addAll(appsToPromptForDonation);
        items.addAll(appsToNotifyAbout);
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
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.recyclerView = recyclerView;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(
                activity,
                AppProvider.getCanUpdateUri(),
                new String[]{
                        Schema.AppMetadataTable.Cols._ID, // Required for cursor loader to work.
                        Schema.AppMetadataTable.Cols.Package.PACKAGE_NAME,
                        Schema.AppMetadataTable.Cols.NAME,
                        Schema.AppMetadataTable.Cols.SUMMARY,
                        Schema.AppMetadataTable.Cols.IS_COMPATIBLE,
                        Schema.AppMetadataTable.Cols.LICENSE,
                        Schema.AppMetadataTable.Cols.ICON,
                        Schema.AppMetadataTable.Cols.ICON_URL,
                        Schema.AppMetadataTable.Cols.InstalledApp.VERSION_CODE,
                        Schema.AppMetadataTable.Cols.InstalledApp.VERSION_NAME,
                        Schema.AppMetadataTable.Cols.SuggestedApk.VERSION_NAME,
                        Schema.AppMetadataTable.Cols.SUGGESTED_VERSION_CODE,
                        Schema.AppMetadataTable.Cols.REQUIREMENTS, // Needed for filtering apps that require root.
                        Schema.AppMetadataTable.Cols.ANTI_FEATURES, // Needed for filtering apps that require anti-features.
                },
                null,
                null,
                Schema.AppMetadataTable.Cols.NAME
        );
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        int numberRemoved = updateableApps.size();
        boolean hadHeader = updateableApps.size() > 0;
        boolean willHaveHeader = cursor.getCount() > 0;

        updateableApps.clear();
        notifyItemRangeRemoved(appsToShowStatus.size(), numberRemoved + (hadHeader ? 1 : 0));

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            updateableApps.add(new UpdateableApp(activity, new App(cursor)));
            cursor.moveToNext();
        }

        populateItems();
        notifyItemRangeInserted(appsToShowStatus.size(), updateableApps.size() + (willHaveHeader ? 1 : 0));
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

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
        activity.getSupportLoaderManager().initLoader(0, null, this);
    }

    /**
     * We have completed a scan of .apk files in the cache, and identified there are
     * some which are ready to install.
     */
    private void onFoundAppsReadyToInstall() {
        if (appsToShowStatus.size() > 0) {
            int size = appsToShowStatus.size();
            appsToShowStatus.clear();
            notifyItemRangeRemoved(0, size);
        }

        populateAppStatuses();
        notifyItemRangeInserted(0, appsToShowStatus.size());

        if (recyclerView != null) {
            recyclerView.smoothScrollToPosition(0);
        }
    }

    private void onAppStatusAdded(String apkUrl) {
        // We could try and find the specific place where we need to add our new item, but it is
        // far simpler to clear the list and rebuild it (sorting it in the process).
        appsToShowStatus.clear();
        populateAppStatuses();

        // After adding the new item to our list (somewhere) we can then look it back up again in
        // order to notify the recycler view and scroll to that item.
        int positionOfNewApp = -1;
        for (int i = 0; i < appsToShowStatus.size(); i++) {
            if (TextUtils.equals(appsToShowStatus.get(i).status.getUniqueKey(), apkUrl)) {
                positionOfNewApp = i;
                break;
            }
        }

        if (positionOfNewApp != -1) {
            notifyItemInserted(positionOfNewApp);

            if (recyclerView != null) {
                recyclerView.smoothScrollToPosition(positionOfNewApp);
            }
        }
    }

    private void onAppStatusRemoved(String apkUrl) {
        // Find out where the item is in our internal data structure, so that we can remove it and
        // also notify the recycler view appropriately.
        int positionOfOldApp = -1;
        for (int i = 0; i < appsToShowStatus.size(); i++) {
            if (TextUtils.equals(appsToShowStatus.get(i).status.getUniqueKey(), apkUrl)) {
                positionOfOldApp = i;
                break;
            }
        }

        if (positionOfOldApp != -1) {
            appsToShowStatus.remove(positionOfOldApp);

            populateItems();
            notifyItemRemoved(positionOfOldApp);
        }
    }

    private final BroadcastReceiver receiverAppStatusChanges = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String apkUrl = intent.getStringExtra(AppUpdateStatusManager.EXTRA_APK_URL);

            switch (intent.getAction()) {
                case AppUpdateStatusManager.BROADCAST_APPSTATUS_LIST_CHANGED:
                    onManyAppStatusesChanged(intent.getStringExtra(AppUpdateStatusManager.EXTRA_REASON_FOR_CHANGE));
                    break;

                case AppUpdateStatusManager.BROADCAST_APPSTATUS_ADDED:
                    onAppStatusAdded(apkUrl);
                    break;

                case AppUpdateStatusManager.BROADCAST_APPSTATUS_REMOVED:
                    onAppStatusRemoved(apkUrl);
                    break;
            }
        }
    };

}
