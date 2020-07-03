package org.fdroid.fdroid.views.manager;

import android.app.Activity;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.App;

import java.util.Collections;
import java.util.Set;

public class CollectionAppListAdapter extends RecyclerView.Adapter<CollectionAppListItemController> {

    protected final Activity activity;
    private final FragmentCollection mFragment;

    private final String TAG = "CollectAppListAdapter";

    private boolean boxVisibly = false;
    private boolean reloaded = false;

    private RecyclerView mRecyclerView;
    private AppManagerActionMode AMActionMode;

    @Nullable
    private Cursor cursor;

    public CollectionAppListAdapter(Activity activity, FragmentCollection fragment) {
        this.activity = activity;
        this.mFragment = fragment;
        setHasStableIds(true);
    }

    @NonNull
    @Override
    public CollectionAppListItemController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = activity.getLayoutInflater().inflate(R.layout.fragment_app_manager_list_item, parent, false);
        if (mFragment != null) {
            view.setOnLongClickListener(onAppLongClicked);
        }
        return new CollectionAppListItemController(activity, view);
    }

    @Override
    public void onBindViewHolder(@NonNull CollectionAppListItemController holder, int position) {
        if (cursor == null) {
            return;
        }

        holder.setBoxVisibly(boxVisibly);

        cursor.moveToPosition(position);
        App app = new App(cursor);
        holder.bindModel(app);

        if (app.collectionHidden != null) {
            if (app.collectionHidden.equals("1")) holder.itemView.setAlpha(0.5f);
            else holder.itemView.setAlpha(1);
        }

        if (AMActionMode == null) {
            holder.resetOnClick();
        }

        if (position >= getItemCount() - 1) {
            if (!reloaded) {
                reloaded = true;
                //FIXME This is awful. Must find a better way to show alpha effect.
                new android.os.Handler().postDelayed(
                        new Runnable() {
                            public void run() {
                                notifyDataSetChanged();
                            }
                        },
                        500
                );
            } else {
                reloaded = false;
            }
        }
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

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        this.mRecyclerView = recyclerView;
    }


    private final View.OnLongClickListener onAppLongClicked = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {

            if (AMActionMode == null) {

                int itemPosition = mRecyclerView.getChildAdapterPosition(view);

                Preferences.get().setPanicTmpSelectedSet(Collections.<String>emptySet()); // set empty entry
                Set<String> wipeSet = Preferences.get().getPanicTmpSelectedSet(); // read empty entry
                wipeSet.add(getItem(itemPosition).packageName); // add an entry
                Preferences.get().setPanicTmpSelectedSet(wipeSet); // set new entry

                AMActionMode = new AppManagerActionMode(activity, mFragment.getContext(), 1) {

                    public void onCreateAction() {
                        boxVisibly = true;
                        notifyDataSetChanged();
                    }

                    @Override
                    public void onDestroyAction() {
                        AMActionMode = null;
                        boxVisibly = false;
                        notifyDataSetChanged();
                        mFragment.onResume();
                    }
                };

            }

            return true;
        }

    };

    public void closeActionMode() {
        if (AMActionMode != null) {
            AMActionMode.closeActionMode();
        }
    }
}