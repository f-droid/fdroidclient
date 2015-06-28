package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.support.v4.view.MenuItemCompat;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import org.fdroid.fdroid.Preferences;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapManager;

public class ConfirmReceive extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    public ConfirmReceive(Context context) {
        super(context);
    }

    public ConfirmReceive(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmReceive(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public ConfirmReceive(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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


    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return true;
    }

    @Override
    public int getStep() {
        return SwapManager.STEP_CONFIRM_SWAP;
    }

    @Override
    public int getPreviousStep() {
        return SwapManager.STEP_INTRO;
    }

    @ColorRes
    public int getToolbarColour() {
        return getResources().getColor(R.color.swap_blue);
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_confirm);
    }
}
