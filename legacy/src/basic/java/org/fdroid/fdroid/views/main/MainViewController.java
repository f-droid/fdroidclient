/*
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

import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.PreferencesFragment;
import org.fdroid.fdroid.views.updates.UpdatesViewBinder;

/**
 * Decides which view on the main screen to attach to a given {@link FrameLayout}. This class
 * doesn't know which view it will be rendering at the time it is constructed. Rather, at some
 * point in the future the {@link MainViewAdapter} will have information about which view we
 * are required to render, and will invoke the relevant "bind*()" method on this class.
 */
class MainViewController extends RecyclerView.ViewHolder {

    private final AppCompatActivity activity;
    private final FrameLayout frame;

    @Nullable
    private UpdatesViewBinder updatesView = null;

    MainViewController(AppCompatActivity activity, FrameLayout frame) {
        super(frame);
        this.activity = activity;
        this.frame = frame;
    }

    /**
     * @see LatestViewBinder
     */
    void bindLatestView() {
        new LatestViewBinder(activity, frame);
    }

    /**
     * @see UpdatesViewBinder
     */
    void bindUpdates() {
        if (updatesView == null) {
            updatesView = new UpdatesViewBinder(activity, frame);
        }

        updatesView.bind();
    }

    void unbindUpdates() {
        if (updatesView != null) {
            updatesView.unbind();
        }
    }

    /**
     * @see CategoriesViewBinder
     */
    void bindCategoriesView() {
        new CategoriesViewBinder(activity, frame);
    }

    void bindSwapView() {
        throw new IllegalStateException("unimplemented");
    }

    /**
     * Attaches a {@link PreferencesFragment} to the view. Everything else is managed by the
     * fragment itself, so no further work needs to be done by this view binder.
     * <p>
     * Note: It is tricky to attach a {@link Fragment} to a view from this view holder. This is due
     * to the way in which the {@link RecyclerView} will reuse existing views and ask us to
     * put a settings fragment in there at arbitrary times. Usually it won't be the same view we
     * attached the fragment to last time, which causes weirdness. The solution is to use code from
     * the com.lsjwzh.widget.recyclerviewpager.FragmentStatePagerAdapter which manages this.
     * The code has been ported to {@link SettingsView}.
     *
     * @see SettingsView
     */
    void bindSettingsView() {
        activity.getLayoutInflater().inflate(R.layout.main_tab_settings, frame, true);
    }
}
