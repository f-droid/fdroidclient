package org.fdroid.fdroid.views.apps;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.AppListItem;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

import java.util.ArrayList;
import java.util.List;

class AppListAdapter extends RecyclerView.Adapter<StandardAppListItemController> {

    private final List<AppListItem> items = new ArrayList<>();
    private Runnable hasHiddenAppsCallback;
    private final AppCompatActivity activity;

    AppListAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    void setItems(List<AppListItem> items) {
        this.items.clear();
        this.items.addAll(items);
        notifyDataSetChanged();
    }

    void setHasHiddenAppsCallback(Runnable callback) {
        hasHiddenAppsCallback = callback;
    }

    @NonNull
    @Override
    public StandardAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StandardAppListItemController(activity, activity.getLayoutInflater()
                .inflate(R.layout.app_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StandardAppListItemController holder, int position) {
        AppListItem appItem = items.get(position);
        final App app = new App(appItem);
        holder.bindModel(app, null, null);

        if (app.isDisabledByAntiFeatures(activity)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            0,
                            0
                    )
            );

            if (this.hasHiddenAppsCallback != null) {
                this.hasHiddenAppsCallback.run();
            }
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }
}
