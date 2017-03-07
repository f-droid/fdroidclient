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

    private final SparseIntArray positionToId = new SparseIntArray();

    private final AppCompatActivity activity;

    MainViewAdapter(AppCompatActivity activity) {
        this.activity = activity;
        setHasStableIds(true);
        positionToId.put(0, R.id.whats_new);
        positionToId.put(1, R.id.categories);
        positionToId.put(2, R.id.nearby);
        positionToId.put(3, R.id.my_apps);
        positionToId.put(4, R.id.settings);
    }

    @Override
    public MainViewController onCreateViewHolder(ViewGroup parent, int viewType) {
        FrameLayout frame = new FrameLayout(activity);
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new MainViewController(activity, frame);
    }

    @Override
    public void onBindViewHolder(MainViewController holder, int position) {
        long menuId = getItemId(position);
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
        return positionToId.size();
    }

    // The RecyclerViewPager and the BottomNavigationView both use menu item IDs to identify pages.
    @Override
    public long getItemId(int position) {
        return positionToId.get(position);
    }

    public int adapterPositionFromItemId(int itemId) {
        return positionToId.indexOfValue(itemId);
    }
}
