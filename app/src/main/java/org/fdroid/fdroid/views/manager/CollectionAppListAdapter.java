package org.fdroid.fdroid.views.manager;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

public class CollectionAppListAdapter extends RecyclerView.Adapter<CollectionAppListItemController> {

    protected final Activity activity;

    private final String TAG = "CollectAppListAdapter";

    @Nullable
    private Cursor cursor;

    public CollectionAppListAdapter(Activity activity, FragmentCollection fragment) {
        this.activity = activity;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public CollectionAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_app_manager_list_item, parent, false);
        return new CollectionAppListItemController(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionAppListItemController holder, int position) {
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