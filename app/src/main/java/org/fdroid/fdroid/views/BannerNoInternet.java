package org.fdroid.fdroid.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.Utils;

/**
 * Widget which monitors the network state and shows a "No Internet" message when it identifies the
 * device is not connected. Will only monitor the wifi state when attached to the window.
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
            Utils.NetworkState state = Utils.getNetworkState(getContext());
            if (state == Utils.NetworkState.NET_UNAVAILABLE) {
                setVisibility(View.VISIBLE);
            } else {
                setVisibility(View.GONE);
            }
        }
    }

    private void stopMonitoringNetworkState() {

    }
}
