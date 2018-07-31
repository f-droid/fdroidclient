package org.fdroid.fdroid.views.updates.items;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.hannesdorfmann.adapterdelegates3.AdapterDelegate;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.views.updates.UpdatesAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of all apps that can be downloaded. Includes a button to download all of them and also
 * a toggle to show or hide the list of each individual item.
 *
 * @see R.layout#updates_header The view that this binds to.
 * @see UpdateableAppsHeader The data that is bound to this view.
 */
public class UpdateableAppsHeader extends AppUpdateData {

    public final List<UpdateableApp> apps;
    public final UpdatesAdapter adapter;

    public UpdateableAppsHeader(Activity activity,
                                UpdatesAdapter updatesAdapter, List<UpdateableApp> updateableApps) {
        super(activity);
        apps = updateableApps;
        adapter = updatesAdapter;
    }

    public static class Delegate extends AdapterDelegate<List<AppUpdateData>> {

        private final LayoutInflater inflater;

        public Delegate(Activity activity) {
            inflater = activity.getLayoutInflater();
        }

        @Override
        protected boolean isForViewType(@NonNull List<AppUpdateData> items, int position) {
            return items.get(position) instanceof UpdateableAppsHeader;
        }

        @NonNull
        @Override
        protected RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
            return new ViewHolder(inflater.inflate(R.layout.updates_header, parent, false));
        }

        @Override
        protected void onBindViewHolder(@NonNull List<AppUpdateData> items, int position,
                                        @NonNull RecyclerView.ViewHolder holder, @NonNull List<Object> payloads) {
            UpdateableAppsHeader app = (UpdateableAppsHeader) items.get(position);
            ((ViewHolder) holder).bindHeader(app);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private UpdateableAppsHeader header;

        private final TextView updatesAvailable;
        private final ImageView downloadAll;
        private final TextView appsToUpdate;
        private final Button toggleAppsToUpdate;

        public ViewHolder(View itemView) {
            super(itemView);

            updatesAvailable = (TextView) itemView.findViewById(R.id.text_updates_available);
            downloadAll = (ImageView) itemView.findViewById(R.id.button_download_all);
            appsToUpdate = (TextView) itemView.findViewById(R.id.text_apps_to_update);
            toggleAppsToUpdate = (Button) itemView.findViewById(R.id.button_toggle_apps_to_update);

            toggleAppsToUpdate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    header.adapter.toggleAllUpdateableApps();
                    updateToggleButtonText();
                }
            });

            downloadAll.setVisibility(View.VISIBLE);
            downloadAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    downloadAll.setVisibility(View.GONE);
                    UpdateService.autoDownloadUpdates(header.activity);
                }
            });
        }

        public void bindHeader(UpdateableAppsHeader header) {
            this.header = header;

            updatesAvailable.setText(itemView.getResources()
                    .getQuantityString(R.plurals.updates__download_updates_for_apps, header.apps.size(),
                            header.apps.size()));

            List<String> appNames = new ArrayList<>(header.apps.size());
            for (UpdateableApp app : header.apps) {
                appNames.add(app.app.name);
            }

            appsToUpdate.setText(TextUtils.join(", ", appNames));
            updateToggleButtonText();
        }

        private void updateToggleButtonText() {
            if (header.adapter.canViewAllUpdateableApps()) {
                toggleAppsToUpdate.setText(R.string.updates__hide_updateable_apps);
            } else {
                toggleAppsToUpdate.setText(R.string.updates__show_updateable_apps);
            }
        }
    }

}
