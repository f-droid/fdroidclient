package org.fdroid.fdroid;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import info.guardianproject.netcipher.NetCipher;

/**
 * Handles shared preferences for FDroid, looking after the names of
 * preferences, default values and caching. Needs to be setup in the FDroidApp
 * (using {@link Preferences#setup(android.content.Context)} before it gets
 * accessed via the {@link org.fdroid.fdroid.Preferences#get()}
 * singleton method.
 */
public final class Preferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "Preferences";

    private final Context context;
    private final SharedPreferences preferences;

    private Preferences(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        if (preferences.getString(PREF_LOCAL_REPO_NAME, null) == null) {
            preferences.edit()
                .putString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName())
                .apply();
        }
    }

    public static final String PREF_UPD_INTERVAL = "updateInterval";
    public static final String PREF_UPD_WIFI_ONLY = "updateOnWifiOnly";
    public static final String PREF_UPD_AUTO_DOWNLOAD = "updateAutoDownload";
    public static final String PREF_UPD_NOTIFY = "updateNotify";
    public static final String PREF_UPD_HISTORY = "updateHistoryDays";
    public static final String PREF_ROOTED = "rooted";
    public static final String PREF_INCOMP_VER = "incompatibleVersions";
    public static final String PREF_THEME = "theme";
    public static final String PREF_IGN_TOUCH = "ignoreTouchscreen";
    public static final String PREF_KEEP_CACHE_TIME = "keepCacheFor";
    public static final String PREF_UNSTABLE_UPDATES = "unstableUpdates";
    public static final String PREF_EXPERT = "expert";
    public static final String PREF_PRIVILEGED_INSTALLER = "privilegedInstaller";
    public static final String PREF_UNINSTALL_PRIVILEGED_APP = "uninstallPrivilegedApp";
    public static final String PREF_LOCAL_REPO_NAME = "localRepoName";
    public static final String PREF_LOCAL_REPO_HTTPS = "localRepoHttps";
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_USE_TOR = "useTor";
    public static final String PREF_ENABLE_PROXY = "enableProxy";
    public static final String PREF_PROXY_HOST = "proxyHost";
    public static final String PREF_PROXY_PORT = "proxyPort";
    public static final String PREF_SHOW_NFC_DURING_SWAP = "showNfcDuringSwap";
    public static final String PREF_POST_PRIVILEGED_INSTALL = "postPrivilegedInstall";

    private static final boolean DEFAULT_ROOTED = true;
    private static final int DEFAULT_UPD_HISTORY = 14;
    private static final boolean DEFAULT_PRIVILEGED_INSTALLER = false;
    //private static final boolean DEFAULT_LOCAL_REPO_BONJOUR = true;
    private static final long DEFAULT_KEEP_CACHE_SECONDS = 86400;  // one day
    private static final boolean DEFAULT_UNSTABLE_UPDATES = false;
    //private static final boolean DEFAULT_LOCAL_REPO_HTTPS = false;
    private static final boolean DEFAULT_INCOMP_VER = false;
    private static final boolean DEFAULT_EXPERT = false;
    private static final boolean DEFAULT_ENABLE_PROXY = false;
    public static final String DEFAULT_THEME = "light";
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String DEFAULT_PROXY_HOST = "127.0.0.1";
    public static final int DEFAULT_PROXY_PORT = 8118;
    private static final boolean DEFAULT_SHOW_NFC_DURING_SWAP = true;
    private static final boolean DEFAULT_POST_PRIVILEGED_INSTALL = false;

    public enum Theme {
        light,
        dark,
        night,
        lightWithDarkActionBar, // Obsolete
    }

    private boolean filterAppsRequiringRoot = DEFAULT_ROOTED;

    private final Map<String, Boolean> initialized = new HashMap<>();

    private final List<ChangeListener> filterAppsRequiringRootListeners = new ArrayList<>();
    private final List<ChangeListener> updateHistoryListeners = new ArrayList<>();
    private final List<ChangeListener> localRepoNameListeners = new ArrayList<>();
    private final List<ChangeListener> localRepoHttpsListeners = new ArrayList<>();
    private final List<ChangeListener> unstableUpdatesListeners = new ArrayList<>();

    private boolean isInitialized(String key) {
        return initialized.containsKey(key) && initialized.get(key);
    }

    private void initialize(String key) {
        initialized.put(key, true);
    }

    private void uninitialize(String key) {
        initialized.put(key, false);
    }

    public boolean isPrivilegedInstallerEnabled() {
        return preferences.getBoolean(PREF_PRIVILEGED_INSTALLER, DEFAULT_PRIVILEGED_INSTALLER);
    }

    public void setPrivilegedInstallerEnabled(boolean enable) {
        preferences.edit().putBoolean(PREF_PRIVILEGED_INSTALLER, enable).apply();
    }

    public boolean isPostPrivilegedInstall() {
        return preferences.getBoolean(PREF_POST_PRIVILEGED_INSTALL, DEFAULT_POST_PRIVILEGED_INSTALL);
    }

    public void setPostPrivilegedInstall(boolean postInstall) {
        preferences.edit().putBoolean(PREF_POST_PRIVILEGED_INSTALL, postInstall).apply();
    }

    /**
     * Old preference replaced by {@link #PREF_KEEP_CACHE_TIME}
     */
    private static final String PREF_CACHE_APK = "cacheDownloaded";

    /**
     * Time in seconds to keep cached files.  Anything that has been around longer will be deleted
     */
    public long getKeepCacheTime() {
        String value = preferences.getString(PREF_KEEP_CACHE_TIME, String.valueOf(DEFAULT_KEEP_CACHE_SECONDS));

        if (preferences.contains(PREF_CACHE_APK)) {
            if (preferences.getBoolean(PREF_CACHE_APK, false)) {
                value = context.getString(R.string.keep_forever);
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(PREF_CACHE_APK);
            editor.putString(PREF_KEEP_CACHE_TIME, value);
            editor.apply();
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return DEFAULT_KEEP_CACHE_SECONDS;
        }
    }

    public boolean getUnstableUpdates() {
        return preferences.getBoolean(PREF_UNSTABLE_UPDATES, DEFAULT_UNSTABLE_UPDATES);
    }

    public boolean showIncompatibleVersions() {
        return preferences.getBoolean(PREF_INCOMP_VER, DEFAULT_INCOMP_VER);
    }

    public boolean showNfcDuringSwap() {
        return preferences.getBoolean(PREF_SHOW_NFC_DURING_SWAP, DEFAULT_SHOW_NFC_DURING_SWAP);
    }

    public void setShowNfcDuringSwap(boolean show) {
        preferences.edit().putBoolean(PREF_SHOW_NFC_DURING_SWAP, show).apply();
    }

    public boolean expertMode() {
        return preferences.getBoolean(PREF_EXPERT, DEFAULT_EXPERT);
    }

    public Theme getTheme() {
        return Theme.valueOf(preferences.getString(Preferences.PREF_THEME, Preferences.DEFAULT_THEME));
    }

    public boolean isLocalRepoHttpsEnabled() {
        return false; // disabled until it works well
    }

    private String getDefaultLocalRepoName() {
        return (Build.BRAND + " " + Build.MODEL + new Random().nextInt(9999))
                .replaceAll(" ", "-");
    }

    public String getLocalRepoName() {
        return preferences.getString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName());
    }

    public boolean isUpdateNotificationEnabled() {
        return preferences.getBoolean(PREF_UPD_NOTIFY, true);
    }

    public boolean isAutoDownloadEnabled() {
        return preferences.getBoolean(PREF_UPD_AUTO_DOWNLOAD, false);
    }

    public boolean isUpdateOnlyOnWifi() {
        return preferences.getBoolean(PREF_UPD_WIFI_ONLY, false);
    }

    /**
     * This preference's default is set dynamically based on whether Orbot is
     * installed. If Orbot is installed, default to using Tor, the user can still override
     */
    public boolean isTorEnabled() {
        // TODO enable once Orbot can auto-start after first install
        //return preferences.getBoolean(PREF_USE_TOR, OrbotHelper.requestStartTor(context));
        return preferences.getBoolean(PREF_USE_TOR, false);
    }

    private boolean isProxyEnabled() {
        return preferences.getBoolean(PREF_ENABLE_PROXY, DEFAULT_ENABLE_PROXY);
    }

    /**
     * Configure the proxy settings based on whether its enabled and set up. This must be
     * run once at app startup, then whenever any of these settings changes.
     */
    public void configureProxy() {
        if (isProxyEnabled()) {
            // if "Use Tor" is set, NetCipher will ignore these proxy settings
            SocketAddress sa = new InetSocketAddress(getProxyHost(), getProxyPort());
            NetCipher.setProxy(new Proxy(Proxy.Type.HTTP, sa));
        }
    }

    public String getProxyHost() {
        return preferences.getString(PREF_PROXY_HOST, DEFAULT_PROXY_HOST);
    }

    public int getProxyPort() {
        final String port = preferences.getString(PREF_PROXY_PORT, String.valueOf(DEFAULT_PROXY_PORT));
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

    /**
     * Calculate the cutoff date we'll use for What's New and Recently
     * Updated...
     */
    public Date calcMaxHistory() {
        final String daysString = preferences.getString(PREF_UPD_HISTORY, Integer.toString(DEFAULT_UPD_HISTORY));
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

    public void registerUnstableUpdatesChangeListener(ChangeListener listener) {
        unstableUpdatesListeners.add(listener);
    }

    public void unregisterUnstableUpdatesChangeListener(ChangeListener listener) {
        unstableUpdatesListeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Utils.debugLog(TAG, "Invalidating preference '" + key + "'.");
        uninitialize(key);

        switch (key) {
            case PREF_ROOTED:
                for (ChangeListener listener : filterAppsRequiringRootListeners) {
                    listener.onPreferenceChange();
                }
                break;
            case PREF_UPD_HISTORY:
                for (ChangeListener listener : updateHistoryListeners) {
                    listener.onPreferenceChange();
                }
                break;
            case PREF_LOCAL_REPO_NAME:
                for (ChangeListener listener : localRepoNameListeners) {
                    listener.onPreferenceChange();
                }
                break;
            case PREF_LOCAL_REPO_HTTPS:
                for (ChangeListener listener : localRepoHttpsListeners) {
                    listener.onPreferenceChange();
                }
                break;
            case PREF_UNSTABLE_UPDATES:
                for (ChangeListener listener : unstableUpdatesListeners) {
                    listener.onPreferenceChange();
                }
                break;
        }
    }

    public void registerUpdateHistoryListener(ChangeListener listener) {
        updateHistoryListeners.add(listener);
    }

    public void unregisterUpdateHistoryListener(ChangeListener listener) {
        updateHistoryListeners.remove(listener);
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

    public interface ChangeListener {
        void onPreferenceChange();
    }

    private static Preferences instance;

    /**
     * Should only be used for unit testing, whereby separate tests are required to invoke `setup()`.
     * The reason we don't instead ask for the singleton to be lazily loaded in the {@link Preferences#get()}
     * method is because that would require each call to that method to require a {@link Context}.
     * While it is likely that most places asking for preferences have access to a {@link Context},
     * it is a minor convenience to be able to ask for preferences without.
     */
    public static void clearSingletonForTesting() {
        instance = null;
    }

    /**
     * Needs to be setup before anything else tries to access it.
     */
    public static void setup(Context context) {
        if (instance != null) {
            final String error = "Attempted to reinitialize preferences after it " +
                    "has already been initialized in FDroidApp";
            Log.e(TAG, error);
            throw new RuntimeException(error);
        }
        instance = new Preferences(context);
    }

    public static Preferences get() {
        if (instance == null) {
            final String error = "Attempted to access preferences before it " +
                    "has been initialized in FDroidApp";
            Log.e(TAG, error);
            throw new RuntimeException(error);
        }
        return instance;
    }

}
