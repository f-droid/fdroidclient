package org.fdroid.fdroid.views.swap;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import org.fdroid.fdroid.R;
import org.fdroid.fdroid.data.NewRepoConfig;
import org.fdroid.fdroid.localrepo.SwapView;

public class ConfirmReceiveView extends SwapView {

    private NewRepoConfig config;

    public ConfirmReceiveView(Context context) {
        super(context);
    }

    public ConfirmReceiveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ConfirmReceiveView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public ConfirmReceiveView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
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

    public void setup(NewRepoConfig config) {
        this.config = config;
    }
}
