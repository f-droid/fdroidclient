package org.fdroid.fdroid.net;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.Preferences;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class DnsCache {

    private volatile HashMap<String, List<InetAddress>> cache;
    private static final int DELAY_TIME = 1;
    private static final TimeUnit DELAY_UNIT = TimeUnit.SECONDS;
    private volatile boolean writeScheduled = false;

    private final Runnable delayedWrite = () -> {
        Preferences prefs = Preferences.get();
        prefs.setDnsCache(cache);
        writeScheduled = false;
    };

    private final ScheduledExecutorService writeExecutor = Executors.newSingleThreadScheduledExecutor();

    private static DnsCache instance;

    private DnsCache() {
        Preferences prefs = Preferences.get();
        cache = prefs.getDnsCache();
        if (cache == null) {
            cache = new HashMap<>();
        }
    }

    public static DnsCache get() {
        if (instance == null) {
            instance = new DnsCache();
        }
        return instance;
    }

    public void insert(@NonNull String url, @NonNull List<InetAddress> ipList) {
        cache.put(url, ipList);
        if (!writeScheduled) {
            writeScheduled = true;
            writeExecutor.schedule(delayedWrite, DELAY_TIME, DELAY_UNIT);
        }
    }

    public List<InetAddress> remove(@NonNull String url) {
        List<InetAddress> removed = cache.remove(url);
        if (!writeScheduled && removed != null) {
            writeScheduled = true;
            writeExecutor.schedule(delayedWrite, DELAY_TIME, DELAY_UNIT);
        }
        return removed;
    }

    public List<InetAddress> lookup(@NonNull String url) {
        Preferences prefs = Preferences.get();
        if (!prefs.isDnsCacheEnabled() || !cache.keySet().contains(url)) {
            return null;
        } else {
            return cache.get(url);
        }
    }
}
