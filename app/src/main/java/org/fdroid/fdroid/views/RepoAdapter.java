package org.fdroid.fdroid.views;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.Repo;

/**
 * Represents a collection of repositories that the F-Droid client knows about.
 * Wraps a {@link Cursor} from the {@link org.fdroid.fdroid.data.Schema.RepoTable} table.
 * @see RepoItemController
 */
public class RepoAdapter extends RecyclerView.Adapter<RepoItemController> {

    public static final int SHOW_REPO_DETAILS = 1;

    public interface EnabledListener {
        void onSetEnabled(Repo repo, boolean isEnabled);
    }

    @NonNull
    private final EnabledListener enabledListener;

    @Nullable
    private Cursor cursor;

    @NonNull
    private final Activity activity;

    public RepoAdapter(@NonNull Activity activity, @NonNull EnabledListener listener) {
        this.activity = activity;
        enabledListener = listener;
    }

    @Override
    public RepoItemController onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RepoItemController(activity, activity.getLayoutInflater().inflate(R.layout.repo_item, parent, false), enabledListener);
    }

    @Override
    public void onBindViewHolder(RepoItemController holder, int position) {
        if (cursor == null) {
            return;
        }

        cursor.moveToPosition(position);
        holder.bindRepo(new Repo(cursor));
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    public void setCursor(@Nullable Cursor cursor) {
        this.cursor = cursor;
        notifyDataSetChanged();
    }

}
