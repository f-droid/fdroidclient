package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.localrepo.SwapService;

// TODO: Use this for the "Preparing local repo" dialog also.
public class SwapConnecting extends LinearLayout implements SwapWorkflowActivity.InnerView {

    @SuppressWarnings("unused")
    private static final String TAG = "SwapConnecting";

    public SwapConnecting(Context context) {
        super(context);
    }

    public SwapConnecting(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(11)
    public SwapConnecting(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public SwapConnecting(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        ((TextView) findViewById(R.id.heading)).setText(R.string.swap_connecting);
        findViewById(R.id.back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().showIntro();
            }
        });

        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                repoUpdateReceiver, new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(
                prepareSwapReceiver, new IntentFilter(SwapWorkflowActivity.PrepareSwapRepo.ACTION));
    }

    /**
     * Remove relevant listeners/receivers/etc so that they do not receive and process events
     * when this view is not in use.
     */
    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(repoUpdateReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(prepareSwapReceiver);
    }

    private final BroadcastReceiver repoUpdateReceiver = new ConnectSwapReceiver();
    private final BroadcastReceiver prepareSwapReceiver = new PrepareSwapReceiver();

    /**
     * Listens for feedback about a local repository being prepared:
     *  * Apk files copied to the LocalHTTPD webroot
     *  * index.html file prepared
     *  * Icons will be copied to the webroot in the background and so are not part of this process.
     */
    class PrepareSwapReceiver extends Receiver {

        @Override
        protected String getMessageExtra() {
            return SwapWorkflowActivity.PrepareSwapRepo.EXTRA_MESSAGE;
        }

        protected int getType(Intent intent) {
            return intent.getIntExtra(SwapWorkflowActivity.PrepareSwapRepo.EXTRA_TYPE, -1);
        }

        @Override
        protected boolean isComplete(Intent intent) {
            return getType(intent) == SwapWorkflowActivity.PrepareSwapRepo.TYPE_COMPLETE;
        }

        @Override
        protected boolean isError(Intent intent) {
            return getType(intent) == SwapWorkflowActivity.PrepareSwapRepo.TYPE_ERROR;
        }

        @Override
        protected void onComplete() {
            getActivity().onLocalRepoPrepared();
        }
    }

    /**
     * Listens for feedback about a repo update process taking place.
     *  * Tracks an index.jar download and show the progress messages
     */
    class ConnectSwapReceiver extends Receiver {

        @Override
        protected String getMessageExtra() {
            return UpdateService.EXTRA_MESSAGE;
        }

        protected int getStatusCode(Intent intent) {
            return intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);
        }

        @Override
        protected boolean isComplete(Intent intent) {
            int status = getStatusCode(intent);
            return status == UpdateService.STATUS_COMPLETE_AND_SAME ||
                    status == UpdateService.STATUS_COMPLETE_WITH_CHANGES;
        }

        @Override
        protected boolean isError(Intent intent) {
            int status = getStatusCode(intent);
            return status == UpdateService.STATUS_ERROR_GLOBAL ||
                    status == UpdateService.STATUS_ERROR_LOCAL ||
                    status == UpdateService.STATUS_ERROR_LOCAL_SMALL;
        }

        @Override
        protected void onComplete() {
            getActivity().showSwapConnected();
        }

    }

    abstract class Receiver extends BroadcastReceiver {

        protected abstract String getMessageExtra();

        protected abstract boolean isComplete(Intent intent);

        protected abstract boolean isError(Intent intent);

        protected abstract void onComplete();

        @Override
        public void onReceive(Context context, Intent intent) {

            TextView progressText = (TextView) findViewById(R.id.heading);
            TextView errorText    = (TextView) findViewById(R.id.error);
            Button   backButton   = (Button) findViewById(R.id.back);

            String message;
            if (intent.hasExtra(getMessageExtra())) {
                message = intent.getStringExtra(getMessageExtra());
                if (message != null) {
                    progressText.setText(message);
                }
            }

            progressText.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);

            if (isError(intent)) {
                progressText.setVisibility(View.GONE);
                errorText.setVisibility(View.VISIBLE);
                backButton.setVisibility(View.VISIBLE);
                return;
            }

            if (isComplete(intent)) {
                onComplete();
            }
        }
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return true;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_CONNECTING;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_SELECT_APPS;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_bright_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_connecting);
    }
}
