package org.fdroid.fdroid.views.main;

import android.content.Context;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;
import org.fdroid.fdroid.R;

public class LatestLayoutPolicy {
    private final Context context;

    public LatestLayoutPolicy(Context context) {
        this.context = context.getApplicationContext();
    }

    public RecyclerView.ItemDecoration getItemDecoration() {
        return new DividerItemDecoration(context, DividerItemDecoration.VERTICAL);
    }

    public int getItemViewType(int position) {
        return R.id.latest_regular_list;
    }

    public int getSpanSize(int position) {
        return 2;
    }
}
