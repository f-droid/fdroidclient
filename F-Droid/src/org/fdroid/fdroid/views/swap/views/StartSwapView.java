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
import org.fdroid.fdroid.views.swap.SwapWorkflowActivity;

public class StartSwapView extends LinearLayout implements SwapWorkflowActivity.InnerView {

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
}
