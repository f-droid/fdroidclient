package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import com.hannesdorfmann.adapterdelegates3.AdapterDelegate;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.AppListItemController;

import java.util.List;

/**
 * Apps that were scheduled for download while the user was offline.
 * @see R.layout#updateable_app_status_item The view that this binds to
 * @see AppListItemController Used for binding the {@link App} to the {@link R.layout#updateable_app_status_item}
 */
public class PendingDownload extends AppUpdateData {

    public final App app;

    public PendingDownload(Activity activity, App app) {
        super(activity);
        this.app = app;
    }

    public static class Delegate extends AdapterDelegate<List<AppUpdateData>> {

        private final Activity activity;

        public Delegate(Activity activity) {
            this.activity = activity;
        }

        @Override
        protected boolean isForViewType(@NonNull List<AppUpdateData> items, int position) {
            return items.get(position) instanceof PendingDownload;
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
            View view = activity.getLayoutInflater().inflate(R.layout.updateable_app_status_item, parent, false);
            return new PendingDownloadAppListItemController(activity, view);
        }

        @Override
        protected void onBindViewHolder(@NonNull List<AppUpdateData> items, int position,
                                        @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
            PendingDownload pendingDownload = (PendingDownload) items.get(position);
            ((AppListItemController) holder).bindModel(pendingDownload.app);
        }
    }

}
