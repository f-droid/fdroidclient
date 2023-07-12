package org.fdroid.fdroid.views.main;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import org.fdroid.fdroid.R;

public class LatestLayoutPolicy {
    private final Context context;

    public LatestLayoutPolicy(Context context) {
        this.context = context.getApplicationContext();
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return new ItemDecorator(context);
    }

    public int getItemViewType(int position) {
        int relativePositionInCycle = position % 5;

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

    public int getSpanSize(int position) {
        int relativePositionInCycle = position % 5;
        if (relativePositionInCycle == 0) {
            return 2;
        } else {
            return 1;
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
