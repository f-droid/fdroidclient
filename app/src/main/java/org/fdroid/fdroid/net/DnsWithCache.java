package org.fdroid.fdroid.net;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.Preferences;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.Dns;

public class DnsWithCache implements Dns {

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

    public DnsWithCache() {
        Preferences prefs = Preferences.get();
        cache = prefs.getDnsCache();
    }

    public void updateCacheAndPrefs(@NonNull String url, @NonNull List<InetAddress> ipList) {
        updateCache(url, ipList);
        if (!writeScheduled) {
            writeScheduled = true;
            writeExecutor.schedule(delayedWrite, DELAY_TIME, DELAY_UNIT);
        }
    }

    public void updateCache(@NonNull String url, @NonNull List<InetAddress> ipList) {
        if (cache == null) {
            cache = new HashMap<String, List<InetAddress>>();
        }
        cache.put(url, ipList);
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String url) throws UnknownHostException {
        Preferences prefs = Preferences.get();
        if (!prefs.isDnsCacheEnabled()
                || cache == null
                || !cache.keySet().contains(url)) {
            // do dns lookup and cache ip list
            List<InetAddress> ipList = Dns.SYSTEM.lookup(url);
            updateCacheAndPrefs(url, ipList);
            return ipList;
        } else {
            // return cached ip list if available
            return cache.get(url);
        }
    }
}
