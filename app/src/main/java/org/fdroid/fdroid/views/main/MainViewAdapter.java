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
        MainViewController holder = createEmptyView();
        switch (viewType) {
            case R.id.whats_new:
                holder.bindWhatsNewView();
                break;
            case R.id.categories:
                holder.bindCategoriesView();
                break;
            case R.id.nearby:
                holder.bindSwapView();
                break;
            case R.id.my_apps:
                holder.bindMyApps();
                break;
            case R.id.settings:
                holder.bindSettingsView();
                break;
            default:
                throw new IllegalStateException("Unknown view type " + viewType);
        }
        return holder;
    }

    private MainViewController createEmptyView() {
        FrameLayout frame = new FrameLayout(activity);
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return new MainViewController(activity, frame);
    }

    @Override
    public void onBindViewHolder(MainViewController holder, int position) {
        // The binding happens in onCreateViewHolder. This is because we never have more than one of
        // each type of view in this main activity. Therefore, there is no benefit to re-binding new
        // data each time we navigate back to an item, as the recycler view will just use the one we
        // created earlier.
    }

    @Override
    public int getItemCount() {
        return positionToId.size();
    }

    @Override
    public int getItemViewType(int position) {
        return positionToId.get(position);
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
