package org.fdroid.fdroid.views.apps;

import android.app.Activity;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

class AppListAdapter extends RecyclerView.Adapter<AppListItemController> {

    private Cursor cursor;
    private final Activity activity;

    AppListAdapter(Activity activity) {
        this.activity = activity;
    }

    public void setAppCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    @Override
    public AppListItemController onCreateViewHolder(ViewGroup parent, int viewType) {
        return new AppListItemController(activity, activity.getLayoutInflater().inflate(R.layout.app_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(AppListItemController holder, int position) {
        cursor.moveToPosition(position);
        holder.bindModel(new App(cursor));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }
}
