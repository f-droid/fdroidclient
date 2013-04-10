package org.fdroid.fdroid.views;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;

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
        final AppListViewFactory viewFactory = new AppListViewFactory(parent);

        fragments[0] = new Fragment() {
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return viewFactory.createAvailableView();
            }
        };

        fragments[1] = new Fragment() {
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return viewFactory.createInstalledView();
            }
        };

        fragments[2] = new Fragment() {
            public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
                return viewFactory.createCanUpdateView();
            }
        };
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
