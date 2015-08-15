package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.localrepo.SwapService;
import org.fdroid.fdroid.localrepo.peers.Peer;

// TODO: Use this for the "Preparing local repo" dialog also.
public class SwapConnecting extends LinearLayout implements SwapWorkflowActivity.InnerView {

    private final static String TAG = "SwapConnecting";

    public SwapConnecting(Context context) {
        super(context);
    }

    public SwapConnecting(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SwapConnecting(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public SwapConnecting(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity)getContext();
    }

    private SwapService getManager() {
        return getActivity().getState();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Peer peer = getManager().getPeer();
        if (peer == null) {
            Log.e(TAG, "Cannot find the peer to connect to.");

            // TODO: Don't go to the selected apps, rather show a Toast message and then go to the intro screen.
            getActivity().showSelectApps();
            return;
        }

        String heading = getContext().getString(R.string.status_connecting_to_repo, peer.getName());
        ((TextView) findViewById(R.id.heading)).setText(heading);

        findViewById(R.id.back).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().showIntro();
            }
        });

        // TODO: Unregister correctly, not just when being notified of completion or errors.
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(repoUpdateReceiver, new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));
        getManager().connectTo(peer, peer.shouldPromptForSwapBack());

    }

    private BroadcastReceiver repoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int statusCode = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE, -1);

            TextView progressText = ((TextView) findViewById(R.id.heading));
            TextView errorText    = ((TextView) findViewById(R.id.error));
            Button   backButton   = ((Button) findViewById(R.id.back));

            if (intent.hasExtra(UpdateService.EXTRA_MESSAGE)) {
                progressText.setText(intent.getStringExtra(UpdateService.EXTRA_MESSAGE));
            }

            boolean finished = false;
            boolean error = false;

            progressText.setVisibility(View.VISIBLE);
            errorText.setVisibility(View.GONE);
            backButton.setVisibility(View.GONE);

            switch (statusCode) {
                case UpdateService.STATUS_ERROR_GLOBAL:
                    finished = true;
                    error = true;
                    break;
                case UpdateService.STATUS_COMPLETE_WITH_CHANGES:
                    finished = true;
                    break;
                case UpdateService.STATUS_COMPLETE_AND_SAME:
                    finished = true;
                    break;
                case UpdateService.STATUS_INFO:
                    break;
            }

            if (finished) {
                LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(repoUpdateReceiver);
                if (error) {
                    progressText.setVisibility(View.GONE);
                    errorText.setVisibility(View.VISIBLE);
                    backButton.setVisibility(View.VISIBLE);
                } else {
                    getActivity().showSwapConnected();
                }
            }
        }
    };

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
