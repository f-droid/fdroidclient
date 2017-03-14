package org.fdroid.fdroid.views.updates;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;

public class UpdatesViewBinder {

    private final UpdatesAdapter adapter;

    public UpdatesViewBinder(AppCompatActivity activity, FrameLayout parent) {
        View view = activity.getLayoutInflater().inflate(R.layout.main_tab_updates, parent, true);

        adapter = new UpdatesAdapter(activity);

        RecyclerView list = (RecyclerView) view.findViewById(R.id.list);
        list.setHasFixedSize(true);
        list.setLayoutManager(new LinearLayoutManager(activity));
        list.setAdapter(adapter);
    }

    public void bind() {
        adapter.listenForStatusUpdates();
    }

    public void unbind() {
        adapter.stopListeningForStatusUpdates();
    }
}
