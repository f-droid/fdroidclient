package org.fdroid.fdroid.net;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.Preferences;
import org.jetbrains.annotations.NotNull;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.Dns;

public final class DnsWithCache implements Dns {

    private final DnsCache cache;

    private static DnsWithCache instance;

    private DnsWithCache() {
        cache = DnsCache.get();
    }

    public static DnsWithCache get() {
        if (instance == null) {
            instance = new DnsWithCache();
        }
        return instance;
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String url) throws UnknownHostException {
        Preferences prefs = Preferences.get();
        if (!prefs.isDnsCacheEnabled()) {
            return Dns.SYSTEM.lookup(url);
        }
        List<InetAddress> list = cache.lookup(url);
        if (list == null) {
            list = Dns.SYSTEM.lookup(url);
            cache.insert(url, list);
        }
        return list;
    }

    /**
     * in case a host is unreachable, check whether the cached dns result is different from
     * the current result.  if the cached result is different, remove that result from the
     * cache.  returns true if a cached result was removed, indicating that the connection
     * should be retried, otherwise returns false.
     */
    public boolean shouldRetryRequest(@NotNull String url) {
        Preferences prefs = Preferences.get();
        if (!prefs.isDnsCacheEnabled()) {
            // the cache feature was not enabled, so a cached result didn't cause the failure
            return false;
        }
        List<InetAddress> list = cache.lookup(url);
        if (list == null) {
            // no cached result was found, so a cached result didn't cause the failure
            return false;
        } else {
            try {
                List<InetAddress> lookupList = Dns.SYSTEM.lookup(url);
                for (InetAddress address : lookupList) {
                    if (!list.contains(address)) {
                        // the cached result doesn't match the current dns result, so the connection should be retried
                        cache.remove(url);
                        return true;
                    }
                }
                // the cached result matches the current dns result, so a cached result didn't cause the failure
                return false;
            } catch (UnknownHostException e) {
                // the url returned an unknown host exception, so there's no point in retrying
                return false;
            }
        }
    }
}
