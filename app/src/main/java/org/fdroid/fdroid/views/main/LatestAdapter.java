package org.fdroid.fdroid.views.main;

import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.fdroid.database.AppOverviewItem;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.categories.AppCardController;

import java.util.List;

public class LatestAdapter extends RecyclerView.Adapter<AppCardController> {

    private List<AppOverviewItem> apps;
    private final AppCompatActivity activity;
    private final LatestLayoutPolicy layoutPolicy;
    private final RecyclerView.ItemDecoration appListDecorator;
    private final GridLayoutManager.SpanSizeLookup spanSizeLookup;

    LatestAdapter(AppCompatActivity activity) {
        this.activity = activity;
        layoutPolicy = new LatestLayoutPolicy(activity);
        appListDecorator = layoutPolicy.getItemDecoration();
        spanSizeLookup = new SpanSizeLookup(layoutPolicy);
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        recyclerView.addItemDecoration(appListDecorator);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        recyclerView.removeItemDecoration(appListDecorator);
        super.onDetachedFromRecyclerView(recyclerView);
    }

    @NonNull
    @Override
    public AppCardController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout;
        if (viewType == R.id.latest_large_tile) {
            layout = R.layout.app_card_large;
        } else if (viewType == R.id.latest_small_tile) {
            layout = R.layout.app_card_horizontal;
        } else if (viewType == R.id.latest_regular_list) {
            layout = R.layout.app_card_list_item;
        } else {
            throw new IllegalArgumentException("Unknown view type when rendering \"What's New\": " + viewType);
        }

        return new AppCardController(activity, activity.getLayoutInflater().inflate(layout, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        return layoutPolicy.getItemViewType(position);
    }

    @Override
    public void onBindViewHolder(@NonNull AppCardController holder, int position) {
        final AppOverviewItem app = apps.get(position);
        holder.bindApp(app);
    }

    @Override
    public int getItemCount() {
        return apps == null ? 0 : apps.size();
    }

    public void setApps(@Nullable List<AppOverviewItem> apps) {
        if (this.apps == apps) {
            // don't notify when the apps did not change
            return;
        }
        this.apps = apps;
        notifyDataSetChanged();
    }

    public GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
        return spanSizeLookup;
    }

    private static final class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        private final LatestLayoutPolicy layoutPolicy;

        private SpanSizeLookup(LatestLayoutPolicy layoutPolicy) {
            this.layoutPolicy = layoutPolicy;
        }

        @Override
        public int getSpanSize(int position) {
            return layoutPolicy.getSpanSize(position);
        }
    }
}
