package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import androidx.lifecycle.Observer;
import androidx.preference.PreferenceManager;

import org.fdroid.database.Repository;
import org.fdroid.download.Mirror;
import org.fdroid.fdroid.FDroidApp;
import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.RepoUpdateManager;
import org.fdroid.fdroid.net.BluetoothDownloader;
import org.fdroid.fdroid.net.ConnectivityMonitorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Banner widget which reflects current status related to repository updates.
 * It will display whether repositories area actively being updated, or
 * whether there is no Internet connection, so repositories cannot be updated
 * from the Internet.
 * <p>
 * It shows a "No Internet" message when it identifies the device is not
 * connected. Will only monitor the wifi state when attached to the window.
 * Note that this does a pretty poor job of responding to network changes in
 * real time. It only knows how to respond to the <em>enabling</em> of WiFi
 * (not disabling of WiFi, nor enabling/disabling of mobile data). However it
 * will always query the network state when it is shown to the user. This way
 * if they change between tabs, hide and then open F-Droid, or do other things
 * which require the view to attach to the window again then it will update the
 * network state. In practice this works pretty well.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroidclient/-/merge_requests/724">"No internet" banner on main, categories, and updates screen</a>
 */
public class StatusBanner extends androidx.appcompat.widget.AppCompatTextView {

    private boolean isUpdatingRepos;
    private int networkState = ConnectivityMonitorService.FLAG_NET_NO_LIMIT;
    private int overDataState;
    private int overWiFiState;

    private final SharedPreferences preferences;
    private final RepoUpdateManager repoUpdateManager;

    public StatusBanner(Context context) {
        this(context, null);
    }

    public StatusBanner(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public StatusBanner(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int padding = (int) getResources().getDimension(R.dimen.banner__padding);
        setPadding(padding, padding, padding, padding);
        setBackgroundColor(0xFF4A4A4A);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        setTextColor(0xFFFFFFFF);

        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        repoUpdateManager = FDroidApp.getRepoUpdateManager(context);
        isUpdatingRepos = repoUpdateManager.isUpdating().getValue();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Context context = getContext();
        networkState = ConnectivityMonitorService.getNetworkState(context);
        context.registerReceiver(onNetworkStateChanged,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        isUpdatingRepos = repoUpdateManager.isUpdating().getValue();
        repoUpdateManager.isUpdatingLiveData().observeForever(onRepoUpdateChanged);

        overDataState = Preferences.get().getOverData();
        overWiFiState = Preferences.get().getOverWifi();
        preferences.registerOnSharedPreferenceChangeListener(dataWifiChangeListener);

        setBannerTextAndVisibility();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Context context = getContext();
        repoUpdateManager.isUpdatingLiveData().removeObserver(onRepoUpdateChanged);
        context.unregisterReceiver(onNetworkStateChanged);
        preferences.unregisterOnSharedPreferenceChangeListener(dataWifiChangeListener);
    }

    /**
     * Display banner with specific text depending on updating status, network
     * connectivity, and Data/WiFi Settings.  This also takes into account
     * whether there are local repos/mirrors available, e.g. if there is a
     * mirror on a USB OTG thumb drive.  Local repos on system partitions are
     * not treated as local mirrors here, they are shipped as part of the
     * device, and users are generally not aware of them.
     */
    private void setBannerTextAndVisibility() {
        if (isUpdatingRepos) {
            setText(R.string.banner_updating_repositories);
            setVisibility(View.VISIBLE);
        } else if (networkState == ConnectivityMonitorService.FLAG_NET_UNAVAILABLE
                || networkState == ConnectivityMonitorService.FLAG_NET_DEVICE_AP_WITHOUT_INTERNET) {
            setText(R.string.banner_no_internet);
            setVisibility(View.VISIBLE);
        } else if (overDataState == Preferences.OVER_NETWORK_NEVER
                && overWiFiState == Preferences.OVER_NETWORK_NEVER) {
            List<Repository> repos = FDroidApp.getRepoManager(getContext()).getRepositories();
            List<Repository> localRepos = getLocalRepos(repos);
            boolean hasLocalNonSystemRepos = true;
            final List<String> systemPartitions = Arrays.asList("odm", "oem", "product", "system", "vendor");
            for (Repository repo : localRepos) {
                for (String segment : Uri.parse(repo.getAddress()).getPathSegments()) {
                    if (systemPartitions.contains(segment)) {
                        hasLocalNonSystemRepos = false;
                        break;
                    }
                    break; // only check the first segment NOPMD
                }
            }
            if (localRepos.size() == 0 || !hasLocalNonSystemRepos) {
                setText(R.string.banner_no_data_or_wifi);
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
            }
        } else {
            setVisibility(View.GONE);
        }
    }

    /**
     * Return the repos in the {@code repos} {@link List} that have either a
     * local canonical URL or a local mirror URL.  These are repos that can be
     * updated and used without using the Internet.
     */
    public static List<Repository> getLocalRepos(List<Repository> repos) {
        ArrayList<Repository> localRepos = new ArrayList<>();
        for (Repository repo : repos) {
            if (isLocalRepoAddress(repo.getAddress())) {
                localRepos.add(repo);
            } else {
                for (Mirror mirror : repo.getMirrors()) {
                    if (!mirror.isHttp()) {
                        localRepos.add(repo);
                        break;
                    }
                }
            }
        }
        return localRepos;
    }

    private static boolean isLocalRepoAddress(String address) {
        return address != null &&
                (address.startsWith(BluetoothDownloader.SCHEME)
                        || address.startsWith(ContentResolver.SCHEME_CONTENT)
                        || address.startsWith(ContentResolver.SCHEME_FILE));
    }

    private final Observer<Boolean> onRepoUpdateChanged = new Observer<>() {
        @Override
        public void onChanged(Boolean isUpdating) {
            isUpdatingRepos = isUpdating;
            setBannerTextAndVisibility();
        }
    };

    private final BroadcastReceiver onNetworkStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            networkState = ConnectivityMonitorService.getNetworkState(context);
            setBannerTextAndVisibility();
        }
    };

    private final SharedPreferences.OnSharedPreferenceChangeListener dataWifiChangeListener =
            new SharedPreferences.OnSharedPreferenceChangeListener() {
                @Override
                public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                    if (key.equals(Preferences.PREF_OVER_DATA) || key.equals(Preferences.PREF_OVER_WIFI)) {
                        overDataState = Preferences.get().getOverData();
                        overWiFiState = Preferences.get().getOverWifi();
                        setBannerTextAndVisibility();
                    }
                }
            };
}
