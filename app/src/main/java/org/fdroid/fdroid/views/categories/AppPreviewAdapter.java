package org.fdroid.fdroid.views.categories;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.fdroid.R;

import java.util.Collections;
import java.util.List;

class AppPreviewAdapter extends RecyclerView.Adapter<AppCardController> {

    private List<AppOverviewItem> items = Collections.emptyList();
    private final AppCompatActivity activity;

    AppPreviewAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @NonNull
    @Override
    public AppCardController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AppCardController(activity, activity.getLayoutInflater()
                .inflate(R.layout.app_card_normal, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AppCardController holder, int position) {
        holder.bindApp(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void setAppCursor(List<AppOverviewItem> items) {
        if (this.items == items) {
            //don't notify when the cursor did not change
            return;
        }
        this.items = items;
        notifyDataSetChanged();
    }
}
