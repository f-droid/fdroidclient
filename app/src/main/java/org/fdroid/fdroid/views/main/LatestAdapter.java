package org.fdroid.fdroid.views.main;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.database.AppOverviewItem;
import org.fdroid.fdroid.BuildConfig;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.categories.AppCardController;

import java.util.List;

public class LatestAdapter extends RecyclerView.Adapter<AppCardController> {

    private List<AppOverviewItem> apps;
    private final AppCompatActivity activity;
    private final RecyclerView.ItemDecoration appListDecorator;

    LatestAdapter(AppCompatActivity activity) {
        this.activity = activity;
        appListDecorator = new LatestAdapter.ItemDecorator(activity);
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
        switch (viewType) {
            case R.id.latest_large_tile:
                layout = R.layout.app_card_large;
                break;
            case R.id.latest_small_tile:
                layout = R.layout.app_card_horizontal;
                break;
            case R.id.latest_regular_list:
                layout = R.layout.app_card_list_item;
                break;
            default:
                throw new IllegalArgumentException("Unknown view type when rendering \"What's New\": " + viewType);
        }

        return new AppCardController(activity, activity.getLayoutInflater().inflate(layout, parent, false));
    }

    @Override
    public int getItemViewType(int position) {
        int relativePositionInCycle = position % 5;

        if (BuildConfig.FLAVOR.startsWith("basic")) {
            if (relativePositionInCycle > 0) {
                return R.id.latest_small_tile;
            } else {
                return R.id.latest_regular_list;
            }
        }

        if (position == 0) {
            return R.id.latest_regular_list;
        } else {
            switch (relativePositionInCycle) {
                case 1:
                case 2:
                    return R.id.latest_large_tile;

                case 3:
                case 4:
                    return R.id.latest_small_tile;

                case 0:
                default:
                    return R.id.latest_regular_list;
            }
        }
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
            //don't notify when the apps did not change
            return;
        }
        this.apps = apps;
        notifyDataSetChanged();
    }

    public static class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            int relativePositionInCycle = position % 5;
            if (relativePositionInCycle == 0) {
                return 2;
            } else {
                return 1;
            }
        }
    }

    /**
     * Applies padding to items, ensuring that the spacing on the left, centre, and right all match.
     * The vertical padding is slightly shorter than the horizontal padding also.
     *
     * @see org.fdroid.fdroid.R.dimen#latest__padding__app_card__horizontal
     * @see org.fdroid.fdroid.R.dimen#latest__padding__app_card__vertical
     */
    private static class ItemDecorator extends RecyclerView.ItemDecoration {
        private final Context context;

        ItemDecorator(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, RecyclerView parent,
                                   @NonNull RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            Resources resources = context.getResources();
            int horizontalPadding = (int) resources.getDimension(R.dimen.latest__padding__app_card__horizontal);
            int verticalPadding = (int) resources.getDimension(R.dimen.latest__padding__app_card__vertical);

            int relativePositionInCycle = position % 5;
            if (position == 0) {
                outRect.set(horizontalPadding, verticalPadding, horizontalPadding, verticalPadding);
            } else if (relativePositionInCycle != 0) {
                // The item on the left will have both left and right padding. The item on the right
                // will only have padding on the right. This will allow the same amount of padding
                // on the left, centre, and right of the grid, rather than double the padding in the
                // middle (which would happen if both left and right padding was set for both items).
                boolean isLtr = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR;
                boolean isAtStart = relativePositionInCycle == 1 || relativePositionInCycle == 3;
                int paddingStart = isAtStart ? horizontalPadding : 0;
                int paddingLeft = isLtr ? paddingStart : horizontalPadding;
                int paddingRight = isLtr ? horizontalPadding : paddingStart;
                outRect.set(paddingLeft, 0, paddingRight, verticalPadding);
            } else {
                outRect.set(horizontalPadding, 0, horizontalPadding, verticalPadding);
            }
        }
    }
}
