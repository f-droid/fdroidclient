package org.fdroid.fdroid;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Minimal Preferences stub for the swap/nearby feature in the new app.
 */
@SuppressWarnings("unused")
public final class Preferences implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "Preferences";

    private static final String PREF_LOCAL_REPO_HTTPS = "localRepoHttps";
    public static final String PREF_LOCAL_REPO_NAME = "localRepoName";
    public static final String PREF_SCAN_REMOVABLE_STORAGE = "scanRemovableStorage";

    private static Preferences instance;
    private final Set<ChangeListener> localRepoHttpsListeners = new HashSet<>();

    private Preferences() {
    }

    public static Preferences get() {
        if (instance == null) {
            instance = new Preferences();
        }
        return instance;
    }

    public boolean isLocalRepoHttpsEnabled() {
        return false;
    }

    public String getLocalRepoName() {
        return android.os.Build.MODEL;
    }

    public boolean isScanRemovableStorageEnabled() {
        return false;
    }

    public boolean forceTouchApps() {
        return false;
    }

    public boolean isForceOldIndexEnabled() {
        return false;
    }

    public boolean isIpfsEnabled() {
        return false;
    }

    public List<String> getActiveIpfsGateways() {
        return new ArrayList<>();
    }

    public boolean isPureBlack() {
        return false;
    }

    public interface ChangeListener {
        void onPreferenceChange();
    }

    public void registerLocalRepoHttpsListeners(ChangeListener listener) {
        localRepoHttpsListeners.add(listener);
    }

    public void unregisterLocalRepoHttpsListeners(ChangeListener listener) {
        localRepoHttpsListeners.remove(listener);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (PREF_LOCAL_REPO_HTTPS.equals(key)) {
            for (ChangeListener listener : localRepoHttpsListeners) {
                listener.onPreferenceChange();
            }
        }
    }
}

