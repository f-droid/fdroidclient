/*
 * Copyright (C) 2016-2017 Peter Serwylo
 * Copyright (C) 2017 Mikael von Pfaler
 * Copyright (C) 2018 Senecto Limited
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301, USA.
 */

package org.fdroid.fdroid.views.main;

import android.util.SparseIntArray;
import android.view.Menu;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.fdroid.R;

/**
 * Represents the main views that are accessible from the main screen via each
 * tab.  They are set and loaded dynamically from {@code menu/main_activity_screens.xml}
 * This class is responsible for understanding the relationship between each
 * tab view that is reachable from the bottom navigation, and its position.
 * <p>
 * It doesn't need to do very much other than redirect requests from the {@link MainActivity}s
 * {@link RecyclerView} to the relevant "bind*()" method
 * of the {@link MainViewController}.
 * <p>
 * {@link PopupMenu} is used as a hack to get a disposable {@link Menu} instance
 * for parsing and reading the menu XML.
 */
class MainViewAdapter extends RecyclerView.Adapter<MainViewController> {

    private final SparseIntArray positionToId;

    private final AppCompatActivity activity;

    MainViewAdapter(AppCompatActivity activity) {
        this.activity = activity;
        setHasStableIds(true);

        PopupMenu p = new PopupMenu(activity, null);
        Menu menu = p.getMenu();
        activity.getMenuInflater().inflate(R.menu.main_activity_screens, menu);
        positionToId = new SparseIntArray(menu.size());
        for (int i = 0; i < menu.size(); i++) {
            positionToId.append(i, menu.getItem(i).getItemId());
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull MainViewController holder) {
        long viewType = getItemId(holder.getAbsoluteAdapterPosition());
        if (viewType == R.id.updates) {
            holder.unbindUpdates();
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull MainViewController holder) {
        long viewType = getItemId(holder.getAbsoluteAdapterPosition());
        if (viewType == R.id.updates) {
            holder.bindUpdates();
        } else if (viewType == R.id.nearby) {
            NearbyViewBinder.updateUsbOtg(activity);
        }
    }

    @NonNull
    @Override
    public MainViewController onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        MainViewController holder = createEmptyView(activity);
        switch (viewType) {
            case R.id.latest:
                holder.bindLatestView();
                break;
            case R.id.categories:
                holder.bindCategoriesView();
                break;
            case R.id.nearby:
                holder.bindSwapView();
                break;
            case R.id.updates:
                // Hold of until onViewAttachedToWindow, because that is where we want to start listening
                // for broadcast events (which is what the data binding does).
                break;
            case R.id.settings:
                holder.bindSettingsView();
                break;
            default:
                throw new IllegalStateException("Unknown view type " + viewType);
        }
        return holder;
    }

    private static MainViewController createEmptyView(AppCompatActivity activity) {
        FrameLayout frame = new FrameLayout(activity);
        frame.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        return new MainViewController(activity, frame);
    }

    @Override
    public void onBindViewHolder(@NonNull MainViewController holder, int position) {
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

    int adapterPositionFromItemId(int itemId) {
        return positionToId.indexOfValue(itemId);
    }
}
