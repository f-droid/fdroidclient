package org.fdroid.fdroid.views;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.compat.TabManager;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.fragments.AppListFragment;
import org.fdroid.fdroid.views.fragments.AvailableAppsFragment;
import org.fdroid.fdroid.views.fragments.CanUpdateAppsFragment;
import org.fdroid.fdroid.views.fragments.InstalledAppsFragment;

/**
 * Used by the FDroid activity in conjunction with its ViewPager to support
 * swiping of tabs for both old devices (< 3.0) and new devices.
 */
public class AppListFragmentPagerAdapter extends FragmentPagerAdapter {

    @NonNull private final FDroid parent;

    @NonNull private final AppListFragment availableFragment;
    @NonNull private final AppListFragment installedFragment;
    @NonNull private final AppListFragment canUpdateFragment;

    public AppListFragmentPagerAdapter(@NonNull FDroid parent) {
        super(parent.getSupportFragmentManager());
        this.parent = parent;

        availableFragment = new AvailableAppsFragment();
        installedFragment = new InstalledAppsFragment();
        canUpdateFragment = new CanUpdateAppsFragment();
    }

    private String getInstalledTabTitle() {
        int installedCount = AppProvider.Helper.count(parent, AppProvider.getInstalledUri());
        return parent.getString(R.string.tab_installed_apps_count, installedCount);
    }

    private String getUpdateTabTitle() {
        int updateCount = AppProvider.Helper.count(parent, AppProvider.getCanUpdateUri());
        return parent.getString(R.string.tab_updates_count, updateCount);
    }

    public void updateSearchQuery(@Nullable String query) {
        availableFragment.updateSearchQuery(query);
        installedFragment.updateSearchQuery(query);
        canUpdateFragment.updateSearchQuery(query);
    }

    @Override
    public Fragment getItem(int i) {
        switch (i) {
            case TabManager.INDEX_AVAILABLE:
                return availableFragment;
            case TabManager.INDEX_INSTALLED:
                return installedFragment;
            default:
                return canUpdateFragment;
        }
    }

    @Override
    public int getCount() {
        return TabManager.INDEX_COUNT;
    }

    @Override
    public String getPageTitle(int i) {
        switch (i) {
            case TabManager.INDEX_AVAILABLE:
                return parent.getString(R.string.tab_available_apps);
            case TabManager.INDEX_INSTALLED:
                return getInstalledTabTitle();
            case TabManager.INDEX_CAN_UPDATE:
                return getUpdateTabTitle();
            default:
                return "";
        }
    }

}
