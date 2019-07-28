package org.fdroid.fdroid.views.installed;

import android.app.Activity;
import android.database.Cursor;
import android.provider.BaseColumns;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

public class InstalledAppListAdapter extends RecyclerView.Adapter<InstalledAppListItemController> {

    protected final Activity activity;

    @Nullable
    private Cursor cursor;

    protected InstalledAppListAdapter(Activity activity) {
        this.activity = activity;
        setHasStableIds(true);
    }

    @Override
    public long getItemId(int position) {
        if (cursor == null) {
            return 0;
        }

        cursor.moveToPosition(position);
        // TODO this should be based on Schema.InstalledAppProvider.Cols._ID
        return cursor.getLong(cursor.getColumnIndex(BaseColumns._ID));
    }

    @NonNull
    @Override
    public InstalledAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.installed_app_list_item, parent, false);
        return new InstalledAppListItemController(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull InstalledAppListItemController holder, int position) {
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

    @Nullable
    public App getItem(int position) {
        if (cursor == null) {
            return null;
        }
        cursor.moveToPosition(position);
        return new App(cursor);
    }
}
