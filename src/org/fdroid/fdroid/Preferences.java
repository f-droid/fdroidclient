package org.fdroid.fdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
        if (preferences.getString(PREF_LOCAL_REPO_NAME, null) == null) {
            preferences.edit()
                    .putString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName())
                    .commit();
        }
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
    public static final String PREF_ROOT_INSTALLER = "rootInstaller";
    public static final String PREF_SYSTEM_INSTALLER = "systemInstaller";
    public static final String PREF_LOCAL_REPO_BONJOUR = "localRepoBonjour";
    public static final String PREF_LOCAL_REPO_NAME = "localRepoName";
    public static final String PREF_LOCAL_REPO_HTTPS = "localRepoHttps";
    public static final String PREF_ENABLE_PROXY = "enableProxy";
    public static final String PREF_PROXY_HOST = "proxyHost";
    public static final String PREF_PROXY_PORT = "proxyPort";

    private static final boolean DEFAULT_COMPACT_LAYOUT = false;
    private static final boolean DEFAULT_ROOTED = true;
    private static final int DEFAULT_UPD_HISTORY = 14;
    private static final boolean DEFAULT_ROOT_INSTALLER = false;
    private static final boolean DEFAULT_SYSTEM_INSTALLER = false;
    private static final boolean DEFAULT_LOCAL_REPO_BONJOUR = true;
    private static final boolean DEFAULT_LOCAL_REPO_HTTPS = false;
    private static final boolean DEFAULT_INCOMP_VER = false;
    private static final boolean DEFAULT_EXPERT = false;
    private static final boolean DEFAULT_PERMISSIONS = false;
    private static final boolean DEFAULT_ENABLE_PROXY = false;
    public static final String DEFAULT_PROXY_HOST = "127.0.0.1";
    public static final int DEFAULT_PROXY_PORT = 8118;

    private boolean compactLayout = DEFAULT_COMPACT_LAYOUT;
    private boolean filterAppsRequiringRoot = DEFAULT_ROOTED;

    private Map<String,Boolean> initialized = new HashMap<String,Boolean>();

    private List<ChangeListener> compactLayoutListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> filterAppsRequiringRootListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> updateHistoryListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> localRepoBonjourListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> localRepoNameListeners = new ArrayList<ChangeListener>();
    private List<ChangeListener> localRepoHttpsListeners = new ArrayList<ChangeListener>();

    private boolean isInitialized(String key) {
        return initialized.containsKey(key) && initialized.get(key);
    }

    private void initialize(String key) {
        initialized.put(key, true);
    }

    private void uninitialize(String key) {
        initialized.put(key, false);
    }

    public boolean isRootInstallerEnabled() {
        return preferences.getBoolean(PREF_ROOT_INSTALLER, DEFAULT_ROOT_INSTALLER);
    }

    public boolean isSystemInstallerEnabled() {
        return preferences.getBoolean(PREF_SYSTEM_INSTALLER, DEFAULT_SYSTEM_INSTALLER);
    }

    public boolean isLocalRepoBonjourEnabled() {
        return preferences.getBoolean(PREF_LOCAL_REPO_BONJOUR, DEFAULT_LOCAL_REPO_BONJOUR);
    }

    public boolean showIncompatibleVersions() {
        return preferences.getBoolean(PREF_INCOMP_VER, DEFAULT_INCOMP_VER);
    }

    public boolean showPermissions() {
        return preferences.getBoolean(PREF_PERMISSIONS, DEFAULT_PERMISSIONS);
    }

    public boolean expertMode() {
        return preferences.getBoolean(PREF_EXPERT, DEFAULT_EXPERT);
    }

    public boolean isLocalRepoHttpsEnabled() {
        return preferences.getBoolean(PREF_LOCAL_REPO_HTTPS, DEFAULT_LOCAL_REPO_HTTPS);
    }

    private String getDefaultLocalRepoName() {
        return (Build.BRAND + " " + Build.MODEL + String.valueOf(new Random().nextInt(9999)))
                .replaceAll(" ", "-");
    }

    public String getLocalRepoName() {
        return preferences.getString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName());
    }

    public boolean isProxyEnabled() {
        return preferences.getBoolean(PREF_ENABLE_PROXY, DEFAULT_ENABLE_PROXY);
    }

    public String getProxyHost() {
        return preferences.getString(PREF_PROXY_HOST, DEFAULT_PROXY_HOST);
    }

    public int getProxyPort() {
        String port = preferences.getString(PREF_PROXY_PORT, String.valueOf(DEFAULT_PROXY_PORT));
        try {
            return Integer.parseInt(port);
        } catch (NumberFormatException e) {
            // hack until this can be a number-only preference
            try {
                return Integer.parseInt(port.replaceAll("[^0-9]", ""));
            } catch (Exception e1) {
                return DEFAULT_PROXY_PORT;
            }
        }
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
     * Providing it here means the shared preferences file only needs to be
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
        } else if (key.equals(PREF_LOCAL_REPO_BONJOUR)) {
            for ( ChangeListener listener : localRepoBonjourListeners ) {
                listener.onPreferenceChange();
            }
        } else if (key.equals(PREF_LOCAL_REPO_NAME)) {
            for ( ChangeListener listener : localRepoNameListeners ) {
                listener.onPreferenceChange();
            }
        } else if (key.equals(PREF_LOCAL_REPO_HTTPS)) {
            for ( ChangeListener listener : localRepoHttpsListeners ) {
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

    public void registerLocalRepoBonjourListeners(ChangeListener listener) {
        localRepoBonjourListeners.add(listener);
    }

    public void unregisterLocalRepoBonjourListeners(ChangeListener listener) {
        localRepoBonjourListeners.remove(listener);
    }

    public void registerLocalRepoNameListeners(ChangeListener listener) {
        localRepoNameListeners.add(listener);
    }

    public void unregisterLocalRepoNameListeners(ChangeListener listener) {
        localRepoNameListeners.remove(listener);
    }

    public void registerLocalRepoHttpsListeners(ChangeListener listener) {
        localRepoHttpsListeners.add(listener);
    }

    public void unregisterLocalRepoHttpsListeners(ChangeListener listener) {
        localRepoHttpsListeners.remove(listener);
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
