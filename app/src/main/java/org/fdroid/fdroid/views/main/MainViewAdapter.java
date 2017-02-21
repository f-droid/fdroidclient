package org.fdroid.fdroid.views.main;

import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.util.SparseIntArray;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import org.fdroid.fdroid.R;

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

    // Helpers for switching between the API of the RecyclerViewPager and the BottomNavigationView.
    // One identifies items by position, the other by menu item ID, yet they need to be able
    // to talk to and control each other. If the user swipes the view pager, the bottom nav needs
    // to update, and vice-versa.
    static final SparseIntArray ID_TO_POSITION = new SparseIntArray();
    static final SparseIntArray POSITION_TO_ID = new SparseIntArray();

    static {
        ID_TO_POSITION.put(R.id.whats_new, 0);
        ID_TO_POSITION.put(R.id.categories, 1);
        ID_TO_POSITION.put(R.id.nearby, 2);
        ID_TO_POSITION.put(R.id.my_apps, 3);
        ID_TO_POSITION.put(R.id.settings, 4);

        POSITION_TO_ID.put(0, R.id.whats_new);
        POSITION_TO_ID.put(1, R.id.categories);
        POSITION_TO_ID.put(2, R.id.nearby);
        POSITION_TO_ID.put(3, R.id.my_apps);
        POSITION_TO_ID.put(4, R.id.settings);
    }

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
        int menuId = POSITION_TO_ID.get(position);
        if (menuId == R.id.whats_new) {
            holder.bindWhatsNewView();
        } else if (menuId == R.id.categories) {
            holder.bindCategoriesView();
        } else if (menuId == R.id.nearby) {
            holder.bindSwapView();
        } else if (menuId == R.id.my_apps) {
            holder.bindMyApps();
        } else if (menuId == R.id.settings) {
            holder.bindSettingsView();
        } else {
            holder.clearViews();
        }
    }

    @Override
    public int getItemCount() {
        return 5;
    }
}
