package org.fdroid.fdroid.views.apps;

import android.content.Context;
import android.graphics.Rect;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

/**
 * Draws a faint line between items, to be used with the {@link AppListItemDivider}.
 */
public class AppListItemDivider extends DividerItemDecoration {
    private final int itemSpacing;

    public AppListItemDivider(Context context) {
        super(context, DividerItemDecoration.VERTICAL);
        setDrawable(ContextCompat.getDrawable(context, R.drawable.app_list_item_divider));
        itemSpacing = Utils.dpToPx(8, context);
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        super.getItemOffsets(outRect, view, parent, state);
        int position = parent.getChildAdapterPosition(view);
        if (position > 0) {
            outRect.bottom = itemSpacing;
        }
    }
}
