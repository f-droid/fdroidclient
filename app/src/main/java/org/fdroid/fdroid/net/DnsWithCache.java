package org.fdroid.fdroid.net;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.fdroid.database.DnsCache;
import org.fdroid.database.DnsCacheDao;
import org.fdroid.database.FDroidDatabase;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import okhttp3.Dns;

public class DnsWithCache implements Dns {

    private DnsCacheDao dao;

    private HashMap<String, List<InetAddress>> cache;

    @Nullable
    private Disposable disposable;

    private DnsWithCache() {
        // no-op, require db
    }

    public DnsWithCache(FDroidDatabase db) {
        dao = db.getDnsCacheDao();
        // initialize cache from db
        disposable = Single.fromCallable(() -> dao.getDnsCache())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dbCache -> {
                    for (DnsCache dbItem : dbCache) {
                        // if an ip address is supplied, only the format of the address is checked.
                        List<InetAddress> addressList = new ArrayList<InetAddress>();
                        for (String s : dbItem.getAddressList()) {
                            try {
                                InetAddress address = InetAddress.getByName(s);
                                addressList.add(address);
                            } catch (UnknownHostException e) {
                                Log.e("DnsWithCache", "Exception thrown when converting " + s + ": " + e);
                            }
                        }
                        populateCache(dbItem.getHostName(), addressList);
                    }
                    disposable.dispose();
                });
    }

    public void populateCache(@NonNull String s, @NonNull List<InetAddress> l) {
        if (cache == null) {
            cache = new HashMap<String, List<InetAddress>>();
        }
        // replace existing lists to reflect db behavior
        cache.put(s, l);
    }

    public void populateCacheAndDb(@NonNull String s, @NonNull List<InetAddress> l) {
        List<String> dbList = new ArrayList<String>();
        for (InetAddress i : l) {
            String ip = i.getHostAddress();
            dbList.add(ip);
        }
        // db table will replace old lists with updated lists
        disposable = Single.fromCallable(() -> dao.getDnsCache())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(dbCache -> {
                    dao.insert(new DnsCache(s, dbList));
                    disposable.dispose();
                });
        populateCache(s, l);
    }

    @NonNull
    @Override
    public List<InetAddress> lookup(@NonNull String s) throws UnknownHostException {
        if (cache != null && cache.keySet().contains(s)) {
            // return cached ip list if available
            return cache.get(s);
        } else {
            // do dns lookup and cache ip list
            List<InetAddress> list = Dns.SYSTEM.lookup(s);
            populateCacheAndDb(s, list);
            return list;
        }
    }
}
