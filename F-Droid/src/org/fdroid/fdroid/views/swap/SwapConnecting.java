package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.ProgressListener;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.localrepo.SwapManager;
import org.fdroid.fdroid.localrepo.peers.Peer;

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

    private SwapManager getManager() {
        return getActivity().getState();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        Peer peer = getManager().getPeer();
        if (peer == null) {
            Log.e(TAG, "Cannot find the peer to connect to.");

            // TODO: Don't go to the selected apps, rather show a Toast message and then
            // go to the intro screen.
            getActivity().showSelectApps();
            return;
        }

        String heading = getContext().getString(R.string.status_connecting_to_repo, getActivity().getState().getPeer().getName());
        ((TextView) findViewById(R.id.heading)).setText(heading);

        UpdateService.UpdateReceiver receiver = getManager().connectTo(peer);

        receiver.hideDialog();
        receiver.setListener(new ProgressListener() {
            @Override
            public void onProgress(Event event) {
                ((TextView) findViewById(R.id.progress)).setText(event.data.getString(UpdateService.EXTRA_ADDRESS));
            }
        });
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return true;
    }

    @Override
    public int getStep() {
        return SwapManager.STEP_CONNECTING;
    }

    @Override
    public int getPreviousStep() {
        return SwapManager.STEP_SELECT_APPS;
    }

    @ColorRes
    public int getToolbarColour() {
        return getResources().getColor(R.color.swap_blue);
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_connecting);
    }
}
