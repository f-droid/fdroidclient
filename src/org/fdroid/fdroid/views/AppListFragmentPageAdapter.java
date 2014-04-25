package org.fdroid.fdroid.views;

import android.database.Cursor;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import org.fdroid.fdroid.FDroid;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.AppProvider;
import org.fdroid.fdroid.views.fragments.AvailableAppsFragment;
import org.fdroid.fdroid.views.fragments.CanUpdateAppsFragment;
import org.fdroid.fdroid.views.fragments.InstalledAppsFragment;

/**
 * Used by the FDroid activity in conjunction with its ViewPager to support
 * swiping of tabs for both old devices (< 3.0) and new devices.
 */
public class AppListFragmentPageAdapter extends FragmentPagerAdapter {

    private FDroid parent = null;

    public AppListFragmentPageAdapter(FDroid parent) {
        super(parent.getSupportFragmentManager());
        this.parent = parent;
    }

    private String getUpdateTabTitle() {
        Uri uri = AppProvider.getCanUpdateUri();
        String[] projection = new String[] { AppProvider.DataColumns._COUNT };
        Cursor cursor = parent.getContentResolver().query(uri, projection, null, null, null);
        String suffix = "";
        if (cursor != null && cursor.getCount() == 1) {
            cursor.moveToFirst();
            int count = cursor.getInt(0);
            suffix = " (" + count + ")";
        }
        return parent.getString(R.string.tab_updates) + suffix;
    }

    @Override
    public Fragment getItem(int i) {
        if ( i == 0 ) {
            return new AvailableAppsFragment();
        }
        if ( i == 1 ) {
            return new InstalledAppsFragment();
        }
        return new CanUpdateAppsFragment();
    }

    @Override
    public int getCount() {
        return 3;
    }

    @Override
    public String getPageTitle(int i) {
        switch(i) {
            case 0:
                return parent.getString(R.string.tab_noninstalled);
            case 1:
                return parent.getString(R.string.inst);
            case 2:
                return getUpdateTabTitle();
            default:
                return "";
        }
    }

}
