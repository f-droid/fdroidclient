package org.fdroid.fdroid.views.updates;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import org.fdroid.fdroid.R;

public class UpdatesViewBinder {

    private final UpdatesAdapter adapter;
    private final RecyclerView list;
    private final TextView emptyState;
    private final ImageView emptyImage;

    public UpdatesViewBinder(AppCompatActivity activity, FrameLayout parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.main_tab_updates, parent, true);

        adapter = new UpdatesAdapter(activity);
        adapter.registerAdapterDataObserver(adapterChangeListener);

        list = (RecyclerView) view.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new UpdatesItemTouchCallback(adapter));
        touchHelper.attachToRecyclerView(list);

        emptyState = (TextView) view.findViewById(R.id.empty_state);
        emptyImage = (ImageView) view.findViewById(R.id.image);
    }

    public void bind() {
        adapter.setIsActive();
    }

    public void unbind() {
        adapter.stopListeningForStatusUpdates();
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            list.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyImage.setVisibility(View.VISIBLE);
        } else {
            list.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            emptyImage.setVisibility(View.GONE);
        }
    }

    @SuppressWarnings("FieldCanBeLocal")
    private final RecyclerView.AdapterDataObserver adapterChangeListener = new RecyclerView.AdapterDataObserver() {
        @Override
        public void onChanged() {
            updateEmptyState();
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount) {
            updateEmptyState();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            updateEmptyState();
        }
    };
}
