package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.hannesdorfmann.adapterdelegates3.AdapterDelegate;

import java.util.List;

/**
 * Each of these apps has a notification to display to the user.
 * The notification will have come from the apps metadata, provided by its maintainer. It may be
 * something about the app being removed from the repository, or perhaps security problems that
 * were identified in the app.
 */
public class AppNotification extends AppUpdateData {

    public AppNotification(Activity activity) {
        super(activity);
    }

    public static class Delegate extends AdapterDelegate<List<AppUpdateData>> {

        @Override
        protected boolean isForViewType(@NonNull List<AppUpdateData> items, int position) {
            return items.get(position) instanceof AppNotification;
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
            return new ViewHolder(new TextView(parent.getContext()));
        }

        @Override
        protected void onBindViewHolder(@NonNull List<AppUpdateData> items, int position, @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
            AppNotification app = (AppNotification) items.get(position);
            ((ViewHolder) holder).bindApp(app);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
        }

        public void bindApp(AppNotification app) {
            ((TextView) itemView).setText("");
        }
    }

}
