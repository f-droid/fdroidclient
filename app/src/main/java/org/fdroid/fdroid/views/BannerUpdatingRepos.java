package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;

/**
 * Widget which reflects whether or not a repo update is currently in progress or not. If so, shows
 * some sort of feedback to the user.
 */
public class BannerUpdatingRepos extends android.support.v7.widget.AppCompatTextView {

    public BannerUpdatingRepos(Context context) {
        this(context, null);
    }

    public BannerUpdatingRepos(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public BannerUpdatingRepos(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int padding = (int) getResources().getDimension(R.dimen.banner__padding);
        setPadding(padding, padding, padding, padding);
        setBackgroundColor(0xFF4A4A4A);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        setText(R.string.update_notification_title);
        setTextColor(0xFFFFFFFF);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        monitorRepoUpdates();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopMonitoringRepoUpdates();
    }

    private void monitorRepoUpdates() {
        if (isInEditMode()) {
            return;
        }

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(onRepoFeedback,
                new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
        setBannerIsVisible(UpdateService.isUpdating());
    }

    private void setBannerIsVisible(boolean isUpdating) {
        if (isUpdating) {
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    private void stopMonitoringRepoUpdates() {
        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(onRepoFeedback);
    }

    private final BroadcastReceiver onRepoFeedback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Anything other than a STATUS_INFO broadcast signifies that it was complete (and out
            // banner should be removed).
            boolean isInfo = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, 0) == UpdateService.STATUS_INFO;
            setBannerIsVisible(isInfo);
        }
    };
}
