package org.fdroid.fdroid;

import java.util.*;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.os.Build;

import org.fdroid.fdroid.views.AppListAdapter;
import org.fdroid.fdroid.views.AvailableAppListAdapter;
import org.fdroid.fdroid.views.CanUpdateAppListAdapter;
import org.fdroid.fdroid.views.InstalledAppListAdapter;

/**
 * Should be owned by the FDroid Activity, but used by the AppListFragments.
 * The idea is that it takes a non-trivial amount of time to work this stuff
 * out, and it is quicker if we only do it once for each view, rather than
 * each fragment figuring out their own list independently.
 */
public class AppListManager {

    private List<DB.App> allApps = null;

    private FDroid fdroidActivity;

    private AppListAdapter availableApps;
    private AppListAdapter installedApps;
    private AppListAdapter canUpgradeApps;
    private ArrayAdapter<String> categories;

    private String currentCategory         = null;
    private String categoryAll             = null;
    private String categoryWhatsNew        = null;
    private String categoryRecentlyUpdated = null;

    public AppListAdapter getAvailableAdapter() {
        return availableApps;
    }

    public AppListAdapter getInstalledAdapter() {
        return installedApps;
    }

    public AppListAdapter getCanUpdateAdapter() {
        return canUpgradeApps;
    }

    public ArrayAdapter<String> getCategoriesAdapter() {
        return categories;
    }

    public AppListManager(FDroid activity) {
        this.fdroidActivity = activity;

        availableApps  = new AvailableAppListAdapter(fdroidActivity);
        installedApps  = new InstalledAppListAdapter(fdroidActivity);
        canUpgradeApps = new CanUpdateAppListAdapter(fdroidActivity);

        // Needs to be created before createViews(), because that will use the
        // getCategoriesAdapter() accessor which expects this object...
        categories = new ArrayAdapter<String>(activity,
                android.R.layout.simple_spinner_item, new ArrayList<String>());
        categories
                .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    }

    private void clear() {
        installedApps.clear();
        availableApps.clear();
        canUpgradeApps.clear();
        categories.clear();
    }

    private void notifyLists() {
        // Tell the lists that the data behind the adapter has changed, so
        // they can refresh...
        availableApps.notifyDataSetChanged();
        installedApps.notifyDataSetChanged();
        canUpgradeApps.notifyDataSetChanged();
        categories.notifyDataSetChanged();
    }

    private void updateCategories() {
        try {
            DB db = DB.getDB();

            // Populate the category list with the real categories, and the
            // locally generated meta-categories for "All", "What's New" and
            // "Recently  Updated"...
            categoryAll = fdroidActivity
                .getString(R.string.category_all);
            categoryWhatsNew = fdroidActivity
                .getString(R.string.category_whatsnew);
            categoryRecentlyUpdated = fdroidActivity
                .getString(R.string.category_recentlyupdated);

            categories.add(categoryWhatsNew);
            categories.add(categoryRecentlyUpdated);
            categories.add(categoryAll);
            if (Build.VERSION.SDK_INT >= 11) {
                categories.addAll(db.getCategories());
            } else {
                List<String> categs = db.getCategories();
                for (String category : categs) {
                    categories.add(category);
                }
            }

            if (currentCategory == null)
                currentCategory = categoryWhatsNew;

        } finally {
            DB.releaseDB();
        }
    }

    // Tell the FDroid activity to update its "Update (x)" tab to correctly
    // reflect the number of updates available.
    private void notifyActivity() {
        fdroidActivity.refreshUpdateTabLabel();
    }

    public void repopulateLists() {

        long startTime = System.currentTimeMillis();

        clear();

        updateCategories();
        updateApps();
        notifyLists();
        notifyActivity();

        Log.d("FDroid", "Updated lists - " + allApps.size() + " in total"
                + " (update took " + (System.currentTimeMillis() - startTime)
                + " ms)");
    }

    // Calculate the cutoff date we'll use for What's New and Recently
    // Updated...
    private Date calcMaxHistory() {
        SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(fdroidActivity.getBaseContext());
        String daysPreference = prefs.getString(Preferences.PREF_UPD_HISTORY, "14");
        int maxHistoryDays = Integer.parseInt(daysPreference);
        Calendar recent = Calendar.getInstance();
        recent.add(Calendar.DAY_OF_YEAR, -maxHistoryDays);
        return recent.getTime();
    }

    // recentDate could really be calculated here, but this is just a hack so
    // it doesn't need to be calculated for every single app. The reason it
    // isn't an instance variable is because the preferences may change, and
    // we wouldn't know.
    private boolean isInCategory(DB.App app, String category, Date recentDate) {
        if (category.equals(categoryAll)) {
            return true;
        }
        if (category.equals(categoryWhatsNew)) {
            if (app.added == null)
                return false;
            if (app.added.compareTo(recentDate) < 0)
                return false;
            return true;
        }
        if (category.equals(categoryRecentlyUpdated)) {
            if (app.lastUpdated == null)
                return false;
            // Don't include in the recently updated category if the
            // 'update' was actually it being added.
            if (app.lastUpdated.compareTo(app.added) == 0)
                return false;
            if (app.lastUpdated.compareTo(recentDate) < 0)
                return false;
            return true;
        }
        if (app.categories == null) return false;
        return app.categories.contains(category);
    }

    // Returns false if the app list is empty and the fdroid activity decided
    // to attempt updating it.
    private boolean updateApps() {

        allApps = ((FDroidApp)fdroidActivity.getApplication()).getApps();

        if (allApps.isEmpty()) {
            // If its the first time we've run the app, this should update
            // the repos. If not, it will do nothing, presuming that the repos
            // are invalid, the internet is stuffed, the sky has fallen, etc...
            return fdroidActivity.updateEmptyRepos();
        }

        Date recentDate = calcMaxHistory();
        List<DB.App> availApps = new ArrayList<DB.App>();
        for (DB.App app : allApps) {

            // Add it to the list(s). Always to installed and updates, but
            // only to available if it's not filtered.
            if (isInCategory(app, currentCategory, recentDate)) {
                availApps.add(app);
            }
            if (app.installedVersion != null) {
                installedApps.addItem(app);
                if (app.toUpdate) {
                    canUpgradeApps.addItem(app);
                }
            }
        }

        if (currentCategory.equals(categoryWhatsNew)) {
            Collections.sort(availApps, new WhatsNewComparator());
        } else if (currentCategory.equals(categoryRecentlyUpdated)) {
            Collections.sort(availApps, new RecentlyUpdatedComparator());
        }

        availableApps.addItems(availApps);

        return true;
    }

    public void setCurrentCategory(String currentCategory) {
        if (!this.currentCategory.equals(currentCategory)){
            this.currentCategory = currentCategory;
            repopulateLists();
        }
    }

    public String getCurrentCategory() {
        return this.currentCategory;
    }

    static class WhatsNewComparator implements Comparator<DB.App> {
        @Override
        public int compare(DB.App lhs, DB.App rhs) {
            return rhs.added.compareTo(lhs.added);
        }
    }

    static class RecentlyUpdatedComparator implements Comparator<DB.App> {
        @Override
        public int compare(DB.App lhs, DB.App rhs) {
            return rhs.lastUpdated.compareTo(lhs.lastUpdated);
        }
    }
}
