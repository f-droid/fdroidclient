package org.fdroid.fdroid.views.main;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * Represents the five main views that are accessible from the main view. These are:
 *  + Whats new
 *  + Categories
 *  + Nearby
 *  + My Apps
 *  + Settings
 *
 *  It is responsible for understanding the relationship between each main view that is reachable
 *  from the bottom navigation, and its position.
 *
 *  It doesn't need to do very much other than redirect requests from the {@link MainActivity}s
 *  {@link RecyclerView} to the relevant "bind*()" method
 *  of the {@link MainViewController}.
 */
class MainViewAdapter extends RecyclerView.Adapter<MainViewController> {

    private final AppCompatActivity activity;

    MainViewAdapter(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public MainViewController onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout frame = new FrameLayout(activity);
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new MainViewController(activity, frame);
    }

    @Override
    public void onBindViewHolder(MainViewController holder, int position) {
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
