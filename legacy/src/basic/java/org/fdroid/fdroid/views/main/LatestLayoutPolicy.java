package org.fdroid.fdroid.views.main;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.fdroid.R;

class LatestLayoutPolicy {
    private final Context context;

    LatestLayoutPolicy(Context context) {
        this.context = context.getApplicationContext();
    }

    RecyclerView.ItemDecoration getItemDecoration() {
        return new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                                       @NonNull RecyclerView.State state) {
                super.getItemOffsets(outRect, view, parent, state);
                Resources resources = context.getResources();
                int padding = (int) resources.getDimension(R.dimen.latest__padding__app_card__normal);
                outRect.set(padding, padding, padding, 0);
            }
        };
    }

    /** @noinspection unused*/
    int getItemViewType(int position) {
        return R.id.latest_regular_list;
    }

    /** @noinspection unused*/
    int getSpanSize(int position) {
        return 2;
    }
}
