package org.fdroid.fdroid.views.myapps;

import android.app.Activity;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;
import org.fdroid.fdroid.views.apps.AppListItemController;

/**
 * Wraps a cursor which should have a list of "apps which can be updated". Also includes a header
 * as the first element which allows for all items to be updated.
 */
public class MyAppsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Cursor updatesCursor;
    private final Activity activity;

    public MyAppsAdapter(Activity activity) {
        this.activity = activity;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = activity.getLayoutInflater();
        switch (viewType) {
            case R.id.my_apps__header:
                return new UpdatesHeaderController(activity, inflater.inflate(R.layout.my_apps_updates_header, parent, false));

            case R.id.my_apps__app:
                return new AppListItemController(activity, inflater.inflate(R.layout.app_list_item, parent, false));

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getItemCount() {
        return updatesCursor == null ? 0 : updatesCursor.getCount() + 1;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (getItemViewType(position)) {
            case R.id.my_apps__header:
                ((UpdatesHeaderController) holder).bindModel(updatesCursor.getCount());
                break;

            case R.id.my_apps__app:
                updatesCursor.moveToPosition(position - 1); // Subtract one to account for the header.
                ((AppListItemController) holder).bindModel(new App(updatesCursor));
                break;

            default:
                throw new IllegalArgumentException();
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return R.id.my_apps__header;
        } else {
            return R.id.my_apps__app;
        }
    }

    public void setApps(Cursor cursor) {
        updatesCursor = cursor;
        notifyDataSetChanged();
    }
}
