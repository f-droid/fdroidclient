package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.support.annotation.ColorRes;
import android.support.annotation.NonNull;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.localrepo.SwapService;

public class ConfirmReceive extends RelativeLayout implements SwapWorkflowActivity.InnerView {

    private NewRepoConfig config;

    public ConfirmReceive(Context context) {
        super(context);
    }

    public ConfirmReceive(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmReceive(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public ConfirmReceive(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    private SwapWorkflowActivity getActivity() {
        return (SwapWorkflowActivity) getContext();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        findViewById(R.id.no_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().denySwap();
            }
        });

        findViewById(R.id.yes_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().swapWith(config);
            }
        });
    }

    @Override
    public boolean buildMenu(Menu menu, @NonNull MenuInflater inflater) {
        return true;
    }

    @Override
    public int getStep() {
        return SwapService.STEP_CONFIRM_SWAP;
    }

    @Override
    public int getPreviousStep() {
        return SwapService.STEP_INTRO;
    }

    @ColorRes
    public int getToolbarColour() {
        return R.color.swap_blue;
    }

    @Override
    public String getToolbarTitle() {
        return getResources().getString(R.string.swap_confirm);
    }

    public void setup(NewRepoConfig config) {
        this.config = config;
        TextView descriptionTextView = (TextView) findViewById(R.id.text_description);
        descriptionTextView.setText(getResources().getString(R.string.swap_confirm_connect, config.getHost()));
    }
}
