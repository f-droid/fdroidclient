package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

/**
 * Widget which monitors the network state and shows a "No Internet" message when it identifies the
 * device is not connected. Will only monitor the wifi state when attached to the window.
 * Note that this does a pretty poor job of responding to network changes in real time. It only
 * knows how to respond to the _enabling_ of wifi (not disabling of wifi, nor enabling/disabling
 * of mobile data). However it will always query the network state when it is shown to the user. This
 * way if they change between tabs, hide and then open F-Droid, or do other things which require the
 * view to attach to the window again then it will update the network state. In practice this works
 * pretty well.
 */
public class BannerNoInternet extends android.support.v7.widget.AppCompatTextView {

    public BannerNoInternet(Context context) {
        this(context, null);
    }

    public BannerNoInternet(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public BannerNoInternet(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int padding = (int) getResources().getDimension(R.dimen.banner__padding);
        setPadding(padding, padding, padding, padding);
        setBackgroundColor(0xFF4A4A4A);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        setText(R.string.banner_no_internet);
        setTextColor(0xFFFFFFFF);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        monitorNetworkState();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMonitoringNetworkState();
    }

    private void monitorNetworkState() {
        if (isInEditMode()) {
            // Don't try and query the network state if in the Android Studio UI Builder (it wont work).
            setVisibility(View.VISIBLE);
        } else {
            getContext().registerReceiver(
                    onNetworkStateChanged, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
            updateNetworkState();
        }
    }

    private void updateNetworkState() {
        Utils.NetworkState state = Utils.getNetworkState(getContext());
        if (state == Utils.NetworkState.NET_UNAVAILABLE) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    private void stopMonitoringNetworkState() {
        getContext().unregisterReceiver(onNetworkStateChanged);
    }

    private final BroadcastReceiver onNetworkStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateNetworkState();
        }
    };
}
