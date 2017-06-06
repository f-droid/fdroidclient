package org.fdroid.fdroid.views.categories;

import android.app.Activity;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

class AppPreviewAdapter extends RecyclerView.Adapter<AppCardController> {

    private Cursor cursor;
    private final Activity activity;

    AppPreviewAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public AppCardController onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppCardController(activity, activity.getLayoutInflater()
                .inflate(R.layout.app_card_normal, parent, false));
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

    public void setAppCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }
}
