package org.fdroid.fdroid.views.apps;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.data.Schema;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

class AppListAdapter extends RecyclerView.Adapter<StandardAppListItemController> {

    private Cursor cursor;
    private Runnable hasHiddenAppsCallback;
    private final AppCompatActivity activity;

    AppListAdapter(AppCompatActivity activity) {
        this.activity = activity;
        setHasStableIds(true);
    }

    public void setAppCursor(Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

    public void setHasHiddenAppsCallback(Runnable callback) {
        hasHiddenAppsCallback = callback;
    }

    @NonNull
    @Override
    public StandardAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new StandardAppListItemController(activity, activity.getLayoutInflater()
                .inflate(R.layout.app_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull StandardAppListItemController holder, int position) {
        cursor.moveToPosition(position);
        final App app = new App(cursor);
        holder.bindModel(app);

        if (app.isDisabledByAntiFeatures(activity)) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            0,
                            0
                    )
            );

            if (this.hasHiddenAppsCallback != null) {
                this.hasHiddenAppsCallback.run();
            }
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(
                    new RecyclerView.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
        }
    }

    @Override
    public long getItemId(int position) {
        cursor.moveToPosition(position);
        return cursor.getLong(cursor.getColumnIndex(Schema.AppMetadataTable.Cols.ROW_ID));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }
}
