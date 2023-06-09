package org.fdroid.fdroid.views.updates;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.Utils;

public class UpdatesViewBinder {

    private final UpdatesAdapter adapter;
    private final RecyclerView list;
    private final TextView emptyState;
    private final ImageView emptyImage;
    private final ProgressBar emptyUpdatingProgress;
    private final AppCompatActivity activity;

    public UpdatesViewBinder(final AppCompatActivity activity, FrameLayout parent) {
        this.activity = activity;
        View view = activity.getLayoutInflater().inflate(R.layout.main_tab_updates, parent, true);

        adapter = new UpdatesAdapter(activity);
        adapter.registerAdapterDataObserver(adapterChangeListener);

        list = view.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);

        ItemTouchHelper touchHelper = new ItemTouchHelper(new UpdatesItemTouchCallback(adapter));
        touchHelper.attachToRecyclerView(list);

        emptyState = view.findViewById(R.id.empty_state);
        emptyImage = view.findViewById(R.id.image);
        emptyUpdatingProgress = view.findViewById(R.id.empty_updating_progress);

        final SwipeRefreshLayout swipeToRefresh = view.findViewById(R.id.swipe_to_refresh);
        Utils.applySwipeLayoutColors(swipeToRefresh);
        swipeToRefresh.setOnRefreshListener(() -> {
            swipeToRefresh.setRefreshing(false);
            UpdateService.updateNow(activity);
        });

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
            emptyImage.setVisibility(View.VISIBLE);
            setUpEmptyUpdatingProgress(UpdateService.isUpdating());
            LocalBroadcastManager.getInstance(activity).registerReceiver(updateServiceStatusReceiver,
                    new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
        } else {
            list.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            emptyImage.setVisibility(View.GONE);
            LocalBroadcastManager.getInstance(activity).unregisterReceiver(updateServiceStatusReceiver);
            emptyUpdatingProgress.setVisibility(View.GONE);
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

    private final BroadcastReceiver updateServiceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Anything other than a STATUS_INFO broadcast signifies that it was complete
            boolean isUpdating = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, 0) == UpdateService.STATUS_INFO;
            setUpEmptyUpdatingProgress(isUpdating);
        }
    };

    private void setUpEmptyUpdatingProgress(boolean isUpdating) {
        if (isUpdating) {
            emptyState.setVisibility(View.GONE);
            emptyUpdatingProgress.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.VISIBLE);
            emptyUpdatingProgress.setVisibility(View.GONE);
        }
    }
}
