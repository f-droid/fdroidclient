package org.fdroid.fdroid.views.swap.views;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.localrepo.SwapState;
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

public class StartSwapView extends LinearLayout implements SwapWorkflowActivity.InnerView {

    // TODO: Is there a way to guarangee which of these constructors the inflater will call?
    // Especially on different API levels? It would be nice to only have the one which accepts
    // a Context, but I'm not sure if that is correct or not. As it stands, this class provides
    // constructurs which match each of the ones available in the parent class.
    // The same is true for the other views in the swap process too.

    public StartSwapView(Context context) {
        super(context);
    }

    public StartSwapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public StartSwapView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        // TODO: Try and find a better way to get to the SwapActivity, which makes less asumptions.
        return (SwapWorkflowActivity)getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.button_start_swap).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().showSelectApps();
            }
        });

    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return false;
    }

    @Override
    public int getStep() {
        return SwapState.STEP_INTRO;
    }

    @Override
    public int getPreviousStep() {
        // TODO: Currently this is handleed by the SwapWorkflowActivity as a special case, where
        // if getStep is STEP_INTRO, don't even bother asking for getPreviousStep. But that is a
        // bit messy. It would be nicer if this was handled using the same mechanism as everything
        // else.
        return SwapState.STEP_INTRO;
    }
}
