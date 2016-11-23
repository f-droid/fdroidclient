package org.fdroid.fdroid.views.whatsnew;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.categories.AppCardController;

public class WhatsNewAdapter extends RecyclerView.Adapter<AppCardController> {

    private Cursor cursor;
    private final Activity activity;

    public WhatsNewAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public AppCardController onCreateViewHolder(ViewGroup parent, int viewType) {
        int layout;
        if (viewType == R.id.whats_new_feature) {
            layout = R.layout.app_card_featured;
        } else if (viewType == R.id.whats_new_large_tile) {
            layout = R.layout.app_card_large;
        } else if (viewType == R.id.whats_new_small_tile) {
            layout = R.layout.app_card_horizontal;
        } else if (viewType == R.id.whats_new_regular_list) {
            layout = R.layout.app_card_list_item;
        } else {
            throw new IllegalArgumentException("Unknown view type when rendering \"Whats New\": " + viewType);
        }

        return new AppCardController(activity, activity.getLayoutInflater().inflate(layout, parent, false));

    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return R.id.whats_new_feature;
        } else if (position <= 2) {
            return R.id.whats_new_large_tile;
        } else if (position <= 4) {
            return R.id.whats_new_small_tile;
        } else {
            return R.id.whats_new_regular_list;
        }
    }

    @Override
    public void onBindViewHolder(AppCardController holder, int position) {
        cursor.moveToPosition(position);
        holder.bindApp(new App(cursor));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public void setAppsCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    // TODO: Replace with https://github.com/lucasr/twoway-view which looks really really cool, but
    //       no longer under active development (despite heaps of forks/stars on github).
    public static class SpanSizeLookup extends GridLayoutManager.SpanSizeLookup {
        @Override
        public int getSpanSize(int position) {
            if (position == 0) {
                return 2;
            } else if (position <= 4) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    /**
     * Applies padding to items, ensuring that the spacing on the left, centre, and right all match.
     * The vertical padding is slightly shorter than the horizontal padding also.
     * @see org.fdroid.fdroid.R.dimen#whats_new__padding__app_card__horizontal
     * @see org.fdroid.fdroid.R.dimen#whats_new__padding__app_card__vertical
     */
    public static class ItemDecorator extends RecyclerView.ItemDecoration {
        private final Context context;

        public ItemDecorator(Context context) {
            this.context = context.getApplicationContext();
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view);
            int horizontalPadding = (int) context.getResources().getDimension(R.dimen.whats_new__padding__app_card__horizontal);
            int verticalPadding = (int) context.getResources().getDimension(R.dimen.whats_new__padding__app_card__vertical);

            if (position == 0) {
                // Don't set any padding for the first item as the FeatureImage behind it needs to butt right
                // up against the left/top/right of the screen.
                outRect.set(0, 0, 0, verticalPadding);
            } else if (position <= 4) {
                // Odd items are on the left, even on the right.
                // The item on the left will have both left and right padding. The item on the right
                // will only have padding on the right. This will allow the same amount of padding
                // on the left, centre, and right of the grid, rather than double the padding in the
                // middle (which would happen if both left+right paddings were set for both items).
                boolean isLtr = ViewCompat.getLayoutDirection(parent) == ViewCompat.LAYOUT_DIRECTION_LTR;
                boolean isAtStart = (position % 2) == 1;
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
