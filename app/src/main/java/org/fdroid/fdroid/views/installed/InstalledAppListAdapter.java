package org.fdroid.fdroid.views.installed;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Schema;

class InstalledAppListAdapter extends RecyclerView.Adapter<InstalledAppListItemController> {

    private final Activity activity;

    @Nullable
    private Cursor cursor;

    InstalledAppListAdapter(Activity activity) {
        this.activity = activity;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (cursor == null) {
            return 0;
        }

        cursor.moveToPosition(position);
        return cursor.getLong(cursor.getColumnIndex(Schema.AppMetadataTable.Cols.ROW_ID));
    }

    @Override
    public InstalledAppListItemController onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.installed_app_list_item, parent, false);
        return new InstalledAppListItemController(activity, view);
    }

    @Override
    public void onBindViewHolder(InstalledAppListItemController holder, int position) {
        if (cursor == null) {
            return;
        }

        cursor.moveToPosition(position);
        holder.bindModel(new App(cursor));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public void setApps(@Nullable Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }
}
