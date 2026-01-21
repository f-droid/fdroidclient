package org.fdroid.fdroid.views.installed;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.AppListItem;
import org.fdroid.database.AppPrefs;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

import java.util.ArrayList;
import java.util.List;

public class InstalledAppListAdapter extends RecyclerView.Adapter<InstalledAppListItemController> {

    protected final AppCompatActivity activity;

    private final List<App> items = new ArrayList<>();

    protected InstalledAppListAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public InstalledAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.installed_app_list_item, parent, false);
        return new InstalledAppListItemController(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppListItemController holder, int position) {
        App app = items.get(position);
        holder.bindModel(app, null, null);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void setApps(@NonNull List<AppListItem> items) {
        this.items.clear();
        for (AppListItem item : items) {
            this.items.add(new App(item));
        }
        notifyDataSetChanged();
    }

    public void updateItem(AppListItem item, AppPrefs appPrefs) {
        for (int i = 0; i < items.size(); i++) {
            App app = items.get(i);
            if (app.packageName.equals(item.getPackageName())) {
                app.prefs = appPrefs;
                notifyItemChanged(i);
                break;
            }
        }
    }

    @Nullable
    App getItem(int position) {
        return items.get(position);
    }
}
