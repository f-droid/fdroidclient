package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.RelativeLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapService;

public class InitialLoadingView extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    public InitialLoadingView(Context context) {
        super(context);
    }

    public InitialLoadingView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public InitialLoadingView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public InitialLoadingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return true;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_INITIAL_LOADING;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_JOIN_WIFI;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap);
    }
}
