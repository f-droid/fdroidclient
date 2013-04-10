package org.fdroid.fdroid.views;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.views.fragments.AvailableAppsFragment;
import org.fdroid.fdroid.views.fragments.CanUpdateAppsFragment;
import org.fdroid.fdroid.views.fragments.InstalledAppsFragment;

/**
 * Used by the FDroid activity in conjunction with its ViewPager to support
 * swiping of tabs for both old devices (< 3.0) and new devices.
 */
public class AppListFragmentPageAdapter extends FragmentPagerAdapter {

    private FDroid parent;
    private Fragment[] fragments = new Fragment[3];

    public AppListFragmentPageAdapter(FDroid parent) {
        super(parent.getSupportFragmentManager());
        this.parent  = parent;
        fragments[0] = new AvailableAppsFragment();
        fragments[1] = new InstalledAppsFragment();
        fragments[2] = new CanUpdateAppsFragment();
    }

    @Override
    public Fragment getItem(int i) {
        return fragments[i];
    }

    @Override
    public int getCount() {
        return fragments.length;
    }

    public String getPageTitle(int i) {
        switch(i) {
            case 0:
                return parent.getString(R.string.tab_noninstalled);
            case 1:
                return parent.getString(R.string.tab_installed);
            case 2:
                String updates = parent.getString(R.string.tab_updates);
                updates += " (" + parent.getCanUpdateAdapter().getCount() + ")";
                return updates;
            default:
                return "";
        }
    }

}
