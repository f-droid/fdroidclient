/*
 * Copyright (C) 2010-12  Ciaran Gultnieks, ciaran@ciarang.com
 * Copyright (C) 2013-2017  Peter Serwylo <peter@serwylo.com>
 * Copyright (C) 2013-2016  Daniel Martí <mvdan@mvdan.cc>
 * Copyright (C) 2014-2018  Hans-Christoph Steiner <hans@eds.org>
 * Copyright (C) 2018  Senecto Limited
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
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.fdroid.fdroid;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateUtils;
import android.util.Log;
import info.guardianproject.netcipher.NetCipher;
import org.fdroid.fdroid.net.ConnectivityMonitorService;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Handles shared preferences for FDroid, looking after the names of
 * preferences, default values and caching. Needs to be setup in the FDroidApp
 * (using {@link Preferences#setup(android.content.Context)} before it gets
 * accessed via the {@link org.fdroid.fdroid.Preferences#get()}
 * singleton method.
 * <p>
 * All defaults should be set in {@code res/xml/preferences.xml}.  The one
 * exception is {@link Preferences#PREF_LOCAL_REPO_NAME} since it needs to be
 * generated per install. The preferences are only written out explicitly when
 * the user changes the preferences.  So the default values need to be reloaded
 * every time F-Droid starts.  The various {@link SharedPreferences} getters are
 * using {@code false} and {@code -1} as fallback default values to help catch
 * problems with the proper default loading as quickly as possible.
 */
public final class Preferences implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "Preferences";

    private final SharedPreferences preferences;

    private Preferences(Context context) {
        PreferenceManager.setDefaultValues(context, R.xml.preferences, true);
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.registerOnSharedPreferenceChangeListener(this);
        if (preferences.getString(PREF_LOCAL_REPO_NAME, null) == null) {
            preferences.edit()
                    .putString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName())
                    .apply();
        }
    }

    public static final String PREF_OVER_WIFI = "overWifi";
    public static final String PREF_OVER_DATA = "overData";
    public static final String PREF_UPDATE_INTERVAL = "updateIntervalSeekBarPosition";
    public static final String PREF_AUTO_DOWNLOAD_INSTALL_UPDATES = "updateAutoDownload";
    public static final String PREF_UPDATE_NOTIFICATION_ENABLED = "updateNotify";
    public static final String PREF_THEME = "theme";
    public static final String PREF_SHOW_INCOMPAT_VERSIONS = "incompatibleVersions";
    public static final String PREF_SHOW_ROOT_APPS = "rooted";
    public static final String PREF_SHOW_ANTI_FEATURE_APPS = "showAntiFeatureApps";
    public static final String PREF_FORCE_TOUCH_APPS = "ignoreTouchscreen";
    public static final String PREF_PROMPT_TO_SEND_CRASH_REPORTS = "promptToSendCrashReports";
    public static final String PREF_KEEP_CACHE_TIME = "keepCacheFor";
    public static final String PREF_UNSTABLE_UPDATES = "unstableUpdates";
    public static final String PREF_KEEP_INSTALL_HISTORY = "keepInstallHistory";
    public static final String PREF_EXPERT = "expert";
    public static final String PREF_FORCE_OLD_INDEX = "forceOldIndex";
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
    public static final String PREF_TRIED_EMPTY_UPDATE = "triedEmptyUpdate";
    public static final String PREF_PREVENT_SCREENSHOTS = "preventScreenshots";
    public static final String PREF_PANIC_EXIT = "pref_panic_exit";
    public static final String PREF_PANIC_HIDE = "pref_panic_hide";
    public static final String PREF_HIDE_ON_LONG_PRESS_SEARCH = "hideOnLongPressSearch";
    public static final String PREF_HIDE_ALL_NOTIFICATIONS = "hideAllNotifications";
    public static final String PREF_SEND_VERSION_AND_UUID_TO_SERVERS = "sendVersionAndUUIDToServers";
    public static final String PREF_ALLOW_PUSH_REQUESTS = "allowPushRequests";

    public static final int OVER_NETWORK_NEVER = 0;
    public static final int OVER_NETWORK_ON_DEMAND = 1;
    public static final int OVER_NETWORK_ALWAYS = 2;

    // these preferences are not listed in preferences.xml so the defaults are set here
    @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
    public static final String DEFAULT_PROXY_HOST = "127.0.0.1"; // TODO move to preferences.xml
    public static final int DEFAULT_PROXY_PORT = 8118; // TODO move to preferences.xml
    private static final boolean DEFAULT_SHOW_NFC_DURING_SWAP = true;
    private static final boolean DEFAULT_POST_PRIVILEGED_INSTALL = false;
    private static final boolean DEFAULT_PANIC_EXIT = true;

    private static final boolean IGNORED_B = false;
    private static final int IGNORED_I = -1;

    /**
     * Old preference replaced by {@link #PREF_KEEP_CACHE_TIME}
     */
    @Deprecated
    private static final String OLD_PREF_CACHE_APK = "cacheDownloaded";
    @Deprecated
    private static final String OLD_PREF_UPDATE_INTERVAL = "updateInterval";
    @Deprecated
    private static final String OLD_PREF_UPDATE_ON_WIFI_ONLY = "updateOnWifiOnly";

    public enum Theme {
        light,
        dark,
        night,
        lightWithDarkActionBar, // Obsolete
    }

    public static final long UPDATE_INTERVAL_DISABLED = Long.MAX_VALUE;
    public static final long[] UPDATE_INTERVAL_VALUES = {
            UPDATE_INTERVAL_DISABLED,
            DateUtils.WEEK_IN_MILLIS * 2,
            DateUtils.WEEK_IN_MILLIS,
            DateUtils.DAY_IN_MILLIS,
            DateUtils.HOUR_IN_MILLIS * 12,
            DateUtils.HOUR_IN_MILLIS * 4,
            DateUtils.HOUR_IN_MILLIS,
    };

    private boolean showAppsRequiringRoot;
    private boolean showAppsWithAntiFeatures;

    private final Map<String, Boolean> initialized = new HashMap<>();

    private final List<ChangeListener> showAppsRequiringRootListeners = new ArrayList<>();
    private final List<ChangeListener> showAppsRequiringAntiFeaturesListeners = new ArrayList<>();
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

    public boolean promptToSendCrashReports() {
        return preferences.getBoolean(PREF_PROMPT_TO_SEND_CRASH_REPORTS, IGNORED_B);
    }

    public boolean isForceOldIndexEnabled() {
        return preferences.getBoolean(PREF_FORCE_OLD_INDEX, IGNORED_B);
    }

    public void setForceOldIndex(boolean flag) {
        preferences.edit().putBoolean(PREF_FORCE_OLD_INDEX, flag).apply();
    }

    /**
     * Whether to use the Privileged Installer, based on if it is installed.  Only the disabled
     * state is stored as a preference since the enabled state is based entirely on the presence
     * of the Privileged Extension.  The preference provides a way to disable using the
     * Privileged Extension even though its installed.
     *
     * @see org.fdroid.fdroid.views.fragments.PreferencesFragment#initPrivilegedInstallerPreference()
     */
    public boolean isPrivilegedInstallerEnabled() {
        return preferences.getBoolean(PREF_PRIVILEGED_INSTALLER, true);
    }

    public boolean isPostPrivilegedInstall() {
        return preferences.getBoolean(PREF_POST_PRIVILEGED_INSTALL, DEFAULT_POST_PRIVILEGED_INSTALL);
    }

    public void setPostPrivilegedInstall(boolean postInstall) {
        preferences.edit().putBoolean(PREF_POST_PRIVILEGED_INSTALL, postInstall).apply();
    }

    /**
     * Get the update interval in milliseconds.
     */
    public long getUpdateInterval() {
        int position = preferences.getInt(PREF_UPDATE_INTERVAL, IGNORED_I);
        return UPDATE_INTERVAL_VALUES[position];
    }

    /**
     * Migrate old preferences to new preferences.  These need to be processed
     * and committed before {@code preferences.xml} is loaded.
     */
    @SuppressLint("ApplySharedPref")
    public void migrateOldPreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        if (migrateUpdateIntervalStringToInt(editor) || migrateOnlyOnWifi(editor)) {
            editor.commit();
        }
    }

    /**
     * The original preference was a {@link String}, now it must be a {@link Integer}
     * since {@link android.support.v7.preference.SeekBarPreference} uses it
     * directly.
     */
    private boolean migrateUpdateIntervalStringToInt(SharedPreferences.Editor editor) {
        if (!preferences.contains(OLD_PREF_UPDATE_INTERVAL)) {
            return false; // already completed
        }
        int updateInterval = 3;
        String value = preferences.getString(OLD_PREF_UPDATE_INTERVAL, String.valueOf(24));
        if ("1".equals(value)) { // 1 hour
            updateInterval = 6;
        } else if ("4".equals(value)) { // 4 hours
            updateInterval = 5;
        } else if ("12".equals(value)) { // 12 hours
            updateInterval = 4;
        } else if ("24".equals(value)) { // 1 day
            updateInterval = 3;
        } else if ("168".equals(value)) { // 2 weeks
            updateInterval = 2;
        } else if ("336".equals(value)) { // 1 week
            updateInterval = 1;
        } else if ("0".equals(value)) { // never
            updateInterval = 0;
        }
        editor
                .putInt(PREF_UPDATE_INTERVAL, updateInterval)
                .remove(OLD_PREF_UPDATE_INTERVAL);
        return true;
    }

    /**
     * The original preference was just a "Only on Wifi" checkbox.
     */
    private boolean migrateOnlyOnWifi(SharedPreferences.Editor editor) {
        if (!preferences.contains(OLD_PREF_UPDATE_ON_WIFI_ONLY)) {
            return false; // already completed
        }
        int wifi;
        int data;
        if (preferences.getBoolean(OLD_PREF_UPDATE_ON_WIFI_ONLY, true)) {
            wifi = OVER_NETWORK_ALWAYS;
            data = OVER_NETWORK_NEVER;
        } else {
            wifi = OVER_NETWORK_ALWAYS;
            data = OVER_NETWORK_ON_DEMAND;
        }
        editor
                .putInt(PREF_OVER_WIFI, wifi)
                .putInt(PREF_OVER_DATA, data)
                .remove(OLD_PREF_UPDATE_ON_WIFI_ONLY);
        return true;
    }

    /**
     * Time in millis to keep cached files.  Anything that has been around longer will be deleted
     */
    public long getKeepCacheTime() {
        String value = preferences.getString(PREF_KEEP_CACHE_TIME, null);
        long fallbackValue = TimeUnit.DAYS.toMillis(1);

        // the first time this was migrated, it was botched, so reset to default
        switch (value) {
            case "3600":
            case "86400":
            case "604800":
            case "2592000":
            case "31449600":
            case "2147483647":
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(PREF_KEEP_CACHE_TIME);
                editor.apply();
                return fallbackValue;
        }

        if (preferences.contains(OLD_PREF_CACHE_APK)) {
            if (preferences.getBoolean(OLD_PREF_CACHE_APK, false)) {
                value = String.valueOf(Long.MAX_VALUE);
            }
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove(OLD_PREF_CACHE_APK);
            editor.putString(PREF_KEEP_CACHE_TIME, value);
            editor.apply();
        }

        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return fallbackValue;
        }
    }

    /**
     * Used the first time F-Droid is installed to flag whether or not we have tried to request
     * apps from the repo. This is used so that when there is no apps available, we can differentiate
     * between whether the repos actually have no apps (in which case we don't need to continue
     * asking), or whether there is no apps because we have never actually asked to update the repos.
     */
    public boolean hasTriedEmptyUpdate() {
        return preferences.getBoolean(PREF_TRIED_EMPTY_UPDATE, IGNORED_B);
    }

    public void setTriedEmptyUpdate(boolean value) {
        preferences.edit().putBoolean(PREF_TRIED_EMPTY_UPDATE, value).apply();
    }

    public boolean getUnstableUpdates() {
        return preferences.getBoolean(PREF_UNSTABLE_UPDATES, IGNORED_B);
    }

    public void setUnstableUpdates(boolean value) {
        preferences.edit().putBoolean(PREF_UNSTABLE_UPDATES, value).apply();
    }

    public boolean isKeepingInstallHistory() {
        return preferences.getBoolean(PREF_KEEP_INSTALL_HISTORY, IGNORED_B);
    }

    public boolean showIncompatibleVersions() {
        return preferences.getBoolean(PREF_SHOW_INCOMPAT_VERSIONS, IGNORED_B);
    }

    public boolean showNfcDuringSwap() {
        return preferences.getBoolean(PREF_SHOW_NFC_DURING_SWAP, DEFAULT_SHOW_NFC_DURING_SWAP);
    }

    public void setShowNfcDuringSwap(boolean show) {
        preferences.edit().putBoolean(PREF_SHOW_NFC_DURING_SWAP, show).apply();
    }

    public boolean expertMode() {
        return preferences.getBoolean(PREF_EXPERT, IGNORED_B);
    }

    public void setExpertMode(boolean flag) {
        preferences.edit().putBoolean(PREF_EXPERT, flag).apply();
    }

    public boolean forceTouchApps() {
        return preferences.getBoolean(Preferences.PREF_FORCE_TOUCH_APPS, IGNORED_B);
    }

    public Theme getTheme() {
        return Theme.valueOf(preferences.getString(Preferences.PREF_THEME, null));
    }

    public boolean isLocalRepoHttpsEnabled() {
        return false; // disabled until it works well
    }

    private String getDefaultLocalRepoName() {
        return (Build.BRAND + " " + Build.MODEL + new Random().nextInt(9999))
                .replaceAll(" ", "-");
    }

    public String getLanguage() {
        return preferences.getString(Preferences.PREF_LANGUAGE, Languages.USE_SYSTEM_DEFAULT);
    }

    public void clearLanguage() {
        preferences.edit().remove(Preferences.PREF_LANGUAGE).apply();
    }

    public String getLocalRepoName() {
        return preferences.getString(PREF_LOCAL_REPO_NAME, getDefaultLocalRepoName());
    }

    public boolean isUpdateNotificationEnabled() {
        return preferences.getBoolean(PREF_UPDATE_NOTIFICATION_ENABLED, true);
    }

    public boolean isAutoDownloadEnabled() {
        return preferences.getBoolean(PREF_AUTO_DOWNLOAD_INSTALL_UPDATES, IGNORED_B);
    }

    /**
     * Do the network conditions and user preferences allow for things to be
     * downloaded in the background.
     */
    public boolean isBackgroundDownloadAllowed() {
        if (FDroidApp.networkState == ConnectivityMonitorService.FLAG_NET_NO_LIMIT) {
            return getOverWifi() == OVER_NETWORK_ALWAYS;
        } else if (FDroidApp.networkState == ConnectivityMonitorService.FLAG_NET_METERED) {
            return getOverData() == OVER_NETWORK_ALWAYS;
        }
        return false;
    }

    public boolean isOnDemandDownloadAllowed() {
        if (FDroidApp.networkState == ConnectivityMonitorService.FLAG_NET_NO_LIMIT) {
            return getOverWifi() != OVER_NETWORK_NEVER;
        } else if (FDroidApp.networkState == ConnectivityMonitorService.FLAG_NET_METERED) {
            return getOverData() != OVER_NETWORK_NEVER;
        }
        return false;
    }

    public int getOverWifi() {
        return preferences.getInt(PREF_OVER_WIFI, IGNORED_I);
    }

    public int getOverData() {
        return preferences.getInt(PREF_OVER_DATA, IGNORED_I);
    }

    /**
     * This preference's default is set dynamically based on whether Orbot is
     * installed. If Orbot is installed, default to using Tor, the user can still override
     */
    public boolean isTorEnabled() {
        // TODO enable once Orbot can auto-start after first install
        //return preferences.getBoolean(PREF_USE_TOR, OrbotHelper.requestStartTor(context));
        return preferences.getBoolean(PREF_USE_TOR, IGNORED_B);
    }

    private boolean isProxyEnabled() {
        return preferences.getBoolean(PREF_ENABLE_PROXY, IGNORED_B);
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

    public boolean preventScreenshots() {
        return preferences.getBoolean(PREF_PREVENT_SCREENSHOTS, IGNORED_B);
    }

    public boolean panicExit() {
        return preferences.getBoolean(PREF_PANIC_EXIT, DEFAULT_PANIC_EXIT);
    }

    public boolean panicHide() {
        return preferences.getBoolean(PREF_PANIC_HIDE, IGNORED_B);
    }

    public boolean hideOnLongPressSearch() {
        return preferences.getBoolean(PREF_HIDE_ON_LONG_PRESS_SEARCH, IGNORED_B);
    }

    /**
     * Preference for whitelabel builds that are meant to be entirely controlled
     * by the server, without user interaction, e.g. "appliances".
     */
    public boolean hideAllNotifications() {
        return preferences.getBoolean(PREF_HIDE_ALL_NOTIFICATIONS, IGNORED_B);
    }

    /**
     * Whether to include the version of this app and a randomly generated ID
     * to the server when downloading from it.
     */
    public boolean sendVersionAndUUIDToServers() {
        return preferences.getBoolean(PREF_SEND_VERSION_AND_UUID_TO_SERVERS, IGNORED_B);
    }

    /**
     * Whether push requests are globally enabled or disabled.
     *
     * @see org.fdroid.fdroid.data.RepoPushRequest
     * @see RepoUpdater#processRepoPushRequests(List)
     */
    public boolean allowPushRequests() {
        return preferences.getBoolean(PREF_ALLOW_PUSH_REQUESTS, IGNORED_B);
    }

    /**
     * This is cached as it is called several times inside app list adapters.
     * Providing it here means the shared preferences file only needs to be
     * read once, and we will keep our copy up to date by listening to changes
     * in PREF_SHOW_ROOT_APPS.
     */
    public boolean showAppsRequiringRoot() {
        if (!isInitialized(PREF_SHOW_ROOT_APPS)) {
            initialize(PREF_SHOW_ROOT_APPS);
            showAppsRequiringRoot = preferences.getBoolean(PREF_SHOW_ROOT_APPS, IGNORED_B);
        }
        return showAppsRequiringRoot;
    }

    /**
     * This is cached as it is called several times inside app list adapters.
     * Providing it here means the shared preferences file only needs to be
     * read once, and we will keep our copy up to date by listening to changes
     * in PREF_SHOW_ANTI_FEATURE_APPS.
     */
    public boolean showAppsWithAntiFeatures() {
        // migrate old preference to new key
        if (isInitialized("hideAntiFeatureApps")) {
            boolean oldPreference = preferences.getBoolean("hideAntiFeatureApps", false);
            preferences.edit().putBoolean(PREF_SHOW_ANTI_FEATURE_APPS, !oldPreference).apply();
        }
        if (!isInitialized(PREF_SHOW_ANTI_FEATURE_APPS)) {
            initialize(PREF_SHOW_ANTI_FEATURE_APPS);
            showAppsWithAntiFeatures = preferences.getBoolean(PREF_SHOW_ANTI_FEATURE_APPS, IGNORED_B);
        }
        return showAppsWithAntiFeatures;
    }

    public void registerAppsRequiringRootChangeListener(ChangeListener listener) {
        showAppsRequiringRootListeners.add(listener);
    }

    public void unregisterAppsRequiringRootChangeListener(ChangeListener listener) {
        showAppsRequiringRootListeners.remove(listener);
    }

    public void registerAppsRequiringAntiFeaturesChangeListener(ChangeListener listener) {
        showAppsRequiringAntiFeaturesListeners.add(listener);
    }

    public void unregisterAppsRequiringAntiFeaturesChangeListener(ChangeListener listener) {
        showAppsRequiringAntiFeaturesListeners.remove(listener);
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
            case PREF_SHOW_ROOT_APPS:
                for (ChangeListener listener : showAppsRequiringRootListeners) {
                    listener.onPreferenceChange();
                }
                break;
            case PREF_SHOW_ANTI_FEATURE_APPS:
                for (ChangeListener listener : showAppsRequiringAntiFeaturesListeners) {
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
    public static void setupForTests(Context context) {
        instance = null;
        setup(context);
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
