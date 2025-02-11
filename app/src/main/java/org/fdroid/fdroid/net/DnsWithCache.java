package org.fdroid.fdroid.net;

import androidx.annotation.NonNull;

import org.fdroid.fdroid.Preferences;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import okhttp3.Dns;

public class DnsWithCache implements Dns {

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String url) throws UnknownHostException {
        Preferences prefs = Preferences.get();
        if (!prefs.isDnsCacheEnabled()) {
            return Dns.SYSTEM.lookup(url);
        }
        DnsCache cache = DnsCache.get();
        List<InetAddress> list = cache.lookup(url);
        if (list == null) {
            list = Dns.SYSTEM.lookup(url);
            cache.insert(url, list);
        }
        return list;
    }
}
