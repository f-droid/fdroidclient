package org.fdroid.fdroid.net;

import android.content.Context;
import android.telephony.TelephonyManager;

import androidx.annotation.NonNull;
import androidx.core.os.LocaleListCompat;

import org.fdroid.download.MirrorParameterManager;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.data.App;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FDroidMirrorParameterManager implements MirrorParameterManager {

    private final ConcurrentHashMap<String, Integer> errorCache;
    private static final int DELAY_TIME = 5;
    private static final TimeUnit DELAY_UNIT = TimeUnit.SECONDS;
    private volatile boolean writeErrorScheduled = false;
    private final Runnable delayedErrorWrite;
    private final ScheduledExecutorService writeErrorExecutor = Executors.newSingleThreadScheduledExecutor();

    public FDroidMirrorParameterManager() {
        Preferences prefs = Preferences.get();
        errorCache = new ConcurrentHashMap<String, Integer>(prefs.getMirrorErrorData());
        delayedErrorWrite = () -> {
            Map<String, Integer> snapshot = Collections.unmodifiableMap(new HashMap<String, Integer>(errorCache));
            Preferences writePrefs = Preferences.get();
            writePrefs.setMirrorErrorData(snapshot);
            writeErrorScheduled = false;
        };
    }

    public void updateErrorCacheAndPrefs(@NonNull String url, @NonNull Integer errorCount) {
        errorCache.put(url, errorCount);
        if (!writeErrorScheduled) {
            writeErrorScheduled = true;
            writeErrorExecutor.schedule(delayedErrorWrite, DELAY_TIME, DELAY_UNIT);
        }
    }

    @Override
    public void incrementMirrorErrorCount(@NonNull String mirrorUrl) {
        if (errorCache.containsKey(mirrorUrl)) {
            updateErrorCacheAndPrefs(mirrorUrl, errorCache.get(mirrorUrl) + 1);
        } else {
            updateErrorCacheAndPrefs(mirrorUrl, 1);
        }
    }

    @Override
    public int getMirrorErrorCount(@NonNull String mirrorUrl) {
        if (errorCache.containsKey(mirrorUrl)) {
            return errorCache.get(mirrorUrl);
        } else {
            return 0;
        }
    }

    @Override
    public boolean preferForeignMirrors() {
        Preferences prefs = Preferences.get();
        return prefs.isPreferForeignSet();
    }

    @NonNull
    @Override
    public String getCurrentLocation() {
        TelephonyManager tm = (TelephonyManager) FDroidApp.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
        if (tm.getSimCountryIso() != null) {
            return tm.getSimCountryIso();
        } else if (tm.getNetworkCountryIso() != null) {
            return tm.getNetworkCountryIso();
        } else {
            LocaleListCompat localeList = App.getLocales();
            if (localeList != null && localeList.size() > 0) {
                return localeList.get(0).getCountry();
            } else {
                return "";
            }
        }
    }
}
