package org.fdroid.fdroid;

import java.util.*;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Handles shared preferences for FDroid, looking after the names of
 * preferences, default values and caching. Needs to be setup in the FDroidApp
 * (using {@link Preferences#setup(android.content.Context)} before it gets
 * accessed via the {@link org.fdroid.fdroid.Preferences#get()}
 * singleton method.
 */
public class Preferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    private final SharedPreferences preferences;

    private Preferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
    }

    public static final String PREF_UPD_INTERVAL = "updateInterval";
    public static final String PREF_UPD_WIFI_ONLY = "updateOnWifiOnly";
    public static final String PREF_UPD_NOTIFY = "updateNotify";
    public static final String PREF_UPD_HISTORY = "updateHistoryDays";
    public static final String PREF_ROOTED = "rooted";
    public static final String PREF_INCOMP_VER = "incompatibleVersions";
    public static final String PREF_THEME = "theme";
    public static final String PREF_PERMISSIONS = "showPermissions";
    public static final String PREF_COMPACT_LAYOUT = "compactlayout";
    public static final String PREF_IGN_TOUCH = "ignoreTouchscreen";
    public static final String PREF_CACHE_APK = "cacheDownloaded";
    public static final String PREF_EXPERT = "expert";
    public static final String PREF_UPD_LAST = "lastUpdateCheck";

    private static final boolean DEFAULT_COMPACT_LAYOUT = false;
    private static final boolean DEFAULT_ROOTED = true;
    private static final int DEFAULT_UPD_HISTORY = 14;

    private boolean compactLayout = DEFAULT_COMPACT_LAYOUT;
    private boolean filterAppsRequiringRoot = DEFAULT_ROOTED;

    private Map<String,Boolean> initialized = new HashMap<String,Boolean>();

    private List<ChangeListener> compactLayoutListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> filterAppsRequiringRootListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> updateHistoryListeners = new ArrayList<ChangeListener>();

    private boolean isInitialized(String key) {
        return initialized.containsKey(key) && initialized.get(key);
    }

    private void initialize(String key) {
        initialized.put(key, true);
    }

    private void uninitialize(String key) {
        initialized.put(key, false);
    }

    public boolean hasCompactLayout() {
        if (!isInitialized(PREF_COMPACT_LAYOUT)) {
            initialize(PREF_COMPACT_LAYOUT);
            compactLayout = preferences.getBoolean(PREF_COMPACT_LAYOUT, DEFAULT_COMPACT_LAYOUT);
        }
        return compactLayout;
    }

    public void registerCompactLayoutChangeListener(ChangeListener listener) {
        compactLayoutListeners.add(listener);
    }

    public void unregisterCompactLayoutChangeListener(ChangeListener listener) {
        compactLayoutListeners.remove(listener);
    }

    /**
     * Calculate the cutoff date we'll use for What's New and Recently
     * Updated...
     */
    public Date calcMaxHistory() {
        String daysString = preferences.getString(PREF_UPD_HISTORY, Integer.toString(DEFAULT_UPD_HISTORY));
        int maxHistoryDays;
        try {
            maxHistoryDays = Integer.parseInt(daysString);
        } catch (NumberFormatException e) {
            maxHistoryDays = DEFAULT_UPD_HISTORY;
        }
        Calendar recent = Calendar.getInstance();
        recent.add(Calendar.DAY_OF_YEAR, -maxHistoryDays);
        return recent.getTime();
    }

    /**
     * This is cached as it is called several times inside the AppListAdapter.
     * Providing it here means sthe shared preferences file only needs to be
     * read once, and we will keep our copy up to date by listening to changes
     * in PREF_ROOTED.
     */
    public boolean filterAppsRequiringRoot() {
        if (!isInitialized(PREF_ROOTED)) {
            initialize(PREF_ROOTED);
            filterAppsRequiringRoot = preferences.getBoolean(PREF_ROOTED, DEFAULT_ROOTED);
        }
        return filterAppsRequiringRoot;
    }

    public void registerAppsRequiringRootChangeListener(ChangeListener listener) {
        filterAppsRequiringRootListeners.add(listener);
    }

    public void unregisterAppsRequiringRootChangeListener(ChangeListener listener) {
        filterAppsRequiringRootListeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Log.d("FDroid", "Invalidating preference '" + key + "'.");
        uninitialize(key);

        if (key.equals(PREF_COMPACT_LAYOUT)) {
            for ( ChangeListener listener : compactLayoutListeners )  {
                listener.onPreferenceChange();
            }
        } else if (key.equals(PREF_ROOTED)) {
            for ( ChangeListener listener : filterAppsRequiringRootListeners ) {
                listener.onPreferenceChange();
            }
        } else if (key.equals(PREF_UPD_HISTORY)) {
            for ( ChangeListener listener : updateHistoryListeners ) {
                listener.onPreferenceChange();
            }
        }
    }

    public void registerUpdateHistoryListener(ChangeListener listener) {
        updateHistoryListeners.add(listener);
    }

    public void unregisterUpdateHistoryListener(ChangeListener listener) {
        updateHistoryListeners.remove(listener);
    }

    public static interface ChangeListener {
        public void onPreferenceChange();
    }

    private static Preferences instance;

    public static void setup(Context context) {
        if (instance != null) {
            String error = "Attempted to reinitialize preferences after it " +
                    "has already been initialized in FDroidApp";
            Log.e("FDroid", error);
            throw new RuntimeException(error);
        }
        instance = new Preferences(context);
    }

    public static Preferences get() {
        if (instance == null) {
            String error = "Attempted to access preferences before it " +
                    "has been initialized in FDroidApp";
            Log.e("FDroid", error);
            throw new RuntimeException(error);
        }
        return instance;
    }

}
